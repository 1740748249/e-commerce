package com.ecommerce.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
@ApiModel(description = "修改个人信息")
public class UpdateUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("姓名")
    private String name;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty("手机号（11位，唯一）")
    private String phone;

    @ApiModelProperty("头像 URL")
    private String avatar;

    @ApiModelProperty("店铺 LOGO URL（仅 role=1 商家可修改）")
    private String logo;
}
