package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "店铺申请记录（管理员端）")
public class ShopApplicationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("店铺ID")
    private Long shopId;

    @ApiModelProperty("店铺名称")
    private String shopName;

    @ApiModelProperty("店铺LOGO")
    private String logo;

    @ApiModelProperty("店主用户ID")
    private Long ownerId;

    @ApiModelProperty("申请人用户名")
    private String userName;

    @ApiModelProperty("申请人手机号")
    private String userPhone;

    @ApiModelProperty("申请时间")
    private LocalDateTime createTime;
}
