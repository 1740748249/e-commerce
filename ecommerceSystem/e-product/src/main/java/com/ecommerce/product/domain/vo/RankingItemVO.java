package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "销量排行榜条目")
public class RankingItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("排名")
    private Integer rank;

    @ApiModelProperty("商品ID")
    private Long productId;

    @ApiModelProperty("商品名称")
    private String productName;

    @ApiModelProperty("商品图片")
    private String productImage;

    @ApiModelProperty("最低售价（分）")
    private Integer minPrice;

    @ApiModelProperty("总销量")
    private Integer sales;

    @ApiModelProperty("店铺名称")
    private String shopName;
}
