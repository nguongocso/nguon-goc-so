# 📘 API Docs: Truy vết phạm vi ảnh hưởng của một lô (NCL-08-CN-010)

## 1. Thông tin chung

| Thuộc tính | Giá trị |
| --- | --- |
| **User Story** | NCL-08-CN-010 - Truy vết phạm vi ảnh hưởng của một lô |
| **Epic** | NCL-08 - Cảnh báo, thu hồi lô và lịch sử hoạt động |
| **Git Branch** | `feature/NCL-08-CN-010_impact-scope-tracing` |
| **Vai trò thực hiện** | VT-02 - Quản lý hợp tác xã (và VT-01 Admin) |
| **Endpoint chính** | `GET /api/v1/trace/impact-scope` |
| **Endpoint Export** | `GET /api/v1/trace/impact-scope/export` |
| **Phương thức** | `GET` |
| **Bảo mật** | Yêu cầu JWT token, `@PreAuthorize("hasAnyRole('VT-02', 'VT-01')")` |

---

## 2. Mô tả nghiệp vụ & Quy tắc (Business Rules)

User Story **NCL-08-CN-010** cho phép Quản lý hợp tác xã (VT-02) truy vết cây quan hệ hai chiều (ngược và xuôi) khi phát hiện sự cố trên một lô sản xuất, lô hàng hoặc mã tem, phục vụ công tác thu hồi chính xác (không thu hồi thiếu hoặc thừa).

### Quy tắc nghiệp vụ chi tiết:

1. **Tự động nhận diện đầu vào (Multi-code Lookup):**
   - Đối tượng tìm kiếm `code` có thể là **Mã lô sản xuất** (`production_lot.id` / `production_lot.name`), **Mã lô hàng** (`shipments.id` / `shipments.name`), hoặc **Mã tem** (`trace_codes.code_value`).
   - Hệ thống tự động xác định nút gốc, dựng cây truy vết theo **chiều ngược (Upstream)** đến Vùng trồng gốc và **chiều xuôi (Downstream)** đến tất cả các lô hàng, tem, sự kiện và tổ chức đã nhận.

2. **Cấu trúc Cây quan hệ 2 chiều (Upstream & Downstream Graph):**
   - **Vùng trồng (Farm Area):** Tên vùng trồng, mã vùng trồng, vị trí địa lý.
   - **Lô sản xuất (Production Lot):** Mã lô, tên sản phẩm, sản lượng dự kiến/thực tế, ngày gieo trồng, ngày thu hoạch, trạng thái.
   - **Lô hàng sinh ra (Shipments):** Danh sách các lô hàng sinh ra từ lô sản xuất đó, gồm: mã lô hàng, tên lô hàng, số lượng, ngày khởi tạo, trạng thái lô hàng (`DRAFT`, `ACTIVATED`, `RECALLED`, ...).
   - **Thống kê Tem & Lượt quét:** 
     - Số lượng tem đã kích hoạt (`activatedStampsCount`).
     - Tổng số lượt quét công khai & Thời điểm quét gần nhất (`recentScanAt`).
   - **Tổ chức nhận hàng (Receiving Organizations):** Các đối tác thu mua/tiếp nhận lô hàng thông qua sự kiện `PROCUREMENT`, `TRANSPORT` hoặc `WAREHOUSE_RECEIPT`.

3. **Cách ly dữ liệu & Bảo mật tổ chức (QTN-01 & TC-04):**
   - Mỗi tổ chức chỉ thấy và thao tác trên dữ liệu thuộc phạm vi sở hữu của mình.
   - Đối với thông tin các **Tổ chức nhận** (bên mua/đối tác bên ngoài HTX): **Chỉ trả về Tên tổ chức và Thời điểm nhận hàng**. Tuyệt đối không trả về dữ liệu nội bộ khác của tổ chức đối tác.

4. **Xử lý kịch bản dữ liệu rỗng (TC-03):**
   - Nếu Lô sản xuất tồn tại nhưng chưa sinh ra Lô hàng nào (`shipments` rỗng): Trả về HTTP `200 OK` kèm thông tin Lô sản xuất, danh sách `shipments: []`, và thông điệp ghi rõ: `"Lô sản xuất chưa phát sinh lô hàng nào."`. Không báo lỗi hệ thống.

5. **Xử lý kịch bản mã không tồn tại (TC-05):**
   - Nếu `code` nhập vào không tìm thấy trong cả 3 đối tượng (Lô sản xuất, Lô hàng, Tem): Trả về HTTP `404 Not Found` kèm thông báo: `"Mã truy vết không tồn tại trên hệ thống. Vui lòng kiểm tra lại."`

6. **Dòng sự kiện immutable (QTN-08):**
   - Các sự kiện vận chuyển/thu mua được hiển thị nguyên bản theo lịch sử ghi nhận, hỗ trợ nhận biết sự kiện đính chính (`isCorrection = true`).

7. **Xuất báo cáo (Export Document):**
   - Hỗ trợ xuất toàn bộ cây quan hệ phạm vi ảnh hưởng ra tệp Excel (`.xlsx`) hoặc PDF để làm hồ sơ minh chứng khi làm việc với siêu thị và cơ quan quản lý.

---

## 3. Chi tiết API Endpoints

### 3.1. API Truy vết phạm vi ảnh hưởng (Get Impact Scope Trace)

```http
GET /api/v1/trace/impact-scope?code={code}
```

#### Headers:
```http
Authorization: Bearer <JWT_TOKEN>
Accept: application/json
```

#### Query Parameters:
| Parameter | Type | Required | Description | Example |
| --- | --- | --- | --- | --- |
| `code` | String | Có | Mã lô sản xuất, Mã lô hàng hoặc Mã tem QR | `LOT-2026-001` / `SHIP-8821` / `NCL0001` |

---

#### Responses:

##### Success: `200 OK` (Truy vết thành công từ Lô sản xuất có nhiều Lô hàng - TC-01)
```json
{
  "success": true,
  "status": 200,
  "message": "Truy vết phạm vi ảnh hưởng thành công",
  "data": {
    "rootNodeType": "PRODUCTION_LOT",
    "searchedCode": "LOT-2026-001",
    "farmArea": {
      "id": "a1e8f234-91bc-4e12-881a-111111111111",
      "code": "FA-DALAT-01",
      "name": "Vùng trồng Dâu tây Đà Lạt Khu A",
      "location": "Đà Lạt, Lâm Đồng"
    },
    "productionLot": {
      "id": "b2f9f345-02cd-5f23-992b-222222222222",
      "code": "LOT-2026-001",
      "name": "Lô dâu tây vụ Mùa 2026",
      "status": "APPROVED",
      "expectedQuantity": 5000.0,
      "expectedQuantityUnit": "kg",
      "actualQuantity": 4850.0,
      "plantingDate": "2026-01-15",
      "harvestDate": "2026-05-20"
    },
    "shipments": [
      {
        "id": "c3a0a456-13de-6a34-003c-333333333333",
        "code": "SHIP-8821",
        "name": "Lô hàng Dâu tây Giao Siêu thị Co.opmart",
        "status": "ACTIVATED",
        "totalQuantity": 1500,
        "packagingInfo": "Thùng carton 5kg x 300",
        "createdAt": "2026-05-22T08:30:00",
        "activatedStampsCount": 300,
        "scanStats": {
          "totalScans": 142,
          "recentScanAt": "2026-09-06T15:45:00",
          "suspectCount": 0
        },
        "receivingOrganizations": [
          {
            "organizationId": "org-coop-99",
            "organizationName": "Liên hiệp HTX Thương mại TP.HCM (Co.opmart)",
            "receivedAt": "2026-05-23T10:15:00",
            "eventType": "PROCUREMENT"
          }
        ]
      },
      {
        "id": "d4b1b567-24ef-7b45-114d-444444444444",
        "code": "SHIP-8822",
        "name": "Lô hàng Dâu tây Giao WinMart",
        "status": "ACTIVATED",
        "totalQuantity": 2000,
        "packagingInfo": "Hộp nhựa 500g x 4000",
        "createdAt": "2026-05-24T09:00:00",
        "activatedStampsCount": 4000,
        "scanStats": {
          "totalScans": 520,
          "recentScanAt": "2026-09-07T08:10:00",
          "suspectCount": 1
        },
        "receivingOrganizations": [
          {
            "organizationId": "org-winmart-88",
            "organizationName": "Công ty CP WinCommerce (WinMart)",
            "receivedAt": "2026-05-25T14:20:00",
            "eventType": "WAREHOUSE_RECEIPT"
          }
        ]
      },
      {
        "id": "e5c2c678-35fa-8c56-225e-555555555555",
        "code": "SHIP-8823",
        "name": "Lô hàng Dâu tây Lưu kho phân phối nội bộ",
        "status": "DRAFT",
        "totalQuantity": 1350,
        "packagingInfo": "Bao xốp 10kg x 135",
        "createdAt": "2026-05-26T11:00:00",
        "activatedStampsCount": 0,
        "scanStats": {
          "totalScans": 0,
          "recentScanAt": null,
          "suspectCount": 0
        },
        "receivingOrganizations": []
      }
    ],
    "summary": {
      "totalShipments": 3,
      "totalActivatedStamps": 4300,
      "totalReceivingOrganizations": 2,
      "totalRecalledShipments": 0
    }
  },
  "timestamp": "2026-09-07T11:00:00.000Z"
}
```

---

##### Success: `200 OK` (Lô sản xuất chưa sinh Lô hàng - TC-03)
```json
{
  "success": true,
  "status": 200,
  "message": "Truy vết phạm vi ảnh hưởng thành công. Lô sản xuất chưa phát sinh lô hàng nào.",
  "data": {
    "rootNodeType": "PRODUCTION_LOT",
    "searchedCode": "LOT-2026-999",
    "farmArea": {
      "id": "a1e8f234-91bc-4e12-881a-111111111111",
      "code": "FA-DALAT-01",
      "name": "Vùng trồng Dâu tây Đà Lạt Khu A",
      "location": "Đà Lạt, Lâm Đồng"
    },
    "productionLot": {
      "id": "f6d3d789-46ab-9d67-336f-666666666666",
      "code": "LOT-2026-999",
      "name": "Lô Dâu tây mới thu hoạch chưa xuất bán",
      "status": "APPROVED",
      "expectedQuantity": 1000.0,
      "expectedQuantityUnit": "kg",
      "actualQuantity": 980.0,
      "plantingDate": "2026-02-01",
      "harvestDate": "2026-06-01"
    },
    "shipments": [],
    "summary": {
      "totalShipments": 0,
      "totalActivatedStamps": 0,
      "totalReceivingOrganizations": 0,
      "totalRecalledShipments": 0
    }
  },
  "timestamp": "2026-09-07T11:00:00.000Z"
}
```

---

##### Error: `404 Not Found` (Mã không tồn tại - TC-05)
```json
{
  "success": false,
  "status": 404,
  "message": "Mã truy vết không tồn tại trên hệ thống. Vui lòng kiểm tra lại mã lô sản xuất, lô hàng hoặc tem.",
  "timestamp": "2026-09-07T11:00:00.000Z"
}
```

---

##### Error: `403 Forbidden` (Không thuộc phạm vi tổ chức / Không đủ quyền - QTN-01)
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền xem thông tin truy vết của đối tượng thuộc tổ chức khác.",
  "timestamp": "2026-09-07T11:00:00.000Z"
}
```

---

### 3.2. API Xuất báo cáo phạm vi ảnh hưởng (Export Impact Scope Document)

```http
GET /api/v1/trace/impact-scope/export?code={code}&format={format}
```

#### Query Parameters:
| Parameter | Type | Required | Default | Description |
| --- | --- | --- | --- | --- |
| `code` | String | Có | | Mã lô sản xuất, Mã lô hàng hoặc Mã tem QR |
| `format` | String | Không | `EXCEL` | Định dạng tệp xuất: `EXCEL` (`.xlsx`) hoặc `PDF` (`.pdf`) |

#### Responses:
- **`200 OK`**: File stream binary.
- **Headers**:
  - `Content-Type`: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (đối với EXCEL) hoặc `application/pdf` (đối với PDF).
  - `Content-Disposition`: `attachment; filename="Truy_vet_pham_vi_anh_huong_LOT-2026-001.xlsx"`

---

## 4. Bảng Ma trận Kiểm thử & Đáp ứng Tiêu chí (Test Case Mapping)

| Mã Test Case | Loại kịch bản | Yêu cầu AC | Đáp ứng trong API Docs | Status Code |
| :--- | :--- | :--- | :--- | :--- |
| **NCL-08-CN-010-TC-01** | Luồng thành công | Mã Lô sản xuất sinh 3 lô hàng, 2 lô đã thu mua | Trả về `shipments` chứa 3 lô hàng, 4300 tem kích hoạt, 2 tổ chức nhận | `200 OK` |
| **NCL-08-CN-010-TC-02** | Luồng thành công | Nhập Mã temQR | Tự động lookup từ `TraceCode` $\rightarrow$ `Shipment` $\rightarrow$ `ProductionLot` $\rightarrow$ `FarmArea` gốc | `200 OK` |
| **NCL-08-CN-010-TC-03** | Dữ liệu rỗng | Lô sản xuất chưa sinh lô hàng nào | Trả về nút `productionLot` kèm `shipments: []` và thông điệp giải thích | `200 OK` |
| **NCL-08-CN-010-TC-04** | Quyền / Bảo mật | Ranh giới dữ liệu tổ chức nhận hàng | Chỉ trả về `organizationName` và `receivedAt` trong `receivingOrganizations`, không lộ dữ liệu kho nội bộ | `200 OK` |
| **NCL-08-CN-010-TC-05** | Dữ liệu không hợp lệ | Mã không tồn tại | Trả về lỗi 404 kèm hướng dẫn kiểm tra lại mã | `404 Not Found` |
