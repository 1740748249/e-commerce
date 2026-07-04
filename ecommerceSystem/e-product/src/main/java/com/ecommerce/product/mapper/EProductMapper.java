package com.ecommerce.product.mapper;

import com.ecommerce.api.dto.StockSyncBatchMessage;
import com.ecommerce.product.domain.po.EProduct;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EProductMapper extends BaseMapper<EProduct> {

    int batchDeductTotalStock(@Param("items") List<StockSyncBatchMessage.SkuItem> items);

    int batchRestoreTotalStock(@Param("items") List<StockSyncBatchMessage.SkuItem> items);

    @Select("SELECT id, name, image, min_price, sales, shop_id " +
            "FROM e_product WHERE status = 1 AND deleted = 0 " +
            "ORDER BY sales DESC LIMIT #{limit}")
    List<EProduct> selectTopBySales(@Param("limit") int limit);
}
