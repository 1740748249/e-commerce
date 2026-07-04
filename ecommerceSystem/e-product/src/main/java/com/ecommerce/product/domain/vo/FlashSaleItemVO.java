package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "秒杀商品条目")
public class FlashSaleItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("秒杀活动ID")
    private Long id;

    @ApiModelProperty("商品ID")
    private Long productId;

    @ApiModelProperty("商品名称")
    private String productName;

    @ApiModelProperty("商品图片")
    private String productImage;

    @ApiModelProperty("原价（分）")
    private Integer originalPrice;

    @ApiModelProperty("秒杀价（分）")
    private Integer flashPrice;

    @ApiModelProperty("秒杀库存")
    private Integer stock;

    @ApiModelProperty("已秒数量")
    private Integer sold;

    @ApiModelProperty("商家ID")
    private Long shopId;

    @ApiModelProperty("商家名称")
    private String shopName;

    @ApiModelProperty("秒杀进度（百分比 0-100）")
    private Integer progress;

    @ApiModelProperty("每人限购数量")
    private Integer perUserLimit;
}
