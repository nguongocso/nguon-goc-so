# AGENTS.md

Two independent apps live in this repo: `backend/` (Spring Boot 3.5, Java 21, Maven) and `frontend/` (Vite 8, React 19, TypeScript, antd 6, Tailwind 4). There is no shared root tooling — run each app from its own directory.

## Backend

- Entry point: `backend/src/main/java/vn/nguongocso/BackendApplication.java` (`@SpringBootApplication` + `@EnableScheduling`).
- Build/run with the wrapper from `backend/`: `./mvnw.cmd` (Windows) or `./mvnw` (Unix). Requires Java 21.
- DB config comes from `backend/.env` (loaded via spring-dotenv) into `application.properties`: MySQL `nguon_goc_so`, user `root`, empty password, `localhost:3306`. Both `backend/.env` and `application.properties` are gitignored yet still tracked — never commit DB credentials.
- Schema gotcha: Flyway migrations exist under `src/main/resources/db/migration/` (`V*.sql`) but `spring.flyway.enabled=false`. Hibernate `ddl-auto=update` owns the schema, so create the database manually (`CREATE DATABASE nguon_goc_so`); tables auto-create and migrations are NOT executed.
- All routes are under `/api/v1`. JWT (`Authorization: Bearer`) is required for everything except `/api/v1/auth/login`, `/api/v1/public/**`, `/actuator/health`, `/files/qr/**` (see `config/SecurityConfig.java`). Frontend dev ports `5173`/`3000` are CORS-allowlisted there.
- Each feature has its own package (`farm`, `event`, `trace`, `organization`, `report`, `certification`, `alert`, `backup`, ...) containing controller -> service interface -> `service/impl`.
- Filesystem gotchas: uploads go to `./uploads`, QR images to `./files/qr` (served at `/files/qr/**`); both `uploads/` and `files/` are gitignored. Backup/restore hardcodes Windows MySQL paths (`C:/Program Files/MySQL/MySQL Server 8.0/bin/...`), so backup is Windows-only.
- Tests: Mockito unit tests need no DB and run fast with `./mvnw.cmd test -Dtest=ClassName`. `@SpringBootTest` tests (and a full `mvn test`) boot the whole context and require a live MySQL instance. `DatabaseConnectionTest` is a boot-time `CommandLineRunner`, not a test.

## Frontend

- Commands: `npm run dev` (Vite), `npm run build` (`tsc -b && vite build`), `npm run lint` (`eslint .`). There is no test script and no eslint config file in the repo.
- Always import with the `@/*` alias (`@` -> `src/`), configured in both `vite.config.ts` and `tsconfig.app.json`.
- `src/api/*` mirrors the backend controllers; `src/api/axiosConfig.ts` attaches the Bearer token and redirects to `/login` on 401. `VITE_API_BASE_URL` in `frontend/.env` defaults to `http://localhost:8080/api/v1`.
- `tsconfig.app.json` enables `noUnusedLocals`, `noUnusedParameters`, `verbatimModuleSyntax`, and `erasableSyntaxOnly` — do not use TypeScript `enum`s in the frontend (use union types / `as const`); the backend uses Java enums.
- `npm_member_check.txt` at the frontend root is a stale artifact listing already-fixed TS errors — ignore it.
- UI changes MUST follow `docs/AI_DESIGN_SYSTEM.md`.

## Docs & Git

- Per-feature API specs live in `docs/api/`; CSV fixtures for the production-lot bulk import are in `docs/sample-data/`.
- No CI exists. Workflow is `feature/*` branches merged into `develop`; commit messages use `fix:` / `feat:` prefixes.

## Ngôn ngữ giao tiếp

- Luôn giải thích và trả lời người dùng bằng tiếng Việt.
- Giữ nguyên tên file, tên biến, lệnh terminal và thuật ngữ kỹ thuật cần thiết.
- Khi sửa code, giải thích rõ từng file đã thay đổi bằng tiếng Việt.