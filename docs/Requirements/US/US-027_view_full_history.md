# US-027: View Complete Medical History

## User Story

**As** Physician  
**I want** to view the complete medical history of a patient from all services  
**So that** I have full context to make better treatment decisions

---

## Conversation

**Q1: Can I see other physicians' consultations?**  
**A:** Yes. In the multidisciplinary record, a physician must be able to see what other specialists prescribed or diagnosed. This avoids drug contraindications.

**Q2: How is the history ordered?**  
**A:** Chronologically reverse (most recent at top).

**Q3: Can I filter by service?**  
**A:** Yes, I should be able to see "Only Cardiology notes" or "Only Pediatrics notes" if I want to focus, but defaults to "All".

**Q4: Are lab studies seen in the timeline?**  
**A:** Yes, ideally the timeline integrates consultations and study results to see full evolution.

**Q5: Is there a quick summary?**  
**A:** The main view must be a "Timeline" type summary (Date - Specialty - Diagnosis). Clicking on an entry expands the full detail.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Unified chronological view
```gherkin
Feature: Integral Clinical History
  As treating Physician
  I want to know patient antecedents
  To avoid medical errors

  Background:
    Given the patient has been attended in "Urgencies", "Internal Medicine" and "Nutrition"

  Scenario: View full timeline
    When the physician accesses "History" tab
    Then sees a list ordered by descending date
    And the list contains entries from all 3 services
    And each entry shows: Date, Service, Physician and Main Diagnosis
```

### Scenario 2: View other specialist's note detail
```gherkin
  Scenario: Consult note from other specialist
    Given the physician is "Cardiologist"
    And a previous note from "Pulmonology" exists
    When clicks on the Pulmonology note
    Then full content displays (SOAP)
    But in Read-Only mode (cannot edit it, see US-026)
```

### Scenario 3: Filtering by service
```gherkin
  Scenario: Focus on specialty
    When selects filter "Service: Nutrition"
    Then the list hides Urgencies and Internal Medicine notes
    And only shows nutritional history
```

### Scenario 4: Privacy on sensitive notes (Gynecology/Psychiatry) - Exception
```gherkin
  Scenario: Confidential Notes (Optional by configuration)
    Given a note marked as "Confidential" exists (e.g. Psychiatry)
    When a physician from another area (e.g. Dermatology) attempts to view it
    Then the system shows "Reserved Note - Requires explicit permission"
    # Note: This scenario depends on clinic privacy policy.
    # For MVP we assume total visibility, but leave this scenario prepared.
```

### Scenario 5: Visual service identification
```gherkin
  Scenario: Color code by area
    When visualizes timeline
    Then Urgencies notes have a RED badge
    And Outpatient notes have BLUE badge
    And Hospitalization notes have GREEN badge
    To facilitate quick visual scanning
```

### Scenario 6: Progressive loading (Performance)
```gherkin
  Scenario: Very long history
    Given a patient with 10 years of history (500 notes)
    When opens history
    Then initially loads last 20 notes
    And upon scrolling to end, loads next 20 (Infinite Scroll)
    To not freeze the browser
```
