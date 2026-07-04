package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "秒杀场次")
public class FlashSessionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("场次ID")
    private Long id;

    @ApiModelProperty("场次名称")
    private String name;

    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("状态: 0=未开始, 1=进行中, 2=已结束")
    private Integer status;

    @ApiModelProperty("状态文本")
    private String statusText;

    @ApiModelProperty("该场次已报名商品数")
    private Integer itemCount;
}
