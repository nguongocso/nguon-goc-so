# Mandatory Codebase Reading and Understanding

Before writing any code, the Agent MUST inspect and understand the actual codebase.

Do not guess.

Do not invent architecture.

Do not assume that a layer, endpoint, entity, service, component, migration, permission, or business rule exists or does not exist without checking the repository.

---

## 1. Required Areas of Understanding

The Agent must read the actual implementation and relevant files needed to understand:

* Project structure;
* Backend architecture;
* Frontend architecture;
* Existing API documentation;
* Entities;
* Repositories;
* DTOs;
* Enums;
* Services;
* Controllers;
* Exception handling;
* Validation;
* Authorization;
* Role/access-control mechanisms;
* State/status transitions;
* History/audit mechanisms;
* Notification mechanisms;
* Database schema;
* Existing Flyway migrations;
* Migration conventions;
* Seed data;
* Test configuration;
* Backend tests;
* Frontend tests;
* Frontend API clients;
* Interfaces/types;
* Components;
* Pages;
* Routing;
* Role access;
* Existing UI/UX patterns;
* Application configuration;
* Runtime configuration;
* Related implementation patterns.

The Agent must use actual source code as the primary source of truth.

If the required behavior can be determined by reading the codebase, the Agent MUST read the relevant code instead of guessing.

Do not blindly rely on the User Story description when the repository contains a more precise existing implementation or business flow.

Do not create a new mechanism when an existing mechanism can be reused.

---

## 2. Mandatory Search Queries Before Decisions

Before making implementation decisions, search the repository for:

* The current User Story ID;
* Related User Stories;
* Predecessor User Stories;
* Successor User Stories;
* Related business rules;
* Related entities;
* Related API endpoints;
* Related services;
* Related status/state transitions;
* Related history/audit implementations;
* Related authorization/role-access implementations;
* Related notifications;
* Similar UI flows;
* Similar tests;
* Similar migrations;
* Similar seed data.

*Example:* If the current User Story is related to locking/unlocking a tag code, inspect the existing lock implementation before designing the unlock implementation.

The Agent must understand the existing business flow before modifying it.
