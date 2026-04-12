# US-024: Create Medical Record

## User Story

**As** Physician or Service Manager  
**I want** to create a new medical record when I register a patient  
**So that** their digital clinical history begins in an organized way

---

## Conversation

**Q1: Is the record created manually?**  
**A:** No, it is created automatically upon registering the patient (US-019), but this story defines the initial *structure* it must have.

**Q2: What sections must a new record have?**  
**A:** It must be born with empty sections ready:
1.  **Clinical History:** Hereditary-Family, Pathological, Non-Pathological.
2.  **Evolution Notes:** Where consultations will go.
3.  **Studies:** Lab and Image.
4.  **Vital Signs:** Graphs.
5.  **Prescriptions:** Prescription history.

**Q3: Who is the "owner" of that record?**  
**A:** The clinic. It does not belong to a specific physician. Any authorized physician can consult it.

**Q4: Is a folio assigned?**  
**A:** Yes, the record ID must be unique and unrepeatable.

**Q5: Must the Clinical History be captured immediately?**  
**A:** It is not mandatory at second 1, but the system must insist (via alerts or "Incomplete Profile" indicators) so the physician fills it in the first consultation.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Automatic structure generation
```gherkin
Feature: Record Structure
  As system
  I want to organize health information
  To facilitate clinical practice

  Background:
    Given patient "New User" was just registered

  Scenario: Creation of empty sections
    When physician opens record for first time
    Then sees tabs: "Clinical History", "Consultations", "Signs", "Prescriptions", "Studies"
    And all sections are enabled for writing
    And shows status "New Record - Clinical History Pending"
```

### Scenario 2: Unique Folio Assignment
```gherkin
  Scenario: Set clinical identifier
    When system confirms patient registration
    Then generates a Record Folio (e.g. 2026-00501)
    And shows it visibly in profile header
    And this folio cannot be manually modified
```

### Scenario 3: Initial access validation
```gherkin
  Scenario: Verify creation permissions
    Given a user "Receptionist"
    When attempts to access "Clinical History" section
    Then system blocks access (read-only demographic data)
    And shows "Clinical Access Restricted to Medical Personnel"
```

### Scenario 4: Pending Antecedents Alert
```gherkin
  Scenario: Remind filling clinical history
    Given new record without captured antecedents
    When physician starts consultation
    Then system shows yellow warning: "Missing Hereditary-Family and Pathological Antecedents"
    And offers direct access "Capture now"
```

### Scenario 5: Record Uniqueness
```gherkin
  Scenario: One patient, one record
    Given patient already has record "EXP-100"
    When system attempts internal processes
    Then never creates "EXP-101" for same patient ID
    And always reuses existing record
```

### Scenario 6: Creation Audit
```gherkin
  Scenario: Opening Log
    When record is created
    Then registered in audit: "Record Created - Date: [Today]"
    And document lifecycle begins
```
