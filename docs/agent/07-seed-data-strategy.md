# Database and Frontend Seed Data Strategy

This document defines the seed data strategy for both platform baselines and frontend UI/test scenarios.

---

## 1. Database Seed Data Strategy (Section 8)

The Agent MUST inspect the existing database seed strategy before creating seed data.

The project contains separate locations for production/platform seed data and test/development seed data:

### Production / Platform Baseline Seed Data
Use:
```text
backend/src/main/resources/db/migration
```
for:
* Production/platform database migrations;
* Production schema changes;
* Platform-level seed data;
* Essential system data;
* Default roles;
* Default permissions;
* Required platform/system configuration;
* Foundational accounts such as the required admin/platform administrator account;
* Other data that must exist as part of the application's platform baseline.

### Test / Development Seed Data
Use:
```text
backend/src/main/resources/db/migration-test
```
for:
* Test-only seed data;
* Development/sample data;
* UI testing data;
* Test scenarios;
* Non-platform entities;
* Temporary or scenario-specific data;
* Data required to exercise Acceptance Criteria through the UI;
* Data required for integration tests when the project's existing test strategy uses this location.

### Strict Separation Rules
* **Do NOT** put test-only/sample data into `backend/src/main/resources/db/migration` unless the data is genuinely required as platform/system baseline data.
* **Do NOT** put foundational platform data such as required administrator accounts, default system roles, or mandatory platform configuration into `backend/src/main/resources/db/migration-test` when such data is required for the actual application baseline.
* Before creating seed data, inspect existing seed files and follow their established conventions.
* Do not create unnecessary seed data.
* Seed data must be deterministic, valid according to the current schema, and compatible with the existing business rules.
* **Never include:**
  * Real user credentials;
  * API keys;
  * Production secrets;
  * Real personal data;
  * Environment credentials.

---

## 2. Frontend Seed Data and UI Testing (Section 14)

Before UI testing, determine whether realistic sample data is required.

If existing data is insufficient, create appropriate test/development seed data under:
```text
backend/src/main/resources/db/migration-test
```
when this is consistent with the project's existing seed strategy.

Use test seed data to support:
* Normal flows;
* Different statuses;
* Role-based access;
* Related entities;
* History / timeline;
* Empty states;
* Validation states;
* Error states;
* State transitions;
* Notification scenarios;
* Acceptance Criteria;
* Edge cases.

Do not pollute production/platform seed data with scenario-specific UI test data.

The Agent must ensure that seed data actually supports the UI flow being tested.
