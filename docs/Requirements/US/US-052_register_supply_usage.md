# US-052: Register Supply Usage in Consultation

## User Story

**As** Physician  
**I want** to register which supplies I used during a consultation  
**So that** inventory updates automatically and costs are tracked per patient

---

## Conversation

**Q1: Must I register every cotton ball?**  
**A:** No, that would be impractical. There are two types of supplies:
1.  **Direct charge/traceable:** Applied medications, special syringes, sutures, large bandages. These ARE registered one by one.
2.  **General expense (Bulk):** Cotton, alcohol, simple tongue depressors. These are prorated or deducted per "Consultation Kit" (each consultation deducts 1 virtual kit).
    *For this story, we focus on explicit registration of traceable materials.*

**Q2: How do I add them?**  
**A:** In Consultation screen, there is a "Materials Used" tab. I search and add (e.g. "Syringe 10ml - 1 pc").

**Q3: Are they deducted from my service inventory?**  
**A:** Yes. They are deducted from the stock of the office/service where I am attending.

**Q4: Is the patient charged?**  
**A:** Depends on configuration. Some materials are included in consultation cost, others are charged extra. The system must know which is which.

**Q5: What happens if I register nothing?**  
**A:** System assumes only general expense. But if I did a cure and didn't register gauze, there will be shrinkage in physical vs system inventory later.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Consumables registration
```gherkin
Feature: Material Expense
  As Physician
  I want to justify supply usage
  To keep inventory up to date

  Background:
    Given physician is finishing a consultation
    And used 1 pair of sterile gloves and 1 suture

  Scenario: Consumption capture
    When goes to "Materials" section
    And adds "Sterile Gloves" (1)
    And adds "Nylon Suture 3-0" (1)
    And finalizes consultation
    Then system deducts those items from Service inventory
```

### Scenario 2: Stock validation upon consuming
```gherkin
  Scenario: Consume what is not there (Logical error)
    When attempts to register "Plaster Bandage"
    But system says 0 stock
    Then allows registering it but throws WARNING: "Negative inventory generated. Please verify."
    (Because physically it WAS used, system must reflect reality even if logical stock was wrong)
```

### Scenario 3: Automatic charge to patient account
```gherkin
  Scenario: Billable material
    Given "Suture" is configured as "Billable"
    When registered in consultation
    Then a charge is added to patient account for suture cost
```

### Scenario 4: Material included in consultation
```gherkin
  Scenario: Non-billable material
    Given "Gloves" are configured as "Included"
    When usage is registered
    Then is deducted from inventory
    But NO extra charge generates for patient
```

### Scenario 5: Predefined kits (Favorites)
```gherkin
  Scenario: Load cure kit
    When physician selects "Basic Cure Kit"
    Then system automatically adds: 2 Gauzes, 1 Bandage, 1 Isodine
    And saves manual capture time
```

### Scenario 6: Traceability per patient
```gherkin
  Scenario: Cost audit
    When administrator reviews cost report
    Then can see exactly what materials were spent on patient "Juan Pérez" in today's visit
```
