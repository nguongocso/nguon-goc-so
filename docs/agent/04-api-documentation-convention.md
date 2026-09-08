# API Documentation First Convention

After the analysis and plan are complete, implement the API documentation first.

API documentation must be created or updated under:

`docs/api`

Follow the existing API documentation conventions in the repository.

---

## 1. Inspection Before Documentation

Before creating new documentation, inspect existing API documentation and follow its:

* Naming;
* Structure;
* Endpoint format;
* Request format;
* Response format;
* Error format;
* Authentication documentation;
* Authorization documentation;
* Examples.

---

## 2. Contract-First Integrity

The API documentation must accurately describe the API contract that will actually be implemented.

* Do not document an API that the backend will not implement.
* Do not implement an API that contradicts the API documentation.

---

## 3. Dedicated Git Commit

After completing API documentation:

1. Inspect `git diff`.
2. Inspect `git status`.
3. Verify that only User Story-related documentation changes exist.
4. Commit the API documentation separately before proceeding to backend implementation.
