package com.ecommerce.order.domain.vo;

import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.enums.CouponType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "优惠券模板")
public class CouponVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("优惠券ID")
    private Long id;

    @ApiModelProperty("优惠券名称")
    private String name;

    @ApiModelProperty("类型: 0=满减券, 1=无门槛券")
    private CouponType type;

    @ApiModelProperty("使用门槛（分），0=无门槛")
    private Integer threshold;

    @ApiModelProperty("减免金额（分）")
    private Integer reduce;

    @ApiModelProperty("优惠券描述")
    private String description;

    @ApiModelProperty("有效天数")
    private Integer validDays;

    @ApiModelProperty("每人限领数量")
    private Integer limitPerUser;

    @ApiModelProperty("限制品类ID（null=全场通用）")
    private Long categoryId;

    @ApiModelProperty("已领取量")
    private Integer claimedCount;

    @ApiModelProperty("总发行量")
    private Integer totalCount;

    @ApiModelProperty("状态: 0=停用, 1=启用")
    private CouponStatus status;
}
