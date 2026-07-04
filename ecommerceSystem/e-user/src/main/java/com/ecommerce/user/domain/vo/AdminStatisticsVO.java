package com.ecommerce.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "管理员统计概览")
public class AdminStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("总用户数")
    private Long totalUsers;

    @ApiModelProperty("总商家数")
    private Long totalMerchants;

    @ApiModelProperty("待审批商家数")
    private Long pendingMerchants;

    @ApiModelProperty("总订单数")
    private Long totalOrders;

    @ApiModelProperty("总销售额（分）")
    private Long totalSales;
}
