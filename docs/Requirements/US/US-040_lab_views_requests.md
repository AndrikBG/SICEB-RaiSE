# US-040: Laboratory Views Pending Requests

## User Story

**As** Laboratory Personnel  
**I want** to view pending study requests  
**So that** I know what tests to process and organize sample collection

---

## Conversation

**Q1: Which patients appear in my list?**  
**A:** Only those who already have the request made AND (very important) have already paid or have credit/urgency authorization. We do not want to process samples if they haven't passed through cashier.

**Q2: How do I know which tubes to prepare?**  
**A:** The system must tell me. If order is "CBC + Chemistry", it must indicate: "1 Purple Tube (EDTA) + 1 Red/Yellow Tube (Serum)".

**Q3: Can I filter by area?**  
**A:** Yes. The laboratory can be divided into Hematology, Microbiology, etc. I must be able to filter my work area.

**Q4: Is a label generated for the sample?**  
**A:** Yes, the system must allow printing labels with barcodes to stick on tubes and avoid confusion.

**Q5: What do I do if the sample is insufficient/hemolyzed?**  
**A:** You must be able to mark the request as "Rejected - Inadequate Sample" so reception notifies patient or nursing to repeat collection.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Sample collection worklist
```gherkin
Feature: Lab Work Management
  As phlebotomist
  I want to know who to stick
  To take correct samples

  Background:
    Given patient "Pedro" paid his "CBC" order

  Scenario: Patient ready in waiting room
    When lab personnel consults "Patients to Attend"
    Then "Pedro" appears in list
    And status is "Ready for Sample Collection"
    And shows instructions: "Purple Tube"
```

### Scenario 2: Urgency filtering
```gherkin
  Scenario: Prioritize urgencies
    Given there are 10 routine patients and 1 emergency
    When analyst opens main board
    Then urgent request appears AT TOP of list
    And has different background color (Red)
```

### Scenario 3: Barcode label generation
```gherkin
  Scenario: Sample identification
    When phlebotomist confirms patient arrival
    And clicks on "Print Labels"
    Then system generates labels with: Patient Name, Date, Tube Type and Unique Barcode
```

### Scenario 4: Sample rejection
```gherkin
  Scenario: Problem with sample quality
    When analyst receives tube
    But notices blood is coagulated
    Then marks study as "Sample Rejected"
    And enters reason "Coagulated sample"
    And system changes status to "Requires New Collection"
    And notifies Reception/Nursing
```

### Scenario 5: Sample collection log
```gherkin
  Scenario: Confirm collection done
    When phlebotomist finishes drawing blood
    And clicks on "Sample Taken"
    Then date/time is recorded (Collection Time)
    And patient disappears from "To Attend" list
    And request moves to "In Analytical Process" list
```

### Scenario 6: Payment validation (Prerequisite)
```gherkin
  Scenario: Patient attempts to pass without paying
    Given physician made order but patient hasn't paid
    When patient arrives at lab window
    Then system DOES NOT show order in "To Attend" list
    And if searched manually, status appears "Blocked - Payment Missing"
```
