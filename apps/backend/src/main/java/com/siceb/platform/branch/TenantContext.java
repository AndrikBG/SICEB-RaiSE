package com.siceb.platform.branch;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the current branch (tenant) context.
 * Set by {@link TenantFilter} at the start of each request;
 * read by {@link TenantConnectionInterceptor} to inject into the DB session.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_BRANCH = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID branchId) {
        CURRENT_BRANCH.set(branchId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_BRANCH.get());
    }

    public static UUID require() {
        return get().orElseThrow(() ->
                new IllegalStateException("No branch_id in tenant context — request not tenant-scoped"));
    }

    public static void clear() {
        CURRENT_BRANCH.remove();
    }
}
