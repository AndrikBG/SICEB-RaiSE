package com.siceb.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline-first convention enforcement: entities must use client-generated UUIDs
 * (no auto-increment) and support idempotent replay via {@code IdempotencyKey}.
 * These rules catch violations as entities are added in Phase 2+.
 */
class OfflineConventionsArchTest {

    private static final JavaClasses allClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.siceb");

    @Test
    void jpaIdFieldsMustBeUuidOrEntityId() {
        for (JavaClass javaClass : allClasses) {
            if (!javaClass.isAnnotatedWith("jakarta.persistence.Entity")) continue;

            for (JavaField field : javaClass.getFields()) {
                if (!field.isAnnotatedWith("jakarta.persistence.Id")) continue;

                String fieldType = field.getRawType().getName();
                boolean validType = fieldType.equals(UUID.class.getName())
                        || fieldType.equals("com.siceb.shared.EntityId");
                assertTrue(validType,
                        "Offline convention violation: @Id field '" + field.getName() +
                                "' in " + javaClass.getName() +
                                " must be UUID or EntityId, found " + fieldType +
                                ". Client-generated UUIDs are required for offline-first ID creation.");
            }
        }
    }

    @Test
    void noAutoIncrementStrategies() {
        for (JavaClass javaClass : allClasses) {
            if (!javaClass.isAnnotatedWith("jakarta.persistence.Entity")) continue;

            for (JavaField field : javaClass.getFields()) {
                assertFalse(
                        field.isAnnotatedWith("jakarta.persistence.GeneratedValue"),
                        "Offline convention violation: @GeneratedValue on '" +
                                field.getName() + "' in " + javaClass.getName() +
                                ". Server-generated IDs break offline-first — use EntityId.generate() instead.");
            }
        }
    }

    @Test
    void idempotencyKeyTypeAvailableInSharedKernel() {
        boolean found = false;
        for (JavaClass javaClass : allClasses) {
            if (javaClass.getName().equals("com.siceb.shared.IdempotencyKey")) {
                found = true;
                break;
            }
        }
        assertTrue(found,
                "IdempotencyKey must exist in com.siceb.shared for offline write deduplication.");
    }
}
