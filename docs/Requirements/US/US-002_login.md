# US-002: Login with Credentials

## User Story
**As** system user  
**I want** to log in with my credentials  
**So that** I can access the system securely

---

## Conversation

**Q1: What fields are used to log in?**  
**A:** Username and password. We do not use email for login, only the username.

**Q2: How many failed attempts do we allow before locking?**  
**A:** 5 consecutive failed attempts. After 5, the account is locked for 15 minutes.

**Q3: What happens if the user has a temporary password?**  
**A:** Upon successful login with their temporary password (indicated in the system as requiring a password change), the system forces the user to change the password before allowing access to any other function.

**Q4: How long does a session last?**  
**A:** 
- Active session: 8 hours (workday)
- Closure due to inactivity: 30 minutes
- On browser close: session closes automatically

**Q5: What does each role see after successful login?**  
**A:**
- **Director:** Executive dashboard
- **Administrator:** Administration panel
- **Service Manager:** Inventory of their service
- **Physicians/Residents:** Patient list
- **Reception:** Patient registration

**Q6: Do we support "Remember session"?**  
**A:** Not in this initial version. For medical security, login is required every time.

**Q7: Is there a "Forgot my password" option?**  
**A:** Not in this version. The user must contact the Administrator for a manual reset.

---

## Acceptance Criteria (Gherkin)

### Scenario 1: Successful login with correct credentials

```gherkin
Feature: Login
  As a system user
  I want to log in with my credentials
  To access the system securely

  Background:
    Given a user exists with:
      | username | password    | role                  | status  |
      | mgarcia  | Actual2024  | Reception Staff       | Active  |
    And the user is on the login page

  Scenario: Successful reception user login
    When enters username "mgarcia"
    And enters password "Actual2024"
    And clicks on "Login"
    Then the system validates the credentials
    And the system creates a session lasting 8 hours
    And the system redirects to "Patient Registration"
    And the menu shows "Reception Staff" options
    And the top bar shows "Welcome, María García"
```

### Scenario 2: Login with incorrect password

```gherkin
  Scenario: Login attempt with incorrect password
    Given a user "mgarcia" exists with password "Actual2024"
    When enters username "mgarcia"
    And enters password "WrongPassword"
    And clicks on "Login"
    Then the system shows the error "Incorrect username or password"
    And the user remains on the login page
    And the failed attempts counter increments to 1
    And NO session is created
```

### Scenario 3: Login with non-existent user

```gherkin
  Scenario: Login attempt with user that does not exist
    Given there is NO user "fakeuser"
    When enters username "fakeuser"
    And enters password "anyPassword123"
    And clicks on "Login"
    Then the system shows the error "Incorrect username or password"
    And the user remains on the login page
    And it is NOT revealed that the user does not exist (security)
```

### Scenario 4: Lockout after multiple failed attempts

```gherkin
  Scenario: Account locked after 5 failed attempts
    Given the user "mgarcia" has 4 previous failed attempts
    When enters username "mgarcia"
    And enters password "WrongPassword"
    And clicks on "Login"
    Then the system shows the error "Account locked for 15 minutes due to multiple failed attempts"
    And the user CANNOT try login again
    And the user status changes to "Temporarily Locked"
    And the lockout is recorded in the security log
```

### Scenario 5: Login with inactive account

```gherkin
  Scenario: Login attempt with disabled account
    Given a user "jlopez" exists with status "Inactive"
    When enters username "jlopez"
    And enters correct password
    And clicks on "Login"
    Then the system shows the error "Your account is inactive. Contact the administrator"
    And NO session is created
```

### Scenario 6: Force temporary password change

```gherkin
  Scenario: First login with temporary password
    Given a user "newuser" exists with mustChangePassword = true
    When enters username "newuser"
    And enters temporary password "Temporal123"
    And clicks on "Login"
    Then the system validates the credentials
    And the system shows "Mandatory Password Change" screen
    And the system DOES NOT allow access to the main menu until password is changed
```

### Scenario 7: Successful temporary password change

```gherkin
  Scenario: Change temporary password on first login
    Given the user "newuser" is on the mandatory change screen
    When enters current password "Temporal123"
    And enters new password "MyNewPass2024"
    And confirms new password "MyNewPass2024"
    And clicks on "Change Password"
    Then the system validates that the new password meets requirements
    And the system updates the password
    And sets mustChangePassword = false
    And creates the user session
    And redirects to the initial screen according to their role
```

### Scenario 8: Temporary password change with weak password

```gherkin
  Scenario: Attempt to change to password that does not meet requirements
    Given the user "newuser" is on the mandatory change screen
    When enters current password "Temporal123"
    And enters new password "12345678"
    And confirms new password "12345678"
    And clicks on "Change Password"
    Then the system shows error "The password must contain letters and numbers"
    And the password is NOT changed
    And remains on mandatory change screen
```

### Scenario 9: Session expiration due to inactivity

```gherkin
  Scenario: Session expires after 30 minutes of inactivity
    Given the user "mgarcia" has logged in successfully
    And 30 minutes have passed without any user action
    When the user attempts to perform any action
    Then the system shows the message "Your session has expired due to inactivity"
    And the system closes the session automatically
    And redirects to the login page
```

### Scenario 10: Successful resident physician login

```gherkin
  Scenario: Resident login shows appropriate screen
    Given a user exists with:
      | username | password      | role        | service    |
      | cramirez | Residente2024 | Resident R2 | Pediatrics |
    When enters username "cramirez"
    And enters password "Residente2024"
    And clicks on "Login"
    Then the system redirects to "Patient List - Pediatrics"
    And the menu shows "Resident R2" options
    And the menu DOES NOT show controlled prescription options
    And the top bar shows "Dr. Carlos Ramírez - Resident R2 - Pediatrics"
```
