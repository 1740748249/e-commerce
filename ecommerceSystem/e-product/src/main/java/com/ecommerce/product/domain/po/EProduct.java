package com.ecommerce.product.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.product.enums.ProductStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品 SPU 表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_product")
@ApiModel(value="EProduct对象", description="商品 SPU 表")
public class EProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "商品 SPU 名称（如 iPhone 15）")
    private String name;

    @ApiModelProperty(value = "分类ID")
    private Long categoryId;

    @ApiModelProperty(value = "所属店铺ID")
    private Long shopId;

    @ApiModelProperty(value = "最低 SKU 售价（分），列表展示用")
    private Integer minPrice;

    @ApiModelProperty(value = "总库存（所有 SKU 库存之和）")
    private Integer totalStock;

    @ApiModelProperty(value = "SPU 主图 URL")
    private String image;

    @ApiModelProperty(value = "商品描述")
    private String description;

    @ApiModelProperty(value = "状态: 0=下架, 1=上架")
    private ProductStatus status;

    @ApiModelProperty(value = "总销量（所有 SKU 销量之和）")
    private Integer sales;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;


}
