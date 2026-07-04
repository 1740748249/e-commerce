package com.ecommerce.order.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CouponStatus implements BaseEnum {
    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    CouponStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static CouponStatus of(int value) {
        for (CouponStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown CouponStatus value: " + value);
    }
}
