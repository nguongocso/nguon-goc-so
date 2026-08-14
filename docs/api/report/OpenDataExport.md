# API Docs – Xuất dữ liệu mở theo lược đồ chuẩn

**Tên nhánh:** `feature/open-data-export`

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Người thực hiện | Nội dung thay đổi |
| :--- | :--- | :--- | :--- |
| 2026-08-02 | v1.0.0 | Antigravity | Khởi tạo tài liệu đặc tả API Xuất dữ liệu mở |

---

## 1. Xuất dữ liệu mở theo lược đồ chuẩn (JSON / XML / CSV)

### Thông tin API

| Thuộc tính | Giá trị |
| :--- | :--- |
| **Method** | `GET` |
| **Endpoint** | `/api/v1/reports/open-data/export` |
| **Quyền** | `VT-05` (Cán bộ quản lý ngành) |

---

### Request

**Query Parameters:**

| Tham số | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Validation | Ví dụ | Mô tả |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `region` | String | Có | `@NotBlank` | `Phú Thọ` | Tên địa bàn cần xuất dữ liệu mở (so khớp case-insensitive trong địa chỉ tổ chức). |
| `fromDate` | LocalDate | Có | Định dạng `yyyy-MM-dd` | `2026-01-01` | Ngày bắt đầu khoảng thời gian thu hoạch nông sản. |
| `toDate` | LocalDate | Có | Định dạng `yyyy-MM-dd`, `toDate >= fromDate` | `2026-07-31` | Ngày kết thúc khoảng thời gian thu hoạch nông sản. |
| `format` | String | Có | Giá trị hợp lệ: `JSON`, `XML`, `CSV` (case-insensitive) | `JSON` | Định dạng file xuất mong muốn. |

---

### Request Example (URL)

```http
GET http://localhost:8080/api/v1/reports/open-data/export?region=Ph%C3%BA%20Th%E1%BB%8D&fromDate=2026-01-01&toDate=2026-07-31&format=JSON
Authorization: Bearer <JWT_TOKEN>
```

---

### Response — Success

| Status Code | When it occurs | Content-Type | File Attachment Name |
| :--- | :--- | :--- | :--- |
| 200 OK | Truy vấn có dữ liệu đủ điều kiện và xuất file thành công | `application/json` (nếu format=JSON)<br>`application/xml` (nếu format=XML)<br>`text/csv` (nếu format=CSV) | `open_data_<region>_<yyyyMMdd_HHmmss>.<format>` |

#### 1. Ví dụ Response thành công dạng JSON (`format=JSON`)

```json
[
  {
    "lotId": "a8df31f4-90b1-4c4f-9e70-a612140a7cf2",
    "lotCode": "Lô Chè Ô Long - Long Cốc 2026",
    "productCategory": "Chè",
    "expectedQuantity": 1500.0,
    "expectedQuantityUnit": "kg",
    "actualQuantity": 1450.5,
    "plantingDate": "2026-03-01",
    "harvestDate": "2026-06-15",
    "status": "PACKAGED",
    "organization": {
      "organizationId": "550e8400-e29b-41d4-a716-446655440000",
      "organizationName": "HTX Chè An Toàn Long Cốc",
      "organizationAddress": "Xã Long Cốc, Huyện Tân Sơn, Phú Thọ"
    },
    "farmArea": {
      "farmAreaId": "3517f6e0-3323-441a-bde0-f2f0d42eba54",
      "farmAreaName": "Đồi chè số 3",
      "farmAreaSize": 2.5,
      "farmAreaLocation": {
        "latitude": 21.214,
        "longitude": 105.142
      }
    },
    "farmLogs": [
      {
        "logId": "b02431f4-2a6e-4972-8d70-53675f868240",
        "activityType": "PLANTING",
        "material": "Hạt giống chè lai LDP1",
        "quantity": 100.0,
        "unit": "kg",
        "executedDate": "2026-03-02",
        "notes": "Xuống giống đợt đầu mùa mưa",
        "attachments": ["planting_cert.jpg"]
      }
    ],
    "shipments": [
      {
        "shipmentId": "0972eb3c-ad37-4e81-b3f7-b47016840f45",
        "shipmentName": "Lô Chè Ô Long Đóng Gói Xuất Xưởng",
        "totalQuantity": 1000,
        "shippedAt": "2026-06-16T08:30:00Z",
        "journeyEvents": [
          {
            "eventId": "64ed177a-f189-4118-8b74-08d9a50b5f8d",
            "eventType": "PACKAGING",
            "recordedAt": "2026-06-16T10:15:00Z",
            "actorName": "Nguyễn Văn A",
            "eventLocation": {
              "latitude": 21.215,
              "longitude": 105.143
            }
          }
        ]
      }
    ]
  }
]
```

#### 2. Ví dụ Response thành công dạng XML (`format=XML`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<OpenDataExport region="Phú Thọ" fromDate="2026-01-01" toDate="2026-07-31">
  <ProductionLots>
    <ProductionLot>
      <LotId>a8df31f4-90b1-4c4f-9e70-a612140a7cf2</LotId>
      <LotCode>Lô Chè Ô Long - Long Cốc 2026</LotCode>
      <ProductCategory>Chè</ProductCategory>
      <ExpectedQuantity>1500.0</ExpectedQuantity>
      <ExpectedQuantityUnit>kg</ExpectedQuantityUnit>
      <ActualQuantity>1450.5</ActualQuantity>
      <PlantingDate>2026-03-01</PlantingDate>
      <HarvestDate>2026-06-15</HarvestDate>
      <Status>PACKAGED</Status>
      <Organization>
        <OrganizationId>550e8400-e29b-41d4-a716-446655440000</OrganizationId>
        <OrganizationName>HTX Chè An Toàn Long Cốc</OrganizationName>
        <OrganizationAddress>Xã Long Cốc, Huyện Tân Sơn, Phú Thọ</OrganizationAddress>
      </Organization>
      <FarmArea>
        <FarmAreaId>3517f6e0-3323-441a-bde0-f2f0d42eba54</FarmAreaId>
        <FarmAreaName>Đồi chè số 3</FarmAreaName>
        <FarmAreaSize>2.50</FarmAreaSize>
        <FarmAreaLocation>
          <Latitude>21.214</Latitude>
          <Longitude>105.142</Longitude>
        </FarmAreaLocation>
      </FarmArea>
      <FarmLogs>
        <FarmLog>
          <LogId>b02431f4-2a6e-4972-8d70-53675f868240</LogId>
          <ActivityType>PLANTING</ActivityType>
          <Material>Hạt giống chè lai LDP1</Material>
          <Quantity>100.0</Quantity>
          <Unit>kg</Unit>
          <ExecutedDate>2026-03-02</ExecutedDate>
          <Notes>Xuống giống đợt đầu mùa mưa</Notes>
          <Attachments>
            <Attachment>planting_cert.jpg</Attachment>
          </Attachments>
        </FarmLog>
      </FarmLogs>
      <Shipments>
        <Shipment>
          <ShipmentId>0972eb3c-ad37-4e81-b3f7-b47016840f45</ShipmentId>
          <ShipmentName>Lô Chè Ô Long Đóng Gói Xuất Xưởng</ShipmentName>
          <TotalQuantity>1000</TotalQuantity>
          <ShippedAt>2026-06-16T08:30:00Z</ShippedAt>
          <JourneyEvents>
            <JourneyEvent>
              <EventId>64ed177a-f189-4118-8b74-08d9a50b5f8d</EventId>
              <EventType>PACKAGING</EventType>
              <RecordedAt>2026-06-16T10:15:00Z</RecordedAt>
              <ActorName>Nguyễn Văn A</ActorName>
              <EventLocation>
                <Latitude>21.215</Latitude>
                <Longitude>105.143</Longitude>
              </EventLocation>
            </JourneyEvent>
          </JourneyEvents>
        </Shipment>
      </Shipments>
    </ProductionLot>
  </ProductionLots>
</OpenDataExport>
```

#### 3. Ví dụ Response thành công dạng CSV (`format=CSV`)

```csv
lotId,lotCode,productCategory,expectedQuantity,expectedQuantityUnit,actualQuantity,plantingDate,harvestDate,status,organizationName,organizationAddress,farmAreaName,totalFarmLogs,totalShipments
a8df31f4-90b1-4c4f-9e70-a612140a7cf2,Lô Chè Ô Long - Long Cốc 2026,Chè,1500.0,kg,1450.5,2026-03-01,2026-06-15,PACKAGED,HTX Chè An Toàn Long Cốc,"Xã Long Cốc, Huyện Tân Sơn, Phú Thọ",Đồi chè số 3,1,1
```

---

### Response — Error

| Status Code | Error Code (if any) | Cause |
| :--- | :--- | :--- |
| `400 Bad Request` | `BUSINESS_ERROR` | Tham số phạm vi lọc không hợp lệ, hoặc định dạng `format` không đúng quy định. |
| `400 Bad Request` | `EMPTY_DATA` | Không có lô hàng hoặc tổ chức nào đủ điều kiện nằm trong địa bàn được lọc (TC-02). |
| `401 Unauthorized` | - | Missing/invalid JWT token. |
| `403 Forbidden` | - | Người dùng không phải là Cán bộ quản lý ngành `VT-05` (TC-03). |
| `500 Internal Error` | - | Hệ thống gặp lỗi xử lý hoặc xuất tập tin (không hiển thị stack trace ra ngoài). |

#### Ví dụ Error Response do thiếu dữ liệu xuất (`400 Bad Request` - TC-02)

```json
{
  "success": false,
  "status": 400,
  "message": "Không có dữ liệu mở đủ điều kiện để xuất trong phạm vi đã chọn.",
  "path": "/api/v1/reports/open-data/export",
  "timestamp": "2026-08-02T10:15:00.000Z"
}
```

#### Ví dụ Error Response do không có quyền (`403 Forbidden` - TC-03)

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "path": "/api/v1/reports/open-data/export",
  "timestamp": "2026-08-02T10:16:00.000Z"
}
```

---

## 2. Quy tắc nghiệp vụ (Business Rules)

### 2.1 Lọc tổ chức theo địa bàn (Region Filter)
- Hệ thống thực hiện tìm kiếm toàn bộ các Tổ chức (`Organization`) có địa chỉ (`address`) chứa chuỗi tìm kiếm `region` (không phân biệt chữ hoa, chữ thường).
- Nếu không có tổ chức nào được tìm thấy hoặc không có lô sản xuất của tổ chức nào trong khoảng thời gian `[fromDate, toDate]`, hệ thống lập tức ném lỗi dữ liệu trống (TC-02).

### 2.2 Quy tắc QTN-11 – Chỉ xuất dữ liệu cho lô đủ điều kiện
Hệ thống bắt buộc phải kiểm duyệt các Lô sản xuất (`ProductionLot`) thỏa mãn các điều kiện dưới đây:
- **Trạng thái hoàn thành:** Lô sản xuất phải ở trạng thái `CLOSED` hoặc `PACKAGED`.
- **Đầy đủ chứng từ minh chứng:** Lịch trình canh tác (`farm_logs`) của lô đó phải có tối thiểu 1 tệp đính kèm (`farm_log_attachments`) cho cả 4 loại hoạt động bắt buộc:
  1. `PLANTING` (Gieo giống/Xuống giống)
  2. `FERTILIZING` (Bón phân)
  3. `PESTICIDE` (Phun thuốc/Bảo vệ thực vật)
  4. `HARVESTING` (Thu hoạch)

### 2.3 Phân quyền chặt chẽ (TC-03)
- Chỉ người dùng có vai trò `VT-05` (Cán bộ quản lý ngành) mới được phép gọi API này.
- Bất kỳ vai trò nào khác (ví dụ `VT-03` - Người ghi sự kiện, `VT-02` - Quản lý HTX) đều bị chặn truy cập và trả về lỗi `403 Forbidden`.

### 2.4 Lưu nhật ký truy cập (Audit Trail)
- Mọi lượt gọi API đều được lưu nhật ký vào bảng `report_access_log` bất kể thành công hay thất bại (nhưng đã xác thực):
  - `user_id`: Người dùng thực hiện gọi API.
  - `organization_id`: Tổ chức của người dùng hiện tại.
  - `target_organization_id`: Đặt bằng tổ chức của người dùng hiện tại (báo cáo phạm vi vùng không có một tổ chức đích cụ thể).
  - `report_name`: Ghi nhận là `"OPEN_DATA_EXPORT"`.
  - `success`: `true` nếu tải file thành công, `false` nếu thất bại.
  - `ip_address`: Địa chỉ IP của client gửi yêu cầu.

---

## 3. Đầu ra tương ứng (Related Endpoints)
- Báo cáo tổng hợp ngành: `GET /api/v1/reports/industry-summary`
- Lịch sử xuất hồ sơ lô hàng: `GET /api/v1/shipments/{shipmentId}/dossier/export`
