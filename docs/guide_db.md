# Hướng dẫn thiết lập Database — Nguồn Gốc Số

> **QUAN TRỌNG:** Flyway là nguồn sự thật (source of truth) cho database schema và master data.
> **TUYỆT ĐỐI KHÔNG** sử dụng `spring.jpa.hibernate.ddl-auto=update`.

---

## 1. Yêu cầu hệ thống

- **MySQL** 8.0+ (đang chạy)
- **Java** 21
- **Maven** (wrapper `mvnw.cmd` đã có sẵn)

---

## 2. Tạo database

Kết nối MySQL và tạo database rỗng:

```sql
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> **Lưu ý:** Port mặc định là `3307` (xem `application.properties`). Điều chỉnh nếu cần qua biến môi trường `DB_PORT`.

---

## 3. Cấu hình datasource

Cấu hình kết nối MySQL thông qua file `backend/.env`:

```env
DB_HOST=localhost
DB_PORT=3307
DB_NAME=nguon_goc_so
DB_USERNAME=root
DB_PASSWORD=your_password_here
```

Nếu không tạo file `.env`, application sẽ dùng giá trị mặc định trong `application.properties`:
- Host: `localhost`
- Port: `3307`
- Database: `nguon_goc_so`
- Username: `root`
- Password: (rỗng)

---

## 4. Khởi động Backend

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

**Flyway sẽ TỰ ĐỘNG:**
1. Tạo toàn bộ schema (các bảng, khóa ngoại, index)
2. Seed dữ liệu master:
   - 6 Roles (VT-01 đến VT-06)
   - ~68 Permissions
   - Role-Permission mappings cho tất cả roles
   - 1 tài khoản Admin mặc định
   - 1 Organization mặc định ("Hệ thống")
   - Cấu hình backup schedule mặc định

---

## 5. Cấu trúc thư mục migration

```
backend/src/main/resources/db/migration/
├── schema/          ← Schema migrations (CREATE TABLE, ALTER TABLE, ...)
│   ├── V1__create_auth_tables.sql
│   ├── V2__create_product_categories.sql
│   ├── V3__create_farm_areas.sql
│   ├── V4__create_production_lot.sql
│   ├── V5__create_farm_logs.sql
│   ├── V6__create_shipments.sql
│   ├── V7__create_standards_and_certifications.sql
│   ├── V8__create_alerts_and_notifications.sql
│   ├── V9__create_chain_events_and_code_ranges.sql
│   ├── V10__create_logging_tables.sql
│   ├── V11__create_export_and_import_history.sql
│   ├── V12__create_backup_tables.sql
│   └── V13__create_permission_mapping_tables.sql
│
└── data/            ← Master data migrations (INSERT seed data)
    ├── V14__seed_roles.sql
    ├── V15__seed_permissions.sql
    ├── V16__seed_role_permissions.sql
    ├── V17__seed_default_admin.sql
    └── V18__seed_backup_schedule.sql
```

---

## 6. Tài khoản Admin mặc định (DEVELOPMENT ONLY)

| Field    | Value       |
|----------|-------------|
| Username | `admin`     |
| Password | `Admin@123` |

> ⚠️ **CHỈ DÙNG CHO MÔI TRƯỜNG DEVELOPMENT.** Đổi mật khẩu ngay khi triển khai production.

---

## 7. Roles mặc định

| Code  | Name          | Mô tả                       |
|-------|---------------|-----------------------------|
| VT-01 | ADMIN         | Quản trị viên hệ thống      |
| VT-02 | ORG_MANAGER   | Quản lý hợp tác xã          |
| VT-03 | EVENT_RECORDER| Người ghi sự kiện           |
| VT-04 | PROCUREMENT   | Doanh nghiệp thu mua        |
| VT-05 | REGULATOR     | Cán bộ quản lý ngành        |
| VT-06 | CONSUMER      | Người tiêu dùng             |

---

## 8. Permissions mặc định

### ADMIN (VT-01) — Toàn quyền hệ thống

Được cấp **tất cả** 68 permissions. Admin có quyền truy cập mọi chức năng.

### ORG_MANAGER (VT-02) — Quản lý hợp tác xã (~43 permissions)

| Resource | Actions |
|---|---|
| organization | READ, UPDATE |
| farm_area | CREATE, READ, UPDATE, DELETE |
| production_lot | CREATE, READ, UPDATE, APPROVE |
| farm_log | READ, VERIFY |
| shipment | CREATE, READ, UPDATE, EXPORT |
| trace_code | READ |
| chain_event | READ |
| certification | CREATE, READ, UPDATE |
| standard | READ |
| product_category | READ |
| organization_user | CREATE, READ, UPDATE, DELETE |
| role_permission | READ, UPDATE |
| code_range | CREATE, READ, UPDATE |
| recall | CREATE, READ |
| export | CREATE, READ |
| report | READ, EXPORT |
| notification | READ |
| alert | READ |
| scan_statistics | READ |
| activity_log | READ |
| product_feedback | READ |
| traceability | READ |

### EVENT_RECORDER (VT-03) — Ghi nhật ký đồng ruộng (~10 permissions)

| Resource | Actions |
|---|---|
| farm_area | READ |
| production_lot | READ |
| farm_log | CREATE, READ, UPDATE |
| chain_event | CREATE, READ |
| shipment | READ |
| trace_code | READ |
| notification | READ |

### PROCUREMENT (VT-04) — Doanh nghiệp thu mua (~10 permissions)

| Resource | Actions |
|---|---|
| production_lot | READ |
| farm_log | READ |
| shipment | READ |
| trace_code | READ, ACTIVATE |
| chain_event | CREATE, READ |
| traceability | READ |
| notification | READ |
| product_feedback | READ |

### REGULATOR (VT-05) — Cơ quan quản lý (~19 permissions)

| Resource | Actions |
|---|---|
| organization | READ |
| farm_area | READ |
| production_lot | READ |
| farm_log | READ |
| shipment | READ |
| trace_code | READ |
| chain_event | READ |
| certification | READ |
| standard | READ |
| product_category | READ |
| report | READ, EXPORT |
| scan_statistics | READ |
| activity_log | READ |
| product_feedback | READ |
| traceability | READ |
| recall | READ |
| alert | READ |
| notification | READ |

### CONSUMER (VT-06) — Người tiêu dùng (~2 permissions)

| Resource | Actions |
|---|---|
| traceability | READ |
| product_feedback | READ |

---

## 9. Kiểm tra trạng thái Flyway

Sau khi khởi động backend, kiểm tra log để xác nhận tất cả migrations đã chạy thành công:

```
Flyway migration executed successfully
```

Hoặc kiểm tra bảng `flyway_schema_history` trong MySQL:

```sql
SELECT version, description, type, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Bạn sẽ thấy 18 migrations đã được áp dụng thành công (success = 1).

---

## 10. Reset database (KHI CẦN THIẾT)

### 10.1 Tại sao cần reset?

- Database hiện tại có schema được tạo bằng `ddl-auto=update`, không tương thích với lịch sử Flyway mới.
- Migration files cũ đã bị xóa và thay thế bằng cấu trúc mới.

### 10.2 Dữ liệu bị mất khi reset

- **TOÀN BỘ** dữ liệu business: organizations, users, farm areas, production lots, shipments, trace codes, ...
- Flyway schema history cũ.

### 10.3 Backup dữ liệu (nếu cần)

```bash
mysqldump -u root -p nguon_goc_so > backup_before_reset.sql
```

### 10.4 Quy trình reset

```sql
-- 1. Xóa database cũ
DROP DATABASE IF EXISTS nguon_goc_so;

-- 2. Tạo database mới
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó khởi động backend — Flyway sẽ tự động tạo lại toàn bộ schema và seed data.

---

## 11. Các lỗi Flyway thường gặp

### 11.1 Checksum mismatch

```
Migration checksum mismatch for migration version X
```

**Nguyên nhân:** File migration đã bị sửa đổi sau khi đã được áp dụng.

**Cách xử lý (DEVELOPMENT ONLY):**
```sql
-- Xóa toàn bộ lịch sử Flyway và chạy lại từ đầu
DROP DATABASE IF EXISTS nguon_goc_so;
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 11.2 Found non-empty schema without metadata table

Flyway không tìm thấy bảng `flyway_schema_history` nhưng database đã có bảng.

**Cách xử lý:** Nếu database có thể xóa được, thực hiện reset như mục 10.4.

---

## 12. Team workflow cho thay đổi database

Khi một feature cần thay đổi database:

1. **Sửa @Entity model** (thêm field, sửa relationship, ...)
2. **Tạo Flyway migration mới** trong `schema/` với version tiếp theo:
   - VD: `V19__add_column_to_production_lot.sql`
3. **Thêm master data** vào `data/` nếu cần:
   - VD: `V20__seed_new_permissions.sql`
4. **Chạy ứng dụng** với database local để verify
5. **Kiểm tra log** — Flyway phải chạy thành công, Hibernate validate không báo lỗi
6. **Chạy tests** — `.\mvnw.cmd test`
7. **Commit** Entity + Migration + Documentation cùng nhau

### KHÔNG BAO GIỜ:

- ❌ Dùng `spring.jpa.hibernate.ddl-auto=update`
- ❌ Sửa file migration đã được áp dụng
- ❌ Tự tay INSERT permissions vào MySQL
- ❌ Tự tay sửa database schema
- ❌ Dùng `INSERT IGNORE` để che giấu lỗi dữ liệu

---

## 13. Cấu hình Hibernate

```
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate chỉ **validate** schema khớp với @Entity model. Mọi thay đổi schema phải thông qua Flyway migration.

---

## 14. Cấu hình Flyway

```
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration/schema,classpath:db/migration/data
```

Flyway quét cả 2 thư mục con:
- `schema/` — Schema migrations (chạy trước)
- `data/` — Data migrations (chạy sau, theo version)

Migrations được sắp xếp theo version number (V1, V2, ...) bất kể thư mục.

---
*Cập nhật lần cuối: 2026-08-08*