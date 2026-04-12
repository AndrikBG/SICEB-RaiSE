# US-030: Register Vital Signs

## User Story

**As** Physician or Nurse (Health Personnel)  
**I want** to register vital signs during a consultation or pre-consultation  
**So that** patient health trends can be tracked and anomalies detected

---

## Conversation

**Q1: What vital signs do we capture?**  
**A:** Blood Pressure (Systolic/Diastolic), Heart Rate, Respiratory Rate, Temperature (°C), Weight (kg), Height (cm), and Oxygen Saturation (SpO2).

**Q2: Is BMI calculated automatically?**  
**A:** Yes. Upon entering Weight and Height, the system must calculate Body Mass Index automatically.

**Q3: Who captures them?**  
**A:** Generally the nurse in triage/somatometry area before patient enters with physician. Or the physician themselves during consultation.

**Q4: Are they graphed?**  
**A:** Yes, we want to see an evolution graph, specially for Weight and Blood Pressure, to see trends over time.

**Q5: Are there alerts for abnormal values?**  
**A:** It would be very useful. If temperature is > 38°C, turn red. If pressure > 140/90, hypertension alert.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Basic somatometry registration
```gherkin
Feature: Vital Signs
  As nursing personnel
  I want to register patient physical data
  To monitor their status

  Background:
    Given user is on "Vital Signs" screen for patient "Luis"

  Scenario: Register signs
    When enters:
      | Height (cm) | 170 |
      | Weight (kg) | 70  |
      | Temp (°C)   | 36.5|
      | HR (bpm)    | 75  |
      | RR (rpm)    | 18  |
      | BP (mmHg)   | 120/80 |
      | SpO2 (%)    | 98  |
    And saves record
    Then is stored with current date and time
```

### Scenario 2: Automatic BMI calculation
```gherkin
  Scenario: Get BMI
    When enters Weight "80" kg
    And enters Height "180" cm (1.80 m)
    Then system shows automatically:
      | BMI | 24.69 |
      | Category | Normal Weight |
```

### Scenario 3: Fever alert (Visual)
```gherkin
  Scenario: High temperature
    When enters Temperature "39.5"
    Then field highlights in RED
    And shows alert icon "Fever"
```

### Scenario 4: Blood Pressure Alert (Hypertension)
```gherkin
  Scenario: Hypertensive crisis
    When enters Blood Pressure "160/100"
    Then system detects values out of normal range
    And shows visible alert "Possible Hypertension Grade 2"
```

### Scenario 5: Trend graph
```gherkin
  Scenario: View weight evolution
    Given patient has 5 weight records in last year
    When physician selects "Graphs" view
    Then shows line graph of Weight vs Time
    And allows clearly seeing if patient gained or lost
```

### Scenario 6: Incomplete registration allowed
```gherkin
  Scenario: Only take pressure
    When nurse only measures Blood Pressure (e.g. quick check)
    And leaves Weight and Height empty
    And saves
    Then system allows saving partial record
    And BMI appears as "Not calculated" (no error)
```
