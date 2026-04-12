package com.siceb.platform.branch;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * DataSource wrapper that injects the current tenant's {@code branch_id}
 * into the PostgreSQL session via {@code SET LOCAL app.branch_id}.
 * <p>
 * {@code SET LOCAL} is transaction-scoped: it applies only within the current
 * transaction and is automatically cleared on commit/rollback, so returned
 * connections in the HikariCP pool carry no stale tenant state.
 * <p>
 * This enables PostgreSQL Row-Level Security policies as defense-in-depth,
 * complementing Hibernate's {@code @TenantId} discriminator filtering.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applyTenant(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applyTenant(conn);
        return conn;
    }

    private void applyTenant(Connection conn) throws SQLException {
        TenantContext.get().ifPresent(branchId -> {
            try (var stmt = conn.createStatement()) {
                stmt.execute("SET LOCAL app.branch_id = '" + branchId + "'");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set tenant context on connection", e);
            }
        });
    }
}
