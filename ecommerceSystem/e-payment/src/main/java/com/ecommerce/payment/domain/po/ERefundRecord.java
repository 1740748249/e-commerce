package com.ecommerce.payment.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.ecommerce.payment.enums.RefundStatus;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 退款流水记录表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_refund_record")
@ApiModel(value="ERefundRecord对象", description="退款流水记录表")
public class ERefundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "支付宝退款交易号")
    private String refundNo;

    @ApiModelProperty(value = "商户退款请求号（雪花ID，幂等关键）")
    private String outRequestNo;

    @ApiModelProperty(value = "关联支付流水号")
    private String payNo;

    @ApiModelProperty(value = "关联订单号")
    private Long orderNo;

    @ApiModelProperty(value = "退款金额（分）")
    private Integer refundAmount;

    @ApiModelProperty(value = "退款原因")
    private String reason;

    @ApiModelProperty(value = "0=处理中, 1=退款成功, 2=退款失败")
    private RefundStatus status;

    @ApiModelProperty(value = "退款到账时间")
    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
