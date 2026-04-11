# Tech Debt & Known Bugs

> Living document. Items discovered during analysis, code review, or implementation.
> Resolved items keep their entry with commit hash for traceability.

---

## Bugs

| ID | Severity | Origin | Description | Blocks | Status | Resolved |
|----|----------|--------|-------------|--------|--------|----------|
| B1 | Medium | Phase 1 | `EntityId.generate()` uses UUID v4 (`UUID.randomUUID()`). Design specifies UUID v7 (CRN-38) for time-ordered IDs. Affects index locality and sync ordering. | Phase 6 (sync ordering) | **Resolved** | pre-E4 fix |
| B2 | High | Phase 2 | `Patient.isMinor()` uses `< 17` threshold. Mexican law and domain model say minors are `< 18`. 17-year-olds bypass guardian requirements. | Patient safety | **Resolved** | pre-E4 fix |

## Architectural Gaps

| ID | Severity | Origin | Description | Blocks | Status | Resolved |
|----|----------|--------|-------------|--------|--------|----------|
| G1 | High | Phase 2→3 | `PrescriptionCommandHandler` doesn't call `ResidencyLevelPolicy` at service layer. Only relies on `@PreAuthorize` at controller. Bypassed by internal calls, offline sync replay, or any non-HTTP code path. Domain model says "enforced at service layer, not only at UI." | Regulatory (SEC-01) | **Partial** | pre-E4 fix — structural hook added, `isControlled` flag on items. Full enforcement when medication catalog exists (Phase 5). |
| G2 | Medium | Phase 3 | No integration tests with Testcontainers. All backend tests are unit or ArchUnit structural. RLS isolation, hash-chain triggers, and multi-tenant scoping not verified against real PostgreSQL. Phase 3 summary notes this as "Phase 4 prerequisite." | E4 S4.2 (triggers, partitioning, RLS) | Open | — |
| G3 | Low | Phase 2 | `PendingLabStudy` is a JPA entity mapped to read-model table (`pending_lab_studies_view`) but `LabStudyCommandHandler` writes to it directly. Violates CQRS separation used everywhere else (events → projector → read model). | None | Open | — |
| G4 | Medium | Phase 3 | No consent verification before patient data access. `ClinicalController.getTimeline()` and `getNom004()` don't check active consent record. Consent check only exists for explicit ARCO flows. | LFPDPPP compliance | Open | — |
| G5 | Low | Phase 2 | Prescription module takes medication IDs/names in payload without catalog validation. Pharmacy module is stub — prescriptions may reference non-existent medications. | None (resolves at Phase 5) | Open | — |

## Notes

- B1, B2 resolved. G1 partially resolved (structural hook, full at Phase 5)
- G2 should be resolved as first task of S4.2 (Testcontainers setup)
- G3, G4, G5 are tracked but not blocking E4
