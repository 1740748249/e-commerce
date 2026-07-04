package com.ecommerce.order.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponClaimMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Long couponId;
    /** 有效天数，消费者直接用此值计算过期时间，无需回查 DB */
    private Integer validDays;
}
