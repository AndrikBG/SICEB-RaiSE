-- Scalability test data seeding script
-- Usage: psql -v branch_count=15 -v patients_per_branch=3334 -f seed-branches.sql
-- Defaults: 15 branches, ~3334 patients/branch (~50K total)
--
-- Requires: Flyway migrations already applied (branches, inventory schema, patient_search_view)
-- Runs as DB owner (bypasses RLS).

\set ON_ERROR_STOP on

-- Default parameters (overridable via -v)
SELECT coalesce(:'branch_count', '15')::int AS branch_count \gset
SELECT coalesce(:'patients_per_branch', '3334')::int AS patients_per_branch \gset
SELECT coalesce(:'items_per_branch', '1000')::int AS items_per_branch \gset
SELECT coalesce(:'deltas_per_item', '5')::int AS deltas_per_item \gset

-- Clean previous test data (idempotent re-runs)
-- Drop branch-specific partitions to avoid append-only trigger on deltas
DO $$
DECLARE
    b_rec RECORD;
    suffix TEXT;
BEGIN
    FOR b_rec IN SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%' LOOP
        suffix := replace(b_rec.id::text, '-', '_');
        EXECUTE format('DROP TABLE IF EXISTS inventory_deltas_%s', suffix);
        EXECUTE format('DROP TABLE IF EXISTS inventory_items_%s', suffix);
    END LOOP;
END $$;
DELETE FROM service_tariffs WHERE branch_id IN (
    SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%'
);
DELETE FROM patient_search_view WHERE branch_id IN (
    SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%'
);
DELETE FROM branch_service_catalog WHERE branch_id IN (
    SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%'
);
DELETE FROM branch_onboarding_status WHERE branch_id IN (
    SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%'
);
DELETE FROM branches WHERE name LIKE 'ScalTest-Branch-%';

-- Seed branches, partitions, services, inventory, patients
DO $$
DECLARE
    b_id UUID;
    svc_id UUID;
    item_id UUID;
    b_idx INT;
    i INT;
    j INT;
    categories TEXT[] := ARRAY['MEDICAMENTO', 'INSUMO', 'EQUIPO', 'REACTIVO', 'MATERIAL'];
    names_prefix TEXT[] := ARRAY['Paracetamol', 'Ibuprofeno', 'Amoxicilina', 'Omeprazol', 'Metformina',
                                  'Jeringa', 'Guantes', 'Gasas', 'Vendaje', 'Algodón',
                                  'Termómetro', 'Oxímetro', 'Baumanómetro', 'Estetoscopio', 'Nebulizador'];
    first_names TEXT[] := ARRAY['María', 'José', 'Juan', 'Ana', 'Carlos', 'Laura', 'Pedro', 'Rosa',
                                 'Miguel', 'Carmen', 'Luis', 'Elena', 'Fernando', 'Patricia', 'Ricardo',
                                 'Gabriela', 'Diego', 'Sofía', 'Alejandro', 'Valentina'];
    last_names TEXT[] := ARRAY['García', 'Hernández', 'López', 'Martínez', 'González', 'Rodríguez',
                                'Pérez', 'Sánchez', 'Ramírez', 'Torres', 'Flores', 'Rivera',
                                'Gómez', 'Díaz', 'Cruz', 'Morales', 'Reyes', 'Gutiérrez',
                                'Ortiz', 'Ramos'];
    v_branch_count INT := :'branch_count';
    v_patients_per_branch INT := :'patients_per_branch';
    v_items_per_branch INT := :'items_per_branch';
    v_deltas_per_item INT := :'deltas_per_item';
    staff_id UUID := gen_random_uuid();
BEGIN
    FOR b_idx IN 1..v_branch_count LOOP
        b_id := gen_random_uuid();

        -- 1. Create branch
        INSERT INTO branches (id, name, address, is_active, onboarding_complete, branch_code)
        VALUES (b_id, 'ScalTest-Branch-' || b_idx,
                'Calle Test ' || b_idx || ', Col. Pruebas',
                true, true, 'ST' || lpad(b_idx::text, 3, '0'));

        -- 2. Create inventory partitions
        PERFORM create_inventory_partitions(b_id);

        -- 3. Create service catalog entry
        svc_id := gen_random_uuid();
        INSERT INTO branch_service_catalog (id, branch_id, service_name, service_code, is_active)
        VALUES (svc_id, b_id, 'Consulta General Branch ' || b_idx, 'CG' || b_idx, true);

        -- 4. Seed inventory items
        FOR i IN 1..v_items_per_branch LOOP
            item_id := gen_random_uuid();
            INSERT INTO inventory_items (
                item_id, branch_id, sku, name, category, service_id,
                current_stock, min_threshold, unit_of_measure,
                expiration_date, stock_status, expiration_status
            ) VALUES (
                item_id, b_id,
                'SKU-' || b_idx || '-' || lpad(i::text, 5, '0'),
                names_prefix[1 + (i % array_length(names_prefix, 1))] || ' ' || i,
                categories[1 + (i % array_length(categories, 1))],
                svc_id,
                50 + (i % 200),
                10,
                'units',
                CURRENT_DATE + (30 + (i % 365)),
                CASE WHEN (50 + (i % 200)) <= 10 THEN 'LOW_STOCK' ELSE 'OK' END,
                'OK'
            );

            -- 5. Seed deltas per item
            FOR j IN 1..v_deltas_per_item LOOP
                INSERT INTO inventory_deltas (
                    delta_id, item_id, branch_id, delta_type,
                    quantity_change, reason, staff_id,
                    idempotency_key, created_at
                ) VALUES (
                    gen_random_uuid(), item_id, b_id, 'INCREMENT',
                    10 + (j % 50),
                    'Scalability test seed',
                    staff_id,
                    'scal-' || b_idx || '-' || i || '-' || j,
                    now() - ((v_deltas_per_item - j) || ' hours')::interval
                );
            END LOOP;
        END LOOP;

        -- 6. Seed tariffs
        INSERT INTO service_tariffs (tariff_id, service_id, branch_id, base_price, effective_from, created_by, created_at)
        VALUES (gen_random_uuid(), svc_id, b_id, 150.0000 + (b_idx * 10),
                '2026-01-01T00:00:00Z', staff_id, now());

        -- 7. Seed patients for patient_search_view
        FOR i IN 1..v_patients_per_branch LOOP
            INSERT INTO patient_search_view (
                patient_id, full_name, date_of_birth, patient_type, gender,
                phone, curp, credential_number, profile_status,
                branch_id, consultation_count, created_at
            ) VALUES (
                gen_random_uuid(),
                first_names[1 + (i % array_length(first_names, 1))] || ' '
                    || last_names[1 + (i % array_length(last_names, 1))] || ' '
                    || last_names[1 + ((i + 7) % array_length(last_names, 1))],
                '1960-01-01'::date + (i % 20000),
                CASE WHEN i % 3 = 0 THEN 'INSURED' WHEN i % 3 = 1 THEN 'PRIVATE' ELSE 'SOCIAL' END,
                CASE WHEN i % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
                '55' || lpad((1000000 + i)::text, 8, '0'),
                'CURP' || lpad(i::text, 14, '0'),
                'CRED' || lpad(i::text, 10, '0'),
                'ACTIVE',
                b_id,
                i % 20,
                now()
            );
        END LOOP;

        RAISE NOTICE 'Branch % seeded: % items, % deltas, % patients',
            b_idx, v_items_per_branch, v_items_per_branch * v_deltas_per_item, v_patients_per_branch;
    END LOOP;

    RAISE NOTICE 'Seeding complete: % branches, % total patients',
        v_branch_count, v_branch_count * v_patients_per_branch;
END $$;

-- Summary
SELECT 'Branches' AS entity, count(*) AS total FROM branches WHERE name LIKE 'ScalTest-Branch-%'
UNION ALL
SELECT 'Inventory Items', count(*) FROM inventory_items ii
    JOIN branches b ON ii.branch_id = b.id WHERE b.name LIKE 'ScalTest-Branch-%'
UNION ALL
SELECT 'Inventory Deltas', count(*) FROM inventory_deltas id
    JOIN branches b ON id.branch_id = b.id WHERE b.name LIKE 'ScalTest-Branch-%'
UNION ALL
SELECT 'Patients', count(*) FROM patient_search_view WHERE branch_id IN (
    SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%')
UNION ALL
SELECT 'Tariffs', count(*) FROM service_tariffs WHERE branch_id IN (
    SELECT id FROM branches WHERE name LIKE 'ScalTest-Branch-%');
