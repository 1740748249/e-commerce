package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "订单商品明细")
public class OrderItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商品SPU ID")
    private Long productId;

    @ApiModelProperty("SKU ID")
    private Long skuId;

    @ApiModelProperty("规格名称快照")
    private String skuName;

    @ApiModelProperty("商品名称快照")
    private String productName;

    @ApiModelProperty("商品图片快照")
    private String productImage;

    @ApiModelProperty("成交单价（分）")
    private Integer price;

    @ApiModelProperty("数量")
    private Integer quantity;
}
