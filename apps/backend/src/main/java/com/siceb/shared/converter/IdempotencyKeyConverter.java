package com.siceb.shared.converter;

import com.siceb.shared.IdempotencyKey;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IdempotencyKeyConverter implements AttributeConverter<IdempotencyKey, String> {

    @Override
    public String convertToDatabaseColumn(IdempotencyKey key) {
        return key == null ? null : key.value();
    }

    @Override
    public IdempotencyKey convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IdempotencyKey.of(dbData);
    }
}
