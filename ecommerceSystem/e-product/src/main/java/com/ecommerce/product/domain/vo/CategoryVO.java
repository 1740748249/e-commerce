package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "商品分类")
public class CategoryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("分类ID")
    private Long id;

    @ApiModelProperty("分类名称")
    private String name;

    @ApiModelProperty("分类图标")
    private String icon;
}
