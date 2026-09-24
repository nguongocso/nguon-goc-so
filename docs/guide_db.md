# Hướng dẫn thiết lập Database — Nguồn Gốc Số

> **QUAN TRỌNG:** Flyway là nguồn sự thật cho database schema và master data.
> `spring.jpa.hibernate.ddl-auto=validate` (không phải `update`).

---

## 1. Yêu cầu hệ thống

- **MySQL** 8.4 (theo `docker-compose.yml` và `.env.example`) — Windows đã có **XAMPP** thì dùng luôn MySQL của XAMPP (xem mục 2.2)
- **Port mặc định:** `3306` 
- **Java / Maven / Wrapper:** 21 / 3.9+ (`mvnw.cmd`)

---

## 2. Tạo database & user (nếu dùng MySQL ngoài Docker)

### 2.1 MySQL cài riêng trên máy

Đăng nhập MySQL bằng quyền `root` (`mysql -u root -p`) rồi chạy:

```sql
CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nguongocso'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON nguon_goc_so.* TO 'nguongocso'@'localhost';
FLUSH PRIVILEGES;
```

- `your_password` phải khớp `DB_PASSWORD` trong `backend/.env`; user `nguongocso` khớp `DB_USERNAME` trong `.env.example`.
- Kiểm tra: `mysql -u nguongocso -p -e "SELECT 1;"` → không báo lỗi.
- **Thiếu `CREATE USER` + `GRANT`** sẽ gặp `Access denied for user 'nguongocso'@'localhost'` khi chạy backend.

### 2.2 Windows — dùng MySQL của XAMPP

1. Mở **XAMPP Control Panel** → bấm **Start** ở module **MySQL** (port mặc định `3306`).
   - Kiểm tra: `netstat -ano | findstr 3306` → có tiến trình đang listen.
   - Nếu XAMPP đang để port `3307`: đặt `DB_PORT=3307` trong `backend/.env` (lưu ý repo đang mặc định `3306`).
2. Mở phpMyAdmin tại `http://localhost/phpmyadmin` (user `root`, mặc định **không có mật khẩu** trên XAMPP) → tab **SQL** → chạy toàn bộ đoạn SQL ở mục **2.1**.
   - Root đã có mật khẩu / muốn đặt mật khẩu: dùng mục **Security** trong XAMPP, hoặc chạy `ALTER USER 'root'@'localhost' IDENTIFIED BY 'mat_khau_moi';`.
3. Điền `backend/.env`: `DB_HOST=localhost`, `DB_PORT=3306`, `DB_USERNAME=nguongocso`, `DB_PASSWORD=your_password`.
4. **MySQL của XAMPP phải ở trạng thái Start** trước mỗi lần chạy backend (vừa mở máy thì vào Control Panel bấm Start lại).

> ⚠️ XAMPP thường đi kèm **MariaDB** (tương thích ngược MySQL). Dự án chuẩn hoá trên **MySQL 8.4** (theo `docker-compose.yml`; staging/production dùng RDS MySQL). Nếu Flyway migrate báo lỗi lạ, hãy thử lại với MySQL 8.4 thật (mục 2.1 hoặc 2.3).

### 2.3 (Tuỳ chọn) Chỉ chạy MySQL bằng Docker, backend vẫn chạy source

```powershell
docker run -d --name ngs-mysql -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=your_root_password `
  -e MYSQL_DATABASE=nguon_goc_so `
  -e MYSQL_USER=nguongocso -e MYSQL_PASSWORD=your_password `
  mysql:8.4
```

- `backend/.env`: `DB_HOST=localhost` (cổng `3306` đã publish ra máy host).
- Gỡ khi không dùng: `docker rm -f ngs-mysql`.

### 2.4 Nếu dùng Docker Compose (Flow B)

Không cần thao tác thủ công: service `mysql` tự tạo DB/user từ `MYSQL_DATABASE=${DB_NAME}`, `MYSQL_USER=${DB_USERNAME}`, `MYSQL_PASSWORD=${DB_PASSWORD}` trong **root `.env`** (kèm `MYSQL_ROOT_PASSWORD`), và chỉ start backend sau khi mysql `healthy`.

---

## 3. Cấu hình kết nối (chọn đúng `.env` cho flow)

**Flow A — Local development (backend chạy trực tiếp, DB trên host hoặc Docker mysql):**

Sử dụng `backend/.env` (tạo từ `backend/.env.example`):

```env
DB_HOST=localhost        # MySQL máy / XAMPP / Docker đã -p 3306:3306 → localhost; host.docker.internal chỉ dùng khi backend chạy TRONG container
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
