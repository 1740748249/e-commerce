package com.ecommerce.product.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.product.enums.SkuStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品 SKU 表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_product_sku")
@ApiModel(value="EProductSku对象", description="商品 SKU 表")
public class EProductSku implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属 SPU ID")
    private Long productId;

    @ApiModelProperty(value = "SKU 编码（商家自定义）")
    @TableField()
    private String skuCode;

    @ApiModelProperty(value = "规格属性 [{'name':'颜色','value':'深空黑'}]")
    private String specs;

    @ApiModelProperty(value = "SKU 售价（分）")
    private Integer price;

    @ApiModelProperty(value = "SKU 库存")
    private Integer stock;

    @ApiModelProperty(value = "SKU 图片（可覆盖 SPU 主图）")
    private String image;

    @ApiModelProperty(value = "状态: 0=禁用, 1=启用")
    private SkuStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;


}
