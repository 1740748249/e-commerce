package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "下单商品项")
public class OrderItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品SPU ID", required = true)
    private Long productId;

    @NotNull(message = "SKU ID不能为空")
    @ApiModelProperty(value = "SKU ID（无规格商品传0）", required = true)
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @ApiModelProperty(value = "数量", required = true)
    private Integer quantity;
}
