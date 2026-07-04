package com.ecommerce.order.query;

import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.enums.CouponType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "优惠券分页查询条件")
public class CouponPageQuery extends PageQuery {

    @ApiModelProperty("优惠券状态")
    private CouponStatus status;

    @ApiModelProperty("优惠券类型")
    private CouponType type;

    @ApiModelProperty("搜索关键词（匹配名称）")
    private String keyword;
}
