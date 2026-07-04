package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "SKU 规格属性")
public class SpecDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("规格名（如：颜色）")
    private String name;

    @ApiModelProperty("规格值（如：黑色）")
    private String value;
}
