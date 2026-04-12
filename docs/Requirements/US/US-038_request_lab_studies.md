# US-038: Request Laboratory Studies

## User Story

**As** Physician  
**I want** to request laboratory studies during a consultation  
**So that** diagnostic tests are ordered and the laboratory processes them

---

## Conversation

**Q1: Is there a catalog of studies?**  
**A:** Yes. The physician must not write "Blood Chemistry" by hand, they must select it from a catalog so the system knows what reagents to deduct and what price to charge.

**Q2: Can multiple studies be requested at once?**  
**A:** Yes, it is a studies "cart". I can ask for CBC, Urinalysis, and 27-element Chemistry in a single order.

**Q3: Does the system warn if fasting is required?**  
**A:** Ideally. Upon selecting the study, the system should show "Instructions for patient: 8-hour fasting".

**Q4: Can urgent studies be requested?**  
**A:** Yes, there must be a checkbox "Priority: URGENT" so the laboratory processes it first.

**Q5: Is the charge automatic?**  
**A:** Yes. Upon generating the request, it is sent to Reception/Cashier as a "Pending Payment Order". The patient must pay before going to the laboratory (US-039), except critical emergencies.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Lab order creation
```gherkin
Feature: Diagnostic Auxiliaries Request
  As Physician
  I want to order clinical analyses
  To confirm my diagnosis

  Background:
    Given physician is in consultation with "Ana López"

  Scenario: Request diabetic profile
    When selects "Fasting Glucose"
    And selects "Glycated Hemoglobin (HbA1c)"
    And adds instruction "Urgent for insulin adjustment"
    And saves request
    Then lab order "LAB-505" is generated
    And status is "Pending Payment"
```

### Scenario 2: Patient instructions visualization
```gherkin
  Scenario: Print request sheet
    When physician prints order
    Then document includes: "Requested studies"
    And for "Lipid Profile" prints: "12-hour fasting required"
    And for "Urinalysis" prints: "First morning urine"
```

### Scenario 3: Duplicate studies validation
```gherkin
  Scenario: Avoid double request same day
    Given an order for "CBC" exists for today
    When physician attempts to order another "CBC"
    Then system shows alert: "This patient already has an equal request for today. Do you wish to duplicate it?"
```

### Scenario 4: Priority Request (Urgencies)
```gherkin
  Scenario: Mark as Urgent
    When selects "Troponin I"
    And checks "URGENT" box
    And saves
    Then request appears in Laboratory with blinking RED badge
```

### Scenario 5: Study packages (Check-up)
```gherkin
  Scenario: Quick group selection
    When physician searches "Hepatic Profile"
    Then system automatically adds the 5 individual studies of the profile (AST, ALT, ALP, TB, DB)
    And allows removing one if not needed
```

### Scenario 6: Request cancellation
```gherkin
  Scenario: Error ordering study
    Given order was created 5 minutes ago
    And patient has not paid yet
    When physician decides to cancel "Urinalysis"
    Then system allows cancellation
    And debt disappears from Cashier
```
