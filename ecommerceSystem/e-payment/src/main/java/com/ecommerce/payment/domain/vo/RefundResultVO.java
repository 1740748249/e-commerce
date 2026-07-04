package com.ecommerce.payment.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResultVO {
    private Long orderNo;
    private String refundNo;
    private String outRequestNo;
    private Integer refundAmount;
    private Integer status;

    @ApiModelProperty(value = "状态文本: 处理中 / 退款成功 / 退款失败")
    private String statusText;

    private LocalDateTime refundTime;
}
