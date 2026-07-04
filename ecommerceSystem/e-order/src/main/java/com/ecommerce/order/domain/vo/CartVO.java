package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "购物车条目")
public class CartVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("购物车记录ID")
    private Long cartItemId;

    @ApiModelProperty("商品SPU ID")
    private Long productId;

    @ApiModelProperty("商品名称")
    private String productName;

    @ApiModelProperty("商品图片")
    private String productImage;

    @ApiModelProperty("单价（分）")
    private Integer price;

    @ApiModelProperty("SKU ID（0=无规格）")
    private Long skuId;

    @ApiModelProperty("规格名称（如 白色 / 256GB）")
    private String skuName;

    @ApiModelProperty("数量")
    private Integer quantity;

    @ApiModelProperty("店铺ID")
    private Long shopId;

    @ApiModelProperty("店铺名称")
    private String shopName;
}
