package com.ecommerce.payment.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RefundStatus {
    PROCESSING(0, "处理中"),
    SUCCESS(1, "退款成功"),
    FAILED(2, "退款失败");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    RefundStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static RefundStatus of(int value) {
        for (RefundStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown RefundStatus value: " + value);
    }
}
