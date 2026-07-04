package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "订单金额预览（计算优惠）")
public class OrderPreviewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "订单商品不能为空")
    @ApiModelProperty(value = "订单商品列表", required = true)
    private List<OrderItemDTO> items;

    @ApiModelProperty("使用的优惠券ID（校验是否可用）")
    private Long couponId;
}
