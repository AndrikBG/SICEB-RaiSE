package com.siceb.shared;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyKeyTest {

    @Test
    void generateProducesNonBlankKey() {
        IdempotencyKey key = IdempotencyKey.generate();
        assertNotNull(key.value());
        assertFalse(key.value().isBlank());
    }

    @Test
    void fromString() {
        String raw = "custom-idempotency-key-123";
        IdempotencyKey key = IdempotencyKey.of(raw);
        assertEquals(raw, key.value());
    }

    @Test
    void uniqueness() {
        Set<IdempotencyKey> keys = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            keys.add(IdempotencyKey.generate());
        }
        assertEquals(1000, keys.size());
    }

    @Test
    void equalityByValue() {
        IdempotencyKey a = IdempotencyKey.of("same-key");
        IdempotencyKey b = IdempotencyKey.of("same-key");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentKeysNotEqual() {
        IdempotencyKey a = IdempotencyKey.generate();
        IdempotencyKey b = IdempotencyKey.generate();
        assertNotEquals(a, b);
    }

    @Test
    void nullRejected() {
        assertThrows(NullPointerException.class, () -> IdempotencyKey.of(null));
    }

    @Test
    void blankRejected() {
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of(""));
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of("   "));
    }

    @Test
    void toStringIsRawValue() {
        IdempotencyKey key = IdempotencyKey.of("my-key");
        assertEquals("my-key", key.toString());
    }
}
