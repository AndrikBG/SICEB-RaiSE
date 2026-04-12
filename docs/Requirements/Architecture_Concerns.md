 # Architecture Concerns — SICEB

Architectural concerns encompass additional aspects that need to be considered as part of architectural design but are not expressed as traditional requirements. This document organizes them by architectural category and assigns priorities based on their impact on SICEB's core operations, regulatory compliance, and quality attribute scenarios.

---

## 1. Data Management and Persistence

### Data Lifecycle

| ID         | Concern                                                                                                                                                                                                                           | Priority   |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-01** | **Data retention policies:** Define retention periods for clinical records (permanent per NOM-004-SSA3-2012), transactional data (prescriptions, payments, inventory movements), and operational data (session logs, sync queues) | **High**   |
| **CRN-02** | **Immutable data model for clinical records:** Design the medical record schema as append-only (insert, never update/delete) to enforce immutability at the database level, not just the application level                        | **High**   |
| **CRN-03** | **Data archiving strategies:** Establish archiving procedures for historical audit logs and append-only medical records that grow indefinitely, including partitioning and cold storage to maintain query performance             | **Medium** |

### Data Migration

| ID         | Concern                                                                                                                                                                                 | Priority |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-04** | **Database migration strategy:** Support schema evolution across the 10 incremental deliveries without data loss or extended downtime in production                                     | **High** |
| **CRN-05** | **Backward compatibility for data schemas:** Ensure new schema versions can coexist with existing data produced by branches that have not yet synced, especially during offline periods | **High** |

### Data Backup and Recovery

| ID         | Concern                                                                                                                                                                                                     | Priority   |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-06** | **Backup frequency and RPO/RTO:** Define backup frequency for the cloud database and acceptable recovery point/time objectives, considering that offline branches hold unsynced data locally                | **High**   |
| **CRN-07** | **Branch deactivation with pending sync data:** If a branch is deactivated while it has unsynced offline data, the system must define whether that data is preserved, rejected, or queued for manual review | **Medium** |

---

## 2. Integration Architecture

### Third-Party Services

| ID         | Concern                                                                                                                                                                                     | Priority   |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-08** | **CFDI integration:** Integrate electronic invoicing by consuming existing payment module interfaces and managing communication with the SAT (tax authority) web services                   | **High**   |
| **CRN-09** | **CFDI issuance during network outage:** CFDI invoices require real-time communication with SAT web services; a strategy is needed for handling invoice requests when the branch is offline | **High**   |
| **CRN-10** | **Fallback mechanisms for external services:** Define graceful degradation strategies when external services (SAT, future insurance APIs, academic integrations) are unavailable            | **Medium** |

### API Design

| ID         | Concern                                                                                                                                                                                           | Priority   |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-11** | **API versioning:** Define the REST API versioning strategy to support external integrations (academic systems) and future integrations (insurance companies) without breaking existing consumers | **High**   |
| **CRN-12** | **API documentation and discovery:** Provide standardized API documentation (OpenAPI/Swagger) to support IOP-01 interoperability scenarios and third-party onboarding                             | **Medium** |

---

## 3. Security Architecture

### Data Protection

| ID         | Concern                                                                                                                                                                                                                                        | Priority |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-13** | **Security hardening:** Protect REST API endpoints against unauthenticated access, enforce HTTPS/Secure WebSocket, and prevent information leaks in error responses                                                                            | **High** |
| **CRN-14** | **Controlled medication dispensation offline:** Prescribing and dispensing controlled medications offline creates a regulatory risk — the system cannot verify real-time stock or validate prescriber permissions against the central database | **High** |

### Access Control

| ID         | Concern                                                                                                                                                                                        | Priority |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-15** | **Authentication and authorization:** Implement role-based access control for 11 roles with branch-scoped permissions, including resident-level restrictions (R1–R4)                           | **High** |
| **CRN-16** | **Supervisor availability offline:** R1/R2 residents require mandatory supervision, but supervisor assignment validation depends on central data — offline consultations may bypass this check | **High** |

### Compliance

| ID         | Concern                                                                                                                                                                                      | Priority |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-17** | **Logging and auditing:** Implement a centralized, immutable audit log that records who accessed which record and when, with support for COFEPRIS traceability and LFPDPPP compliance        | **High** |
| **CRN-18** | **Audit log immutability:** Ensure audit log entries cannot be tampered with — neither by application users nor by database administrators — to comply with regulatory requirements (AUD-03) | **High** |

---

## 4. Operational Architecture

### Monitoring and Observability

| ID         | Concern                                                                                                                                                                                                                          | Priority   |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-19** | **Exception management:** Define a unified strategy for handling errors across the system — distinguishing between recoverable errors (e.g., sync retry) and fatal errors (e.g., corrupt cache), including user-facing messaging | **High**   |
| **CRN-20** | **Logging standards:** Standardize log formats and levels across PWA frontend and REST API backend to facilitate debugging and regulatory audit trails                                                                           | **Medium** |

### Disaster Recovery

| ID         | Concern                                                                                                                                                                                                                        | Priority   |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- |
| **CRN-21** | **Offline-first architecture:** Define the strategy for the PWA to operate offline (local data storage, service workers, sync queue) and seamlessly transition between connected and disconnected modes (REL-01, USA-01)       | **High**   |
| **CRN-22** | **Resilience under secondary failures:** Implement resilience mechanisms so that failures in secondary subsystems (notifications, reports) do not halt core workflows like consultations, prescriptions, or inventory (REL-03) | **Medium** |

### Capacity Planning

| ID         | Concern                                                                                                                                                                                                                | Priority   |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-23** | **Data volume growth for audit logs:** The immutable audit log and append-only medical records will grow indefinitely — a strategy for archival, partitioning, or cold storage is needed to maintain query performance | **Medium** |
| **CRN-24** | **Multi-tenant scalability:** Guarantee that the multi-tenant model supports the growth of the clinic network without performance degradation as branches, users, and data volume increase (ESC-02)                    | **High**   |

---

## 5. Development Architecture

### Code Organization

| ID         | Concern                                                                                                                                                                                                          | Priority |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-25** | **Overall system structure:** Define the top-level decomposition of the system (PWA frontend, REST API backend, cloud database, local offline storage) and the interaction patterns between these layers         | **High** |
| **CRN-26** | **Allocation of functionality to modules:** Distribute the 18 epics (EP-01 to EP-18) into cohesive, loosely coupled modules with well-defined boundaries and interfaces                                          | **High** |
| **CRN-27** | **Dependency management:** Manage dependencies between modules (e.g., Pharmacy depends on Prescriptions from Medical Records; Payments depend on Consultation, Pharmacy, and Lab) to avoid circular dependencies | **High** |

### Development Workflow

| ID         | Concern                                                                                                                                                                                  | Priority   |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-28** | **Code organization and team allocation:** Structure the codebase to enable parallel development across the 10 incremental deliveries without merge conflicts or cross-team dependencies | **Medium** |

---

## 6. Business Architecture

### Business Continuity

| ID         | Concern                                                                                                                                                                                              | Priority   |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-29** | **Multi-branch architecture:** Establish how the system supports multiple branches as a single deployable unit with tenant-scoped data isolation, rather than separate instances per branch (ESC-02) | **High**   |
| **CRN-30** | **Configuration management:** Externalize branch-specific settings (tariffs, service catalogs, discount rules) so they can be modified through the administration UI without code changes            | **Medium** |

---

## 7. Compliance and Legal

### Regulatory Requirements

| ID         | Concern                                                                                                                                                                                                                     | Priority |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-31** | **NOM-004-SSA3-2012 compliance:** Ensure the clinical record schema and workflows comply with the Mexican standard for medical records, including mandatory sections, healthcare provider signatures, and retention periods | **High** |
| **CRN-32** | **LFPDPPP data protection:** Handle personally identifiable patient data in compliance with the Federal Law on Protection of Personal Data, including consent management, access rights, and data portability               | **High** |
| **CRN-33** | **COFEPRIS controlled substance tracking:** Maintain regulatory traceability for controlled medications across prescriptions, dispensation, and inventory, even when branches operate offline                               | **High** |

---

## 8. Synchronization and Conflict Resolution

### Data Synchronization

| ID         | Concern                                                                                                                                                                                                                                         | Priority |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-34** | **Data synchronization and conflict resolution:** Establish the protocol for syncing offline data to the cloud — including queue-based sync, partial failure recovery, duplicate prevention, and ordering guarantees (REL-01)                   | **High** |
| **CRN-35** | **Inventory consistency under concurrent offline edits:** If two branches use the same supply item offline and both sync later, the final inventory count may become negative or inconsistent — a conflict resolution policy is needed (PER-01) | **High** |
| **CRN-36** | **Caching strategy:** Define what data is cached locally for offline operation (patient records, inventory, catalogs), cache invalidation policies, and corruption detection mechanisms (checksum validation)                                   | **High** |

### Identity Management

| ID         | Concern                                                                                                                                                                                         | Priority |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| **CRN-37** | **Unique patient identifier:** Establish a system-wide unique patient ID that persists across branches, ensuring a patient has exactly one medical record regardless of which branch they visit | **High** |
| **CRN-38** | **Offline ID generation:** Define a strategy for generating unique IDs for records created offline (e.g., UUIDs) to prevent collisions when multiple branches sync simultaneously               | **High** |

---

## 9. User Experience

### Performance Perception

| ID         | Concern                                                                                                                                                                                                                    | Priority   |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **CRN-39** | **Offline-to-online transition UX:** Design clear visual indicators and seamless user flows for when the system transitions between offline and online modes, including sync status and data freshness indicators (USA-01) | **High**   |
| **CRN-40** | **Input validation:** Standardize validation rules across all modules — required fields, data formats, and business rule validations (e.g., guardian required for patients under 17, professional license for physicians)  | **Medium** |

---

## 10. Internal Technical Requirements

These are derived requirements not typically specified by customers but necessary for development, deployment, or maintenance.

| ID         | Concern                                                                                                                                                                                                | Priority |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| **CRN-41** | **Timestamps in UTC:** Store all timestamps in UTC internally and convert to local timezone (America/Mexico_City) only at the presentation layer, to prevent synchronization conflicts across branches | **High** |
| **CRN-42** | **Currency handling:** Use a fixed-precision data type (e.g., BigDecimal) for all monetary values (tariffs, payments, discounts) to avoid floating-point rounding errors in financial calculations     | **High** |

---

## Priority Summary

| Priority   | Count | Concern IDs                                                                                                                                                                                                                                                    |
| ---------- | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **High**   | 32    | CRN-01, CRN-02, CRN-04, CRN-05, CRN-06, CRN-08, CRN-09, CRN-11, CRN-13, CRN-14, CRN-15, CRN-16, CRN-17, CRN-18, CRN-19, CRN-21, CRN-24, CRN-25, CRN-26, CRN-27, CRN-29, CRN-31, CRN-32, CRN-33, CRN-34, CRN-35, CRN-36, CRN-37, CRN-38, CRN-39, CRN-41, CRN-42 |
| **Medium** | 10    | CRN-03, CRN-07, CRN-10, CRN-12, CRN-20, CRN-22, CRN-23, CRN-28, CRN-30, CRN-40                                                                                                                                                                                 |

### Priority Rationale

**High Priority** — Concerns that directly impact:
- Core clinical and business workflows (medical records, prescriptions, inventory, payments)
- Regulatory compliance (NOM-004, LFPDPPP, COFEPRIS, SAT/CFDI)
- The offline-first architecture, which is the system's most complex technical challenge (REL-01)
- Data integrity and multi-tenant security (SEC-01, SEC-02, AUD-03)
- Multi-branch synchronization and conflict resolution (PER-01)

**Medium Priority** — Concerns that are important for system quality but have lower immediate impact:
- Operational efficiency and developer experience
- Graceful degradation for non-critical subsystems (REL-03)
- Documentation and observability
- Configuration flexibility
