package com.ecommerce.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SkuStatus implements BaseEnum {
    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    SkuStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static SkuStatus of(int value) {
        for (SkuStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown SkuStatus value: " + value);
    }
}
