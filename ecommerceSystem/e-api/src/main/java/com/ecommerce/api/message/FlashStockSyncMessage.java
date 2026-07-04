package com.ecommerce.api.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashStockSyncMessage {
    private String messageId;
    private Long flashSaleId;
    private Integer quantity;
}
