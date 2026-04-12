package com.siceb.shared;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Client-generated key attached to every write command for safe offline replay.
 * The server rejects duplicate keys idempotently — retrying an operation with
 * the same key returns the original result without side effects.
 */
public record IdempotencyKey(@JsonValue String value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "IdempotencyKey value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey must not be blank");
        }
    }

    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }

    @JsonCreator
    public static IdempotencyKey of(String key) {
        return new IdempotencyKey(key);
    }

    @Override
    public String toString() {
        return value;
    }
}
