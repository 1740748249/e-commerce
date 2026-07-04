package com.ecommerce.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum FlashSessionStatus implements BaseEnum {
    UPCOMING(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    FlashSessionStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FlashSessionStatus of(int value) {
        for (FlashSessionStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown FlashSessionStatus value: " + value);
    }
}
