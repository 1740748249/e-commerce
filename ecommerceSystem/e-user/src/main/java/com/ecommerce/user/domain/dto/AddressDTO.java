package com.ecommerce.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
@ApiModel(description = "新增/修改收货地址")
public class AddressDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "收货人姓名不能为空")
    @ApiModelProperty(value = "收货人姓名", required = true)
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "收货人手机号（11位）", required = true)
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    @ApiModelProperty(value = "省", required = true)
    private String province;

    @NotBlank(message = "城市不能为空")
    @ApiModelProperty(value = "市", required = true)
    private String city;

    @NotBlank(message = "区/县不能为空")
    @ApiModelProperty(value = "区/县", required = true)
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @ApiModelProperty(value = "详细地址", required = true)
    private String detail;

    @ApiModelProperty("是否默认地址: 0=否, 1=是")
    private Integer isDefault;
}
