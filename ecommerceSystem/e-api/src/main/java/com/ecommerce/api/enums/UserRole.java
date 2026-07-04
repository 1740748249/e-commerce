package com.ecommerce.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserRole implements BaseEnum {
    USER(0, "普通用户"),
    VENDOR(1, "商家"),
    ADMIN(2, "管理员");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    UserRole(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserRole of(int value) {
        for (UserRole e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown UserRole value: " + value);
    }
}
