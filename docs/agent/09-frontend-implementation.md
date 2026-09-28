# Frontend Implementation Guidelines

This document defines the rules and requirements for frontend development.

---

## 1. Prerequisites Before Starting Frontend

Only proceed to frontend after:
* Backend implementation is complete;
* Backend runtime validation succeeds;
* Relevant backend tests pass.

---

## 2. Implementation Scope and Alignment

Implement frontend changes according to the existing frontend architecture.

Before creating new components or patterns, inspect existing frontend implementations.

Implement only what is required, such as:
* Axios / API integration;
* Interfaces / types;
* Enums;
* Reusable components;
* Forms;
* Dialogs / modals;
* Tables;
* History / timeline;
* Layout;
* Pages;
* Routing;
* `AppRoutes` or equivalent;
* `roleAccess` or equivalent;
* Validation;
* Loading states;
* Error states;
* Success states;
* UI / UX.

---

## 3. Reuse Existing Patterns

Reuse existing:
* API clients;
* Components;
* Hooks;
* Types;
* Form patterns;
* Validation patterns;
* Routing patterns;
* Role-access mechanisms;
* Design patterns.

Do not introduce unnecessary new architecture.
