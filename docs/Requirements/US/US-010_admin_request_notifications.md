# US-010: Admin Receives Request Notifications

## User Story

**As** General Administrator  
**I want** to receive automatic notifications when supply requests are sent  
**So that** I can review them promptly and not delay supply

---

## Conversation

**Q1: Where do I see notifications?**  
**A:** In the top bar, "Bell" icon with a red counter. And on the main Dashboard, a "Pending Tasks" widget.

**Q2: Do I get an external message?**  
**A:** No. For now, all notifications are internal within the platform (In-App).

**Q3: What info must the quick notification have?**  
**A:** "Service", "Requesting User", "Date/Time", and "Priority".

**Q4: If I have 50 requests, how do I organize myself?**  
**A:** They should be ordered by date (FIFO: First In, First Out) to attend to those who asked first. Or filter by "Urgent".

**Q5: Do they disappear on their own?**  
**A:** No, they disappear (or are marked read) only until I enter to manage the request (Approve/Reject).

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Immediate notification receipt
```gherkin
Feature: Notification Center
  As Administrator
  I want to know what happens
  To act fast

  Background:
    Given Admin is logged into the system
    And has no pending notifications

  Scenario: New incoming request
    When the Urgencies user sends request "SOL-001"
    Then Admin's notification counter changes to "1"
    And a popup notice (Toast) appears: "New request from Urgencies"
```

### Scenario 2: Dashboard Widget
```gherkin
  Scenario: Pending tasks
    Given there are 3 requests awaiting approval
    When Admin enters Dashboard
    Then "Pending Requests" widget shows number "3"
    And lists the 3 requests with their age (e.g., "2 hours ago")
```

### Scenario 3: Direct access from notification
```gherkin
  Scenario: Quick navigation
    When clicks on the bell notification
    Then the system redirects directly to request "SOL-001" details
    To proceed with its review (US-011)
```

### Scenario 4: Visual notification
```gherkin
  Scenario: Screen warning
    When request is generated
    Then a temporary banner is shown in the corner of Admin's screen
```

### Scenario 5: Mark as read
```gherkin
  Scenario: Notification clearing
    Given has active notification
    When enters to review request
    Then notification is automatically marked as READ
    And red counter decreases
```

### Scenario 6: Urgency notification
```gherkin
  Scenario: Priority order
    Given Urgencies marked their order as "High Priority"
    When notification arrives
    Then it is shown with a "Fire" icon or Red color
    And is positioned at top of list
```
