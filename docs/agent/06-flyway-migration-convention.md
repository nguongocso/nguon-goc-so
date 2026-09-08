# Flyway Migration Convention

When database changes are required, create or update Flyway migration files under:

`backend/src/main/resources/db/migration`

---

## 1. Mandatory Timestamp-Based Versioning

**IMPORTANT:** All newly created Flyway migration files MUST use timestamp-based versioning.

The required format is:

```text
V<timestamp>__<description>.sql
```

**Example:**
```text
V20260830113500__add_unlock_fields_to_trace_codes.sql
```

**DO NOT** create new migrations using sequential numeric versions such as:
```text
V60__add_unlock_fields_to_trace_codes.sql
V61__add_unlock_fields_to_trace_codes.sql
V62__add_unlock_fields_to_trace_codes.sql
```

Do not simply rename a sequential migration and claim that timestamp versioning has been implemented.

---

## 2. Pre-Migration Checklist

Before creating a migration:

1. Read the existing migration directory.
2. Inspect the existing migration naming/versioning convention.
3. Determine whether the project already uses timestamp-based versions.
4. Identify the latest existing migration version.
5. Generate a timestamp-based version representing the actual creation time.
6. Ensure that the timestamp does not conflict with any existing migration.
7. Ensure the timestamp ordering is valid.
8. Verify the final filename before committing.

Every new migration must follow:
```text
V<timestamp>__<description>.sql
```

The Agent must explicitly verify migration filenames before commit.

---

## 3. Immutability of Applied Migrations

* Do not modify already-applied migrations unless the project's established migration strategy explicitly requires it.
* If an already-applied migration needs new behavior, create a new migration instead of modifying the old one.
