package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "商家编辑商品")
public class ProductUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商品 SPU 名称")
    private String name;

    @ApiModelProperty("分类ID")
    private Long categoryId;

    @ApiModelProperty("SPU 主图 URL")
    private String image;

    @ApiModelProperty("商品描述")
    private String description;

    @ApiModelProperty("SKU 列表（全量替换）")
    private List<SkuCreateDTO> skus;
}
