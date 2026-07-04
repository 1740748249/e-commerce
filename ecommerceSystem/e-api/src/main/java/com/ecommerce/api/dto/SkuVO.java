package com.ecommerce.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "商品SKU")
public class SkuVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("SKU ID")
    private Long id;

    @ApiModelProperty("所属SPU ID")
    private Long productId;

    @ApiModelProperty("SKU编码")
    private String skuCode;

    @ApiModelProperty("规格属性 [{name, value}]")
    private List<SpecVO> specs;

    @ApiModelProperty("SKU售价（分）")
    private Integer price;

    @ApiModelProperty("SKU库存")
    private Integer stock;

    @ApiModelProperty("SKU图片")
    private String image;

    @ApiModelProperty("状态: 0=禁用, 1=启用")
    private Integer status;

    @Data
    @ApiModel(description = "规格键值对")
    public static class SpecVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty("规格名（如颜色）")
        private String name;

        @ApiModelProperty("规格值（如黑色）")
        private String value;
    }
}
