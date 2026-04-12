# US-009: Send Digital Supply Request

## User Story

**As** Service Manager  
**I want** to send a digital supply request  
**So that** the restocking process is faster and traceable

---

## Conversation

**Q1: Who do I ask for supplies?**  
**A:** The Central Warehouse (General Administrator).

**Q2: How do I know what to order?**  
**A:** The system shows me the catalog of authorized supplies for my service. I can see how much I have (Current Stock) and how much I need.

**Q3: Must I justify the order?**  
**A:** Yes, especially if I order unusual quantities. The "Observations/Justification" field is useful to explain ("Upcoming vaccination campaign", "Accidental spill", etc.).

**Q4: Can I save a draft?**  
**A:** Yes, I can build my order during the shift and send it at the end.

**Q5: How do I know they received it?**  
**A:** The status changes from "Draft" to "Sent/Pending Approval".

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Create standard request
```gherkin
Feature: Material Requisition
  As Service Chief
  I want supply of materials
  To operate my area

  Background:
    Given the user "chief_urgencies" is in "Request Supplies"

  Scenario: Normal order
    When selects "Medium Latex Gloves" -> Quantity: 10 boxes
    And selects "Syringes 5ml" -> Quantity: 100 pieces
    And adds comment: "Weekly replenishment"
    And clicks on "Send Request"
    Then the system generates Folio "SOL-2026-001"
    And notifies that the request was sent to Warehouse
```

### Scenario 2: Negative quantity validation
```gherkin
  Scenario: Typos
    When attempts to order "-5" boxes
    Then the system blocks the input
    And shows error "Quantity must be greater than 0"
```

### Scenario 3: Draft
```gherkin
  Scenario: Save for later
    When adds items to cart
    And clicks on "Save Draft"
    Then the request IS NOT sent to admin yet
    And remains saved in "My Requests" with status "Draft"
```

### Scenario 4: Verify available stock in warehouse (Optional)
```gherkin
  Scenario: Order out-of-stock item
    Given the Central Warehouse has 0 stock of "Gauze"
    When the user attempts to order "Gauze"
    Then the system shows warning: "Item out of stock in Central Warehouse"
    But allows sending it (as backorder/pending) or blocks it according to configuration
```

### Scenario 5: Cancel sent request
```gherkin
  Scenario: Regret
    Given sent the request 5 minutes ago
    And status is still "Pending"
    When selects "Cancel Request"
    Then status changes to "Cancelled by User"
    And disappears from Admin's pending list
```

### Scenario 6: History visualization
```gherkin
  Scenario: See past orders
    When consults history
    Then sees a list of all their requests
    And can see details of what was ordered and what was actually delivered
```
