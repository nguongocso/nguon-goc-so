# Git Workflow and Safety Conventions

This document covers the required Git workflow for User Story implementation, including branch setup, continuous commits, and safety checks before every commit.

---

## 1. Git Branch Setup

First, inspect the current Git state.

Start from the `develop` branch.

Make sure the local repository is clean enough to safely start the User Story. Do not overwrite or discard unrelated user changes.

Update the local `develop` branch from the appropriate remote if the project's normal Git workflow requires it.

Create a new feature branch from `develop` and immediately switch to it.

The branch name must contain:
- The User Story ID;
- The User Story name translated into English;
- Clear `kebab-case`.

**Example:**
```text
feature/NCL-08-CN-013-unlock-tag-code-after-verification
```

Do not create the feature branch from another feature branch.

Verify the branch with:
```bash
git branch --show-current
git status
```

---

## 2. Continuous Git Commits

Commit continuously after each meaningful development context.

Do not create one huge final commit.

Recommended logical contexts include:
- API documentation;
- Database migration;
- Platform seed data;
- Backend entities / repositories / enums;
- Backend DTOs;
- Backend services / business logic;
- Backend controllers / endpoints;
- Backend authorization;
- Backend history / audit;
- Backend notification;
- Backend tests;
- Backend runtime fixes;
- Test seed data;
- Frontend Axios / API integration;
- Frontend interfaces / types;
- Frontend components;
- Frontend pages / layout;
- Frontend routing;
- Frontend role access;
- Frontend UI / UX;
- Frontend runtime fixes;
- Frontend tests;
- Final validation / fixes.

Do not create meaningless micro-commits.

Combine closely related changes into a coherent commit when appropriate.

---

## 3. Git Safety Before Every Commit

Before every commit, run:
```bash
git status
git diff
```

Review the actual changes.

Ensure the commit contains only changes related to the current User Story.

Never commit:
- Unrelated changes;
- Secrets;
- API keys;
- Credentials;
- Environment secrets;
- Debugging code;
- Temporary files;
- Generated unnecessary files;
- Unrelated refactoring;
- Accidental formatting changes;
- Unnecessary seed data.

If unrelated changes already existed before the User Story, preserve them and do not include them in the User Story commits.
