package com.siceb.shared.converter;

import java.util.UUID;

import com.siceb.shared.EntityId;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntityIdConverter implements AttributeConverter<EntityId, UUID> {

    @Override
    public UUID convertToDatabaseColumn(EntityId entityId) {
        return entityId == null ? null : entityId.value();
    }

    @Override
    public EntityId convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : EntityId.of(dbData);
    }
}
