package com.siceb.shared;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * UUID v7 entity identifier (RFC 9562). Time-ordered for index locality
 * and sync ordering (CRN-38). No auto-increment sequences — IDs can be
 * generated offline on the client and remain globally unique without
 * server coordination.
 */
public record EntityId(@JsonValue UUID value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public EntityId {
        Objects.requireNonNull(value, "EntityId value must not be null");
    }

    /**
     * Generates a UUID v7 per RFC 9562:
     * - Bits 0-47: Unix timestamp in milliseconds
     * - Bits 48-51: version (0111 = 7)
     * - Bits 52-63: random
     * - Bits 64-65: variant (10)
     * - Bits 66-127: random
     */
    public static EntityId generate() {
        long timestamp = System.currentTimeMillis();
        long random = RANDOM.nextLong();

        long msb = (timestamp << 16) | (0x7000L) | (random & 0x0FFFL);
        long lsb = (0x8000000000000000L) | (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL);

        return new EntityId(new UUID(msb, lsb));
    }

    @JsonCreator
    public static EntityId of(UUID uuid) {
        return new EntityId(uuid);
    }

    public static EntityId of(String uuid) {
        return new EntityId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
