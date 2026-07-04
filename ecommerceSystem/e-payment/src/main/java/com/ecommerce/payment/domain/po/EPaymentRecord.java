package com.ecommerce.payment.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.ecommerce.payment.enums.PaymentStatus;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 支付流水记录表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_payment_record")
@ApiModel(value="EPaymentRecord对象", description="支付流水记录表")
public class EPaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "支付宝交易号（唯一）")
    private String payNo;

    @ApiModelProperty(value = "关联订单号")
    private Long orderNo;

    @ApiModelProperty(value = "支付用户ID")
    private Long userId;

    @ApiModelProperty(value = "支付金额（分）")
    private Integer totalAmount;

    @ApiModelProperty(value = "0=待支付, 1=支付成功, 2=已关闭")
    private PaymentStatus status;

    @ApiModelProperty(value = "支付成功时间")
    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
