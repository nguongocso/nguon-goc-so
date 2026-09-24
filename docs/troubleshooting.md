# Xử lý sự cố — Nguồn Gốc Số

> Mục tiêu: thành viên mới tự xử lý lỗi thường gặp.

## Database connection fail
- Kiểm tra `DB_HOST` (Docker: `mysql`; local: `localhost`).
- Đảm bảo `mysqladmin ping` thành công trước khi `mvn spring-boot:run` nếu không dùng Compose.
- Tạo DB: `CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`

## Flyway migrate fail
- `spring.jpa.hibernate.ddl-auto=validate`; Flyway là nguồn schema.
- Nếu lỗi: reset DB rồi khởi động lại backend.

## JWT / 401
- `JWT_SECRET` trống → điền; `JWT_EXPIRATION` mặc định `86400000`.
- `ALLOWED_ORIGINS` phải chứa `http://localhost:3000`.

## CORS error
- Cập nhật `.env`: `ALLOWED_ORIGINS=http://localhost:3000` (thêm production domain nếu cần).

## Upload từ chối / quá lớn
- `spring.servlet.multipart.max-file-size=10MB` mặc định; tăng nếu cần.
- Kiểm tra `UPLOAD_*_MAX_SIZE` khớp nhu cầu.

## QR không sinh / không hiển thị
- `QR_IMAGE_STORAGE_PATH=/app/files/qr`; kiểm tra thư mục tồn tại và quyền ghi.
- Docker volume `backend_files` phải mount đúng.

## Backup / Restore lỗi
- Kiểm tra `MYSQL_DUMP_PATH=/usr/bin/mysqldump` và `MYSQL_PATH=/usr/bin/mysql`.
- `mysql-client` đã cài trong `backend/Dockerfile`.

## Docker port conflict
- Đổi `BACKEND_HOST_PORT` / `FRONTEND_HOST_PORT` trong `.env`; hoặc kiểm tra `netstat`.

## Node build / Frontend lỗi
- `node -v` phải 22.x; xóa `node_modules` + `package-lock.json` rồi `npm install` lại.
