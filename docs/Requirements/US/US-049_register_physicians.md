# US-049: Register Medical Personnel

## User Story

**As** Administrator  
**I want** to register medical personnel (staff and Residents R1-R4)  
**So that** personnel are in the system and their correct clinical responsibilities are assigned

---

## Conversation

**Q1: What is the difference between registering a Staff Physician and a Resident?**  
**A:** Both are physicians, but the Resident requires specifying their level (R1, R2, R3, R4) as this will determine their restrictions in the system (e.g. blocking controlled prescriptions for R1). The Staff Physician has full privileges.

**Q2: Is registering Professional License mandatory?**  
**A:** Yes, it is an indispensable legal requirement so they can sign prescriptions and records. The system must not allow saving without license.

**Q3: Are they assigned a service upon creation?**  
**A:** Yes, all medical personnel must be linked to a Service (Pediatrics, Gynecology, etc.). This defines which inventories they see and which patients they manage mainly.

**Q4: Can a physician belong to two services?**  
**A:** Not in this version. They are assigned to their main service. If they rotate (resident case), Admin must update their service manually.

**Q5: What personal data do we need?**  
**A:** Full name, CURP, RFC, Institutional Email, Contact Phone, and University of origin (for HR file).

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Register Staff Physician (Specialist)
```gherkin
Feature: Medical Personnel Registration
  As Administrator
  I want to register physicians and residents
  To enable their operation in clinic

  Background:
    Given user "admin" has logged in
    And navigates to "User Management > Medical Personnel"

  Scenario: Staff Physician Registration
    When selects "New Physician"
    And enters data:
      | Name      | Dr. Juan Pérez        |
      | Type      | Staff Physician       |
      | License   | 12345678              |
      | Service   | Cardiology            |
      | Email     | jperez@hospital.mx    |
    And saves record
    Then system creates user account
    And assigns total prescription permissions
```

### Scenario 2: Register Resident R1 (Initial Level)
```gherkin
  Scenario: R1 Resident Registration with restrictions
    When selects "New Physician"
    And enters data:
      | Name      | Dr. Ana López         |
      | Type      | Resident              |
      | Grade     | R1                    |
      | License   | PENDING-PROCESS       |
      | Service   | Urgencies             |
    And saves record
    Then system creates account
    And configures security restrictions (controlled block)
    And marks profile with "Supervision Required"
```

### Scenario 3: Duplicate License Validation
```gherkin
  Scenario: Avoid personnel duality
    Given a physician with license "888888" already exists
    When attempts to register another physician with license "888888"
    Then system shows error "Professional license is already registered"
    And does not allow saving duplicate
```

### Scenario 4: Mandatory service assignment
```gherkin
  Scenario: Attempt to save without service
    When enters physician data
    But leaves "Service" field empty
    And attempts to save
    Then system marks "Service" field in red
    And shows message "Must assign a clinical service"
```

### Scenario 5: Resident Grade Update
```gherkin
  Scenario: Resident promotion from R1 to R2
    Given user "resident_ana" is currently R1
    When Admin edits profile
    And changes grade from "R1" to "R2"
    And saves changes
    Then system updates permissions automatically
    And records grade change in history
```

### Scenario 6: Medical personnel termination
```gherkin
  Scenario: Deactivate account of resigning physician
    Given physician "Dr. House" stops working
    When Admin changes status to "Inactive"
    Then user "drhouse" can no longer log in
    But previous prescriptions and notes remain unaltered in system (historical integrity)
```
