package com.ecommerce.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "订单统计数据")
public class OrderStatisticsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("总订单数")
    private Long totalOrders;

    @ApiModelProperty("总销售额（分）")
    private Long totalSales;
}
