# API Docs — Ghi sự kiện ngoài đồng trên thiết bị di động

*Mã User Story: NCL-10-CN-03 Ghi sự kiện ngoài đồng trên thiết bị di động*

---

## Nhật ký thay đổi (Changelog)

| Ngày | Phiên bản | Nội dung thay đổi | Người thực hiện |
| :--- | :--- | :--- | :--- |
| 2026-07-31 | v1.0.0 | Khởi tạo tài liệu đặc tả API ghi nhận sự kiện trên thiết bị di động ngoài đồng | Antigravity |

---

## 1. Thông tin chung

**Mục tiêu**
Cho phép Người ghi sự kiện (`VT-03` / `EVENT_RECORDER`) hoặc Quản lý hợp tác xã (`VT-02` / `COOPERATIVE_MANAGER`) ghi nhận các sự kiện trong chuỗi cung ứng (ví dụ: Thu hoạch, Đóng gói) trực tiếp từ thiết bị di động tại hiện trường (ngoài đồng). 

Hệ thống sẽ thực hiện xác thực quyền của người ghi đối với lô sản xuất được chọn, kiểm tra tính hợp lệ của lô, thu thập tọa độ địa lý (GPS), lưu trữ hình ảnh thực tế và ghi nhận nguồn gốc thiết bị (`deviceSource: MOBILE`) trong dòng thời gian sự kiện.

---

## 2. API: Ghi sự kiện từ thiết bị di động

### 2.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/mobile/chain-events` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 2.2 Request Details

**Request Headers**
```http
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body Schema**

| Location | Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Body | `productionLotId` | UUID | Yes | Phải là UUID hợp lệ của lô sản xuất đang tồn tại và thuộc tổ chức của người dùng. | `"85d91b0c-c3b8-4c1f-bcb0-2b86737d1406"` |
| Body | `eventType` | String | Yes | Phải thuộc danh sách loại sự kiện: `HARVEST`, `PACKAGING`. | `"HARVEST"` |
| Body | `recordedAt` | String (ISO LocalDateTime) | Yes | Định dạng: `YYYY-MM-DDTHH:mm:ss`. Không được là ngày ở tương lai. | `"2026-07-31T08:30:00"` |
| Body | `latitude` | Double | Yes | Vĩ độ của thiết bị khi ghi nhận sự kiện (-90.0 đến 90.0). | `21.028512` |
| Body | `longitude` | Double | Yes | Kinh độ của thiết bị khi ghi nhận sự kiện (-180.0 đến 180.0). | `105.854244` |
| Body | `images` | List\<String\> | Yes | Danh sách URL ảnh chụp thực địa ngoài đồng. Tối thiểu phải có 1 ảnh. | `["https://storage.nguongocso.vn/events/harvest_lot1_01.jpg"]` |
| Body | `deviceSource` | String | No | Mặc định hệ thống tự điền là `"MOBILE"`. | `"MOBILE"` |
| Body | `eventData` | Map\<String, Object\> | Yes | Chứa dữ liệu chi tiết của từng loại sự kiện (Xem cấu trúc chi tiết bên dưới). | (Xem ví dụ bên dưới) |

#### Cấu trúc chi tiết của trường `eventData` theo `eventType`

##### 1. Trường hợp `eventType = HARVEST` (Sự kiện thu hoạch)
| Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- |
| `quantity` | Double | Yes | Phải lớn hơn 0. | `1500.0` |
| `harvestDate` | String (LocalDate) | Yes | Định dạng: `YYYY-MM-DD`. Không được vượt quá ngày hiện tại. | `"2026-07-31"` |

##### 2. Trường hợp `eventType = PACKAGING` (Sự kiện đóng gói)
| Field Name | Data Type | Required | Constraints / Validation | Example |
| :--- | :--- | :--- | :--- | :--- |
| `packagingSpecification` | String | Yes | Không được để trống. Tối đa 255 ký tự. | `"Túi hút chân không 500g, đóng thùng 20 túi"` |
| `packagingDate` | String (LocalDate) | Yes | Định dạng: `YYYY-MM-DD`. Không được vượt quá ngày hiện tại. | `"2026-07-31"` |

---

### 2.3 Request Examples

#### Ví dụ 1: Ghi sự kiện Thu hoạch (HARVEST)
```json
{
  "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
  "eventType": "HARVEST",
  "recordedAt": "2026-07-31T08:30:00",
  "latitude": 20.985412,
  "longitude": 105.798541,
  "images": [
    "https://storage.nguongocso.vn/events/harvest_lot1_01.jpg",
    "https://storage.nguongocso.vn/events/harvest_lot1_02.jpg"
  ],
  "deviceSource": "MOBILE",
  "eventData": {
    "quantity": 1250.5,
    "harvestDate": "2026-07-31"
  }
}
```

#### Ví dụ 2: Ghi sự kiện Đóng gói (PACKAGING)
```json
{
  "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
  "eventType": "PACKAGING",
  "recordedAt": "2026-07-31T09:15:00",
  "latitude": 20.985412,
  "longitude": 105.798541,
  "images": [
    "https://storage.nguongocso.vn/events/packaging_lot1_01.jpg"
  ],
  "deviceSource": "MOBILE",
  "eventData": {
    "packagingSpecification": "Hộp giấy 250g, thùng 40 hộp",
    "packagingDate": "2026-07-31"
  }
}
```

---

### 2.4 Response — Success (`201 Created`)

| Status Code | Khi xảy ra |
| :--- | :--- |
| `201 Created` | Sự kiện được ghi nhận thành công từ thiết bị di động vào dòng thời gian của lô và trạng thái lô được cập nhật tương ứng. |

**Response Example (Success)**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "f5b6c7d8-e9a0-4b1c-8d2e-3f4a5b6c7d8e",
    "shipmentId": null,
    "eventType": "HARVEST",
    "eventData": {
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "productionLotName": "Lô chè xanh VietGAP số 10",
      "quantity": 1250.5,
      "harvestDate": "2026-07-31",
      "images": [
        "https://storage.nguongocso.vn/events/harvest_lot1_01.jpg",
        "https://storage.nguongocso.vn/events/harvest_lot1_02.jpg"
      ],
      "deviceSource": "MOBILE"
    },
    "latitude": 20.985412,
    "longitude": 105.798541,
    "recordedAt": "2026-07-31T08:30:00",
    "recordedByName": "Trần Văn Ghi",
    "createdAt": "2026-07-31T08:38:04"
  },
  "timestamp": "2026-07-31T01:38:04Z"
}
```

---

### 2.5 Response — Error

| Status Code | Error Code (nếu có) | Nguyên nhân |
| :--- | :--- | :--- |
| `400 Bad Request` | `VALIDATION_ERROR` | Thiếu trường bắt buộc, sai định dạng dữ liệu, ngày sự kiện ở tương lai, hoặc thiếu ảnh thực địa. |
| `401 Unauthorized` | - | Token xác thực không hợp lệ hoặc đã hết hạn. |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng không có vai trò phù hợp, hoặc không thuộc tổ chức quản lý lô sản xuất (Không được phân công lô). |
| `404 Not Found` | `LOT_NOT_FOUND` | Không tìm thấy lô sản xuất theo ID được cung cấp. |
| `409 Conflict` | `INVALID_LOT_STATUS` | Trạng thái của lô sản xuất không hợp lệ để ghi nhận sự kiện (ví dụ: Lô chưa duyệt mà ghi thu hoạch). |

#### Error Response Examples

##### Ví dụ 1: Lỗi thiếu loại sự kiện (AC ID: NCL-10-CN-003-TC-02)
* Request thiếu trường `eventType` hoặc `eventType` truyền lên trống.
```json
{
  "success": false,
  "status": 400,
  "message": "Loại sự kiện không được để trống."
}
```

##### Ví dụ 2: Lỗi không có quyền / không được phân công lô (AC ID: NCL-10-CN-003-TC-03 & QTN-07)
* Người ghi sự kiện cố gắng ghi nhận cho một lô thuộc về một HTX (tổ chức) khác.
```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không thuộc tổ chức quản lý của lô sản xuất này."
}
```

##### Ví dụ 3: Lỗi thiếu ảnh chụp thực địa (Ràng buộc di động)
* Thiết bị di động không truyền kèm danh sách hình ảnh thực địa.
```json
{
  "success": false,
  "status": 400,
  "message": "Sự kiện ghi nhận ngoài đồng yêu cầu tối thiểu một hình ảnh thực địa."
}
```

##### Ví dụ 4: Lỗi sai trạng thái lô (Ví dụ ghi thu hoạch cho lô chưa được duyệt)
```json
{
  "success": false,
  "status": 409,
  "message": "Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch."
}
```

---

## 3. Quy tắc nghiệp vụ & Ràng buộc thiết kế (Business Rules)

### 3.1 Quy tắc QTN-07 & Phân quyền
* Chỉ người dùng có vai trò **Quản lý hợp tác xã** (`VT-02`) hoặc **Người ghi sự kiện** (`VT-03`) thuộc tổ chức sở hữu lô sản xuất mới được phép thực hiện ghi sự kiện.
* Hệ thống so sánh: `currentUser.organizationId == productionLot.organizationId`. Nếu không khớp, trả về lỗi `403 Forbidden` nhằm đảm bảo an toàn bảo mật dữ liệu giữa các HTX khác nhau.

### 3.2 Tái sử dụng logic nghiệp vụ cốt lõi (Task NCL-10-CN-003-CV-04)
API di động sẽ gọi lại các logic kiểm tra trạng thái lô sản xuất của hệ thống chung:
* Với sự kiện `HARVEST`: Lô sản xuất phải ở trạng thái `APPROVED` (Đã duyệt). Sau khi ghi nhận thành công, trạng thái lô chuyển sang `HARVESTED` (Đã thu hoạch).
* Với sự kiện `PACKAGING`: Lô sản xuất phải ở trạng thái `HARVESTED` (Đã thu hoạch). Sau khi ghi nhận thành công, trạng thái lô chuyển sang `PACKAGED` (Đã đóng gói).

### 3.3 Lưu nhật ký lịch sử & Nguồn thiết bị (AC ID: NCL-10-CN-003-TC-04)
* Mỗi sự kiện ghi nhận thành công sẽ được lưu trữ trong bảng `chain_events` với trường `event_data` chứa thông tin chi tiết của sự kiện kèm theo:
  - `deviceSource`: `"MOBILE"` (nhận diện nguồn từ thiết bị di động).
  - `images`: Danh sách link ảnh được chụp ngoài đồng.
* Nếu thao tác ghi nhận thất bại do vi phạm ràng buộc hoặc lỗi nghiệp vụ, hệ thống sẽ tự động gọi dịch vụ ghi nhật ký để lưu vết lỗi vào bảng `failed_event_logs` bao gồm: người ghi, loại sự kiện cố gắng ghi, lý do thất bại, ID lô và thời điểm thực hiện.

### 3.4 Ràng buộc vị trí GPS
* Vì sự kiện được ghi nhận trực tiếp ngoài đồng trên di động, tọa độ GPS (`latitude` và `longitude`) là thông tin bắt buộc và phải được định vị thực tế tại thời điểm ghi nhận sự kiện để đảm bảo tính xác thực thông tin nguồn gốc.

---

## 4. Các Endpoint liên quan (Related Endpoints)

* `POST /api/v1/chain-events/harvest` - Ghi nhận sự kiện thu hoạch chung (Bản Web).
* `POST /api/v1/chain-events/packaging` - Ghi nhận sự kiện đóng gói chung (Bản Web).
* `GET /api/v1/production-lots` - Lấy danh sách lô sản xuất thuộc tổ chức (để chọn lô trên giao diện di động).
