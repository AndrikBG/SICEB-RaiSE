# US-006: Low Stock Alerts

## User Story

**As** Service Manager  
**I want** to see automatic alerts when inventory items are running low  
**So that** I can request restocking before running out of stock

---

## Conversation

**Q1: When is an item considered "running low"?**  
**A:** When the physically held quantity is equal to or less than the minimum level we have defined as safe for that item.

**Q2: Where do these alerts appear?**  
**A:** 
1. On the Manager's Main Dashboard (in a section dedicated to "Critical Items").
2. In the general inventory list, where these items are clearly highlighted (with yellow color or a warning icon).

**Q3: Does the system send emails?**  
**A:** Not at this stage. Alerts are solely visual within the application so the manager sees them while working.

**Q4: Is the minimum stock configurable?**  
**A:** Yes. For this version, the Manager can adjust the "Minimum Stock" of their items to adapt alerts to the reality and specific needs of their service.

**Q5: What happens if the quantity reaches 0?**  
**A:** The alert changes priority from "Low Stock" (Yellow) to "Out of Stock" (Red) and the system must give it greater visibility so it is attended to immediately.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Low stock alert visualization
```gherkin
Feature: Low Stock Alerts
  As Service Manager
  I want to quickly identify which items need restocking
  To avoid shortages in my service

  Background:
    Given the user "urgencies_manager" has logged in
    And the item "Sterile Gauze" has:
      | Current Quantity | 15  |
      | Minimum Stock    | 20  |
    And the item "Bandages 10cm" has:
      | Current Quantity | 50  |
      | Minimum Stock    | 20  |

  Scenario: Identify items with low stock in the list
    When navigates to the "Inventory" section
    Then the item "Sterile Gauze" is shown with a "Low Stock" alert indicator (Yellow)
    And the item "Bandages 10cm" is shown with status "OK" (Green/Normal)
```

### Scenario 2: Dashboard widget on main board
```gherkin
  Scenario: View summary of critical items on Dashboard
    Given there are 3 items with low stock in the "Urgencies" service
    When navigates to the "Main Dashboard"
    Then the "Inventory Alerts" widget shows the number "3" in yellow
    And clicking on the widget redirects to the inventory list filtered to show only problem items
```

### Scenario 3: Out of Stock Alert (Zero existence)
```gherkin
  Scenario: Item with zero existence must be priority
    Given the item "Adrenaline Ampoules" has quantity 0
    When navigates to the "Inventory" section
    Then the item is shown with "OUT OF STOCK" alert indicator (Red)
    And this item appears listed before items with low stock
```

### Scenario 4: Filter inventory by alert type
```gherkin
  Scenario: Filter critical items for quick management
    Given there are items in "OK", "Low Stock", and "Out of Stock" states
    When selects filter "Status: Critical"
    Then the list shows only "Out of Stock" and "Low Stock" items
    And does not show items with "OK" status
```

### Scenario 5: Disappearance of alert after restocking
```gherkin
  Scenario: Alert disappears automatically upon receiving supplies
    Given the item "Sterile Gauze" has "Low Stock" alert (Quantity: 15, Minimum: 20)
    When an inventory entry of 10 units is registered for "Sterile Gauze"
    Then the current quantity updates to 25
    And the "Low Stock" alert disappears
    And the item status changes to "OK"
```

### Scenario 6: Configure custom Minimum Stock
```gherkin
  Scenario: Manager adjusts alert level
    Given the item "Latex Gloves" has minimum stock of 100
    When the Manager changes the minimum stock to 50
    And the current quantity is 80
    Then the item DOES NOT show "Low Stock" alert (previously alert, now OK)
```
