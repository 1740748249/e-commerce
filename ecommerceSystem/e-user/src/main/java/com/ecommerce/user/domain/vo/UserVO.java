package com.ecommerce.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "用户信息")
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("姓名")
    private String name;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("角色: 0=普通用户, 1=商家, 2=管理员")
    private Integer role;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("账号状态: 0=禁用, 1=正常, 2=注销")
    private Integer status;

    @ApiModelProperty("最近登录时间")
    private LocalDateTime lastLoginTime;

    @ApiModelProperty("默认收货地址")
    private String addr;
}
