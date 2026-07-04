package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "SKU 修改")
public class SkuUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商家自定义 SKU 编码")
    private String skuCode;

    @ApiModelProperty("规格属性")
    private java.util.List<SpecDTO> specs;

    @NotNull(message = "SKU 售价不能为空")
    @ApiModelProperty(value = "SKU 售价（分）", required = true)
    private Integer price;

    @NotNull(message = "库存不能为空")
    @ApiModelProperty(value = "库存", required = true)
    private Integer stock;

    @ApiModelProperty("SKU 图片 URL")
    private String image;

    @ApiModelProperty("状态: 0=禁用, 1=启用")
    private Integer status;
}
