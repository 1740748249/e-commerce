package com.ecommerce.product.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.product.enums.ApprovalStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_flash_sale")
@ApiModel(value = "EFlashSale对象", description = "秒杀活动表（商家报名 → 管理员审核 → 场次时间到达后生效）")
public class EFlashSale implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属场次ID")
    private Long sessionId;

    @ApiModelProperty(value = "商品ID")
    private Long productId;

    @ApiModelProperty(value = "参与秒杀的SKU ID")
    private Long skuId;

    @ApiModelProperty(value = "报名商家ID")
    private Long shopId;

    @ApiModelProperty(value = "秒杀价（分）")
    private Integer flashPrice;

    @ApiModelProperty(value = "秒杀库存")
    private Integer stock;

    @ApiModelProperty(value = "已秒数量")
    private Integer sold;

    @ApiModelProperty(value = "每人限购数量")
    private Integer perUserLimit;

    @ApiModelProperty(value = "审核状态: 0=待审核, 1=已通过, 2=已拒绝")
    private ApprovalStatus approvalStatus;

    @ApiModelProperty(value = "拒绝原因")
    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
