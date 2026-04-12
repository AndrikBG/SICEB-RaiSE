# US-025: Add Consultation to Record

## User Story

**As** Physician  
**I want** to add a new consultation entry to a patient's existing record  
**So that** their medical attention is documented chronologically

---

## Conversation

**Q1: What are the minimum fields of a consultation note?**  
**A:** Subjective (Symptoms), Objective (Physical Exploration), Analysis (Diagnosis), and Plan (Treatment). SOAP format. Plus Vital Signs (which can pulled from US-030).

**Q2: Does diagnosis use ICD-10?**  
**A:** Yes, the system must have an ICD-10 catalog search to standardize diagnoses, although it must also allow free text diagnoses for preliminary cases.

**Q3: Can I leave a consultation "in draft"?**  
**A:** Yes, while the patient is in the office. But it is not considered "Legal" until "Finalize Consultation" is clicked.

**Q4: Is the exact time saved?**  
**A:** Yes, date and time of start and end (saved).

**Q5: Who signs the note?**  
**A:** The logged-in user. Their name and license number appear at the foot of the note automatically.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Basic consultation capture (SOAP)
```gherkin
Feature: Consultation Notes
  As Physician
  I want to document patient visit
  For clinical follow-up

  Background:
    Given physician has started new consultation for "Juan Pérez"

  Scenario: Filling SOAP note
    When enters "Headache" in Subjective
    And enters "Inflamed throat" in Objective
    And selects diagnosis "J00 - Common cold" (ICD-10)
    And enters "Rest and hydration" in Plan
    And clicks "Finalize Consultation"
    Then system saves note in history
    And marks consultation as "Finished"
```

### Scenario 2: ICD-10 Diagnosis Search
```gherkin
  Scenario: Diagnosis autocomplete
    When physician writes "diab" in Diagnosis field
    Then system displays catalog options:
      | E11 - Type 2 diabetes mellitus |
      | E10 - Type 1 diabetes mellitus |
    And allows selecting one
```

### Scenario 3: Author data
```gherkin
  Scenario: Automatic signature
    Given physician is "Dr. House" with license "123456"
    When finalizes consultation
    Then saved note shows at end: "Elaborated by: Dr. House - Lic. 123456"
    And current date and time
```

### Scenario 4: Save draft (Autosave)
```gherkin
  Scenario: Protection against accidental closure
    Given physician has written data without finalizing
    When attempts to leave screen or close browser (simulated)
    Then system shows alert "Do you wish to save a draft?"
    And if confirms, saves status "In Progress"
```

### Scenario 5: Mandatory fields validation
```gherkin
  Scenario: Attempt to finalize empty note
    When clicks "Finalize Consultation" without writing anything
    Then system marks "Reason for Consultation" and "Diagnosis" fields in red
    And shows error "Must complete minimum clinical data to close consultation"
```

### Scenario 6: Automatic vital signs integration
```gherkin
  Scenario: Pull previously captured signs
    Given nurse registered vital signs 10 minutes ago
    When physician opens new consultation
    Then Blood Pressure, Weight, and Temperature fields appear pre-filled with those values
    And allows physician to confirm or edit them
```
