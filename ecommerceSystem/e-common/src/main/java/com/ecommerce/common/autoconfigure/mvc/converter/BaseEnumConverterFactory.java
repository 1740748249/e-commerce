package com.ecommerce.common.autoconfigure.mvc.converter;

import com.ecommerce.common.constants.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

import java.util.HashMap;
import java.util.Map;

public class BaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    private final Map<Class<?>, Converter<String, ?>> cache = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return (Converter<String, T>) cache.computeIfAbsent(targetType, k -> new BaseEnumConverter<>(targetType));
    }

    private static class BaseEnumConverter<T extends BaseEnum> implements Converter<String, T> {
        private final Map<Integer, T> valueMap = new HashMap<>();

        BaseEnumConverter(Class<T> enumClass) {
            for (T constant : enumClass.getEnumConstants()) {
                valueMap.put(constant.getValue(), constant);
            }
        }

        @Override
        public T convert(String source) {
            T result = valueMap.get(Integer.valueOf(source));
            if (result == null) {
                throw new IllegalArgumentException("Unknown value " + source + " for " + valueMap.values().iterator().next().getClass().getSimpleName());
            }
            return result;
        }
    }
}
