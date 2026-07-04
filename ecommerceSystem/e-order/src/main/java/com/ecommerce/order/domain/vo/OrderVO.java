package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import com.ecommerce.order.enums.OrderStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(description = "订单信息（列表）")
public class OrderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单内部ID")
    private Long id;

    @ApiModelProperty("订单号（对外展示）")
    private Long orderNo;

    @ApiModelProperty("订单总金额（分）")
    private Integer totalAmount;

    @ApiModelProperty("优惠金额（分）")
    private Integer discountAmount;

    @ApiModelProperty("实付金额（分）")
    private Integer payAmount;

    @ApiModelProperty("状态")
    private OrderStatus status;

    @ApiModelProperty("状态文本")
    private String statusText;

    @ApiModelProperty("收货人姓名（脱敏）")
    private String receiverName;

    @ApiModelProperty("收货人手机号（脱敏）")
    private String receiverPhone;

    @ApiModelProperty("收货地址（脱敏）")
    private String receiverAddr;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("订单商品列表")
    private List<OrderItemVO> items;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
