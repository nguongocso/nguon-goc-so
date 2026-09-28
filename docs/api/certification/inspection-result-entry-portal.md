# API Docs — Cổng nhập kết quả dành cho đơn vị kiểm nghiệm (NCL-11-CN-007)

> **Story:** NCL-11-CN-007 — Cổng nhập kết quả dành cho đơn vị kiểm nghiệm<br>
> **Epic:** NCL-11 — Sơ chế và kiểm nghiệm chất lượng<br>
> **Vai trò:** Quản lý hợp tác xã (`VT-02`) và Đơn vị kiểm nghiệm bên ngoài (Public Token)<br>
> **Trạng thái:** Implemented / Approved Contract<br>
> **Quy tắc nghiệp vụ:** QTN-14, QTN-20, QTN-21<br>
> **Tiêu chí chấp nhận:** NCL-11-CN-007-TC-01, TC-02, TC-03, TC-04

---

## 1. Mục tiêu và phạm vi nghiệp vụ

Tài liệu này định nghĩa API phục vụ cổng nhập kết quả kiểm nghiệm độc lập dành cho đơn vị kiểm nghiệm bên ngoài thông qua liên kết dùng một lần có thời hạn, nhằm đảm bảo:
1. Đơn vị kiểm nghiệm tự nhập kết quả trực tiếp mà không cần tài khoản hay vai trò người dùng trong hệ thống (`TC-01`).
2. Phân biệt rõ nguồn nhập của từng kết quả kiểm nghiệm: `TESTING_UNIT_PORTAL` (đơn vị kiểm nghiệm tự khai) và `COOPERATIVE_MANUAL` (HTX nhập thủ công) (`TC-01`, `TC-04`).
3. Liên kết có thời hạn (1–30 ngày, mặc định 7 ngày). Khi quá hạn (`now >= expiresAt`), liên kết bị từ chối truy cập với mã lỗi `410 GONE` kèm hướng dẫn liên hệ HTX để cấp lại (`TC-02`, `QTN-14`).
4. Liên kết chỉ dùng được một lần: ngay khi gửi kết quả thành công, liên kết chuyển sang trạng thái `USED`. Các lần truy cập lại hoặc gửi lại đều bị từ chối với mã `410 GONE` (`TC-03`, `QTN-14`).
5. Bảo mật theo `QTN-20`: Token 32-byte ngẫu nhiên an toàn, chỉ lưu hàm băm SHA-256 trong cơ sở dữ liệu, không ghi log token thô hoặc URL hoàn chỉnh; phạm vi truy cập bị giới hạn nghiêm ngặt ở đúng một yêu cầu kiểm nghiệm và các chỉ tiêu của nó; áp dụng rate-limiting.
6. Tính nhất quán theo `QTN-21`: Tái sử dụng toàn bộ logic kiểm tra hợp lệ, chốt trạng thái yêu cầu (`PASSED`/`FAILED`), quét cảnh báo hạn hiệu lực và điều kiện kích hoạt tem như luồng HTX nhập tay.

---

## 2. Ma trận ánh xạ yêu cầu (Traceability Matrix)

| Mã yêu cầu / Quy tắc | Mục tiêu nghiệp vụ | Endpoint phụ trách | Trạng thái / Mã lỗi |
|---|---|---|---|
| `TC-01` | Cấp link, đơn vị mở link, nhập đủ kết quả và gửi thành công | `POST /api/v1/inspection-requests/{id}/result-entry-links`<br>`GET /api/v1/public/inspection-result-entry/{token}`<br>`PUT /api/v1/public/inspection-result-entry/{token}/results` | `201 CREATED`<br>`200 OK`<br>`200 OK` (`entrySource = TESTING_UNIT_PORTAL`) |
| `TC-02` | Link quá hạn bị từ chối và hướng dẫn liên hệ HTX | `GET /api/v1/public/inspection-result-entry/{token}`<br>`POST .../file`, `PUT .../results` | `410 GONE` ("Liên kết đã hết hạn...") |
| `TC-03` | Link đã dùng chỉ dùng một lần, chống gửi trùng / replay | `PUT /api/v1/public/inspection-result-entry/{token}/results` | `410 GONE` (lần 2 trở đi bị từ chối) |
| `TC-04` | Đơn vị gửi bản giấy, HTX tự nhập | `PUT /api/v1/inspection-requests/{id}/results` (API hiện hữu) | `200 OK` (`entrySource = COOPERATIVE_MANUAL`, thu hồi link active cũ) |
| `QTN-14` | Vòng đời token có hạn, dùng một lần, cấp lại tạo token mới | `POST /api/v1/inspection-requests/{id}/result-entry-links` | Thu hồi link `ACTIVE` cũ, sinh token mới |
| `QTN-20` | Băm SHA-256, không log secret, scope hẹp, rate limiting | Bộ lọc / Service / Controller Public | Generic error, 429 nếu vượt hạn mức |
| `QTN-21` | Dùng chung kiểm tra đạt/không đạt, cảnh báo hết hạn, kích hoạt tem | `InspectionCriterionResultServiceImpl` | Cập nhật `PASSED`/`FAILED`, quét expiry |

---

## 3. Vòng đời liên kết (State Machine)

```text
[Chưa có link]
      │
      │ (VT-02 cấp link: request PENDING_RESULT, có testingUnitId)
      ▼
   ACTIVE ──(Quá hạn: now >= expires_at)──► EXPIRED (410 GONE)
      │
      ├──(VT-02 cấp lại link mới HOẶC HTX tự nhập tay)──► REVOKED (410 GONE)
      │
      └──(Đơn vị kiểm nghiệm submit batch thành công)──► USED (410 GONE)
```

- **ACTIVE:** Liên kết hợp lệ, cho phép xem chi tiết chỉ tiêu, tải lên tệp và gửi kết quả.
- **USED:** Kết quả đã được ghi nhận vào hệ thống. Không thể tái sử dụng.
- **REVOKED:** Đã bị thu hồi do Quản lý HTX cấp lại link mới hoặc đã tự ghi nhận kết quả thủ công.
- **EXPIRED:** Đã quá hạn thời gian hiệu lực được cấp.

---

## 4. Chi tiết các Endpoint

### 4.1. Cấp hoặc cấp lại liên kết nhập kết quả (Authenticated VT-02)

- **Phương thức:** `POST`
- **Đường dẫn:** `/api/v1/inspection-requests/{requestId}/result-entry-links`
- **Xác thực:** Bắt buộc JWT Access Token, vai trò `VT-02`.
- **Tenant Isolation:** Yêu cầu kiểm nghiệm phải thuộc tổ chức của người dùng hiện tại; nếu không thuộc hoặc không tồn tại thì trả về `404 NOT FOUND`.
- **Điều kiện tiên quyết:**
  - Yêu cầu kiểm nghiệm phải ở trạng thái `PENDING_RESULT` (nếu không trả về `409 CONFLICT`).
  - Yêu cầu phải có `testingUnitId` hợp lệ được chọn từ trước (nếu chưa có trả về `400 BAD REQUEST`).
- **Hành vi:**
  - Nếu đã có liên kết `ACTIVE` cho yêu cầu này, chuyển trạng thái liên kết cũ thành `REVOKED` kèm thông tin `revokedBy`, `revokedAt`.
  - Tạo liên kết mới với 32-byte SecureRandom, lưu SHA-256 vào database.
  - Gửi email thông báo bất đồng bộ tới `recipientEmail` chứa URL cổng nhập kết quả.
  - Trả về URL thô **đúng một lần duy nhất** trong response `201 CREATED`. Tuyệt đối không ghi log URL này.

#### Request Body
```json
{
  "recipientEmail": "kiemnghiem@trungtam-a.vn",
  "expiryDays": 7
}
```
- `recipientEmail`: Chuỗi email bắt buộc, đúng định dạng RFC, độ dài tối đa 255 ký tự.
- `expiryDays`: Số nguyên từ 1 đến 30 (mặc định 7 nếu không truyền).

#### Response `201 CREATED`
```json
{
  "success": true,
  "status": 201,
  "message": "Cấp liên kết nhập kết quả kiểm nghiệm thành công.",
  "data": {
    "id": "c1f7a052-64e8-466d-9be2-e8d9c22880b1",
    "status": "ACTIVE",
    "recipientEmail": "kiemnghiem@trungtam-a.vn",
    "expiresAt": "2026-09-23T14:30:00",
    "entryUrl": "https://nguongocso.vn/inspection-result-entry/d41d8cd98f00b204e9800998ecf8427e..."
  },
  "timestamp": "2026-09-16T14:30:00"
}
```

---

### 4.2. Xem trạng thái liên kết mới nhất (Authenticated VT-02)

- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/inspection-requests/{requestId}/result-entry-links/latest`
- **Xác thực:** Bắt buộc JWT Access Token, vai trò `VT-02`.
- **Tenant Isolation:** Kiểm tra quyền sở hữu theo tổ chức.
- **Bảo mật:** Không bao giờ trả về token thô, mã băm token, hay đường dẫn đầy đủ chứa token bí mật.

#### Response `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy thông tin liên kết mới nhất thành công.",
  "data": {
    "id": "c1f7a052-64e8-466d-9be2-e8d9c22880b1",
    "status": "ACTIVE",
    "recipientEmail": "kiemnghiem@trungtam-a.vn",
    "tokenPrefix": "d41d8cd9",
    "expiresAt": "2026-09-23T14:30:00",
    "usedAt": null,
    "createdAt": "2026-09-16T14:30:00"
  },
  "timestamp": "2026-09-16T14:35:00"
}
```
*(Nếu yêu cầu chưa từng được cấp liên kết nào, trả về `404 NOT FOUND`).*

---

### 4.3. Đọc dữ liệu yêu cầu kiểm nghiệm công khai qua Token (Public)

- **Phương thức:** `GET`
- **Đường dẫn:** `/api/v1/public/inspection-result-entry/{token}`
- **Xác thực:** Không yêu cầu JWT đăng nhập.
- **Bảo mật:**
  - Token thô được băm SHA-256 để tìm kiếm liên kết.
  - Phản hồi có header `Cache-Control: no-store, no-cache, must-revalidate`.
  - Không tiết lộ `organizationId`, mã UUID nội bộ của lô hay yêu cầu, thông tin người tạo hay đường dẫn tệp trên server.
  - Nếu token không tồn tại, trả về `404 NOT FOUND` với thông báo chung: `"Liên kết không hợp lệ hoặc không tồn tại."`.
  - Nếu token hết hạn (`now >= expiresAt`), trả về `410 GONE`: `"Liên kết đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp lại."`.
  - Nếu token đã dùng (`status = USED`) hoặc đã thu hồi (`status = REVOKED`), trả về `410 GONE`: `"Liên kết đã được sử dụng hoặc đã được thay thế."`.

#### Response `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy thông tin yêu cầu kiểm nghiệm thành công.",
  "data": {
    "testingUnitName": "Trung tâm Kiểm nghiệm Chất lượng Nông sản Quốc gia",
    "lotCode": "LO-XOAI-2026-001",
    "lotName": "Lô Xoài Cát Chu xuất khẩu",
    "sampleSentDate": "2026-09-15",
    "expiresAt": "2026-09-23T14:30:00",
    "criteria": [
      {
        "criterionId": "a90ef599-270f-4889-b883-938b8eb3065a",
        "code": "RESIDUE_PESTICIDE",
        "name": "Dư lượng thuốc bảo vệ thực vật",
        "standardName": "TCVN 11892-1:2017"
      },
      {
        "criterionId": "b11ef599-270f-4889-b883-938b8eb3065b",
        "code": "HEAVY_METAL_LEAD",
        "name": "Hàm lượng Chì (Pb)",
        "standardName": "QCVN 8-2:2011/BYT"
      }
    ]
  },
  "timestamp": "2026-09-16T14:40:00"
}
```

---

### 4.4. Tải lên tệp phiếu kết quả kiểm nghiệm qua Token (Public)

- **Phương thức:** `POST`
- **Đường dẫn:** `/api/v1/public/inspection-result-entry/{token}/criteria/{criterionId}/file`
- **Content-Type:** `multipart/form-data`
- **Tham số form:** `file` (MultipartFile)
- **Quy tắc kiểm tra:**
  - Token phải ở trạng thái `ACTIVE` và chưa hết hạn (`expiresAt > now`).
  - `criterionId` phải thuộc đúng yêu cầu kiểm nghiệm mà liên kết được cấp (kiểm soát phạm vi đối tượng / chống ghi đè trái phép).
  - Tệp cho phép: Định dạng JPG, PNG, PDF; dung lượng tối đa 5 MB.
  - Tên tệp được sinh ngẫu nhiên trên máy chủ để chống path traversal.
- **Phản hồi `200 OK`:**
```json
{
  "success": true,
  "status": 200,
  "message": "Tải lên phiếu kết quả kiểm nghiệm thành công.",
  "data": {
    "fileHandle": "pfh_8a3f9e4210d742b6a90ef599270f4889",
    "filePath": "pfh_8a3f9e4210d742b6a90ef599270f4889"
  },
  "timestamp": "2026-09-16T14:45:00"
}
```
> **Lưu ý bảo mật (BLOCKER 1 & QTN-20):** Server không trả về đường dẫn tệp tin thực tế trên máy chủ (`/uploads/...`), mà trả về mã định danh che giấu **Opaque File Handle** (`pfh_...`) được gán chặt với bộ ba `(tokenHash, requestId, criterionId)`. Client sử dụng mã `fileHandle` này để truyền vào trường `filePath` trong payload gửi kết quả. Mọi hành vi dùng file handle của yêu cầu/chỉ tiêu khác sẽ bị từ chối `403 FORBIDDEN`.

---

### 4.5. Gửi toàn bộ kết quả kiểm nghiệm qua Token (Public Submit)

- **Phương thức:** `PUT`
- **Đường dẫn:** `/api/v1/public/inspection-result-entry/{token}/results`
- **Xác thực:** Không yêu cầu JWT đăng nhập; quyền hạn được xác lập qua Token hợp lệ.
- **Bảo đảm giao dịch và chống gửi đồng thời (Atomic One-time Consumption):**
  1. Băm SHA-256 token thô, tìm kiếm liên kết kèm kiểm tra trạng thái `ACTIVE` và `expiresAt > now`.
  2. Xác minh yêu cầu kiểm nghiệm vẫn đang ở trạng thái `PENDING_RESULT`.
  3. Kiểm tra toàn bộ danh sách kết quả (all-or-nothing): phải chứa đầy đủ và không trùng lặp tất cả các chỉ tiêu thuộc yêu cầu kiểm nghiệm.
  4. Thực hiện câu lệnh cập nhật nguyên tử có điều kiện:
     ```sql
     UPDATE inspection_result_entry_links
        SET status = 'USED', used_at = :now, used_ip = :ip, used_user_agent = :ua
      WHERE id = :id AND status = 'ACTIVE' AND expires_at > :now
     ```
     Nếu số dòng bị ảnh hưởng = 0 (do race condition hoặc đã bị dùng trước đó), lập tức dừng và báo lỗi `410 GONE`.
  5. Lưu toàn bộ kết quả kiểm nghiệm với:
     - `entry_source = 'TESTING_UNIT_PORTAL'`
     - `portal_link_id = link.id`
     - `created_by = null`
  6. Chốt trạng thái yêu cầu kiểm nghiệm (`PASSED` nếu tất cả chỉ tiêu đạt và còn hạn, `FAILED` nếu có chỉ tiêu không đạt).
  7. Kích hoạt quét và cảnh báo hiệu lực kiểm nghiệm (`checkAndAlertLotExpiry`) theo `QTN-21`.

#### Request Body
```json
{
  "results": [
    {
      "criterionId": "a90ef599-270f-4889-b883-938b8eb3065a",
      "resultDate": "2026-09-16",
      "expiryDate": "2027-09-16",
      "passed": true,
      "filePath": "inspection-results/3fa85f64-5717-4562-b3fc-2c963f66afa6/8a3f9e42...pdf"
    },
    {
      "criterionId": "b11ef599-270f-4889-b883-938b8eb3065b",
      "resultDate": "2026-09-16",
      "expiryDate": "2027-09-16",
      "passed": true,
      "filePath": null
    }
  ]
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Ghi nhận kết quả kiểm nghiệm từ đơn vị kiểm nghiệm thành công.",
  "data": [
    {
      "resultId": "7d9b23b1-4f9e-4c7b-99f2-00b848c08123",
      "criterionId": "a90ef599-270f-4889-b883-938b8eb3065a",
      "criterionCode": "RESIDUE_PESTICIDE",
      "criterionName": "Dư lượng thuốc bảo vệ thực vật",
      "resultDate": "2026-09-16",
      "expiryDate": "2027-09-16",
      "passed": true,
      "filePath": "inspection-results/3fa85f64-5717-4562-b3fc-2c963f66afa6/8a3f9e42...pdf",
      "entrySource": "TESTING_UNIT_PORTAL",
      "createdByName": null,
      "createdAt": "2026-09-16T14:50:00",
      "updatedAt": "2026-09-16T14:50:00"
    }
  ],
  "timestamp": "2026-09-16T14:50:00"
}
```

---

## 5. Cập nhật bổ sung cho các API hiện hữu (Additive Changes)

### 5.1. DTO `InspectionCriterionResultResponse`
Bổ sung trường mới:
- `entrySource` (String enum: `"COOPERATIVE_MANUAL"` | `"TESTING_UNIT_PORTAL"`).
- `createdByName` (String, nullable khi kết quả được ghi qua cổng của đơn vị kiểm nghiệm).

### 5.2. Luồng HTX nhập tay (`PUT /api/v1/inspection-requests/{requestId}/results`)
- Gán `entry_source = 'COOPERATIVE_MANUAL'`.
- `created_by = currentUser.getUser()`.
- Tự động thu hồi (`status = REVOKED`) bất kỳ liên kết `ACTIVE` nào đang tồn tại của yêu cầu kiểm nghiệm này để chống ghi đè song song giữa hai bên.

### 5.3. Cổng tra cứu công khai (`PublicInspectionCriterionResultDto`)
- Bổ sung trường `entrySource` để hiển thị huy hiệu nguồn gốc ("Đơn vị kiểm nghiệm khai" hoặc "Hợp tác xã nhập").

### 5.4. Thông báo kết quả cho Hợp tác xã
- Khi toàn bộ chỉ tiêu đã được ghi nhận và yêu cầu chuyển sang `PASSED`, hệ thống gửi thông báo
  "Kết quả kiểm nghiệm đạt" cho người dùng thuộc tổ chức sở hữu lô có quyền `notification:READ`.
- Khi toàn bộ chỉ tiêu đã được ghi nhận và yêu cầu chuyển sang `FAILED`, hệ thống gửi cảnh báo
  "Kết quả kiểm nghiệm không đạt" cho cùng nhóm người nhận để quản lý HTX xử lý lô.
- Mỗi thông báo chỉ được tạo khi trạng thái yêu cầu thực sự thay đổi, tránh gửi lặp khi tải lại hoặc xem kết quả.

---

## 6. Bảng mã lỗi chi tiết

| Mã HTTP | Tình huống | Thông báo phản hồi mẫu |
|---:|---|---|
| `400 BAD REQUEST` | Email không đúng cú pháp, thời hạn ngoài 1–30 ngày, thiếu chỉ tiêu | `"Email người nhận không hợp lệ."` / `"Phải ghi kết quả cho tất cả chỉ tiêu của yêu cầu kiểm nghiệm."` |
| `403 FORBIDDEN` | Người dùng không có vai trò `VT-02` khi cấp link | `"Bạn không có quyền thực hiện chức năng này."` |
| `404 NOT FOUND` | Request không thuộc tổ chức (bảo vệ tenant) hoặc token không tồn tại | `"Yêu cầu kiểm nghiệm không tồn tại."` / `"Liên kết không hợp lệ hoặc không tồn tại."` |
| `409 CONFLICT` | Cấp link hoặc submit khi yêu cầu không còn `PENDING_RESULT` | `"Yêu cầu kiểm nghiệm phải ở trạng thái chờ kết quả."` |
| `410 GONE` | Token đã hết hạn | `"Liên kết đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp lại."` |
| `410 GONE` | Token đã dùng hoặc đã bị thu hồi | `"Liên kết đã được sử dụng hoặc đã được thay thế."` |
| `413 PAYLOAD TOO LARGE` | Tệp đính kèm vượt quá 5 MB | `"File vượt quá dung lượng cho phép (5MB)"` |
| `415 UNSUPPORTED MEDIA TYPE` | Tệp đính kèm không phải JPG, PNG, PDF | `"Loại file không hỗ trợ. Chỉ chấp nhận JPG, PNG, PDF"` |
| `429 TOO MANY REQUESTS` | Vượt quá tần suất truy cập cổng công khai | `"Bạn thao tác quá nhanh. Vui lòng thử lại sau."` |
