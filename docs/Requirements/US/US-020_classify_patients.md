# US-020: Classify Patients and Apply Automatic Discounts

## User Story

**As** Reception Personnel  
**I want** to classify patients by type (Student/Worker/External) and have the system automatically apply the corresponding discount  
**So that** billing is consistent and automatic (30% Students, 20% Workers, 0% External)

---

## Conversation

**Q1: What exactly are the discounts?**  
**A:** 
- **Institution Students:** 30% discount.
- **Workers (Teachers/Admin):** 20% discount.
- **External (General Public):** 0% discount (pay full rate).

**Q2: Does the discount apply to everything?**  
**A:** It applies to Consultations and Medical Services. It *does not* necessarily apply to pharmacy medications (that is configured separately), but for this story let's assume it applies to services charged at reception.

**Q3: How do we validate if they are student or worker?**  
**A:** We must capture their ID number or employee number. The system should (ideally) validate against a school database, but for this scope, registering the current credential number is enough.

**Q4: Can the patient type perform be changed later?**  
**A:** Yes. An alumnus can become a graduate (External) or worker. Upon changing their classification in the profile, *future* charges will adjust. Past charges do not change.

**Q5: Can the cashier apply an extra manual discount?**  
**A:** Not in the normal flow. Discounts must be automatic by policy to avoid abuse. Only a Supervisor could authorize exceptions (another story).

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Student Patient Registration
```gherkin
Feature: Classification and Discounts
  As billing system
  I want to apply discounts according to regulations
  To ensure fair and correct charges

  Background:
    Given "General Consultation" service has base price of $200.00

  Scenario: Charge to Student
    Given patient "Juan" is classified as "Student"
    When Reception generates charge for "General Consultation"
    Then the system automatically applies 30% discount
    And subtotal is $200.00
    And discount is $60.00
    And total to pay is $140.00
```

### Scenario 2: Worker Patient Registration
```gherkin
  Scenario: Charge to Teacher Worker
    Given patient "Profe Jirafales" is classified as "Worker"
    When Reception generates charge for "General Consultation"
    Then the system automatically applies 20% discount
    And subtotal is $200.00
    And discount is $40.00
    And total to pay is $160.00
```

### Scenario 3: External Patient Registration
```gherkin
  Scenario: Charge to External
    Given patient "Mr. Barriga" is classified as "External"
    When Reception generates charge for "General Consultation"
    Then the system applies 0% discount
    And total to pay is $200.00
```

### Scenario 4: Classification change affects new charges
```gherkin
  Scenario: Student graduates (becomes External)
    Given patient "María" was "Student"
    When administrator updates profile to "External"
    And generates a NEW charge for "General Consultation"
    Then system charges full price ($200.00) without discount
```

### Scenario 5: Credential validation for discounts
```gherkin
  Scenario: Require ID for discount
    When user selects patient type "Student"
    Then "ID/Credential" field becomes MANDATORY
    If attempts to save empty
    Then system shows error "Must capture ID to apply student discount"
```

### Scenario 6: Clear breakdown on receipt
```gherkin
  Scenario: Receipt shows savings
    Given a consultation was charged to a student
    When payment receipt is printed
    Then receipt shows:
      | Concept  | Price   |
      | Consult  | $200.00 |
      | Stu Desc | -$60.00 |
      | Total    | $140.00 |
```
