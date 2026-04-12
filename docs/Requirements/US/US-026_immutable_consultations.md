# US-026: Immutable Consultations

## User Story

**As** Physician  
**I want** consultation entries to be immutable (not editable) once finalized  
**So that** medical records maintain legal integrity and reliability for audits

---

## Conversation

**Q1: What exactly does "immutable" mean?**  
**A:** That once the "Finalize Consultation" button is pressed, the text CANNOT be deleted or modified. It is a closed legal document.

**Q2: What if I made a typo or data error?**  
**A:** You must create a subsequent "Clarification Note" or "Erratum" that references the original note, but the original note remains as is. The system must allow adding these attached notes.

**Q3: Can the Administrator edit it?**  
**A:** No. No one. Not even the programmer (via database yes, but the application must not allow it). This gives confidence that no one altered the history.

**Q4: How much time do I have to edit before it locks?**  
**A:** While it is in "Draft" or "In Progress" status, it is fully editable. The lock occurs when changing the status to "Finalized".

**Q5: Do clarification notes also lock?**  
**A:** Yes, they follow the same logic. Once the clarification is saved, it cannot be edited either.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Edit block after finalizing
```gherkin
Feature: Clinical Integrity
  As legal system
  I want to protect historical information
  To avoid alterations

  Background:
    Given a consultation with status "Finalized" from yesterday exists

  Scenario: Attempt to edit closed note
    When the physician opens the consultation to view details
    Then all fields (Subjective, Objective, Plan) appear in READ ONLY mode
    And "Edit" or "Save" buttons do not exist
    And fields are locked (disabled)
```

### Scenario 2: Correction via subsequent note
```gherkin
  Scenario: Add erratum
    Given a finalized consultation with a weight error (says 80kg, was 70kg)
    When the physician selects option "Add Clarification Note"
    And writes: "Erratum: The correct weight is 70kg, not 80kg."
    And saves the note
    Then the original consultation MAINTAINS "80kg"
    And a visible attachment appears: "Clarification Note [Date]: Erratum..."
```

### Scenario 3: Draft is editable
```gherkin
  Scenario: Edit consultation in progress
    Given a consultation with status "In Progress" (not finalized)
    When the physician changes the diagnosis
    And saves changes
    Then the system updates information correctly (not yet immutable)
```

### Scenario 4: Deletion prohibition
```gherkin
  Scenario: Attempt to delete consultation from history
    Given the patient's history has 5 past consultations
    When the user looks for option to "Delete Consultation"
    Then the delete button DOES NOT exist in the interface
    And no consultation can be deleted under any circumstance
```

### Scenario 5: Violation attempt audit (Security)
```gherkin
  Scenario: Forced edit attempt via API
    Given a "hacker" user attempts to send POST /api/consultations/{finalized_id}/update
    When the backend receives the request
    Then checks that status is "Finalized"
    And rejects request with Error 409 Conflict ("Resource is immutable")
    And records incident in security log
```

### Scenario 6: Clear status visualization
```gherkin
  Scenario: Distinguish open from closed notes
    When the physician views the consultation list
    Then finalized notes have a "Closed Padlock" icon
    And draft notes have a "Pencil" or "Draft" icon
```
