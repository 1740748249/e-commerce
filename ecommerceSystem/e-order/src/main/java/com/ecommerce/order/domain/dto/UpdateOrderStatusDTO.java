package com.ecommerce.order.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "修改订单状态")
public class UpdateOrderStatusDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "状态不能为空")
    @ApiModelProperty(value = "订单状态: 2=发货, 3=完成", required = true)
    private Integer status;
}
