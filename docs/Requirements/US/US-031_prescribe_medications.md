# US-031: Prescribe Medications

## User Story

**As** Physician  
**I want** to prescribe medications during a consultation  
**So that** the prescription is registered and available for pharmacy immediately

---

## Conversation

**Q1: How does the physician search for medications?**  
**A:** By trade name or active substance. The system must autocomplete from internal catalog.

**Q2: What data must be indicated for each medication line?**  
**A:** Medication, Dose (e.g. 500mg), Frequency (every 8 hours), Duration (for 5 days), and Route of administration (Oral, IM, IV).

**Q3: Does the system calculate total quantity to supply?**  
**A:** No.

**Q4: Can they prescribe something we don't have in inventory?**  
**A:** Yes. The physician prescribes what the patient needs. If Pharmacy doesn't have it, it's another problem (external supply), but clinical need stays registered.

**Q5: Can previous prescriptions be repeated?**  
**A:** Yes, for chronic patients, there should be a button "Copy last prescription" to expedite.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Standard prescription
```gherkin
Feature: Electronic Medical Prescription
  As Physician
  I want to indicate treatment
  To cure patient

  Background:
    Given physician is in active consultation with "Juan Pérez"

  Scenario: Add medication
    When searches "Paracetamol"
    And selects "Paracetamol 500mg Tablets"
    And indicates: "1 tablet every 8 hours for 3 days" (Oral)
    And clicks "Add"
    Then medication adds to current prescription list
    And system calculates suggested quantity: 9 tablets
```

### Scenario 2: Allergy Warning (Medication-Patient Interaction)
```gherkin
  Scenario: Patient allergic to Penicillin
    Given patient has registered allergy to "Penicillin"
    When physician attempts to prescribe "Amoxicillin"
    Then system shows RED ALERT: "CONTRAINDICATION: Patient allergic to Penicillins"
    And asks confirmation and justification to proceed
```

### Scenario 3: Printed/Digital prescription generation
```gherkin
  Scenario: Finalize prescription
    Given 3 medications have been added
    When physician finalizes consultation
    Then a unique prescription folio is generated
    And information is sent to Pharmacy module instantly
    And printable PDF with physician digital signature is generated
```

### Scenario 4: Controlled medication prescription (Antibiotics/Psychotropics)
```gherkin
  Scenario: Antibiotic validation
    When prescribes "Ceftriaxone" (Antibiotic IV)
    Then system marks medication as "Requires Control"
    And forces physician to capture associated diagnosis (according to norm)
    And in printout, highlights data required by COFEPRIS/Authority
```

### Scenario 5: Copy chronic treatment
```gherkin
  Scenario: Recurrent hypertensive patient
    Given patient has prescription from a month ago (Losartan)
    When physician selects "Repeat last prescription"
    Then medications load with same dose and frequency
    And allows editing them before saving
```

### Scenario 6: Dosage fields validation
```gherkin
  Scenario: Forget frequency
    When selects "Ibuprofen"
    But does not specify "Frequency" nor "Duration"
    And attempts to add
    Then system shows error "Must specify frequency and duration of treatment"
```
