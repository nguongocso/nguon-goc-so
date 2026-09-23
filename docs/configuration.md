# Hướng dẫn Cấu hình — Nguồn Gốc Số

> Tài liệu này tổng hợp toàn bộ biến môi trường và cấu hình quan trọng từ `.env.example`, `docker-compose.yml`, `application.properties`, `.env` frontend. Không ghi secret thật.

---

## 1. What

Mô tả cách cấu hình hệ thống cho local development, staging (`staging.agri-trace.online`), và production (`agri-trace.online`).

## 2. Why

Nếu một biến bị thiếu hoặc sai, backend sẽ không kết nối DB, JWT không hoạt động, upload bị từ chối, hoặc CORS chặn frontend.

## 3. Prerequisites

- Đã clone repository.
- Đã tạo `.env` từ `.env.example`.

---

## 4. Bảng biến môi trường (đã xác minh từ `.env.example` + `docker-compose.yml` + `application.properties`)

| Variable | Service | Required | Example | Purpose | Default |
|----------|---------|----------|---------|---------|---------|
| `DB_HOST` | backend / mysql | Có | `localhost` hoặc `mysql` (Docker) | Host MySQL | `mysql` (Docker) |
| `DB_PORT` | backend / mysql | Có | `3306` | Port MySQL | `3306` |
| `DB_NAME` | backend / mysql | Có | `nguon_goc_so` | Tên database | `nguon_goc_so` |
| `DB_USERNAME` | backend / mysql | Có | `nguongocso` | User DB | `nguongocso` |
| `DB_PASSWORD` | backend / mysql | Có | (placeholder) | Mật khẩu DB | — |
| `MYSQL_ROOT_PASSWORD` | mysql (Docker) | Có | (placeholder) | Root password cho container MySQL | — |
| `PORT` | backend | Khuyến nghị | `8080` | Port backend | `8080` |
| `JWT_SECRET` | backend | Có | `your_secret_key` | Secret ký JWT | — |
| `JWT_EXPIRATION` | backend | Khuyến nghị | `86400000` | Thời hạn JWT (ms) | `86400000` |
| `ALLOWED_ORIGINS` | backend | Có | `http://localhost:3000` (dev server `vite.config.ts` port 3000) | CORS origins | `http://localhost:3000` |
| `UPLOAD_BASE_DIR` | backend | Có | `/app/uploads` | Thư mục gốc upload | `/app/uploads` |
| `UPLOAD_FARM_LOG_MAX_SIZE` | backend | Khuyến nghị | `5242880` | Kích thước tối đa tệp farm log (bytes) | `5242880` |
| `UPLOAD_INSPECTION_RESULT_MAX_SIZE` | backend | Khuyến nghị | `5242880` | Kích thước tối đa kết quả kiểm nghiệm | `5242880` |
| `FARM_AREA_BOUNDARY_DEVIATION_THRESHOLD_PERCENT` | backend | Khuyến nghị | `30.0` | Ngưỡng sai lệch ranh giới vùng trồng (%) | `30.0` |
| `QR_IMAGE_STORAGE_PATH` | backend | Có | `/app/files/qr` | Đường dẫn lưu ảnh QR | `/app/files/qr` |
| `MYSQL_DUMP_PATH` | backend | Có (backup) | `/usr/bin/mysqldump` | Đường dẫn `mysqldump` | `/usr/bin/mysqldump` |
| `MYSQL_PATH` | backend | Có (restore) | `/usr/bin/mysql` | Đường dẫn `mysql` CLI | `/usr/bin/mysql` |
| `BACKUP_LOCAL_DIR` | backend | Có (backup) | `/app/backups` | Thư mục sao lưu cục bộ | `/app/backups` |
| `FRONTEND_URL` | backend | Khuyến nghị | `http://localhost:3000` | URL frontend cho email/invitation | `http://localhost:3000` |
| `MAIL_HOST` | backend | Tùy chọn | `smtp.gmail.com` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | backend | Tùy chọn | `587` | SMTP port | `587` |
| `MAIL_USERNAME` | backend | Tùy chọn | `your_email` | Tài khoản gửi mail | — |
| `MAIL_PASSWORD` | backend | Tùy chọn | `your_app_password` | Mật khẩu ứng dụng | — |
| `LOCATIONIQ_API_KEY` | backend | Tùy chọn | (placeholder) | API key LocationIQ | — |
| `LOCATIONIQ_BASE_URL` | backend | Khuyến nghị | `https://us1.locationiq.com/v1/reverse` | Base URL LocationIQ | Đã đặt sẵn |
| `APP_TIMEZONE` | backend | Khuyến nghị | `Asia/Ho_Chi_Minh` | Múi giờ nghiệp vụ | `Asia/Ho_Chi_Minh` |
| `VITE_API_URL` | frontend | Có | `http://localhost:8080/api/v1` | API base URL cho frontend | `http://localhost:8080` |
| `FRONTEND_HOST_PORT` | docker-compose | Khuyến nghị | `3000` | Port host cho frontend container | `3000` |
| `BACKEND_HOST_PORT` | docker-compose | Khuyến nghị | `8080` | Port host cho backend container | `8080` |

---

## 5. Cấu hình Spring Boot (`application.properties`)

File gốc: `backend/src/main/resources/application.properties`.

Các cấu hình chính đã xác minh:

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&...
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
app.jwt.secret=${JWT_SECRET}
app.jwt.expiration=${JWT_EXPIRATION}
app.cors.allowed-origins=${ALLOWED_ORIGINS}
app.upload.base-dir=${UPLOAD_BASE_DIR}
```

> `ddl-auto=validate`: Hibernate chỉ kiểm tra schema, không tạo bảng. Flyway (`db/migration/`) là nguồn sự thật cho schema.

---

## 6. Profile Spring

- `dev`: `application-dev.properties` (mặc định cho local)
- `staging`: `application-staging.properties`
- `prod`: `application-prod.properties`
- `test`: `application-test.properties`

Chạy với profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 7. Verify — Kiểm tra cấu hình

- Kiểm tra `.env` đã tạo từ `.env.example`.
- Kiểm tra `docker-compose.yml` đọc đúng các biến (`DB_HOST`, `DB_PORT`, `DB_NAME`, `JWT_SECRET`).
- Kiểm tra `backend/src/main/resources/application.properties` sử dụng `${VAR}` khớp với `.env.example`.
- Nếu một biến thiếu, Spring Boot sẽ báo `Could not resolve placeholder` khi khởi động.
