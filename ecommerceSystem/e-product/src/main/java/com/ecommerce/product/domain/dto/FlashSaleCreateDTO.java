package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "商家报名秒杀")
public class FlashSaleCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "场次ID不能为空")
    @ApiModelProperty(value = "秒杀场次ID", required = true)
    private Long sessionId;

    @NotNull(message = "商品ID不能为空")
    @ApiModelProperty(value = "商品ID", required = true)
    private Long productId;

    @NotNull(message = "秒杀价不能为空")
    @ApiModelProperty(value = "秒杀价（分）", required = true)
    private Integer flashPrice;

    @NotNull(message = "秒杀库存不能为空")
    @ApiModelProperty(value = "秒杀库存", required = true)
    private Integer stock;

    @ApiModelProperty(value = "每人限购数量，默认1")
    private Integer perUserLimit;
}
