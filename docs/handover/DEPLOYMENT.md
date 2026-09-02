# Hướng dẫn Triển khai — Nguồn Gốc Số

> Mô tả quy trình deploy thực tế của repository **Nguồn Gốc Số**.
> Chỉ mô tả những thành phần tồn tại thật trong repository:
> GitHub Actions, GHCR (GitHub Container Registry), Kubernetes (k3s),
> Docker Compose, AWS EC2/RDS (cấu hình tham chiếu).

- Mã story: **NCL-10-CN-011-CV-05**
- Tài liệu tham chiếu: [OPERATIONS.md](./OPERATIONS.md), [ARCHITECTURE.md](./ARCHITECTURE.md),
  [SECURITY.md](./SECURITY.md), `docs/deployment-aws-ec2.md` (hướng dẫn chi tiết EC2/k3s).

---

## 1. Môi trường (Environment)

Repository định nghĩa **hai môi trường deploy** trong CI/CD & Kubernetes:

| Môi trường | Branch | Kubernetes namespace | Host/URL | NodePort frontend |
|---|---|---|---|---|
| **staging** | `develop` | `staging` | `https://staging.agri-trace.online` | `31691` |
| **production** | `main` | `production` | `https://agri-trace.online` | `31690` |

Các hằng số trên nằm trong:

- `.github/workflows/ci-cd.yml` (override CORS, FRONTEND_URL, DB_NAME, NodePort).
- `k8s/namespace.yaml`, `k8s/ingress-staging.yaml`, `k8s/ingress.yaml`.
- `k8s/configmap.yaml` (mặc định trỏ RDS: `database-1.…rds.amazonaws.com`, `nguongocso_db`).

> IP mặc định trong ci-cd (staging/prod CORS): `3.105.159.125` (AWS Sydney).
> `docs/deployment-aws-ec2.md` dùng host mẫu `nguongocso.example.com` — chỉ là ví dụ.

---

## 2. Yêu cầu trước khi deploy (Prerequisites)

Những yêu cầu thực tế cho từng chế độ deploy:

### 2.1 CI/CD (GitHub Actions)

| Yêu cầu | Giá trị |
|---|---|
| Kho lưu ảnh | **GitHub Container Registry (GHCR)** — `ghcr.io/<owner>/nguongocso-backend` và `…/nguongocso-frontend` |
| Secrets | `KUBECONFIG_B64` (kubeconfig base64), `KUBE_NAMESPACE` (staging/production), `GHCR_TOKEN` tự động qua `GITHUB_TOKEN` |
| GitHub Environments | `staging`, `production` (dùng trong job deploy) |
| Nhân CI | `ubuntu-latest`, JDK 21 (Temurin), Node 22 |

> Chi tiết tạo secrets: `docs/deployment-aws-ec2.md` mục 2 & 4.

### 2.2 Docker Compose (tự deploy 1 máy)

- Docker 24+ / Docker Compose v2 (file `docker-compose.yml` ở **root repo**).
- File `.env` ở root (tạo từ `.env.example`).
- MySQL có thể chạy trong container (service `mysql`) hoặc dùng RDS bên ngoài.

### 2.3 Kubernetes (k3s)

- Cluster k3s (cài theo `docs/deployment-aws-ec2.md` mục 2.3).
- Ingress-Nginx controller + cert-manager (`cluster-issuer.yaml` — Let's Encrypt).
- Manifest trong `k8s/`: namespace, secrets, configmap, persistent-volumes,
  deployments, services, ingresses.

---

## 3. Cấu hình (Configuration)

### 3.1 Biến môi trường backend

Toàn bộ do env quyết định (`application.properties`). Đầy đủ tại
`.env.example`; quan trọng nhất:

```
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
JWT_SECRET, JWT_EXPIRATION
ALLOWED_ORIGINS
FRONTEND_URL
MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD
LOCATIONIQ_API_KEY, LOCATIONIQ_BASE_URL
UPLOAD_BASE_DIR, QR_IMAGE_STORAGE_PATH
MYSQL_DUMP_PATH, MYSQL_PATH
APP_TIMEZONE
```

### 3.2 Nguồn cấu hình trong Kubernetes

| Manifest | Vai trò |
|---|---|
| `k8s/secrets.yaml` | Bí mật (DB credentials, JWT_SECRET, mail, LocationIQ) — **file dùng placeholder base64, phải thay trước khi deploy thật** |
| `k8s/configmap.yaml` | Cấu hình không bí mật (DB host/name, CORS, upload paths, FRONTEND_URL, MAIL_HOST…) |
| CI (`ci-cd.yml`) | Override theo env: CORS origins, FRONTEND_URL, DB_NAME, NodePort |

### 3.3 Frontend — cấu hình runtime

Frontend container **không build sẵn API URL**. Tại runtime:

- `API_BASE_URL` (mặc định `/api/v1`) được viết vào `/config.js` bởi
  `frontend/docker-entrypoint.d/40-runtime-config.sh`.
- Nginx reverse-proxy `/api/` và `/files/` đến `BACKEND_UPSTREAM`
  (mặc định `http://backend:8080`, k8s dùng `http://backend-service:8080`).

---

## 4. Build

### 4.1 Backend

```bash
cd backend
./mvnw clean package -Pprod
# JAR: target/backend-01.jar (version "01" trong pom.xml)
```

- Profile `prod` có trong `pom.xml` (skip tests, loại bỏ resource test khỏi JAR).
- **Lưu ý:** README cũ ghi `-Pproduction`; profile đúng trong `pom.xml` là **`prod`**.

### 4.2 Frontend

```bash
cd frontend
npm ci
npm run lint      # ESLint
npm run build     # tsc -b && vite build → dist/
```

### 4.3 Kiểm thử trước build (CI chạy mỗi lần push)

```bash
cd backend && ./mvnw clean test        # H2, profile test
cd frontend && npm run test            # Vitest
```

> Không có Playwright/Cypress. Không có script `test:e2e` trong `package.json`
> (README cũ ghi `npm run test:e2e` — **không tồn tại**; đây là gap đã ghi nhận).

---

## 5. Docker

### 5.1 Image

| Image | Dockerfile | Mô tả |
|---|---|---|
| `ghcr.io/<owner>/nguongocso-backend` | `backend/Dockerfile` | Multi-stage: builder (eclipse-temurin:21-jdk) → runtime (eclipse-temurin:21-jre + mysql-client). Chạy non-root user `spring`. |
| `ghcr.io/<owner>/nguongocso-frontend` | `frontend/Dockerfile` | Builder (node:22-alpine) → nginx:1.27-alpine + gettext; entrypoint render nginx template & config.js. |

### 5.2 Tag image (từ CI `ci-cd.yml`)

| Branch | Tag |
|---|---|
| `main` | `latest` |
| `develop` / khác | `edge` |
| mọi build | thêm tag `github.sha` (SHA commit) |

> Manifest k8s mặc định tham chiếu `:latest`. CI dùng `sed` thay tag
> theo `image_tag` output rồi `kubectl apply`.

### 5.3 Build thủ công

```bash
docker build -t nguongocso-backend ./backend
docker build -t nguongocso-frontend ./frontend
```

### 5.4 Docker Compose (đã mô tả trong OPERATIONS.md)

```bash
docker compose up -d --build
```

---

## 6. Database

### 6.1 Chạy database

- **Compose:** service `mysql` (mysql:8.4), volume `mysql_data`.
- **Kubernetes:** CI có hỗ trợ `k8s/mysql.yaml` nếu tồn tại; file hiện nằm
  trong `k8s/archived/mysql.yaml`. Production khuyến nghị **AWS RDS**.
- ConfigMap k8s hiện trỏ RDS: `database-1.ct2w4qggumqo.ap-southeast-2.rds.amazonaws.com`.

### 6.2 Migration

- Flyway chạy tự động khi backend khởi động (locations:
  `classpath:db/migration/schema` + `classpath:db/migration/data`).
- **Không deploy schema thủ công**; chỉ cần backend mới được rollout.

### 6.3 Seed data

Migrations `data/V14…V58` tự chèn roles, permissions, admin, tài khoản demo,
dữ liệu demo VT-02 (chi tiết: [DEMO_DATA.md](./DEMO_DATA.md)).

---

## 7. Triển khai bằng CI/CD (đường chính)

Pipeline thực tế trong `.github/workflows/ci-cd.yml`:

```text
[Push develop/main OR workflow_dispatch(staging|production)]
        │
        ▼
Job backend-test    → ./mvnw clean test (JDK 21, H2)
        │
        ▼
Job frontend-build  → npm ci + npm run lint + npm run build
        │
        ▼
Job build-push      → docker login GHCR
        │             tag = latest (main) | edge (develop)
        │             build & push backend + frontend
        │             (kèm tag github.sha)
        │
        ▼
Job deploy          → environment: staging|production
                      setup kubectl + KUBECONFIG_B64
                      sed thay image tag trong k8s/*.yaml
                      kubectl apply namespace/secrets/configmap/pv/services/deployments
                      patch NodePort frontend (31690 prod / 31691 staging)
                      patch configmap CORS + FRONTEND_URL + DB_NAME
                      apply ingress (ingress.yaml | ingress-staging.yaml)
                      kubectl rollout restart + rollout status (timeout 180s)
```

Chạy thủ công:

```bash
# Deploy staging
gh workflow run ci-cd.yml -f environment=staging

# Deploy production
gh workflow run ci-cd.yml -f environment=production
```

---

## 8. Triển khai thủ công (Kubernetes)

```bash
kubectl apply -f k8s/namespace.yaml
# Chuẩn bị secrets thật (KHÔNG dùng placeholder trong k8s/secrets.yaml)
kubectl apply -f k8s/secrets.yaml -n <ns>
kubectl apply -f k8s/configmap.yaml -n <ns>
kubectl apply -f k8s/persistent-volumes.yaml -n <ns>
kubectl apply -f k8s/backend-service.yaml -n <ns>
kubectl apply -f k8s/backend-deployment.yaml -n <ns>
kubectl apply -f k8s/frontend-service.yaml -n <ns>
kubectl apply -f k8s/frontend-deployment.yaml -n <ns>
kubectl apply -f k8s/ingress.yaml -n <ns>       # production
# hoặc kubectl apply -f k8s/ingress-staging.yaml -n <ns>  # staging
```

> Chi tiết từng bước (EC2, k3s, Let's Encrypt, secrets, RDS): **`docs/deployment-aws-ec2.md`**.

---

## 9. Verification sau deploy

```bash
# Frontend
curl -I https://agri-trace.online                    # 200 cho prod
curl -s https://agri-trace.online/config.js           # thấy API_BASE_URL: "/api/v1"

# Backend health
kubectl -n production exec deploy/backend -- curl -s localhost:8080/actuator/health
# kỳ vọng {"status":"UP"}

# Rollout
kubectl -n production rollout status deployment/backend --timeout=180s
kubectl -n production rollout status deployment/frontend --timeout=180s

# Pods
kubectl -n production get pods
```

---

## 10. Version & Deployment Time (TC-04)

### 10.1 Cơ chế nhận biết version hiện có

| Nguồn | Có triển khai? | Ý nghĩa |
|---|---|---|
| Docker image tag `github.sha` | ✅ (GHCR) | Mỗi build có SHA commit; có thể đối chiếu image ↔ commit |
| `:latest` / `:edge` | ✅ | Chỉ phân biệt production/develop, không xác định commit |
| Git tag | ❌ | Repository **không có tag** |
| Endpoint version (vd `/api/v1/version`) | ❌ | Không tồn tại trong code |
| Deployment history/timestamp trong app | ❌ | Không có bảng/UI deployment history |
| Actuator `/actuator/info` build-info | ❌ | Không cấu hình build-info/git commit |
| `pom.xml` version | ⚠️ | `01` — không mang ý nghĩa release |

### 10.2 Kết luận

```text
GAP — TC-04 deployment version/time tracking is not currently verifiable.
```

Hiện tại cách duy nhất để xác định phiên bản đang chạy là đối chiếu
**image đang chạy trong cluster** (kubectl get deploy -o yaml → image tag/sha)
với **tag trên GHCR**. Không có endpoint hay UI hiển thị
commit/version/deployment timestamp.

> Quyết định xử lý TC-04 (documentation-only hoặc thêm endpoint version tối
> thiểu) nằm ở phase tiếp theo — **không tự ý thêm code trong phase này**.