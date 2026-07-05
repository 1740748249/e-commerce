package com.ecommerce.product.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleTimeoutMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long flashSaleOrderId;
    private Long flashSaleId;
    private Long orderNo;
    private Long userId;
    private Integer quantity;
}
