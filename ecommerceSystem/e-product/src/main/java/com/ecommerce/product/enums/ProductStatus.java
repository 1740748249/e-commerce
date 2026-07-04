package com.ecommerce.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ecommerce.common.constants.BaseEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ProductStatus implements BaseEnum {
    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    @EnumValue
    @JsonValue
    private final int value;
    private final String desc;

    ProductStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @JsonCreator
    public static ProductStatus of(int value) {
        for (ProductStatus e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("Unknown ProductStatus value: " + value);
    }
}
