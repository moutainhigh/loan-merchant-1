package com.mod.loan.controller.user;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.enums.ResponseEnum;
import com.mod.loan.common.model.Page;
import com.mod.loan.common.model.RequestThread;
import com.mod.loan.common.model.ResultMessage;
import com.mod.loan.config.Constant;
import com.mod.loan.mapper.*;
import com.mod.loan.model.*;
import com.mod.loan.model.moxie.CallInContactList;
import com.mod.loan.model.moxie.CallOutContactList;
import com.mod.loan.model.moxie.ContactList;
import com.mod.loan.model.moxie.Linkmans;
import com.mod.loan.service.UserService;
import com.mod.loan.util.ExcelUtil;
import com.mod.loan.util.OkHttpReader;
import com.mod.loan.util.TimeUtils;
import com.mod.loan.util.moxie.AddressListUtil;
import com.mod.loan.util.moxie.ContactUtil;
import com.mod.loan.util.moxie.MoxieOssUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserDeviceMapper userDeviceMapper;
    @Autowired
    private UserBankMapper userBankMapper;
    @Autowired
    private UserIdentMapper userIdentMapper;
    @Autowired
    private MoxieZfbMapper moxieZfbMapper;
    @Autowired
    private MoxieMobileMapper moxieMobileMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderAuditMapper orderAuditMapper;
    @Autowired
    private OkHttpReader okHttpReader;

    @RequestMapping(value = "user_select")
    public ModelAndView user_select(ModelAndView view) {
        view.setViewName("user/user_select");
        return view;
    }

    @RequestMapping(value = "user_list")
    public ModelAndView user_list(ModelAndView view) {
        view.setViewName("user/user_list");
        return view;
    }

    @RequestMapping(value = "user_list_ajax", method = {RequestMethod.POST})
    public ResultMessage user_list_ajax(User user, Page page, String startTime, String endTime) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("userName", StringUtils.isNotEmpty(user.getUserName()) ? user.getUserName() : null);
        param.put("userPhone", StringUtils.isNotEmpty(user.getUserPhone()) ? user.getUserPhone() : null);
        param.put("merchant", RequestThread.get().getMerchant());
        param.put("startTime", StringUtils.isNotEmpty(startTime) ? startTime : null);
        param.put("endTime", StringUtils.isNotEmpty(endTime) ? endTime : null);
        param.put("userOrigin", StringUtils.isNotEmpty(user.getUserOrigin()) ? user.getUserOrigin() : null);
        return new ResultMessage(ResponseEnum.M2000, userService.findUserList(param, page), page);
    }

    @RequestMapping(value = "user_detail")
    public ModelAndView user_detail(ModelAndView view, HttpServletRequest request, Long id) {
        //七天通讯录数据url
        String moheTaskId = getTaskIdFromItf(id, "mobile");
        view.addObject("moheTaskId", getTaskIdFromItf(id, "mobile"));
        view.addObject("id", id);
        view.setViewName("user/user_detail");
        return view;
    }

    @RequestMapping(value = "user_detail_ajax", method = {RequestMethod.POST})
    public ResultMessage user_detail_ajax(Long id) {
        Map<String, Object> data = new HashMap<>();
        User user = userService.selectByPrimaryKey(id);
//        if (!user.getMerchant().equals(RequestThread.get().getMerchant())) {
//            return new ResultMessage(ResponseEnum.M4004);
//        }
        data.put("user", user);
        UserInfo userInfo = userInfoMapper.selectByPrimaryKey(id);
        data.put("userInfo", userInfo);
        UserDevice userDevice = userDeviceMapper.selectOneByUid(id);
        data.put("userDevice", userDevice);
        UserBank userBank = userBankMapper.selectOneByUid(id);
        data.put("userBank", userBank);
        UserIdent userIdent = userIdentMapper.selectByPrimaryKey(id);
        data.put("userIdent", userIdent);

        //魔蝎报告
        //MoxieZfb moxieZfb = moxieZfbMapper.selectLastOne(id);
        //data.put("moxieZfbTaskId", moxieZfb != null ? moxieZfb.getTaskId() : null);
        //MoxieMobile moxieMobile = moxieMobileMapper.selectLastOne(id);
        //data.put("moxieMobileTaskId", moxieMobile != null ? moxieMobile.getTaskId() : null);

        //获取数据魔盒淘宝报告url
        String taskId = getTaskIdFromItf(id, "taobao");
        data.put("moheTaobaoReportUrl", getMoheReportUrl(taskId));
        //获取数据魔盒运营商报告url
        taskId = getTaskIdFromItf(id, "mobile");
        data.put("moheMobileReportUrl", getMoheReportUrl(taskId));
        // 共债记录
        data.putAll(orderMapper.countDebtRecord(user.getUserPhone()));
        // 提单历史
        data.putAll(orderMapper.countUserOrderRecord(RequestThread.get().getMerchant(), id));
        return new ResultMessage(ResponseEnum.M2000, data);
    }

    /**
     * 七天通话记录分析
     */
    @RequestMapping(value = "user_call_report")
    public ModelAndView user_call_report(ModelAndView view, String taskId, Long uid) {
        view.addObject("uid",uid);
        view.addObject("taskId", taskId);
        view.setViewName("user/user_call_report");
        return view;
    }

    /**
     * 七天通话记录分析
     */
    @RequestMapping(value = "user_call_report_ajax")
    public String user_call_report_ajax(String taskId, Long uid) {
        try {
            //获取数据魔盒taskId
            String url = Constant.server_itf_url + "tongdun/getOssData?type=weekaddress" + "&taskId=" + taskId + "&uid=" + uid;
            Response execute = Jsoup.connect(url).ignoreContentType(true).ignoreHttpErrors(true).execute();
           return execute.body();
        } catch (Exception e) {
            logger.error("获取七天通话记录分析失败", e);
        }
        return "";
    }

    @RequestMapping(value = "user_zfb_mxreport")
    public ModelAndView user_report_zfb(ModelAndView view, String taskId) {
        view.addObject("taskId", taskId);
        view.setViewName("user/user_zfb_mxreport");
        return view;
    }

    @RequestMapping(value = "user_zfb_mxreport_ajax")
    public String user_zfb_mxreport_ajax(String taskId) throws IOException {
        return MoxieOssUtil.zfbData(Constant.env, taskId);
    }

    @RequestMapping(value = "user_zfb_zmscore_ajax")
    public String user_zfb_zmscore_ajax(String taskId) throws IOException {
        String url = String.format(Constant.moxie_zfb_zmscore, taskId);
        Response response = Jsoup.connect(url).header("Authorization", "token " + Constant.moxie_token).ignoreHttpErrors(true).ignoreContentType(true).execute();
        return response.body();
    }

    @RequestMapping(value = "user_carrier_jxreport")
    public ModelAndView user_carrier_jxreport(ModelAndView view, String taskId) {
        view.addObject("taskId", taskId);
        view.setViewName("user/user_carrier_jxreport");
        return view;
    }

    @RequestMapping(value = "user_carrier_jxreport_ajax", method = {RequestMethod.POST})
    public ResultMessage user_carrier_jxreport_ajax(String taskId) {
        MoxieMobile moxieMobile = moxieMobileMapper.selectByTaskId(taskId);
        Map<String, Object> data = new HashMap<String, Object>();
        UserInfo userInfo = userInfoMapper.selectByPrimaryKey(moxieMobile.getUid());
        String carrierReport = MoxieOssUtil.mobileJxlReport(Constant.env, taskId);
        data.put("userInfo", userInfo);
        JSONObject carrierReportJson = JSONObject.parseObject(carrierReport);
        // 获取呼入和呼出的通话记录信息
        String contact_list = carrierReportJson.getString("contact_list");
        carrierReportJson.remove("contact_list");// 去掉呼入和呼出的通话记录信息
        if (null != contact_list) {
            try {
                // 从itf接口获取通讯录数据
                String url = Constant.server_itf_url + "user/address_list?uid=" + userInfo.getUid();
                Response execute = Jsoup.connect(url).ignoreContentType(true).ignoreHttpErrors(true).execute();
                List<Linkmans> addressList = AddressListUtil.getLinkmans(execute.body());
                Map<String, String> map = AddressListUtil.getMap(addressList);
                List<ContactList> listContactList = JSON.parseArray(contact_list, ContactList.class);
                List<CallInContactList> callInList = ContactUtil.getCallInContactList(map, listContactList);
                List<CallOutContactList> callOutList = ContactUtil.getCallOutContactList(map, listContactList);
                data.put("callInList", callInList);
                data.put("callOutList", callOutList);
            } catch (IOException e) {
                logger.error("从itf获取通讯录失败，uid={}", userInfo.getUid());
            }
        }
        data.put("carrierReport", carrierReportJson);
        return new ResultMessage(ResponseEnum.M2000, data);
    }

    @RequestMapping(value = "user_magic_wand_report")
    public ModelAndView user_magic_wand_report(ModelAndView view, String taskId) {
        view.addObject("taskId", taskId);
        view.setViewName("user/user_magic_wand_report");
        return view;
    }

    @RequestMapping(value = "user_magic_wand_ajax")
    public String user_magic_wand_ajax(String taskId) throws IOException {
        String itfUrl = Constant.server_itf_url + "moxie/magic_wand_report?taskId=" + taskId;
        String data = "";
        try {
            Response execute = Jsoup.connect(itfUrl).ignoreContentType(true).ignoreHttpErrors(true).execute();
            data = execute.body();
        } catch (Exception e) {
            logger.error("oss获取魔杖报告异常", e);
        }
        return data;
    }

    @RequestMapping(value = "user_audit_list")
    public ModelAndView user_audit_list(ModelAndView view, Long id) {
        view.addObject("id", id);
        view.setViewName("user/user_audit_list");
        return view;
    }

    @RequestMapping(value = "user_audit_list_ajax")
    public ResultMessage user_audit_list_ajax(Long id) {
        return new ResultMessage(ResponseEnum.M2000, orderAuditMapper.findAuditListByUid(id, RequestThread.get().getMerchant()));
    }

    @RequestMapping(value = "user_recycle_list")
    public ModelAndView user_recycle_list(ModelAndView view, Long id) {
        view.addObject("id", id);
        view.setViewName("user/user_recycle_list");
        return view;
    }

    @RequestMapping(value = "user_basic_edit")
    public ModelAndView user_basic_edit(ModelAndView view, Long id) {
        view.addObject("id", id);
        view.setViewName("user/user_basic_edit");
        return view;
    }

    @RequestMapping(value = "user_basic_ajax", method = {RequestMethod.POST})
    public ResultMessage user_basic_ajax(Long id) {
        User user = userService.selectByPrimaryKey(id);
        if (user == null || !user.getMerchant().equals(RequestThread.get().getMerchant())) {
            return new ResultMessage(ResponseEnum.M4000);
        }
        return new ResultMessage(ResponseEnum.M2000, user);
    }

    @RequestMapping(value = "user_basic_update_ajax", method = {RequestMethod.POST})
    public ResultMessage user_basic_update_ajax(User user) {
        if (user == null || !user.getMerchant().equals(RequestThread.get().getMerchant())) {
            return new ResultMessage(ResponseEnum.M4000.getCode(), "用户不存在");
        }
        return new ResultMessage(ResponseEnum.M2000, userService.updateByPrimaryKeySelective(user));
    }

    //@RequestMapping(value = "export_report_user_list")
    public void export_report(HttpServletResponse response, String userOrigin, String startTime, String endTime) {
        if (StringUtils.isBlank(userOrigin) || StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            return;
        }
        try {
            String[] title = null;
            String sheetName = null;
            String[] columns = null;
            List<Map<String, Object>> list = null;
            String downloadFileName = TimeUtils.parseTime(new Date(), TimeUtils.dateformat4);
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("merchant", RequestThread.get().getMerchant());
            param.put("userOrigin", userOrigin);
            param.put("startTime", StringUtils.isNotEmpty(startTime) ? startTime : null);
            param.put("endTime", StringUtils.isNotEmpty(endTime) ? endTime : null);

            downloadFileName += "-用户渠道列表";
            title = new String[]{"渠道编号", "用户姓名", "用户手机", "注册时间", "是否借款"};
            sheetName = "用户渠道列表";
            columns = new String[]{"user_origin", "user_name", "user_phone", "create_time", "borrow_flag"};
            list = userService.exportUserOriginReport(param);
            HSSFWorkbook workbook = new HSSFWorkbook();
            ExcelUtil.createSheet(workbook, sheetName, title, ExcelUtil.mapToArray(list, columns));
            ExcelUtil.excelExp(response, downloadFileName, workbook);
        } catch (Exception e) {
            logger.error("用户渠道列表报告导出异常。", e);
        }
    }

    /**
     * 获取数据魔盒报告url
     */
    private String getMoheReportUrl(String taskId) {
        String url = "";
        try {
            if(StringUtils.isNotBlank(taskId)){
                //step 1.从api获取同盾数据魔盒配置账号
                url = Constant.server_api_url + "tongdun/getConfig";
                Response execute = Jsoup.connect(url).ignoreContentType(true).ignoreHttpErrors(true).execute();
                JSONObject data = JSONObject.parseObject(execute.body());
                data = data.getJSONObject("data");

                //step 2.查询数据魔盒免密token
                url = String.format("%s?partner_code=%s&partner_key=%s", data.getString("tokenUrl"), data.getString("partnerCode"), data.getString("partnerKey"));
                String result = okHttpReader.get(url, null, null);
                logger.info("mohe token result:", result);
                JSONObject json = JSONObject.parseObject(result);

                //step 3.获取报告url地址
                url = data.getString("reportUrl") + String.format("/%s/%s", taskId, json.getString("data"));
                logger.info("mohe report url:", url);
            }
        } catch (Exception e) {
            logger.error("getMoheReportUrl error", e);
        }
        return url;
    }

    /**
     * 根据uid从itf服务器获取同盾taskId
     */
    private String getTaskIdFromItf(Long uid, String type) {
        try {
            String url = Constant.server_itf_url + "tongdun/getTaskId?type=" + type + "&uid=" + uid;
            Response execute = Jsoup.connect(url).ignoreContentType(true).ignoreHttpErrors(true).execute();
            return execute.body();
        } catch (IOException e) {
            logger.error("getTaskIdFromItf", e);
        }
        return "";
    }
}
