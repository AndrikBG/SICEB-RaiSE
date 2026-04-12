# US-033: Validate Prescription before Dispensing

## User Story

**As** Pharmacy Personnel  
**I want** the system to validate that a prescription exists before dispensing and verify stock  
**So that** I do not dispense without a medical order and ensure inventory control

---

## Conversation

**Q1: Can I sell medicine without a prescription?**  
**A:** The system is designed for the internal clinic. Everything leaving this pharmacy must be linked to a Consultation/Prescription (US-031). If it is external public sale without consultation, it would be another process, but for now we assume internal flow: No Prescription = No Medication.

**Q2: What does the system validate upon supplying?**  
**A:** 
1. That the prescription exists and is valid.
2. That the selected medication corresponds to what was prescribed.
3. That there is sufficient stock in inventory.

**Q3: Does the system deduct from inventory at the moment?**  
**A:** Yes. Upon confirming delivery, it is subtracted from stock (US-005/006 update).

**Q4: Can a medication be changed for an equivalent (similar)?**  
**A:** Yes, but the system must record the change ("Substitution by bioequivalent") and the pharmacist must confirm it.

**Q5: What happens if I attempt to dispense the same prescription twice?**  
**A:** The system blocks. Once marked as "Fully Delivered", it does not allow restocking it.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Existence validation in prescription
```gherkin
Feature: Dispensation Control
  As security system
  I want to avoid unauthorized exits
  To protect inventory and health

  Background:
    Given prescription "REC-100" includes "Ibuprofen 400mg"

  Scenario: Dispense correct item
    When pharmacist scans barcode of "Ibuprofen 400mg" box
    Then system marks line as "Ready to deliver" (Green check)
    And verifies quantity matches request
```

### Scenario 2: Attempt to dispense non-prescribed medication
```gherkin
  Scenario: Supply error
    When pharmacist scans "Paracetamol" (not in prescription)
    Then system emits error sound
    And shows alert "THIS ITEM DOES NOT CORRESPOND TO PRESCRIPTION"
    And blocks product exit
```

### Scenario 3: Block due to lack of stock
```gherkin
  Scenario: Insufficient inventory
    Given prescription asks for 5 boxes of "Insulin"
    And inventory only has 2 boxes
    When attempts to process exit of 5
    Then system alerts "Insufficient stock (Available: 2)"
    And offers option "Partial Supply (2)"
```

### Scenario 4: Prevention of double supply
```gherkin
  Scenario: Prescription already supplied
    Given prescription "REC-100" already has status "Delivered"
    When someone attempts to process it again
    Then system shows error "This prescription was already supplied on [Date/Time]"
    And does not allow taking more supplies
```

### Scenario 5: Expiration validation upon dispensing (Cross-check)
```gherkin
  Scenario: FEFO Alert (First Expired, First Out)
    Given there is lot A expiring in a month and lot B expiring in a year
    When pharmacist scans lot B
    Then system suggests: "Alert: A lot (Lot A) with closer expiration exists. Using that one first is recommended."
    And asks confirmation to use Lot B anyway
```

### Scenario 6: Automatic inventory deduction
```gherkin
  Scenario: Kardex update
    Given item "Gauze" has 100 units
    When dispensation of 10 units is confirmed
    Then inventory updates to 90 units immediately
    And an exit movement type "Dispensation to Patient" is generated
```
