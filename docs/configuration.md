# Hướng dẫn Cấu hình — Nguồn Gốc Số

> Tổng hợp biến môi trường từ `.env.example`, `docker-compose.yml`, `application.properties`. Không ghi secret thật.

---

## Bảng biến môi trường

| Variable | Service | Required | Example | Purpose |
|----------|---------|----------|---------|---------|
| `DB_HOST` | backend / mysql | Có | `mysql` (Docker) / `localhost` | DB host |
| `DB_PORT` | backend / mysql | Có | `3306` | DB port |
| `DB_NAME` | backend / mysql | Có | `nguon_goc_so` | DB name |
| `DB_USERNAME` | backend / mysql | Có | `nguongocso` | DB user |
| `DB_PASSWORD` | backend / mysql | Có | (placeholder) | DB password |
| `MYSQL_ROOT_PASSWORD` | mysql (Docker) | Có | (placeholder) | Root password |
| `PORT` | backend | Khuyến nghị | `8080` | Backend port |
| `JWT_SECRET` | backend | Có | `your_secret_key` | JWT signing |
| `JWT_EXPIRATION` | backend | Khuyến nghị | `86400000` | Token lifetime (ms) |
| `ALLOWED_ORIGINS` | backend | Có | `http://localhost:3000` | CORS origins |
| `UPLOAD_BASE_DIR` | backend | Có | `/app/uploads` | Upload base directory |
| `QR_IMAGE_STORAGE_PATH` | backend | Có | `/app/files/qr` | QR storage path |
| `APP_TIMEZONE` | backend | Khuyến nghị | `Asia/Ho_Chi_Minh` | Business timezone |
| `FRONTEND_URL` | backend | Khuyến nghị | `http://localhost:3000` | Frontend URL |
| `VITE_API_URL` | frontend | Có | `http://localhost:8080/api/v1` | API base URL |

---

## Spring Boot (`application.properties`)

File gốc: `backend/src/main/resources/application.properties`.

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}...
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
app.jwt.secret=${JWT_SECRET}
app.cors.allowed-origins=${ALLOWED_ORIGINS}
```

> `ddl-auto=validate`: Flyway (`db/migration/`) là nguồn sự thật cho schema.
