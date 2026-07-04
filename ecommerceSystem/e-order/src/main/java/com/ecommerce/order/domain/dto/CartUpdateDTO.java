package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "修改购物车商品数量")
public class CartUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @ApiModelProperty(value = "新数量", required = true)
    private Integer quantity;
}
