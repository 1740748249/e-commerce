package com.ecommerce.user.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerce.user.enums.UserRole;
import com.ecommerce.user.enums.UserStatus;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_user")
@ApiModel(value="EUser对象", description="用户表")
public class EUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户名（登录用）")
    private String username;

    @ApiModelProperty(value = "BCrypt 加密密码")
    private String password;

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "手机号（第二登录凭证）")
    private String phone;

    @ApiModelProperty(value = "角色: 0=普通用户, 1=商家, 2=管理员")
    private UserRole role;

    @ApiModelProperty(value = "头像 URL")
    private String avatar;

    @ApiModelProperty(value = "账号状态: 0=禁用, 1=正常, 2=注销")
    private UserStatus status;

    @ApiModelProperty(value = "最近登录时间")
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer deleted;


}
