package com.ecommerce.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus implements BaseEnum {
    DISABLED(0, "禁用"),
    ACTIVE(1, "正常"),
    DELETED(2, "注销");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static UserStatus of(int value) {
        for (UserStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown UserStatus value: " + value);
    }
}
