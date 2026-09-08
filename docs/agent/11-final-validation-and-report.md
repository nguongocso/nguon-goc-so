# Final Validation, Acceptance Criteria Verification, Final Report, and Completion Rule

This document defines the requirements for final validation, acceptance criteria reporting, the completion checklist, and the final completion rule.

---

## 1. Final Validation (Section 24)

Do not consider the User Story complete immediately after implementation.

Perform a complete final validation across all categories:

### Git
* Correct feature branch;
* Clean working tree or only explicitly intended final changes;
* No unrelated changes;
* Commits are logical and reviewable.

### Codebase
* Implementation follows existing architecture;
* No unnecessary duplicate mechanisms;
* No unnecessary refactoring.

### API
* API documentation exists;
* API documentation matches implementation;
* Endpoint behavior is correct;
* Request/response contracts are correct.

### Database
* Migrations are valid;
* Migrations use timestamp-based versioning;
* Every new migration follows:
  ```text
  V<timestamp>__<description>.sql
  ```
* No new sequential migration such as `V60`, `V61`, etc.;
* Applied migrations were not modified unnecessarily;
* Production/platform seed data is in:
  `backend/src/main/resources/db/migration`
* Test/development seed data is in:
  `backend/src/main/resources/db/migration-test`

### Backend
* Backend compiles;
* Backend starts successfully;
* Backend logs contain no unresolved relevant errors;
* Relevant endpoints work;
* Authorization works;
* Business rules work;
* Backend tests pass.

### Frontend
* Frontend starts successfully;
* No unresolved browser console errors;
* No unresolved network/API errors;
* UI flow works;
* Role access works;
* Validation works;
* Frontend tests/checks pass where available;
* Production build succeeds.

### User Story
* Every Acceptance Criterion is satisfied;
* Related business rules are enforced;
* Related functionality is not unintentionally broken.

### Code Quality
* JavaDoc / comments are in Vietnamese;
* Modified/new files use valid UTF-8;
* No debug code remains;
* No secrets remain;
* No unnecessary files remain.

---

## 2. Mandatory Acceptance Criteria Verification (Section 25)

At the end, create an explicit Acceptance Criteria verification table.

For every Acceptance Criterion, report:
* Acceptance Criterion ID;
* Expected behavior;
* Implementation location;
* Validation method;
* Actual result;
* PASS/FAIL.

Do not mark an Acceptance Criterion as PASS based only on code inspection if runtime or test validation is required.

---

## 3. Final Report (Section 26)

At the end, report:

1. Implementation summary.
2. Analysis summary.
3. Important architectural decisions based on the codebase.
4. Files created.
5. Files modified.
6. Database migrations created.
7. Platform seed data created/modified.
8. Test/development seed data created/modified.
9. Backend validation commands executed.
10. Backend runtime result.
11. Backend test result.
12. Frontend runtime result.
13. Frontend test/lint/type-check result.
14. Frontend build result.
15. Acceptance Criteria verification.
16. Business-rule verification.
17. Authorization/role-access verification.
18. API documentation verification.
19. UTF-8 verification.
20. Git commits created.
21. Remaining issues, if any.

Only report `PASS` when the corresponding command, test, build, runtime check, or validation was actually executed and returned a successful result.

Never claim PASS based only on code inspection.

---

## 4. Completion Rule (Section 27)

The User Story is considered COMPLETE only when:

```text
CODEBASE READ
✓
ANALYSIS COMPLETED
✓
IMPLEMENTATION PLAN COMPLETED
✓
API DOCUMENTATION COMPLETED
✓
API DOCUMENTATION COMMITTED
✓
BACKEND IMPLEMENTED
✓
FLYWAY MIGRATIONS VALIDATED
✓
BACKEND STARTED SUCCESSFULLY
✓
BACKEND LOGS CHECKED
✓
BACKEND TESTS PASSED
✓
BACKEND COMMITTED
✓
REQUIRED TEST/DEVELOPMENT SEED DATA CREATED
✓
FRONTEND IMPLEMENTED
✓
FRONTEND STARTED SUCCESSFULLY
✓
BROWSER CONSOLE/NETWORK CHECKED
✓
UI FLOW VALIDATED
✓
FRONTEND TESTS/LINT/TYPE CHECKS PASSED
✓
FRONTEND PRODUCTION BUILD PASSED
✓
FRONTEND COMMITTED
✓
ACCEPTANCE CRITERIA VERIFIED
✓
BUSINESS RULES VERIFIED
✓
AUTHORIZATION VERIFIED
✓
API CONTRACT VERIFIED
✓
UTF-8 VERIFIED
✓
GIT STATUS/DIFF REVIEWED
✓
FINAL VALIDATION PASSED
```

If any required item fails, the User Story is **NOT** complete.

Fix the issue and rerun the appropriate validation.

If a blocking issue remains unresolved after 5 meaningful fix-and-rerun attempts, stop the retry loop and report the root cause and blocker to the Human Developer instead of claiming completion (see [AGENTS.md](../../AGENTS.md#cross-cutting-rules)).

The Agent must prioritize correctness, evidence from the actual codebase, consistency with existing architecture, and complete validation over speed.
