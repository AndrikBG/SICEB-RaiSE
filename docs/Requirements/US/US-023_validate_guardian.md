# US-023: Validate Guardian Present for Minors

## User Story

**As** Reception Personnel  
**I want** the system to validate that a guardian is present for patients under 17 years old  
**So that** legal requirements and clinic child protection policies are met

---

## Conversation

**Q1: What is the age limit to require a guardian?**  
**A:** Under 17 years old (according to our new policy). Every patient from 0 to 16 years and 11 months requires a guardian. From 17 onwards (although legally minors until 18, for the clinic they are considered fit for simple consultation), the system is more flexible, but for <17 it is strict.

**Q2: What data do we need from the guardian?**  
**A:** Full name, relationship (father, mother, grandfather, etc.), and an official ID (INE/Passport).

**Q3: Does the system block registration if there is no guardian?**  
**A:** If the date of birth indicates less than 17 years, the system MUST block saving until Guardian fields are filled. **EXCEPTION:** In authorized "Special Cases", a supervisor can unlock registration without guardian (e.g., emergencies or emancipated minors/identified street situation).

**Q4: Can another minor be a guardian?**  
**A:** No. The guardian must be over 18 years old.

**Q5: Is this information saved in the record?**  
**A:** Yes, on the record cover it must clearly say "Minor - Responsible Guardian: [Name]".

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Minor registration requires guardian mandatorily
```gherkin
Feature: Child Protection
  As system
  I want to ensure every child has a responsible adult
  For legal compliance

  Background:
    Given current date is 2026

  Scenario: Detect minor under 17
    Given user registers patient of 16 years
    When fills patient data
    Then system automatically displays "Guardian Data" section
    And marks these fields as MANDATORY
```

### Scenario 2: Block save without guardian
```gherkin
  Scenario: Attempt to save minor without responsible adult
    Given is a patient of 16 years
    When user attempts to save leaving guardian section empty
    Then system prevents saving
    And shows error "Patient under 17 requires registering a responsible guardian"
```

### Scenario 3: Successful registration with guardian
```gherkin
  Scenario: Register minor with mother
    Given is a patient of 10 years
    When user fills child data
    And enters in Guardian:
      | Name        | Laura Martínez |
      | Relationship| Mother         |
      | Phone       | 5599887766     |
    And clicks Save
    Then system successfully registers patient
    And links "Laura Martínez" as main emergency contact
```

### Scenario 4: Exception for special cases (No Guardian)
```gherkin
  Scenario: Attention to minor in special situation without guardian
    Given is a patient of 15 years without guardian present
    When user activates checkbox "Special Case / No Guardian"
    Then system requests "Supervisor Key" to authorize
    When enters correct key
    Then allows saving record without guardian data
    And marks record with alert "MINOR WITHOUT GUARDIAN - SPECIAL CASE"
```

### Scenario 5: Guardian age validation (manual)
```gherkin
  Scenario: System asks to confirm guardian majority age
    When guardian is registered
    Then system shows mandatory checkbox: "[ ] I confirm guardian presented official ID and is of legal age"
    If not checked
    Then does not allow saving
```

### Scenario 6: Visualization in Record
```gherkin
  Scenario: Visual alert in record
    Given registered patient "Pepito" (14 years)
    When physician opens record
    Then visible label appears: "MINOR PATIENT (<17)"
```
