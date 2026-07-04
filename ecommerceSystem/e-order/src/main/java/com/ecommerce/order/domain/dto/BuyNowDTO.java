package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "立即购买（跳过购物车直接下单）")
public class BuyNowDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品SPU ID", required = true)
    private Long productId;

    @NotNull(message = "SKU ID不能为空")
    @ApiModelProperty(value = "SKU ID（无规格传0）", required = true)
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @ApiModelProperty(value = "数量", required = true)
    private Integer quantity;

    @NotNull(message = "收货地址ID不能为空")
    @ApiModelProperty(value = "收货地址ID", required = true)
    private Long addressId;

    @ApiModelProperty("使用的优惠券ID")
    private Long couponId;

    @ApiModelProperty("备注")
    private String remark;
}
