# AI Agent User Story Implementation Lifecycle

This repository enforces a strict agentic implementation lifecycle for every User Story. The Agent must not skip or reorder lifecycle stages unless the project architecture clearly proves that a particular stage is not applicable. Before starting any User Story, read `docs/agent/00-index.md` and the relevant stage files under `docs/agent/`. To submit a new User Story, follow the instructions in `docs/agent/templates/user-story-prompt.md`.

## Mandatory Implementation Lifecycle

```text
READ CODEBASE → ANALYZE → PLAN → API DOCUMENTATION → BACKEND IMPLEMENTATION → BACKEND RUNTIME VALIDATION → BACKEND TESTS → FRONTEND IMPLEMENTATION → FRONTEND RUNTIME/UI VALIDATION → FRONTEND TESTS/LINT/TYPE CHECK → FRONTEND BUILD → FINAL VALIDATION
```

## Cross-Cutting Rules

The following rules apply across all lifecycle stages:

1. **5-Attempt Error Retry Limit (Backend & Frontend):**
   To prevent infinite debugging loops, each distinct blocking error may be retried/fixed up to 5 times (`RUN → ERROR → ANALYZE ROOT CAUSE → FIX → RUN AGAIN`). If the same blocking problem remains unresolved after 5 meaningful fix-and-rerun attempts: stop the retry loop, do not claim PASS, summarize the error, root-cause investigation, attempted fixes, commands executed, and remaining blocker, and report that Human Developer assistance is required. Never perform unlimited retries without new evidence.

2. **Do Not Blindly Implement:**
   Only implement the layers and components actually required by the User Story, Acceptance Criteria, and existing architecture. If an entity, repository, migration, endpoint, service, or component already exists or is not required, reuse or omit it instead of creating duplicates.

3. **API / Backend / Frontend Consistency (Section 23):**
   Continuously verify consistency across:
   ```text
   User Story ↕ Acceptance Criteria ↕ Business Rules ↕ API Documentation ↕ Backend Implementation ↕ Database ↕ Backend Tests ↕ Frontend API Integration ↕ Frontend UI
   ```
   Never allow situations where:
   - API documentation says one thing while backend does another;
   - Backend returns a different response structure from the documented API;
   - Frontend assumes a different API contract;
   - Frontend allows an action that backend rejects unexpectedly;
   - Backend allows an action that frontend should restrict;
   - Role access differs between backend and frontend;
   - Database state differs from the expected business state;
   - History/audit does not match actual state changes.

4. **Javadoc, Comments and Language (Section 18):**
   All JavaDoc, block comments, and inline comments must be written in clear Vietnamese with correct diacritics. Do not write new comments in English unless required by framework, annotation, library, or generated code. See `docs/agent/12-language-and-encoding-rules.md`.

5. **UTF-8 Encoding (Section 19):**
   All newly created or modified files must use valid UTF-8 encoding without mojibake or corrupted characters. Verify UTF-8 validity before committing. See `docs/agent/12-language-and-encoding-rules.md`.

6. **Git Safety Checklist Before Every Commit (Section 21):**
   Before every commit, always run `git status` and `git diff`. Ensure the commit contains only changes related to the current User Story. Never commit secrets, API keys, credentials, environment secrets, debug code, temporary files, generated unnecessary files, unrelated refactoring, accidental formatting changes, or unnecessary seed data. Preserve any pre-existing unrelated changes. See `docs/agent/01-git-workflow.md`.
