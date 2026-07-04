package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "商家发布商品")
public class ProductCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商品名称不能为空")
    @ApiModelProperty(value = "商品 SPU 名称", required = true)
    private String name;

    @NotNull(message = "分类ID不能为空")
    @ApiModelProperty(value = "分类ID", required = true)
    private Long categoryId;

    @ApiModelProperty("SPU 主图 URL")
    private String image;

    @ApiModelProperty("商品描述")
    private String description;

    @ApiModelProperty("SKU 列表（无规格商品至少传一个）")
    private List<SkuCreateDTO> skus;
}
