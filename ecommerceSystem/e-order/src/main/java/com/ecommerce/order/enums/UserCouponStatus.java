package com.ecommerce.order.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserCouponStatus implements BaseEnum {
    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    EXPIRED(2, "已过期");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    UserCouponStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static UserCouponStatus of(int value) {
        for (UserCouponStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown UserCouponStatus value: " + value);
    }
}
