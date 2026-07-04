package com.ecommerce.order.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "订单金额预览")
public class OrderPreviewVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商品明细")
    private List<PreviewItemVO> items;

    @ApiModelProperty("商品总金额（分）")
    private Integer totalAmount;

    @ApiModelProperty("优惠金额（分）")
    private Integer discountAmount;

    @ApiModelProperty("实付金额（分）")
    private Integer payAmount;
}
