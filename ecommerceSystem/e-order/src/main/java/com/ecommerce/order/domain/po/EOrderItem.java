package com.ecommerce.order.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单商品明细表（快照）
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_order_item")
@ApiModel(value="EOrderItem对象", description="订单商品明细表（快照）")
public class EOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "订单ID")
    private Long orderId;

    @ApiModelProperty(value = "商品SPU ID")
    private Long productId;

    @ApiModelProperty(value = "SKU ID")
    private Long skuId;

    @ApiModelProperty(value = "规格名称快照")
    private String skuName;

    @ApiModelProperty(value = "商品名称快照")
    private String productName;

    @ApiModelProperty(value = "商品图片快照")
    private String productImage;

    @ApiModelProperty(value = "成交单价（分）")
    private Integer price;

    @ApiModelProperty(value = "数量")
    private Integer quantity;

    @ApiModelProperty(value = "店铺ID")
    private Long shopId;

    private LocalDateTime createTime;


}
