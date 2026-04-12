# US-012: Manager Receives Decision Notification

## User Story

**As** Service Manager  
**I want** to receive automatic notifications when my request is approved or rejected  
**So that** I know the status of my order without having to ask or check manually constantly

---

## Conversation

**Q1: How do I find out if I was approved?**  
**A:** You get a notification in the system (bell icon).

**Q2: Does the notification say what happened?**  
**A:** Yes. "Your request SOL-001 has been APPROVED". If rejected, it says "REJECTED" and includes the reason written by the admin.

**Q3: If it was partial, does it tell me?**  
**A:** Yes. "Approved with modifications". You must enter to see the detail to know what was removed.

**Q4: When does it arrive?**  
**A:** Immediately after the Admin clicks save decision (US-011).

**Q5: Can I go pick up my things then?**  
**A:** The notification indicates the next step. E.g., "Go pick up at warehouse" or "Wait for delivery".

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Approval notice
```gherkin
Feature: Request Feedback
  As requester
  I want to know the response
  To plan my operation

  Background:
    Given my request "SOL-001" was pending

  Scenario: Positive response
    When the Admin approves the request
    Then I receive an internal notification: "Good news! Your request has been approved"
    And the status icon changes to Green
```

### Scenario 2: Rejection notice with reason
```gherkin
  Scenario: Negative response
    When the Admin rejects the request with reason "Budget exhausted"
    Then I receive an alert notification: "Request Rejected"
    And upon opening it, I clearly see the reason: "Budget exhausted"
```

### Scenario 3: Adjustment notification (Partial)
```gherkin
  Scenario: Modified approval
    When the Admin changes quantity from 50 to 20
    Then the notification says: "Request Partially Approved"
    And invites me to review the details of the changes
```

### Scenario 4: Internal message
```gherkin
  Scenario: Internal messaging backup
    When the decision is made
    Then it is recorded in the "My Messages" system history
    And contains the decision summary
```

### Scenario 5: Real-time dashboard update
```gherkin
  Scenario: Visual status change
    Given I see my request list
    When the Admin makes the decision on their computer
    Then my screen updates (or upon refresh) showing the new status
```

### Scenario 6: Next instructions
```gherkin
  Scenario: Collection flow
    Given the request was approved
    Then the notification includes text: "Your supplies are ready for collection at Central Warehouse. Present your folio SOL-001."
```
