package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "预览订单商品条目")
public class PreviewItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商品SPU ID")
    private Long productId;

    @ApiModelProperty("SKU ID")
    private Long skuId;

    @ApiModelProperty("商品名称")
    private String productName;

    @ApiModelProperty("规格名称")
    private String skuName;

    @ApiModelProperty("单价（分）")
    private Integer price;

    @ApiModelProperty("数量")
    private Integer quantity;

    @ApiModelProperty("小计（分）")
    private Integer subtotal;
}
