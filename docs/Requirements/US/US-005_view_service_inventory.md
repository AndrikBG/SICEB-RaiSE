# US-005: Service Manager Views ONLY Their Inventory

## User Story

**As** Service Manager  
**I want** to see ONLY the inventory of my service  
**So that** I can manage the resources of my area effectively

## Conversation 

**Q1: How does the system know which inventory to show the Manager?**  
**A:** The system identifies the service the Manager belongs to upon login and automatically filters the inventory to show only the items corresponding to their area.

**Q2: Can the Manager see inventory of other services even if read-only?**  
**A:** No. They must not have any visibility of other services. This is a strict requirement to maintain data separation between areas.

**Q3: What happens if a Manager tries to directly access information from another service?**  
**A:** The system detects that the user does not have permissions to view that information, denies access, and records the attempt in the security history.

**Q4: Does the Manager see the same columns as the Admin?**  
**A:** Yes, they see the same information but filtered: Code, Name, Quantity, Minimum Stock, Expiration Date, and Status.

**Q5: Can the Manager edit inventory quantities?**  
**A:** Not directly. They can only:
- View their inventory
- Request supplies (US-009)
- Confirm receipts (US-013)
The quantities are updated automatically by the system.

**Q6: Can the Manager export their inventory?**  
**A:** Yes, but only for their service. The exported file contains only the items from their area.

**Q7: If a physician belongs to a service, do they see that service's inventory?**  
**A:** Yes. Physicians and Residents assigned to a service see the same inventory as their Manager, in read-only mode.

**Q8: What does the Manager see if their service has no items yet?**  
**A:** The system shows a message indicating: "Your service does not yet have items in inventory. Contact the Administrator for initial configuration."

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Manager sees only their inventory
```gherkin
Feature: Inventory Visibility by Service
  As Service Manager
  I want to see only my service's inventory
  To manage my resources without accessing other areas' information

  Background:
    Given the following items exist in the system:
      | SKU   | Name             | Service    | Quantity |
      | PED01 | Tongue Depressor | Pediatrics | 500      |
      | PED02 | Thermometer      | Pediatrics | 10       |
      | ADM01 | Bond Paper       | Admin      | 100      |
      | CIR01 | Scalpel          | Surgery    | 50       |
    And the user "pediatrics_manager" has logged in with role "Service Manager" and service "Pediatrics"

  Scenario: Default inventory view
    When the user navigates to the "Inventory" section
    Then the system shows a list of items
    And the list contains the item "Tongue Depressor"
    And the list contains the item "Thermometer"
    But the list DOES NOT contain the item "Bond Paper"
    And the list DOES NOT contain the item "Scalpel"
```

### Scenario 2: Attempt to access another service's inventory
```gherkin
  Scenario: Blocking direct access to resources of another service
    When the user attempts to directly access the URL "/api/inventory/Surgery"
    Then the system responds with a 403 Forbidden error
    And shows a message "You do not have permissions to access this resource"
    And records the unauthorized access attempt in the audit log
```

### Scenario 3: Inventory export
```gherkin
  Scenario: Export inventory to Excel
    When the user clicks on the "Export Inventory" button
    Then an Excel file is downloaded
    And the file contains the rows corresponding to "Tongue Depressor" and "Thermometer"
    But the file DOES NOT contain rows for "Bond Paper" or "Scalpel"
```

### Scenario 4: Item details visualization
```gherkin
  Scenario: View full details of own item
    When the user selects the item "PED01" (Tongue Depressor)
    Then the system shows the full detail of the item:
      | Field            | Value            |
      | SKU              | PED01            |
      | Name             | Tongue Depressor |
      | Current Quantity | 500              |
      | Minimum Stock    | 100              |
      | Expiration Date  | N/A              |
```
