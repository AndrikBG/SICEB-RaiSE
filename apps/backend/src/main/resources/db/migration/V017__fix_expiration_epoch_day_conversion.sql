-- V017: Fix EXPIRATION delta date conversion in materialization trigger.
-- absolute_quantity stores epoch day (days since 1970-01-01), not a date string.
-- The cast NEW.absolute_quantity::text::date fails for epoch day integers like 20999.
-- Fix: use date arithmetic — '1970-01-01'::date + epoch_day_integer.

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
        -- absolute_quantity carries epoch day (days since 1970-01-01)
        v_expiration_date := '1970-01-01'::date + NEW.absolute_quantity;
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
