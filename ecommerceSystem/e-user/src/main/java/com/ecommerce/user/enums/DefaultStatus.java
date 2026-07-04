package com.ecommerce.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum DefaultStatus implements BaseEnum {
    NO(0, "否"),
    YES(1, "是");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    DefaultStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static DefaultStatus of(int value) {
        for (DefaultStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown DefaultStatus value: " + value);
    }
}
