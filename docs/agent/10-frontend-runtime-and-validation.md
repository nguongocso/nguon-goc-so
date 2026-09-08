# Frontend Runtime, Validation, Tests, and Build

This document defines frontend runtime verification, testing, linting, type-checking, production build requirements, and error retry limits.

---

## 1. Frontend Runtime Validation (Section 15)

After frontend implementation, actually run the frontend application.

Do not rely only on static inspection or build success.

If the frontend uses a development server, run it using the project's established configuration.

If necessary, run the frontend as a background process so that the CLI remains usable (e.g., using background job facilities).

Check:
* Terminal output;
* Browser console;
* Network requests;
* API requests;
* API responses;
* Routing;
* Rendering;
* Loading states;
* Error states;
* Permissions;
* Displayed data;
* State transitions;
* Form validation.

If console/runtime errors occur, investigate and fix them.

The required loop is:
```text
IMPLEMENT → RUN FRONTEND → CHECK TERMINAL → CHECK BROWSER CONSOLE → CHECK NETWORK/API → IDENTIFY ROOT CAUSE → FIX → RUN FRONTEND AGAIN → CHECK AGAIN → REPEAT
```

Do not ignore a console error merely because the page appears to work.

---

## 2. Frontend Test, Lint, Type Check, and Build (Section 16)

Run all applicable frontend validation commands defined by the project, including where available:
* Lint;
* Type check;
* Unit tests;
* Integration tests;
* Production build.

If errors occur:
1. Read the actual error.
2. Identify the root cause.
3. Inspect the related code.
4. Fix the appropriate layer.
5. Rerun the affected command.
6. Rerun broader validation where necessary.

Continue until all applicable checks pass.

**Strict Prohibitions:**
* Do not disable lint rules;
* Do not suppress TypeScript errors;
* Do not introduce unnecessary `any`;
* Do not skip tests;
* Do not hide console errors;
* Do not ignore failed API calls;
* Do not remove functionality;
* Do not weaken validation.

---

## 3. Frontend Error Retry Limit (Section 17)

Use the same maximum retry rule for frontend blocking errors.

Each distinct blocking issue is subject to the **5-Attempt Error Retry Limit**.

For full rules and reporting requirements when the limit is reached, see [AGENTS.md](../../AGENTS.md#cross-cutting-rules) (Rule 1).
