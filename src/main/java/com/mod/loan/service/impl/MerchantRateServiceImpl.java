package com.mod.loan.service.impl;

import com.mod.loan.common.mapper.BaseServiceImpl;
import com.mod.loan.common.model.Page;
import com.mod.loan.mapper.MerchantRateMapper;
import com.mod.loan.model.MerchantRate;
import com.mod.loan.service.MerchantRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MerchantRateServiceImpl extends BaseServiceImpl<MerchantRate, Long> implements MerchantRateService {

	@Resource
	private MerchantRateMapper merchantRateMapper;

	@Override
	public List<Map<String, Object>> findMerchantRateList(MerchantRate merchantRate, Page page) {
		Map<String, Object> param = new HashMap<String, Object>();

		param.put("merchant", merchantRate.getMerchant());
		param.put("productStatus", merchantRate.getProductStatus());

		param.put("startIndex", page.getStartIndex());
		param.put("pageSize", page.getPageSize());
		page.setTotalCount(merchantRateMapper.merchantRateCount(param));
		return merchantRateMapper.findMerchantRateList(param);
	}
}