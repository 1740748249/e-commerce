package com.ecommerce.payment.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING(0, "待支付"),
    SUCCESS(1, "支付成功"),
    CLOSED(2, "已关闭");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    PaymentStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static PaymentStatus of(int value) {
        for (PaymentStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown PaymentStatus value: " + value);
    }
}
