# US-007: Expiration Alerts

## User Story

**As** Service Manager  
**I want** to see alerts for items close to expiration  
**So that** I can use them before they expire and minimize waste

---

## Conversation

**Q1: How far in advance does the system notify us about expiration?**  
**A:** The system has two alert levels:
- **Preventive Alert (Orange Color):** Activates 30 days before the supply expires, to give us time to use it.
- **Critical Alert (Red Color):** Activates on the day of expiration and remains so to indicate it should no longer be used.

**Q2: Does the system prevent using an item if it has expired?**  
**A:** The system will show a very clear and visible warning if you attempt to use or dispense something expired, but will not totally block the action. This is to allow flexibility in cases of extreme emergency or if there was an error registering the date, although a record will remain that the warning was ignored.

**Q3: Does this apply to all inventory?**  
**A:** No, exclusively for items requiring expiration control, like medications and reagents. Materials like stationery will not have these alerts.

**Q4: How do we see these alerts day-to-day?**  
**A:** In the inventory list there will be a specific "Expiration" column showing the date and a color icon according to status.

**Q5: Can we quickly see what is going to expire first?**  
**A:** Yes, there will be an option to sort or filter the list by "Expiring Soon", which will facilitate taking out the oldest first (FIFO - First In, First Out / FEFO - First Expired, First Out).

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Preventive alert for item close to expiration
```gherkin
Feature: PROFECO Expiration Alerts
  As Service Manager
  I want to identify supplies close to expiration
  To prioritize their use (FEFO system: First Expired, First Out)

  Background:
    Given the current date is "2026-06-01"
    And the following batches of "Paracetamol" exist:
      | Batch  | Quantity | Expiration Date | Expected Status    |
      | L-100  | 50       | 2026-06-15      | About to Expire (<30 days) |
      | L-200  | 100      | 2027-01-01      | OK                 |

  Scenario: Visualize items about to expire in the list
    When the user consults the inventory
    Then batch "L-100" is shown highlighted in ORANGE
    And shows an explanatory message "Expires in 14 days"
    And batch "L-200" is shown with normal status
```

### Scenario 2: Critical alert for expired item
```gherkin
  Scenario: Clearly identify expired items
    Given the current date is "2026-06-01"
    And a batch "L-OLD" exists with expiration date "2026-05-30"
    When the user consults the inventory
    Then batch "L-OLD" is shown highlighted in intense RED
    And shows the status "EXPIRED" prominently
    And the system suggests the option to "Write off"
```

### Scenario 3: Priority filter to take out old items first
```gherkin
  Scenario: Filter supplies to apply FEFO
    When the user applies the filter "Expiring Soon"
    Then the list shows only items with Orange or Red alerts
    And automatically orders them by expiration date ascending (what expires first appears at top)
```

### Scenario 4: Dispensation of item about to expire (Orange Alert)
```gherkin
  Scenario: Use item with preventive alert
    Given a user attempts to dispense from batch "L-100" (Orange Alert)
    When selects the item to dispense
    Then the system shows an informational warning: "Attention: This supply expires soon (14 days)"
    And allows proceeding with the operation without blocks
```

### Scenario 5: Attempt to dispense expired item (Red Alert)
```gherkin
  Scenario: Warning when attempting to use expired item
    Given a user attempts to dispense from batch "L-OLD" (Expired)
    When selects the item to dispense
    Then the system shows a CRITICAL WARNING: "DANGER: The selected supply IS EXPIRED"
    And requests explicit confirmation to proceed
    And if the user confirms, records the event in the security history
```

### Scenario 6: Management of multiple batches with different dates
```gherkin
  Scenario: Visualize multiple batches of the same product
    Given the product "Amoxicillin" has 3 batches with different dates
    When the user expands the details of "Amoxicillin"
    Then can see each batch individually
    And each batch shows its own expiration date and independent alert color
```
