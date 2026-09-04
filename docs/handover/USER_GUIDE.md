# Hướng dẫn Sử dụng — Nguồn Gốc Số

> Hướng dẫn dành cho **Quản lý hợp tác xã / người dùng thực tế** của nền tảng
> Nguồn Gốc Số. Tên chức năng trong tài liệu lấy **chính xác từ giao diện**
> (menu sidebar `frontend/src/components/layout/Sidebar.tsx`) và route thực tế
> (`frontend/src/routes/AppRoutes.tsx`).

- Mã story: **NCL-10-CN-011-CV-05**
- Đối tượng: Quản lý HTX (VT-02), Người ghi sự kiện (VT-03), Doanh nghiệp thu mua (VT-04), người tiêu dùng tra cứu.

---

## 1. Đăng nhập

### Mục đích
Truy cập vào hệ thống quản lý với vai trò đã được cấp.

### Các bước
1. Mở trình duyệt, truy cập địa chỉ hệ thống (ví dụ `http://localhost:3000` ở môi trường dev).
2. Tại màn hình đăng nhập, nhập **tên người dùng** và **mật khẩu**.
3. Bấm nút **Đăng nhập**.
4. Nếu tài khoản thuộc nhiều tổ chức, chọn **tổ chức** muốn làm việc
   (màn hình "Chọn tổ chức" xuất hiện sau bước đăng nhập).

### Kết quả mong đợi
- Chuyển đến màn hình **Dashboard** theo vai trò.
- Nếu sai mật khẩu, hệ thống báo lỗi và tính năng **khóa tài khoản** sẽ kích
  hoạt sau nhiều lần sai liên tiếp.
- Nếu quên mật khẩu: nhấn **Quên mật khẩu**, nhập email để nhận liên kết
  đặt lại mật khẩu.

> Tài khoản demo (môi trường dev/demo): `admin/admin123`, `orgmanager/admin123`…
> xem [DEMO_DATA.md](./DEMO_DATA.md).

---

## 2. Tổ chức

> Menu **Quản lý → Tổ chức** (VT-01) và **Hệ thống → Hồ sơ tổ chức** (VT-01, VT-02).

### 2.1 Tạo tổ chức (VT-01)

**Mục đích:** đăng ký một hợp tác xã/doanh nghiệp mới lên hệ thống.

**Các bước**
1. Menu **Quản lý → Tổ chức** → nút **Tạo tổ chức**.
2. Điền tên tổ chức, loại (HTX, doanh nghiệp, cơ quan quản lý), địa chỉ,
   số điện thoại, email.
3. Lưu.

**Kết quả mong đợi:** tổ chức xuất hiện trong danh sách với trạng thái **ACTIVE**.

### 2.2 Xem / cập nhật hồ sơ tổ chức (VT-01, VT-02)

**Các bước**
1. Menu **Hệ thống → Hồ sơ tổ chức**.
2. Sửa thông tin liên hệ / địa chỉ → Lưu.

**Kết quả mong đợi:** thông tin tổ chức được cập nhật, lịch sử hoạt động được ghi lại.

---

## 3. Quản lý thành viên

> Menu **Quản lý → Quản lý thành viên** (VT-02).

### Mục đích
Thêm, khóa, phân vai trò cho thành viên của hợp tác xã.

### Các bước
1. Vào **Quản lý → Quản lý thành viên**.
2. **Thêm thành viên:**
   - Bấm **Thêm thành viên**; nhập thông tin (email/tên) và chọn vai trò
     (VT-03 Người ghi sự kiện, hoặc quyền quản trị HTX).
   - Hệ thống gửi **lời mời qua email** (invitation) hoặc tạo tài khoản theo quy trình của HTX.
3. **Phân quyền chi tiết (nếu có):** menu **Cấu hình phân quyền** cho phép
   bật/tắt từng quyền của vai trò trong tổ chức.
4. Vô hiệu hóa thành viên khi nghỉ việc/không còn cộng tác.

### Kết quả mong đợi
- Thành viên mới có tài khoản và vai trò đúng.
- Thành viên bị khóa không thể đăng nhập vào tổ chức nữa.

---

## 4. Vùng trồng (Farm / Growing Area)

> Menu **Vận hành sản xuất → Vùng trồng** (VT-02).

### Mục đích
Khai báo các khu vực canh tác của HTX để gắn với lô sản xuất.

### Các bước
1. Vào **Vùng trồng** → **Tạo vùng trồng**.
2. Nhập tên vùng, loại cây trồng, diện tích, đơn vị (ha), vị trí (điểm tọa độ / bản đồ).
3. Lưu.

### Kết quả mong đợi
Vùng trồng xuất hiện trong danh sách với trạng thái hoạt động; có thể sửa, ngừng hoạt động, hoặc xóa.

> Route thực tế: `/farm-areas`, `/farm-areas/create`, `/farm-areas/:id/edit`.

---

## 5. Lô sản xuất (Production Lot)

> Menu **Vận hành sản xuất → Lô sản xuất** (VT-02, VT-03).

### Mục đích
Tạo và quản lý lô sản xuất từ gieo trồng đến thu hoạch để xây dựng chuỗi truy xuất.

### 5.1 Tạo lô sản xuất
1. Vào **Lô sản xuất** → **Tạo lô sản xuất**.
2. Chọn **vùng trồng**, **loại nông sản**, nhập số lượng dự kiến, ngày trồng, ngày thu hoạch.
3. Lưu ở trạng thái **Nháp**.

### 5.2 Trình duyệt & phê duyệt
1. Sau khi bổ sung đầy đủ thông tin, **Gửi duyệt**.
2. Quản lý HTX (VT-02) vào danh sách lô → bấm **Duyệt** lô.
3. Lô chuyển trạng thái đã duyệt, có thể bắt đầu ghi sự kiện.

### 5.3 Nhập lô hàng loạt
- Menu **Lô sản xuất** → nhập nhiều lô từ file Excel (template & hướng dẫn tại
  `docs/sample-data/ProductionLotImportGuide.md`).

### Kết quả mong đợi
Lô có trạng thái rõ ràng (**Nháp → Chờ duyệt → Đã duyệt**), gắn vùng trồng, loại nông sản, chứng nhận.

> Route thực tế: `/production-lots`, `/production-lots/create`, `/production-lots/:id`,
> `/production-lots/import`.

---

## 6. Nhật ký canh tác (Farm Log)

> Menu **Lô sản xuất → chi tiết lô → Nhật ký** / route `/farm-logs/*` (VT-02, VT-03).

### Mục đích
Ghi nhận hoạt động canh tác (bón phân, tưới, phun thuốc…) kèm hình ảnh/chứng từ.

### Các bước
1. Mở chi tiết **Lô sản xuất** → phần nhật ký → **Tạo nhật ký**.
2. Chọn loại hoạt động, nhập nội dung, đính kèm hình ảnh.
3. Lưu.

### Kết quả mong đợi
Nhật ký hiển thị trong lịch sử của lô; người ghi sự kiện có thể **đính chính**
nếu ghi sai; quản lý có thể **xác minh** nhật ký.

---

## 7. Sự kiện chuỗi cung ứng (Production Event)

> Menu **Vận hành sản xuất** (VT-02, VT-03) — ghi từng giai đoạn.

| Sự kiện | Cách thực hiện | Ghi chú |
|---|---|---|
| **Thu hoạch** | Chi tiết lô → ghi sự kiện thu hoạch | Chuyển lô sang trạng thái `HARVESTED` |
| **Sơ chế** | Route `/preprocessing-events/create` | Ghi công đoạn sơ chế |
| **Đóng gói** | Route `/packaging-events/create` | Chuyển lô sang `PACKAGED`; điều kiện để tạo lô hàng/tem |
| **Vận chuyển** | Menu **Ghi sự kiện vận chuyển** | Quét mã lô hàng, ghi điểm đi/đến |
| **Quét mã ghi sự kiện nhanh** | Menu **Quét mã ghi sự kiện nhanh** | Quét QR hàng loạt để ghi nhanh |
| **Điều kiện bảo quản** | Menu **Điều kiện bảo quản** | Ghi nhiệt độ/điều kiện kho |
| **Nhập kho & đối chiếu** | Menu **Thu mua → Nhập kho & đối chiếu** (VT-04) | Nghiệp vụ thu mua/đối chiếu |
| **Sự kiện chờ đồng bộ** | Menu **Sự kiện chờ đồng bộ** | Sự kiện ghi offline sẽ đồng bộ khi có mạng |

### Kết quả mong đợi
Sự kiện xuất hiện đúng thứ tự thời gian trong **dòng sự kiện** của lô/lô hàng;
nếu ghi sai, dùng tính năng **đính chính** (menu tương ứng).

---

## 8. Kiểm nghiệm & Chứng nhận (Inspection)

> Menu **Quản lý → Chứng nhận** (VT-02) + route `/inspection-requests/*`,
> `/inspection-requests/:requestId/results`.

### Mục đích
Gắn chứng nhận chất lượng và quản lý yêu cầu kiểm nghiệm cho lô sản xuất.

### Các bước
1. **Tạo chứng nhận:** menu **Chứng nhận** → **Tạo chứng nhận** → điền tiêu chuẩn,
   ngày cấp/hết hạn → gắn vào lô.
2. **Tạo yêu cầu kiểm nghiệm:** từ lô sản xuất → route
   `production-lots/:lotId/inspection-requests/create` → chọn đơn vị kiểm nghiệm và chỉ tiêu.
3. **Ghi kết quả kiểm nghiệm:** route `inspection-requests/:requestId/results` →
   nhập kết quả từng chỉ tiêu.
4. Kết quả đạt → lô đủ điều kiện kích hoạt tem (nếu loại nông sản yêu cầu kiểm nghiệm).

### Kết quả mong đợi
- Lô có đầy đủ chứng nhận & kết quả kiểm nghiệm.
- Kết quả công khai hiển thị trên trang tra cứu của người tiêu dùng
  (xem mục 10).

> Liên quan: **Quản lý → Chỉ tiêu kiểm nghiệm**, **Đơn vị kiểm nghiệm** (VT-01).

---

## 9. Truy xuất & Tem mã (Traceability)

> Menu **Vận hành sản xuất → Lô sản xuất → tạo lô hàng** và
> **Quản lý → Quản lý dải mã** (VT-01, VT-02).

### Mục đích
Tạo **lô hàng** từ lô sản xuất đã đóng gói, cấp **mã truy xuất** trong dải mã đã
đăng ký và sinh **tem QR** cho sản phẩm.

### 9.1 Quản lý dải mã (VT-01)
1. Menu **Quản lý → Quản lý dải mã** → tạo dải mã với số lượng mã.
2. Dải mã dùng làm nguồn cấp mã truy xuất (GS1 mô phỏng).

### 9.2 Tạo lô hàng & sinh tem
1. Từ lô sản xuất đã **PACKAGED** → route `/production-lots/:productionLotId/shipments/create`.
2. Hệ thống sinh lô hàng và mã truy xuất từ dải mã; sinh file QR.
3. **Kích hoạt tem** cho phép tra cứu công khai.

### 9.3 Các chức năng liên quan
- **Xuất tem QR để in**: menu/route `label-export`.
- **Hủy tem / lịch sử hủy tem**: route `/shipments/:id/cancel-labels`.
- **Thu hồi lô hàng**: menu **Tạo yêu cầu thu hồi** (VT-03) /
  **Danh sách yêu cầu thu hồi** (VT-02).
- **Tem nghi vấn** (VT-01): mã bị nghi ngờ quét bất thường.
- **Cảnh báo tem bất thường** (VT-01, VT-02).

### Kết quả mong đợi
Mỗi sản phẩm có tem QR duy nhất; khi quét, người tiêu dùng xem được hành trình đầy đủ.

---

## 10. Người tiêu dùng — Tra cứu khi quét mã (Consumer Lookup)

> Trang công khai — không cần đăng nhập. Route `/public/trace/:codeValue`.

### Mục đích
Cho phép người mua quét mã QR trên sản phẩm để xem nguồn gốc, hành trình, chứng
nhận và kết quả kiểm nghiệm.

### Các bước
1. Mở trang chủ công khai (ví dụ `http://localhost:3000/`).
2. **Quét mã QR** bằng camera, hoặc **nhập mã tra cứu** trên hộp tìm kiếm.
3. Xem kết quả:
   - Thông tin sản phẩm, mã lô hàng.
   - **Timeline** dòng sự kiện (bản đồ hoặc danh sách).
   - **Chứng nhận** và **kết quả kiểm nghiệm** công khai.
   - Nếu lô bị thu hồi → cảnh báo rõ ràng.

### Kết quả mong đợi
- Mã hợp lệ và tem đã kích hoạt → hiển thị hành trình.
- Mã không tồn tại → thông báo **"Mã lô hàng không tồn tại."** (không lộ lỗi kỹ thuật).
- Tem chưa kích hoạt → **"Tem chưa có hiệu lực..."**.
- Lô đã thu hồi → vẫn hiển thị nhưng kèm cảnh báo thu hồi.

---

## 11. Thống kê & Báo cáo

> Menu **Thống kê & Báo cáo** (VT-01, VT-02, VT-05).

| Chức năng | Vai trò | Mô tả |
|---|---|---|
| **Thống kê tra cứu** | VT-01, VT-02 | Số lượt quét theo lô/thời gian/vị trí |
| **Phân tích vùng trồng** | VT-01, VT-05 | Phân tích theo vùng trồng, nhóm cây trồng |
| **So sánh mùa vụ** | VT-01, VT-05 | So sánh năng suất giữa các mùa vụ |
| **Báo cáo ngành** | VT-05 | Báo cáo tổng hợp ngành (theo địa bàn phân công) |
| **Xuất dữ liệu mở** | VT-05 | Xuất dữ liệu công khai theo lược đồ chuẩn |

---

## 12. Hệ thống — Dành cho Quản trị viên (VT-01)

> Menu **Hệ thống**.

| Chức năng | Mô tả |
|---|---|
| **Kiểm chứng dòng sự kiện** | Kiểm tra tính hợp lệ/toàn vẹn của chuỗi sự kiện |
| **Lịch sử hoạt động** | Nhật ký hoạt động của tổ chức |
| **Lịch sử đăng nhập** | Lịch sử đăng nhập của người dùng |
| **Theo dõi đăng nhập bất thường** | Danh sách IP/phiên bất thường, xử lý nghi vấn |
| **Sao lưu & Phục hồi dữ liệu** | Lịch sao lưu, sao lưu thủ công, tải/xóa/khôi phục bản sao lưu |
| **Giám sát hệ thống** | Theo dõi hiệu năng/hệ thống |
| **Hồ sơ người dùng** | Cập nhật thông tin cá nhân, email, đổi mật khẩu |
| **Hồ sơ tổ chức** | Thông tin tổ chức hiện tại |

---

## 13. Lưu ý chung

- Nếu mất mạng khi ghi sự kiện ở ngoài đồng: sự kiện được lưu **offline** và
  xuất hiện ở menu **Sự kiện chờ đồng bộ**, tự đồng bộ khi có mạng.
- Thông báo hệ thống hiển thị ở góc phải (chuông thông báo).
- Khi gặp lỗi hệ thống, liên hệ quản trị viên; xem thêm
  [OPERATIONS.md](./OPERATIONS.md) phần Troubleshooting.