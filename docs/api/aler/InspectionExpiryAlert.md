# API Docs — Cảnh báo kết quả kiểm nghiệm sắp hết hiệu lực (Inspection Expiry Alert)

*Mã User Story: NCL-11-CN-004 Cảnh báo kết quả kiểm nghiệm sắp hết hiệu lực*

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
| :--- | :--- | :--- | :--- |
| 2026-09-10 | v1.0.0 | Khởi tạo tài liệu đặc tả API cảnh báo kết quả kiểm nghiệm sắp hết hiệu lực và hết hiệu lực, tích hợp in-app notifications, bổ sung dữ liệu hiệu lực vào DTO lô sản xuất theo QTN-21 và QTN-13 | Software Architect |

---

## 1. Thông tin chung

### 1.1 Mục tiêu
Hệ thống tự động theo dõi ngày hết hiệu lực sớm nhất (`earliestExpiryDate`) của các kết quả kiểm nghiệm đạt (`PASSED`) thuộc các lô sản xuất (`ProductionLot`) của hợp tác xã. Khi kết quả kiểm nghiệm tiến gần đến ngày hết hiệu lực (nằm trong ngưỡng cảnh báo cấu hình) hoặc đã quá ngày hết hiệu lực mà lô vẫn còn tem chưa kích hoạt, hệ thống sẽ:
1. Tự động tạo cảnh báo (`Alert`) lưu vào cơ sở dữ liệu với loại `INSPECTION_EXPIRING` hoặc `INSPECTION_EXPIRED`.
2. Tự động gửi thông báo (`Notification`) vào hộp thư thông báo của các tài khoản có vai trò Quản lý hợp tác xã (`VT-02`) thuộc tổ chức sở hữu lô.
3. Bổ sung khối thông tin hiệu lực (`inspectionValidity`) vào API danh sách và chi tiết lô sản xuất để frontend hiển thị nhãn trạng thái và cung cấp lối tắt tạo yêu cầu kiểm nghiệm mới khi quá hạn.
4. Đảm bảo quy tắc chống trùng lặp thông báo trong cùng một ngày: `1 lô + 1 loại cảnh báo hiệu lực kiểm nghiệm + 1 ngày = tối đa 1 notification`.
5. Loại trừ không cảnh báo với các lô đã bị thu hồi (`RECALLED`) hoặc các lô đã kích hoạt hết tem (`inactiveStampCount == 0`).

### 1.2 Vai trò & Phân quyền (RBAC)
- **Quản lý hợp tác xã (`VT-02`):**
  - Nhận thông báo trong hộp thông báo cá nhân khi kết quả kiểm nghiệm của lô thuộc tổ chức mình sắp hết hạn hoặc đã hết hạn.
  - Xem nhãn trạng thái hiệu lực kiểm nghiệm trên danh sách lô và trang chi tiết kiểm nghiệm lô.
  - Sử dụng lối tắt tạo yêu cầu kiểm nghiệm mới khi kết quả kiểm nghiệm đã hết hiệu lực.
  - Xem và giải quyết cảnh báo kiểm nghiệm thuộc tổ chức của mình.
- **Quản trị viên nền tảng (`VT-01`):**
  - Xem và quản lý toàn bộ cảnh báo trên toàn hệ thống.
  - Kích hoạt thủ công tiến trình quét kiểm tra hạn kiểm nghiệm phục vụ kiểm thử, vận hành và quản trị.

### 1.3 Quy tắc nghiệp vụ (Business Rules)
- **QTN-21 (Chặn kích hoạt tem khi chưa đủ điều kiện kiểm nghiệm):**
  - Lô sản xuất bắt buộc kiểm nghiệm phải có tất cả chỉ tiêu đạt và còn hiệu lực (`expiryDate >= LocalDate.now()`). Nếu kết quả kiểm nghiệm đã quá hạn, hệ thống chặn kích hoạt tem (`canActivate = false`). Cảnh báo sớm NCL-11-CN-004 giúp Quản lý HTX chủ động hoàn tất kích hoạt tem hoặc kiểm nghiệm lại trước khi bị chặn cứng bởi QTN-21.
- **QTN-13 (Chỉ gắn và hiển thị chứng nhận/kết quả còn hiệu lực):**
  - Kết quả kiểm nghiệm đã quá ngày hết hiệu lực (`today > earliestExpiryDate`) được coi là hết hiệu lực. Hệ thống không cho phép gắn kết quả hết hiệu lực này cho các đợt phát hành tem mới.
- **Ngưỡng cảnh báo sắp hết hiệu lực (Threshold):**
  - Ngưỡng cảnh báo được cấu hình thông qua thuộc tính hệ thống: `app.inspection.expiry-warning-threshold-days` (mặc định là `15` ngày).
  - Khi `0 <= earliestExpiryDate - LocalDate.now() <= threshold`, trạng thái hiệu lực là `EXPIRING`, hệ thống tạo Alert loại `INSPECTION_EXPIRING` và mức độ nghiêm trọng `MEDIUM`.
  - Khi `earliestExpiryDate < LocalDate.now()`, trạng thái hiệu lực là `EXPIRED`, hệ thống tạo Alert loại `INSPECTION_EXPIRED` và mức độ nghiêm trọng `HIGH`.
- **Điều kiện xét cảnh báo (Inspection Alert Eligibility):**
  Chỉ tạo cảnh báo cho lô sản xuất thỏa mãn **đồng thời** các điều kiện:
  1. Thuộc loại nông sản bắt buộc kiểm nghiệm (`productCategory.requiresInspection == true`).
  2. Lô chưa bị thu hồi (`lot.status != RECALLED`).
  3. Lô chưa ở trạng thái cuối hoặc hủy (`lot.status NOT IN (CANCELLED, DISPOSED, CLOSED)`).
  4. Lô có kết quả kiểm nghiệm với kết luận Đạt (`PASSED`) cho tất cả chỉ tiêu bắt buộc.
  5. Kết quả kiểm nghiệm có ngày hết hiệu lực xác định (`earliestExpiryDate != null`).
  6. **Lô còn tem chưa kích hoạt:** Tồn tại ít nhất 1 mã tem ở trạng thái `INACTIVE` thuộc các lô hàng chưa bị thu hồi (`inactiveStampCount > 0`).
  7. Ngày hết hiệu lực nằm trong ngưỡng cảnh báo (`daysRemaining <= threshold` hoặc `daysOverdue > 0`).
- **Không tạo cảnh báo trong các trường hợp:**
  - Lô đã bị thu hồi (`lot.status == RECALLED` hoặc tất cả lô hàng đã bị thu hồi).
  - Lô đã kích hoạt hết tem (`inactiveStampCount == 0` — tất cả tem in ra đều đã được kích hoạt `ACTIVE` hoặc hủy hỏng `CANCELLED`).
  - Lô không thuộc loại bắt buộc kiểm nghiệm (`requiresInspection == false`).
  - Lô chưa có kết quả kiểm nghiệm đạt hợp lệ.
  - Không có ngày hết hiệu lực hợp lệ.
  - **Đã gửi cảnh báo cùng loại cho cùng lô trong cùng ngày** (chống thông báo trùng lặp).
- **Quy tắc chống thông báo trùng lặp:**
  - `1 lô + 1 loại cảnh báo hiệu lực kiểm nghiệm + 1 ngày = tối đa 1 notification`.
  - Nếu scheduler chạy lại nhiều lần trong cùng ngày (do cron, retry, hoặc admin trigger thủ công), hệ thống kiểm tra và không tạo thêm bản ghi `Alert` hay `Notification` nào nếu đã có cảnh báo trong ngày.
- **Bảo toàn lịch sử:**
  - Kết quả kiểm nghiệm hết hiệu lực **tuyệt đối không bị xóa hoặc ghi đè**.
  - Khi tạo yêu cầu kiểm nghiệm mới, toàn bộ lịch sử kiểm nghiệm cũ vẫn được lưu giữ và hiển thị qua `GET /api/v1/production-lots/{lotId}/inspection-history`.

---

## 2. Đặc tả các Endpoints

### 2.1 GET /api/v1/production-lots
**Description:** Lấy danh sách lô sản xuất thuộc tổ chức hiện tại. Bổ sung khối dữ liệu `inspectionValidity` (additive, tương thích ngược) để frontend hiển thị badge trạng thái hiệu lực kiểm nghiệm, số ngày còn lại/quá hạn và hỗ trợ lọc theo trạng thái hiệu lực.

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** `VT-01`, `VT-02`, `VT-03` (đã đăng nhập).

**Query Parameters:**
| Field Name | Data Type | Required | Constraints / Description | Example |
|---|---|---|---|---|
| `search` | String | No | Tìm kiếm theo tên lô, mã lô | `"Bưởi Da Xanh"` |
| `status` | String | No | Lọc theo trạng thái lô sản xuất (`APPROVED`, `PACKAGED`, ...) | `"PACKAGED"` |

**Response — Success (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "name": "Lô Bưởi Da Xanh Vụ Thu 2026",
      "farmAreaId": "e2a3b4c5-5555-4a2a-9f3d-1a2b3c4d5e6f",
      "farmAreaName": "Vườn Bưởi Khu A",
      "productCategoryId": "c1a2b3c4-1111-4a2a-9f3d-1a2b3c4d5e6f",
      "productCategoryName": "Bưởi Da Xanh",
      "organizationName": "HTX Nông Nghiệp Sạch Bến Tre",
      "expectedQuantity": 1500.0,
      "expectedQuantityUnit": "kg",
      "actualQuantity": 1450.0,
      "plantingDate": "2026-03-10",
      "harvestDate": "2026-08-20",
      "status": "PACKAGED",
      "approvalNotes": "Đã duyệt kế hoạch thu hoạch",
      "createdByName": "Nguyễn Văn Nông",
      "approvedByName": "Trần Quản Lý",
      "createdAt": "2026-03-10T08:00:00",
      "updatedAt": "2026-08-25T14:30:00",
      "inspectionValidity": {
        "requiresInspection": true,
        "status": "EXPIRING",
        "earliestExpiryDate": "2026-09-20",
        "daysRemaining": 10,
        "daysOverdue": null,
        "canActivate": true,
        "canCreateNewRequest": true,
        "latestPassedRequestId": "d3b07384-d113-49d6-a212-32b704c35b6c",
        "inactiveStampCount": 200
      }
    },
    {
      "id": "99e8d7c6-b5a4-4c3d-2e1f-0a9b8c7d6e5f",
      "name": "Lô Thanh Long Ruột Đỏ Vụ Hè 2026",
      "farmAreaId": "f3b4c5d6-6666-4a2a-9f3d-1a2b3c4d5e6f",
      "farmAreaName": "Vườn Thanh Long Khu B",
      "productCategoryId": "d2b3c4d5-2222-4a2a-9f3d-1a2b3c4d5e6f",
      "productCategoryName": "Thanh Long Ruột Đỏ",
      "organizationName": "HTX Nông Nghiệp Sạch Bến Tre",
      "expectedQuantity": 3000.0,
      "expectedQuantityUnit": "kg",
      "actualQuantity": 2900.0,
      "plantingDate": "2026-02-15",
      "harvestDate": "2026-07-10",
      "status": "PACKAGED",
      "approvalNotes": null,
      "createdByName": "Nguyễn Văn Nông",
      "approvedByName": "Trần Quản Lý",
      "createdAt": "2026-02-15T08:00:00",
      "updatedAt": "2026-07-15T10:00:00",
      "inspectionValidity": {
        "requiresInspection": true,
        "status": "EXPIRED",
        "earliestExpiryDate": "2026-09-01",
        "daysRemaining": null,
        "daysOverdue": 9,
        "canActivate": false,
        "canCreateNewRequest": true,
        "latestPassedRequestId": "a1b2c3d4-e5f6-4a2a-9f3d-1a2b3c4d5e6f",
        "inactiveStampCount": 150
      }
    }
  ]
}
```

### 2.2 GET /api/v1/production-lots/{id}
**Description:** Lấy thông tin chi tiết một lô sản xuất. Bổ sung khối dữ liệu `inspectionValidity` phục vụ hiển thị card "Hiệu lực kết quả kiểm nghiệm" trên trang chi tiết kiểm nghiệm lô và hiển thị nút "Tạo yêu cầu kiểm nghiệm mới" khi quá hạn.

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** `VT-01`, `VT-02`, `VT-03` (thuộc cùng tổ chức sở hữu lô).

**Path Parameters:**
| Field Name | Data Type | Required | Description | Example |
|---|---|---|---|---|
| `id` | UUID | Yes | ID của lô sản xuất | `"85d91b0c-c3b8-4c1f-bcb0-2b86737d1406"` |

**Response — Success (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "name": "Lô Bưởi Da Xanh Vụ Thu 2026",
    "farmAreaId": "e2a3b4c5-5555-4a2a-9f3d-1a2b3c4d5e6f",
    "farmAreaName": "Vườn Bưởi Khu A",
    "productCategoryId": "c1a2b3c4-1111-4a2a-9f3d-1a2b3c4d5e6f",
    "productCategoryName": "Bưởi Da Xanh",
    "organizationName": "HTX Nông Nghiệp Sạch Bến Tre",
    "expectedQuantity": 1500.0,
    "expectedQuantityUnit": "kg",
    "actualQuantity": 1450.0,
    "plantingDate": "2026-03-10",
    "harvestDate": "2026-08-20",
    "status": "PACKAGED",
    "approvalNotes": "Đã duyệt kế hoạch thu hoạch",
    "createdByName": "Nguyễn Văn Nông",
    "approvedByName": "Trần Quản Lý",
    "createdAt": "2026-03-10T08:00:00",
    "updatedAt": "2026-08-25T14:30:00",
    "inspectionValidity": {
      "requiresInspection": true,
      "status": "EXPIRING",
      "earliestExpiryDate": "2026-09-20",
      "daysRemaining": 10,
      "daysOverdue": null,
      "canActivate": true,
      "canCreateNewRequest": true,
      "latestPassedRequestId": "d3b07384-d113-49d6-a212-32b704c35b6c",
      "inactiveStampCount": 200
    }
  }
}
```

### 2.3 POST /api/v1/production-lots/check-inspection-expiry
**Description:** Kích hoạt thủ công tiến trình quét kiểm tra hạn kết quả kiểm nghiệm của tất cả các lô sản xuất trên hệ thống và tạo cảnh báo/thông báo nếu thỏa mãn điều kiện. Phục vụ kiểm thử, vận hành và quản trị.

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** Chỉ Quản trị viên nền tảng (`VT-01`).

**Request Example:**
```http
POST /api/v1/production-lots/check-inspection-expiry
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response — Success (200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "totalLotsScanned": 35,
    "expiringAlertsCreated": 2,
    "expiredAlertsCreated": 1,
    "skippedDuplicateToday": 4
  },
  "message": "Quét kiểm tra hiệu lực kết quả kiểm nghiệm hoàn tất."
}
```

**Response — Error:**
| Status Code | Error Code | Nguyên nhân |
|---|---|---|
| 401 Unauthorized | - | Token bị thiếu hoặc hết hạn |
| 403 Forbidden | - | Người dùng không có vai trò `VT-01` |

### 2.4 GET /api/v1/alerts
**Description:** Lấy danh sách cảnh báo trong hệ thống. Hỗ trợ lọc theo loại cảnh báo kiểm nghiệm mới: `INSPECTION_EXPIRING` và `INSPECTION_EXPIRED`.

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** `VT-01` (xem toàn hệ thống), `VT-02` (chỉ xem cảnh báo thuộc tổ chức của mình).

**Query Parameters:**
| Field Name | Data Type | Required | Constraints / Description | Example |
|---|---|---|---|---|
| `type` | String | No | Lọc theo loại cảnh báo: `INSPECTION_EXPIRING`, `INSPECTION_EXPIRED`, `CERT_EXPIRING`, `SCAN_ANOMALY` | `"INSPECTION_EXPIRING"` |
| `status` | String | No | Lọc theo trạng thái xử lý: `PENDING`, `RESOLVED` | `"PENDING"` |
| `page` | int | No | Trang số (mặc định = `0`) | `0` |
| `size` | int | No | Số bản ghi mỗi trang (mặc định = `10`) | `10` |

**Response Example (Success 200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "type": "INSPECTION_EXPIRING",
        "relatedEntityType": "ProductionLot",
        "relatedEntityId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
        "severity": "MEDIUM",
        "status": "PENDING",
        "message": "Lô Bưởi Da Xanh Vụ Thu 2026 có kết quả kiểm nghiệm sắp hết hiệu lực vào ngày 2026-09-20 (còn 10 ngày). Còn 200 tem chưa kích hoạt.",
        "details": {
          "productionLotName": "Lô Bưởi Da Xanh Vụ Thu 2026",
          "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
          "earliestExpiryDate": "2026-09-20",
          "daysRemaining": 10,
          "daysOverdue": null,
          "inactiveStampCount": 200,
          "thresholdConfigured": 15
        },
        "createdAt": "2026-09-10T01:30:00",
        "resolvedAt": null,
        "resolvedBy": null
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

### 2.5 PATCH /api/v1/alerts/{alertId}/resolve
**Description:** Đánh dấu cảnh báo đã được giải quyết (ví dụ sau khi Quản lý HTX đã kích hoạt hết tem hoặc đã tạo vòng kiểm nghiệm mới).

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** `VT-01` (toàn hệ thống), `VT-02` (chỉ giải quyết cảnh báo thuộc tổ chức của mình).

**Request Body:**
```json
{
  "resolutionNote": "Đã tạo yêu cầu kiểm nghiệm mới và gửi mẫu đi kiểm định ngày 10/09/2026."
}
```

**Response Example (Success 200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "status": "RESOLVED",
    "resolvedAt": "2026-09-10T09:15:00",
    "resolvedBy": "7f6e5d4c-3333-4a2a-9f3d-1a2b3c4d5e6f"
  }
}
```

### 2.6 GET /api/v1/notifications
**Description:** Lấy danh sách thông báo của người dùng đang đăng nhập (bao gồm thông báo cảnh báo sắp hết hạn / đã hết hạn kiểm nghiệm).

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** Tất cả người dùng đã đăng nhập.

**Response Example (Success 200 OK):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [
      {
        "id": "c4d5e6f7-8888-4a2a-9f3d-1a2b3c4d5e6f",
        "type": "ALERT",
        "title": "Cảnh báo: Kết quả kiểm nghiệm sắp hết hiệu lực",
        "content": "Lô sản xuất \"Lô Bưởi Da Xanh Vụ Thu 2026\" có kết quả kiểm nghiệm sẽ hết hiệu lực vào ngày 20/09/2026 (còn 10 ngày). Lô hiện còn 200 tem chưa kích hoạt. Vui lòng hoàn tất kích hoạt tem hoặc lập yêu cầu kiểm nghiệm mới trước ngày hết hạn.",
        "isRead": false,
        "readAt": null,
        "createdAt": "2026-09-10T01:30:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10
  }
}
```

### 2.7 POST /api/v1/production-lots/{lotId}/test-requests (Lối tắt Tạo yêu cầu kiểm nghiệm mới)
**Description:** Tái sử dụng API tạo yêu cầu kiểm nghiệm hiện có khi kết quả kiểm nghiệm của lô đã hết hạn (`EXPIRED`). Frontend cung cấp nút lối tắt điều hướng tới `/production-lots/{lotId}/inspection-requests/create`.

**Authentication:** Bearer JWT Token.
**Authorization / RBAC:** `VT-02` (Quản lý hợp tác xã).

**Request Body:**
```json
{
  "testingUnit": "Trung tâm Kỹ thuật Tiêu chuẩn Đo lường Chất lượng 3 (QUATEST 3)",
  "sampleSentDate": "2026-09-10",
  "criteriaIds": [101, 102],
  "confirmDuplicate": false
}
```

**Nguyên tắc bảo toàn lịch sử:**
- Yêu cầu mới được khởi tạo ở trạng thái `PENDING_RESULT` với snapshot chỉ tiêu mới.
- Toàn bộ kết quả kiểm nghiệm cũ (kể cả kết quả đã hết hạn) **không bị ghi đè hay xóa**. Lịch sử dòng thời gian của lô vẫn phản ánh đầy đủ qua `GET /api/v1/production-lots/{lotId}/inspection-history`.

---

## 3. Scheduled Job & Deduplication Contract

### 3.1 Cấu hình Tiến trình Quét
| Thuộc tính | Giá trị mặc định | Mô tả |
|---|---|---|
| `app.inspection.expiry-check-cron` | `0 30 1 * * ?` | Biểu thức Cron chạy lúc 01:30 AM hằng ngày |
| `app.inspection.expiry-warning-threshold-days` | `15` | Ngưỡng cảnh báo trước khi hết hiệu lực (số ngày) |

### 3.2 Quy trình Xử lý của Scheduler
1. **Lấy danh sách lô ứng viên:** Quét các lô sản xuất có `productCategory.requiresInspection == true` và `status NOT IN ('CANCELLED', 'DISPOSED', 'RECALLED', 'CLOSED')`.
2. **Kiểm tra trạng thái thu hồi:** Nếu lô đã thu hồi (`status == RECALLED`) -> Bỏ qua.
3. **Tính toán hiệu lực kiểm nghiệm:** Lấy kết quả kiểm nghiệm mới nhất cho từng chỉ tiêu bắt buộc.
   - Nếu chưa đạt đủ mọi chỉ tiêu -> Bỏ qua (đây là trách nhiệm của luồng kiểm nghiệm chưa hoàn thành / không đạt).
   - Nếu tất cả chỉ tiêu đạt -> Xác định `earliestExpiryDate = MIN(expiryDate)`.
4. **Kiểm tra số tem chưa kích hoạt (`inactiveStampCount`):**
   - Đếm số mã tem thuộc các lô hàng chưa thu hồi có trạng thái `INACTIVE`.
   - Nếu `inactiveStampCount == 0` (lô đã kích hoạt hết tem) -> Bỏ qua, không cảnh báo.
5. **So sánh ngày hiện tại với ngày hết hiệu lực:**
   - Nếu `0 <= daysRemaining <= warningThresholdDays`: Xét loại cảnh báo `INSPECTION_EXPIRING`.
   - Nếu `daysOverdue > 0` (`today > earliestExpiryDate`): Xét loại cảnh báo `INSPECTION_EXPIRED`.
6. **Kiểm tra chống trùng lặp trong ngày (Deduplication Check):**
   - Truy vấn: Đã có `Alert` cho lô này, cùng `type`, được tạo trong khoảng thời gian từ `today 00:00:00` đến `today 23:59:59` hay chưa?
   - Nếu **ĐÃ CÓ** -> Bỏ qua, không tạo Alert mới, không gửi Notification mới.
   - Nếu **CHƯA CÓ**:
     - Lưu bản ghi `Alert` vào bảng `alerts`.
     - Tạo và lưu `Notification` vào bảng `notifications` cho tất cả người dùng có quyền `notification:READ` thuộc tổ chức sở hữu lô.

---

## 4. Ma trận Tương thích & Tính toàn vẹn Dữ liệu

| Thành phần / Story liên quan | Mức độ tương thích | Đánh giá & Ràng buộc thực thi |
|---|---|---|
| **NCL-11-CN-003 (Ghi nhận kết quả kiểm nghiệm)** | 100% Tương thích | Giữ nguyên vẹn bảng `inspection_criterion_result`, không xóa/sửa dữ liệu kết quả cũ. Lịch sử kiểm nghiệm giữ nguyên vẹn. |
| **QTN-21 (Chặn kích hoạt tem khi chưa đủ điều kiện)** | 100% Đồng bộ | Đồng nhất logic tính `canActivate`: khi `expiryDate < today` thì chặn kích hoạt tem. Cảnh báo NCL-11-CN-004 là lớp cảnh báo sớm trước khi QTN-21 có hiệu lực. |
| **QTN-13 (Chỉ hiển thị chứng nhận/kết quả còn hạn)** | 100% Đồng bộ | Kết quả kiểm nghiệm quá hạn hiển thị rõ nhãn `EXPIRED` ("Hết hiệu lực"), không cho phép phát hành tem mới. |
| **NCL-11-CN-005 (QTN-30 — Xử lý lô không đạt)** | 100% Tương thích | Lô ở trạng thái `DISPOSED` được loại trừ khỏi phạm vi quét cảnh báo. |
| **NCL-08-CN-011 (Thu hồi lô hàng)** | 100% Tương thích | Lô ở trạng thái `RECALLED` được loại trừ khỏi phạm vi quét cảnh báo. Tem thuộc lô hàng đã thu hồi không tính vào `inactiveStampCount`. |
| **NCL-08-CN-005 (Hộp thư thông báo)** | 100% Tương thích | Tái sử dụng hoàn toàn hạ tầng `Notification` và phân phối thông báo theo quyền `notification:READ`. |
