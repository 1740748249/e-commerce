package com.ecommerce.product.domain.query;

import com.ecommerce.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "商品分页查询条件")
public class ProductPageQuery extends PageQuery {

    @ApiModelProperty("搜索关键词")
    private String keyword;

    @ApiModelProperty("分类ID")
    private Long categoryId;

    @ApiModelProperty("排序方式: default / price_asc / price_desc / sales")
    private String sort;
}
