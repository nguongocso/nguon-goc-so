# API Docs — Cảnh báo và kiểm soát thu hoạch trước thời gian cách ly (Early Harvest Quarantine Warning)

* **Mã User Story:** `NCL-03-CN-005`
* **Mã Feature:** `NCL-681`
* **Tên tính năng:** Cảnh báo thu hoạch trước thời gian cách ly
* **Git Branch:** `feature/NCL-681-early-harvest-quarantine-warning`
* **Danh sách Subtasks:**
  * `NCL-843` (CV-01): Chốt cách tính ngày đủ điều kiện thu hoạch
  * `NCL-845` (CV-02): Thiết kế cảnh báo và luồng ghi đè có lý do
  * `NCL-847` (CV-03): Phát triển kiểm tra thời gian cách ly khi ghi thu hoạch
  * `NCL-849` (CV-04): Hiển thị cờ thu hoạch sớm trong hồ sơ truy xuất
  * `NCL-851` (CV-05): Kiểm thử quy tắc thời gian cách ly

---

## 1. Tổng quan nghiệp vụ (Business Overview)

### 1.1 Mục tiêu
Bảo vệ an toàn vệ sinh thực phẩm và nâng cao tính minh bạch cho chuỗi nông sản bằng cách:
1. Tự động tính toán **Ngày đủ điều kiện thu hoạch an toàn** (`eligibleHarvestDate`) dựa trên thời gian cách ly (Pre-Harvest Interval - PHI) của tất cả các loại thuốc Bảo vệ thực vật (BVTV / `PESTICIDE`) đã sử dụng trong nhật ký canh tác của Lô sản xuất.
2. Ngăn chặn triệt để **Người ghi sự kiện (`VT-03`)** thu hoạch nông sản khi chưa hết thời gian cách ly.
3. Cho phép **Quản lý hợp tác xã (`VT-02`)** ghi đè thu hoạch sớm trong các trường hợp bất khả kháng (ví dụ: bão lũ, ngập úng, dịch bệnh lây lan) với điều kiện **bắt buộc phải giải trình lý do**.
4. Chặn **Quản trị viên (`VT-01`)** ghi đè sự kiện thu hoạch của HTX theo đúng ma trận phân quyền.
5. Tự động ghi vết kiểm toán (Audit Trail) vào **Nhật ký hoạt động** (`activity_logs`) khi có hành động ghi đè, hoặc ghi nhận nhật ký lỗi (`eventValidationService.logFailedAttempt`) khi có nỗ lực thu hoạch trái phép.
6. Hiển thị nhãn cảnh báo thu hoạch sớm trên Hồ sơ truy xuất nội bộ (kèm lý do) và Hồ sơ tra cứu công khai Public QR (ẩn lý do nội bộ).

---

### 1.2 Công thức tính ngày cách ly (Calculation Engine - NCL-843)

Đối với mỗi lô sản xuất $L$, hệ thống quét toàn bộ danh sách nhật ký canh tác có loại hoạt động `PESTICIDE`:

$$\text{eligibleHarvestDate} = \max_{i \in \text{PESTICIDE logs}} \left( \text{executedDate}_i + \text{quarantineDays}_i \right)$$

* Trong đó:
  * $\text{executedDate}_i$: Ngày thực hiện phun thuốc BVTV (bắt buộc phải có, nếu thiếu $\rightarrow$ ném `BusinessException`).
  * $\text{quarantineDays}_i$: Số ngày cách ly an toàn của loại thuốc tương ứng trong danh mục vật tư chuẩn (`input_materials`).
* **Trường hợp lô không có nhật ký PESTICIDE:** $\text{eligibleHarvestDate} = \text{null}$, $\text{determined} = \text{true}$ (Đủ điều kiện thu hoạch bất kỳ lúc nào).
* **Trường hợp có vật tư ngoài danh mục (`unmatchedMaterials`):** Hệ thống phát Cảnh báo mềm (Soft Warning) để người dùng rà soát, đồng thời vẫn áp dụng ngày cách ly lớn nhất của các vật tư đã nhận diện được.

---

### 1.3 Ma trận phân quyền & Xử lý nghiệp vụ (Permission Matrix - B-01, B-02, B-03)

| Vai trò | Mã Role | Điều kiện $\text{harvestDate} < \text{eligibleHarvestDate}$ | Xử lý Backend | Xử lý Audit / Nhật ký |
| :--- | :---: | :--- | :--- | :--- |
| **Người ghi sự kiện** | `VT-03` | ❌ **BỊ CHẶN HOÀN TOÀN** (Không có quyền ghi đè) | Ném `BusinessException` (HTTP 400) | Ghi nhận `logFailedAttempt` |
| **Quản lý Hợp tác xã** | `VT-02` | ✅ **CHO PHÉP GHI ĐÈ** kèm lý do bắt buộc | Nếu `earlyHarvestReason` rỗng/khoảng trắng $\rightarrow$ Ném `BusinessException`.<br>Nếu có lý do $\rightarrow$ Cho phép tạo sự kiện, gắn `earlyHarvest: true`. | Ghi nhận `ActivityLogEvent` thành công |
| **Quản trị viên hệ thống** | `VT-01` | ❌ **BỊ CHẶN** (Không trực tiếp ghi sự kiện HTX) | Ném `BusinessException` (HTTP 400) | Ghi nhận `logFailedAttempt` |
| **Người tiêu dùng / Công khai** | `VT-06` | Tra cứu mã QR công khai (`Public Trace`) | Trả về `earlyHarvest: true`, `eligibleHarvestDate`. **Ẩn hoàn toàn `earlyHarvestReason`**. | Đảm bảo an toàn dữ liệu nội bộ |

---

## 2. Danh sách API Đặc Tả

```text
1. GET  /api/v1/farm-logs/harvest-eligibility  --> Tính toán & kiểm tra ngày đủ điều kiện thu hoạch
2. POST /api/v1/chain-events/harvest          --> Ghi nhận sự kiện thu hoạch (Web)
3. POST /api/v1/chain-events/mobile           --> Ghi nhận sự kiện thu hoạch (Mobile App)
4. GET  /api/v1/public/trace/{traceCode}      --> Tra cứu hồ sơ truy xuất nguồn gốc công khai (Public QR)
5. GET  /api/v1/chain-events/timeline/{lotId} --> Lấy dòng thời gian sự kiện nội bộ của lô
```

---

## 3. Chi tiết API 1: Tính toán & Kiểm tra điều kiện thu hoạch

Cho phép Frontend và Mobile App kiểm tra trước ngày đủ điều kiện thu hoạch an toàn của một lô sản xuất để hiển thị cảnh báo và cấu hình trạng thái nút bấm form.

### 3.1 Thông tin Endpoint
* **URL:** `/api/v1/farm-logs/harvest-eligibility`
* **Method:** `GET`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header `Authorization: Bearer <token>`
* **Quyền truy cập:** `VT-01` (Admin), `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện)

### 3.2 Query Parameters
| Tên tham số | Kiểu dữ liệu | Bắt buộc | Mô tả | Ví dụ |
| :--- | :---: | :---: | :--- | :--- |
| `productionLotId` | `UUID` | Có (`required`) | ID định danh duy nhất của Lô sản xuất | `5b050d90-be97-467b-a4cb-dcbf3db81dfd` |

### 3.3 Response thành công (200 OK)
**DTO:** `ApiResult<HarvestEligibilityResponse>`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "determined": true,
    "eligibleHarvestDate": "2026-09-04",
    "unmatchedMaterials": []
  },
  "timestamp": "2026-08-28T08:04:26.303Z"
}
```

#### Cấu trúc thuộc tính dữ liệu (`HarvestEligibilityResponse`):
| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :---: | :--- |
| `determined` | `boolean` | `true` nếu tất cả vật tư PESTICIDE đều được đối soát thành công trong danh mục; `false` nếu có vật tư ngoài danh mục. |
| `eligibleHarvestDate` | `LocalDate` (`YYYY-MM-DD`) | Ngày sớm nhất đủ điều kiện thu hoạch an toàn. Trả về `null` nếu lô chưa từng sử dụng thuốc BVTV có thời gian cách ly. |
| `unmatchedMaterials` | `List<String>` | Danh sách tên các vật tư PESTICIDE ghi nhận trong nhật ký nhưng chưa có trong danh mục chuẩn (`input_materials`). |

### 3.4 Trường hợp lỗi thường gặp

#### 🔴 Lỗi 400: Nhật ký thuốc BVTV bị thiếu ngày thực hiện (B-02)
```json
{
  "success": false,
  "status": 400,
  "message": "Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện. Vui lòng bổ sung ngày trước khi thu hoạch.",
  "path": "/api/v1/farm-logs/harvest-eligibility",
  "timestamp": "2026-08-28T08:05:00.000Z"
}
```

#### 🔴 Lỗi 404: Không tìm thấy lô sản xuất
```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy lô sản xuất",
  "path": "/api/v1/farm-logs/harvest-eligibility",
  "timestamp": "2026-08-28T08:05:00.000Z"
}
```

---

## 4. Chi tiết API 2: Ghi nhận sự kiện thu hoạch (Web)

Thực hiện ghi nhận sự kiện thu hoạch cho Lô sản xuất. Backend tự động tính toán lại thời gian cách ly độc lập (Anti-bypass) và kiểm tra phân quyền ghi đè.

### 4.1 Thông tin Endpoint
* **URL:** `/api/v1/chain-events/harvest`
* **Method:** `POST`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header `Authorization: Bearer <token>`
* **Quyền truy cập:** `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện)

### 4.2 Request Body
**DTO:** `RecordHarvestEventRequest`

| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Validate | Mô tả |
| :--- | :---: | :---: | :--- | :--- |
| `productionLotId` | `UUID` | ✓ | `@NotNull` | ID của Lô sản xuất (phải ở trạng thái `APPROVED`). |
| `quantity` | `Double` | ✓ | `@NotNull`, `@Positive` | Sản lượng thu hoạch thực tế (phải > 0). |
| `harvestDate` | `LocalDate` | ✓ | `@NotNull` (`YYYY-MM-DD`) | Ngày thực hiện thu hoạch. Không được vượt quá ngày hiện tại. |
| `earlyHarvestReason` | `String` | Tùy chọn | Bắt buộc khi thu hoạch sớm | Lý do ghi đè thu hoạch trước thời gian cách ly (bắt buộc đối với VT-02 khi thu hoạch sớm). |
| `latitude` | `Double` | | $-90 \le \text{lat} \le 90$ | Tọa độ vĩ độ điểm thu hoạch. |
| `longitude` | `Double` | | $-180 \le \text{lng} \le 180$ | Tọa độ kinh độ điểm thu hoạch. |
| `images` | `List<String>` | | Tối đa 5 URLs | Danh sách đường dẫn ảnh chụp thực địa thu hoạch. |
| `deviceSource` | `String` | | Mặc định `WEB` | Nguồn thiết bị thực hiện (`WEB` / `MOBILE`). |

#### Ví dụ Request (Quản lý HTX VT-02 ghi đè thu hoạch sớm kèm lý do):
```json
{
  "productionLotId": "0b526c7f-7cc2-49c9-b50f-7cba7becb3d4",
  "quantity": 148.5,
  "harvestDate": "2026-08-28",
  "earlyHarvestReason": "Thu hoạch sớm tránh bão số 3 gây ngập úng",
  "latitude": 20.9854,
  "longitude": 105.7985,
  "images": [
    "https://storage.nguongocso.vn/harvest/img_01.jpg"
  ],
  "deviceSource": "WEB"
}
```

### 4.3 Response thành công (201 Created)
**DTO:** `ApiResult<ChainEventResponse>`

```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": "28219a31-7f15-4b0a-bdcd-165f6223cfc7",
    "shipmentId": null,
    "eventType": "HARVEST",
    "eventData": {
      "productionLotId": "0b526c7f-7cc2-49c9-b50f-7cba7becb3d4",
      "productionLotName": "Lô xoài Cát Chu xuất khẩu đợt 8",
      "quantity": 148.5,
      "harvestDate": "2026-08-28",
      "earlyHarvest": true,
      "earlyHarvestReason": "Thu hoạch sớm tránh bão số 3 gây ngập úng",
      "eligibleHarvestDate": "2026-09-04",
      "unmatchedMaterials": [],
      "deviceSource": "WEB",
      "images": [
        "https://storage.nguongocso.vn/harvest/img_01.jpg"
      ]
    },
    "latitude": 20.9854,
    "longitude": 105.7985,
    "recordedAt": "2026-08-28T15:12:29",
    "recordedByName": "Nguyễn Văn Minh",
    "createdAt": "2026-08-28T15:12:29"
  },
  "timestamp": "2026-08-28T15:12:29.123Z"
}
```

### 4.4 Các trường hợp bị chặn & Mã lỗi nghiệp vụ

#### 🔴 Case 1: Người ghi sự kiện (VT-03) cố tình thu hoạch sớm
* **Điều kiện:** `user.roleCode == 'VT-03'` và `harvestDate < eligibleHarvestDate`.
* **Mã lỗi:** `400 Bad Request`
* **Hành vi hệ thống:** Ném ngoại lệ, tự động gọi `eventValidationService.logFailedAttempt` để ghi nhận nhật ký lỗi bảo mật.
```json
{
  "success": false,
  "status": 400,
  "message": "Lô chưa hết thời gian cách ly (ngày đủ điều kiện: 2026-09-04). Người ghi sự kiện không có quyền ghi đè thu hoạch sớm.",
  "path": "/api/v1/chain-events/harvest",
  "timestamp": "2026-08-28T15:10:00.000Z"
}
```

#### 🔴 Case 2: Quản lý HTX (VT-02) thu hoạch sớm nhưng không nhập lý do hoặc lý do chỉ có khoảng trắng
* **Điều kiện:** `user.roleCode == 'VT-02'` và `earlyHarvestReason.trim().isEmpty()`.
* **Mã lỗi:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Lô thu hoạch sớm trước thời gian cách ly (ngày đủ điều kiện: 2026-09-04). Quản lý cần nhập lý do ghi đè bắt buộc.",
  "path": "/api/v1/chain-events/harvest",
  "timestamp": "2026-08-28T15:10:00.000Z"
}
```

#### 🔴 Case 3: Admin (VT-01) cố tình ghi đè thu hoạch của HTX
* **Điều kiện:** `user.roleCode == 'VT-01'` và `harvestDate < eligibleHarvestDate`.
* **Mã lỗi:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm kèm lý do bắt buộc.",
  "path": "/api/v1/chain-events/harvest",
  "timestamp": "2026-08-28T15:10:00.000Z"
}
```

#### 🔴 Case 4: Lô sản xuất chưa được duyệt (`status != APPROVED`)
* **Mã lỗi:** `400 Bad Request`
```json
{
  "success": false,
  "status": 400,
  "message": "Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.",
  "path": "/api/v1/chain-events/harvest",
  "timestamp": "2026-08-28T15:10:00.000Z"
}
```

---

## 5. Chi tiết API 3: Ghi nhận sự kiện thu hoạch qua Mobile App

Đồng bộ hành vi nghiệp vụ 100% giữa Web và Mobile App (Web/Mobile Parity).

### 5.1 Thông tin Endpoint
* **URL:** `/api/v1/chain-events/mobile`
* **Method:** `POST`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header `Authorization: Bearer <token>`
* **Quyền truy cập:** `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện)

### 5.2 Request Body
**DTO:** `RecordMobileEventRequest`

```json
{
  "productionLotId": "0b526c7f-7cc2-49c9-b50f-7cba7becb3d4",
  "eventType": "HARVEST",
  "eventData": {
    "quantity": 148.5,
    "harvestDate": "2026-08-28",
    "earlyHarvestReason": "Thu hoạch sớm tránh mưa ngập từ thiết bị di động"
  },
  "latitude": 20.9854,
  "longitude": 105.7985,
  "recordedAt": "2026-08-28T15:12:00",
  "deviceSource": "MOBILE",
  "images": [
    "https://storage.nguongocso.vn/mobile/upload_01.jpg"
  ]
}
```

### 5.3 Response thành công (201 Created)
Trả về `ApiResult<ChainEventResponse>` tương tự như API Web, có gắn `deviceSource: "MOBILE"`.

---

## 6. Chi tiết API 4: Tra cứu Hồ Sơ Truy Xuất Nguồn Gốc Công Khai (Public QR)

API công khai phục vụ người tiêu dùng quét mã QR (`VT-06`). Bảo vệ an toàn dữ liệu nội bộ bằng cách hiển thị cờ cảnh báo thu hoạch sớm nhưng **không trả `earlyHarvestReason` ra bên ngoài**.

### 6.1 Thông tin Endpoint
* **URL:** `/api/v1/public/trace/{traceCode}`
* **Method:** `GET`
* **Authentication:** **Không yêu cầu** (Công khai cho Người tiêu dùng `VT-06`)

### 6.2 Path Parameters
| Tên tham số | Kiểu dữ liệu | Mô tả | Ví dụ |
| :--- | :---: | :--- | :--- |
| `traceCode` | `String` | Mã định danh tem truy xuất nguồn gốc | `893001000001` |

### 6.3 Response thành công (200 OK)
**DTO:** `ApiResult<PublicTraceResponse>`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "traceCode": "893001000001",
    "productName": "Xoài Cát Chu xuất khẩu",
    "organizationName": "Hợp tác xã Nông sản VietGAP",
    "status": "ACTIVATED",
    "events": [
      {
        "id": "28219a31-7f15-4b0a-bdcd-165f6223cfc7",
        "eventType": "HARVEST",
        "eventTypeName": "Thu hoạch",
        "recordedAt": "2026-08-28T15:12:29",
        "location": "Vùng trồng Xoài Công Nghệ Cao",
        "eventData": {
          "quantity": 148.5,
          "harvestDate": "2026-08-28",
          "earlyHarvest": true,
          "eligibleHarvestDate": "2026-09-04",
          "unmatchedMaterials": []
        }
      }
    ]
  },
  "timestamp": "2026-08-28T15:15:00.000Z"
}
```

> [!IMPORTANT]
> **Bảo mật dữ liệu (Data Sanitization - B-03):**
> Trong mảng `events` của `PublicTraceResponse`, trường `earlyHarvestReason` bị loại bỏ hoàn toàn khỏi `eventData` bởi `PublicTraceServiceImpl.filterEventData(...)`. Người tiêu dùng chỉ thấy nhãn cảnh báo thu hoạch sớm và ngày đủ điều kiện an toàn, không thấy lý do nội bộ của HTX.

---

## 7. Chi tiết API 5: Dòng thời gian sự kiện nội bộ (Internal Timeline)

Phục vụ màn hình quản trị nội bộ của Hợp tác xã, Quản trị viên và Cơ quan quản lý kiểm tra chi tiết.

### 7.1 Thông tin Endpoint
* **URL:** `/api/v1/chain-events/timeline/{productionLotId}`
* **Method:** `GET`
* **Authentication:** Yêu cầu JWT Bearer Token trong Header `Authorization: Bearer <token>`
* **Quyền truy cập:** `VT-01`, `VT-02`, `VT-03`, `VT-04`, `VT-05`

### 7.2 Response thành công (200 OK)
Trả về danh sách sự kiện kèm **đầy đủ thông tin `earlyHarvestReason`** để phục vụ công tác thanh tra, kiểm toán nội bộ.

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "28219a31-7f15-4b0a-bdcd-165f6223cfc7",
      "eventType": "HARVEST",
      "recordedByName": "Nguyễn Văn Minh",
      "recordedAt": "2026-08-28T15:12:29",
      "eventData": {
        "quantity": 148.5,
        "harvestDate": "2026-08-28",
        "earlyHarvest": true,
        "earlyHarvestReason": "Thu hoạch sớm do thiên tai bão lũ",
        "eligibleHarvestDate": "2026-09-04",
        "unmatchedMaterials": []
      }
    }
  ],
  "timestamp": "2026-08-28T15:15:00.000Z"
}
```

---

## 8. Bảng Mã Lỗi (Error Codes Summary)

| HTTP Status | Message | Nguyên nhân & Hướng xử lý |
| :---: | :--- | :--- |
| `400` | `Lô chưa hết thời gian cách ly... Người ghi sự kiện không có quyền ghi đè.` | `VT-03` cố tình gửi yêu cầu thu hoạch trước ngày cách ly. |
| `400` | `Lô thu hoạch sớm trước thời gian cách ly... Quản lý cần nhập lý do ghi đè bắt buộc.` | `VT-02` gửi yêu cầu thu hoạch sớm nhưng để trống trường `earlyHarvestReason`. |
| `400` | `Chỉ Quản lý hợp tác xã (VT-02) mới có quyền ghi đè thu hoạch sớm...` | `VT-01` hoặc vai trò khác ngoài `VT-02` cố tình gửi yêu cầu ghi đè. |
| `400` | `Mục nhật ký sử dụng thuốc BVTV thiếu ngày thực hiện...` | Tồn tại bản ghi nhật ký `PESTICIDE` có `executedDate == null`. Bắt buộc bổ sung ngày trước khi thu hoạch. |
| `400` | `Lô sản xuất chưa được duyệt, không thể ghi sự kiện thu hoạch.` | Lô sản xuất ở trạng thái `DRAFT` hoặc `REJECTED`. |
| `404` | `Không tìm thấy lô sản xuất` | `productionLotId` không tồn tại trong hệ thống. |
| `403` | `Bạn không thuộc tổ chức quản lý của lô sản xuất này.` | Người dùng thuộc tổ chức A nhưng cố tình thao tác trên lô của tổ chức B. |
