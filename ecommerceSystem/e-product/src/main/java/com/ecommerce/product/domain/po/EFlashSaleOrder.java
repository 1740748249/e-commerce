package com.ecommerce.product.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.product.enums.FlashSaleOrderStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 秒杀订单记录表（防刷）
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_flash_sale_order")
@ApiModel(value="EFlashSaleOrder对象", description="秒杀订单记录表（防刷）")
public class EFlashSaleOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "参与秒杀的用户ID")
    private Long userId;

    @ApiModelProperty(value = "秒杀活动ID")
    private Long flashSaleId;

    @ApiModelProperty(value = "关联的正式订单号")
    private Long orderNo;

    @ApiModelProperty(value = "商品ID")
    private Long productId;

    @ApiModelProperty(value = "购买数量")
    private Integer quantity;

    @ApiModelProperty(value = "秒杀成交价（分）")
    private Integer price;

    @ApiModelProperty(value = "状态: 0=待支付, 1=已支付, 2=已取消, 3=已退款, 4=支付超时")
    private FlashSaleOrderStatus status;

    @ApiModelProperty(value = "支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty(value = "取消时间")
    private LocalDateTime cancelTime;

    private LocalDateTime createTime;


}
