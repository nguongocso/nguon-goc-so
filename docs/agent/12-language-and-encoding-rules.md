# Language and UTF-8 Encoding Rules

This document sets forth mandatory rules for comment language, documentation conventions, and character encoding across all source files.

---

## 1. JavaDoc, Comments and Language (Section 18)

JavaDoc and source-code comments must be written in Vietnamese.

When creating or modifying:
* JavaDoc;
* Block comments;
* Inline comments;

use clear Vietnamese with correct Vietnamese characters.

Do not write new JavaDoc/comments in English unless required by:
* Framework;
* Library;
* Annotation;
* Generated code;
* Unavoidable technical convention.

Do not unnecessarily rewrite unrelated comments.

---

## 2. UTF-8 Encoding (Section 19)

All newly created or modified source files must use valid UTF-8 encoding.

The Agent must inspect modified files for:
* Mojibake;
* Corrupted Vietnamese characters;
* Malformed UTF-8;
* Encoding inconsistencies;
* Broken JavaDoc;
* Broken comments;
* Broken string literals.

If a modified file already contains an encoding problem and fixing it is necessary for correctness/readability, fix it safely.

Do not modify unrelated business logic while fixing encoding.

Before commit, verify that modified files remain valid UTF-8.
