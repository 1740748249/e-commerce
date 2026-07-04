package com.ecommerce.api.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateFailedMessage implements Serializable {

    private String messageId;
    private List<SkuItem> items;
    private LocalDateTime createTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuItem implements Serializable {
        private Long skuId;
        private Integer quantity;
    }
}
