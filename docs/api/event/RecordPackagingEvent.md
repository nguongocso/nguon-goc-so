# API Docs — Ghi sự kiện đóng gói & đính chính sự kiện đóng gói

*Mã User Story: NCL-05-CN-003 Ghi sự kiện đóng gói*

---

## 1. Thông tin chung

**Mục tiêu**

Cho phép Người ghi sự kiện (`VT-03` / `EVENT_RECORDER`) hoặc Quản lý hợp tác xã (`VT-02` / `COOPERATIVE_MANAGER`) ghi nhận hoạt động đóng gói cho Lô sản xuất (`ProductionLot`) đã thu hoạch. Khi ghi nhận thành công, lô sản xuất chuyển sang trạng thái đã đóng gói (`PACKAGED`).

Bên cạnh đó, hỗ trợ chức năng đính chính thông tin đóng gói (theo Quy tắc nghiệp vụ **QTN-08** - dòng sự kiện chỉ thêm không sửa). Khi có nhu cầu sửa đổi thông tin đóng gói của một sự kiện đóng gói đã ghi nhận, hệ thống sẽ tạo một sự kiện đính chính mới tham chiếu đến sự kiện gốc, giữ nguyên vẹn dữ liệu gốc nhằm mục đích minh bạch và lưu nhật ký lịch sử.

---

## 2. API 1: Ghi sự kiện đóng gói

Cho phép người ghi nhận thông tin đóng gói của một lô sản xuất đã thu hoạch.

### 2.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/packaging` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 2.2 Request Body

**DTO:** `RecordPackagingEventRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `productionLotId` | UUID | ✓ | `@NotNull` - Vui lòng chọn lô sản xuất. |
| `packagingSpecification` | String | ✓ | `@NotBlank` - Quy cách đóng gói không được để trống.<br>`@Size(max = 255)` - Quy cách đóng gói không được vượt quá 255 ký tự. |
| `packagingDate` | Date (LocalDate) | ✓ | `@NotNull` - Vui lòng chọn ngày đóng gói. <br>Định dạng: `YYYY-MM-DD`. Không được vượt quá ngày hiện tại. |
| `latitude` | Double | | Vĩ độ của địa điểm đóng gói. |
| `longitude` | Double | | Kinh độ của địa điểm đóng gói. |

**Ví dụ Request:**

```json
{
  "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
  "packagingSpecification": "Túi hút chân không 500g, đóng thùng 20 túi/thùng",
  "packagingDate": "2026-07-25",
  "latitude": 21.028512,
  "longitude": 105.854244
}
```

### 2.3 Response thành công (`201 Created`)

**DTO:** `ApiResult<ChainEventResponse>`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "e305bb65-cfda-46df-9d58-45fa97f64245",
    "shipmentId": null,
    "eventType": "PACKAGING",
    "eventData": {
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "productionLotName": "Lô chè Ô Long vụ xuân 2026",
      "packagingSpecification": "Túi hút chân không 500g, đóng thùng 20 túi/thùng",
      "packagingDate": "2026-07-25"
    },
    "latitude": 21.028512,
    "longitude": 105.854244,
    "recordedAt": "2026-07-25T15:30:00",
    "recordedByName": "Nguyễn Văn Ghi",
    "createdAt": "2026-07-25T15:30:00"
  },
  "timestamp": "2026-07-25T08:30:00Z"
}
```

---

## 3. API 2: Đính chính sự kiện đóng gói

Sử dụng khi cần sửa đổi thông tin của một sự kiện đóng gói đã được ghi nhận. API này tạo một sự kiện đính chính mới và liên kết với sự kiện cũ qua `parentEventId`, không làm thay đổi hay xóa sự kiện đóng gói cũ.

### 3.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/packaging/{id}/correct` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 3.2 Path Parameter

* `id` (UUID - Bắt buộc): ID của sự kiện đóng gói gốc cần đính chính.

### 3.3 Request Body

**DTO:** `CorrectPackagingEventRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `packagingSpecification` | String | ✓ | `@NotBlank` - Quy cách đóng gói đính chính không được để trống.<br>`@Size(max = 255)` - Quy cách đóng gói không được vượt quá 255 ký tự. |
| `packagingDate` | Date (LocalDate) | ✓ | `@NotNull` - Vui lòng chọn ngày đóng gói đính chính.<br>Định dạng: `YYYY-MM-DD`. Không được vượt quá ngày hiện tại. |
| `latitude` | Double | | Vĩ độ của địa điểm đóng gói điều chỉnh. |
| `longitude` | Double | | Kinh độ của địa điểm đóng gói điều chỉnh. |
| `correctionReason` | String | ✓ | `@NotBlank` - Lý do đính chính không được để trống.<br>`@Size(max = 500)` - Lý do không được vượt quá 500 ký tự. |

**Ví dụ Request:**

```json
{
  "packagingSpecification": "Túi hút chân không 1kg, đóng thùng 10 túi/thùng",
  "packagingDate": "2026-07-25",
  "latitude": 21.028512,
  "longitude": 105.854244,
  "correctionReason": "Do nhập sai quy cách đóng gói từ túi 500g sang túi 1kg"
}
```

### 3.4 Response thành công (`201 Created`)

**DTO:** `ApiResult<ChainEventResponse>`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "78fa89b2-df82-4148-9ad5-54602f82c4bd",
    "shipmentId": null,
    "eventType": "PACKAGING",
    "eventData": {
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "productionLotName": "Lô chè Ô Long vụ xuân 2026",
      "packagingSpecification": "Túi hút chân không 1kg, đóng thùng 10 túi/thùng",
      "packagingDate": "2026-07-25",
      "correctionReason": "Do nhập sai quy cách đóng gói từ túi 500g sang túi 1kg",
      "parentEventId": "e305bb65-cfda-46df-9d58-45fa97f64245"
    },
    "latitude": 21.028512,
    "longitude": 105.854244,
    "recordedAt": "2026-07-25T15:45:00",
    "recordedByName": "Nguyễn Văn Ghi",
    "createdAt": "2026-07-25T15:45:00"
  },
  "timestamp": "2026-07-25T08:45:00Z"
}
```

---

## 4. Quy tắc nghiệp vụ (Business Rules)

### 4.1 Kiểm tra ràng buộc dữ liệu (Bean Validation)

Hệ thống tự động kiểm tra định dạng và dữ liệu đầu vào. Nếu vi phạm, trả về `400 Bad Request` cùng với thông điệp cụ thể:
* Thiếu `productionLotId` khi tạo mới: `"Vui lòng chọn lô sản xuất"`.
* Thiếu/Rỗng `packagingSpecification`: `"Quy cách đóng gói không được để trống"`.
* Quy cách đóng gói > 255 ký tự: `"Quy cách đóng gói không được vượt quá 255 ký tự"`.
* Thiếu `packagingDate`: `"Vui lòng chọn ngày đóng gói"`.
* `packagingDate` là ngày ở tương lai: `"Ngày đóng gói không được là ngày ở tương lai"`.
* Thiếu/Rỗng `correctionReason` khi đính chính: `"Lý do đính chính không được để trống"`.
* Lý do đính chính > 500 ký tự: `"Lý do không được vượt quá 500 ký tự"`.

### 4.2 Kiểm tra vai trò người dùng (Role Authorization)

Chỉ cho phép người dùng có vai trò `VT-02` (Quản lý HTX) hoặc `VT-03` (Người ghi sự kiện) thực hiện.
* Nếu sai vai trò, trả về `403 Forbidden`:
  ```json
  {
    "success": false,
    "status": 403,
    "message": "Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện."
  }
  ```

### 4.3 Kiểm tra quyền quản lý tổ chức (Organization Verification)

Người dùng thực hiện phải thuộc cùng tổ chức sở hữu lô sản xuất:
`currentUser.organizationId == productionLot.organizationId`
* Nếu không trùng khớp tổ chức, trả về `403 Forbidden`:
  ```json
  {
    "success": false,
    "status": 403,
    "message": "Bạn không thuộc tổ chức quản lý của lô sản xuất này."
  }
  ```

### 4.4 Kiểm tra sự tồn tại của lô sản xuất

* Nếu không tìm thấy lô sản xuất theo `productionLotId` được truyền vào, trả về `404 Not Found`:
  ```json
  {
    "success": false,
    "status": 404,
    "message": "Không tìm thấy lô sản xuất."
  }
  ```

### 4.5 Kiểm tra trạng thái lô sản xuất khi ghi sự kiện

* Chỉ cho phép ghi sự kiện đóng gói khi lô sản xuất đang ở trạng thái `HARVESTED` (Đã thu hoạch).
* Nếu lô sản xuất có trạng thái khác (ví dụ: `DRAFT`, `PENDING`, `APPROVED`, `PACKAGED`, `CLOSED`), trả về `409 Conflict`:
  ```json
  {
    "success": false,
    "status": 409,
    "message": "Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch."
  }
  ```

### 4.6 Quy tắc ngày đóng gói so với ngày thu hoạch

* Ngày đóng gói (`packagingDate`) của sự kiện phải bằng hoặc sau ngày thu hoạch (`harvestDate`) của lô sản xuất đó.
* Nếu `packagingDate` đứng trước `harvestDate`, hệ thống chặn và trả về `400 Bad Request`:
  ```json
  {
    "success": false,
    "status": 400,
    "message": "Ngày đóng gói phải sau hoặc bằng ngày thu hoạch của lô sản xuất."
  }
  ```

### 4.7 Quy tắc QTN-08: Đính chính sự kiện

Đối với sự kiện đính chính đóng gói:
1. **Tìm kiếm sự kiện gốc**: Tìm sự kiện theo `id` truyền trên URL. Nếu không thấy, trả về `404 Not Found`:
   `"Không tìm thấy sự kiện đóng gói cần đính chính."`
2. **Kiểm tra kiểu sự kiện**: Sự kiện gốc tìm thấy phải có `eventType` là `PACKAGING`. Nếu không phải, trả về `400 Bad Request`:
   `"Sự kiện gốc không phải là sự kiện đóng gói."`
3. **Giữ nguyên sự kiện gốc**: Không thực hiện chỉnh sửa dữ liệu, trạng thái hay xóa dòng dữ liệu gốc trong DB.
4. **Tạo sự kiện đính chính mới**:
   * Tạo bản ghi `ChainEvent` mới với `isCorrection = true`.
   * Liên kết sự kiện mới này đến sự kiện gốc bằng cách gán `parentEvent` là thực thể sự kiện gốc.
   * Cập nhật `eventData` dạng JSON chứa các thuộc tính mới được đính chính, bao gồm cả lý do đính chính (`correctionReason`) và ID của sự kiện cha (`parentEventId`).
   * Trạng thái của lô sản xuất liên quan vẫn được duy trì là `PACKAGED`.

---

## 5. Luồng xử lý chi tiết phía Backend

### 5.1 Ghi sự kiện đóng gói mới

```
Client
  │
  ▼
POST /api/v1/chain-events/packaging
  │
  ▼
Bean Validation (Request Body)
  │
  ▼
Kiểm tra vai trò người dùng (VT-02, VT-03)
  │
  ▼
Truy vấn ProductionLot từ DB
  │
  ▼
Kiểm tra trùng khớp tổ chức (Organization) của Lô và Người ghi
  │
  ▼
Kiểm tra trạng thái Lô sản xuất (phải là HARVESTED)
  │
  ▼
Kiểm tra ngày đóng gói >= ngày thu hoạch của Lô
  │
  ▼
Cập nhật trạng thái Lô sản xuất thành PACKAGED -> Lưu DB
  │
  ▼
Tạo ChainEvent mới (eventType = PACKAGING, isCorrection = false, parentEvent = null)
  │
  ▼
Lưu ChainEvent vào DB
  │
  ▼
Trả về kết quả ChainEventResponse (HTTP 201 Created)
```

### 5.2 Ghi sự kiện đính chính đóng gói

```
Client
  │
  ▼
POST /api/v1/chain-events/packaging/{id}/correct
  │
  ▼
Bean Validation (Request Body & Path Variable)
  │
  ▼
Kiểm tra vai trò người dùng (VT-02, VT-03)
  │
  ▼
Truy vấn ChainEvent gốc (parentEvent) theo {id} từ DB
  │
  ▼
Kiểm tra ChainEvent gốc có eventType == PACKAGING
  │
  ▼
Trích xuất ProductionLot liên quan từ eventData của sự kiện gốc và truy vấn DB
  │
  ▼
Kiểm tra trùng khớp tổ chức (Organization) của Lô liên quan và Người đính chính
  │
  ▼
Kiểm tra ngày đóng gói đính chính >= ngày thu hoạch của Lô
  │
  ▼
Tạo ChainEvent đính chính mới (eventType = PACKAGING, isCorrection = true, parentEvent = ChainEvent gốc)
  │
  ▼
Lưu ChainEvent đính chính vào DB (Sự kiện gốc giữ nguyên vẹn không đổi)
  │
  ▼
Trả về kết quả ChainEventResponse (HTTP 201 Created)
```
