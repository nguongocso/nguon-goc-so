# Hướng dẫn thiết lập Database — Nguồn Gốc Số

> **QUAN TRỌNG:** Flyway là nguồn sự thật cho database schema và master data.
> `spring.jpa.hibernate.ddl-auto=validate` (không phải `update`).

---

## 1. Yêu cầu hệ thống

- **MySQL** 8.4 (theo `docker-compose.yml` và `.env.example`)
- **Port mặc định:** `3306` (theo `DB_PORT=3306`; không phải `3307`)
- **Java / Maven / Wrapper:** 21 / 3.9+ (`mvnw.cmd`)

---

## 2. Tạo database (nếu dùng MySQL ngoài Docker)

```sql
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Nếu dùng Docker Compose, service `mysql` tự tạo DB từ `MYSQL_DATABASE=${DB_NAME}`.

---

## 3. Cấu hình kết nối (chọn đúng `.env` cho flow)

**Flow A — Local development (backend chạy trực tiếp, DB trên host hoặc Docker mysql):**

Sử dụng `backend/.env` (tạo từ `backend/.env.example`):

```env
DB_HOST=localhost        # hoặc host.docker.internal nếu mysql chạy trong Docker
DB_PORT=3306
DB_NAME=nguon_goc_so
DB_USERNAME=nguongocso
DB_PASSWORD=your_password
```

**Flow B — Docker Compose (toàn bộ trong container):**

Sử dụng `root .env` (tạo từ `.env.example`):

```env
DB_HOST=mysql
DB_PORT=3306
DB_NAME=nguon_goc_so
DB_USERNAME=nguongocso
DB_PASSWORD=your_password
MYSQL_ROOT_PASSWORD=your_root_password
```

> **Không dùng `DB_USERNAME=root` hoặc `DB_PASSWORD=` rỗng** trừ khi thực sự cấu hình như vậy trong `.env`.

---

## 4. Khởi động Backend và Flyway

```powershell
cd backend
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

Flyway tự động khi khởi động (`spring.flyway.enabled=true`):

- Quét `classpath:db/migration/schema` và `classpath:db/migration/data`
- Chạy các file `V...__...sql` theo thứ tự version
- Tạo bảng `flyway_schema_history` để theo dõi

Kiểm tra log để xác nhận: tìm dòng `Flyway migration executed successfully`.

---

## 5. Cấu trúc thư mục migration (hiện tại)

```
backend/src/main/resources/db/migration/
├── schema/     # Schema migrations (CREATE / ALTER / INDEX ...)
└── data/       # Data / seed migrations (INSERT ...)
```

Các file có tên theo quy tắc `V{version}__{description}.sql`. Kiểm tra trực tiếp thư mục để xem danh sách hiện tại (không hard-code số lượng ví dụ V1-V18).

---

## 6. Tài khoản Admin mặc định (DEVELOPMENT ONLY)

Theo seed data `backend/src/main/resources/db/migration/data/V17__seed_default_admin.sql`:

| Field    | Value       |
|----------|-------------|
| Username | `admin`     |
| Password | `admin123`  |

> **Chỉ dùng cho dev/demo.** Phải đổi trước khi triển khai production.

---

## 7. Kiểm tra trạng thái Flyway

```sql
SELECT version, description, type, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

- Nếu `success = 1`: migration đã chạy thành công.
- Nếu `checksum mismatch`: file migration đã sửa sau khi áp dụng → tạo migration mới, không sửa file đã áp dụng.

---

## 8. Reset database (KHI CẦN)

### Tại sao cần reset?

- Database có schema từ `ddl-auto=update` (không khớp Flyway).
- Đã sửa file migration đã áp dụng (checksum mismatch).
- Cần khởi động từ trạng thái sạch để verify migration mới.

### Dữ liệu bị mất

Toàn bộ business data (organizations, users, farm areas, production lots, ...). Chỉ dùng cho development.

### Backup trước reset (nếu cần giữ data)

```bash
mysqldump -u nguongocso -p nguon_goc_so > backup_before_reset.sql
```

### Quy trình reset

```sql
DROP DATABASE IF EXISTS nguon_goc_so;
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó khởi động lại backend — Flyway tự chạy lại từ đầu.

Nếu dùng Docker Compose: `docker compose -f docker-compose.yml down -v` (xóa volume DB), rồi `up -d --build`.

---

## 9. Các lỗi Flyway thường gặp

| Lỗi | Nguyên nhân | Cách xử lý (development) |
|-----|-------------|--------------------------|
| Checksum mismatch | Đã sửa file migration đã áp dụng | Tạo migration mới (`V...__...`); hoặc reset DB (xem §8) |
| Found non-empty schema without metadata | DB có bảng nhưng không có `flyway_schema_history` | Reset DB (xem §8) nếu có thể xóa; ngoài ra phải khớp thủ công |
| Migration fail (syntax / FK / ...) | SQL lỗi hoặc dependency chưa tạo | Sửa SQL; kiểm tra thứ tự version; đảm bảo `data/` chạy sau `schema/` nếu cần |

---

## 10. Quy trình tạo migration mới

1. Sửa `@Entity` / model.
2. Tạo file mới trong `backend/src/main/resources/db/migration/schema/` (nếu thay đổi schema) hoặc `data/` (nếu seed):
   - Ví dụ: `V50__add_new_column.sql` (phiên bản tiếp theo so với cao nhất hiện tại).
3. Chạy local để verify.
4. Commit cùng entity + migration.

---

## 11. Cấu hình Hibernate / Flyway (từ repository)

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.repair-on-migrate=true
spring.flyway.validate-on-migrate=false
spring.flyway.out-of-order=true
spring.flyway.locations=classpath:db/migration/schema,classpath:db/migration/data
```

---

*Cập nhật lần cuối theo repository `develop` — không hard-code số migration hay password cũ.*
