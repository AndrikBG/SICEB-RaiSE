# US-019: Register New Patients

## User Story

**As** Reception Personnel  
**I want** to register new patients with their demographic information  
**So that** they can receive medical attention and have a clinical history

---

## Conversation

**Q1: What data are mandatory to register a patient?**  
**A:** We mandatorily need: Name(s), Paternal Surname, Maternal Surname, Date of Birth (to calculate age), Gender, and a Contact Phone.

**Q2: Is the CURP mandatory?**  
**A:** Ideally yes, but if the patient doesn't know it at that moment (e.g., emergency or foreigner), we can leave it pending. The system should mark the record as "Incomplete" until those data are filled, but allow medical attention.

**Q3: What happens if the patient already exists?**  
**A:** Before saving, the system must check duplicates by Full Name + Date of Birth. If a match is found, it must show an alert: "Possible duplicate patient: [Existing Data]" and ask if we wish to use that one or create a new one (homonyms).

**Q4: Is an automatic record number generated?**  
**A:** Yes. The reception user DOES NOT invent the number. The system assigns a unique sequential ID or specific format (e.g., YYYY-MM-XXXX) upon successful save.

**Q5: Can we take a photo of the patient?**  
**A:** Yes, it would be excellent for visual identification in security, but it is optional.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Successful patient registration with complete data
```gherkin
Feature: Patient Registration
  As receptionist
  I want to register a person in the system
  To start their care process

  Background:
    Given the user "recep_ana" is on the "New Patient" screen

  Scenario: Standard registration
    When enters data:
      | Name             | Roberto                  |
      | Pat. Surname     | Gómez                    |
      | Mat. Surname     | Bolaños                  |
      | Date of Birth    | 21/02/1929               |
      | Gender           | Male                     |
      | Phone            | 5512345678               |
    And clicks on "Save"
    Then the system registers the patient
    And assigns a unique Record Number
    And shows message "Patient registered successfully"
```

### Scenario 2: Detection of possible duplicates
```gherkin
  Scenario: Homonym alert
    Given a patient "Roberto Gómez Bolaños" born "21/02/1929" already exists
    When the user attempts to register another patient with the SAME data
    Then the system shows alert "Possible duplicate patient found"
    And shows data of existing patient to compare
    And allows choosing "Is same person" (open record) or "Is another person" (create new)
```

### Scenario 3: Mandatory fields validation
```gherkin
  Scenario: Attempt to save without name
    When leaves "Name" field empty
    And clicks on "Save"
    Then the system shows error "Name is mandatory"
    And does not create the record
```

### Scenario 4: Automatic age calculation
```gherkin
  Scenario: Visualize age upon entering date of birth
    When the user enters Date of Birth "01/01/2000"
    Then the system calculates and displays automatically "Age: 26 years" (depending on current date)
    And if enters "01/01/2023", shows "Age: 3 years"
```

### Scenario 5: Foreign patient registration (no CURP)
```gherkin
  Scenario: Patient without national documents
    When enters basic demographic data
    But leaves "CURP" field empty
    And checks "Foreigner / No CURP" box
    And saves the record
    Then the system allows saving
    But marks profile with label "Incomplete Fiscal Data"
```

### Scenario 6: Phone format validation
```gherkin
  Scenario: Enter invalid phone
    When enters "123" in Phone field
    And attempts to save
    Then the system shows error "Phone must be 10 digits"
```
