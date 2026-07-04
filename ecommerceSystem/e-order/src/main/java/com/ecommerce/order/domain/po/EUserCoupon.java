package com.ecommerce.order.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.order.enums.UserCouponStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户优惠券表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_user_coupon")
@ApiModel(value="EUserCoupon对象", description="用户优惠券表")
public class EUserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "优惠券ID")
    private Long couponId;

    @ApiModelProperty(value = "状态")
    private UserCouponStatus status;

    @ApiModelProperty(value = "领取时间")
    private LocalDateTime claimedAt;

    @ApiModelProperty(value = "过期时间")
    private LocalDateTime expireAt;

    @ApiModelProperty(value = "使用时间")
    private LocalDateTime usedAt;

    @ApiModelProperty(value = "使用的订单号")
    private Long orderNo;


}
