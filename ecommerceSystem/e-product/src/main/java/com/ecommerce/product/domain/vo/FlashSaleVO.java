package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(description = "秒杀列表（公开）")
public class FlashSaleVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("场次ID")
    private Long id;

    @ApiModelProperty("场次名称")
    private String name;

    @ApiModelProperty("场次开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("场次结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("秒杀商品列表")
    private List<FlashSaleItemVO> items;
}
