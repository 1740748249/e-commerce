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
public class PayResultVO {
    private Long orderNo;
    private String payNo;
    private Integer totalAmount;
    private Integer status;

    @ApiModelProperty(value = "状态文本: 待支付 / 支付成功 / 已关闭")
    private String statusText;

    private LocalDateTime payTime;
}
