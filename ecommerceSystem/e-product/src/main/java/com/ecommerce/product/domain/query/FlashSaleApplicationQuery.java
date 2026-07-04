package com.ecommerce.product.domain.query;

import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.enums.ApprovalStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "秒杀报名分页查询条件")
public class FlashSaleApplicationQuery extends PageQuery {

    @ApiModelProperty("场次ID筛选")
    private Long sessionId;

    @ApiModelProperty("审核状态筛选：0=待审核, 1=已通过, 2=已拒绝")
    private ApprovalStatus approvalStatus;
}
