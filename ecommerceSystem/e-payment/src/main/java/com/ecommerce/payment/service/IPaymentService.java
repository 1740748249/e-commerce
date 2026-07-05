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

    /**
     * 主动查支付宝并同步订单状态（供 order-service 超时取消前调用）
     * @return true=支付宝已支付且已同步回调，false=支付宝未支付或查单失败
     */
    boolean checkAndSyncByOrderNo(Long orderNo);
}
