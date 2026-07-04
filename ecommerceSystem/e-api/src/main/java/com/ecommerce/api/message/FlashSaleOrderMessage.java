package com.ecommerce.api.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleOrderMessage {
    private Long flashSaleOrderId;
    private Long orderNo;           // 正式订单号（product-service 预生成，避免 async MQ 回传延迟）
    private Long flashSaleId;
    private Long userId;
    private Long productId;
    private Long skuId;
    private Long shopId;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddr;
    private String productName;
    private String productImage;
    private String skuName;
    private Integer quantity;
    private Integer price;
}
