package com.ecommerce.product.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.enums.ShopStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 店铺表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_shop")
@ApiModel(value="EShop对象", description="店铺表")
public class EShop implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "店铺名称")
    private String name;

    @ApiModelProperty(value = "店铺 LOGO")
    private String logo;

    @ApiModelProperty(value = "状态: 0=关闭, 1=营业中")
    private ShopStatus status;

    @ApiModelProperty(value = "店主用户ID")
    private Long ownerId;

    @ApiModelProperty(value = "审批状态: 0=待审批, 1=已通过, 2=已拒绝")
    private ApprovalStatus approved;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
