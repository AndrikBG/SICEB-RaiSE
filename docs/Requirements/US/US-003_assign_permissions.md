# US-003: Assign Different Permission Levels

## User Story

**As** Administrator  
**I want** to assign different permission levels (Director, Admin, Manager, Physician, Resident, etc.)  
**So that** users only see information relevant to their role

---

## Conversation
**Q1: What specific permissions does each role have?**  
**A:** Permission Matrix:

| Module | Director | Admin | Manager | Physician | Resident R4 | Resident R3-R1 | Reception |
|--------|----------|-------|-----------|--------|--------------|-----------------|-----------|
| View ALL Inventory | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| View Own Inventory | N/A | N/A | ✅ | ✅ | ✅ | ✅ | ❌ |
| Request Supplies | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Approve Requests | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Register Patients | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| View Records | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (with supervision) | ❌ |
| Edit Records | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ (limited) | ❌ |
| Prescribe Medications | ❌ | ❌ | ✅ | ✅ | ✅ | Limited by level | ❌ |
| Prescribe Controlled Meds | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ |
| View Financial Reports | ✅ | ✅ | Only their service | ❌ | ❌ | ❌ | ❌ |
| Register Payments | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |

**Q2: Are permissions assigned automatically when creating a user?**  
**A:** Yes, when selecting the role in US-001, permissions are assigned automatically according to this matrix.

**Q3: Can "extra" permissions be given to a user?**  
**A:** Not in this version. Permissions are fixed by role. If a user needs more permissions, they must be assigned a different role that contains them.

**Q4: How do we implement multi-tenancy?**  
**A:** 
- For Managers, Physicians, and Residents, the system automatically filters information to show only that belonging to their service.
- The Administrator and Director do not have this filter and can see information from all services.
- The system validates permissions before performing any operation.

---
## Acceptance Criteria (Gherkin)

```gherkin
Feature: Role-Based Access Control (RBAC)
  As system
  I want to apply permissions according to the user's role
  To protect sensitive information and maintain multi-tenancy

  Background:
    Given the following users exist:
      | username  | role                  | service    |
      | director  | General Director      | N/A        |
      | admin     | General Administrator | N/A        |
      | encpedia  | Service Manager       | Pediatrics |
      | drmedico  | Attending Physician   | Medicine   |
      | resR4     | Resident R4           | Pediatrics |
      | resR1     | Resident R1           | Surgery    |
      | recep     | Reception Staff       | N/A        |

  Scenario: General Director can see inventory of ALL services
    Given the user "director" has logged in
    When accesses "Inventory Management"
    Then can see inventory of "Pediatrics"
    And can see inventory of "Medicine"
    And can see inventory of "Surgery"
    And can see "General Clinic Inventory"

  Scenario: Service Manager only sees inventory of THEIR service
    Given the user "encpedia" has logged in (Pediatrics)
    When accesses "Inventory Management"
    Then can see ONLY inventory of "Pediatrics"
    And CANNOT see inventory of "Medicine"
    And CANNOT see inventory of "Surgery"
    And the system automatically filters by serviceId = Pediatrics

  Scenario: Resident R1 CANNOT prescribe controlled medications
    Given the user "resR1" has logged in
    When accesses prescription form
    Then can select NON-controlled medications
    And controlled medications appear DISABLED
    And upon attempting to prescribe controlled shows: "Resident R1 not authorized"

  Scenario: Resident R4 CAN prescribe controlled medications
    Given the user "resR4" has logged in
    When accesses prescription form
    Then can select NON-controlled medications
    And can select controlled medications
    And can complete prescription without restrictions

  Scenario: Reception Personnel CANNOT see medical records
    Given the user "recep" has logged in
    When attempts to access "Medical Records"
    Then the system shows error "Access denied"
    And records unauthorized access attempt in audit log
    And shows NO data from the record

  Scenario: Attending Physician can see records from ANY service
    Given the user "drmedico" (Medicine) has logged in
    When searches for patient who was attended in "Pediatrics"
    Then can see the complete record
    And can see consultations registered by other services
    And can add new consultation from "Medicine"

  Scenario: Service Manager CANNOT approve supply requests
    Given the user "encpedia" has logged in
    When accesses "Supply Requests"
    Then can create new request
    And can see status of their requests
    But CANNOT see "Approve" or "Reject" button
    And DOES NOT have access to approvals screen

  Scenario: General Administrator can approve requests
    Given the user "admin" has logged in
    When accesses "Pending Supply Requests"
    Then can see requests from ALL services
    And can approve or reject any request
    And can add justification comments

  Scenario: System menu adapts to user role
    Given the user "resR1" has logged in
    Then the menu shows:
      | Visible Option              |
      | 📋 Patient List             |
      | 🩺 Medical Consultations    |
      | 💊 Prescriptions (limited)  |
      | 🧪 Request Lab Studies      |
      | 📦 Inventory My Service     |
    And the menu DOES NOT show:
      | Invisible Option            |
      | 👥 User Management          |
      | 📊 Financial Reports        |
      | ✅ Approve Requests         |
      | ⚙️ System Configuration     |

  Scenario: System validates permissions in backend (not just frontend)
    # Note: This scenario includes technical considerations for the development team.
    Given the user "recep" has logged in
    When attempts to make direct HTTP request: GET /api/records/123
    Then the backend validates the role from the JWT token
    And responds with code 403 Forbidden
    And message: "Your role does not have permissions for this operation"
    And records attempt in security log
```
