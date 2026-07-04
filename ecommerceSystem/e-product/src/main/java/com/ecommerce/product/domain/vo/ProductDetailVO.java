package com.ecommerce.product.domain.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "商品详情（含SKU）")
public class ProductDetailVO extends com.ecommerce.api.dto.ProductVO {
}
