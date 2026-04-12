# US-008: Reagent Temperature Alerts

## User Story

**As** Lab Personnel  
**I want** to register refrigerator temperatures in three shifts (morning, afternoon, and night) and receive alerts if it is out of range  
**So that** I ensure the cold chain and comply with logbook regulations

---

## Conversation

**Q1: What temperature range is "safe"?**  
**A:** Generally "Cold Chain" is 2°C to 8°C. The system must allow configuring the range per refrigerator.

**Q2: Is it automatic with sensors?**  
**A:** NO. For now, registration is MANUAL. Personnel must go to the physical thermometer, read the value, and write it in the system.

**Q3: When must they register it?**  
**A:** At least 3 times a day: Morning Shift, Afternoon Shift, and Night Shift. The system must mark if the shift registration was done or if it is pending.

**Q4: What happens if I enter a bad value?**  
**A:** If I write a value out of range (e.g., 9°C), the system must show an IMMEDIATE ALERT on screen and ask to register the corrective action mandatorily.

**Q5: Is the graph generated?**  
**A:** Yes, with the manually captured points, the trend is drawn.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Routine temperature capture (Morning Shift)
```gherkin
Feature: Temperature Logbook
  As Chemist
  I want to register temperature
  To comply with regulation

  Background:
    Given it is the Morning Shift (07:00 - 14:00)
    And "Refrigerator A" requires temperature from 2 to 8°C

  Scenario: Successful registration
    When the user enters the logbook
    And captures 5.0°C for "Refrigerator A"
    Then the system saves the record
    And marks the shift semaphore as GREEN (Completed)
```

### Scenario 2: Temperature out of range alert
```gherkin
  Scenario: Temperature excursion
    When the user captures 9.5°C
    Then the system shows a RED ALERT on screen: "Temperature out of range!"
    And automatically opens the "Incidence Registration" window
    And does not allow saving just the value without justifying the corrective action
```

### Scenario 3: Corrective action registration
```gherkin
  Scenario: Justify incidence
    Given the 9.5°C alert popped up
    When the user writes: "Door poorly closed, adjusted and verified descent to 7°C"
    Then the system saves the temperature (9.5°C) AND the corrective note
    And the record is marked in RED in the history for audit
```

### Scenario 4: Shift compliance audit
```gherkin
  Scenario: Shift without record (oversight)
    Given it is 23:00 and no one captured the Afternoon Shift temperature
    When the supervisor reviews the dashboard
    Then "Refrigerator A" shows a "MISSING AFTERNOON RECORD" alert
    And evidence of non-compliance remains
```

### Scenario 5: Manual point graph
```gherkin
  Scenario: Visualize trend
    When the user consults the monthly graph
    Then sees connected points (Morning-Afternoon-Night) for each day
    And lines visualize thermal stability
```

### Scenario 6: Equipment configuration
```gherkin
  Scenario: New equipment registration
    When the Admin registers "Ultra-Deep Freezer"
    And defines safe range from -80°C to -60°C
    And defines monitoring schedules
    Then the system enables the logbook for this new equipment
```
