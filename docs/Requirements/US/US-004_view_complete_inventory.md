# US-004: General Administrator Views Complete Inventory

## User Story

**As** General Administrator  
**I want** to see the complete inventory of ALL services  
**So that** I have total visibility of clinical resources

---

## Conversation
**Q1: What information should the inventory show?**  
**A:** For each item:
- Code/SKU
- Item name
- Category (medication, medical material, reagent, etc.)
- Service it belongs to
- Current quantity
- Unit of measure
- Minimum stock
- Expiration date (if applicable)
- Status (OK, Low Stock, About to Expire, Expired)

**Q2: How are items organized?**  
**A:** The default view is grouped by service. Also, filters by category, by status, and by service must be available.

**Q3: Can the Admin edit quantities directly?**  
**A:** No. They can only view the information. Changes in inventory come from:
- Approved requests (US-013)
- Pharmacy dispensation (US-034)
- Usage in consultation (US-052)

**Q4: Can the Admin add new items to the catalog?**  
**A:** Yes, but that functionality corresponds to a separate user story and is not included in this initial scope.

---
## Acceptance Criteria (Gherkin)

```gherkin
Feature: Global Inventory View for Administrator
  As General Administrator
  I want to see the complete inventory of all services
  To have full control of resources

  Background:
    Given the user "admin" (General Administrator) has logged in
    And the following items exist:
      | SKU   | Name             | Service    | Quantity | Min Stock | Status      |
      | MED01 | Paracetamol 500mg| Pharmacy   | 50       | 20        | OK          |
      | MED02 | Ibuprofen 400mg  | Pharmacy   | 15       | 20        | Low Stock   |
      | REA01 | Glucose Reagent  | Laboratory | 5        | 10        | Low Stock   |
      | MAT01 | Syringes 5ml     | Pediatrics | 100      | 30        | OK          |
      | MAT02 | Sterile Gauze    | Surgery    | 200      | 50        | OK          |

  Scenario: View complete inventory of all services
    When accesses "Inventory Management"
    Then can see items from "Pharmacy"
    And can see items from "Laboratory"
    And can see items from "Pediatrics"
    And can see items from "Surgery"
    And the total of items shown is 5

  Scenario: Inventory grouped by service
    When accesses "Inventory Management"
    Then items are grouped by service:
      | Service     | Item Count |
      | Pharmacy    | 2          |
      | Laboratory  | 1          |
      | Pediatrics  | 1          |
      | Surgery     | 1          |
    And each group shows the service name as header

  Scenario: Filter inventory by specific service
    When accesses "Inventory Management"
    And selects filter "Service: Pharmacy"
    Then shows only items from "Pharmacy"
    And shows 2 items (MED01, MED02)
    And DOES NOT show items from other services

  Scenario: Filter by status "Low Stock"
    When accesses "Inventory Management"
    And selects filter "Status: Low Stock"
    Then shows only items with quantity < minimum stock
    And shows 2 items (MED02, REA01)
    And each item shows alert visual indicator (⚠️)

  Scenario: View item details
    When accesses "Inventory Management"
    And clicks on item "Paracetamol 500mg"
    Then details panel is shown with:
      | Field               | Value               |
      | SKU                 | MED01               |
      | Name                | Paracetamol 500mg   |
      | Category            | Medication          |
      | Service             | Pharmacy            |
      | Current Quantity    | 50 units            |
      | Minimum Stock       | 20 units            |
      | Expiration Date     | 2025-12-31          |
      | Status              | OK                  |
      | Last Update         | 2026-01-30 14:23    |

  Scenario: Search item by name or SKU
    When accesses "Inventory Management"
    And enters "Paracetamol" in search bar
    Then shows only "Paracetamol 500mg"
    When clears search
    And enters "MED02" in search bar
    Then shows only "Ibuprofen 400mg"

  Scenario: Export complete inventory to Excel
    When accesses "Inventory Management"
    And clicks on "Export to Excel"
    Then file "Inventory_Complete_2026-01-31.xlsx" is downloaded
    And the file contains all items from all services
    And includes all visible columns

  Scenario: View alerts visually highlighted
    When accesses "Inventory Management"
    Then items in "Low Stock" are shown with yellow background
    And "Expired" items are shown with red background
    And "About to Expire" items (<30 days) are shown with orange background
    And "OK" items are shown with white background

  Scenario: Sort inventory by different columns
    When accesses "Inventory Management"
    And clicks on header "Quantity"
    Then items are sorted from lowest to highest quantity
    When clicks again on "Quantity"
    Then items are sorted from highest to lowest quantity
    When clicks on "Expiration Date"
    Then items are sorted by expiration date ascending

  Scenario: Inventory pagination with many items
    Given there are 150 items in inventory
    When accesses "Inventory Management"
    Then shows the first 50 items
    And shows pagination controls: "Page 1 of 3"
    When clicks on "Page 2"
    Then shows items 51-100
```
