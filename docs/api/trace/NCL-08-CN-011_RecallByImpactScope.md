# 📘 API Docs: Thu hồi theo phạm vi ảnh hưởng (NCL-08-CN-011)

## 1. Thông tin chung

| Thuộc tính | Giá trị |
| --- | --- |
| **User Story** | NCL-08-CN-011 - Thu hồi theo phạm vi ảnh hưởng |
| **Epic** | NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động |
| **Git Branch** | `feature/NCL-08-CN-011_recall-by-impact-scope` |
| **Vai trò thực hiện** | VT-02 - Quản lý hợp tác xã (và VT-01 Admin) |
| **Endpoint chính** | `POST /api/v1/recall-requests/bulk` |
| **Endpoint phụ** | `GET /api/v1/recall-requests/bulk/{id}` |
| **Endpoint phụ** | `PUT /api/v1/recall-requests/bulk/{id}/approve` |
| **Endpoint phụ** | `PUT /api/v1/recall-requests/bulk/{id}/reject` |
| **Endpoint phụ** | `GET /api/v1/recall-requests/bulk` |
| **Phương thức** | `POST`, `GET`, `PUT` |
| **Bảo mật** | Yêu cầu JWT token (ACCESS). Phân quyền qua `PermissionChecker` (RBAC trong DB): `recall:CREATE` (tạo), `recall:READ` (tra cứu), `recall:UPDATE` (phê duyệt/từ chối). Vai trò mặc định được cấp: VT-02 Quản lý HTX, VT-01 Admin (xem `V20260909090000__seed_recall_update_permission.sql`) |
| **Phụ thuộc** | NCL-08-CN-010 (Truy vết phạm vi ảnh hưởng) |

---

## 2. Mô tả nghiệp vụ & Quy tắc (Business Rules)

User Story **NCL-08-CN-011** cho phép Quản lý hợp tác xã (VT-02) tạo yêu cầu thu hồi **nhiều lô hàng** cùng lúc dựa trên kết quả truy vết phạm vi ảnh hưởng từ NCL-08-CN-010. Story này mở rộng quy trình thu hồi hiện có (NCL-08-CN-008) để hỗ trợ thu hồi hàng loạt theo phạm vi ảnh hưởng của một lô sản xuất.

### Quy tắc nghiệp vụ chi tiết:

1. **Thu hồi theo phạm vi ảnh hưởng (QTN-24):**
   - Khi thu hồi vì nguyên nhân bắt nguồn từ lô sản xuất, hệ thống phải xét toàn bộ lô hàng sinh từ lô sản xuất đó.
   - Người quản lý chọn phạm vi từ kết quả truy vết (NCL-08-CN-010).
   - Có thể bỏ chọn từng lô cụ thể.
   - Mỗi lô bị loại khỏi phạm vi phải có lý do.
   - Phải có lý do chung cho vụ việc thu hồi.

2. **Loại bỏ lô thuộc organization khác:**
   - Không được đưa lô thuộc organization khác vào phạm vi thu hồi.
   - Backend phải validate mỗi shipment thuộc organization của người dùng hiện tại.

3. **Loại bỏ lô đã RECALLED:**
   - Lô đã thu hồi trước đó (status = RECALLED) phải bị loại khỏi phạm vi.
   - Phải ghi rõ lý do loại bỏ.

4. **Validation phạm vi cuối cùng:**
   - Nếu phạm vi cuối cùng rỗng (bỏ chọn toàn bộ) → Không được tạo yêu cầu.
   - Nếu có lô bị loại nhưng không có lý do → Không được tạo yêu cầu.

5. **Mã vụ việc thu hồi:**
   - Mỗi vụ việc thu hồi có mã riêng (UUID).
   - Một vụ việc có thể chứa nhiều lô hàng.

6. **Trạng thái yêu cầu:**
   - Yêu cầu ban đầu ở trạng thái `PENDING` (chờ phê duyệt).
   - Sau khi được phê duyệt: `APPROVED`.
   - Sau khi bị từ chối: `REJECTED`.

7. **Người tạo KHÔNG được tự phê duyệt (QTN-22):**
   - Người tạo yêu cầu không được phép phê duyệt chính yêu cầu của mình.

8. **Tác động khi phê duyệt:**
   - Các lô trong phạm vi chuyển sang trạng thái `RECALLED`.
   - Bật cảnh báo công khai cho từng mã tem (TraceCode) thuộc các lô.
   - Gửi notification đến các doanh nghiệp thu mua đã nhận lô.
   - Ghi nhận lịch sử/audit tương ứng.

---

## 3. Chi tiết API Endpoints

### 3.1. API Tạo yêu cầu thu hồi theo phạm vi (Create Bulk Recall Request)

```http
POST /api/v1/recall-requests/bulk
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Mô tả:
Tạo yêu cầu thu hồi nhiều lô hàng cùng lúc dựa trên phạm vi ảnh hưởng đã truy vết. API này mở rộng từ `POST /api/v1/recall-requests` (NCL-08-CN-008) để hỗ trợ thu hồi hàng loạt.

#### Request Body:

```json
{
  "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
  "reason": "Phát hiện dấu hiệu nhiễm khuẩn trên lô dâu tây vụ Mùa 2026",
  "evidence": "Biên bản kiểm tra số 2026-KT-001",
  "includedShipments": [
    {
      "shipmentId": "c3g0a456-13de-6g34-003c-333333333333"
    },
    {
      "shipmentId": "d4h1b567-24ef-7h45-114d-444444444444"
    }
  ],
  "excludedShipments": [
    {
      "shipmentId": "e5i2c678-35fg-8i56-225e-555555555555",
      "exclusionReason": "Lô hàng đã được thu hồi trước đó (RECALLED)"
    }
  ]
}
```

#### Request Schema:

| Field | Type | Required | Description | Validation |
| --- | --- | --- | --- | --- |
| `productionLotId` | UUID | Có | ID lô sản xuất nguồn | Phải tồn tại và thuộc tổ chức hiện tại |
| `reason` | String | Có | Lý do chung cho vụ việc thu hồi | Không rỗng, tối đa 1000 ký tự |
| `evidence` | String | Không | Bằng chứng kèm theo | Tối đa 2000 ký tự |
| `includedShipments` | Array | Có | Danh sách lô hàng cần thu hồi | Phải có ít nhất 1 phần tử sau khi loại bỏ |
| `includedShipments[].shipmentId` | UUID | Có | ID lô hàng | Phải thuộc productionLotId và chưa RECALLED |
| `excludedShipments` | Array | Không | Danh sách lô hàng bị loại khỏi phạm vi | Có thể rỗng |
| `excludedShipments[].shipmentId` | UUID | Có | ID lô hàng bị loại | Phải thuộc productionLotId |
| `excludedShipments[].exclusionReason` | String | Có | Lý do loại bỏ | Không rỗng, tối đa 500 ký tự |

#### Validation Rules:

| Rule | Error Code | Error Message |
| --- | --- | --- |
| `productionLotId` không tồn tại | 404 | `"Không tìm thấy lô sản xuất."` |
| `productionLotId` không thuộc tổ chức hiện tại | 403 | `"Bạn không có quyền thao tác trên lô sản xuất của tổ chức khác."` |
| `reason` rỗng | 400 | `"Lý do thu hồi không được để trống."` |
| `includedShipments` rỗng hoặc null | 400 | `"Phải chọn ít nhất một lô hàng để thu hồi."` |
| `includedShipments` chứa shipmentId không thuộc productionLotId | 400 | `"Lô hàng {shipmentId} không thuộc lô sản xuất đã chọn."` |
| `includedShipments` chứa shipmentId đã RECALLED | 400 | `"Lô hàng {shipmentId} đã được thu hồi trước đó. Vui lòng loại bỏ khỏi phạm vi."` |
| `includedShipments` chứa shipmentId không thuộc tổ chức hiện tại | 403 | `"Lô hàng {shipmentId} không thuộc tổ chức của bạn."` |
| `excludedShipments` có phần tử không có `exclusionReason` | 400 | `"Lô hàng bị loại {shipmentId} phải có lý do loại bỏ."` |
| Sau khi loại bỉ, phạm vi rỗng | 400 | `"Phạm vi thu hồi không được rỗng. Vui lòng chọn ít nhất một lô hàng."` |
| Đã có yêu cầu PENDING cho cùng productionLotId | 409 | `"Đã có yêu cầu thu hồi đang chờ duyệt cho lô sản xuất này."` |

#### Responses:

##### Success: `201 Created`
```json
{
  "success": true,
  "status": 201,
  "message": "Tạo yêu cầu thu hồi theo phạm vi thành công.",
  "data": {
    "id": "f6j3d789-46gh-9j67-336f-666666666666",
    "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
    "productionLotCode": "LOT-2026-001",
    "productionLotName": "Lô dâu tây vụ Mùa 2026",
    "recallCode": "RC-2026-001",
    "reason": "Phát hiện dấu hiệu nhiễm khuẩn trên lô dâu tây vụ Mùa 2026",
    "evidence": "Biên bản kiểm tra số 2026-KT-001",
    "status": "PENDING",
    "totalShipments": 3,
    "includedShipments": [
      {
        "shipmentId": "c3g0a456-13de-6g34-003c-333333333333",
        "shipmentCode": "SHIP-8821",
        "shipmentName": "Lô hàng dâu tây siêu thị A",
        "status": "ACTIVATED",
        "totalQuantity": 1500,
        "excluded": false,
        "exclusionReason": null
      },
      {
        "shipmentId": "d4h1b567-24ef-7h45-114d-444444444444",
        "shipmentCode": "SHIP-8822",
        "shipmentName": "Lô hàng dâu tây siêu thị B",
        "status": "ACTIVATED",
        "totalQuantity": 2000,
        "excluded": false,
        "exclusionReason": null
      }
    ],
    "excludedShipments": [
      {
        "shipmentId": "e5i2c678-35fg-8i56-225e-555555555555",
        "shipmentCode": "SHIP-8820",
        "shipmentName": "Lô hàng dâu tây đã thu hồi trước đó",
        "status": "RECALLED",
        "totalQuantity": 1000,
        "excluded": true,
        "exclusionReason": "Lô hàng đã được thu hồi trước đó (RECALLED)"
      }
    ],
    "requestedBy": {
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "fullName": "Nguyễn Văn An"
    },
    "requestedAt": "2026-09-08T10:30:00",
    "approvedBy": null,
    "approvedAt": null,
    "approvalRemarks": null,
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedBuyerCount": 0,
    "createdAt": "2026-09-08T10:30:00",
    "updatedAt": "2026-09-08T10:30:00"
  },
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

##### Error: `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ.",
  "errors": [
    {
      "field": "reason",
      "message": "Lý do thu hồi không được để trống."
    }
  ],
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

##### Error: `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thao tác trên lô sản xuất của tổ chức khác.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

##### Error: `404 Not Found`
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy lô sản xuất.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

##### Error: `409 Conflict`
```json
{
  "success": false,
  "status": 409,
  "message": "Đã có yêu cầu thu hồi đang chờ duyệt cho lô sản xuất này.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

---

### 3.2. API Xem chi tiết yêu cầu thu hồi theo phạm vi (Get Bulk Recall Request Detail)

```http
GET /api/v1/recall-requests/bulk/{id}
Authorization: Bearer <JWT_TOKEN>
```

#### Mô tả:
Lấy chi tiết một yêu cầu thu hồi theo phạm vi ảnh hưởng. Trả về thông tin đầy đủ bao gồm danh sách lô hàng đã chọn và lô hàng bị loại.

#### Path Parameters:

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | UUID | Có | ID của yêu cầu thu hồi |

#### Responses:

##### Success: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy chi tiết yêu cầu thu hồi thành công.",
  "data": {
    "id": "f6j3d789-46gh-9j67-336f-666666666666",
    "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
    "productionLotCode": "LOT-2026-001",
    "productionLotName": "Lô dâu tây vụ Mùa 2026",
    "recallCode": "RC-2026-001",
    "reason": "Phát hiện dấu hiệu nhiễm khuẩn trên lô dâu tây vụ Mùa 2026",
    "evidence": "Biên bản kiểm tra số 2026-KT-001",
    "status": "APPROVED",
    "totalShipments": 3,
    "includedShipments": [
      {
        "shipmentId": "c3g0a456-13de-6g34-003c-333333333333",
        "shipmentCode": "SHIP-8821",
        "shipmentName": "Lô hàng dâu tây siêu thị A",
        "status": "RECALLED",
        "totalQuantity": 1500,
        "excluded": false,
        "exclusionReason": null,
        "recalledAt": "2026-09-08T11:00:00",
        "buyerOrganizations": [
          {
            "organizationId": "org-001",
            "organizationName": "Siêu thị A",
            "receivedAt": "2026-08-20T08:00:00"
          }
        ]
      },
      {
        "shipmentId": "d4h1b567-24ef-7h45-114d-444444444444",
        "shipmentCode": "SHIP-8822",
        "shipmentName": "Lô hàng dâu tây siêu thị B",
        "status": "RECALLED",
        "totalQuantity": 2000,
        "excluded": false,
        "exclusionReason": null,
        "recalledAt": "2026-09-08T11:00:00",
        "buyerOrganizations": [
          {
            "organizationId": "org-002",
            "organizationName": "Siêu thị B",
            "receivedAt": "2026-08-22T09:00:00"
          }
        ]
      }
    ],
    "excludedShipments": [
      {
        "shipmentId": "e5i2c678-35fg-8i56-225e-555555555555",
        "shipmentCode": "SHIP-8820",
        "shipmentName": "Lô hàng dâu tây đã thu hồi trước đó",
        "status": "RECALLED",
        "totalQuantity": 1000,
        "excluded": true,
        "exclusionReason": "Lô hàng đã được thu hồi trước đó (RECALLED)"
      }
    ],
    "requestedBy": {
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "fullName": "Nguyễn Văn An"
    },
    "requestedAt": "2026-09-08T10:30:00",
    "approvedBy": {
      "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "fullName": "Trần Thị Bình"
    },
    "approvedAt": "2026-09-08T11:00:00",
    "approvalRemarks": "Đã xác minh phạm vi ảnh hưởng, phê duyệt thu hồi.",
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedBuyerCount": 5,
    "createdAt": "2026-09-08T10:30:00",
    "updatedAt": "2026-09-08T11:00:00"
  },
  "timestamp": "2026-09-08T11:05:00.000Z"
}
```

##### Error: `404 Not Found`
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy yêu cầu thu hồi.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

##### Error: `403 Forbidden`
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xem yêu cầu thu hồi của tổ chức khác.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

---

### 3.3. API Phê duyệt yêu cầu thu hồi theo phạm vi (Approve Bulk Recall Request)

```http
PUT /api/v1/recall-requests/bulk/{id}/approve
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Mô tả:
Phê duyệt yêu cầu thu hồi theo phạm vi ảnh hưởng. Khi phê duyệt:
- Tất cả lô hàng trong `includedShipments` chuyển sang trạng thái `RECALLED`.
- Toàn bộ TraceCode thuộc các lô hàng chuyển sang `RECALLED`.
- Cảnh báo công khai được bật cho từng mã tem.
- Notification được gửi đến các doanh nghiệp thu mua.
- AuditLog được ghi nhận.

#### Path Parameters:

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | UUID | Có | ID của yêu cầu thu hồi |

#### Request Body:

```json
{
  "remarks": "Đã xác minh phạm vi ảnh hưởng, phê duyệt thu hồi."
}
```

#### Request Schema:

| Field | Type | Required | Description | Validation |
| --- | --- | --- | --- | --- |
| `remarks` | String | Không | Ghi chú khi phê duyệt | Tối đa 1000 ký tự |

#### Validation Rules:

| Rule | Error Code | Error Message |
| --- | --- | --- |
| `id` không tồn tại | 404 | `"Không tìm thấy yêu cầu thu hồi."` |
| `id` không thuộc tổ chức hiện tại | 403 | `"Bạn không có quyền thao tác trên yêu cầu của tổ chức khác."` |
| Người dùng chưa được cấp permission `recall:UPDATE` | 403 | `"Bạn không có quyền thực hiện chức năng này."` |
| Yêu cầu không ở trạng thái PENDING | 409 | `"Chỉ có thể phê duyệt yêu cầu ở trạng thái PENDING."` |
| Người phê duyệt trùng người tạo | 400 | `"Bạn không thể phê duyệt yêu cầu do chính mình tạo."` |
| Một trong các lô hàng đã RECALLED sau khi tạo yêu cầu | 409 | `"Lô hàng {shipmentId} đã bị thu hồi bởi một yêu cầu khác. Vui lòng cập nhật yêu cầu."` |

#### Responses:

##### Success: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Phê duyệt yêu cầu thu hồi thành công. 2 lô hàng đã chuyển sang trạng thái RECALLED.",
  "data": {
    "id": "f6j3d789-46gh-9j67-336f-666666666666",
    "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
    "productionLotCode": "LOT-2026-001",
    "productionLotName": "Lô dâu tây vụ Mùa 2026",
    "recallCode": "RC-2026-001",
    "reason": "Phát hiện dấu hiệu nhiễm khuẩn trên lô dâu tây vụ Mùa 2026",
    "evidence": "Biên bản kiểm tra số 2026-KT-001",
    "status": "APPROVED",
    "totalShipments": 3,
    "includedShipments": [
      {
        "shipmentId": "c3g0a456-13de-6g34-003c-333333333333",
        "shipmentCode": "SHIP-8821",
        "shipmentName": "Lô hàng dâu tây siêu thị A",
        "status": "RECALLED",
        "totalQuantity": 1500,
        "excluded": false,
        "exclusionReason": null,
        "recalledAt": "2026-09-08T11:00:00"
      },
      {
        "shipmentId": "d4h1b567-24ef-7h45-114d-444444444444",
        "shipmentCode": "SHIP-8822",
        "shipmentName": "Lô hàng dâu tây siêu thị B",
        "status": "RECALLED",
        "totalQuantity": 2000,
        "excluded": false,
        "exclusionReason": null,
        "recalledAt": "2026-09-08T11:00:00"
      }
    ],
    "excludedShipments": [
      {
        "shipmentId": "e5i2c678-35fg-8i56-225e-555555555555",
        "shipmentCode": "SHIP-8820",
        "shipmentName": "Lô hàng dâu tây đã thu hồi trước đó",
        "status": "RECALLED",
        "totalQuantity": 1000,
        "excluded": true,
        "exclusionReason": "Lô hàng đã được thu hồi trước đó (RECALLED)"
      }
    ],
    "requestedBy": {
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "fullName": "Nguyễn Văn An"
    },
    "requestedAt": "2026-09-08T10:30:00",
    "approvedBy": {
      "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "fullName": "Trần Thị Bình"
    },
    "approvedAt": "2026-09-08T11:00:00",
    "approvalRemarks": "Đã xác minh phạm vi ảnh hưởng, phê duyệt thu hồi.",
    "rejectedBy": null,
    "rejectedAt": null,
    "rejectionReason": null,
    "notifiedBuyerCount": 5,
    "createdAt": "2026-09-08T10:30:00",
    "updatedAt": "2026-09-08T11:00:00"
  },
  "timestamp": "2026-09-08T11:00:00.000Z"
}
```

##### Error: `400 Bad Request` (Tự phê duyệt - QTN-22)
```json
{
  "success": false,
  "status": 400,
  "message": "Bạn không thể phê duyệt yêu cầu do chính mình tạo (QTN-22).",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

##### Error: `409 Conflict` (Sai trạng thái)
```json
{
  "success": false,
  "status": 409,
  "message": "Chỉ có thể phê duyệt yêu cầu ở trạng thái PENDING.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

---

### 3.4. API Từ chối yêu cầu thu hồi theo phạm vi (Reject Bulk Recall Request)

```http
PUT /api/v1/recall-requests/bulk/{id}/reject
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Mô tả:
Từ chối yêu cầu thu hồi theo phạm vi ảnh hưởng. Yêu cầu phải có lý do từ chối.

#### Path Parameters:

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `id` | UUID | Có | ID của yêu cầu thu hồi |

#### Request Body:

```json
{
  "rejectionReason": "Chưa đủ bằng chứng xác thực. Vui lòng bổ sung kết quả kiểm nghiệm."
}
```

#### Request Schema:

| Field | Type | Required | Description | Validation |
| --- | --- | --- | --- | --- |
| `rejectionReason` | String | Có | Lý do từ chối | Không rỗng, tối đa 1000 ký tự |

#### Validation Rules:

| Rule | Error Code | Error Message |
| --- | --- | --- |
| `id` không tồn tại | 404 | `"Không tìm thấy yêu cầu thu hồi."` |
| `id` không thuộc tổ chức hiện tại | 403 | `"Bạn không có quyền thao tác trên yêu cầu của tổ chức khác."` |
| Yêu cầu không ở trạng thái PENDING | 409 | `"Chỉ có thể từ chối yêu cầu ở trạng thái PENDING."` |
| `rejectionReason` rỗng | 400 | `"Lý do từ chối không được để trống."` |

#### Responses:

##### Success: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Từ chối yêu cầu thu hồi thành công.",
  "data": {
    "id": "f6j3d789-46gh-9j67-336f-666666666666",
    "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
    "productionLotCode": "LOT-2026-001",
    "productionLotName": "Lô dâu tây vụ Mùa 2026",
    "recallCode": "RC-2026-001",
    "reason": "Phát hiện dấu hiệu nhiễm khuẩn trên lô dâu tây vụ Mùa 2026",
    "evidence": "Biên bản kiểm tra số 2026-KT-001",
    "status": "REJECTED",
    "totalShipments": 3,
    "includedShipments": [
      {
        "shipmentId": "c3g0a456-13de-6g34-003c-333333333333",
        "shipmentCode": "SHIP-8821",
        "shipmentName": "Lô hàng dâu tây siêu thị A",
        "status": "ACTIVATED",
        "totalQuantity": 1500,
        "excluded": false,
        "exclusionReason": null
      },
      {
        "shipmentId": "d4h1b567-24ef-7h45-114d-444444444444",
        "shipmentCode": "SHIP-8822",
        "shipmentName": "Lô hàng dâu tây siêu thị B",
        "status": "ACTIVATED",
        "totalQuantity": 2000,
        "excluded": false,
        "exclusionReason": null
      }
    ],
    "excludedShipments": [
      {
        "shipmentId": "e5i2c678-35fg-8i56-225e-555555555555",
        "shipmentCode": "SHIP-8820",
        "shipmentName": "Lô hàng dâu tây đã thu hồi trước đó",
        "status": "RECALLED",
        "totalQuantity": 1000,
        "excluded": true,
        "exclusionReason": "Lô hàng đã được thu hồi trước đó (RECALLED)"
      }
    ],
    "requestedBy": {
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "fullName": "Nguyễn Văn An"
    },
    "requestedAt": "2026-09-08T10:30:00",
    "approvedBy": null,
    "approvedAt": null,
    "approvalRemarks": null,
    "rejectedBy": {
      "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "fullName": "Trần Thị Bình"
    },
    "rejectedAt": "2026-09-08T11:30:00",
    "rejectionReason": "Chưa đủ bằng chứng xác thực. Vui lòng bổ sung kết quả kiểm nghiệm.",
    "notifiedBuyerCount": 0,
    "createdAt": "2026-09-08T10:30:00",
    "updatedAt": "2026-09-08T11:30:00"
  },
  "timestamp": "2026-09-08T11:30:00.000Z"
}
```

##### Error: `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Lý do từ chối không được để trống.",
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

---

### 3.5. API Danh sách yêu cầu thu hồi theo phạm vi (List Bulk Recall Requests)

```http
GET /api/v1/recall-requests/bulk?status=PENDING&page=0&size=20
Authorization: Bearer <JWT_TOKEN>
```

#### Mô tả:
Lấy danh sách yêu cầu thu hồi theo phạm vi ảnh hưởng với phân trang. Hỗ trợ lọc theo trạng thái.

#### Query Parameters:

| Parameter | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `status` | String | Không | | Lọc theo trạng thái: `PENDING`, `APPROVED`, `REJECTED` |
| `page` | Integer | Không | 0 | Số trang (bắt đầu từ 0) |
| `size` | Integer | Không | 20 | Số phần tử mỗi trang |

#### Responses:

##### Success: `200 OK`
```json
{
  "success": true,
  "status": 200,
  "message": "Lấy danh sách yêu cầu thu hồi thành công.",
  "data": {
    "content": [
      {
        "id": "f6j3d789-46gh-9j67-336f-666666666666",
        "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
        "productionLotCode": "LOT-2026-001",
        "productionLotName": "Lô dâu tây vụ Mùa 2026",
        "recallCode": "RC-2026-001",
        "reason": "Phát hiện dấu hiệu nhiễm khuẩn trên lô dâu tây vụ Mùa 2026",
        "status": "PENDING",
        "totalShipments": 3,
        "requestedBy": {
          "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "fullName": "Nguyễn Văn An"
        },
        "requestedAt": "2026-09-08T10:30:00",
        "approvedBy": null,
        "approvedAt": null,
        "rejectedBy": null,
        "rejectedAt": null,
        "notifiedBuyerCount": 0
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": true,
        "unsorted": false
      }
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "first": true,
    "numberOfElements": 1,
    "size": 20,
    "number": 0
  },
  "timestamp": "2026-09-08T10:30:00.000Z"
}
```

---

## 4. Response DTO Schema

### 4.1. BulkRecallRequestResponse

| Field | Type | Description |
| --- | --- | --- |
| `id` | UUID | ID yêu cầu thu hồi |
| `productionLotId` | UUID | ID lô sản xuất nguồn |
| `productionLotCode` | String | Mã lô sản xuất |
| `productionLotName` | String | Tên lô sản xuất |
| `recallCode` | String | Mã vụ việc thu hồi (VD: RC-2026-001) |
| `reason` | String | Lý do thu hồi |
| `evidence` | String | Bằng chứng |
| `status` | String | Trạng thái: PENDING, APPROVED, REJECTED |
| `totalShipments` | Integer | Tổng số lô hàng trong phạm vi truy vết |
| `includedShipments` | Array | Danh sách lô hàng cần thu hồi |
| `excludedShipments` | Array | Danh sách lô hàng bị loại khỏi phạm vi |
| `requestedBy` | UserInfo | Người tạo yêu cầu |
| `requestedAt` | DateTime | Thời điểm tạo |
| `approvedBy` | UserInfo | Người phê duyệt |
| `approvedAt` | DateTime | Thời điểm phê duyệt |
| `approvalRemarks` | String | Ghi chú phê duyệt |
| `rejectedBy` | UserInfo | Người từ chối |
| `rejectedAt` | DateTime | Thời điểm từ chối |
| `rejectionReason` | String | Lý do từ chối |
| `notifiedBuyerCount` | Integer | Số lượng doanh nghiệp thu mua đã nhận notification |
| `createdAt` | DateTime | Thời điểm tạo bản ghi |
| `updatedAt` | DateTime | Thời điểm cập nhật cuối |

### 4.2. BulkRecallShipmentItem

| Field | Type | Description |
| --- | --- | --- |
| `shipmentId` | UUID | ID lô hàng |
| `shipmentCode` | String | Mã lô hàng |
| `shipmentName` | String | Tên lô hàng |
| `status` | String | Trạng thái lô hàng (DRAFT, ACTIVATED, RECALLED) |
| `totalQuantity` | Long | Tổng số lượng |
| `excluded` | Boolean | true nếu bị loại khỏi phạm vi |
| `exclusionReason` | String | Lý do loại bỏ (nếu excluded = true) |
| `recalledAt` | DateTime | Thời điểm bị thu hồi (chỉ có khi status = RECALLED) |
| `buyerOrganizations` | Array | Danh sách tổ chức thu mua (chỉ trả về khi xem chi tiết) |

### 4.3. BuyerOrganizationInfo

| Field | Type | Description |
| --- | --- | --- |
| `organizationId` | UUID | ID tổ chức thu mua |
| `organizationName` | String | Tên tổ chức thu mua |
| `receivedAt` | DateTime | Thời điểm nhận hàng |

---

## 5. Authorization Matrix

| API Endpoint | VT-01 (Admin) | VT-02 (Quản lý HTX) | VT-03 (Người ghi sự kiện) | VT-04 (Người tra cứu) |
| --- | --- | --- | --- | --- |
| `POST /api/v1/recall-requests/bulk` | ✅ | ✅ | ❌ | ❌ |
| `GET /api/v1/recall-requests/bulk` | ✅ | ✅ | ❌ | ❌ |
| `GET /api/v1/recall-requests/bulk/{id}` | ✅ | ✅ | ❌ | ❌ |
| `PUT /api/v1/recall-requests/bulk/{id}/approve` | ✅ | ✅ (khác người tạo) | ❌ | ❌ |
| `PUT /api/v1/recall-requests/bulk/{id}/reject` | ✅ | ✅ | ❌ | ❌ |

### Quyền chi tiết:

1. **VT-01 (Admin):** Có thể xem và xử lý tất cả yêu cầu thu hồi trên toàn hệ thống.
2. **VT-02 (Quản lý HTX):** Có thể tạo, xem, phê duyệt, từ chối yêu cầu thu hồi **chỉ thuộc tổ chức của mình**. Không được tự phê duyệt yêu cầu do mình tạo.
3. **VT-03, VT-04:** Không có quyền truy cập.

---

## 6. Transaction Boundary

### 6.1. Khi tạo yêu cầu (POST):
- **Transaction:** READ_ONLY cho việc validate
- **Không có ghi dữ liệu** ngoài việc tạo bản ghi `BulkRecallRequest` và `BulkRecallShipment`

### 6.2. Khi phê duyệt (PUT /approve):
- **Transaction:** READ_WRITE - Toàn bộ thao tác phải thành công hoặc rollback
- **Các thao tác trong transaction:**
  1. Cập nhật trạng thái `BulkRecallRequest` → APPROVED
  2. Cập nhật từng `Shipment` trong `includedShipments` → RECALLED
  3. Cập nhật toàn bộ `TraceCode` thuộc từng Shipment → RECALLED
  4. Tạo bản ghi `Recall` cho từng Shipment
  5. Gửi Notification đến các doanh nghiệp thu mua
  6. Ghi `AuditLog` cho toàn bộ vụ việc

### 6.3. Khi từ chối (PUT /reject):
- **Transaction:** READ_WRITE
- **Các thao tác trong transaction:**
  1. Cập nhật trạng thái `BulkRecallRequest` → REJECTED
  2. Ghi `AuditLog`

---

## 7. Idempotency & Concurrency

### 7.1. Idempotency:
- **POST:** Không idempotent. Mỗi lần gọi tạo một yêu cầu mới. Sử dụng unique constraint trên `(productionLotId, status=PENDING)` để ngăn tạo trùng.
- **PUT /approve:** Idempotent. Nếu yêu cầu đã APPROVED, trả về 409.
- **PUT /reject:** Idempotent. Nếu yêu cầu đã REJECTED, trả về 409.

### 7.2. Concurrency:
- Sử dụng **pessimistic lock** khi đọc `BulkRecallRequest` trước khi approve/reject.
- Sử dụng **pessimistic lock** khi đọc từng `Shipment` trước khi cập nhật status → RECALLED.
- Database unique constraint ngăn tạo 2 yêu cầu PENDING cho cùng `productionLotId`.

---

## 8. Notification Behavior

### 8.1. Khi phê duyệt yêu cầu:

| Đối tượng nhận | Loại thông báo | Nội dung |
| --- | --- | --- |
| Doanh nghiệp thu mua (từng shipment) | `RECALL_NOTIFICATION` | "Lô hàng {shipmentName} thuộc {productionLotName} đã bị thu hồi. Lý do: {reason}" |
| Quản lý HTX (cùng tổ chức) | `RECALL_APPROVED_NOTIFICATION` | "Yêu cầu thu hồi {recallCode} đã được phê duyệt. {count} lô hàng đã chuyển sang RECALLED." |

### 8.2. Quy tắc gửi notification:
- **Không gửi duplicate:** Nếu một doanh nghiệp thu mua nhận nhiều shipment trong cùng vụ việc, chỉ gửi **một notification** duy nhất liệt kê tất cả shipment.
- **Người tạo yêu cầu:** Luôn nhận notification khi yêu cầu được phê duyệt/từ chối.
- **Thời điểm gửi:** Sau khi transaction commit thành công (sử dụng `@TransactionalEventListener`).

### 8.3. Lấy danh sách doanh nghiệp thu mua:
- Từ `chain_events.recorded_organization_id` của sự kiện `PROCUREMENT` trên từng shipment.
- Không suy ngược từ membership hiện tại của người ghi (tránh gửi nhầm khi tài khoản thuộc nhiều tổ chức).

---

## 9. Audit Behavior

### 9.1. Audit Events:

| Hành động | Action | Resource | Mô tả |
| --- | --- | --- | --- |
| Tạo yêu cầu | `CREATE_BULK_RECALL_REQUEST` | `bulk_recall_request` | Ghi nhận người tạo, thời điểm, phạm vi |
| Phê duyệt | `APPROVE_BULK_RECALL_REQUEST` | `bulk_recall_request` | Ghi nhận người phê duyệt, thời điểm, danh sách shipment bị thu hồi |
| Từ chối | `REJECT_BULK_RECALL_REQUEST` | `bulk_recall_request` | Ghi nhận người từ chối, lý do |
| Thu hồi từng shipment | `RECALL_SHIPMENT` | `shipment` | Ghi nhận chi tiết từng shipment bị thu hồi |

### 9.2. Audit Log Schema:

```json
{
  "action": "APPROVE_BULK_RECALL_REQUEST",
  "resource": "bulk_recall_request",
  "resourceId": "f6j3d789-46gh-9j67-336f-666666666666",
  "userId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "organizationId": "org-001",
  "timestamp": "2026-09-08T11:00:00",
  "details": {
    "recallCode": "RC-2026-001",
    "productionLotId": "b2f9f345-02cd-5f23-992b-222222222222",
    "includedShipmentIds": ["c3g0a456-...", "d4h1b567-..."],
    "excludedShipmentIds": ["e5i2c678-..."],
    "remarks": "Đã xác minh phạm vi ảnh hưởng, phê duyệt thu hồi."
  }
}
```

---

## 10. RECALLED State Transition

### 10.1. Sơ đồ chuyển trạng thái Shipment:

```
ACTIVATED/CODE_PRINTED/DRAFT
         │
         │ (Khi BulkRecallRequest được APPROVED)
         ▼
      RECALLED
         │
         │ (Không có chuyểi ngược - QTN-09)
         ▼
      (Kết thúc)
```

### 10.2. Quy tắc chuyển trạng thái:
- **RECALLED là trạng thái cuối:** Không có cơ chế "un-recall" hoặc "khôi phục" (QTN-09).
- **Không cho phép chuyểi ngược:** Một khi đã RECALLED, không thể chuyển về ACTIVATED hoặc trạng thái trước đó.
- **Cảnh báo công khai:** Khi Shipment ở trạng thái RECALLED, toàn bộ TraceCode liên quan phải hiển thị cảnh báo thu hồi trên trang tra cứu công khai.

---

## 11. Impact on Existing APIs

### 11.1. APIs không thay đổi:

| API | Mô tả | Lý do không thay đổi |
| --- | --- | --- |
| `GET /api/v1/trace/impact-scope` | Truy vết phạm vi ảnh hưởng (NCL-08-CN-010) | API này đã đáp ứng đủ thông tin cần thiết |
| `POST /api/v1/recall-requests` | Tạy yêu cầu thu hồi đơn lẻ (NCL-08-CN-008) | Vẫn hỗ trợ thu hồi một shipment |
| `GET /api/v1/recall-requests` | Danh sách yêu cầu thu hồi đơn lẻ | Vẫn hoạt động độc lập |
| `PUT /api/v1/recall-requests/{id}/approve` | Phê duyệt đơn lẻ | Vẫn hoạt động độc lập |
| `PUT /api/v1/recall-requests/{id}/reject` | Từ chối đơn lẻ | Vẫn hoạt động độc lập |

### 11.2. APIs mới (NCL-08-CN-011):

| API | Mô tả |
| --- | --- |
| `POST /api/v1/recall-requests/bulk` | Tạo yêu cầu thu hồi theo phạm vi |
| `GET /api/v1/recall-requests/bulk` | Danh sách yêu cầu thu hồi theo phạm vi |
| `GET /api/v1/recall-requests/bulk/{id}` | Chi tiết yêu cầu thu hồi theo phạm vi |
| `PUT /api/v1/recall-requests/bulk/{id}/approve` | Phê duyệt thu hồi theo phạm vi |
| `PUT /api/v1/recall-requests/bulk/{id}/reject` | Từ chối thu hồi theo phạm vi |

### 11.3. Entity mới cần tạo:

| Entity | Table | Mô tả |
| --- | --- | --- |
| `BulkRecallRequest` | `bulk_recall_requests` | Yêu cầu thu hồi theo phạm vi |
| `BulkRecallShipment` | `bulk_recall_shipments` | Chi tiết lô hàng trong yêu cầu (bao gồm cả excluded) |

---

## 12. Bảng Ma trận Kiểm thử & Đáp ứng Tiêu chí (Test Case Mapping)

| Mã Test Case | Loại kịch bản | Yêu cầu AC | Đáp ứng trong API Docs | Status Code |
| :--- | :--- | :--- | :--- | :--- |
| **NCL-08-CN-011-TC-01** | Luồng thành công | Kết quả truy vết có 3 lô. Quản lý tạo yêu cầu cho cả 3. Một quản lý khác phê duyệt. Cả 3 lô chuyển sang RECALLED và cảnh báo công khai được bật. | `POST /bulk` → `PUT /bulk/{id}/approve` → Kiểm tra Shipment.status = RECALLED | `201` → `200` |
| **NCL-08-CN-011-TC-02** | Tự phê duyệt | Người tạo yêu cầu tự bấm phê duyệt. API phải từ chối. | `PUT /bulk/{id}/approve` → Trả về 400 với thông báo QTN-22 | `400 Bad Request` |
| **NCL-08-CN-011-TC-03** | Lô đã RECALLED | Một lô trong phạm vi đã RECALLED trước đó. Lô này bị loại khỏi phạm vi và phải có lý do. | `POST /bulk` với `excludedShipments` chứa lý do | `201 Created` |
| **NCL-08-CN-011-TC-04** | Notification | Yêu cầu nhiều lô được phê duyệt. Mỗi doanh nghiệp thu mua đã nhận lô trong phạm vi phải nhận notification phù hợp. Không gửi duplicate. | `PUT /bulk/{id}/approve` → Gửi notification theo quy tắc 8.2 | `200 OK` |
| **NCL-08-CN-011-TC-05** | Phạm vi rỗng | Người dùng bỏ chọn toàn bộ lô. Không cho tạo yêu cầu. | `POST /bulk` với `includedShipments` rỗng → Trả về 400 | `400 Bad Request` |

---

## 13. Error Codes Tập trung

| Error Code | HTTP Status | Message | Nguyên nhân |
| --- | --- | --- | --- |
| `BULK_RECALL_001` | 400 | `"Lý do thu hồi không được để trống."` | `reason` rỗng |
| `BULK_RECALL_002` | 400 | `"Phải chọn ít nhất một lô hàng để thu hồi."` | `includedShipments` rỗng |
| `BULK_RECALL_003` | 400 | `"Lô hàng bị loại {{shipmentId}} phải có lý do loại bỏ."` | `exclusionReason` rỗng |
| `BULK_RECALL_004` | 400 | `"Lô hàng {{shipmentId}} đã được thu hồi trước đó."` | Shipment đã RECALLED |
| `BULK_RECALL_005` | 400 | `"Lô hàng {{shipmentId}} không thuộc lô sản xuất đã chọn."` | Shipment không thuộc ProductionLot |
| `BULK_RECALL_006` | 400 | `"Bạn không thể phê duyệt yêu cầu do chính mình tạo."` | Tự phê duyệt (QTN-22) |
| `BULK_RECALL_007` | 403 | `"Bạn không có quyền thao tác trên lô sản xuất của tổ chức khác."` | Khác tổ chức |
| `BULK_RECALL_008` | 404 | `"Không tìm thấy lô sản xuất."` | ProductionLot không tồn tại |
| `BULK_RECALL_009` | 404 | `"Không tìm thấy yêu cầu thu hồi."` | BulkRecallRequest không tồn tại |
| `BULK_RECALL_010` | 409 | `"Đã có yêu cầu thu hồi đang chờ duyệt cho lô sản xuất này."` | Trùng PENDING |
| `BULK_RECALL_011` | 409 | `"Chỉ có thể phê duyệt yêu cầu ở trạng thái PENDING."` | Sai trạng thái |
| `BULK_RECALL_012` | 409 | `"Lô hàng {{shipmentId}} đã bị thu hồi bởi một yêu cầu khác."` | Concurrent recall |

---

## 14. Lưu ý triển khai

1. **Không tạo trạng thái mới:** Chỉ sử dụng `RECALLED` cho Shipment.status. Không tạo `IN_RECALL`, `RECALLING` hoặc tương tự.

2. **Tái sử dụng service hiện có:**
   - `ShipmentRecallService` (NCL-08-CN-003) - Cập nhật Shipment + TraceCode → RECALLED
   - `NotificationService.sendRecallNotification()` - Gửi notification
   - `AuditLogService` - Ghi audit

3. **Backend enforce business rule:** Tất cả validation phải thực hiện ở backend, không phụ thuộc vào frontend.

4. **Organization isolation:** Mỗi query phải kèm điều kiện `organizationId` của người dùng hiện tại.

5. **Tích hợp với NCL-08-CN-010:** Frontend nên gọi `GET /api/v1/trace/impact-scope?code={code}` trước để lấy phạm vi ảnh hưởng, sau đó cho phép người dùng chọn/lọc trước khi gọi `POST /api/v1/recall-requests/bulk`.
