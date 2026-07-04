package com.ecommerce.order.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.enums.CouponType;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 优惠券模板表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_coupon")
@ApiModel(value="ECoupon对象", description="优惠券模板表")
public class ECoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "优惠券名称")
    private String name;

    @ApiModelProperty(value = "类型")
    private CouponType type;

    @ApiModelProperty(value = "使用门槛（分），0 表示无门槛")
    private Integer threshold;

    @ApiModelProperty(value = "减免金额（分）")
    private Integer reduce;

    @ApiModelProperty(value = "限制品类ID，NULL 表示全场通用")
    private Long categoryId;

    @ApiModelProperty(value = "有效天数")
    private Integer validDays;

    @ApiModelProperty(value = "每人限领数量")
    private Integer limitPerUser;

    @ApiModelProperty(value = "总发行量")
    private Integer totalCount;

    @ApiModelProperty(value = "已领取量（乐观锁防超发）")
    private Integer claimedCount;

    @ApiModelProperty(value = "状态")
    private CouponStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;


}
