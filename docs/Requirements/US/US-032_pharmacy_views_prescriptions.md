# US-032: Pharmacy Views Patient Prescriptions

## User Story

**As** Pharmacy Personnel  
**I want** to view a patient's prescriptions  
**So that** I can dispense correct medications and mark them as delivered

---

## Conversation

**Q1: Does Pharmacy see the whole medical record?**  
**A:** No. For privacy, they only see the prescription (medications, dose, prescribing doctor). They don't see sensitive diagnostic notes or detailed clinical history, except what is necessary for dispensation.

**Q2: How do they know there is a pending prescription?**  
**A:** They have an "Orders Board" that updates in real time. When physician closes consultation, patient appears in "To Supply" list.

**Q3: Can they see old prescriptions?**  
**A:** Yes, they can consult dispensation history to see what was delivered before, but default view is "Today's/Pending".

**Q4: Can it be partially supplied?**  
**A:** Yes. If they prescribed 3 medicines and we only have 2, we supply 2 and prescription stays "Partially Supplied".

**Q5: How do they search for a specific prescription?**  
**A:** By Prescription Folio or Patient Name.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: New prescription receipt in real time
```gherkin
Feature: Pharmacy Queue
  As dispenser
  I want to know which patients await medicine
  To attend them in order

  Background:
    Given physician just finalized prescription "REC-100" for "Lucía Méndez"

  Scenario: Appearance on board
    When pharmacy personnel refreshes screen
    Then "Lucía Méndez" appears in "Pending Delivery" list
    And shows status "New"
    And shows emission time (1 min ago)
```

### Scenario 2: Prescription detail visualization
```gherkin
  Scenario: See what to supply
    When selects order for "Lucía Méndez"
    Then sees medication list:
      | Med                    | Quantity | Dose |
      | Paracetamol 500mg      | 1 box    | ...  |
      | Ambroxol Syrup         | 1 bottle | ...  |
    And sees brief taking instructions (to explain to patient if needed)
```

### Scenario 3: Supply vs Pending filter
```gherkin
  Scenario: Clean workspace
    Given there are 5 pending prescriptions and 20 delivered today
    When selects filter "Status: Pending"
    Then only sees the 5 orders requiring immediate attention
```

### Scenario 4: Diagnostic data privacy
```gherkin
  Scenario: Pharmacy does not see clinical notes
    When opens prescription detail
    Then sees doctor name and medications
    But DOES NOT see "Subjective/Objective" field of medical note
    And diagnosis only appears if legal requirement (e.g. antibiotics)
```

### Scenario 5: Patient identification at counter
```gherkin
  Scenario: Quick search
    Given patient arrives with ID
    When pharmacist searches by Name "Lucía Méndez"
    Then system finds active electronic prescription
    And allows starting supply process
```

### Scenario 6: Expired prescriptions
```gherkin
  Scenario: Very old prescription not supplied
    Given a prescription was issued 30 days ago and never picked up
    When pharmacist consults it
    Then appears marked as "EXPIRED" (depending on validity policy)
    And system warns re-medical assessment is required
```
