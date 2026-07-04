package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@ApiModel(description = "申请开店")
public class ShopApplyDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "店铺名称不能为空")
    @ApiModelProperty(value = "店铺名称", required = true)
    private String name;

    @ApiModelProperty("店铺 LOGO URL")
    private String logo;
}
