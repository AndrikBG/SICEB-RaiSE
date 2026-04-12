# US-028: Search Patients

## User Story

**As** Physician or Reception Personnel  
**I want** to search patients by name, ID, or student/employee number  
**So that** I can quickly access their records and perform procedures

---

## Conversation

**Q1: By what exact criteria can I search?**  
**A:** By Full Name (or part of name), Record Number (internal ID), CURP, and ID/Employee Number (for university members).

**Q2: Does the system show partial results?**  
**A:** Yes. If I write "Juan", it must show all "Juan"s. Search must be flexible (partial match).

**Q3: What information does the results list show?**  
**A:** Minimum necessary to identify without opening the record: Name, Date of Birth/Age, Sex, and the key data used (e.g., ID Number).

**Q4: Can it search inactive/deceased records?**  
**A:** Yes, but they must appear clearly marked (e.g., in gray or with "Inactive" icon).

**Q5: What happens if there are many results?**  
**A:** The list is paginated (10-20 results per page) to not saturate the screen.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Successful search by name
```gherkin
Feature: Patient Search
  As clinical user
  I want to find a patient record
  To attend them

  Background:
    Given patients "Juan Pérez", "Juan López" and "Ana García" exist

  Scenario: Search by partial name
    When user enters "Juan" in search bar
    And presses Enter
    Then system shows 2 results
    And list includes "Juan Pérez" and "Juan López"
    And DOES NOT include "Ana García"
```

### Scenario 2: Exact search by Record ID
```gherkin
  Scenario: Search by unique ID
    Given patient "Ana García" has record ID "EXP-1005"
    When user searches "EXP-1005"
    Then system shows unique result: "Ana García"
    And allows opening her record directly
```

### Scenario 3: Search by School ID
```gherkin
  Scenario: Search student by ID
    Given patient "Pedro" has ID "A00123456"
    When user searches "A00123456"
    Then system finds "Pedro"
    And shows his ID number in result details
```

### Scenario 4: Search with no results
```gherkin
  Scenario: Patient not found
    When user searches "NonExistentName"
    Then system shows message "No patients found with that criteria"
    And suggests button "Register New Patient"
```

### Scenario 5: Inactive patient visualization
```gherkin
  Scenario: Find discharged patient
    Given patient "Luis" is marked as "Inactive"
    When user searches "Luis"
    Then system shows him in list
    But highlights him in GRAY
    And shows label "INACTIVE" next to name
```

### Scenario 6: Quick filter by multiple match
```gherkin
  Scenario: Smart search
    Given patient is named "Maria Jose" with CURP "MAJO90..."
    When user writes "MAJO90"
    Then system finds by CURP match
    When user deletes and writes "Jose"
    Then system finds by Name match
```
