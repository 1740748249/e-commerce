package com.ecommerce.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ShopStatus implements BaseEnum {
    CLOSED(0, "关闭"),
    OPEN(1, "营业中");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    ShopStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ShopStatus of(int value) {
        for (ShopStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown ShopStatus value: " + value);
    }
}
