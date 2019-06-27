package com.mod.loan.controller.order;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mod.loan.common.enums.ResponseEnum;
import com.mod.loan.common.model.RequestThread;
import com.mod.loan.common.model.ResultMessage;
import com.mod.loan.mapper.OrderMapper;
import com.mod.loan.model.Order;

@RestController
@RequestMapping(value = "order")
public class OperateController {

	@Autowired
	private OrderMapper orderMapper;

	@RequestMapping(value = "order_bad")
	public ResultMessage order_bad(Long orderId) {
		Order order = orderMapper.selectByPrimaryKey(orderId);
		if (order == null || !order.getMerchant().equals(RequestThread.get().getMerchant())) {
			return new ResultMessage(ResponseEnum.M4000.getCode(), "非法操作");
		}
		if (order.getStatus() != 33) {
			return new ResultMessage(ResponseEnum.M4000.getCode(), "逾期订单才能设为坏账");
		}
		Order record = new Order();
		record.setId(orderId);
		record.setStatus(34);
		record.setUpdateTime(new Date());
		orderMapper.updateByPrimaryKeySelective(record);
		return new ResultMessage(ResponseEnum.M2000);
	}

	@RequestMapping(value = "order_reduce")
	public ResultMessage order_reduce(Long orderId, BigDecimal money) {
		Order order = orderMapper.selectByPrimaryKey(orderId);
		if (money == null) {
			return new ResultMessage(ResponseEnum.M4000.getCode(), "请输入减免金额");
		}
		if (order == null || !order.getMerchant().equals(RequestThread.get().getMerchant())) {
			return new ResultMessage(ResponseEnum.M4000.getCode(), "非法操作");
		}
//		if (order.getStatus() != 33 && order.getStatus() != 34 && order.getStatus() != 36
//			&& order.getStatus() != 37 && order.getStatus() != 38) {
//			return new ResultMessage(ResponseEnum.M4000.getCode(), "逾期订单才能减免金额");
//		}
		if (money.signum() < 0) {
			return new ResultMessage(ResponseEnum.M4000.getCode(), "减免金额不应小于0");
		}
		if (order.getShouldRepay().compareTo(money) <= 0) {
			return new ResultMessage(ResponseEnum.M4000.getCode(), "减免金额应小于应还金额");
		}
		Order record = new Order();
		record.setId(orderId);
		record.setUpdateTime(new Date());
		record.setReduceMoney(money);
		record.setShouldRepay(order.getInterestFee().add(order.getBorrowMoney()).add(order.getOverdueFee()).subtract(money));
		orderMapper.updateByPrimaryKeySelective(record);
		return new ResultMessage(ResponseEnum.M2000);
	}
}
