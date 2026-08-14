## 📘 API Docs: Xuất dữ liệu mở theo lược đồ chuẩn (NCL-10-CN-007)

### 1️⃣ Thông tin chung

| Thuộc tính | Giá trị |
|-----------|---------|
| **User Story** | NCL-10-CN-007 – Xuất dữ liệu mở theo lược đồ chuẩn |
| **Vai trò** | Cán bộ quản lý ngành (VT-05) |
| **Mục tiêu** | Cho phép Cán bộ quản lý ngành xuất dữ liệu truy xuất theo lược đồ chuẩn mô phỏng để chia sẻ cho hệ thống khác và lưu trữ |
| **Nhánh git** | `feature/open-data-export` |

---

### 2️⃣ Endpoint

| Thuộc tính | Giá trị |
|-----------|---------|
| **Phương thức** | `POST` |
| **URL** | `/api/v1/export/open-data` |
| **Quyền** | Chỉ VT-05 (Cán bộ quản lý ngành) |
| **Content-Type** | `application/json` |

---

### 3️⃣ Request

#### Headers
| Tên | Giá trị | Bắt buộc |
|-----|---------|----------|
| `Authorization` | Bearer `<token>` | ✅ Có |
| `Content-Type` | `application/json` | ✅ Có |

#### Body (JSON)

```json
{
  "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "fromDate": "2026-07-01T00:00:00",
  "toDate": "2026-08-01T23:59:59",
  "productCategoryIds": ["c59ff5d6-8a6b-11f1-b7b2-e00af63e88f4"],
  "shipmentIds": [],
  "format": "JSON"
}
```

#### Chi tiết trường

| Trường | Kiểu | Bắt buộc | Mô tả |
|--------|------|----------|-------|
| `organizationId` | UUID (string) | ❌ | Lọc theo tổ chức cụ thể. Nếu bỏ qua, lấy tất cả tổ chức |
| `fromDate` | ISO datetime | ❌ | Ngày bắt đầu lọc (theo `createdAt` của shipment). Mặc định: không giới hạn |
| `toDate` | ISO datetime | ❌ | Ngày kết thúc lọc (theo `createdAt` của shipment). Mặc định: không giới hạn |
| `productCategoryIds` | List<UUID> | ❌ | Lọc theo danh mục sản phẩm. Nếu rỗng hoặc null, không lọc theo danh mục |
| `shipmentIds` | List<UUID> | ❌ | Lọc theo danh sách lô hàng cụ thể. Ưu tiên hơn các bộ lọc khác |
| `format` | string | ❌ | Định dạng file xuất: `JSON`, `XML`, `CSV`. Mặc định: `JSON`. **Lưu ý:** XML chưa được hỗ trợ trong phiên bản hiện tại |

---

### 4️⃣ Response

#### Thành công (HTTP 200 OK)

Trả về file tải xuống với các định dạng tương ứng.

**Response Headers:**
- `Content-Type`: `application/json` (hoặc `text/csv`)
- `Content-Disposition`: `attachment; filename="export_20260802_183000.json"`

**Cấu trúc dữ liệu JSON (OpenDataSchema):**

```json
{
  "exportedAt": "2026-08-02T18:30:00",
  "exporter": {
    "userId": "da430de5-4fb1-4564-b1e4-5d67627841a3",
    "fullName": "Nguyễn Văn A",
    "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
    "organizationName": "HTX-BBC"
  },
  "shipments": [
    {
      "id": "a1b2c3d4-1111-4a2a-9f3d-1a2b3c4d5e6f",
      "name": "Lô hàng ABC",
      "productionLotName": "Lô sản xuất Doraemon",
      "productCategory": "Xoài",
      "totalQuantity": 1000.0,
      "unit": "kg",
      "status": "ACTIVATED",
      "timeline": [
        {
          "eventType": "HARVEST",
          "recordedAt": "2026-08-02T10:00:00",
          "recordedBy": "Nguyễn Văn B",
          "location": {
            "latitude": 10.8231,
            "longitude": 106.6297
          },
          "data": {
            "quantity": 1000,
            "harvestDate": "2026-08-02"
          }
        }
      ],
      "certifications": [
        {
          "standardName": "VietGAP",
          "certificationCode": "VG-2025-001",
          "issueDate": "2025-01-01T00:00:00",
          "expiryDate": "2026-12-31T00:00:00"
        }
      ]
    }
  ]
}
```

#### Lỗi thường gặp

**400 Bad Request — Không có dữ liệu trong phạm vi lọc (TC-02)**
```json
{
  "success": false,
  "status": 400,
  "message": "Không có lô hàng nào trong phạm vi lọc.",
  "path": "/api/v1/export/open-data",
  "timestamp": "2026-08-02T18:30:00"
}
```

**400 Bad Request — Không có lô đủ điều kiện QTN-11**
```json
{
  "success": false,
  "status": 400,
  "message": "Không có lô hàng nào đáp ứng đủ điều kiện QTN-11 (thiếu sự kiện chuỗi cung ứng hoặc chứng từ).",
  "path": "/api/v1/export/open-data",
  "timestamp": "2026-08-02T18:30:00"
}
```

**400 Bad Request — Định dạng không hỗ trợ**
```json
{
  "success": false,
  "status": 400,
  "message": "Định dạng XML chưa được hỗ trợ trong phiên bản này. Vui lòng chọn JSON.",
  "path": "/api/v1/export/open-data",
  "timestamp": "2026-08-02T18:30:00"
}
```

**403 Forbidden — Không có quyền (TC-03)**
```json
{
  "success": false,
  "status": 403,
  "message": "Chỉ Cán bộ quản lý ngành (VT-05) mới được xuất dữ liệu này.",
  "path": "/api/v1/export/open-data",
  "timestamp": "2026-08-02T18:30:00"
}
```

**401 Unauthorized — Chưa đăng nhập**
```json
{
  "success": false,
  "status": 401,
  "message": "Bạn cần đăng nhập để thực hiện thao tác này."
}
```

---

### 5️⃣ Business Rules (QTN-11)

**Điều kiện xuất dữ liệu (QTN-11):** Hồ sơ truy xuất chỉ xuất cho lô đã hoàn tất và đủ chứng từ.

Một lô hàng (Shipment) được coi là **đủ điều kiện xuất** khi:

1. **Tất cả sự kiện chuỗi cung ứng bắt buộc đều có:**
   - `HARVEST` (Thu hoạch)
   - `PACKAGING` (Đóng gói)
   - `TRANSPORT` (Vận chuyển)
   - `PROCUREMENT` (Thu mua)

2. **Có ít nhất một trong hai loại chứng từ:**
   - Có ít nhất 1 Nhật ký canh tác (`FarmLog`) gắn với lô sản xuất
   - Có ít nhất 1 Chứng nhận (`Certification`) gắn với lô sản xuất

Nếu không đáp ứng, hệ thống trả về lỗi **400** với thông báo tương ứng.

---

### 6️⃣ Ví dụ cURL

#### JSON – mặc định
```bash
curl -X POST http://localhost:8080/api/v1/export/open-data \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "d53ea88e-8837-4722-acf9-761f0d0c7a48",
    "fromDate": "2026-07-01T00:00:00",
    "toDate": "2026-08-01T23:59:59",
    "format": "JSON"
  }' \
  --output export_data.json
```

#### CSV
```bash
curl -X POST http://localhost:8080/api/v1/export/open-data \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "format": "CSV"
  }' \
  --output export_data.csv
```

#### Lọc theo danh mục sản phẩm
```bash
curl -X POST http://localhost:8080/api/v1/export/open-data \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productCategoryIds": ["c59ff5d6-8a6b-11f1-b7b2-e00af63e88f4"],
    "format": "JSON"
  }' \
  --output export_data.json
```

---

### 7️⃣ Kiểm thử (Test Cases)

| Test Case | Mô tả | Expected |
|-----------|-------|----------|
| TC-01 | Luồng thành công – lô đủ điều kiện, format hợp lệ | HTTP 200, file tải về đúng định dạng |
| TC-02 | Dữ liệu rỗng – không có lô trong phạm vi | HTTP 400, message "Không có lô hàng nào..." |
| TC-03 | Không có quyền – user VT-02 gọi API | HTTP 403 |
| TC-04 | Lô thiếu sự kiện bắt buộc | HTTP 400, message về QTN-11 |
| TC-05 | Format không hỗ trợ (XML) | HTTP 400, message chưa hỗ trợ |

---

### 8️⃣ Lưu ý khi tích hợp

- **Dung lượng file lớn:** Nếu số lượng lô hàng lớn, file có thể nặng. Có thể cân nhắc thêm phân trang cho request.
- **CSV:** Do cấu trúc dữ liệu có nested objects (timeline, certifications), dữ liệu được flatten với các cột JSON, dễ gây khó đọc. Khuyến nghị dùng JSON cho dữ liệu đầy đủ.
- **XML:** Chưa hỗ trợ trong phiên bản hiện tại, sẽ được bổ sung sau.
- **Tên file:** Định dạng `export_YYYYMMDD_HHmmss.{format}`.

---

✅ Tài liệu này đáp ứng user story NCL-10-CN-007 và các tiêu chí chấp nhận. Frontend có thể dựa vào đây để thiết kế giao diện chọn phạm vi và định dạng xuất. 🚀