package com.ecommerce.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ApprovalStatus implements BaseEnum {
    PENDING(0, "待审批"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    ApprovalStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ApprovalStatus of(int value) {
        for (ApprovalStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown ApprovalStatus value: " + value);
    }
}
