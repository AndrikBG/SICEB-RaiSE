package com.siceb.platform.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Resolves the current tenant identifier from {@link TenantContext}.
 * Hibernate calls this on each session open to determine the tenant.
 * <p>
 * Additionally, {@link TenantAwareConnectionProvider} uses the resolved
 * tenant to execute {@code SET LOCAL app.branch_id} for PostgreSQL RLS.
 */
@Component
public class TenantConnectionInterceptor implements CurrentTenantIdentifierResolver<String> {

    static final String DEFAULT_TENANT = "00000000-0000-4000-a000-000000000001";

    @Override
    @NonNull
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.get()
                .map(UUID::toString)
                .orElse(DEFAULT_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /**
     * Sets the PostgreSQL session variable {@code app.branch_id} so that
     * RLS policies can filter by the current tenant. Must be called
     * at transaction start.
     */
    public static void applyToConnection(Connection connection) throws SQLException {
        TenantContext.get().ifPresent(branchId -> {
            try (var stmt = connection.createStatement()) {
                stmt.execute("SET LOCAL app.branch_id = '" + branchId + "'");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set tenant context on DB connection", e);
            }
        });
    }
}
