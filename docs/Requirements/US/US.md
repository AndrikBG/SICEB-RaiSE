# User stories — phased rollout (deprecated)

> **Deprecated.** This file is **no longer authoritative**. It was an early approximation of how user stories might be grouped and sequenced **before** the Attribute-Driven Design (ADD) process was applied. Actual scope, iteration order, and driver traceability are defined in **[`../../ADD/Design/IterationPlan.md`](../../ADD/Design/IterationPlan.md)**, the **[`../../ADD/implementation/`](../../ADD/implementation/)** specs and phase guides, and the per-story files **`US-*.md`** in this folder. Keep this document only for historical context.

---

### **PHASE 1: Technical Foundations **

| Order | ID | Story |
|-------|----|---------| 
| **1** | **US-001** | Create user accounts with role-based permissions |
| **2** | **US-002** | Login with credentials |
| **3** | **US-003** | Assign permission levels (9 roles) |
| **4** | **US-066** | Register access audit to records |
| **5** | **US-064** | Configure tariffs per service |
| **6** | **US-004** | Admin sees ALL inventory |
| **7** | **US-005** | Manager sees ONLY their inventory |
| **8** | **US-049** | Register medical personnel (attending and residents R1-R4) |

---

### **PHASE 2: Patient Management and Core Records **

| Order | ID | Story |
|-------|----|---------| 
| **9** | **US-019** | Register new patients |
| **10** | **US-020** | Classify patients and apply automatic discounts |
| **11** | **US-023** | Validate guardian present for minors <17 years |
| **12** | **US-024** | Create medical record |
| **13** | **US-025** | Add consultation to record |
| **14** | **US-026** | Immutable consultations |
| **15** | **US-027** | View complete patient history |
| **16** | **US-030** | Register vital signs |
| **17** | **US-028** | Search patients |

---

### **PHASE 3: Pharmacy and Laboratory Complete  **

| Order | ID | Story |
|-------|----|---------| 
| **18** | **US-031** | Prescribe medications in consultation |
| **19** | **US-032** | Pharmacy sees patient prescriptions |
| **20** | **US-033** | Validate prescription exists before dispensing |
| **22** | **US-038** | Request laboratory studies |
| **23** | **US-040** | Lab sees pending requests |
| **24** | **US-041** | Lab enters results in text |

---

### **PHASE 4: Complete Inventory **

| Order | ID | Story |
|-------|----|---------| 
| **25** | **US-006** | See low stock alerts |
| **26** | **US-008** | Reagent temperature alerts |
| **27** | **US-009** | Send digital supply request |
| **28** | **US-010** | Admin receives request notifications |
| **29** | **US-011** | Approve or reject requests with justification |
| **30** | **US-012** | Manager receives decision notification |
| **31** | **US-052** | Register supply usage in consultation |

---
