package com.siceb.platform.iam;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the current staff member's identity for the request.
 * In Phase 2, populated from X-Staff-Id header (dev).
 * In Phase 3, populated from JWT claims.
 */
public final class StaffContext {

    private static final ThreadLocal<UUID> CURRENT_STAFF = new ThreadLocal<>();

    private StaffContext() {}

    public static void set(UUID staffId) {
        CURRENT_STAFF.set(staffId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_STAFF.get());
    }

    public static UUID require() {
        return get().orElseThrow(() ->
                new IllegalStateException("Staff context not set — X-Staff-Id header or JWT required"));
    }

    public static void clear() {
        CURRENT_STAFF.remove();
    }
}
