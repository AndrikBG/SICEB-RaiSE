# Critical User Stories Analysis

## Overview

This document identifies the most architecturally significant User Stories by cross-referencing the **Quality Attributes Priority Matrix** (scenarios rated **High Business Importance** AND **High Technical Difficulty**) with the User Stories table.

### High/High Quality Attribute Scenarios

| Scenario ID | Quality Attribute | Description                                        |
| ----------- | ----------------- | -------------------------------------------------- |
| PER-01      | Performance       | Real-time inventory update across branches         |
| SEC-02      | Security          | Data segmentation by branch (multi-tenancy)        |
| REL-01      | Reliability       | Offline-online synchronization without data loss   |
| REL-02      | Reliability       | Recovery from partial synchronization failure      |
| USA-01      | Usability         | Transparent offline operation during consultations |
| ESC-02      | Scalability       | Branch growth without performance degradation      |

---

## Mapping: Scenarios → User Stories

### PER-01 — Real-time inventory update

| US ID | User Story | Justification |
|---|---|---|
| **US-004** | View complete inventory of ALL services | Without real-time multi-branch inventory, the administrator makes decisions on stale data |
| **US-005** | View ONLY my service's inventory | Each Service Manager depends on instantly accurate data to operate |
| **US-052** | Register supplies used during consultation | This is the **origin** of the update — without this registration there is no data to synchronize |

### SEC-02 — Data segmentation by branch

| US ID | User Story | Justification |
|---|---|---|
| **US-003** | Assign role-based permission levels | Foundation of access control; without roles, segmentation is impossible |
| **US-074** | Select "Active Branch" upon login | Defines the **security context** — all visible data depends on this selection |
| **US-071** | Register a new Branch | Without the Branch entity registered, there is nothing to segment against |

### REL-01 — Offline-online synchronization without data loss

| US ID | User Story | Justification |
|---|---|---|
| **US-076** | Continue registering consultations when internet is lost | This US **directly implements** the scenario — the most technically critical story |
| **US-025** | Add new consultation entry to patient record | Consultations are the primary data generated offline that must be synchronized |
| **US-044** | Register payments for consultations, medications, and labs | Payments are also recorded offline and must synchronize without duplicates |

### REL-02 — Recovery from partial synchronization failure

| US ID | User Story | Justification |
|---|---|---|
| **US-076** | Continue registering consultations when internet is lost | Same US as REL-01 — partial sync recovery is part of the same offline flow |
| **US-026** | Consultation entries must be immutable | Immutability **simplifies** conflict resolution in partial sync (no merge conflicts) |

### USA-01 — Transparent offline operation

| US ID | User Story | Justification |
|---|---|---|
| **US-076** | Continue registering consultations when internet is lost | Same functionality from usability perspective: user **must not notice** the drop |
| **US-024** | Create new medical record for a patient | A new patient may arrive during a network outage — registration must be possible |
| **US-031** | Prescribe medications during consultation | The prescription flow cannot be halted by lack of internet |

### ESC-02 — Branch growth without degradation

| US ID | User Story | Justification |
|---|---|---|
| **US-071** | Register a new Branch | The registration system must support growth without affecting existing branches |
| **US-074** | Select Active Branch upon login | With 15 active branches, context switching must remain fast |
| **US-075** | Consolidated dashboard of all branches | This is the highest stress point — aggregating data from N branches without degrading response |

---

## Summary: Top 10 Most Critical User Stories

| Rank | US ID | Short Name | High/High Scenarios Supported |
|---|---|---|---|
| 1 | **US-076** | Offline operation & sync | REL-01, REL-02, USA-01 |
| 2 | **US-074** | Active Branch selection | SEC-02, ESC-02 |
| 3 | **US-071** | Branch registration | SEC-02, ESC-02 |
| 4 | **US-003** | Role-based permissions | SEC-02 |
| 5 | **US-026** | Record immutability | REL-02 |
| 6 | **US-025** | Add consultation to record | REL-01, USA-01 |
| 7 | **US-004** | Full inventory view (Admin) | PER-01 |
| 8 | **US-024** | Create medical record | USA-01 |
| 9 | **US-031** | Prescribe medications | USA-01 |
| 10 | **US-044** | Register payments | REL-01 |



---

## HIGH Priority User Stories (from [Requirements/US/Table_US.md](US/Table_US.md))


### Authentication & Access Control

| US ID | User Story |
|---|---|
| **US-001** | Create user accounts with role-based permissions |
| **US-002** | Log in with credentials to access the system securely |
| **US-003** | Assign different permission levels by role |

### Inventory Management

| US ID | User Story |
|---|---|
| **US-004** | View complete inventory of ALL services (General Admin) |
| **US-005** | View ONLY my service's inventory (Service Manager) |

### Patient Registration

| US ID | User Story |
|---|---|
| **US-019** | Register new patients with demographic information |
| **US-020** | Classify patients by type with automatic discount (Student 30%, Worker 20%, External 0%) |
| **US-023** | Validate guardian presence for patients under 17 |

### Medical Records

| US ID | User Story |
|---|---|
| **US-024** | Create a new medical record for a patient |
| **US-025** | Add a new consultation entry to an existing record |
| **US-026** | Consultation entries must be immutable (not editable) |
| **US-027** | View comprehensive medical history from all services |

### Pharmacy

| US ID | User Story |
|---|---|
| **US-031** | Prescribe medications during a consultation |
| **US-032** | View a patient's prescriptions for dispensing |
| **US-033** | Validate prescription exists before dispensing |
| **US-034** | Verify inventory before dispensing medications |
| **US-035** | Register dispensation of controlled medications |

### Laboratory

| US ID | User Story |
|---|---|
| **US-038** | Request laboratory studies during a consultation |
| **US-040** | View pending study requests |
| **US-041** | Enter study results in text format |
| **US-042** | View laboratory results in the patient's record |

### Payments & Billing

| US ID | User Story |
|---|---|
| **US-044** | Register payments for consultations, medications, and labs |
| **US-064** | Configure tariffs by medical service |

### Personnel & Security

| US ID | User Story |
|---|---|
| **US-050** | Validate resident actions by level (R1-R4) |
| **US-051** | Block R1, R2, R3 from prescribing controlled medications |
| **US-066** | Register audit log of who accessed which record and when |

### Multi-Branch & Offline

| US ID | User Story |
|---|---|
| **US-071** | Register a new Branch with its details |
| **US-074** | Select "Active Branch" upon login |
| **US-076** | Continue registering consultations when internet is lost |
