# Requirements — SICEB

This directory holds **business and quality requirements** that feed Attribute-Driven Design (ADD). For architecture decisions, iteration order, and implementation status, use **[`../ADD/`](../ADD/)** (especially [`../ADD/Design/IterationPlan.md`](../ADD/Design/IterationPlan.md) and [`../ADD/implementation/`](../ADD/implementation/)).

## Layout

| Path | Description |
|------|-------------|
| [`Vision_Scope_SICEB.md`](Vision_Scope_SICEB.md) | Vision, scope, epics, and system context. |
| [`Quality_Attribute_Scenarios.md`](Quality_Attribute_Scenarios.md) | Named QA scenarios (PER, SEC, REL, …). |
| [`Quality_Attributes_Priority.md`](Quality_Attributes_Priority.md) | Prioritization of quality attributes. |
| [`Architecture_Concerns.md`](Architecture_Concerns.md) | Cross-cutting architectural concerns. |
| [`Constraints.md`](Constraints.md) | Technical and organizational constraints. |
| [`Critical_US_Analysis.md`](Critical_US_Analysis.md) | Analysis of high-priority user stories (references [`US/Table_US.md`](US/Table_US.md)). |
| [`US/`](US/) | User stories: [`Table_US.md`](US/Table_US.md), per-story `US-*.md`, and deprecated phased rollup [`US/US.md`](US/US.md). |

## User stories (`US/`)

- **`US/Table_US.md`** — Master table of user story IDs, text, and priority.
- **`US/US-*.md`** — Detailed acceptance-style notes per story.
- **`US/US.md`** — **Deprecated** early phased grouping; kept for history only. Prefer [`../ADD/Design/IterationPlan.md`](../ADD/Design/IterationPlan.md) and [`../ADD/implementation/`](../ADD/implementation/) for current scope and sequencing.
