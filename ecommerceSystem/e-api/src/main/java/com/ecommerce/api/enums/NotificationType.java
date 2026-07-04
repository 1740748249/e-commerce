package com.ecommerce.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NotificationType implements BaseEnum {
    NEW_ORDER(0, "新订单"),
    SYSTEM(1, "系统通知"),
    PROMOTION(2, "促销活动");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    NotificationType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NotificationType of(int value) {
        for (NotificationType e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown NotificationType value: " + value);
    }
}
