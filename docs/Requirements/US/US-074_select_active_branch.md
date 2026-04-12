# US-074: Select Active Branch

## User Story

**As** Multi-Branch User  
**I want** to select my "Active Branch" upon login or switch it mid-session  
**So that** the system shows me the correct inventory, patients, and data for that location

---

## Conversation

**Q1: When does the user select a branch?**  
**A:** There are two moments:
1. **At login:** After successful authentication, the user is presented with a list of their authorized branches and must select one before proceeding.
2. **Mid-session:** The user can switch their active branch at any time from the navigation bar without logging out. This is critical for staff who operate across multiple locations during the same shift.

**Q2: What happens when a user switches branches mid-session?**  
**A:** The system performs a context switch:
- A new JWT is issued with the selected branch claim (no re-authentication required)
- All data views (inventory, patients, clinical records) reload to reflect the new branch scope
- WebSocket subscriptions switch to the new branch's real-time channels
- Local cache is cleared to prevent cross-branch data leakage
- The entire switch must complete in less than 3 seconds (ESC-03)

**Q3: Can a user see data from multiple branches simultaneously?**  
**A:** No, regular users see only data from their active branch. However, the General Administrator role has a special "All Branches" view for consolidated dashboards (see US-075). The active branch selector always reflects the current scope.

**Q4: What if a user only has access to one branch?**  
**A:** The system automatically sets that branch as active upon login without showing the selection screen. The branch name is still displayed in the navigation bar for context.

**Q5: What branches appear in the selection list?**  
**A:** Only branches that are:
- Assigned to the user (via US-073)
- In "Active" status (not deactivated)

If a user has no active branches assigned, they see an error message and cannot proceed.

**Q6: Is the selected branch remembered for next login?**  
**A:** Yes. The system remembers the last active branch per user. On next login, it is pre-selected as the default, but the user can change it before proceeding.

**Q7: What visual indicator shows the current active branch?**  
**A:** The navigation bar permanently displays the active branch name with a distinguishable badge/icon. The branch selector is always accessible from this location.

---

## Acceptance Criteria (Gherkin)

```gherkin
Feature: Active Branch Selection
  As Multi-Branch User
  I want to select and switch my active branch
  So that I work with the correct data for each location

  Background:
    Given user "doctor_maria" has authorized branches:
      | Branch           | Status |
      | Sucursal Norte   | Active |
      | Sucursal Centro  | Active |
      | Sucursal Sur     | Active |

  Scenario: Select branch at login
    Given user "doctor_maria" has successfully authenticated
    When the branch selection screen is displayed
    Then shows all 3 authorized active branches
    And each branch shows its name and address
    When selects "Sucursal Norte"
    And clicks "Continue"
    Then the system sets "Sucursal Norte" as the active branch
    And redirects to the main dashboard
    And the navigation bar shows "Sucursal Norte" as active branch

  Scenario: Switch branch mid-session without logout
    Given user "doctor_maria" is logged in with active branch "Sucursal Norte"
    When clicks on the branch selector in the navigation bar
    Then shows the list of authorized branches
    And "Sucursal Norte" appears as currently selected
    When selects "Sucursal Centro"
    Then the system issues a new JWT with "Sucursal Centro" as branch claim
    And does NOT require re-authentication
    And all data views reload to show "Sucursal Centro" data
    And the navigation bar updates to show "Sucursal Centro"
    And the switch completes in less than 3 seconds

  Scenario: Data isolation after branch switch
    Given user "doctor_maria" is logged in with active branch "Sucursal Norte"
    And "Sucursal Norte" has 25 patients registered
    And "Sucursal Centro" has 40 patients registered
    When switches to "Sucursal Centro"
    And navigates to "Patient List"
    Then shows 40 patients from "Sucursal Centro"
    And does NOT show patients from "Sucursal Norte"

  Scenario: Inventory reflects active branch
    Given user "doctor_maria" is logged in with active branch "Sucursal Norte"
    And "Sucursal Norte" inventory has 30 items
    And "Sucursal Centro" inventory has 50 items
    When accesses "Inventory"
    Then shows 30 items from "Sucursal Norte"
    When switches to "Sucursal Centro"
    And accesses "Inventory"
    Then shows 50 items from "Sucursal Centro"

  Scenario: Single-branch user skips selection
    Given user "nurse_carlos" has only one authorized branch "Sucursal Sur"
    When "nurse_carlos" successfully authenticates
    Then the branch selection screen is NOT displayed
    And "Sucursal Sur" is automatically set as active branch
    And redirects directly to the main dashboard
    And the navigation bar shows "Sucursal Sur"

  Scenario: Remember last active branch on next login
    Given user "doctor_maria" last used branch "Sucursal Centro"
    When "doctor_maria" logs in again
    Then the branch selection screen shows "Sucursal Centro" pre-selected
    And the user can confirm or change the selection

  Scenario: No authorized branches available
    Given user "temp_user" has no active branches assigned
    When "temp_user" successfully authenticates
    Then the system shows error "No active branches available. Contact your administrator."
    And does NOT allow proceeding to the dashboard

  Scenario: Deactivated branch not shown in selection
    Given branch "Sucursal Sur" has been deactivated
    When user "doctor_maria" opens the branch selector
    Then shows only "Sucursal Norte" and "Sucursal Centro"
    And does NOT show "Sucursal Sur"

  Scenario: WebSocket reconnects on branch switch
    Given user "doctor_maria" is logged in with active branch "Sucursal Norte"
    And is subscribed to real-time inventory updates for "Sucursal Norte"
    When switches to "Sucursal Centro"
    Then WebSocket disconnects from "Sucursal Norte" channels
    And reconnects to "Sucursal Centro" channels
    And receives real-time updates only from "Sucursal Centro"

  Scenario: Branch indicator always visible
    Given user "doctor_maria" is logged in with active branch "Sucursal Norte"
    When navigates to any section of the application
    Then the navigation bar always displays "Sucursal Norte" with a branch icon
    And clicking on it opens the branch selector dropdown
```
