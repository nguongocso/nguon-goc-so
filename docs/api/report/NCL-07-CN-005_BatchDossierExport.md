# API Docs – NCL-07-CN-005: Xuất hồ sơ truy xuất cho nhiều lô trong một lần

**Mã User Story:** `NCL-07-CN-005`  
**Tên User Story:** Xuất hồ sơ truy xuất cho nhiều lô trong một lần  
**Nhánh Git:** `feature/NCL-07-CN-005_batch-dossier-export`

---

## Danh sách APIs

| STT | Tên API | Method | Endpoint Path | Quyền truy cập |
|-----|---------|--------|---------------|----------------|
| 1 | Kiểm tra điều kiện xuất hồ sơ hàng loạt (QTN-11 & QTN-01) | `POST` | `/api/v1/shipments/dossiers/batch-check` | `VT-01`, `VT-02`, `VT-04` |
| 2 | Xuất và tải bộ hồ sơ truy xuất hợp nhất (PDF) | `POST` | `/api/v1/shipments/dossiers/batch-export` | `VT-01`, `VT-02`, `VT-04` |
| 3 | Lấy lịch sử xuất bộ hồ sơ hàng loạt | `GET` | `/api/v1/shipments/dossiers/batch-history` | `VT-01`, `VT-02`, `VT-04` |

---

## 1. Kiểm tra điều kiện xuất hồ sơ hàng loạt (QTN-11 & QTN-01)

### Thông tin API

| Thuộc tính | Giá trị |
|------------|---------|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/shipments/dossiers/batch-check` |
| **Quyền** | `VT-01`, `VT-02`, `VT-04` |

- `VT-01`: Quản trị viên hệ thống (Admin).
- `VT-02`: Quản lý hợp tác xã (Cooperative Manager).
- `VT-04`: Doanh nghiệp thu mua (Procurement Enterprise).

---

### Request Body (`application/json`)

```json
{
  "shipmentIds": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002",
    "550e8400-e29b-41d4-a716-446655440003"
  ]
}
```

#### Tham số Request:

| Parameter | Kiểu | Bắt buộc | Mô tả |
|-----------|------|----------|-------|
| `shipmentIds` | `List<UUID>` | Có | Danh sách ID của các lô hàng được chọn để xuất bộ hồ sơ. |

---

### Response `200 OK` – Thành công (Đáp ứng `TC-01` & `TC-02` & `TC-04`)

Hệ thống phân loại các lô thành **Lô đủ điều kiện** và **Lô không đủ điều kiện** (do thiếu chứng từ theo `QTN-11` hoặc vi phạm phân quyền theo `QTN-01`).

```json
{
  "success": true,
  "status": 200,
  "data": {
    "totalSelected": 3,
    "totalEligible": 2,
    "totalIneligible": 1,
    "eligibleShipments": [
      {
        "shipmentId": "550e8400-e29b-41d4-a716-446655440001",
        "shipmentName": "Lô xoài Cát Chu xuất khẩu #1",
        "eligible": true,
        "missingDocuments": []
      },
      {
        "shipmentId": "550e8400-e29b-41d4-a716-446655440002",
        "shipmentName": "Lô xoài Cát Chu xuất khẩu #2",
        "eligible": true,
        "missingDocuments": []
      }
    ],
    "ineligibleShipments": [
      {
        "shipmentId": "550e8400-e29b-41d4-a716-446655440003",
        "shipmentName": "Lô xoài Cát Chu xuất khẩu #3",
        "eligible": false,
        "missingDocuments": [
          "Thiếu chứng từ bón phân (FERTILIZING)",
          "Lô chưa ghi nhận sự kiện thu hoạch (HARVESTING)"
        ]
      }
    ]
  },
  "timestamp": "2026-09-07T22:20:00.000Z"
}
```

---

## 2. Xuất và tải bộ hồ sơ truy xuất hợp nhất (PDF)

### Thông tin API

| Thuộc tính | Giá trị |
|------------|---------|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/shipments/dossiers/batch-export` |
| **Quyền** | `VT-01`, `VT-02`, `VT-04` |

---

### Request Body (`application/json`)

```json
{
  "shipmentIds": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002"
  ],
  "title": "Bộ hồ sơ Chuyến hàng Giao Siêu thị WinMart #882",
  "note": "Xuất phần các lô đủ điều kiện"
}
```

#### Tham số Request:

| Parameter | Kiểu | Bắt buộc | Mô tả |
|-----------|------|----------|-------|
| `shipmentIds` | `List<UUID>` | Có | Danh sách ID các lô hàng ĐỦ ĐIỀU KIỆN cần xuất bộ hồ sơ. |
| `title` | `String` | Không | Tiêu đề tùy chỉnh cho Chuyến hàng / Đơn giao siêu thị. |
| `note` | `String` | Không | Ghi chú bổ sung trên trang bìa bộ hồ sơ. |

---

### Response `200 OK` – Đồng bộ / Số lượng lô vừa và nhỏ (`TC-01`)

- **Content-Type:** `application/pdf`
- **Content-Disposition:** `attachment; filename="Bo_ho_so_truy_xuat_<YYYYMMDD_HHmmss>.pdf"`
- **Body:** Binary Stream tệp PDF duy nhất gồm:
  - **Trang 1 - TRANG BÌA TỔNG HỢP (Summary Cover Page):**
    - Tiêu đề bộ hồ sơ, thông tin Doanh nghiệp thu mua / Hợp tác xã cung cấp, ngày tạo.
    - Bảng danh sách tổng hợp các lô hàng (Tên lô, Mã lô sản xuất, Số lượng, Quy cách đóng gói, Trạng thái).
  - **Các trang tiếp theo - HỒ SƠ CHI TIẾT TỪNG LÔ:**
    - Tự động ghép nối toàn bộ hồ sơ chi tiết (Thông tin lô sản xuất, nhật ký canh tác, kiểm nghiệm, dòng sự kiện timeline) cho từng lô đủ điều kiện.

---

### Response `202 Accepted` – Xử lý nền số lượng lô lớn (`TC-03`)

Khi danh sách lô có số lượng lớn (ví dụ: > 50 lô), hệ thống chuyển sang chế độ xử lý bất đồng bộ (Background Job) để không gây nghẽn kết nối HTTP.

```json
{
  "success": true,
  "status": 202,
  "message": "Yêu cầu xuất bộ hồ sơ số lượng lớn đã được chuyển vào tiến trình xử lý nền. Hệ thống sẽ gửi thông báo kèm liên kết tải tệp sau khi hoàn thành.",
  "data": {
    "jobId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "status": "PROCESSING",
    "totalLots": 500,
    "createdAt": "2026-09-07T22:20:00.000Z"
  },
  "timestamp": "2026-09-07T22:20:00.000Z"
}
```

---

## 3. Lấy lịch sử xuất bộ hồ sơ hàng loạt

### Thông tin API

| Thuộc tính | Giá trị |
|------------|---------|
| **Method** | `GET` |
| **Endpoint** | `/api/v1/shipments/dossiers/batch-history` |
| **Quyền** | `VT-01`, `VT-02`, `VT-04` |

---

### Response `200 OK` – Thành công

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440000",
      "title": "Bộ hồ sơ Chuyến hàng Giao Siêu thị WinMart #882",
      "exportedAt": "2026-09-07T22:20:00.000Z",
      "exporterName": "Nguyễn Văn A",
      "organizationName": "Doanh nghiệp Thu mua Nông sản Việt",
      "totalSelectedLots": 10,
      "eligibleLotsCount": 8,
      "ineligibleLotsCount": 2,
      "fileName": "Bo_ho_so_truy_xuat_20260907_222000.pdf",
      "fileSize": 2450820,
      "status": "SUCCESS",
      "ipAddress": "192.168.1.15"
    }
  ],
  "timestamp": "2026-09-07T22:21:00.000Z"
}
```
