package com.ecommerce.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "登录响应")
public class LoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("JWT令牌")
    private String token;

    @ApiModelProperty("用户信息")
    private UserVO user;
}
