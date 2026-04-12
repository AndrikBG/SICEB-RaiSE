package com.siceb.shared;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Timestamp always stored and transmitted in UTC.
 * Conversion to America/Mexico_City happens exclusively in the presentation layer.
 * Eliminates timezone ambiguity during offline synchronization and DST transitions.
 */
public record UtcDateTime(@JsonValue Instant value) {

    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final ZoneId MEXICO_CITY = ZoneId.of("America/Mexico_City");

    public UtcDateTime {
        Objects.requireNonNull(value, "UtcDateTime value must not be null");
    }

    public static UtcDateTime now() {
        return new UtcDateTime(Instant.now());
    }

    @JsonCreator
    public static UtcDateTime of(Instant instant) {
        return new UtcDateTime(instant);
    }

    public static UtcDateTime of(ZonedDateTime zonedDateTime) {
        return new UtcDateTime(zonedDateTime.toInstant());
    }

    public Instant toInstant() {
        return value;
    }

    public ZonedDateTime toUtcZoned() {
        return value.atZone(UTC);
    }

    public ZonedDateTime toMexicoCity() {
        return value.atZone(MEXICO_CITY);
    }

    public boolean isBefore(UtcDateTime other) {
        return this.value.isBefore(other.value);
    }

    public boolean isAfter(UtcDateTime other) {
        return this.value.isAfter(other.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
