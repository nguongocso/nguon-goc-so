# Ma trận Môi trường — Nguồn Gốc Số

> Đối chiếu local / staging / production dựa trên `DEPLOYMENT.md`, `.github/workflows/`, `k8s/`, `docker-compose.yml` và production URL `https://agri-trace.online`.

---

## 1. What

Bảng so sánh nhanh để hiểu hệ thống chạy ở đâu, dùng DB nào, domain nào, port nào.

## 2. Why

Tránh nhầm lẫn khi deploy từ local sang staging/prod; hiểu rõ CORS, API URL và DB endpoint thay đổi như thế nào.

---

| Hạng mục | Local (Dev) | Staging | Production |
|----------|------------|---------|------------|
| **Branch** | `develop` hoặc `feature/*` | `develop` | `main` |
| **Domain / URL** | `http://localhost:3000` (FE), `http://localhost:8080` (BE) | `https://staging.agri-trace.online` | `https://agri-trace.online` |
| **Kubernetes namespace** | — (Docker Compose) | `staging` | `production` |
| **Frontend dev server / NodePort** | `3000` (`vite.config.ts`) | `31691` | `31690` |
| **Backend port** | `8080` | `8080` (service) | `8080` (service) |
| **Database** | MySQL 8.4 (container hoặc local) | RDS (`database-1.…rds.amazonaws.com`, DB `nguongocso_db`) | RDS (`database-1.…rds.amazonaws.com`, DB `nguongocso_db`) |
| **CORS allowed origins** | `http://localhost:3000`, `http://localhost:5173` | `https://staging.agri-trace.online` | `https://agri-trace.online` |
| **Frontend URL config (`FRONTEND_URL`)** | `http://localhost:3000` | `https://staging.agri-trace.online` | `https://agri-trace.online` |
| **API base URL cho FE** | `http://localhost:8080/api/v1` | `https://staging.agri-trace.online/api` (qua Nginx proxy) | `https://agri-trace.online/api` (qua Nginx proxy) |
| **Image registry** | Local build | GHCR (`ghcr.io/<owner>/nguongocso-backend`) | GHCR (`ghcr.io/<owner>/nguongocso-frontend`) |
| **CI/CD trigger** | — | Push `develop` → CI → deploy `staging` | Pull Request `main` → CI → deploy `production` |
| **Secrets** | `.env` cục bộ | Kubernetes Secret + GitHub Env (`staging`) | Kubernetes Secret + GitHub Env (`production`) |
| **SSL / HTTPS** | Không (localhost) | Let's Encrypt (`cert-manager`, `cluster-issuer.yaml`) | Let's Encrypt (`cert-manager`) |
| **Backup / Restore** | `mysqldump` cục bộ (`/app/backups`) | Tự động theo cron + tải xuống từ RDS | Tự động theo cron + tải xuống từ RDS |

---

## 3. Verify

- Kiểm tra `docker-compose.yml`: services `mysql` (`mysql:8.4`), `backend`, `frontend`; port mapping khớp bảng trên.
- Kiểm tra `.github/workflows/ci-cd.yml`: triển khai `staging` từ `develop`, `production` từ `main`; override CORS, `FRONTEND_URL`, DB name.
- Kiểm tra `k8s/ingress.yaml` / `k8s/ingress-staging.yaml`: host `agri-trace.online` / `staging.agri-trace.online`; NodePort `31690` / `31691`.
- Kiểm tra `https://agri-trace.online`: phản hồi HTTP 200; tiêu đề "Nguồn gốc số".

---

## 4. Known / Unknown

| Hạng mục | Trạng thái | Ghi chú |
|----------|-----------|---------|
| Production DB endpoint thực tế | **Unknown** | `DEPLOYMENT.md` đề cập RDS nhưng không có endpoint thật trong public docs. |
| Kubernetes node/IP thực tế | **Unknown** | Không thể xác minh từ repository / public endpoint. |
| Secret thực tế (`JWT_SECRET`, DB password) | **Unknown / Need Team Confirmation** | Không được đưa vào tài liệu; cần team cung cấp qua Secret Manager / Vault. |
