package com.ecommerce.user.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "收货地址")
public class AddressVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("地址ID")
    private Long id;

    @ApiModelProperty("收货人姓名")
    private String receiverName;

    @ApiModelProperty("收货人手机号")
    private String receiverPhone;

    @ApiModelProperty("省")
    private String province;

    @ApiModelProperty("市")
    private String city;

    @ApiModelProperty("区/县")
    private String district;

    @ApiModelProperty("详细地址")
    private String detail;

    @ApiModelProperty("是否默认: 0=否, 1=是")
    private Integer isDefault;
}
