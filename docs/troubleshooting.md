# Xử lý sự cố — Nguồn Gốc Số

> Mục tiêu: Một thành viên mới có thể tự kiểm tra và xử lý các lỗi thường gặp mà không cần hỏi người khác.
> Mọi nguyên nhân và cách xử lý được suy từ repository (`README.md`, `OPERATIONS.md`, `.env.example`, `application.properties`, `Dockerfile`).

---

## 1. Database connection fail

### Triệu chứng
Backend khởi động nhưng báo `Connection refused` hoặc `Unknown database`.

### Nguyên nhân
- `DB_HOST` trong `.env` sai (`localhost` khi dùng Docker Compose cần `mysql`).
- `DB_NAME` chưa tạo hoặc gõ sai.
- `DB_USERNAME` / `DB_PASSWORD` sai.

### Kiểm tra
```bash
# Nếu dùng Docker
docker-compose ps
# Kiểm tra DB
mysql -h localhost -P 3306 -u nguongocso -p -e "SHOW DATABASES;"
```

### Cách xử lý
- Nếu chạy Docker: đảm bảo `.env` dùng `DB_HOST=mysql`.
- Nếu chạy local (không Docker): dùng `DB_HOST=localhost`.
- Tạo DB: `CREATE DATABASE nguon_goc_so CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`.

---

## 2. Flyway migrate fail

### Triệu chứng
Backend báo lỗi liên quan `Flyway` (`validate` hoặc `migration` thất bại).

### Nguyên nhân
- Schema DB không khớp với `db/migration/`.
- Đã có dữ liệu nhưng `spring.flyway.repair-on-migrate=true` không đủ.

### Kiểm tra
```bash
# Xem log chi tiết
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Cách xử lý
- Đảm bảo `spring.jpa.hibernate.ddl-auto=validate` (không tự tạo bảng).
- Nếu DB bị lỗi: `DROP DATABASE nguon_goc_so; CREATE DATABASE ...;` rồi chạy lại.
- Kiểm tra `db/migration/` có đủ `schema` + `data`.

---

## 3. JWT / Authentication lỗi (401, token không hợp lệ)

### Triệu chứng
Frontend gọi API trả `401 Unauthorized`.

### Nguyên nhân
- `JWT_SECRET` trong `.env` trống hoặc khác với backend.
- Token hết hạn (`JWT_EXPIRATION` quá ngắn).
- `ALLOWED_ORIGINS` không bao gồm domain frontend.

### Kiểm tra
- Kiểm tra `.env`: `JWT_SECRET` phải có giá trị (không để trống).
- Kiểm tra `ALLOWED_ORIGINS` bao gồm URL frontend (`http://localhost:3000` — dev server port theo `vite.config.ts`; thêm `5173` nếu bạn cấu hình khác)

### Cách xử lý
- Điền `JWT_SECRET` (ít nhất 32 ký tự).
- Kiểm tra `ALLOWED_ORIGINS` trong `.env` khớp với frontend.
- Nếu token hết hạn: đăng nhập lại.

---

## 4. CORS error

### Triệu chứng
Frontend báo lỗi CORS trong console browser (`Access-Control-Allow-Origin`).

### Nguyên nhân
`ALLOWED_ORIGINS` trong `.env` không chứa domain frontend thực tế.

### Cách xử lý
Cập nhật `.env`:
```env
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```
Nếu dùng production:
```env
ALLOWED_ORIGINS=https://agri-trace.online
```

---

## 5. Upload bị từ chối hoặc file quá lớn

### Triệu chứng
API upload trả lỗi kích thước hoặc `MultipartException`.

### Nguyên nhân
- `spring.servlet.multipart.max-file-size` (10MB mặc định) thấp hơn file tải lên.
- `UPLOAD_*_MAX_SIZE` thấp hơn kích thước tệp.

### Kiểm tra
```bash
# Kiểm tra cấu hình trong application.properties
grep -n "multipart.max-file-size" backend/src/main/resources/application.properties
```

### Cách xử lý
- Nếu cần tải file lớn hơn 10MB: tăng `spring.servlet.multipart.max-file-size` và `max-request-size` trong `application.properties` (hoặc profile `dev`).
- Kiểm tra `UPLOAD_*_MAX_SIZE` trong `.env` khớp nhu cầu.

---

## 6. QR code không được sinh / không hiển thị

### Triệu chứng
Lô hàng được tạo nhưng không có ảnh QR; đường dẫn file rỗng.

### Nguyên nhân
- `QR_IMAGE_STORAGE_PATH` không tồn tại hoặc không có quyền ghi.
- Service chưa tạo thư mục `/app/files/qr`.

### Kiểm tra
```bash
# Nếu dùng Docker
ls -la /app/files/qr
# Hoặc kiểm tra volume
docker inspect nguon-goc-so-fixed-backend-1
```

### Cách xử lý
- Đảm bảo `QR_IMAGE_STORAGE_PATH` đúng (`/app/files/qr` cho Docker, `./uploads/qr` cho local nếu tùy chỉnh).
- Tạo thư mục và cấp quyền: `mkdir -p /app/files/qr && chmod 777 /app/files/qr` (chỉ cho dev).

---

## 7. Backup / Restore lỗi

### Triệu chứng
`mysqldump` hoặc `mysql` CLI không tìm thấy.

### Nguyên nhân
- `MYSQL_DUMP_PATH` hoặc `MYSQL_PATH` sai.
- `mysql-client` không được cài trong container backend (`Dockerfile` đã cài `mysql-client`).

### Kiểm tra
```bash
# Trong container backend
which mysqldump
which mysql
```

### Cách xử lý
- Đảm bảo `.env` có `MYSQL_DUMP_PATH=/usr/bin/mysqldump` và `MYSQL_PATH=/usr/bin/mysql`.
- Nếu chạy local (không Docker): cài `mysql-client` hoặc điều chỉnh đường dẫn.

---

## 8. Docker port conflict

### Triệu chứng
`docker-compose up` báo lỗi `Bind for 0.0.0.0:8080 failed`.

### Nguyên nhân
Port `8080` hoặc `3000` đã bị chiếm trên host.

### Cách xử lý
- Đổi `BACKEND_HOST_PORT` hoặc `FRONTEND_HOST_PORT` trong `.env` (ví dụ `8081`, `3001`).
- Hoặc dừng service đang dùng port đó (`netstat -ano | findstr 8080`).

---

## 9. Node build / Frontend lỗi

### Triệu chứng
`npm run build` thất bại; lỗi TypeScript hoặc import.

### Kiểm tra
```bash
cd frontend
npm install
npm run build
```

### Cách xử lý
- Đảm bảo `node` đúng phiên bản (`node -v` phải là 22.x).
- Xóa `node_modules` và `package-lock.json` rồi cài lại: `rm -rf node_modules package-lock.json && npm install`.
- Kiểm tra `vite.config.ts` và `tsconfig.json` không bị sửa lỗi.

---

## 10. Verify sau khi xử lý

- Mỗi lỗi trên phải kiểm tra lại bằng lệnh tương ứng (ví dụ `curl`, `docker-compose ps`, `mvn test`).
- Nếu vẫn không giải quyết được sau 5 lần thử (theo quy tắc 5-attempt), dừng lại và báo cáo cho team với đầy đủ log, command đã chạy, và trạng thái còn lại.
