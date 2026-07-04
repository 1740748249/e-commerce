package com.ecommerce.user.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@ApiModel(description = "修改密码")
public class PasswordDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "旧密码不能为空")
    @ApiModelProperty(value = "旧密码", required = true)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20位")
    @ApiModelProperty(value = "新密码（6-20位）", required = true)
    private String newPassword;
}
