package com.siceb.shared.converter;

import java.time.Instant;

import com.siceb.shared.UtcDateTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UtcDateTimeConverter implements AttributeConverter<UtcDateTime, Instant> {

    @Override
    public Instant convertToDatabaseColumn(UtcDateTime utcDateTime) {
        return utcDateTime == null ? null : utcDateTime.toInstant();
    }

    @Override
    public UtcDateTime convertToEntityAttribute(Instant dbData) {
        return dbData == null ? null : UtcDateTime.of(dbData);
    }
}
