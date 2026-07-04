package com.ecommerce.product.mapper;

import com.ecommerce.api.dto.StockSyncBatchMessage;
import com.ecommerce.product.domain.po.EProductSku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EProductSkuMapper extends BaseMapper<EProductSku> {

    int batchDeductStock(@Param("items") List<StockSyncBatchMessage.SkuItem> items);

    int batchRestoreStock(@Param("items") List<StockSyncBatchMessage.SkuItem> items);
}
