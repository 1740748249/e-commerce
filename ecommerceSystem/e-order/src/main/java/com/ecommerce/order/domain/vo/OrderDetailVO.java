package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "订单详情")
public class OrderDetailVO extends OrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("使用的优惠券ID")
    private Long couponId;

    @ApiModelProperty("收货人姓名（完整）")
    private String receiverFullName;

    @ApiModelProperty("收货人手机号（完整）")
    private String receiverFullPhone;

    @ApiModelProperty("收货完整地址")
    private String receiverFullAddr;

    @ApiModelProperty("备注")
    private String remark;
}
