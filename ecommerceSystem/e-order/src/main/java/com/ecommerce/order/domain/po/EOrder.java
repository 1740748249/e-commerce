package com.ecommerce.order.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.order.enums.OrderStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_order")
@ApiModel(value="EOrder对象", description="订单表")
public class EOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "内部自增主键（不对外暴露）")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "订单号（Snowflake生成，对外展示）")
    private Long orderNo;

    @ApiModelProperty(value = "关联的秒杀订单ID（非秒杀订单为NULL）")
    private Long flashSaleOrderId;

    @ApiModelProperty(value = "下单用户ID")
    private Long userId;

    @ApiModelProperty(value = "商家店铺ID")
    private Long shopId;

    @ApiModelProperty(value = "订单总金额（分）")
    private Integer totalAmount;

    @ApiModelProperty(value = "优惠金额（分）")
    private Integer discountAmount;

    @ApiModelProperty(value = "使用的优惠券ID")
    private Long couponId;

    @ApiModelProperty(value = "状态")
    private OrderStatus status;

    @ApiModelProperty(value = "收货地址ID")
    private Long addressId;

    @ApiModelProperty(value = "收货人姓名（快照）")
    private String receiverName;

    @ApiModelProperty(value = "收货人手机号（快照）")
    private String receiverPhone;

    @ApiModelProperty(value = "收货完整地址（快照）")
    private String receiverAddr;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "第三方支付流水号")
    private String payNo;

    @ApiModelProperty(value = "支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty(value = "取消时间")
    private LocalDateTime cancelTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;


}
