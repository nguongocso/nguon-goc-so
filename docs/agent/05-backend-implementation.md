# Backend Implementation Guidelines

Based on the API documentation, implement the backend.

Inspect existing backend patterns before creating or modifying code.

---

## 1. Required Backend Components

Implement only the components actually required by the User Story and architecture, such as:

* Entities;
* Repositories;
* Enums;
* DTOs;
* Mappers;
* Services;
* Business logic;
* Validation;
* Authorization;
* Controllers;
* Endpoints;
* State transitions;
* History / audit;
* Notifications;
* Exception handling;
* Other required backend components.

---

## 2. Reuse Existing Architecture

* Reuse existing architecture whenever possible.
* Do not create duplicate services, utilities, access-control mechanisms, history mechanisms, or notification mechanisms if equivalent implementations already exist.

---

## 3. Server-Side Business Rule Enforcement

* Business rules must be enforced at the appropriate backend layer.
* Do not rely only on frontend validation for business-critical rules.
