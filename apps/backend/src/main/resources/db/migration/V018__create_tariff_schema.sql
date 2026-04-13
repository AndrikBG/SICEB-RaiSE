-- V018: Tariff configuration schema — temporal effective-date pattern with immutable history
-- Drivers: US-064, D-042, D-043, CRN-42
-- Story: S4.3

-- ============================================================
-- 1. Service tariffs table (NOT partitioned — low data volume)
-- ============================================================

CREATE TABLE service_tariffs (
    tariff_id      UUID          NOT NULL DEFAULT gen_random_uuid(),
    service_id     UUID          NOT NULL REFERENCES branch_service_catalog(id),
    branch_id      UUID          NOT NULL REFERENCES branches(id),
    base_price     DECIMAL(19,4) NOT NULL,
    effective_from TIMESTAMPTZ   NOT NULL,
    created_by     UUID          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_service_tariffs PRIMARY KEY (tariff_id),
    CONSTRAINT uq_tariff_service_branch_effective UNIQUE (service_id, branch_id, effective_from),
    CONSTRAINT chk_base_price_non_negative CHECK (base_price >= 0)
);

CREATE INDEX ix_tariff_service_branch ON service_tariffs (service_id, branch_id);
CREATE INDEX ix_tariff_effective_from ON service_tariffs (effective_from);
CREATE INDEX ix_tariff_branch ON service_tariffs (branch_id);

-- ============================================================
-- 2. Permissions seed (tariff:read — tariff:manage already in V010)
-- ============================================================

INSERT INTO permissions (key, description, category, requires_residency_check) VALUES
    ('tariff:read', 'View tariff catalog', 'admin', FALSE);

-- Grant tariff:read to roles that interact with billing or branch info
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE p.key = 'tariff:read'
  AND r.name IN ('Director General', 'Administrador General', 'Jefe de Servicio', 'Recepción');

-- Grant tariff:manage to Recepción (handles billing at front desk)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Recepción'
  AND p.key = 'tariff:manage'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id
  );

-- ============================================================
-- 3. admin_reporting grants
-- ============================================================

GRANT SELECT ON service_tariffs TO admin_reporting;
