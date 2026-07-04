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
 * 购物车表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_cart")
@ApiModel(value="ECart对象", description="购物车表")
public class ECart implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "商品SPU ID")
    private Long productId;

    @ApiModelProperty(value = "SKU ID（0=无规格的商品）")
    private Long skuId;

    @ApiModelProperty(value = "规格名称")
    private String skuName;

    @ApiModelProperty(value = "数量")
    private Integer quantity;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
