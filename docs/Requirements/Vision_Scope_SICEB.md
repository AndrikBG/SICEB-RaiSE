# **Vision and Scope Document**

## Index

- [1. Introduction](#vision-1-introduction)
- [2. Business Context](#vision-2-business-context)
  - [Background and Problem Statement](#vision-background-and-problem-statement)
  - [Needs](#vision-needs)
  - [Current Key Business Processes](#vision-current-key-business-processes)
- [3. Solution Vision](#vision-3-solution-vision)
  - [Vision Statement](#vision-vision-statement)
  - [Future Key Business Processes](#vision-future-key-business-processes)
  - [High-Level System Functionalities (Epics)](#vision-high-level-system-functionalities-epics)
- [4. Project Scope](#vision-4-project-scope)
- [5. System Context](#vision-5-system-context)
  - [Stakeholder Summary](#vision-stakeholder-summary)
  - [Operating Environment](#vision-operating-environment)

---

<a id="vision-1-introduction"></a>
## **1. Introduction**

This document presents the vision and scope of **SICEB: "Comprehensive Wellness Control and Records System"** (Sistema Integral de Control y Expedientes de Bienestar). Its objective is to establish an initial agreement with the direction of the **Private Wellness Integrated Clinics Network** for the development of a system that optimizes the management of the medical services offered across its **multiple branches (sucursales)**.

The document defines the business context, the current problem, the identified needs, the vision of the proposed solution, the high-level functionalities of the system, and its scope divided into incremental deliveries.

---

<a id="vision-2-business-context"></a>
## **2. Business Context**

<a id="vision-background-and-problem-statement"></a>
### **Background and Problem Statement**

The **Private Wellness Integrated Clinics Network** is a private health organization that provides various specialized medical services in the city. The clinic operates as a **medical teaching institution**, attending to patients while training medical residents (levels R1 to R4), across **multiple physical locations (branches)** that currently operate disjointedly.

**Medical Services Offered:**

1. General Consultation
2. Emergencies
3. Internal Medicine
4. Pediatrics
5. Gynecology and Obstetrics
6. Internal Clinical Laboratory
7. Internal Pharmacy
8. Electronic Clinical Record (to be implemented)
9. Management and assignment of medical personnel
10. Supervised internal training services

**Business Objectives:**

The clinic seeks to achieve the following strategic objectives:

- **Financial Management:** Efficiently record income and expenses per service, generate financial profitability reports.
- **Client Management:** Maintain centralized digital records with complete care history.
- **Inventory Management:** Rigorous control of medical supplies, materials, and medications.
- **Personnel Management:** Control over attending physicians and residents, registration of training activities.

**Current Problem:**

Currently, the clinic operates with **completely manual processes** that result in:

- **Operational Slowness:** Processes based on Word documents and face-to-face meetings.
- **Lack of Material Control:** No visibility of total inventory or material expenditure per service.
- **Fragmented Records:** Each service maintains its own paper templates; there is no consolidation of patient information across branches.
- **Inefficient Personnel Management:** Manual registration of training activities for residents and attending physicians.
- **Slow Financial Reports:** Manual generation of income and expense reports consumes valuable time.

<a id="vision-needs"></a>
### **Needs**

Below are the needs that the organization has and that would allow it to solve its problems:

|ID|Description of the Need|
|---|---|
|NEC-01|Control of material per service and general clinic material|
|NEC-02|Automated registration of requested, approved, and delivered supplies|
|NEC-03|Centralized digital client/patient record accessible by all medical services|
|NEC-04|Registration of workshops, trainings, and training activities for attending physicians and residents|
|NEC-05|Registration of economic income by service and patient type|
|NEC-06|Registration of economic expenses by service (expenses on supplies, materials, medications)|
|NEC-07|Automated generation of financial reports of income, expenses, and profitability|
|NEC-08|System access control by user with permissions differentiated by role|
|NEC-09|Generation of supply request reports (history, status, frequency)|
|NEC-10|Validation of resident restrictions according to level (R1, R2, R3, R4)|
|NEC-11|Control of regulated medications with complete traceability|
|NEC-12|Management of differentiated discounts for students (30%) and workers (20%)|
|NEC-13|Centralized management of multiple branches with consolidated reporting and inventory|

<a id="vision-current-key-business-processes"></a>
### **Current Key Business Processes**

The main business processes in their current state (AS IS) are as follows:

|Process ID|Process Name|Description and Steps|
|---|---|---|
|PROC-01|Material Request|**Description:** Service managers request supplies from the general administrator.<br/>**Steps:**<br/>1. Service manager creates request in Word format<br/>2. Manager sends request by email or physical delivery<br/>3. Administrator reviews request manually<br/>**Problems:** Slow, no traceability, no automatic notifications|
|PROC-02|Approve/Reject Material Request|**Description:** The administrator approves or rejects supply requests.<br/>**Steps:**<br/>1. Administrator reviews request<br/>2. Administrator decides to approve/reject indicating reasons<br/>3. Administrator notifies the manager in person or by email<br/>**Problems:** No historical record, no decision alerts|
|PROC-03|Deliver Materials|**Description:** The administrator delivers approved materials.<br/>**Steps:**<br/>1. Administrator contacts the manager in person<br/>2. Manager physically picks up material<br/>3. No formal delivery record<br/>**Problems:** No digital receipt, no delivery traceability|
|PROC-04|Register Material Usage|**Description:** Managers register supplies used with patients.<br/>**Steps:**<br/>1. After consultation, manager notes supplies in Word template<br/>2. Templates accumulate without consolidation<br/>**Problems:** No real-time visibility of consumption|
|PROC-05|Register Discarded Materials|**Description:** Registration of materials discarded due to expiration or other reasons.<br/>**Steps:**<br/>1. Manager records discarded materials in Word template<br/>2. No consolidation or reports<br/>**Problems:** Unmonitored losses, no expiration alerts|
|PROC-06|Record Creation|**Description:** Creation of medical record for patients.<br/>**Steps:**<br/>1. Manager creates own template (each service has different format)<br/>2. Completely manual registration<br/>3. Paper record stored in filing cabinet<br/>**Problems:** No consolidation between services, duplicate or fragmented information|
|PROC-07|Workshop Request|**Description:** Request to hold training workshops.<br/>**Steps:**<br/>1. Manager requests face-to-face meeting with administrator<br/>2. In meeting, explains reasons for the workshop<br/>3. No formal request record<br/>**Problems:** No request history, no follow-up|
|PROC-08|Approve/Reject Workshop|**Description:** Approval or rejection of requested workshops.<br/>**Steps:**<br/>1. After meeting, administrator notifies decision within person<br/>2. No record of approval/rejection reasons<br/>**Problems:** No traceability, no record of workshops held|
|PROC-09|Medical Attention|**Description:** Patient care in consultation.<br/>**Steps:**<br/>1. Patient arrives at reception without prior appointment<br/>2. Reception records data in notebook<br/>3. Physician/resident attends and records in template<br/>4. If medication prescribed, patient goes to pharmacy<br/>5. Pharmacy dispenses without digital prescription validation<br/>6. Payment at reception without automated system<br/>**Problems:** No digital record, no prescription validation, no resident restriction control|
|PROC-10|Lab Study Processing|**Description:** Request and processing of diagnostic studies.<br/>**Steps:**<br/>1. Physician gives paper order to patient<br/>2. Patient pays at reception<br/>3. Lab processes study<br/>4. Results delivered on paper<br/>5. Physician files results in physical record<br/>**Problems:** No digital result record, no sample traceability|

---

<a id="vision-3-solution-vision"></a>
## **3. Solution Vision**

<a id="vision-vision-statement"></a>
### **Vision Statement**

The **SICEB** system will be a **Hybrid Cloud / PWA (Progressive Web App)** application that will automate and centralize the management of information related to the business model of the **Wellness Integrated Clinics Network**.


The system will allow:

- **Consolidated electronic medical records** accessible from any branch
- Management of **inventories with automatic alerts** (materials, medications, reagents)
- **Digitized requests and approvals** (supplies, workshops)
- Control of **medical personnel with level restrictions** (R1-R4)
- **Automated financial reports** (income, expenses, profitability) per branch and consolidated
- **Critical validations** (prescriptions, stock, supervision)
- **Complete traceability** (controlled medications, dispensations, consultations)
- **Offline Operation:** Capability to operate critical modules (consultations, sales) without internet connection, synchronizing data automatically when connectivity is restored.

This system should **optimize time**, **improve operational control**, and **provide a better experience** to all involved (administrative staff, physicians, residents, patients).

<a id="vision-future-key-business-processes"></a>
### **Future Key Business Processes**

The business processes after implementing SICEB (TO BE) are as follows:

|Process ID|Process Name|Description and Steps|
|---|---|---|
|PROC-01-FUT|Digitized Material Request|**Description:** Automated supply request.<br/>**Steps:**<br/>1. Manager accesses SICEB and creates digital request<br/>2. SICEB automatically notifies the administrator<br/>3. Request is recorded with timestamp and traceability<br/>**Improvements:** Complete traceability, automatic notifications, searchable history|
|PROC-02-FUT|Digital Approval of Requests|**Description:** Approval/rejection with system record.<br/>**Steps:**<br/>1. Administrator reviews request in SICEB<br/>2. Approves/rejects with justification in system<br/>3. SICEB automatically notifies the manager<br/>4. Decision is permanently recorded<br/>**Improvements:** Reason recording, automatic notifications, request reports|
|PROC-03-FUT|Digital Material Delivery|**Description:** Delivery registration with digital receipt.<br/>**Steps:**<br/>1. Administrator records delivery in SICEB<br/>2. Manager confirms receipt in SICEB<br/>3. Inventory updates automatically<br/>**Improvements:** Digital receipt, automatic inventory update, traceability|
|PROC-04-FUT|Automatic Consumption Registration|**Description:** Registration of supplies used in consultations.<br/>**Steps:**<br/>1. During/after consultation, physician registers supplies in SICEB<br/>2. System updates inventory in real time<br/>3. System generates alerts if stock drops below minimum<br/>**Improvements:** Real-time visibility, automatic alerts, cost calculation per consultation|
|PROC-06-FUT|Electronic Medical Record|**Description:** Creation and update of consolidated digital record.<br/>**Steps:**<br/>1. Upon registering new patient, system creates unique record<br/>2. Each service adds information to the SAME record<br/>3. Record is IMMUTABLE (cannot be edited, only consultations added)<br/>4. Record accessible from any medical service<br/>**Improvements:** Total consolidation, no duplication, complete history, multi-service access|
|PROC-09-FUT|Digitized Medical Attention|**Description:** Care with system support.<br/>**Steps:**<br/>1. Patient arrives, reception searches/creates patient in SICEB<br/>2. System applies automatic discount by type (Stu 30%, Work 20%)<br/>3. Physician/resident accesses complete record in SICEB<br/>4. System validates resident restrictions (R1 cannot prescribe controlled meds)<br/>5. If prescribed, pharmacy validates prescription in SICEB before dispensing<br/>6. System verifies stock before dispensing<br/>7. Payment recorded in SICEB with digital receipt issuance<br/>**Improvements:** Consolidated record, automatic validations, restriction control, traceability|
|PROC-10-FUT|Digital Lab Processing|**Description:** Request and digital registration of studies.<br/>**Steps:**<br/>1. Physician requests study in SICEB<br/>2. System registers request and charges automatically<br/>3. Lab sees pending requests in SICEB<br/>4. Lab enters results (text) in SICEB<br/>5. Physician consults results in patient record<br/>**Improvements:** Complete traceability, results in record, no lost orders|
|PROC-11-FUT|Branch Provisioning|**Description:** Setup and configuration of a new physical branch.<br/>**Steps:**<br/>1. General Administrator registers new branch in SICEB (Name, Address, Phone)<br/>2. Admin assigns a Service Manager to the new branch<br/>3. Admin configures initial inventory load for the branch<br/>4. Branch becomes active in the global dashboard<br/>**Improvements:** Scalability, centralized control of expansion, instant operational readiness|
|PROC-12-FUT|Personnel Branch Rotation|**Description:** Enabling medical personnel to operate across different branches.<br/>**Steps:**<br/>1. Admin assigns multiple authorized branches to a Physician (or "Roving" status)<br/>2. Physician logs in and selects the "Current Branch" context for the session<br/>3. System filters inventory and patients for that specific location context<br/>4. Physician can switch active branch context without relogging if authorized<br/>**Improvements:** Flexibility for staff rotation, covering absences, resource optimization|

<a id="vision-high-level-system-functionalities-epics"></a>
### **High-Level System Functionalities (Epics)**

Below are the high-level functionalities (epics) that the solution will have:

|ID|Functionality Description|Priority|Associated Business Process|
|---|---|---|---|
|EP-01|**Authentication and Access Control:** The system must allow users to access via credentials and manage users with permissions differentiated by role (9 defined roles) and **Branch assignment**|High|All|
|EP-02|**General Inventory Management:** Administrator views inventory of ALL services; Managers see ONLY their service. Includes low stock, expiration, and temperature alerts|High|PROC-01, PROC-03, PROC-04, PROC-05|
|EP-03|**Supply Request and Approval:** Managers request, Administrator approves/rejects, system notifies decisions, managers register receipt|High|PROC-01-FUT, PROC-02-FUT, PROC-03-FUT|
|EP-04|**Workshop Request and Approval:** Managers/residents request workshops, Administrator approves/rejects, system notifies, attendance is recorded|High|PROC-07, PROC-08|
|EP-05|**Patient Registration:** Administrative staff and managers can register patients with classification (Student 30% disc, Worker 20% disc, External, Minor <18 years)|High|PROC-09-FUT|
|EP-06|**Medical Record Management:** Creation of UNIQUE record per patient (30+ fields), update via addition of consultations (IMMUTABLE), history consultation, patient search, attachments (PDF/images)|High|PROC-06-FUT, PROC-09-FUT|
|EP-07|**Pharmacy Management:** Validation of medical prescriptions, stock verification, dispensation registration, controlled medication control, separate payment|High|PROC-09-FUT|
|EP-08|**Laboratory Management:** Registration of study requests, result entry (text), reagent management, temperature/expiration control|High|PROC-10-FUT|
|EP-09|**Payment Registration:** Registration of payments with issuance of simple receipts and CFDI invoices, payment method registration, assignment to patient|High|PROC-09-FUT|
|EP-10|**Financial Reports:** Automatic generation of income reports (by service, concept, patient type), expenses (supply costs), profitability, trends|Medium|Todos|
|EP-11|**Medical Personnel Control:** Registration of attending physicians and residents (R1-R4), automatic validation of level restrictions, registration of training activities (NO academic evaluations), and **Multi-Branch Rotation Support** (assignment to one or multiple locations)|High|PROC-09-FUT, PROC-12-FUT|
|EP-12|**Material Usage Management:** Registration of supplies used in consultations, automatic inventory update, alert generation|Medium|PROC-04-FUT|
|EP-13|**Notification System:** Automatic notifications of requests created/approved/rejected, inventory alerts (stock, expiration, temperature)|Medium|PROC-01-FUT, PROC-02-FUT, EP-02|
|EP-14|**Security and Multi-tenancy:** Implement security scheme that allows segmented access (each service sees only THEIR information) and **Multi-branch support** (Administrator sees ALL branches, local managers see only THEIR branch)|High|All|
|EP-15|**History and Operations Reports:** Consultation of request history (supplies/workshops), discarded material reports, attended patient reports|Medium|All|
|EP-16|**Interoperability API:** REST API to allow integration with external systems (academic, insurers, etc.)|Medium|All|
|EP-17|**Offline Mode & Sync:** Local storage logic and background synchronization to allow operation without internet in critical modules|High|PROC-09-FUT, PROC-04-FUT|
|EP-18|**Branch Administration:** Module for the General Administrator to Create, Update, and Deactivate branches. Includes management of branch-specific settings (contact info, active status) and assignment of Head Manager.|High|PROC-11-FUT, EP-14|

---

<a id="vision-4-project-scope"></a>
## **4. Project Scope**

The project will be developed in **10 incremental deliveries**, prioritizing functionalities that add the most value to the business:

|Delivery Number|Main Topic|Epics IDs to Include|
|---|---|---|
|**Delivery 1.0**|Access and base security|EP-01|
|**Delivery 2.0**|Inventory and basic consultation|EP-02 (partial: inventory consultation)|
|**Delivery 3.0**|Supply request|EP-03, EP-13 (supply notifications)|
|**Delivery 4.0**|Workshop request|EP-04, EP-13 (workshop notifications)|
|**Delivery 5.0**|Patient registration|EP-05|
|**Delivery 6.0**|Medical records (core)|EP-06, EP-11 (personnel registration)|
|**Delivery 7.0**|Pharmacy and Laboratory|EP-07, EP-08|
|**Delivery 8.0**|Payments and basic reports|EP-09, EP-10 (partial: basic reports)|
|**Delivery 9.0**|Material usage and complete inventory|EP-12, EP-02 (complete with alerts)|
|**Delivery 10.0**|Advanced reports and final security|EP-10 (complete), EP-14, EP-15|

**Scope Limitations:**

- ✅ **Electronic billing (CFDI) included** in version 1.0. The system will support CFDI invoice generation in addition to simple receipts.
- ❌ **NO mobile applications included** in MVP. The system will be solely responsive web (PWA).
- ✅ **API for external integrations included** (Basic support for academic systems).
- ❌ **NO telemedicine included** (virtual consultations).
- ❌ **NO academic evaluations included** for residents, only participation registration in activities.
- ❌ **NO diagnostic imaging (DICOM/PACS) included**. Laboratory results are captured in text format only; medical images are not managed by the system.

---

<a id="vision-5-system-context"></a>
## **5. System Context**

<a id="vision-stakeholder-summary"></a>
### **Stakeholder Summary**

Below are the people who participate in relation to the development and operation of the system:

| Name | Description | Responsibilities |
| --- | --- | --- |
| **General Director** | Director of the Wellness Integrated Clinics Network | • Supervises strategic clinic operations<br/>• Reviews executive and financial reports generated by SICEB<br/>• Authorizes high-level decisions<br/>• Has access to ALL information of ALL services |
| **General Administrator** | Responsible for administrative and operational management | • Approves/rejects supply and workshop requests<br/>• Manages general inventory of all services<br/>• Generates administrative reports<br/>• Registers material deliveries<br/>• Receives notifications of new requests<br/>• Has access to inventory of ALL services |
| **Service Managers** | Coordinators of medical services (General Consultation, Pediatrics, Gynecology, etc.) | • Request supplies for THEIR service<br/>• Request training workshops<br/>• Register material usage in consultations<br/>• Manage patient medical records<br/>• Consult inventory of THEIR service only<br/>• Receive notifications of approvals/rejections |
| **Attending Physicians** | Specialist physicians with degree, senior staff | • Attend patients autonomously<br/>• Register consultations and diagnoses in records<br/>• Prescribe medications (includes controlled)<br/>• Request laboratory studies<br/>• Supervise resident activities<br/>• Do NOT have restrictions in the system |
| **Residents R4** | Physicians in 4th year of specialty | • Practically autonomous<br/>• Perform complex procedures<br/>• Prescribe controlled medications<br/>• Supervise R1, R2, and R3 residents<br/>• Participate in training workshops |
| **Residents R3** | Physicians in 3rd year of specialty | • Perform consultations without supervision<br/>• Minor procedures<br/>• Prescribe basic medications (NOT controlled)<br/>• Supervise R1 and R2 residents<br/>• System validates R3 level restrictions |
| **Residents R2** | Physicians in 2nd year of specialty | • Consultations with occasional supervision<br/>• Basic procedures<br/>• Limited medication prescription<br/>• System validates R2 level restrictions |
| **Residents R1** | Physicians in 1st year of specialty | • Consultations ONLY UNDER MANDATORY SUPERVISION<br/>• Can NOT prescribe controlled medications<br/>• Very basic procedures<br/>• System BLOCKS restricted actions for R1 |
| **Lab Personnel** | Specialized technicians in study processing | • Register received study requests<br/>• Process diagnostic studies<br/>• Enter results in text format<br/>• Manage reagent inventory<br/>• Register reagent temperature control |
| **Pharmacy Personnel** | Pharmacists and pharmacy assistants | • Dispense prescribed medications<br/>• Validate medical prescriptions in system<br/>• Verify stock before dispensing<br/>• Register controlled medication control<br/>• Charge for medications (separate from consultation) |
| **Admin / Reception Staff** | Reception and administrative support staff | • Register new patients in system<br/>• Manage medical appointments<br/>• Process consultation payments<br/>• Issue payment receipts<br/>• Attend patient ARCO requests<br/>• System applies automatic discounts |
| **Patients** | End users of medical services (Students 30% disc, Workers 20% disc, External, Minors <18) | • Receive medical care<br/>• Provide personal and medical information<br/>• Can request access to their record (ARCO rights)<br/>• Sign informed consents |
| **Emergency Personnel** | Physicians and medical staff assigned to the Emergency service | • Assign triage levels (Red/Yellow/Green) to incoming patients<br/>• Register emergency consultations in records<br/>• Coordinate urgent referrals to other services |

> **System User Definition:** A *System User* is any person who accesses SICEB through an authenticated account. Each user has a unique set of credentials (email and password), exactly one assigned role from the stakeholders above, and one or more authorized branches. Upon login, multi-branch users must select their active branch. Deactivating a user preserves all their historical records for audit purposes.

<a id="vision-operating-environment"></a>
### **Operating Environment**

#### **Architecture Overview**
The system adopts a **Hybrid Cloud (SaaS)** architecture to centralize information while ensuring operational continuity.

- **Cloud Server:** Hosts the central database, API, and main application. Accessible securely via the internet.
- **Branches (Sucursales):** Each physical location accesses the system via web browser (Desktop/Tablet).
- **Offline Support (PWA):** Workstations cache critical data locally to allow basic operation (consultation recording, sales) during internet outages. Data syncs automatically when connection is restored.
- **External Integrations:** Secure API points for future connections with insurers or academic systems.

#### **System Context Diagram**

```mermaid
graph TD

    subgraph External_World
        EXT[External Systems]
    end

    subgraph Service_Cloud
        SICEB[SICEB Cloud Server]
    end

    subgraph Branch_1 [Branch: Matriz]
        USR1[Medical & Admin Team]
    end

    subgraph Branch_N [Branch: Sucursal N]
        USRN[Medical & Admin Team]
    end

    subgraph End_Users
        PAC[Patients]
    end

    %% Connections
    USR1 <-->|HTTPS / Secure WebSocket| SICEB
    USRN <-->|HTTPS / Secure WebSocket| SICEB
    
    SICEB <-->|API REST| EXT
    PAC <-->|"Patient Portal (Future)"| SICEB

    %% Internal Descriptions
    note1[Users operate via PWA<br/>Offline Sync Support]
    note1 -.-> USR1
    note1 -.-> USRN
```

---

