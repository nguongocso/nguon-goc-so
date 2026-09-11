# Báo cáo môi trường kiểm thử — NCL-08-CN-012

> Ngày/hora: phiên làm việc hiện tại
> Branch: feature/NCL-08-CN-012-close-recall-case
> Phạm vi: chỉ dựng môi trường, không sửa mã, không nạp dữ liệu.

---

## BACKEND

- Image: chưa build được (`docker build -t nguongocso-backend ./backend` chưa thực thi được).
- Container: chưa tạo (`nguongocso-backend` chưa tồn tại).
- Trạng thái: **CHƯA SẴN SÀNG**.
- URL dự kiến: `http://localhost:8080/api/v1`
- Health check: chưa thực hiện (container chưa chạy).
- Log gần nhất: N/A — container chưa tạo.
- Lệnh dừng (khi đã chạy): `docker stop nguongocso-backend` (hoặc `docker compose down backend` nếu dùng compose).

**Nguyên nhân:** Docker daemon không khả dụng trong phiên này.
- Lỗi khi gọi `docker ps`: `failed to connect to the docker API at npipe://./pipe/dockerDesktopLinuxEngine; check if the path is correct...`
- Cách bật: mở **Docker Desktop** (Windows) hoặc chạy `systemctl start docker` (Linux). Nếu không có quyền admin → cần người dùng bật rồi quay lại.

**Lệnh cần chạy (khi Docker đã bật):**
```powershell
# Build
cd D:\nguon-goc-so\nguon-goc-so
docker build -t nguongocso-backend ./backend

# Chạy (dùng .env sẵn có)
docker run -d --name nguongocso-backend `
  --env-file .env `
  -p 8080:8080 `
  nguongocso-backend

# Hoặc dùng compose (đã có mysql + backend cùng .env)
docker compose up -d mysql backend
```

**Lưu ý từ `.env`:** `DB_HOST=mysql` (compose), `PORT=8080`, `DB_PORT=3306` (container), `MYSQL_HOST_PORT=3307` (host). Nếu chạy standalone (`docker run`) cần chỉnh `.env` tạm thời thành `DB_HOST=host.docker.internal` hoặc dùng `--network host` nếu cần, nhưng tốt nhất là dùng `docker compose up -d backend` để mysql tự động sẵn sàng.

---

## FRONTEND

- URL dev: `http://localhost:5173` (Vite mặc định; xác nhận bằng terminal khi chạy).
- Cổng: `5173` (theo `vite` và `frontend/package.json` script `"dev": "vite"`).
- Trạng thái: **ĐANG CHẠY** (process `node` PID `5712` đã khởi động bằng `npm run dev`).
- Log: `frontend/dev.log` (đã tạo; nội dung hiện có dòng `> vite` — đang compile khởi động).
- Biến môi trường: `frontend/.env` đã có `VITE_API_BASE_URL=http://localhost:8080/api/v1` (trỏ đúng backend khi backend lên `8080`).
- Lệnh dừng: `Get-Process -Id 5712 | Stop-Process` (hoặc tìm PID chính xác từ `Get-Process node` rồi `Stop-Process`).

**Cách kiểm tra đã lên:**
```powershell
# Sau vài giây compile, kiểm tra
curl -I http://localhost:5173
# Hoặc mở trình duyệt: http://localhost:5173
```

**Ghi chú:** Nếu cổng `5173` đã bị chiếm (ít khả năng nhưng cần kiểm tra), Vite sẽ tự chọn cổng tiếp theo (`5174`, `5175`) và in ra terminal. Khi đó URL sẽ đổi theo.

---

## KẾT NỐI

- Frontend → Backend: **Chưa xác nhận được** vì backend container chưa chạy.
- CORS: `.env` có `ALLOWED_ORIGINS=http://localhost:3000`; frontend dev ở `5173`. Nếu backend chạy, cần đảm bảo `ALLOWED_ORIGINS` chứa `http://localhost:5173` (hoặc sửa `.env` tạm thời trước khi build image, nhưng không sửa mã nghiệp vụ). **Ghi chú:** Nếu có lỗi CORS khi test, nguyên nhân dự kiến là `ALLOWED_ORIGINS` thiếu `5173`; sửa qua `.env` + rebuild hoặc dùng proxy Vite (`vite.config.ts` proxy) nếu cần — nhưng đây là cấu hình môi trường, không phải sửa nghiệp vụ.
- Kiểm tra kết nối sau khi backend lên: mở `http://localhost:5173/login`; nhập `orgmanager` / `admin123`; xem có vào `/recall-cases` không.

---

## VIỆC CẦN LÀM TIẾP (cho người dùng)

1. **Bật Docker Desktop** (Windows) hoặc `systemctl start docker` (Linux), rồi chạy lại các lệnh backend trên.
2. **Xác nhận backend sẵn sàng:** `curl http://localhost:8080/api/v1/recall-cases` (hoặc qua frontend đăng nhập). Nếu `mysql` chưa chạy trong compose, dùng `docker compose up -d mysql backend` thay vì chỉ `backend`.
3. **Xác nhận frontend đã compile xong:** xem `frontend/dev.log`; bấm vào `http://localhost:5173`; đăng nhập `orgmanager`; mở `/recall-cases`.
4. **Nạp dữ liệu:** tự làm qua giao diện theo `docs/test-preparation/NCL-08-CN-012-chuan-bi-kiem-thu-thu-cong.md` (Phần 1 + Phần 3 F checklist).
5. **Nếu cần dừng/restart:** backend `docker stop nguongocso-backend`; frontend `Stop-Process -Id <node_pid>` (hiện `5712`).

---

## CẢNH BÁO

- **Docker daemon không chạy:** không thể build/chạy container. Đã báo đúng nguyên nhân, không tự đoán.
- **Cổng 8080 đã nghe:** `netstat` cho thấy port `8080` đang `LISTENING` (có thể từ một instance backend cũ hoặc service khác). Nếu `docker run -p 8080:8080` lỗi vì cổng đông, đề xuất đổi thành `-p 8081:8080` và sửa `frontend/.env` thành `VITE_API_BASE_URL=http://localhost:8081/api/v1`.
- **Frontend đang compile:** log chỉ có `> vite`; chưa có dòng `Local:` hoặc `Ready`. Có thể mất thêm 10–30 giây tùy tốc độ. Đừng tắt process sớm.
- **Không có thay đổi mã nguồn:** không commit, không sửa `.java`, không sửa API contract.
- **Dữ liệu chưa nạp:** không tự nạp vào DB; để người dùng tự làm qua `npm run dev` giao diện.
