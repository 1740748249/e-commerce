package com.ecommerce.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FlashSaleOrderStatus implements BaseEnum {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    REFUNDED(3, "已退款"),
    PAYMENT_TIMEOUT(4, "支付超时");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    FlashSaleOrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static FlashSaleOrderStatus of(int value) {
        for (FlashSaleOrderStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown FlashSaleOrderStatus value: " + value);
    }
}
