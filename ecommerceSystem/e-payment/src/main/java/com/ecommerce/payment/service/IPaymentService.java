package com.ecommerce.payment.service;

import com.ecommerce.payment.domain.vo.PayResultVO;

import java.util.Map;

public interface IPaymentService {

    /**
     * 发起支付宝页面支付，返回支付表单 HTML
     */
    String pay(Long orderNo);

    /**
     * 处理支付宝异步通知（含验签）
     */
    String handleNotify(Map<String, String> params);

    /**
     * 查询支付状态
     */
    PayResultVO queryStatus(Long orderNo);
}
