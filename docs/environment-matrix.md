# Ma trận Môi trường — Nguồn Gốc Số

| Hạng mục | Local (Dev) | Staging | Production |
|----------|------------|---------|------------|
| Branch | `develop` / `feature/*` | `develop` | `main` |
| Domain / URL | `http://localhost:3000` (FE) / `http://localhost:8080` (BE) | `https://staging.agri-trace.online` | `https://agri-trace.online` |
| Namespace | — (Docker Compose) | `staging` | `production` |
| Frontend port | `3000` (`vite.config.ts`) | `31691` | `31690` |
| Backend port | `8080` | `8080` | `8080` |
| Database | MySQL 8.4 (container / local) | RDS (`nguongocso_db`) | RDS (`nguongocso_db`) |
| CORS origins | `http://localhost:3000` | `https://staging.agri-trace.online` | `https://agri-trace.online` |
| API base URL FE | `http://localhost:8080/api/v1` | `https://staging.agri-trace.online/api` | `https://agri-trace.online/api` |
| Image registry | Local build | GHCR | GHCR |

---

## Ghi chú

- `docker-compose.yml`: `mysql:8.4`, `backend`, `frontend`; `depends_on` + `condition: service_healthy`.
- `.env.example`: đồng bộ với code; `VITE_API_URL` phải chứa `/api/v1` để khớp `runtimeConfig.ts`.
- Production infrastructure: `https://agri-trace.online` đã xác minh (HTTP 200); chi tiết server (EC2, RDS endpoint, Kubernetes node) **chưa thể xác minh từ repository / public endpoint**.
