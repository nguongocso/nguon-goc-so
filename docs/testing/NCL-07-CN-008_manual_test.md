# Kịch bản kiểm thử thủ công — NCL-07-CN-008

> Bảng điều khiển mức độ sử dụng nền tảng theo tổ chức, dành riêng cho vai trò VT-01.
> Môi trường + dữ liệu: xem `docs/test-preparation/NCL-07-CN-008-chuan-bi-kiem-thu-thu-cong.md`.
> Frontend: `http://localhost:3000` (dev). Backend: `http://localhost:8080`.
> Thao tác trên màn hình, không viết test tự động.

## Đăng nhập và vào trang

1. Mở `http://localhost:3000/login` → đăng nhập `admin` / `admin123`.
2. Chọn tổ chức `Hệ thống` (SYSTEM) nếu app yêu cầu.
3. Vào menu **"Mức độ sử dụng nền tảng"** (nhóm Báo cáo) hoặc mở thẳng `/reports/organization-usage`.
4. Bấm nút trợ giúp (?) nếu có để xem nội dung hướng dẫn.

---

## Nhóm 1 — Luồng chính (Cao)

| Mã | Tên | Given | When | Then | Dữ liệu | Ưu tiên |
|---|---|---|---|---|---|---|
| MTC-01 | Mặc định hiển thị đủ 4 tổ chức | VT-01 `admin`, mở `/reports/organization-usage` (dữ liệu tự tải, không cần nhấn nút) | Quan sát bảng | Có 4 dòng: HTXA, HTXB, HTXC, SYSTEM. Đúng tổng (Summary card "Tổng số tổ chức" = 4; ba nhóm trạng thái cộng lại bằng tổng số). Thứ tự mặc định: Cần liên hệ hỗ trợ → Đang hoạt động → Chưa có dữ liệu. | — | Cao |
| MTC-02 | Số liệu 6 metric cho tổ chức đang hoạt động | Ở bước MTC-01 | Chọn kỳ `2026-08-16 → 2026-09-14` (hoặc nhập `16/08/2026` + `14/09/2026`, kỳ tự áp dụng ngay) | Dòng HTXA hiển thị đúng bảng Phần 1.B: Lô 3 (+50%), Nhật ký 4 (+100%), Sự kiện 3 (+200%), Tem 2 (+100%), Tra cứu 3 (+200%), Người dùng 1 (0.0%). Ô so sánh chỉ hiện % kèm mũi tên. Không hiển thị tag nào. | HTXA | Cao |
| MTC-03 | Tổ chức ngừng hoạt động → "Cần liên hệ hỗ trợ" | Kỳ `2026-08-16 → 2026-09-14` đang áp dụng | Quan sát dòng HTXB | Tag **"Cần liên hệ hỗ trợ"**. Các cột metric = 0 (hoặc "—"). Cột "Hoạt động gần nhất" = `01/06/2026`. | HTXB | Cao |
| MTC-04 | Tổ chức mới → "Chưa có dữ liệu" | Kỳ trên đang áp dụng | Quan sát dòng HTXC (và SYSTEM) | Tag **"Chưa có dữ liệu"**. Các cột metric = 0; cột "Hoạt động gần nhất" trống. | HTXC, SYSTEM | Cao |
| MTC-05 | Metric có previous = 0 → quy ước "+100.0%" | Kỳ hiện tại đang là 2026-09-01 → 2026-09-14 | Nhập kỳ `01/09/2026 → 14/09/2026` (tự áp dụng ngay). Quan sát cột metric của HTXA | Các metric hiển thị số `current` kèm mũi tên lên và **+100.0%** (quy ước: tăng từ 0 = 100%). Dòng HTXB/HTXC hiển thị "0.0%". | HTXA | Cao |
| MTC-06 | Export CSV | Đang ở màn hình với kỳ `2026-08-16 → 2026-09-14` | Nhấn nút **Xuất Excel/CSV** (hoặc nút tương đương). Mở file tải về | File `Bao_cao_muc_do_su_dung_YYYYMMDD.csv`; có 4 dòng tổ chức + cột 6 metric; số liệu khớp MTC-02; file mở bằng Excel không lỗi font (UTF-8 BOM). | — | Cao |

---

## Nhóm 2 — Bộ lọc / tìm kiếm / sắp xếp (Trung bình)

| Mã | Tên | Given | When | Then | Dữ liệu | Ưu tiên |
|---|---|---|---|---|---|---|
| MTC-07 | Tìm kiếm theo tên | Màn hình mặc định | Gõ `chè` vào ô tìm kiếm | Chỉ còn HTXA. Xóa từ khóa → trả lại 4 dòng. Gõ mã `HTXB` → không còn dòng nào (chỉ tìm theo tên tổ chức). | HTXA, HTXB | Trung bình |
| MTC-08 | Lọc theo trạng thái | Màn hình mặc định | Mở dropdown trạng thái, chọn "Cần liên hệ hỗ trợ" | Hiển thị HTXB (+ HTXC/SYSTEM nếu chúng cũng nằm trong trạng thái này). Chọn "Đang hoạt động" → chỉ HTXA. | — | Trung bình |
| MTC-09 | Sắp xếp theo cột metric | Kỳ `2026-08-16 → 2026-09-14` | Nhấn vào header cột "Lô sản xuất" (2 lần) | Lần 1 giảm dần, lần 2 tăng dần; HTXA đứng đầu khi giảm dần. Các cột khác sắp xếp tương tự (thử "Người dùng HT", "Trạng thái"). Chỉ cột đang sort mới hiện mũi tên. | HTXA | Trung bình |
| MTC-10 | Tải lại kỳ đang chọn bằng nút Làm mới | Đã chọn kỳ tùy chỉnh | Nhấn **Làm mới** (cuối hàng bộ lọc) | Bảng gọi lại API với đúng kỳ đang chọn; 2 ô ngày giữ nguyên; không treo UI. | — | Thấp |

---

## Nhóm 3 — Phân quyền (Cao)

| Mã | Tên | Given | When | Then | Dữ liệu | Ưu tiên |
|---|---|---|---|---|---|---|
| MTC-11 | VT-02 bị chặn truy cập | Mở trình duyệt/Phiên ẩn danh, đăng nhập `managerA` / `admin123` (VT-02) | Truy cập trực tiếp `/reports/organization-usage` (hoặc tìm menu nếu hiển thị) | Màn hình báo lỗi truy cập (403) — không hiển thị bảng dữ liệu. Kiểm tra Network: API `organization-usage` trả `403` với body chứa `"errors":"ACCESS_DENIED"`. Menu "Mức độ sử dụng nền tảng" không xuất hiện cho VT-02. | managerA | Cao |
| MTC-12 | VT-01 vẫn truy cập được sau khi VT-02 thử | Đăng nhập lại `admin` (VT-01) | Mở trang + gọi export | Vẫn hiển thị đủ dữ liệu và export được (xác nhận chặn chỉ ảnh hưởng vai trò khác). | admin | Cao |

---

## Nhóm 4 — Ứng phó lỗi (Trung bình)

| Mã | Tên | Given | When | Then | Dữ liệu | Ưu tiên |
|---|---|---|---|---|---|---|
| MTC-13 | Nhập kỳ nghịch đảo | Đang ở màn hình | Nhập Từ ngày `14/09/2026`, Đến ngày `01/09/2026` (đủ ngày là kiểm tra ngay) | Hiển thị thông báo lỗi (frontend chặn), **không** gọi API (Network không có request organization-usage). | — | Trung bình |
| MTC-14 | Backend trả lỗi → màn hình báo lỗi | Không cần thay đổi dữ liệu | Tắt backend (hoặc đợi timeout), bấm **Làm mới** | Trang hiển thị trạng thái lỗi + nút thử lại; không vỡ UI. | — | Trung bình |
| MTC-15 | Hết hạn token | Mở trang, để lâu (> thời gian JWT là 1 ngày) hoặc cố tình đổi token | Thao tác lọc | Bị chuyển về login (hoặc báo 401); app xử lý rẽ nhánh đăng nhập lại. | — | Thấp |

---

## Nhóm 5 — Đối chiếu API (kiểm tra không bắt buộc, Trung bình)

| Mã | Tên | When | Then | Ưu tiên |
|---|---|---|---|---|
| MTC-16 | Gọi API đối chiếu số liệu | Lấy token `admin` rồi gọi `GET /api/v1/reports/organization-usage?startDate=2026-08-16&endDate=2026-09-14` | Response `success=true`; `data.items` có 4 phần tử; HTXA có `productionLots`.current=3, previous=2, change=1, changePercent=50.0; `farmLogs.current=4`; `chainEvents.current=3`; `activatedLabels.current=2`; `publicLookups.current=3`; `activeUsers.current=1`; `needsSupport=false`; `hasData=true`. HTXB `needsSupport=true`, `hasData=false`. HTXC/SYSTEM `hasData=false`. | Trung bình |
| MTC-17 | Export đối chiếu | Gọi `GET /api/v1/reports/organization-usage/export?startDate=2026-08-16&endDate=2026-09-14` | HTTP `200`, `Content-Type=text/csv;charset=UTF-8`, header `Content-Disposition` có filename `Bao_cao_muc_do_su_dung_...csv`; nội dung CSV có dòng HTXA với 6 cột đúng số liệu. | Trung bình |

---

## Tóm tắt

| Nhóm | Kịch bản | Phạm vi AC |
|---|---|---|
| Luồng chính | MTC-01 → MTC-06 | Hiển thị 6 metric, so sánh 2 kỳ, tag trạng thái, Mongo mới/Chưa có dữ liệu, export |
| Bộ lọc/tìm/sort | MTC-07 → MTC-10 | Lọc theo kỳ, trạng thái, tìm kiếm, sắp xếp |
| Phân quyền | MTC-11 → MTC-12 | Chỉ VT-01; VT-02 bị 403 |
| Ứng phó lỗi | MTC-13 → MTC-15 | Validate kỳ, lỗi backend/network, hết hạn token |
| Đối chiếu API | MTC-16 → MTC-17 | Khớp giữa UI và API/back tài liệu |

**Ghi chú môi trường nếu gặp trục trặc:**
- Backend chưa chạy → xem cách khởi động ở `docs/test-preparation/NCL-07-CN-008-chuan-bi-kiem-thu-thu-cong.md` Phần 2.B.
- Số liệu lệch do test sau ngày 2026-09-14 → luôn truyền kỳ cố định `16/08/2026 → 14/09/2026`.
- Dữ liệu test bị xóa → chạy lại seed (Phần 2.C cùng file).