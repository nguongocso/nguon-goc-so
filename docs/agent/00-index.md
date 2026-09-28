# User Story Implementation Lifecycle Index

This document maps each of the 12 lifecycle stages defined in `AGENTS.md` to the corresponding detail files under `docs/agent/`.

## Lifecycle Stage Mapping

| Stage # | Lifecycle Stage | Primary Documentation File(s) | Key Topics Covered |
|:---|:---|:---|:---|
| 1 | **READ CODEBASE** | `docs/agent/02-codebase-reading-checklist.md` | Mandatory repository inspection, architecture discovery, related stories/rules search |
| 2 | **ANALYZE** | `docs/agent/03-analysis-and-planning.md` | 18-point analysis checklist before coding, business flow, entity mapping |
| 3 | **PLAN** | `docs/agent/03-analysis-and-planning.md` | 21-point logical implementation plan, "do not blindly implement" rule |
| 4 | **API DOCUMENTATION** | `docs/agent/04-api-documentation-convention.md` | `docs/api` conventions, contract-first principle, standalone git commit |
| 5 | **BACKEND IMPLEMENTATION** | `docs/agent/05-backend-implementation.md`<br>`docs/agent/06-flyway-migration-convention.md`<br>`docs/agent/07-seed-data-strategy.md` | Backend components, service logic, timestamp Flyway migrations, seed data |
| 6 | **BACKEND RUNTIME VALIDATION** | `docs/agent/08-backend-testing-and-runtime.md` | Starting backend, checking logs, health verification loop, 5-attempt retry limit |
| 7 | **BACKEND TESTS** | `docs/agent/08-backend-testing-and-runtime.md` | Unit and integration tests, coverage of ACs and edge cases, test execution |
| 8 | **FRONTEND IMPLEMENTATION** | `docs/agent/09-frontend-implementation.md`<br>`docs/agent/07-seed-data-strategy.md` | Prerequisites, UI architecture, API integration, types, components, routing, roles |
| 9 | **FRONTEND RUNTIME/UI VALIDATION** | `docs/agent/10-frontend-runtime-and-validation.md`<br>`docs/agent/07-seed-data-strategy.md` | Running dev server, checking console/network/API, UI testing with realistic seed data |
| 10 | **FRONTEND TESTS/LINT/TYPE CHECK** | `docs/agent/10-frontend-runtime-and-validation.md` | Linting, TypeScript type checking, unit tests, 5-attempt error retry limit |
| 11 | **FRONTEND BUILD** | `docs/agent/10-frontend-runtime-and-validation.md` | Production build verification |
| 12 | **FINAL VALIDATION** | `docs/agent/11-final-validation-and-report.md` | Comprehensive checklist (Git, Code, API, DB, BE, FE, Quality), AC table, final report, completion rule |

---

## Supporting & Cross-Cutting Documentation

In addition to the 12 stages above, refer to these cross-cutting guides:

- **Git Workflow & Safety:** `docs/agent/01-git-workflow.md` — Branch creation from `develop`, continuous logical commits, and pre-commit safety checklists.
- **Language & Encoding Rules:** `docs/agent/12-language-and-encoding-rules.md` — Vietnamese JavaDoc/comments requirements and UTF-8 encoding integrity.
- **User Story Prompt Template:** `docs/agent/templates/user-story-prompt.md` — Standard prompt template for initiating a new User Story.
