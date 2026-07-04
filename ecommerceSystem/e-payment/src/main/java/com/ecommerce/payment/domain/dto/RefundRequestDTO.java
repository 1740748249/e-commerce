package com.ecommerce.payment.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(value = "退款请求DTO")
public class RefundRequestDTO {

    @NotNull
    @ApiModelProperty(value = "订单号", required = true)
    private Long orderNo;

    @NotNull
    @Min(1)
    @ApiModelProperty(value = "退款金额（分）", required = true)
    private Integer refundAmount;

    @ApiModelProperty(value = "退款原因")
    private String reason;
}
