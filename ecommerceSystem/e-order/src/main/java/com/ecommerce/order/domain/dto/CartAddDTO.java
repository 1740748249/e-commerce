package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "添加商品到购物车")
public class CartAddDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品SPU ID", required = true)
    private Long productId;

    @ApiModelProperty("SKU ID（有规格时必填，无规格传0或不传）")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @ApiModelProperty(value = "数量", required = true)
    private Integer quantity;
}
