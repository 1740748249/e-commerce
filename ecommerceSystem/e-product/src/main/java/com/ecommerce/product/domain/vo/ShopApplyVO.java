package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "开店申请结果")
public class ShopApplyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("店铺ID")
    private Long shopId;

    @ApiModelProperty("店铺名称")
    private String name;

    @ApiModelProperty("店铺 LOGO URL")
    private String logo;

    @ApiModelProperty("审批状态: 0=待审批, 1=已通过, 2=已拒绝")
    private Integer approved;

    @ApiModelProperty("审批状态文本")
    private String approvedText;
}
