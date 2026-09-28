# Backend Testing and Runtime Validation

This document governs backend test implementation, runtime validation, test execution, and error resolution policies.

---

## 1. Backend Test Implementation (Section 9)

Create or update backend tests under:

`backend/src/test/java/vn/nguongocso`

The Agent MUST inspect existing test conventions before creating tests.

Ensure all test source code:
* Uses valid Java syntax;
* Compiles correctly;
* Follows the project's test framework;
* Follows existing naming conventions;
* Follows existing test setup;
* Reuses existing test utilities where appropriate.

Tests must cover the Acceptance Criteria.

Where applicable, tests must cover:
* Happy path;
* Validation;
* Missing required fields;
* Authorization;
* Role access;
* Business rules;
* State transitions;
* Persistence;
* Database constraints;
* History / audit;
* Notifications;
* Related entities;
* Edge cases;
* Error handling;
* Regression scenarios.

Do not modify tests simply to make an incorrect implementation pass.

---

## 2. Backend Runtime Validation (Section 10)

After backend implementation, the Agent MUST actually run the backend.

Do not rely only on compilation.

Use the project's existing backend run configuration and commands.

If the backend requires a background process, run it in a way that does not block the CLI session (e.g., using the harness background job facilities).

Then verify application health using the project's actual health endpoint or an appropriate HTTP request such as `curl` or an equivalent command.

The Agent must inspect:
* Startup logs;
* Console logs;
* Application logs;
* Database connection;
* Flyway migration output;
* Bean initialization;
* Endpoint availability;
* Runtime exceptions.

If any relevant error occurs, fix it. Errors include, but are not limited to:
* Java syntax errors;
* Compilation errors;
* Startup errors;
* Dependency errors;
* Configuration errors;
* Flyway errors;
* Database errors;
* Bean initialization errors;
* Runtime exceptions;
* Endpoint errors;
* Serialization errors;
* Validation errors;
* Authorization errors;
* Encoding errors.

The required loop is:
```text
IMPLEMENT → RUN BACKEND → CHECK LOGS → IDENTIFY ROOT CAUSE → FIX → RUN BACKEND AGAIN → CHECK LOGS AGAIN → REPEAT
```

Do not report backend runtime validation as PASS unless the backend was actually started and the logs/health were checked.

---

## 3. Backend Test Execution (Section 11)

After the backend can successfully start, run the backend tests under:

`backend/src/test/java/vn/nguongocso`

Run the relevant test classes and the broader relevant backend test suite according to the project's build system.

Verify:
* Java syntax;
* Compilation;
* Test execution;
* Database setup;
* Migration setup;
* Assertions;
* Runtime behavior.

If any test fails:
1. Inspect the actual failure output.
2. Identify the root cause.
3. Read the relevant implementation.
4. Fix the appropriate source code or test setup.
5. Rerun the affected test.
6. Rerun the broader relevant test suite.

Continue until all relevant tests pass.

Do not:
* Skip tests;
* Disable tests;
* Remove assertions;
* Weaken assertions;
* Suppress errors;
* Ignore exceptions;
* Change expected behavior only to make tests pass.

---

## 4. Backend Error Retry Limit (Section 12)

To prevent infinite debugging loops, each distinct blocking error is subject to the **5-Attempt Error Retry Limit**.

For full rules and reporting requirements when the limit is reached, see [AGENTS.md](../../AGENTS.md#cross-cutting-rules) (Rule 1).
