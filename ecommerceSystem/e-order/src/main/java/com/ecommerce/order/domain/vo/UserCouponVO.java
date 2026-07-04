package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "用户优惠券")
public class UserCouponVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户优惠券记录ID")
    private Long id;

    @ApiModelProperty("优惠券模板ID")
    private Long couponId;

    @ApiModelProperty("优惠券名称")
    private String couponName;

    @ApiModelProperty("类型: 0=满减券, 1=无门槛券")
    private Integer type;

    @ApiModelProperty("使用门槛（分）")
    private Integer threshold;

    @ApiModelProperty("减免金额（分）")
    private Integer reduce;

    @ApiModelProperty("优惠券描述")
    private String description;

    @ApiModelProperty("状态: 0=未使用, 1=已使用, 2=已过期")
    private Integer status;

    @ApiModelProperty("状态文本")
    private String statusText;

    @ApiModelProperty("领取时间")
    private LocalDateTime claimedAt;

    @ApiModelProperty("过期时间")
    private LocalDateTime expireAt;
}
