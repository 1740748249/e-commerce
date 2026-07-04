package com.ecommerce.product.domain.vo;

import com.ecommerce.product.enums.ApprovalStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "秒杀报名记录（管理员审核列表 & 商家报名记录）")
public class FlashSaleApplicationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("秒杀活动ID")
    private Long id;

    @ApiModelProperty("场次ID")
    private Long sessionId;

    @ApiModelProperty("场次名称")
    private String sessionName;

    @ApiModelProperty("商品ID")
    private Long productId;

    @ApiModelProperty("商品名称")
    private String productName;

    @ApiModelProperty("商品图片")
    private String productImage;

    @ApiModelProperty("商家ID")
    private Long shopId;

    @ApiModelProperty("商家名称")
    private String shopName;

    @ApiModelProperty("秒杀价（分）")
    private Integer flashPrice;

    @ApiModelProperty("原价（分）")
    private Integer originalPrice;

    @ApiModelProperty("秒杀库存")
    private Integer stock;

    @ApiModelProperty("已秒数量")
    private Integer sold;

    @ApiModelProperty("每人限购数量")
    private Integer perUserLimit;

    @ApiModelProperty("审核状态")
    private ApprovalStatus approvalStatus;

    @ApiModelProperty("审核状态文本")
    private String approvalStatusText;

    @ApiModelProperty("拒绝原因")
    private String rejectReason;

    @ApiModelProperty("报名时间")
    private LocalDateTime createTime;
}
