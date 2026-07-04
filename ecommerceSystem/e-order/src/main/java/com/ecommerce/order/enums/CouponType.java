package com.ecommerce.order.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum CouponType implements BaseEnum {
    FULL_REDUCTION(0, "满减券"),
    NO_THRESHOLD(1, "无门槛券");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    CouponType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static CouponType of(int value) {
        for (CouponType e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown CouponType value: " + value);
    }
}
