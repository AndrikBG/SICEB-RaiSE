# US-071: Register New Branch

## User Story

**As** General Administrator  
**I want** to register a new Branch (Sucursal) with its details (name, address)  
**So that** the system can manage operations in that location

---

## Conversation

**Q1: What information is required to register a new branch?**  
**A:** The following fields are mandatory:
- Branch name (unique across the network)
- Address (street, city, state, postal code)
- Phone number
- Contact email
- Operating hours (opening/closing time)

Optional fields include a branch code/alias for internal reference.

**Q2: Who can register new branches?**  
**A:** Only the General Administrator has permissions to register branches. This is restricted because a new branch triggers onboarding steps that affect infrastructure (database schemas, security policies, default configurations).

**Q3: What happens after a branch is registered?**  
**A:** An automated onboarding process begins that provisions the branch for operations:
1. Database schema and tenant isolation setup
2. Default role and permission assignment
3. Default inventory catalog seeding
4. Default tariff configuration
5. WebSocket channel provisioning for real-time updates

The administrator can monitor onboarding progress from the branch details panel. The entire process must complete in less than 1 hour (ESC-01).

**Q4: Can two branches have the same name?**  
**A:** No. Branch names must be unique. The system validates uniqueness before saving.

**Q5: Can a branch be edited after registration?**  
**A:** Yes. Name, address, phone, email, and operating hours can be updated at any time. However, certain onboarding-level configurations (database schema, tenant isolation) are immutable after provisioning.

**Q6: What about deactivating a branch?**  
**A:** Deactivation is handled by a separate user story (US-072). A deactivated branch no longer allows new operations but all historical data is preserved.

**Q7: Is there a limit on the number of branches?**  
**A:** The system is designed to scale from 3 to 15 branches with less than 10% performance degradation (ESC-02). There is no hard-coded limit, but the architecture is validated for this range.

---

## Acceptance Criteria (Gherkin)

```gherkin
Feature: Branch Registration
  As General Administrator
  I want to register new branches
  So that the system can manage operations in new locations

  Background:
    Given the user "admin" (General Administrator) has logged in
    And navigates to "Branch Management"

  Scenario: Successfully register a new branch
    When clicks "New Branch"
    And enters branch data:
      | Field            | Value                              |
      | Name             | Sucursal Norte                     |
      | Street           | Av. Universidad 1500               |
      | City             | Monterrey                          |
      | State            | Nuevo León                         |
      | Postal Code      | 64000                              |
      | Phone            | +52 81 1234 5678                   |
      | Email            | norte@clinica.mx                   |
      | Opening Time     | 08:00                              |
      | Closing Time     | 20:00                              |
    And clicks "Save"
    Then the system creates the branch "Sucursal Norte"
    And shows confirmation message "Branch registered successfully"
    And the onboarding process starts automatically
    And the branch appears in the branch list with status "Onboarding"

  Scenario: Onboarding progress tracking
    Given branch "Sucursal Norte" was just registered
    When clicks on branch "Sucursal Norte"
    Then the details panel shows onboarding progress:
      | Step                          | Status      |
      | Schema and tenant setup       | Completed   |
      | Default roles and permissions | In Progress |
      | Inventory catalog seeding     | Pending     |
      | Tariff configuration          | Pending     |
      | Real-time channel setup       | Pending     |
    And a progress indicator shows "2 of 5 steps completed"

  Scenario: Onboarding completes successfully
    Given branch "Sucursal Norte" onboarding is in progress
    When all 5 onboarding steps complete
    Then the branch status changes to "Active"
    And the branch is available for user assignment
    And the branch appears in the branch selection list
    And total onboarding time is less than 1 hour

  Scenario: Duplicate branch name validation
    Given branch "Sucursal Norte" already exists
    When clicks "New Branch"
    And enters name "Sucursal Norte"
    And fills remaining required fields
    And clicks "Save"
    Then the system shows error "A branch with this name already exists"
    And does not create the branch

  Scenario: Required field validation
    When clicks "New Branch"
    And leaves "Name" empty
    And leaves "Street" empty
    And clicks "Save"
    Then the system shows validation errors:
      | Field  | Error                    |
      | Name   | Branch name is required  |
      | Street | Address is required      |
    And does not create the branch

  Scenario: Edit existing branch details
    Given branch "Sucursal Norte" exists with status "Active"
    When clicks on branch "Sucursal Norte"
    And clicks "Edit"
    And changes phone to "+52 81 9876 5432"
    And clicks "Save"
    Then the system updates branch information
    And shows confirmation message "Branch updated successfully"
    And the audit log records the modification

  Scenario: View list of all branches
    Given the following branches exist:
      | Name             | Status   |
      | Sucursal Norte   | Active   |
      | Sucursal Centro  | Active   |
      | Sucursal Sur     | Onboarding |
    When accesses "Branch Management"
    Then shows all 3 branches with their status
    And each branch shows name, address, and status
    And active branches show a green indicator
    And onboarding branches show a yellow indicator

  Scenario: Branch registration generates audit event
    When registers a new branch "Sucursal Poniente"
    Then the audit log records:
      | Field     | Value                          |
      | Action    | BRANCH_REGISTERED              |
      | User      | admin                          |
      | Entity    | Sucursal Poniente              |
      | Timestamp | current date and time          |

  Scenario: Unauthorized user cannot register branch
    Given the user "manager" (Service Manager) has logged in
    When attempts to access "Branch Management"
    Then the system shows "Access Denied"
    And does not display the "New Branch" option
```
