package com.ecommerce.product.domain.po;

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
 * 商品分类表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_category")
@ApiModel(value="ECategory对象", description="商品分类表")
public class ECategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "父分类ID（0=顶级分类）")
    private Long parentId;

    @ApiModelProperty(value = "分类名称")
    private String name;

    @ApiModelProperty(value = "分类图标 URL")
    private String icon;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;


}
