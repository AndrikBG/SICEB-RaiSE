package com.siceb.shared;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityIdTest {

    @Test
    void generateProducesUuidV7() {
        EntityId id = EntityId.generate();
        assertNotNull(id.value());
        assertEquals(7, id.value().version());
    }

    @Test
    void generatedIdsAreTimeOrdered() throws InterruptedException {
        EntityId first = EntityId.generate();
        Thread.sleep(1); // Force different millisecond timestamp
        EntityId second = EntityId.generate();
        // UUID v7 MSB encodes timestamp — lexicographic order follows time
        assertTrue(first.value().compareTo(second.value()) < 0);
    }

    @Test
    void fromUuid() {
        UUID uuid = UUID.randomUUID();
        EntityId id = EntityId.of(uuid);
        assertEquals(uuid, id.value());
    }

    @Test
    void fromString() {
        String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
        EntityId id = EntityId.of(uuidStr);
        assertEquals(UUID.fromString(uuidStr), id.value());
    }

    @Test
    void invalidStringRejected() {
        assertThrows(IllegalArgumentException.class, () -> EntityId.of("not-a-uuid"));
    }

    @Test
    void uniqueness() {
        Set<EntityId> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(EntityId.generate());
        }
        assertEquals(1000, ids.size());
    }

    @Test
    void equalityByValue() {
        UUID uuid = UUID.randomUUID();
        EntityId a = EntityId.of(uuid);
        EntityId b = EntityId.of(uuid);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void nullRejected() {
        assertThrows(NullPointerException.class, () -> EntityId.of((UUID) null));
    }

    @Test
    void toStringIsUuidFormat() {
        EntityId id = EntityId.of("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", id.toString());
    }
}
