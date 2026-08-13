# API Docs — Ghi sự kiện sơ chế & phân loại

*Mã User Story: NCL-11-CN-001 Ghi sự kiện sơ chế và phân loại*

---

## 1. Thông tin chung

**Mục tiêu**

Cho phép Người ghi sự kiện (`VT-03` / `EVENT_RECORDER`) hoặc Quản lý hợp tác xã (`VT-02` / `COOPERATIVE_MANAGER`) ghi nhận hoạt động sơ chế và phân loại cho Lô sản xuất (`ProductionLot`) đã thu hoạch. Khi ghi nhận thành công, lô sản xuất chuyển sang trạng thái đã sơ chế (`PREPROCESSED`), dòng thời gian sự kiện bổ sung một mốc sự kiện sơ chế (`PREPROCESSING`) đính kèm tỷ lệ hao hụt (%) đã được tự động tính toán.

Bên cạnh đó, hỗ trợ chức năng đính chính thông tin sơ chế và phân loại (theo Quy tắc nghiệp vụ **QTN-08** - dòng sự kiện chỉ thêm không sửa). Khi có nhu cầu sửa đổi thông tin của một sự kiện sơ chế đã ghi nhận, hệ thống sẽ tạo một sự kiện đính chính mới tham chiếu đến sự kiện gốc, giữ nguyên vẹn dữ liệu gốc nhằm mục đích minh bạch và lưu nhật ký lịch sử.

---

## 2. API 1: Ghi sự kiện sơ chế và phân loại

Cho phép người ghi nhận thông tin sơ chế, phân loại theo hạng và tỷ lệ hao hụt của một lô sản xuất đã thu hoạch.

### 2.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/preprocessing` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 2.2 Request Body

**DTO:** `RecordPreprocessingEventRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `productionLotId` | UUID | ✓ | `@NotNull` - Vui lòng chọn lô sản xuất. |
| `inputQuantity` | Double | ✓ | `@NotNull` - Vui lòng nhập khối lượng đưa vào sơ chế.<br>`@Positive` - Khối lượng vào sơ chế phải lớn hơn 0. |
| `outputQuantity` | Double | ✓ | `@NotNull` - Vui lòng nhập khối lượng sau sơ chế.<br>`@PositiveOrZero` - Khối lượng sau sơ chế phải lớn hơn hoặc bằng 0. |
| `grade` | String | | Hạng phân loại nông sản (Ví dụ: "Loại 1", "Loại 2", "Hạng A", "Hạng B").<br>`@Size(max = 100)` - Hạng phân loại không được vượt quá 100 ký tự. |
| `processingMethod` | String | | Mô tả/ghi chú cách thức sơ chế (Ví dụ: "Rửa qua bể sục ozone, gọt gốc, sấy bớt nước").<br>`@Size(max = 500)` - Mô tả cách sơ chế không được vượt quá 500 ký tự. |
| `preprocessingDate` | Date (LocalDate) | ✓ | `@NotNull` - Vui lòng chọn ngày sơ chế.<br>Định dạng: `YYYY-MM-DD`. Không được vượt quá ngày hiện tại và không được trước ngày thu hoạch. |
| `images` | List<String> | | Danh sách đường dẫn URL hình ảnh sơ chế & phân loại thực tế. |
| `latitude` | Double | | Vĩ độ của địa điểm sơ chế. |
| `longitude` | Double | | Kinh độ của địa điểm sơ chế. |

**Ví dụ Request:**

```json
{
  "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
  "inputQuantity": 1000.0,
  "outputQuantity": 900.0,
  "grade": "Hạng A",
  "processingMethod": "Rửa sạch qua bể sục ozone, loại bỏ phần nông sản dập hỏng và sấy khô bớt nước",
  "preprocessingDate": "2026-07-26",
  "images": [
    "https://cdn.nguongocso.vn/images/soche-1.jpg",
    "https://cdn.nguongocso.vn/images/soche-2.jpg"
  ],
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
    "id": "c1a8d42e-13b7-4a92-8e10-9b48f9d12345",
    "shipmentId": null,
    "eventType": "PREPROCESSING",
    "eventData": {
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "productionLotName": "Lô chè Ô Long vụ xuân 2026",
      "inputQuantity": 1000.0,
      "outputQuantity": 900.0,
      "lossRate": 10.0,
      "grade": "Hạng A",
      "processingMethod": "Rửa sạch qua bể sục ozone, loại bỏ phần nông sản dập hỏng và sấy khô bớt nước",
      "preprocessingDate": "2026-07-26",
      "images": [
        "https://cdn.nguongocso.vn/images/soche-1.jpg",
        "https://cdn.nguongocso.vn/images/soche-2.jpg"
      ],
      "deviceSource": "WEB"
    },
    "latitude": 21.028512,
    "longitude": 105.854244,
    "recordedAt": "2026-07-26T10:00:00",
    "recordedByName": "Nguyễn Văn Ghi",
    "createdAt": "2026-07-26T10:00:00"
  },
  "timestamp": "2026-07-26T03:00:00Z"
}
```

---

## 3. API 2: Đính chính sự kiện sơ chế và phân loại

Sử dụng khi cần sửa đổi thông tin của một sự kiện sơ chế đã được ghi nhận. API này tạo một sự kiện đính chính mới và liên kết với sự kiện cũ qua `parentEventId`, giữ nguyên vẹn sự kiện cũ để đảm bảo tính minh bạch theo quy tắc **QTN-08**.

### 3.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `POST` |
| **Endpoint** | `/api/v1/chain-events/preprocessing/{id}/correct` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 3.2 Path Parameter

* `id` (UUID - Bắt buộc): ID của sự kiện sơ chế gốc cần đính chính.

### 3.3 Request Body

**DTO:** `CorrectPreprocessingEventRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `inputQuantity` | Double | ✓ | `@NotNull` - Vui lòng nhập khối lượng đưa vào sơ chế.<br>`@Positive` - Khối lượng vào sơ chế phải lớn hơn 0. |
| `outputQuantity` | Double | ✓ | `@NotNull` - Vui lòng nhập khối lượng sau sơ chế.<br>`@PositiveOrZero` - Khối lượng sau sơ chế phải lớn hơn hoặc bằng 0. |
| `grade` | String | | Hạng phân loại đính chính.<br>`@Size(max = 100)` - Hạng phân loại không được vượt quá 100 ký tự. |
| `processingMethod` | String | | Mô tả/ghi chú cách sơ chế đính chính.<br>`@Size(max = 500)` - Mô tả cách sơ chế không được vượt quá 500 ký tự. |
| `preprocessingDate` | Date (LocalDate) | ✓ | `@NotNull` - Vui lòng chọn ngày sơ chế đính chính.<br>Định dạng: `YYYY-MM-DD`. Không được vượt quá ngày hiện tại. |
| `latitude` | Double | | Vĩ độ của địa điểm sơ chế điều chỉnh. |
| `longitude` | Double | | Kinh độ của địa điểm sơ chế điều chỉnh. |
| `correctionReason` | String | ✓ | `@NotBlank` - Lý do đính chính không được để trống.<br>`@Size(max = 500)` - Lý do không được vượt quá 500 ký tự. |

**Ví dụ Request:**

```json
{
  "inputQuantity": 1000.0,
  "outputQuantity": 920.0,
  "grade": "Hạng A",
  "processingMethod": "Rửa sạch qua bể sục ozone và sấy nhẹ",
  "preprocessingDate": "2026-07-26",
  "latitude": 21.028512,
  "longitude": 105.854244,
  "correctionReason": "Cân lại khối lượng sau sơ chế chính xác là 920kg thay vì 900kg"
}
```

### 3.4 Response thành công (`201 Created`)

**DTO:** `ApiResult<ChainEventResponse>`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "f5892d11-b456-4c99-b123-7a8901234567",
    "shipmentId": null,
    "eventType": "PREPROCESSING",
    "eventData": {
      "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
      "productionLotName": "Lô chè Ô Long vụ xuân 2026",
      "inputQuantity": 1000.0,
      "outputQuantity": 920.0,
      "lossRate": 8.0,
      "grade": "Hạng A",
      "processingMethod": "Rửa sạch qua bể sục ozone và sấy nhẹ",
      "preprocessingDate": "2026-07-26",
      "correctionReason": "Cân lại khối lượng sau sơ chế chính xác là 920kg thay vì 900kg",
      "parentEventId": "c1a8d42e-13b7-4a92-8e10-9b48f9d12345"
    },
    "latitude": 21.028512,
    "longitude": 105.854244,
    "recordedAt": "2026-07-26T10:15:00",
    "recordedByName": "Nguyễn Văn Ghi",
    "createdAt": "2026-07-26T10:15:00"
  },
  "timestamp": "2026-07-26T03:15:00Z"
}
```

---

## 4. Quy tắc nghiệp vụ (Business Rules) & Test Cases Mapping

### 4.1 Kiểm tra ràng buộc dữ liệu (Bean Validation)

Hệ thống tự động kiểm tra định dạng và dữ liệu đầu vào. Nếu vi phạm, trả về `400 Bad Request` cùng với thông điệp cụ thể:
* Thiếu `productionLotId` khi tạo mới: `"Vui lòng chọn lô sản xuất"`.
* Thiếu `inputQuantity`: `"Vui lòng nhập khối lượng đưa vào sơ chế"`.
* `inputQuantity` <= 0: `"Khối lượng vào sơ chế phải lớn hơn 0"`.
* Thiếu `outputQuantity`: `"Vui lòng nhập khối lượng sau sơ chế"`.
* `outputQuantity` < 0: `"Khối lượng sau sơ chế phải lớn hơn hoặc bằng 0"`.
* Thiếu `preprocessingDate`: `"Vui lòng chọn ngày sơ chế"`.
* `preprocessingDate` là ngày ở tương lai: `"Ngày sơ chế không được là ngày ở tương lai"`.
* Thiếu/Rỗng `correctionReason` khi đính chính: `"Lý do đính chính không được để trống"`.
* Lý do đính chính > 500 ký tự: `"Lý do không được vượt quá 500 ký tự"`.

---

### 4.2 Kiểm tra khối lượng sau sơ chế so với khối lượng vào (TC-02)

* Khối lượng sau sơ chế (`outputQuantity`) **không được vượt quá** khối lượng đưa vào sơ chế (`inputQuantity`).
* Nếu `outputQuantity > inputQuantity`, hệ thống từ chối và trả về `400 Bad Request`:
  ```json
  {
    "success": false,
    "status": 400,
    "message": "Khối lượng sau sơ chế không được lớn hơn khối lượng vào."
  }
  ```

---

### 4.3 Kiểm tra trạng thái lô sản xuất (TC-03)

* Chỉ cho phép ghi nhận sự kiện sơ chế khi lô sản xuất đang ở trạng thái **`HARVESTED`** (Đã thu hoạch).
* Nếu lô sản xuất đang ở các trạng thái khác (ví dụ: `APPROVED`, `DRAFT`, `PENDING`, `PREPROCESSED`, `PACKAGED`, `CLOSED`), hệ thống từ chối và trả về `400 Bad Request` / `409 Conflict`:
  ```json
  {
    "success": false,
    "status": 400,
    "message": "Lô sản xuất chưa được thu hoạch, không thể ghi nhận sự kiện sơ chế."
  }
  ```

---

### 4.4 Kiểm tra quyền truy cập & Tổ chức (TC-04)

* **Vai trò:** Chỉ người dùng có vai trò `VT-02` (Quản lý HTX) hoặc `VT-03` (Người ghi sự kiện) mới có quyền ghi sự kiện.
  * Nếu không đúng vai trò, trả về `403 Forbidden`: `"Chỉ thành viên được cấp quyền trong tổ chức mới được ghi sự kiện."`
* **Tổ chức (QTN-05):** Người dùng phải thuộc cùng tổ chức quản lý lô sản xuất (`currentUser.organizationId == productionLot.organizationId`).
  * Nếu khác tổ chức, hệ thống từ chối và không tạo sự kiện, trả về `403 Forbidden`:
  ```json
  {
    "success": false,
    "status": 403,
    "message": "Bạn không thuộc tổ chức quản lý của lô sản xuất này."
  }
  ```

---

### 4.5 Tự động tính toán Tỷ lệ hao hụt (%) (TC-01)

Hệ thống tự động tính tỷ lệ hao hụt theo công thức:
$$\text{lossRate} = \text{round}\left( \frac{\text{inputQuantity} - \text{outputQuantity}}{\text{inputQuantity}} \times 100, 2 \right)$$
* Đính kèm `lossRate` vào thuộc tính `eventData` của sự kiện.
* Cập nhật trạng thái lô sản xuất từ `HARVESTED` $\rightarrow$ `PREPROCESSED`.
* Cập nhật `actualQuantity` của lô sản xuất bằng `outputQuantity`.

---

### 4.6 Quy tắc QTN-08: Đính chính sự kiện

Đối với sự kiện đính chính sơ chế:
1. Tìm sự kiện gốc theo `id` trên URL. Nếu không thấy, trả về `404 Not Found`: `"Không tìm thấy sự kiện sơ chế cần đính chính."`.
2. Kiểm tra sự kiện gốc phải có `eventType == PREPROCESSING`. Nếu không đúng, trả về `400 Bad Request`: `"Sự kiện gốc không phải là sự kiện sơ chế."`.
3. Giữ nguyên dữ liệu sự kiện gốc trong CSDL (không chỉnh sửa hay xóa).
4. Tạo bản ghi `ChainEvent` mới với `isCorrection = true` và `parentEvent` là sự kiện gốc.
5. Cập nhật lại `actualQuantity` của lô sản xuất theo `outputQuantity` đính chính mới và cập nhật `lossRate` mới trong `eventData`.

---

## 5. Luồng xử lý chi tiết phía Backend

### 5.1 Ghi sự kiện sơ chế và phân loại mới

```
Client
  │
  ▼
POST /api/v1/chain-events/preprocessing
  │
  ▼
Bean Validation (inputQuantity > 0, outputQuantity >= 0,...)
  │
  ▼
Kiểm tra vai trò người dùng (VT-02, VT-03)
  │
  ▼
Truy vấn ProductionLot từ DB theo productionLotId
  │
  ▼
Kiểm tra trùng khớp tổ chức (Organization) giữa Lô và Người ghi (TC-04)
  │
  ▼
Kiểm tra trạng thái Lô sản xuất (phải là HARVESTED) (TC-03)
  │
  ▼
Kiểm tra khối lượng: outputQuantity <= inputQuantity (TC-02)
  │
  ▼
Kiểm tra ngày sơ chế >= ngày thu hoạch của Lô
  │
  ▼
Tính Tỷ lệ hao hụt (%) = (inputQuantity - outputQuantity) / inputQuantity * 100 (TC-01)
  │
  ▼
Cập nhật Lô sản xuất: status = PREPROCESSED, actualQuantity = outputQuantity -> Lưu DB
  │
  ▼
Tạo ChainEvent mới (eventType = PREPROCESSING, isCorrection = false, parentEvent = null)
  │
  ▼
Lưu ChainEvent vào DB
  │
  ▼
Trả về kết quả ChainEventResponse (HTTP 201 Created)
```

### 5.2 Ghi sự kiện đính chính sơ chế

```
Client
  │
  ▼
POST /api/v1/chain-events/preprocessing/{id}/correct
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
Kiểm tra ChainEvent gốc có eventType == PREPROCESSING
  │
  ▼
Trích xuất ProductionLot liên quan và truy vấn DB
  │
  ▼
Kiểm tra trùng khớp tổ chức (Organization) giữa Lô và Người đính chính
  │
  ▼
Kiểm tra khối lượng đính chính: outputQuantity <= inputQuantity
  │
  ▼
Tính lại Tỷ lệ hao hụt (%) mới và cập nhật actualQuantity của Lô
  │
  ▼
Tạo ChainEvent đính chính mới (eventType = PREPROCESSING, isCorrection = true, parentEvent = ChainEvent gốc)
  │
  ▼
Lưu ChainEvent đính chính vào DB (Sự kiện gốc giữ nguyên vẹn không đổi)
  │
  ▼
Trả về kết quả ChainEventResponse (HTTP 201 Created)
```
