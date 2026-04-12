# US-066: Register Record Access Audit

## User Story

**As** system  
**I want** to register in an audit log who accessed which record and when  
**So that** traceability required by LFPDPPP and security norms is met

---

## Conversation

**Q1: What specific information must we register in each access?**  
**A:** We must capture: the user performing action, exact date and time, accessed patient/record ID, action type (Read, Edit, Print), and IP address from where connected.

**Q2: Can these records be deleted?**  
**A:** Never. Audit logs are immutable. Not even General Administrator should be able to delete them, as they are our legal evidence in case of incidents.

**Q3: Who can see these records?**  
**A:** Only General Director and General Administrator have permissions to consult audit history.

**Q4: How long must we keep this info?**  
**A:** By legal requirements, we must guarantee them for at least 5 years.

**Q5: Is it also registered if someone attempts to access and fails?**  
**A:** Yes, denied attempts (403 Forbidden) are even more important and must be marked with visual alert in report.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Automatic record view registration
```gherkin
Feature: Clinical Access Audit
  As security officer
  I want system to register every access to sensitive data
  To have forensic traceability

  Background:
    Given user "drmedico" (Staff Physician) has logged in
    And patient "Juan Pérez" with ID 1001 exists

  Scenario: Physician consults record
    When user opens record of patient 1001
    Then system shows patient information
    And system creates audit record with:
      | User      | drmedico            |
      | Resource  | Record 1001         |
      | Action    | READ                |
      | Date      | (Current Date/Time) |
      | Result    | SUCCESSFUL          |
```

### Scenario 2: Data modification registration
```gherkin
  Scenario: Physician adds evolution note
    When user saves new note in record 1001
    Then system saves medical note
    And system creates audit record with:
      | User      | drmedico            |
      | Resource  | Record 1001         |
      | Action    | WRITE               |
      | Detail    | New evolution note  |
```

### Scenario 3: Unauthorized access attempt
```gherkin
  Scenario: User without permissions attempts to view record
    Given user "recep_ana" (Reception) attempts to access "/api/records/1001"
    When system denies access
    Then system creates CRITICAL audit record with:
      | User      | recep_ana           |
      | Resource  | Record 1001         |
      | Action    | READ                |
      | Result    | DENIED (403)        |
      | Level     | SECURITY ALERT      |
```

### Scenario 4: Administrator consults logs by date
```gherkin
  Scenario: Filter audit logs by date range
    Given user "admin" has entered "Audit" module
    When selects date range "01/01/2026" to "31/01/2026"
    Then system shows all accesses registered in that period
    And allows exporting report to PDF
```

### Scenario 5: Specific user activity search
```gherkin
  Scenario: Investigate suspicious user activity
    Given user "admin" is in "Audit" module
    When filters by user "new_resident"
    Then system shows chronologically all actions of "new_resident"
    And highlights in red any denied access
```

### Scenario 6: Immutability validation
```gherkin
  Scenario: Attempt to delete audit records
    Given user "admin" selects an audit record
    Then NO button or option to "Delete" exists
    And if attempts to send delete request via API
    Then system rejects request with error "Operation not allowed: Immutable logs"
```
