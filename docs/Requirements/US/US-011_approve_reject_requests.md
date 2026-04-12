# US-011: Approve or Reject Supply Requests

## User Story

**As** General Administrator  
**I want** to approve or reject supply requests with justification  
**So that** decisions are documented and spending is controlled

---

## Conversation

**Q1: Can I partially approve?**  
**A:** Yes. If they ask for 50 boxes and I only want to give them 20 (because there is little stock or they ask for too much), I can edit the "Approved Quantity" before confirming.

**Q2: Is justification mandatory when rejecting?**  
**A:** Yes. If I reject or modify the quantity, I must write why (e.g., "Budget exceeded", "No stock"). When approving as is, justification is optional.

**Q3: What happens to the inventory when I approve?**  
**A:** It is set aside (reserved) from the Central Warehouse, but is not permanently deducted until marked as "Delivered" (US-013, outside this phase but part of the flow). Or, it is deducted from Warehouse and moves to "In Transit". Let's assume that approving generates the exit order.

**Q4: Can I reject line by line?**  
**A:** Yes. I can approve the Gloves but reject the Syringes in the same request.

**Q5: Is there a record of who approved?**  
**A:** Always. User, Date, and Time of the decision.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Total approval
```gherkin
Feature: Supply Management
  As Administrator
  I want to authorize orders
  To supply the areas

  Background:
    Given reviewing request "SOL-001" asking for 10 Boxes of Gloves

  Scenario: Authorize all
    When the Admin checks there is sufficient stock
    And clicks on "Approve Complete Request"
    Then the status changes to "Approved"
    And the warehouse exit order for 10 Boxes is generated
```

### Scenario 2: Quantity modification (Partial Approval)
```gherkin
  Scenario: Budget cut
    Given they ask for 50 "Disposable Gowns"
    But only 30 remain in warehouse
    When the Admin changes "Approved Quantity" to 30
    And adds note: "Partial supply due to lack of stock"
    And confirms approval
    Then the request is marked as "Partially Approved"
    And the requester is notified of the change
```

### Scenario 3: Total rejection with justification
```gherkin
  Scenario: Deny order
    When the Admin decides the order is unnecessary
    And clicks on "Reject"
    Then the system demands a reason
    When writes "Duplicate request, supplied yesterday"
    And confirms
    Then the status changes to "Rejected"
    And the process ends there
```

### Scenario 4: Decision audit
```gherkin
  Scenario: Traceability
    When the request is approved
    Then the system saves: "Approved by: Admin Juan, Date: [Today]"
    So there are no doubts about who authorized the exit
```

### Scenario 5: Stock validation upon approving
```gherkin
  Scenario: Attempt to give what is not there
    Given warehouse has 5 pieces
    When the Admin attempts to manually approve 10 pieces
    Then the system shows error "You cannot approve more than you have in stock (Avail: 5)"
```

### Scenario 6: Irreversible rejection
```gherkin
  Scenario: Change of mind
    Given a request was "Rejected"
    When the Admin attempts to "Reactivate" it
    Then the system DOES NOT allow it (must create a new request)
```
