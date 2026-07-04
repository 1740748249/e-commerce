package com.ecommerce.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderBasicDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long orderNo;
    private Long userId;
    private Long flashSaleOrderId;
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer status;
    private String subject;
}
