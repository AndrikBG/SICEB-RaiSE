-- V010: Seed system roles and permissions
-- Drivers: US-003, US-001, MNT-03, CRN-15, SEC-01

-- ============================================================
-- Permission catalog — 37 permissions across 9 categories
-- ============================================================
INSERT INTO permissions (key, description, category, requires_residency_check) VALUES
    -- Patient management
    ('patient:create',      'Register new patients',                'patient',    FALSE),
    ('patient:read',        'View patient demographics',            'patient',    FALSE),
    ('patient:update',      'Update patient demographics',          'patient',    FALSE),
    ('patient:search',      'Search patients',                      'patient',    FALSE),

    -- Medical records
    ('record:create',       'Create medical records',               'record',     FALSE),
    ('record:read',         'View medical records',                 'record',     FALSE),

    -- Consultation
    ('consultation:create', 'Record consultations',                 'consultation', TRUE),
    ('consultation:read',   'View consultations',                   'consultation', FALSE),

    -- Prescriptions
    ('prescription:create',       'Create prescriptions',                 'prescription', TRUE),
    ('prescription:read',         'View prescriptions',                   'prescription', FALSE),
    ('controlled_med:prescribe',  'Prescribe controlled medications',     'prescription', TRUE),

    -- Laboratory
    ('lab:order',           'Request laboratory studies',            'laboratory', TRUE),
    ('lab:result',          'Enter laboratory results',             'laboratory', FALSE),
    ('lab:read',            'View laboratory studies and results',  'laboratory', FALSE),

    -- Pharmacy
    ('pharmacy:dispense',   'Dispense medications',                 'pharmacy',   FALSE),
    ('pharmacy:read',       'View pharmacy prescriptions',          'pharmacy',   FALSE),
    ('pharmacy:catalog',    'Manage medication catalog',            'pharmacy',   FALSE),

    -- Inventory
    ('inventory:read',      'View inventory levels',                'inventory',  FALSE),
    ('inventory:manage',    'Manage inventory (adjustments)',       'inventory',  FALSE),
    ('supply:request',      'Create supply requests',               'inventory',  FALSE),
    ('supply:approve',      'Approve/reject supply requests',       'inventory',  FALSE),

    -- Administration
    ('user:manage',         'Create and manage user accounts',      'admin',      FALSE),
    ('user:read',           'View user list',                       'admin',      FALSE),
    ('role:manage',         'Create and configure roles',           'admin',      FALSE),
    ('role:read',           'View roles and permissions',           'admin',      FALSE),
    ('branch:manage',       'Manage branches',                      'admin',      FALSE),
    ('branch:read',         'View branch information',              'admin',      FALSE),
    ('tariff:manage',       'Configure service tariffs',            'admin',      FALSE),

    -- Reporting
    ('report:financial',    'View financial reports',               'reporting',  FALSE),
    ('report:operational',  'View operational reports',             'reporting',  FALSE),
    ('report:clinical',     'View clinical reports',                'reporting',  FALSE),
    ('report:cross_branch', 'View cross-branch consolidated reports','reporting', FALSE),

    -- Audit
    ('audit:read',          'View audit logs',                      'audit',      FALSE),
    ('audit:verify',        'Verify audit chain integrity',         'audit',      FALSE),

    -- Billing
    ('billing:create',      'Register payments',                    'billing',    FALSE),
    ('billing:read',        'View payment records',                 'billing',    FALSE),

    -- Scheduling
    ('schedule:manage',     'Manage appointments',                  'scheduling', FALSE);

-- ============================================================
-- 11 system roles (is_system_role = TRUE, protected)
-- ============================================================
INSERT INTO roles (name, description, is_system_role) VALUES
    ('Director General',        'Full system access, strategic reports, cross-branch visibility',        TRUE),
    ('Administrador General',   'Complete system management, user and role configuration',               TRUE),
    ('Jefe de Servicio',        'Service-specific management, inventory for assigned service',           TRUE),
    ('Médico Adscrito',         'Full clinical care without restrictions, attending physician',          TRUE),
    ('Residente R4',            'Clinical care with minimal supervision requirements',                  TRUE),
    ('Residente R3',            'Clinical care with some restrictions, no controlled substances',       TRUE),
    ('Residente R2',            'Clinical care with moderate restrictions, requires supervision',       TRUE),
    ('Residente R1',            'Clinical care with maximum restrictions, mandatory supervision',       TRUE),
    ('Recepción',               'Patient registration, scheduling, billing',                            TRUE),
    ('Farmacia',                'Medication dispensing, prescription validation',                        TRUE),
    ('Laboratorio',             'Laboratory study management, result entry',                             TRUE);

-- ============================================================
-- Role-permission assignments
-- ============================================================

-- Director General — everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Director General';

-- Administrador General — everything except controlled_med:prescribe and clinical write
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Administrador General'
  AND p.key NOT IN ('controlled_med:prescribe', 'consultation:create', 'prescription:create', 'lab:order');

-- Jefe de Servicio — service management + inventory + read clinical
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Jefe de Servicio'
  AND p.key IN (
    'patient:read', 'patient:search',
    'record:read', 'consultation:read', 'prescription:read', 'lab:read',
    'inventory:read', 'inventory:manage', 'supply:request',
    'user:read', 'branch:read',
    'report:operational', 'report:clinical',
    'billing:read'
  );

-- Médico Adscrito — full clinical care including controlled substances
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Médico Adscrito'
  AND p.key IN (
    'patient:create', 'patient:read', 'patient:update', 'patient:search',
    'record:create', 'record:read',
    'consultation:create', 'consultation:read',
    'prescription:create', 'prescription:read', 'controlled_med:prescribe',
    'lab:order', 'lab:read',
    'schedule:manage',
    'report:clinical'
  );

-- Residente R4 — clinical care, no controlled substances
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Residente R4'
  AND p.key IN (
    'patient:create', 'patient:read', 'patient:update', 'patient:search',
    'record:create', 'record:read',
    'consultation:create', 'consultation:read',
    'prescription:create', 'prescription:read',
    'lab:order', 'lab:read',
    'schedule:manage'
  );

-- Residente R3 — same as R4 (controlled substances blocked by residency policy, not permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Residente R3'
  AND p.key IN (
    'patient:create', 'patient:read', 'patient:update', 'patient:search',
    'record:create', 'record:read',
    'consultation:create', 'consultation:read',
    'prescription:create', 'prescription:read',
    'lab:order', 'lab:read',
    'schedule:manage'
  );

-- Residente R2 — clinical care with supervision required
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Residente R2'
  AND p.key IN (
    'patient:create', 'patient:read', 'patient:update', 'patient:search',
    'record:create', 'record:read',
    'consultation:create', 'consultation:read',
    'prescription:create', 'prescription:read',
    'lab:order', 'lab:read'
  );

-- Residente R1 — most restricted clinical care
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Residente R1'
  AND p.key IN (
    'patient:read', 'patient:search',
    'record:read',
    'consultation:create', 'consultation:read',
    'prescription:read',
    'lab:read'
  );

-- Recepción — patient registration + billing + scheduling
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Recepción'
  AND p.key IN (
    'patient:create', 'patient:read', 'patient:update', 'patient:search',
    'record:create',
    'billing:create', 'billing:read',
    'schedule:manage',
    'branch:read'
  );

-- Farmacia — dispensing + prescriptions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Farmacia'
  AND p.key IN (
    'pharmacy:dispense', 'pharmacy:read', 'pharmacy:catalog',
    'prescription:read',
    'patient:read', 'patient:search',
    'inventory:read',
    'branch:read'
  );

-- Laboratorio — lab management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'Laboratorio'
  AND p.key IN (
    'lab:result', 'lab:read',
    'patient:read', 'patient:search',
    'branch:read'
  );

-- ============================================================
-- Seed dev admin user (password: Admin123! — bcrypt hash)
-- ============================================================
INSERT INTO users (username, email, full_name, password_hash, role_id, is_active, must_change_password, branch_id)
SELECT 'admin', 'admin@siceb.mx', 'Administrador del Sistema',
       '$2a$10$Wupx5mkPpBvJnhmxDC4gEO94unT47.lnK7rZuHC5p/Hdd9G8i4UqO',
       r.role_id, TRUE, FALSE,
       (SELECT id FROM branches LIMIT 1)
FROM roles r WHERE r.name = 'Administrador General';

-- Assign admin to all branches
INSERT INTO user_branch_assignments (user_id, branch_id)
SELECT u.user_id, b.id
FROM users u CROSS JOIN branches b
WHERE u.username = 'admin';
