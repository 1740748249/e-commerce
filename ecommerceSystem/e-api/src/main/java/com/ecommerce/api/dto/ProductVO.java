package com.ecommerce.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "商品SPU（列表展示）")
public class ProductVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商品ID")
    private Long id;

    @ApiModelProperty("商品名称")
    private String name;

    @ApiModelProperty("分类ID")
    private Long categoryId;

    @ApiModelProperty("分类名称")
    private String categoryName;

    @ApiModelProperty("店铺ID")
    private Long shopId;

    @ApiModelProperty("店铺名称")
    private String shopName;

    @ApiModelProperty("最低SKU售价（分）")
    private Integer minPrice;

    @ApiModelProperty("总库存")
    private Integer totalStock;

    @ApiModelProperty("SPU主图URL")
    private String image;

    @ApiModelProperty("商品描述")
    private String description;

    @ApiModelProperty("状态: 0=下架, 1=上架")
    private Integer status;

    @ApiModelProperty("总销量")
    private Integer sales;

    @ApiModelProperty("SKU列表")
    private List<SkuVO> skus;
}
