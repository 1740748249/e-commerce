package com.ecommerce.user.domain.query;

import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.user.enums.UserRole;
import com.ecommerce.user.enums.UserStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "用户分页查询条件")
public class UserPageQuery extends PageQuery {

    @ApiModelProperty("用户状态筛选")
    private UserStatus status;

    @ApiModelProperty("用户角色筛选")
    private UserRole role;

    @ApiModelProperty("搜索关键词（用户名 / 姓名 / 手机号）")
    private String keyword;

    @ApiModelProperty("注册时间起始")
    private LocalDateTime startTime;

    @ApiModelProperty("注册时间截止")
    private LocalDateTime endTime;
}
