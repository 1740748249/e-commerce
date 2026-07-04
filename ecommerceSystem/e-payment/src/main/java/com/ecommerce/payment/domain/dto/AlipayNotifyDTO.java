package com.ecommerce.payment.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlipayNotifyDTO {
    private String notifyId;
    private String notifyType;
    private String notifyTime;
    private String signType;
    private String sign;
    private String outTradeNo;
    private String tradeNo;
    private String tradeStatus;
    private BigDecimal totalAmount;
    private String gmtPayment;
    private String gmtCreate;
    private String buyerLogonId;
    private String sellerEmail;
    private String appId;
    private String charset;
    private String version;
}
