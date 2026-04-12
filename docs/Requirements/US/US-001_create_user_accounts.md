# US-001: Create User Accounts with Role-Based Permissions

## User Story

**As** Administrator  
**I want** to create user accounts with role-based permissions  
**So that** staff can access the system according to their responsibilities

---

## Conversation

**Q1: What roles do we need to support initially?**  
**A:** We need 9 roles:
1. **General Director** - Full access, strategic reports
2. **General Administrator** - Complete system management
3. **Service Manager** - Management of their specific service
4. **Attending Physician** - Complete medical care without restrictions
5. **Resident R4** - Medical care with minimal supervision
6. **Resident R3** - Medical care with some restrictions
7. **Resident R2** - Medical care with moderate restrictions
8. **Resident R1** - Medical care with maximum restrictions
9. **Reception Staff** - Patient registration and billing

**Q2: Can a user have multiple roles?**  
**A:** Not in the MVP. Each user has ONLY ONE role. If someone needs capabilities of two roles, the role with more permissions is assigned.

**Q3: What determines the minimum information to create a user?**  
**A:** 
- Full Name (mandatory)
- Institutional Email (mandatory, unique)
- Role (mandatory, list selection)
- Username (mandatory, unique, alphanumeric without spaces)
- Initial Password (mandatory, minimum 8 characters)
- Service they belong to (mandatory only for Service Manager, Physicians, and Residents)
- Status (Active/Inactive)

**Q4: How is the initial password assigned?**  
**A:** The Administrator enters a temporary password. The user will be required to change it on their first login (see US-002).

**Q5: What happens if I try to create a user with a duplicate email or username?**  
**A:** The system must show a clear error indicating that the email or username already exists in the system.

**Q6: Can I edit a user's role after creating it?**  
**A:** Yes, but with restrictions:
- Only the General Administrator can change roles
- The change must be recorded in the audit log (who, when, previous role, new role)
- The change is effective immediately (the user will see the new options on their next access)

**Q7: What happens with inactive users?**  
**A:** They cannot log in. They are marked as "inactive" but are NOT deleted from the system, as we need to maintain the history of their activities.

**Q8: Do we need to validate the email format?**  
**A:** Yes, it must be a valid email format (contain @) and preferably from the clinic's domain, although the latter is not restrictive.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Create user successfully with basic role

```gherkin
Feature: User Account Creation
  As Administrator
  I want to create user accounts with specific roles
  So that staff can access the system

  Background:
    Given the Administrator has logged in
    And is on the "User Management" screen

  Scenario: Create reception user successfully
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | María García López             |
      | Email            | mgarcia@clinicabienestar.mx    |
      | Username         | mgarcia                        |
      | Password         | Temporal123                    |
      | Role             | Reception Staff                |
      | Status           | Active                         |
    And clicks on "Save"
    Then the system shows the message "User created successfully"
    And the user "mgarcia" appears in the user list
    And the role displayed is "Reception Staff"
    And the status displayed is "Active"
```

### Scenario 2: Create medical user with service assignment

```gherkin
  Scenario: Create R2 Resident with assigned service
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | Dr. Carlos Ramírez Soto        |
      | Email            | cramirez@clinicabienestar.mx   |
      | Username         | cramirez                       |
      | Password         | Residente2024                  |
      | Role             | Resident R2                    |
    Then the system shows the "Service" field as mandatory
    When selects "Pediatrics" in the Service field
    And clicks on "Save"
    Then the system shows the message "User created successfully"
    And the user "cramirez" appears with service "Pediatrics"
```

### Scenario 3: Error due to duplicate email

```gherkin
  Scenario: Attempt to create user with existing email
    Given a user with email "mgarcia@clinicabienestar.mx" exists
    And the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | María Guadalupe García         |
      | Email            | mgarcia@clinicabienestar.mx    |
      | Username         | mggarcia                       |
      | Password         | Temporal123                    |
      | Role             | Reception Staff                |
    And clicks on "Save"
    Then the system shows the error "The email already exists in the system"
    And the user is NOT created
    And remains on the creation form with entered data
```

### Scenario 4: Error due to duplicate username

```gherkin
  Scenario: Attempt to create user with existing username
    Given a user with username "mgarcia" exists
    And the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | Manuel García Pérez            |
      | Email            | manuelgarcia@clinicabienestar.mx |
      | Username         | mgarcia                        |
      | Password         | Temporal123                    |
      | Role             | Reception Staff                |
    And clicks on "Save"
    Then the system shows the error "The username is already in use"
    And the user is NOT created
```

### Scenario 5: Weak password validation

```gherkin
  Scenario: Attempt to create user with password less than 8 characters
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | Pedro López Ruiz               |
      | Email            | plopez@clinicabienestar.mx     |
      | Username         | plopez                         |
      | Password         | Temp12                         |
      | Role             | Reception Staff                |
    And clicks on "Save"
    Then the system shows the error "The password must be at least 8 characters long"
    And the user is NOT created
```

### Scenario 6: Password validation without numbers

```gherkin
  Scenario: Attempt to create user with letters-only password
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | Pedro López Ruiz               |
      | Email            | plopez@clinicabienestar.mx     |
      | Username         | plopez                         |
      | Password         | Temporal                       |
      | Role             | Reception Staff                |
    And clicks on "Save"
    Then the system shows the error "The password must contain at least one number"
    And the user is NOT created
```

### Scenario 7: Invalid email validation

```gherkin
  Scenario: Attempt to create user with invalid email format
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | Ana Martínez Flores            |
      | Email            | anamartinez.com                |
      | Username         | amartinez                      |
      | Password         | Temporal123                    |
      | Role             | Reception Staff                |
    And clicks on "Save"
    Then the system shows the error "Enter a valid email"
    And the "Email" field is marked in red
    And the user is NOT created
```

### Scenario 8: Username validation with special characters

```gherkin
  Scenario: Attempt to create user with special characters in username
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | José Luis Hernández            |
      | Email            | jlhernandez@clinicabienestar.mx |
      | Username         | j.luis@hernandez               |
      | Password         | Temporal123                    |
      | Role             | Reception Staff                |
    And clicks on "Save"
    Then the system shows the error "The username can only contain letters and numbers without spaces"
    And the user is NOT created
```

### Scenario 9: Create General Director user

```gherkin
  Scenario: Create user with maximum privileges
    Given the Administrator clicks on "New User"
    When enters the following data:
      | Field            | Value                          |
      | Full Name        | Dr. Roberto Sánchez Díaz       |
      | Email            | rsanchez@clinicabienestar.mx   |
      | Username         | rsanchez                       |
      | Password         | Director2024                   |
      | Role             | General Director               |
      | Status           | Active                         |
    And clicks on "Save"
    Then the system shows the message "User created successfully"
    And the user "rsanchez" appears with role "General Director"
    And the "Service" field is NOT requested (Director sees everything)
```

### Scenario 10: Mandatory service field only for medical roles

```gherkin
  Scenario: Validate that service is not mandatory for General Administrator
    Given the Administrator clicks on "New User"
    When selects role "General Administrator"
    Then the "Service" field is NOT visible
    When selects role "Resident R1"
    Then the "Service" field is shown as mandatory
    When selects role "Reception Staff"
    Then the "Service" field is NOT visible
```
