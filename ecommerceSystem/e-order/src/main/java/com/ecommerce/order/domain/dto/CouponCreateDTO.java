package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "创建/更新优惠券")
public class CouponCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "优惠券名称不能为空")
    @ApiModelProperty(value = "优惠券名称", required = true)
    private String name;

    @NotNull(message = "优惠券类型不能为空")
    @ApiModelProperty(value = "类型: 0=满减券, 1=无门槛券", required = true)
    private Integer type;

    @ApiModelProperty("使用门槛（分），0=无门槛")
    private Integer threshold;

    @NotNull(message = "减免金额不能为空")
    @Min(value = 1, message = "减免金额必须大于0")
    @ApiModelProperty(value = "减免金额（分）", required = true)
    private Integer reduce;

    @ApiModelProperty("限制品类ID，null=全场通用")
    private Long categoryId;

    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数必须大于0")
    @ApiModelProperty(value = "有效天数", required = true)
    private Integer validDays;

    @NotNull(message = "每人限领数量不能为空")
    @Min(value = 1, message = "每人限领数量必须大于0")
    @ApiModelProperty(value = "每人限领数量", required = true)
    private Integer limitPerUser;

    @NotNull(message = "总发行量不能为空")
    @Min(value = 1, message = "总发行量必须大于0")
    @ApiModelProperty(value = "总发行量", required = true)
    private Integer totalCount;
}
