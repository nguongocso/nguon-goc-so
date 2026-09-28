# Analysis, Planning, and Selective Implementation

This document details the analysis and planning stages that must precede any implementation, as well as the fundamental principle of selective, architecture-grounded implementation.

---

## 1. Analysis Before Implementation (Section 3)

After reading the codebase and related User Stories, perform an explicit analysis.

The analysis must identify:

1. Current business flow.
2. Existing implementation related to the User Story.
3. Existing entities and database relationships.
4. Existing API contracts.
5. Existing authorization and role-access rules.
6. Existing state/status transitions.
7. Existing history/audit mechanisms.
8. Existing notification mechanisms.
9. Existing frontend patterns.
10. Existing test patterns.
11. Existing migration and seed conventions.
12. What must be created.
13. What must be modified.
14. What can be reused.
15. What must NOT be changed.
16. Potential regression risks.
17. Acceptance Criteria mapping.
18. Related business-rule mapping.

Do not start implementation until the Agent has enough evidence from the codebase to explain how the requested functionality fits into the existing architecture.

If information is unclear, search and read more of the codebase.

Never replace repository investigation with assumptions.

---

## 2. Implementation Plan (Section 4)

After the analysis, create a concrete implementation plan.

The plan must describe the expected changes in logical order:

1. API documentation.
2. Database/migration changes if required.
3. Domain/entity changes if required.
4. Repository changes if required.
5. DTO changes if required.
6. Service/business logic changes.
7. Authorization changes.
8. Controller/API endpoint changes.
9. History/audit changes.
10. Notification changes.
11. Backend tests.
12. Seed data if required.
13. Frontend API integration.
14. Frontend types/interfaces.
15. Components.
16. Pages/layout.
17. Routing.
18. Role access.
19. UI validation.
20. Runtime validation.
21. Final regression validation.

Do not blindly implement every item in this list.

Only implement the layers actually required by the User Story and existing architecture.

If an existing component or mechanism already satisfies the requirement, reuse it.

---

## 3. Do Not Blindly Implement (Section 22)

The Agent must not blindly follow the list of possible components in this guide.

The actual implementation must be determined by:

* User Story;
* Acceptance Criteria;
* Business rules;
* Related User Stories;
* Actual codebase;
* Actual API documentation;
* Existing architecture;
* Existing database structure;
* Existing tests;
* Existing frontend patterns.

Rules:
- If an entity is not required, do not create one.
- If a repository is not required, do not create one.
- If a migration is not required, do not create one.
- If an endpoint already exists and can be reused, do not create a duplicate endpoint.
- If an existing service already handles the required behavior, extend/reuse it when appropriate.
- If a component already exists, reuse it instead of creating a duplicate.
