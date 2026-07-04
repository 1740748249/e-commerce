package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "创建订单（下单）")
public class CreateOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "订单商品不能为空")
    @ApiModelProperty(value = "订单商品列表", required = true)
    private List<OrderItemDTO> items;

    @NotNull(message = "收货地址ID不能为空")
    @ApiModelProperty(value = "收货地址ID", required = true)
    private Long addressId;

    @ApiModelProperty("使用的优惠券ID（仅限未使用且未过期的券）")
    private Long couponId;

    @ApiModelProperty("备注")
    private String remark;
}
