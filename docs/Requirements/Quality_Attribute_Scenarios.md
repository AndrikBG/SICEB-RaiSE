# Quality Attribute Scenarios - SICEB

This document defines detailed quality attribute scenarios for the SICEB system. Each scenario follows the six-part structure: Source, Stimulus, Artifact, Environment, Response, and Response Measure.

---

## Performance

### PER-01: Real-time inventory update
| Part | Description |
|---|---|
| **Stimulus Source** | Physician attending a consultation at Branch A |
| **Stimulus** | Registers the use of 3 medical supplies during a consultation |
| **Artifact** | General Inventory Management Module (EP-02) |
| **Environment** | Normal operation, multiple branches connected simultaneously |
| **Response** | The system updates the branch inventory and reflects the change in the General Administrator's dashboard |
| **Response Measure** | The update is reflected across all views in less than 2 seconds |

### PER-02: Consolidated financial report generation
| Part | Description |
|---|---|
| **Stimulus Source** | General Director |
| **Stimulus** | Requests a monthly report of income, expenses, and profitability across all branches |
| **Artifact** | Financial Reports Module (EP-10) |
| **Environment** | Normal operation with 10 active branches and thousands of recorded transactions |
| **Response** | The system generates the consolidated report with breakdown by branch, service, and patient type |
| **Response Measure** | The report is displayed on screen in less than 10 seconds |

### PER-03: Patient search
| Part | Description |
|---|---|
| **Stimulus Source** | Reception Staff |
| **Stimulus** | Searches for a patient by partial name upon arrival at the clinic |
| **Artifact** | Patient Registration Module (EP-05) |
| **Environment** | Normal operation with more than 50,000 registered records |
| **Response** | The system displays a list of matches with basic patient information |
| **Response Measure** | Results are displayed in less than 1 second |

### PER-04: Prescription validation at pharmacy
| Part | Description |
|---|---|
| **Stimulus Source** | Pharmacy Personnel |
| **Stimulus** | Scans or selects a medical prescription for medication dispensing |
| **Artifact** | Pharmacy Management Module (EP-07) |
| **Environment** | Normal operation, peak hours with multiple simultaneous dispensations |
| **Response** | The system validates the prescription, verifies available stock, and confirms whether the prescriber has permissions for that medication |
| **Response Measure** | The complete validation executes in less than 2 seconds |

---

## Security

### SEC-01: Role-based access control
| Part | Description |
|---|---|
| **Stimulus Source** | Authenticated R1 Resident |
| **Stimulus** | Attempts to prescribe a controlled medication to a patient |
| **Artifact** | Medical Personnel Control Module (EP-11) and Pharmacy Management Module (EP-07) |
| **Environment** | Normal operation during a medical consultation |
| **Response** | The system blocks the action, displays a message indicating that the resident's level does not permit prescribing controlled medications, and logs the attempt in the audit trail |
| **Response Measure** | 100% of level-restricted actions are blocked; the attempt is logged in less than 1 second |

### SEC-02: Data segmentation by branch
| Part | Description |
|---|---|
| **Stimulus Source** | Pediatrics Service Manager at Branch B |
| **Stimulus** | Attempts to view the supply inventory of Branch A |
| **Artifact** | General Inventory Management Module (EP-02) and Security & Multi-tenancy Module (EP-14) |
| **Environment** | Normal operation with multiple active branches |
| **Response** | The system denies access and displays only the inventory for Branch B corresponding to their service |
| **Response Measure** | Zero unauthorized cross-branch access permitted |

### SEC-03: Controlled medication traceability
| Part | Description |
|---|---|
| **Stimulus Source** | Pharmacy Personnel |
| **Stimulus** | Dispenses a controlled medication to a patient |
| **Artifact** | Pharmacy Management Module (EP-07) |
| **Environment** | Normal operation |
| **Response** | The system automatically records: prescriber, dispenser, patient, medication, lot number, quantity, date, and time in an immutable audit log |
| **Response Measure** | 100% of controlled medication dispensations are recorded with all 8 traceability fields complete |

### SEC-04: REST API protection
| Part | Description |
|---|---|
| **Stimulus Source** | Unauthenticated external user |
| **Stimulus** | Sends an HTTP request to a SICEB REST API endpoint |
| **Artifact** | Interoperability API (EP-16) |
| **Environment** | Normal operation, API exposed to the internet |
| **Response** | The system returns an HTTP 401 status code with a generic message without revealing internal structure, endpoints, or system data |
| **Response Measure** | 100% of requests without valid authentication are rejected; zero information leaks in error responses |

---

## Maintainability

### MNT-01: Adding new medical services
| Part | Description |
|---|---|
| **Stimulus Source** | Clinics Network Directorate |
| **Stimulus** | Requests the incorporation of a new medical service (e.g., Dermatology) into the system |
| **Artifact** | Inventory Module (EP-02), Medical Record Module (EP-06), and Personnel Control Module (EP-11) |
| **Environment** | System in production with existing services operating |
| **Response** | The new service is configured through system parameters (service catalog) without modifying the source code of existing modules |
| **Response Measure** | The service becomes operational in less than 1 day, with zero code modifications to pre-existing modules |

### MNT-02: CFDI module integration
| Part | Description |
|---|---|
| **Stimulus Source** | Development Team |
| **Stimulus** | Electronic invoicing (CFDI) needs to be integrated into the payment module in version 1.0 |
| **Artifact** | Payment Registration Module (EP-09) |
| **Environment** | Transition from version 1.0 to 2.0, system in production |
| **Response** | The CFDI module integrates by consuming existing interfaces from the payment module without altering its internal logic |
| **Response Measure** | Integration is completed in less than 2 sprints without generating regressions in the existing payment flow |

### MNT-03: New role configuration
| Part | Description |
|---|---|
| **Stimulus Source** | General Administrator |
| **Stimulus** | Needs to create a new user role (e.g., Nutritionist) with specific permissions |
| **Artifact** | Authentication and Access Control Module (EP-01) |
| **Environment** | System in production |
| **Response** | The administrator configures the new role and its permissions from the administration interface without development team intervention |
| **Response Measure** | The role becomes operational in less than 30 minutes, with zero lines of code modified |

---

## Reliability

### REL-01: Offline-online synchronization without data loss
| Part | Description |
|---|---|
| **Stimulus Source** | Branch internet network |
| **Stimulus** | The connection is lost for 2 hours and then restored |
| **Artifact** | Offline Mode & Sync Module (EP-17) |
| **Environment** | Branch operating in offline mode with consultations and sales recorded locally |
| **Response** | When the connection is restored, the system automatically synchronizes all locally stored data with the cloud server |
| **Response Measure** | 100% of records generated offline are synchronized correctly, with zero losses and zero duplicates |

### REL-02: Recovery from partial synchronization failure
| Part | Description |
|---|---|
| **Stimulus Source** | Network infrastructure |
| **Stimulus** | The connection drops mid-synchronization process (50 of 100 records sent) |
| **Artifact** | Offline Mode & Sync Module (EP-17) |
| **Environment** | Synchronization in progress after an offline period |
| **Response** | The system detects the interruption, marks the cutoff point, and upon connection restoration resumes from record 51 without resending previous records |
| **Response Measure** | Zero duplicate records; synchronization completes on the next attempt without manual intervention |

### REL-03: Fault isolation between modules
| Part | Description |
|---|---|
| **Stimulus Source** | Internal system error |
| **Stimulus** | The Financial Reports Module (EP-10) throws an unhandled exception |
| **Artifact** | Medical Care Module (EP-06), Pharmacy Module (EP-07), and Laboratory Module (EP-08) |
| **Environment** | Normal operation with patients being attended |
| **Response** | The medical care, pharmacy, and laboratory modules continue operating without interruption; the error is logged and the technical team is notified |
| **Response Measure** | Zero impact on critical care modules; the failure is logged in less than 1 second |

### REL-04: Local cache corruption detection
| Part | Description |
|---|---|
| **Stimulus Source** | Branch workstation |
| **Stimulus** | The PWA's local cache presents inconsistent or corrupt data |
| **Artifact** | Offline Mode & Sync Module (EP-17) |
| **Environment** | Internet connection available |
| **Response** | The system detects the inconsistency through checksum validation, discards the corrupt cache, and forces a full re-download from the cloud server |
| **Response Measure** | The cache is restored in less than 5 minutes with zero incorrect data persisting |

---

## Usability

### USA-01: Transparent offline operation
| Part | Description |
|---|---|
| **Stimulus Source** | Attending Physician treating a patient |
| **Stimulus** | The internet connection is lost during an ongoing consultation |
| **Artifact** | Offline Mode & Sync Module (EP-17) and Medical Record Module (EP-06) |
| **Environment** | Medical consultation in progress, transition from online to offline mode |
| **Response** | The system switches to offline mode transparently, allowing the physician to continue recording the consultation without interruptions or blocking error messages |
| **Response Measure** | The user perceives the transition in less than 3 seconds with a non-intrusive visual indicator; zero work steps lost |

### USA-02: New resident onboarding
| Part | Description |
|---|---|
| **Stimulus Source** | Newly admitted R1 Resident |
| **Stimulus** | Accesses the system for the first time to register a medical consultation |
| **Artifact** | Medical Record Module (EP-06) |
| **Environment** | User's first session, without prior formal training |
| **Response** | The interface guides the resident step by step through the consultation registration flow with clear fields, inline validations, and contextual help |
| **Response Measure** | The resident successfully completes their first consultation in less than 15 minutes without external assistance |

### USA-03: Functional interface on tablets
| Part | Description |
|---|---|
| **Stimulus Source** | Pharmacy Personnel |
| **Stimulus** | Accesses the dispensing module from a 10-inch tablet |
| **Artifact** | Pharmacy Management Module (EP-07), responsive PWA interface |
| **Environment** | Normal operation at the pharmacy counter |
| **Response** | All interface elements (buttons, tables, forms) adapt correctly to the screen without requiring horizontal scrolling or overlapping elements |
| **Response Measure** | 100% of module functionalities are operable on tablet; zero interface elements are inaccessible or unreadable |

### USA-04: Quick supply request creation
| Part | Description |
|---|---|
| **Stimulus Source** | Service Manager |
| **Stimulus** | Needs to request urgent supplies for their service |
| **Artifact** | Supply Request and Approval Module (EP-03) |
| **Environment** | Normal operation from the main dashboard |
| **Response** | The system allows creating, detailing, and submitting the request with product autocomplete from the catalog and suggested quantities based on history |
| **Response Measure** | The request is completed and submitted in a maximum of 3 steps and less than 2 minutes |

---

## Scalability

### ESC-01: Addition of new branches
| Part | Description |
|---|---|
| **Stimulus Source** | General Administrator |
| **Stimulus** | Registers a new branch in the system with its configuration data |
| **Artifact** | Branch Administration Module (EP-18) |
| **Environment** | System in production with existing branches operating |
| **Response** | The system creates the branch, allows assigning a Service Manager, configuring initial inventory, and activating it on the global dashboard |
| **Response Measure** | The branch becomes fully operational in less than 1 hour without affecting the performance of existing branches |

### ESC-02: Branch growth without degradation
| Part | Description |
|---|---|
| **Stimulus Source** | Organic growth of the clinics network |
| **Stimulus** | The network grows from 3 to 15 active branches with concurrent users |
| **Artifact** | SICEB Cloud Server, central database |
| **Environment** | Normal operation during peak hours with all branches active |
| **Response** | The system maintains response times within defined thresholds for all modules |
| **Response Measure** | Performance degradation of less than 10% compared to the 3-branch baseline |

### ESC-03: Staff rotation between branches
| Part | Description |
|---|---|
| **Stimulus Source** | Attending Physician assigned to multiple branches |
| **Stimulus** | Switches active branch context from Branch A to Branch B during their shift |
| **Artifact** | Medical Personnel Control Module (EP-11) and Security & Multi-tenancy Module (EP-14) |
| **Environment** | Normal operation, physician authenticated with permissions at both branches |
| **Response** | The system switches context displaying inventory, patients, and data corresponding to Branch B without requiring logout |
| **Response Measure** | The context switch completes in less than 3 seconds; data from the previous branch ceases to be visible immediately |

---

## Auditability

### AUD-01: Administrative decision traceability
| Part | Description |
|---|---|
| **Stimulus Source** | General Director |
| **Stimulus** | Requests a review of the supply request approval and rejection history for the last quarter |
| **Artifact** | Supply Request and Approval Module (EP-03) and History & Operations Reports Module (EP-15) |
| **Environment** | Normal operation |
| **Response** | The system generates a report with each decision, including: requester, approver, justification, date/time, and final status |
| **Response Measure** | 100% of decisions include all 5 traceability fields; the report is generated in less than 15 seconds |

### AUD-02: Controlled medication report for health authority
| Part | Description |
|---|---|
| **Stimulus Source** | External health authority (COFEPRIS) |
| **Stimulus** | Requests an audit of controlled medication handling for a specific period |
| **Artifact** | Pharmacy Management Module (EP-07) and History & Operations Reports Module (EP-15) |
| **Environment** | External regulatory requirement |
| **Response** | The system generates a complete report with every controlled medication movement: prescriber, dispenser, patient, medication, lot number, quantity, date/time, and branch |
| **Response Measure** | The report is generated in less than 30 seconds; it covers 100% of transactions for the requested period with no missing records |

### AUD-03: Medical record immutability
| Part | Description |
|---|---|
| **Stimulus Source** | Any system user |
| **Stimulus** | Attempts to modify or delete a previously recorded consultation in a patient's medical record |
| **Artifact** | Medical Record Module (EP-06) |
| **Environment** | Normal operation |
| **Response** | The system rejects the operation; the record only allows the addition of new consultations, never the editing or deletion of existing entries |
| **Response Measure** | 100% of modification attempts are blocked; the attempt is logged in the audit trail |

---

## Interoperability

### IOP-01: Integration with external academic system
| Part | Description |
|---|---|
| **Stimulus Source** | Educational institution's academic system |
| **Stimulus** | Queries via REST API the training activities and workshops completed by residents |
| **Artifact** | Interoperability API (EP-16) and Personnel Control Module (EP-11) |
| **Environment** | Normal operation, external system authenticated with valid API credentials |
| **Response** | The API returns structured data in JSON format with the list of activities, dates, attendees, and status |
| **Response Measure** | API response time under 3 seconds; data conforms to the documented OpenAPI specification |

### IOP-02: Extensibility for new integrations
| Part | Description |
|---|---|
| **Stimulus Source** | Development Team |
| **Stimulus** | Integration with an insurance company for patient policy validation is required |
| **Artifact** | Interoperability API (EP-16) |
| **Environment** | System in production, version 2.0 |
| **Response** | New endpoints are added to the API without modifying existing endpoints or internal system modules |
| **Response Measure** | The new integration is developed and deployed in less than 2 weeks without generating regressions in previous integrations |
