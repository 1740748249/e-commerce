package com.ecommerce.order.query;

import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.order.enums.OrderStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "订单分页查询条件")
public class OrderPageQuery extends PageQuery {

    @ApiModelProperty("订单状态")
    private OrderStatus status;
}
