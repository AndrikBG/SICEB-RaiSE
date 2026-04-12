# US-041: Laboratory Enters Results

## User Story

**As** Laboratory Personnel  
**I want** to enter study results in text/numeric format  
**So that** physicians can review them and complete diagnosis

---

## Conversation

**Q1: Is it free text or structured fields?**  
**A:** Depends on the study.
- For **CBC**: We want numeric fields (Hemoglobin: [Field], Leukocytes: [Field]).
- For **Urinalysis**: Selection fields (Color: yellow/amber, Aspect: light/turbid).
- For **Cultures**: Large free text to describe findings.

**Q2: Does the system mark abnormal values?**  
**A:** Yes. If we define reference values (Range: 12-16), and I capture 10, it must turn bold/red or mark with an asterisk (*).

**Q3: Can I save partial and continue later?**  
**A:** Yes (Save Draft). But the physician only sees the result when I hit "Release/Publish".

**Q4: Is a PDF generated?**  
**A:** Yes. Upon releasing, the system generates the official PDF report with clinic header and Responsible Chemist digital signature.

**Q5: Can I attach files?**  
**A:** Yes, for studies generating graphs or images (histopathology), uploading an attached JPG or PDF is necessary.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Numeric results capture with references
```gherkin
Feature: Results Reporting
  As Analytical Chemist
  I want to transcribe equipment data
  To generate report

  Background:
    Given order "LAB-505" is in process

  Scenario: Glucose Capture
    When chemist opens result capture
    And in "Glucose" field enters "105"
    Then system shows reference range next to it (70-100 mg/dL)
    And marks value "105" as "HIGH" (H) visually
```

### Scenario 2: Results release
```gherkin
  Scenario: Publish complete study
    When finishes capturing all fields
    And clicks on "Validate and Publish"
    Then request status changes to "Completed"
    And results become visible to Physician in Record (US-042)
    And final PDF is generated
```

### Scenario 3: Post-release edit block
```gherkin
  Scenario: Guarantee report immutability
    Given result was already published
    When chemist attempts to change a value
    Then fields are blocked
    And requires special "Rectification" process to correct errors (with correction note)
```

### Scenario 4: Panic values (Critical)
```gherkin
  Scenario: Dangerous result alert
    When enters Platelets "20,000" (Very low)
    Then system shows CRITICAL ALERT: "PANIC VALUE - Notify physician immediately"
    And records that alert was shown
```

### Scenario 5: Descriptive text capture
```gherkin
  Scenario: Qualitative result
    When captures Urinalysis
    And selects Color: "Reddish"
    And Aspect: "Turbid"
    And writes in Observations: "Abundant bacteria+++"
    Then report saves text as is
```

### Scenario 6: Attach external evidence
```gherkin
  Scenario: Upload microscopy image
    When user selects "Attach File"
    And uploads "parasite_photo.jpg"
    Then image appends to end of PDF report
```
