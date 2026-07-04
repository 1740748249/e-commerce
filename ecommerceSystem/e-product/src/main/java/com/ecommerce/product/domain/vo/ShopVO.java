package com.ecommerce.product.domain.vo;

import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.enums.ShopStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "店铺信息")
public class ShopVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("店铺ID")
    private Long id;

    @ApiModelProperty("店铺名称")
    private String name;

    @ApiModelProperty("店铺LOGO")
    private String logo;

    @ApiModelProperty("状态: 0=关闭, 1=营业中")
    private ShopStatus status;

    @ApiModelProperty("审批状态: 0=待审批, 1=已通过, 2=已拒绝")
    private ApprovalStatus approved;
}
