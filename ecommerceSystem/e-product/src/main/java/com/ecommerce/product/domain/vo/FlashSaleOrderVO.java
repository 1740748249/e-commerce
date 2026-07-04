package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "秒杀订单")
public class FlashSaleOrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("秒杀订单ID")
    private Long id;

    @ApiModelProperty("正式订单号")
    private Long orderNo;

    @ApiModelProperty("秒杀活动ID")
    private Long flashSaleId;

    @ApiModelProperty("商品名称")
    private String productName;

    @ApiModelProperty("购买数量")
    private Integer quantity;

    @ApiModelProperty("成交价（分）")
    private Integer price;

    @ApiModelProperty("状态: 0=待支付, 1=已支付, 2=已取消, 3=已退款, 4=支付超时")
    private Integer status;

    @ApiModelProperty("状态文本")
    private String statusText;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("取消时间")
    private LocalDateTime cancelTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
