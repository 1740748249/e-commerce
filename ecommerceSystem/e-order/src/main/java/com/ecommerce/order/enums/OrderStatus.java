package com.ecommerce.order.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum OrderStatus implements BaseEnum {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    OrderStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static OrderStatus of(int value) {
        for (OrderStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown OrderStatus value: " + value);
    }
}
