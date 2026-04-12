# US-064: Configure Medical Service Tariffs

## User Story

**As** Administrator  
**I want** to configure medical service tariffs specifying base price  
**So that** system calculates charges correctly and automatically

---

## Conversation

**Q1: Who defines service prices?**  
**A:** Only General Administrator or Director have permissions to create or modify price lists.

**Q2: Can free services be handled?**  
**A:** Yes, it is possible to assign $0.00 price for certain public services or health campaigns, and system must process them normally (generating $0 receipt).

**Q3: What happens if we change a price today? Does it affect past charges?**  
**A:** No. Change only applies for new charges generated from change moment onwards. Historical records must maintain price they had when charged.

**Q4: Does price include taxes?**  
**A:** Yes, configured prices are final.

**Q5: Can we have different tariffs for same service (e.g. employee vs public tariff)?**  
**A:** Base tariff is unique per service. Adjustments for employees or students are handled via automatic discounts (see US-020), not creating multiple tariffs for same thing.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Register new service tariff
```gherkin
Feature: Tariff Management
  As Administrator
  I want to define service costs
  To keep price catalog updated

  Background:
    Given user "admin" has logged in
    And navigates to "Tariff Configuration"

  Scenario: Create tariff for new service
    When clicks "New Tariff"
    And enters data:
      | Service    | Nutrition Consultation |
      | Code       | NUT-001                |
      | Price      | 350.00                 |
    And saves record
    Then service "Nutrition Consultation" appears available for charging with price $350.00
```

### Scenario 2: Update existing service price
```gherkin
  Scenario: Price increase due to inflation
    Given service "General Consultation" exists with current price $200.00
    When user edits service "General Consultation"
    And changes price to $250.00
    And saves changes
    Then system updates price for future charges
    And shows confirmation message "Tariff updated successfully"
```

### Scenario 3: Non-negative price validation
```gherkin
  Scenario: Attempt to assign negative price
    When user attempts to create tariff with price "-50.00"
    Then system shows error "Price cannot be negative"
    And does not allow saving record
```

### Scenario 4: Free service configuration
```gherkin
  Scenario: Configure free vaccination campaign
    When user creates service "Flu Vaccine Application"
    And enters price "$0.00"
    And saves record
    Then system accepts price
    And upon charging this service, total to pay is $0.00
```

### Scenario 5: Tariff search in catalog
```gherkin
  Scenario: Search price of specific service
    Given 50 services exist registered
    When user searches "Dental" in search bar
    Then system filters list
    And shows "Dental Cleaning - $400.00" and "Dental Extraction - $600.00"
```

### Scenario 6: Integration with billing module
```gherkin
  Scenario: Reception visualizes updated price
    Given Admin just changed "Cure" price to $150.00
    When user "reception" selects "Cure" to charge patient
    Then system automatically loads $150.00 price
    And price field appears blocked (not editable by reception)
```
