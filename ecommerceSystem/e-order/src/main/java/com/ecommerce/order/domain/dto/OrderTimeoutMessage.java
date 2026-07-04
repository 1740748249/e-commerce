package com.ecommerce.order.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long orderNo;
    /** 商家店铺ID（用于通知） */
    private Long shopId;
    /** 订单金额分（用于通知） */
    private Integer totalAmount;
    /** 需要补偿的 SKU 列表 */
    private List<SkuItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long skuId;
        private Integer quantity;
    }
}
