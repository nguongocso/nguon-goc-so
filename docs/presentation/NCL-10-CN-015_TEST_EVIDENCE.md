# Bằng chứng Kiểm thử Hồi quy Đầu cuối — User Story NCL-10-CN-015

> **Mã User Story:** NCL-10-CN-015  
> **Tên Story:** Kiểm thử hồi quy đầu cuối và cập nhật tài liệu vận hành  
> **Môi trường kiểm thử:** Local Runtime (Spring Boot 3.5.16 + Java 21, Vite Frontend 8 + React 19, MariaDB 10.4 / MySQL 8.4)  
> **Ngày thực hiện:** 11/09/2026 – 12/09/2026  
> **Trạng thái nghiệm thu:** **ĐẠT CÓ ĐIỀU KIỆN (CONDITIONAL PASS / PARTIAL)** — Chi tiết ma trận kiểm toán và khoảng trống Audit Log ở Mục 5 & 8.

---

## 1. Bảng tổng hợp kết quả kiểm thử (Theo từng vế tiêu chí)

| Ký hiệu tiêu chí | Nội dung kiểm thử chi tiết | Trạng thái | Ghi chú & Bằng chứng thực tế |
|---|---|:---:|---|
| **NCL-10-CN-015-TC-01** | **Kịch bản hồi quy đầu cuối đầy đủ (20 bước)**:<br>- Tạo tổ chức mới & gán quyền<br>- Khai báo vùng trồng, tạo & duyệt lô<br>- Ghi nhật ký canh tác đủ 5 mốc bắt buộc<br>- Thu hoạch sau cách ly, sơ chế riêng biệt, đóng gói<br>- In tem QR (PDF export) tách biệt kích hoạt tem<br>- Vận chuyển (Transport event)<br>- Bàn giao & tiếp nhận (NCL-05-CN-009, QTN-31)<br>- Tra cứu công khai & Phản ánh chất lượng (`ProductFeedback`)<br>- Truy vết phạm vi & Thu hồi hàng loạt (QTN-22)<br>- Đóng vụ việc thu hồi kèm khắc phục (NCL-08-CN-012, QTN-27) | **PASS** ✅ | Đã chạy live trên backend. 100% các bước đều có log API/JSON thực tế.<br>- Tính năng Phản ánh: đã có sẵn từ trước (`ProductFeedbackController` + `ProductFeedbackManagementController`), tạo & tra cứu thành công.<br>- In tem: trả về file PDF chuẩn MIME `application/pdf` 42,292 bytes.<br>- Sơ chế & Vận chuyển: sinh sự kiện chuỗi độc lập `PREPROCESSING` và `TRANSPORT`. |
| **NCL-10-CN-015-TC-02 (QTN-19)** | **Tính toàn vẹn chuỗi băm sự kiện (Event Hash Chaining)** trên lô hàng có cả sự kiện Bàn giao (`HANDOVER`) và Thu hồi (`RECALL`) | **PASS** ✅ | Chuỗi 12 sự kiện liên tục trên lô hàng `Lo hang 1`, thuật toán SHA-256, `isIntegrityVerified: true`, `verificationStatus: INTACT`. Không có vết đứt gãy hay can thiệp. |
| **NCL-10-CN-015-TC-03a (QTN-01)** | **Từ chối truy cập chéo tổ chức (Cross-Org Access Denial)** trên 7 endpoint mới từ 4 sprint gần nhất | **PASS** ✅ | 7/7 bài kiểm toán bị từ chối chính xác qua `HTTP 403 Forbidden` hoặc `HTTP 400 Bad Request` (tenant filter). 0% rò rỉ dữ liệu chéo giữa các bên. |
| **NCL-10-CN-015-TC-03b (Audit Log)** | **Ghi lịch sử truy cập trái phép (Audit Log Generation)** khi bị từ chối truy cập chéo tổ chức | **FAIL** ❌ *(Khoảng trống)* | Hệ thống hiện tại (`GlobalExceptionHandler.java:225`) **chỉ ghi `ActivityLogEvent` cho các URI bắt đầu bằng `/api/v1/admin/monitoring`**. Các request bị 403/400 nghiệp vụ chéo tổ chức **không sinh bản ghi nào** trong bảng `activity_logs`. Số bản ghi `ACCESS_DENIED` trước và sau test vẫn giữ nguyên = 0. |
| **NCL-10-CN-015-TC-04 / CV-05** | **Cập nhật tài liệu vận hành và bàn giao** (`OPERATIONS.md`, `USER_GUIDE.md`, `DEMO_DATA.md`, `TEST_EVIDENCE.md`) | **PASS** ✅ | Đã bổ sung toàn diện quy trình vận hành bàn giao (§7.5), vụ việc thu hồi (§7.6), tiến độ chuỗi (§7.7), kiểm chứng chuỗi băm (§7.8), ma trận cách ly dữ liệu (§7.9), tài khoản demo mới (`orgmanager2`, `cm_tc2`, `recorder_tc`), và sửa đổi ký hiệu TC. |
| **Frontend Runtime & Quality Gate** | **Xác minh giao diện người dùng, unit test và build** | **PASS** ✅ | - Vitest: 43 test files, **245/245 tests PASS**.<br>- ESLint: **0 errors, 0 warnings**.<br>- Production Build: **Thành công** (`dist/assets/index-B1U3etrz.js` 3,238 kB).<br>- Ràng buộc UI: Nút thao tác bàn giao ẩn hoàn toàn khi không thuộc bên nhận (`canRespond = false`). |

---

## 2. Xác minh tính năng "Phản ánh" (Product Feedback — Bước 17/20)

### 2.1 Kết quả rà soát codebase
Tính năng "Phản ánh sản phẩm" **ĐÃ TỒN TẠI ĐẦY ĐỦ** trong hệ thống (thuộc các story nghiệp vụ giai đoạn trước, migration `V20260906232000` và `V20260908170000`):
- Entity: `ProductFeedback.java` (bảng `product_feedbacks`)
- Controller công khai:
  + `POST /api/v1/public/production-lots/{productionLotId}/feedbacks` (người tiêu dùng gửi phản ánh)
  + `POST /api/v1/public/product-feedbacks/lookup` (người tiêu dùng tra cứu tình trạng xử lý bằng mã một lần `lookupCode`)
- Controller quản lý nội bộ:
  + `GET /api/v1/product-feedbacks` (VT-01 xem toàn bộ, VT-02 xem theo tổ chức)
  + `GET /api/v1/product-feedbacks/{id}` (xem chi tiết)
  + `POST /api/v1/product-feedbacks/{id}/assign`, `close`, `recall-request`

### 2.2 Bằng chứng kiểm thử thực tế (Runtime API)
- **Gửi phản ánh công khai (Người tiêu dùng):**
  + **Endpoint:** `POST /api/v1/public/production-lots/00000000-0000-0000-0000-000200000003/feedbacks`
  + **Request Body:**
    ```json
    {
      "content": "San pham Xoai co mui la bat thuong va bao bi bi hong trong qua trinh van chuyen. De nghi doi tra.",
      "traceCodeValue": ""
    }
    ```
  + **Response (HTTP 200 OK):**
    ```json
    {
      "success": true,
      "status": 200,
      "data": {
        "id": "b5d58f17-08f4-4aa4-ac67-cb76b9075364",
        "lookupCode": "PA-VD2J-ZTGP-TD68-JYHS",
        "status": "NEW",
        "createdAt": "2026-09-12T09:40:55.293361"
      }
    }
    ```
- **Tra cứu phản ánh công khai qua mã tra cứu:**
  + **Endpoint:** `POST /api/v1/public/product-feedbacks/lookup`
  + **Request Body:** `{"lookupCode": "PA-VD2J-ZTGP-TD68-JYHS"}`
  + **Response (HTTP 200 OK):** `{"status": "NEW", "publicResponse": null}`
- **Quản lý HTX (VT-02 `orgmanager`) xem danh sách phản ánh nội bộ:**
  + **Endpoint:** `GET /api/v1/product-feedbacks?page=0&size=5`
  + **Response (HTTP 200 OK):**
    ```json
    {
      "id": "b5d58f17-08f4-4aa4-ac67-cb76b9075364",
      "content": "San pham Xoai co mui la bat thuong va bao bi bi hong trong qua trinh van chuyen. De nghi doi tra.",
      "status": "NEW",
      "severity": "INFORMATION",
      "productionLotId": "00000000-0000-0000-0000-000200000003",
      "productionLotName": "Lô Xoài 03"
    }
    ```

---

## 3. Hoàn thiện các bước còn thiếu trong kịch bản 20 bước

### 3.1 Tạo tổ chức mới và phân quyền thành viên
- **Mục tiêu:** Tạo tổ chức độc lập hoàn toàn (không dùng dữ liệu demo có sẵn) và phân quyền tài khoản quản lý (VT-02) cùng người ghi sự kiện (VT-03).
- **Tạo tổ chức mới:**
  + **API:** `POST /api/v1/admin/organizations` (gọi bởi Quản trị viên VT-01 `admin`)
  + **Payload:**
    ```json
    {
      "organizationName": "HTX Che Bao Loc Test NCL015",
      "organizationCode": "HTX-BAOLOC-015",
      "organizationType": "COOPERATIVE",
      "address": "123 Tran Phu Bao Loc Lam Dong",
      "phone": "0912345678",
      "email": "htxbaoloc015@test.vn",
      "fullName": "Quan Ly HTX Bao Loc",
      "userName": "htxbaoloc015mgr",
      "password": "Admin@12345",
      "managerPhone": "0912111222",
      "managerEmail": "mgr015@htxbaoloc.vn"
    }
    ```
  + **Response:** Tạo thành công tổ chức ID `a1016671-10dc-40b0-8b88-b3c3ff4f5327`.
- **Thêm thành viên VT-03:**
  + **API:** `POST /api/v1/admin/organizations/a1016671-10dc-40b0-8b88-b3c3ff4f5327/members`
  + **Payload:** `{"username":"recorder015baoloc","password":"Recorder@12345","fullName":"Nguoi Ghi Su Kien Bao Loc","phone":"0912333444","email":"recorder015@htxbaoloc.vn","roleId":3}`
  + **Response:** Thành viên `recorder015baoloc` gia nhập với vai trò `VT-03`, trạng thái `ACTIVE`.
- **Đăng nhập:** Đăng nhập thành công tài khoản `htxbaoloc015mgr` (vai trò `VT-02` tại `HTX Che Bao Loc Test NCL015`).

### 3.2 Khai báo vùng trồng, tạo & duyệt lô sản xuất
- **Khai báo vùng trồng:**
  + **API:** `POST /api/v1/farm-areas`
  + **Response:** Vùng trồng ID `1290567b-0bbe-4715-988b-5a13305b6fea` ("Vung Trong Che Bao Loc 01", 3.5 HA).
- **Tạo lô sản xuất:**
  + **API:** `POST /api/v1/production-lots`
  + **Response:** Lô sản xuất ID `59414648-a774-4dac-ae05-1ffa88388809` ("Lo Che ST Bao Loc 2026", 500 kg), trạng thái khởi tạo `DRAFT`.
- **Trình duyệt & Phê duyệt:**
  + `POST /api/v1/production-lots/59414648-a774-4dac-ae05-1ffa88388809/submit` → Trạng thái `PENDING`.
  + `POST /api/v1/production-lots/59414648-a774-4dac-ae05-1ffa88388809/approve` với `{"approved":true,"reason":"Chap thuan lo san xuat NCL-10-CN-015"}` → Trạng thái chuyển sang `APPROVED`.

### 3.3 Ghi nhật ký canh tác đủ các mốc bắt buộc
- **API:** `POST /api/v1/farm-logs` (thực hiện bởi VT-03 `recorder015baoloc`).
- **Bản ghi mốc bón phân (`FERTILIZING`):**
  + **Payload:**
    ```json
    {
      "productionLotId": "59414648-a774-4dac-ae05-1ffa88388809",
      "activityType": "FERTILIZING",
      "executedDate": "2026-02-15",
      "material": "Phan huu co sinh hoc",
      "quantity": 200.0,
      "unit": "kg",
      "notes": "Bon phan huu co 200kg/1000m2"
    }
    ```
  + **Response (HTTP 200):** FarmLog ID `6878c8ac-ab0c-49bc-a24b-19f30c3bfa63`, ngày thực hiện `2026-02-15`.

### 3.4 Ghi sự kiện Thu hoạch sau cách ly & Sơ chế độc lập
- **Sự kiện Thu hoạch (`HARVEST`):**
  + **API:** `POST /api/v1/chain-events/harvest`
  + **Payload:** `{"productionLotId":"59414648-a774-4dac-ae05-1ffa88388809","latitude":11.54,"longitude":107.81,"harvestDate":"2026-08-10","quantity":480}`
  + **Response (HTTP 201 CREATED):** Event ID `25bcb335-2d8b-4b63-971b-c1bfdb5acd2d`, loại sự kiện `HARVEST`.
- **Sự kiện Sơ chế (`PREPROCESSING`) — Bước độc lập:**
  + **API:** `POST /api/v1/chain-events/preprocessing`
  + **Payload:**
    ```json
    {
      "productionLotId": "59414648-a774-4dac-ae05-1ffa88388809",
      "latitude": 11.54,
      "longitude": 107.81,
      "preprocessingDate": "2026-08-12",
      "inputQuantity": 480,
      "outputQuantity": 400,
      "grade": "Hang A",
      "processingMethod": "Sao say truyen thong bang lo hoi"
    }
    ```
  + **Response (HTTP 201 CREATED):** Event ID `1b8bc96a-8aae-4996-84c1-676eaa54134b`, phương pháp sơ chế: *"Sao say truyen thong bang lo hoi"*, tỷ lệ thu hồi 400kg/480kg.
- **Sự kiện Đóng gói (`PACKAGING`):**
  + **API:** `POST /api/v1/chain-events/packaging`
  + **Response (HTTP 201 CREATED):** Event ID `599e7196-6cb5-4b27-bab8-ed047c59af64`.

### 3.5 In tem (Label Export) tách biệt với kích hoạt tem
Hệ thống thiết kế in tem (xuất file PDF) là một thao tác độc lập hoàn toàn với việc kích hoạt tem:
- **API:** `POST /api/v1/shipments/00000000-0000-0000-0000-000b00000002/labels/export` (thực hiện bởi VT-02 `quanly_htx`).
- **Payload:**
  ```json
  {
    "startIndex": 0,
    "count": 10,
    "labelSize": "50x40",
    "includeFields": {
      "productName": true,
      "cooperativeName": true,
      "lotCode": true,
      "packagingDate": true
    }
  }
  ```
- **Kết quả HTTP:** `200 OK`
- **Headers trả về:**
  + `Content-Type: application/pdf`
  + `Content-Disposition: attachment; filename="Tem_QR_...pdf"`
  + `Content-Length: 42292` bytes (file nhị phân PDF hoàn chỉnh có Magic Header `%PDF`).

### 3.6 Ghi sự kiện Vận chuyển (TRANSPORT) như một bước riêng
- **API:** `POST /api/v1/chain-events/transport` (thực hiện bởi VT-03 `nguoighi` thuộc `HTX Nông sản Xanh`).
- **Payload:**
  ```json
  {
    "codeValue": "893001000031",
    "fromLocation": "Kho HTX Nong San Xanh, Can Tho",
    "toLocation": "Trung Tam Phan Phoi Sai Gon, TP. HCM",
    "transportTime": "2026-09-12T10:00:00",
    "deviceSource": "WEB"
  }
  ```
- **Kết quả HTTP:** `201 CREATED`
- **Response:**
  ```json
  {
    "id": "ff97fa57-b157-49f3-970a-13fe4c2387b7",
    "eventType": "TRANSPORT",
    "eventData": {
      "fromLocation": "Kho HTX Nong San Xanh, Can Tho",
      "toLocation": "Trung Tam Phan Phoi Sai Gon, TP. HCM"
    }
  }
  ```

---

## 4. Kiểm chứng tính toàn vẹn chuỗi băm sự kiện (NCL-10-CN-015-TC-02, QTN-19)

Kiểm thử trên lô hàng `Lo hang 1` (`347feea9-7825-4f4a-9413-cc9005da9e07`):
- **API:** `GET /api/v1/shipments/347feea9-7825-4f4a-9413-cc9005da9e07/verify-chain`
- **Kết quả xác minh:**
  + `hashAlgorithm`: `SHA-256`
  + `isIntegrityVerified`: `true`
  + `verificationStatus`: `INTACT`
  + `totalEvents`: `12`
- **Chuỗi liên kết băm:**
  + Sự kiện 1 (`WAREHOUSE_ENTRY`): `hash: 13480abc...`, `previousHash: null`
  + Sự kiện 2 (`WAREHOUSE_EXIT`): `hash: cf28fd95...`, `previousHash: 13480abc...` (khớp 100%)
  + ...
  + Sự kiện 12 (`HANDOVER`): `hash: 79dd4006...`, `previousHash: 0c4b66ea...` (khớp 100%)

---

## 5. Kiểm toán Cách ly Dữ liệu Đa tổ chức & Audit Log (NCL-10-CN-015-TC-03)

### 5.1 Phân tích cơ chế Audit Log hiện có
Trong `GlobalExceptionHandler.java:224-247`:
```java
private void publishAccessDeniedAudit(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/api/v1/admin/monitoring")) {
        return; // CHỈ GHI AUDIT LOG CHO ENDPOINT GIÁM SÁT HỆ THỐNG
    }
    // publish ActivityLogEvent(action="ACCESS_DENIED")
}
```
Khi người dùng thuộc tổ chức A truy cập trái phép tài nguyên của tổ chức B:
- Bộ lọc Security hoặc Service Layer ném ra `AccessDeniedException` (HTTP 403) hoặc `ResourceNotFoundException` / `BusinessException` (HTTP 400).
- Do URI không bắt đầu bằng `/api/v1/admin/monitoring`, hàm `publishAccessDeniedAudit` thoát ngay (`return`), **không có sự kiện `ActivityLogEvent` nào được phát ra**.
- Truy vấn trực tiếp cơ sở dữ liệu: `SELECT COUNT(*) FROM activity_logs WHERE action = 'ACCESS_DENIED'` trước và sau khi kích hoạt các truy cập trái phép đều trả về giá trị **`0`**.

### 5.2 Ma trận kiểm toán độc lập 2 vế (Từ chối vs Ghi lịch sử)

| # | Endpoint kiểm toán | Hành động kiểm tra | Kết quả: Từ chối truy cập | Đánh giá | Kết quả: Ghi lịch sử Audit Log | Đánh giá |
|---|---|---|---|:---:|---|:---:|
| 1 | `GET /api/v1/handovers` | HTX 1 vs HTX 2 lấy danh sách phiếu bàn giao | Scoped theo `organizationId` (HTX 1: 0, HTX 2: 14 bản ghi) | **PASS** ✅ | N/A (query hợp lệ, không phải truy cập trái phép) | **PASS** ✅ |
| 2 | `POST /api/v1/handovers/{id}/accept` | HTX 1 cố ý duyệt phiếu bàn giao của HTX 2 | Chặn qua `HTTP 403 Forbidden` | **PASS** ✅ | Không ghi bản ghi `ACCESS_DENIED` vào `activity_logs` | **FAIL** ❌ |
| 3 | `GET /api/v1/recall-cases` | HTX 1 vs HTX 2 lấy danh sách vụ việc thu hồi | Scoped theo `organizationId` (HTX 1: 1, HTX 2: 1 vụ việc) | **PASS** ✅ | N/A (query hợp lệ, scoped đúng tenant) | **PASS** ✅ |
| 4 | `GET /api/v1/recall-cases/{id}` | HTX 2 nhập ID vụ việc của HTX 1 | Chặn qua `HTTP 400 Bad Request` ("Không tìm thấy vụ việc thu hồi") | **PASS** ✅ | Không ghi log vi phạm bảo mật đa tổ chức | **FAIL** ❌ |
| 5 | `GET /api/v1/recall-cases` | Thu mua (VT-04) truy cập danh sách vụ việc | Chặn qua `HTTP 403 Forbidden` | **PASS** ✅ | Không ghi bản ghi vào `activity_logs` | **FAIL** ❌ |
| 6 | `GET /api/v1/production-lots/chain-progress` | Phân tách tiến độ chuỗi đa tổ chức | Lọc nghiêm ngặt theo `organization_id` của phiên | **PASS** ✅ | N/A (query hợp lệ theo đúng phân quyền) | **PASS** ✅ |
| 7 | `GET /api/v1/trace/impact-scope` | HTX 2 truy vết lô của HTX 1 | Chặn qua `HTTP 400 Bad Request` ("Không có quyền xem thông tin truy vết của tổ chức khác") | **PASS** ✅ | Không ghi log vi phạm bảo mật đa tổ chức | **FAIL** ❌ |

**Kết luận tiêu chí TC-03:**
- Vế **Từ chối truy cập chéo tổ chức**: **PASS (7/7)**.
- Vế **Ghi lịch sử vi phạm (Audit Log)**: **FAIL**. Đây là một khoảng trống kỹ thuật trong `GlobalExceptionHandler` cần được bổ sung bởi một User Story chuyên sâu về Security Auditing trong tương lai.

---

## 6. Xác minh Frontend Runtime & Quality Gate

### 6.1 Kiểm thử tự động (Frontend Unit/Integration Tests)
- Chạy lệnh `npm test -- --run` (Vitest v4.1.11):
  + **Tổng số file test:** 43 files passed.
  + **Tổng số test cases:** **245 passed / 245 total (100% PASS, 0 fail)**.
  + Thời gian chạy: 86.91s.
  + Bao gồm kiểm thử chi tiết cho: `HandoverDetailPage.test.tsx` (12 tests), `BulkRecallRequestForm.test.tsx` (5 tests), `ProductFeedbackForm.test.tsx` (2 tests), `AreaAssignmentPage.test.tsx` (14 tests).

### 6.2 Kiểm tra mã nguồn tĩnh (ESLint)
- Chạy lệnh `npm run lint`:
  + **Kết quả:** 0 lỗi, 0 cảnh báo (`eslint .` kết thúc mã thoát 0).

### 6.3 Kiểm tra đóng gói sản xuất (Production Build)
- Chạy lệnh `npm run build` (`tsc -b && vite build`):
  + **Kết quả:** Build thành công trong 16.41s.
  + Output artifacts: `dist/index.html` (0.59 kB), `dist/assets/index-B1U3etrz.js` (3,238.42 kB), `dist/assets/index-BqEgVfvM.css` (208.57 kB).

### 6.4 Xác minh Ràng buộc Giao diện Đa tổ chức (UI-level Verification)
Đối chiếu mã nguồn giao diện `frontend/src/pages/shipment-handover/HandoverDetailPage.tsx`:
```tsx
const canRespond =
  user?.organizationId != null &&
  handover.status === "PENDING_CONFIRMATION" &&
  user.organizationId === handover.toOrganizationId &&
  (user.roleCode === "VT-02" || user.roleCode === "VT-04");
```
- Khi người dùng đăng nhập không thuộc tổ chức nhận (`user.organizationId !== handover.toOrganizationId`) hoặc phiếu không ở trạng thái chờ:
  + Biến `canRespond` nhận giá trị `false`.
  + Khối thẻ hành động `<Card className="border-emerald-200 bg-emerald-50/40">` chứa 2 nút **"Xác nhận nhận hàng"** và **"Từ chối nhận hàng"** **KHÔNG ĐƯỢC RENDER VÀO DOM**.
  + Người dùng tổ chức khác hoàn toàn không có khả năng kích hoạt thao tác từ UI (bảo vệ kép: UI disable/ẩn + API 403 chặn).

---

## 7. Kết quả Pre-check 3 User Story Phụ thuộc

Mặc dù trong một số tài liệu tổng hợp backlog trước đó còn để nhãn *"Chưa thực hiện"*, điều tra mã nguồn và lịch sử Git xác nhận cả 3 User Story phụ thuộc đều **ĐÃ ĐƯỢC TRIỂN KHAI VÀ MERGE VÀO NHÁNH CHÍNH**:

| Story ID | Trạng thái thực tế | Bằng chứng Commit & PR | Tệp mã nguồn & Endpoint cốt lõi chứng minh |
|---|:---:|---|---|
| **NCL-05-CN-009** (Bàn giao lô hàng) | **ĐÃ HOÀN THÀNH** | PR #120, Commit `17eac11d` & `7cdea5a4` | - `HandoverController.java` (`POST /{id}/accept`, `POST /{id}/reject`)<br>- `HandoverServiceImpl.java` (chuyển `organizationId` của shipment QTN-31)<br>- `HandoverDetailPage.tsx`, `HandoverListPage.tsx` |
| **NCL-08-CN-012** (Đóng vụ việc thu hồi) | **ĐÃ HOÀN THÀNH** | PR #117, Commit `d8b76729` & `5516bb85` | - `RecallCaseController.java` (`GET /recall-cases`, `PUT /recall-cases/{id}/close`)<br>- `RecallCaseServiceImpl.java` (Lazy materialization, kiểm tra QTN-27)<br>- `BulkRecallRequestServiceImpl.java` (kiểm tra 4 mắt QTN-22) |
| **NCL-10-CN-011** (Tài liệu bàn giao) | **ĐÃ HOÀN THÀNH** | PR #92, Commit `c3abb6a1` & `f5fdb723` | - `docs/handover/OPERATIONS.md`<br>- `docs/handover/USER_GUIDE.md`<br>- `docs/handover/DEMO_DATA.md`<br>- `docs/presentation/TEST_EVIDENCE.md` |

*Lý do nhãn backlog chưa cập nhật:* Các nhóm tính năng được merge thông qua các PR độc lập từ các nhánh tính năng song song, tài liệu checklist tổng thể ở `docs/agent/` chưa được đồng bộ kịp thời sau các đợt merge PR.

---

## 8. Nguồn gốc Nhánh & Tính toàn vẹn Quy tắc QTN-31

- **Gốc phân nhánh (Merge Base):** Nhánh `feature/NCL-10-CN-015-e2e-regression-test` được tạo trực tiếp từ HEAD của `develop` tại commit `17eac11d` (chính là commit merge PR #120 của `NCL-05-CN-009`).
- **Trạng thái bản sửa lỗi QTN-31:** Commit `7cdea5a4` (*"feat(trace): chuyen quyen so huu lo hang sang to chuc nhan khi xac nhan ban giao"*) đã nằm trọn vẹn bên trong PR #120 và đã được merge vào `develop`.
- **Kết luận:** Nhánh `feature/NCL-10-CN-015-e2e-regression-test` **KHÔNG phụ thuộc vào bất kỳ nhánh tính năng unmerged nào**. Nhánh hoàn toàn độc lập và sẵn sàng để mở Pull Request vào `develop`.

---

## 9. Đánh giá Tổng kết Nghiệm thu

- **Kịch bản E2E 20 bước (NCL-10-CN-015-TC-01):** **PASS** ✅
- **Toàn vẹn chuỗi băm sự kiện (NCL-10-CN-015-TC-02):** **PASS** ✅
- **Cách ly truy cập chéo tổ chức (NCL-10-CN-015-TC-03a):** **PASS** ✅
- **Ghi lịch sử truy cập trái phép (NCL-10-CN-015-TC-03b):** **FAIL ❌ (Khoảng trống kỹ thuật)**
- **Tài liệu vận hành & Hướng dẫn (NCL-10-CN-015-TC-04):** **PASS** ✅
- **Frontend Quality Gate (Vitest, ESLint, Build):** **PASS** ✅
