-- V016: Inventory CQRS schema — partitioned tables, materialization trigger, pg_notify, permissions
-- Drivers: US-004, US-005, D-036, D-037, D-040, D-041, D-043, CRN-44
-- Story: S4.2

-- ============================================================
-- 1. Partitioned parent tables
-- ============================================================

CREATE TABLE inventory_items (
    item_id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    branch_id         UUID        NOT NULL,
    sku               VARCHAR(50) NOT NULL,
    name              VARCHAR(200) NOT NULL,
    category          VARCHAR(50) NOT NULL,
    service_id        UUID        NOT NULL,
    current_stock     INTEGER     NOT NULL DEFAULT 0,
    min_threshold     INTEGER     NOT NULL DEFAULT 0,
    unit_of_measure   VARCHAR(20) NOT NULL DEFAULT 'units',
    expiration_date   DATE,
    stock_status      VARCHAR(20) NOT NULL DEFAULT 'OK',
    expiration_status VARCHAR(20) NOT NULL DEFAULT 'OK',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_inventory_items PRIMARY KEY (item_id, branch_id),
    CONSTRAINT chk_stock_status CHECK (stock_status IN ('OK', 'LOW_STOCK', 'OUT_OF_STOCK')),
    CONSTRAINT chk_expiration_status CHECK (expiration_status IN ('OK', 'EXPIRING_SOON', 'EXPIRED')),
    CONSTRAINT chk_current_stock_non_negative CHECK (current_stock >= 0),
    CONSTRAINT chk_min_threshold_non_negative CHECK (min_threshold >= 0)
) PARTITION BY LIST (branch_id);

-- Unique SKU per branch (must include partition key)
CREATE UNIQUE INDEX uq_item_sku_branch ON inventory_items (branch_id, sku);
CREATE INDEX ix_inventory_items_service ON inventory_items (service_id);
CREATE INDEX ix_inventory_items_status ON inventory_items (stock_status);
CREATE INDEX ix_inventory_items_name ON inventory_items (name varchar_pattern_ops);

CREATE TABLE inventory_deltas (
    delta_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    item_id           UUID         NOT NULL,
    branch_id         UUID         NOT NULL,
    delta_type        VARCHAR(20)  NOT NULL,
    quantity_change   INTEGER,
    absolute_quantity INTEGER,
    reason            TEXT,
    source_ref        VARCHAR(200),
    staff_id          UUID         NOT NULL,
    idempotency_key   VARCHAR(64)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_inventory_deltas PRIMARY KEY (delta_id, branch_id),
    CONSTRAINT chk_delta_type CHECK (delta_type IN ('INCREMENT', 'DECREMENT', 'ADJUST', 'THRESHOLD', 'EXPIRATION'))
) PARTITION BY LIST (branch_id);

-- Idempotency key uniqueness (includes branch_id for partitioning constraint;
-- application layer enforces global uniqueness via SELECT before INSERT)
CREATE UNIQUE INDEX uq_delta_idempotency ON inventory_deltas (branch_id, idempotency_key);
CREATE INDEX ix_deltas_item ON inventory_deltas (item_id);
CREATE INDEX ix_deltas_created ON inventory_deltas (created_at);

-- ============================================================
-- 2. Default partition (catches inserts before branch-specific partitions exist)
-- ============================================================

CREATE TABLE inventory_items_default PARTITION OF inventory_items DEFAULT;
CREATE TABLE inventory_deltas_default PARTITION OF inventory_deltas DEFAULT;

-- ============================================================
-- 3. Partition creation function (called by BranchOnboardingOrchestrator step 1)
-- ============================================================

CREATE OR REPLACE FUNCTION create_inventory_partitions(p_branch_id UUID)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    partition_suffix TEXT := replace(p_branch_id::text, '-', '_');
    items_partition TEXT := 'inventory_items_' || partition_suffix;
    deltas_partition TEXT := 'inventory_deltas_' || partition_suffix;
BEGIN
    -- Create partitions only if they don't exist (idempotent)
    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = items_partition) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF inventory_items FOR VALUES IN (%L)',
            items_partition, p_branch_id
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = deltas_partition) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF inventory_deltas FOR VALUES IN (%L)',
            deltas_partition, p_branch_id
        );
    END IF;
END;
$$;

COMMENT ON FUNCTION create_inventory_partitions(UUID) IS
    'Creates branch-specific partitions for inventory_items and inventory_deltas. '
    'Idempotent — safe to call multiple times for the same branch.';

-- ============================================================
-- 4. Stock materialization trigger (D-036, D-041)
-- Runs AFTER INSERT on inventory_deltas within the same transaction.
-- Materializes stock, computes status, fires pg_notify.
-- ============================================================

CREATE OR REPLACE FUNCTION materialize_inventory_delta()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_current_stock   INTEGER;
    v_min_threshold   INTEGER;
    v_new_stock       INTEGER;
    v_new_threshold   INTEGER;
    v_stock_status    VARCHAR(20);
    v_expiration_date DATE;
    v_exp_status      VARCHAR(20);
BEGIN
    -- Lock the item row for concurrency serialization (D-041: SELECT ... FOR UPDATE)
    SELECT current_stock, min_threshold, expiration_date
    INTO v_current_stock, v_min_threshold, v_expiration_date
    FROM inventory_items
    WHERE item_id = NEW.item_id AND branch_id = NEW.branch_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Inventory item not found: %', NEW.item_id
            USING ERRCODE = 'P0002';
    END IF;

    v_new_stock := v_current_stock;
    v_new_threshold := v_min_threshold;

    -- Apply delta by type
    CASE NEW.delta_type
        WHEN 'INCREMENT' THEN
            v_new_stock := v_current_stock + NEW.quantity_change;
        WHEN 'DECREMENT' THEN
            v_new_stock := v_current_stock - NEW.quantity_change;
            IF v_new_stock < 0 THEN
                RAISE EXCEPTION 'Insufficient stock for item %: current=%, requested=%',
                    NEW.item_id, v_current_stock, NEW.quantity_change
                    USING ERRCODE = 'P0001';
            END IF;
        WHEN 'ADJUST' THEN
            v_new_stock := NEW.absolute_quantity;
        WHEN 'THRESHOLD' THEN
            v_new_threshold := NEW.absolute_quantity;
        WHEN 'EXPIRATION' THEN
            -- Expiration date update handled via separate UPDATE below
            NULL;
    END CASE;

    -- Compute stock status
    v_stock_status := CASE
        WHEN v_new_stock = 0 THEN 'OUT_OF_STOCK'
        WHEN v_new_stock < v_new_threshold THEN 'LOW_STOCK'
        ELSE 'OK'
    END;

    -- Compute expiration status
    IF NEW.delta_type = 'EXPIRATION' THEN
        v_expiration_date := NEW.absolute_quantity::text::date;
    END IF;
    v_exp_status := CASE
        WHEN v_expiration_date IS NULL THEN 'OK'
        WHEN v_expiration_date <= CURRENT_DATE THEN 'EXPIRED'
        WHEN v_expiration_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'EXPIRING_SOON'
        ELSE 'OK'
    END;

    -- Update materialized state
    UPDATE inventory_items SET
        current_stock     = v_new_stock,
        min_threshold     = v_new_threshold,
        stock_status      = v_stock_status,
        expiration_date   = v_expiration_date,
        expiration_status = v_exp_status,
        updated_at        = now()
    WHERE item_id = NEW.item_id AND branch_id = NEW.branch_id;

    -- Notify listeners (payload < 8KB — ~250 bytes)
    PERFORM pg_notify('inventory_changes', json_build_object(
        'branchId', NEW.branch_id,
        'itemId', NEW.item_id,
        'deltaType', NEW.delta_type,
        'newStock', v_new_stock,
        'stockStatus', v_stock_status,
        'expirationStatus', v_exp_status,
        'timestamp', extract(epoch from now())
    )::text);

    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION materialize_inventory_delta() IS
    'AFTER INSERT trigger on inventory_deltas. Materializes stock into inventory_items, '
    'computes stock/expiration status, and fires pg_notify for real-time WebSocket push.';

-- Attach trigger to parent table (propagates to all partitions)
CREATE TRIGGER trg_materialize_delta
    AFTER INSERT ON inventory_deltas
    FOR EACH ROW
    EXECUTE FUNCTION materialize_inventory_delta();

-- ============================================================
-- 5. Append-only enforcement on inventory_deltas (AC9)
-- ============================================================

CREATE OR REPLACE FUNCTION prevent_delta_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'inventory_deltas is append-only: UPDATE and DELETE are prohibited'
        USING ERRCODE = 'P0003';
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_prevent_delta_update
    BEFORE UPDATE ON inventory_deltas
    FOR EACH ROW
    EXECUTE FUNCTION prevent_delta_mutation();

CREATE TRIGGER trg_prevent_delta_delete
    BEFORE DELETE ON inventory_deltas
    FOR EACH ROW
    EXECUTE FUNCTION prevent_delta_mutation();

-- ============================================================
-- 6. Permissions seed (inventory:read_all, inventory:read_service, inventory:adjust)
-- ============================================================

INSERT INTO permissions (key, description, category, requires_residency_check) VALUES
    ('inventory:read_all',     'View inventory across all branches',          'inventory', FALSE),
    ('inventory:read_service', 'View inventory scoped to assigned service',   'inventory', FALSE),
    ('inventory:adjust',       'Perform absolute stock adjustments',          'inventory', FALSE);

-- Grant new permissions to appropriate roles
-- Director General + Administrador General: inventory:read_all, inventory:adjust
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('Director General', 'Administrador General')
  AND p.key IN ('inventory:read_all', 'inventory:adjust');

-- Jefe de Servicio: inventory:read_service
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Jefe de Servicio'
  AND p.key = 'inventory:read_service';

-- Médico Adscrito + Residents: inventory:read_service
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('Médico Adscrito', 'Residente R4', 'Residente R3', 'Residente R2', 'Residente R1')
  AND p.key = 'inventory:read_service';

-- Farmacia: inventory:read_service (scoped to pharmacy service)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Farmacia'
  AND p.key = 'inventory:read_service';

-- ============================================================
-- 7. admin_reporting grants on new tables (T4.5.3)
-- ============================================================

GRANT SELECT ON inventory_items TO admin_reporting;
GRANT SELECT ON inventory_deltas TO admin_reporting;
