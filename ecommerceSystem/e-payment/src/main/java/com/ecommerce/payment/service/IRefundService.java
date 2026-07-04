package com.ecommerce.payment.service;

import com.ecommerce.payment.domain.vo.RefundResultVO;

import java.util.Map;

public interface IRefundService {

    /**
     * 发起退款
     */
    RefundResultVO refund(Long orderNo, Integer refundAmount, String reason);

    /**
     * 查询退款状态
     */
    RefundResultVO queryRefund(String outRequestNo);

    /**
     * 处理退款异步通知（含验签）
     */
    String handleRefundNotify(Map<String, String> params);
}
