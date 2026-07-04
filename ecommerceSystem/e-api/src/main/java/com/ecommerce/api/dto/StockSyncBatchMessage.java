package com.ecommerce.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSyncBatchMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String messageId;
    private List<SkuItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long skuId;
        private Integer quantity;
    }
}
