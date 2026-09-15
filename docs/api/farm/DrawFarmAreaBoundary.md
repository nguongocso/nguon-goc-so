# NCL-02-CN-008 – Khoanh ranh giới vùng trồng trên bản đồ

> **Jira Story:** `NCL-713`
>
> **Mã Story dự án:** `NCL-02-CN-008`
>
> **Jira Task hiện tại:** `NCL-865` / `NCL-02-CN-008-CV-03`
>
> **Epic:** `NCL-02` (Khai báo vùng trồng và lô sản xuất)
>
> **Tài liệu:** API Contract / Contract-first
>
> **Trạng thái:** Proposed
>
> **Quyết định CV-01:** Đã chốt ngày 15/09/2026
>
> **Quyết định CV-02:** Đã chốt ngày 15/09/2026
>
> **Quyết định CV-03:** Đã chốt ngày 15/09/2026
>
> **Phạm vi công việc:** `NCL-02-CN-008-CV-01` đến `CV-05`
>
> **Phụ thuộc:** `NCL-02-CN-001`, `NCL-02-CN-005` (cập nhật vùng trồng), `NCL-06-CN-002` (bản đồ chuỗi cung ứng)

---

## 1. Mục tiêu

Cho phép **Quản lý hợp tác xã (`VT-02`)** khoanh ranh giới vùng trồng trên bản đồ thay vì chỉ nhập một điểm tọa độ đơn lẻ (`location POINT`).
- Hồ sơ truy xuất nguồn gốc chứng minh được tọa độ thực địa và quy mô không gian thật của vùng trồng.
- Tự động tính diện tích thực tế từ ranh giới hình học và đối chiếu với diện tích khai báo; cảnh báo khi mức chênh lệch vượt ngưỡng cấu hình, mặc định 30%.
- Hiển thị ranh giới vùng trồng ở chế độ chỉ xem (`read-only`) trên trang tra cứu công khai cho người tiêu dùng và trên bảng phân tích theo vùng trồng.
- Lưu trữ lịch sử phiên bản ranh giới trong Nhật ký hoạt động (`ActivityLog`) mỗi khi có điều chỉnh.

---

## 2. Nguồn yêu cầu

| Thành phần | Mã / Nguồn | Nội dung tóm tắt |
|---|---|---|
| **Jira Story** | `NCL-713` | Khoanh ranh giới vùng trồng trên bản đồ |
| **Jira Task CV-01** | `NCL-870` / `NCL-02-CN-008-CV-01` | Chốt cách khoanh ranh giới và ngưỡng chênh lệch diện tích |
| **Jira Task CV-02** | `NCL-868` / `NCL-02-CN-008-CV-02` | Thiết kế dữ liệu ranh giới vùng trồng: tọa độ đỉnh, diện tích tính toán và phiên bản cũ/mới |
| **Jira Task CV-03** | `NCL-865` / `NCL-02-CN-008-CV-03` | Thiết kế màn hình khoanh ranh giới trên bản đồ và luồng xác nhận chênh lệch diện tích |
| **User Story** | `NCL-02-CN-008` | Khoanh ranh giới vùng trồng trên bản đồ thay vì chỉ nhập một điểm tọa độ |
| **Vai trò** | `VT-02` (Quản lý hợp tác xã) | Toàn quyền thiết lập và chỉnh sửa ranh giới vùng trồng thuộc tổ chức |
| **Quy tắc** | `QTN-01` | Cách ly dữ liệu: Tổ chức chỉ được thao tác trên vùng trồng của mình |
| **Quy tắc** | `QTN-12` | Tra cứu công khai: Người tiêu dùng chỉ xem, không có quyền sửa đổi ranh giới |
| **Quy tắc hình học** | `CV-01`, `CV-04` | Tối thiểu 3 đỉnh; các cạnh không được cắt nhau (Simple Polygon); vĩ độ [-90, 90], kinh độ [-180, 180] |
| **Quy tắc diện tích** | `CV-01`, `TC-03` | Cảnh báo khi diện tích tính từ ranh giới lệch > 30% so với diện tích khai báo; yêu cầu xác nhận trước khi lưu |
| **Quy tắc audit** | `CV-02` | Mọi lần cập nhật ranh giới đều lưu phiên bản cũ và mới vào `ActivityLog` |
| **Tiêu chí TC-01** | `TC-01` | Luồng thành công: Vùng trồng 1 ha, khoanh 4 đỉnh, hệ thống lưu ranh giới và hiển thị diện tích tính được |
| **Tiêu chí TC-02** | `TC-02` | Dữ liệu không hợp lệ: Đánh dấu < 3 đỉnh, hệ thống từ chối (HTTP 400) |
| **Tiêu chí TC-03** | `TC-03` | Ngoại lệ diện tích: Diện tích lệch > 30%, cảnh báo và yêu cầu xác nhận trước khi lưu |
| **Tiêu chí TC-04** | `TC-04` | Hiển thị công khai: Trang tra cứu hiển thị ranh giới vùng trồng dạng read-only |

---

## 3. Công việc liên quan và thứ tự triển khai

1. **`NCL-02-CN-008-CV-01` (Phân tích nghiệp vụ):**
   Chốt cách nhập và khép kín ranh giới; số đỉnh phân biệt tối thiểu = 3; backend tính diện tích trắc địa trên WGS84 và chuyển đổi về hecta; ngưỡng cảnh báo là cấu hình, mặc định 30%.
2. **`NCL-02-CN-008-CV-02` (Thiết kế dữ liệu):**
   Bổ sung trường lưu trữ hình học `boundary` (Polygon SRID 4326), `calculated_area` (Decimal) vào bảng `farm_areas`. Bắn sự kiện `ActivityLogEvent` để lưu lịch sử phiên bản.
3. **`NCL-02-CN-008-CV-03` (Thiết kế giao diện):**
   Dựng công cụ vẽ đa giác trên nền Leaflet, hỗ trợ chấm đỉnh, kéo đỉnh và dán danh sách tọa độ từ clipboard; tính diện tích tức thời; hiển thị hộp thoại xác nhận khi lệch vượt ngưỡng cấu hình.
4. **`NCL-02-CN-008-CV-04` (Phát triển máy chủ):**
   Kiểm tra tính hợp lệ đa giác (Simple Polygon, no self-intersection, >= 3 đỉnh); tính diện tích; kiểm tra ngưỡng cấu hình và cờ `confirmed`; lưu trữ và audit log.
5. **`NCL-02-CN-008-CV-05` (Tích hợp tra cứu công khai):**
   Tích hợp thông tin ranh giới vùng trồng vào `PublicTraceResponse` và hiển thị trực quan trên `RouteMap` ở chế độ read-only.

### 3.1. Quyết định đã chốt cho CV-01

| Nội dung | Quyết định |
|---|---|
| Cách khoanh ranh giới | Người dùng chấm/kéo đỉnh trên bản đồ hoặc dán từng dòng tọa độ theo dạng `latitude, longitude`. Hai cách tạo cùng một danh sách đỉnh có thứ tự. |
| Số đỉnh | Tối thiểu 3 đỉnh **phân biệt**. Điểm đầu không lặp lại ở cuối request; backend tự khép kín vòng polygon khi kiểm tra và lưu. |
| Dữ liệu không hợp lệ | Từ chối tọa độ ngoài miền hợp lệ, đỉnh liên tiếp trùng nhau, dưới 3 đỉnh phân biệt, polygon tự cắt hoặc có diện tích bằng 0. `confirmed` không bỏ qua các lỗi này. |
| Thứ tự tọa độ | API dùng object `{ latitude, longitude }`; khi tạo geometry, trục X là `longitude`, trục Y là `latitude`, hệ tọa độ WGS84/SRID 4326. |
| Diện tích chính thức | Backend tính diện tích trắc địa trên WGS84. Kết quả chuẩn hóa sang hecta theo `1 ha = 10.000 m²`, giữ độ chính xác khi tính và làm tròn `HALF_UP` đến 4 chữ số thập phân khi lưu/trả API. |
| Diện tích xem trước | Frontend có thể tính tức thời để hỗ trợ thao tác, nhưng kết quả backend trả về là giá trị chính thức dùng để lưu và so sánh. |
| Ngưỡng cảnh báo | Cấu hình `app.farm-area.boundary-deviation-threshold-percent`, mặc định `30.0`; biến môi trường `FARM_AREA_BOUNDARY_DEVIATION_THRESHOLD_PERCENT`. Không hard-code trong service. |
| Điều kiện cảnh báo | Cảnh báo khi `deviationPercentage > thresholdPercentage`. Bằng đúng ngưỡng thì được lưu không cần xác nhận. |
| Xác nhận vượt ngưỡng | Nếu vượt ngưỡng và `confirmed=false`, không lưu và trả `409 Conflict` cùng số liệu so sánh. Gửi lại cùng ranh giới với `confirmed=true` mới cho phép lưu; backend luôn tính lại trước khi lưu. |

Diện tích khai báo dùng trong phép so sánh là `farm_areas.area`, hiện đã được backend chuẩn hóa về hecta và bắt buộc lớn hơn 0. Công thức:

$$
\text{deviationPercentage}
= \frac{|\text{calculatedAreaHa} - \text{declaredAreaHa}|}{\text{declaredAreaHa}} \times 100
$$

### 3.2. Quyết định đã chốt cho CV-02

| Nội dung | Quyết định |
|---|---|
| Ranh giới hiện hành | Lưu trực tiếp trên `farm_areas.boundary` bằng kiểu `POLYGON` giới hạn SRID 4326. Entity ánh xạ sang `org.locationtech.jts.geom.Polygon`; mọi geometry được tạo với X = kinh độ, Y = vĩ độ và `SRID = 4326`. |
| Danh sách đỉnh | API giữ danh sách có thứ tự dưới dạng `{ latitude, longitude }`. Khi lưu, backend thêm lại đỉnh đầu để tạo `LinearRing` khép kín; khi trả API, backend bỏ tọa độ đóng vòng cuối để không làm thay đổi contract CV-01. |
| Diện tích tính toán | Lưu ở `farm_areas.calculated_area` bằng `DECIMAL(10,4)`, đơn vị cố định là hecta. Không dùng `area_unit` cho trường này; `area` và `area_unit` hiện có tiếp tục biểu diễn diện tích khai báo. |
| Thời điểm thay đổi | `farm_areas.boundary_updated_at` lưu thời điểm ranh giới được thiết lập hoặc cập nhật thành công. Không cập nhật trường này khi request bị từ chối hoặc chỉ sửa thông tin vùng trồng khác. |
| Tương thích dữ liệu cũ | Ba cột mới đều cho phép `NULL`; không backfill polygon từ `location POINT` vì một điểm không đủ suy ra ranh giới. Vùng trồng cũ tiếp tục hoạt động và API trả `points = []`, `calculatedArea = null` khi chưa khoanh ranh giới. |
| Phiên bản ranh giới | Không tạo bảng phiên bản riêng. Mỗi lần lưu thành công phát `ActivityLogEvent`; `activity_logs.before_value` và `after_value` lưu snapshot JSON gọn của phiên bản cũ/mới, còn `entity_id` liên kết logic tới `farm_areas.id`. |
| Spatial index | Chưa tạo spatial index trong Story này. `boundary` phải nullable để tương thích dữ liệu cũ, trong khi MySQL 8.4 yêu cầu cột geometry thuộc spatial index là `NOT NULL`; các endpoint hiện tại cũng truy xuất theo khóa vùng trồng thay vì truy vấn giao/cắt không gian. Chỉ bổ sung index bằng migration mới khi có User Story tìm kiếm không gian và chiến lược backfill bắt buộc `boundary`. |

Snapshot audit dùng cùng một cấu trúc cho `beforeValue` và `afterValue`:

```json
{
  "schemaVersion": 1,
  "points": [
    { "latitude": 21.587568, "longitude": 105.826176 },
    { "latitude": 21.588500, "longitude": 105.826500 },
    { "latitude": 21.588200, "longitude": 105.828000 },
    { "latitude": 21.587000, "longitude": 105.827800 }
  ],
  "calculatedArea": 6.8500
}
```

- Lần thiết lập ranh giới đầu tiên: `beforeValue = null`, `afterValue` chứa snapshot mới.
- Các lần chỉnh sửa tiếp theo: `beforeValue` chứa snapshot đang lưu và `afterValue` chứa snapshot vừa được xác nhận.
- Chỉ ghi audit sau khi cập nhật DB thành công; request `400`, `403`, `404` hoặc `409` không tạo phiên bản mới.
- Snapshot không chứa thông tin cá nhân hoặc dữ liệu tổ chức ngoài ranh giới và diện tích tính toán.

### 3.3. Quyết định đã chốt cho CV-03

| Nội dung | Quyết định |
|---|---|
| Vị trí màn hình | Giữ route sửa vùng trồng hiện tại `/farm-areas/{id}/edit`; bổ sung hai tab `Thông tin chung` và `Ranh giới trên bản đồ`. `EditFarmAreaForm` hiện có nằm nguyên trong tab đầu, tránh thay đổi luồng cập nhật `location POINT`. |
| Phạm vi chỉnh sửa | Tab ranh giới dùng endpoint `GET/PUT /api/v1/farm-areas/{id}/boundary` riêng. Lưu thông tin chung và lưu ranh giới là hai thao tác độc lập, không ghép hai request vào một nút lưu. |
| Công cụ bản đồ | Tái sử dụng Leaflet/React Leaflet đã có. Click bản đồ để thêm đỉnh; mỗi đỉnh hiển thị số thứ tự và có thể kéo; danh sách bên cạnh cho phép xóa đỉnh. Polygon được nối theo đúng thứ tự danh sách. Không bổ sung plugin vẽ mới cho phạm vi Story. |
| Dán tọa độ | Textarea nhận mỗi dòng theo contract `latitude, longitude`. Nút `Áp dụng danh sách` chỉ thay draft khi toàn bộ dòng hợp lệ; lỗi hiển thị đúng số dòng, không áp dụng một phần. Điểm đóng vòng lặp lại ở dòng cuối bị báo lỗi. |
| Diện tích xem trước | Panel so sánh luôn hiển thị diện tích khai báo, diện tích ranh giới xem trước và phần trăm chênh lệch. Gắn nhãn `Tạm tính` cho kết quả frontend; sau khi lưu thành công thay bằng số liệu chính thức backend trả về. |
| Lưu và xác nhận | Lần đầu luôn gửi `confirmed=false`. Nếu backend trả `409 AREA_DEVIATION_CONFIRMATION_REQUIRED`, mở dialog so sánh số liệu backend; chỉ nút `Tôi hiểu và đồng ý lưu` mới gửi lại cùng draft với `confirmed=true`. |
| Dữ liệu chưa lưu | Khi draft khác dữ liệu đã tải, hiển thị trạng thái `Chưa lưu`; nút `Khôi phục ranh giới đã lưu` đưa draft về snapshot gần nhất. Chuyển tab hoặc rời trang phải cảnh báo để tránh mất thay đổi. |
| Phân quyền | Chỉ `VT-02` có quyền cập nhật mới thấy công cụ vẽ và nút lưu. Chế độ công khai chỉ render polygon, không render marker đỉnh, textarea hay hành động chỉnh sửa; backend vẫn là lớp phân quyền chính. |
| Responsive và truy cập | Desktop dùng bố cục bản đồ 2/3 và panel 1/3; màn hình nhỏ xếp bản đồ trước, panel sau. Textarea và danh sách đỉnh là phương thức nhập thay thế cho người không thao tác chính xác bằng chuột; mọi nút icon có nhãn truy cập. |

---

## 4. Quyết định nghiệp vụ & phơi bày dữ liệu

- **Dữ liệu được phép công khai:**
  - Tọa độ các đỉnh của ranh giới vùng trồng (`points` / GeoJSON polygon coordinates).
  - Diện tích tính toán từ ranh giới (`calculatedArea`).
  - Tên vùng trồng, loại cây trồng.
- **Dữ liệu KHÔNG được công khai:**
  - Không phơi bày các thông tin nhạy cảm nội bộ của tổ chức, thông tin cá nhân của người quản lý, hoặc nhật ký chi tiết ngoài phạm vi tra cứu.
  - Người dùng ẩn danh truy cập qua tra cứu công khai không được phép gọi endpoint cập nhật ranh giới (`QTN-12`).

---

## 5. Vai trò và phân quyền

| Vai trò | Mã | Hành động | Quyền truy cập |
|---|---|---|---|
| Quản lý hợp tác xã | `VT-02` | Cập nhật, khoanh ranh giới vùng trồng | Thuộc tổ chức quản lý vùng trồng (`QTN-01`) |
| Quản lý tổ chức khác | `VT-02` | Thao tác trên vùng trồng tổ chức khác | **Bị chặn: 403 Forbidden** |
| Người ghi sự kiện | `VT-03` | Thao tác cập nhật ranh giới | **Bị chặn: 403 Forbidden** |
| Khách vãng lai / Người tiêu dùng | Công khai | Xem ranh giới trên trang tra cứu tem | Cho phép xem (read-only, `QTN-12`) |

---

## 6. Mô hình trạng thái & luồng xử lý

```
[Người quản lý mở bản đồ vùng trồng]
          │
          ▼
[Vẽ đỉnh hoặc dán tọa độ `latitude, longitude`]
          │
          ├── < 3 đỉnh phân biệt ────────────────────► [Báo lỗi: Tối thiểu 3 đỉnh]
          │
          ├── Trùng đỉnh liên tiếp / tự cắt / diện tích 0 ─► [Báo lỗi: Ranh giới không hợp lệ]
          │
          ▼
[Backend tự khép kín polygon và tính diện tích trắc địa WGS84]
          │
          ▼
[So sánh với diện tích khai báo]
          │
          ├── Chênh lệch > ngưỡng cấu hình VÀ confirmed == false ─► [409, yêu cầu xác nhận]
          │                                                    │
          │                                                    ├─ Người dùng hủy ──► Chỉnh sửa lại
          │                                                    └─ Người dùng xác nhận (confirmed = true)
          │                                                                 │
          └── Chênh lệch <= ngưỡng HOẶC confirmed == true ◄──────────────────┘
                    │
                    ▼
          [Lưu ranh giới + diện tích tính được vào DB]
          [Ghi ActivityLog phiên bản ranh giới cũ & mới]
                    │
                    ▼
          [Hoàn tất - HTTP 200 OK]
```

---

## 7. Chi tiết Endpoint

### Endpoint 1: Cập nhật ranh giới vùng trồng

* **Method:** `PUT`
* **URL:** `/api/v1/farm-areas/{id}/boundary`
* **Auth:** Bearer JWT (`VT-02`)

#### Path Parameters
* `id` (UUID, bắt buộc): ID của vùng trồng.

#### Request Body (`UpdateFarmAreaBoundaryRequest`)
```json
{
  "points": [
    { "latitude": 21.587568, "longitude": 105.826176 },
    { "latitude": 21.588500, "longitude": 105.826500 },
    { "latitude": 21.588200, "longitude": 105.828000 },
    { "latitude": 21.587000, "longitude": 105.827800 }
  ],
  "confirmed": false
}
```

* `points` (List<LatLngDto>, bắt buộc): Danh sách tọa độ các đỉnh theo thứ tự nối vòng. Tối thiểu 3 đỉnh phân biệt; không lặp điểm đầu ở cuối danh sách vì backend tự khép kín polygon.
* `confirmed` (Boolean, mặc định `false`): Cờ xác nhận lưu khi diện tích tính toán vượt ngưỡng chênh lệch đang cấu hình.

#### Validation Rules:
1. `points` không được null và phải có tối thiểu 3 tọa độ phân biệt (`NCL-02-CN-008-TC-02`).
2. Tọa độ mỗi điểm: `latitude` nằm trong [-90.0, 90.0], `longitude` nằm trong [-180.0, 180.0].
3. Không chấp nhận hai đỉnh liên tiếp trùng nhau; nếu client lặp điểm đầu ở cuối danh sách thì request không đúng contract.
4. Backend tự khép kín vòng và đa giác kết quả phải là đa giác đơn, không tự giao cắt và có diện tích lớn hơn 0.
5. Nếu độ chênh lệch diện tích:
   $$\Delta = \frac{|A_{\text{tính}} - A_{\text{khai báo}}|}{A_{\text{khai báo}}} \times 100\% > \text{ngưỡng cấu hình}$$
   mà `confirmed == false` thì hệ thống không lưu và trả `409 Conflict` kèm chi tiết để người dùng xác nhận.

#### Response thành công (`200 OK`)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "19001664-577e-4b3b-beda-7218066e2f23",
    "name": "Vùng chè Tân Cương",
    "organizationId": "11111111-1111-1111-1111-111111111111",
    "declaredArea": 6.69,
    "declaredAreaUnit": "HA",
    "calculatedArea": 6.85,
    "points": [
      { "latitude": 21.587568, "longitude": 105.826176 },
      { "latitude": 21.588500, "longitude": 105.826500 },
      { "latitude": 21.588200, "longitude": 105.828000 },
      { "latitude": 21.587000, "longitude": 105.827800 }
    ],
    "areaDeviationPercentage": 2.39,
    "updatedAt": "2026-09-15T10:00:00Z"
  },
  "timestamp": "2026-09-15T10:00:00.123Z"
}
```

---

### Endpoint 2: Tra cứu ranh giới vùng trồng (Nội bộ tổ chức)

* **Method:** `GET`
* **URL:** `/api/v1/farm-areas/{id}/boundary`
* **Auth:** Bearer JWT (`VT-01`, `VT-02`, `VT-03` thuộc tổ chức)

#### Response `200 OK`
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "19001664-577e-4b3b-beda-7218066e2f23",
    "name": "Vùng chè Tân Cương",
    "declaredArea": 6.69,
    "declaredAreaUnit": "HA",
    "calculatedArea": 6.85,
    "points": [
      { "latitude": 21.587568, "longitude": 105.826176 },
      { "latitude": 21.588500, "longitude": 105.826500 },
      { "latitude": 21.588200, "longitude": 105.828000 },
      { "latitude": 21.587000, "longitude": 105.827800 }
    ],
    "updatedAt": "2026-09-15T10:00:00Z"
  }
}
```

---

### Endpoint 3: Tra cứu công khai (Bổ sung vào `PublicTraceResponse`)

* **Method:** `GET`
* **URL:** `/api/v1/public/trace/{codeValue}`
* **Auth:** Không yêu cầu (`QTN-12`)

#### Trường dữ liệu mở rộng trong `PublicTraceResponse`:
```json
{
  "success": true,
  "data": {
    "codeValue": "TC-2026-001234",
    "lotName": "Lô chè búp Tân Cương",
    "farmAreaBoundary": {
      "id": "19001664-577e-4b3b-beda-7218066e2f23",
      "name": "Vùng chè Tân Cương",
      "calculatedArea": 6.85,
      "points": [
        { "latitude": 21.587568, "longitude": 105.826176 },
        { "latitude": 21.588500, "longitude": 105.826500 },
        { "latitude": 21.588200, "longitude": 105.828000 },
        { "latitude": 21.587000, "longitude": 105.827800 }
      ]
    },
    "...": "các trường hiện có giữ nguyên"
  }
}
```

---

## 8. Hợp đồng lỗi (Error Contract)

| HTTP Status | Mã lỗi logic | Điều kiện xảy ra | Thông điệp phản hồi |
|---:|---|---|---|
| `400` | `INVALID_BOUNDARY_POINTS` | Dưới 3 đỉnh phân biệt, lặp điểm đầu ở cuối, đỉnh liên tiếp trùng nhau hoặc diện tích bằng 0 | Ranh giới vùng trồng phải có tối thiểu 3 đỉnh phân biệt và tạo được polygon có diện tích |
| `400` | `SELF_INTERSECTING_BOUNDARY` | Các cạnh của đa giác cắt nhau | Ranh giới vùng trồng không hợp lệ do các cạnh tự cắt nhau |
| `400` | `INVALID_COORDINATES` | Tọa độ đỉnh vượt ngoài dải địa lý chuẩn | Tọa độ đỉnh không hợp lệ (vĩ độ [-90, 90], kinh độ [-180, 180]) |
| `409` | `AREA_DEVIATION_CONFIRMATION_REQUIRED` | Lệch diện tích vượt ngưỡng cấu hình và `confirmed == false` | Diện tích tính từ ranh giới vượt ngưỡng chênh lệch cho phép; cần xác nhận trước khi lưu |
| `403` | `FORBIDDEN` | Người dùng không phải `VT-02` hoặc khác tổ chức | Bạn không có quyền chỉnh sửa vùng trồng của tổ chức này |
| `404` | `FARM_AREA_NOT_FOUND` | ID vùng trồng không tồn tại trong hệ thống | Không tìm thấy vùng trồng tương ứng |

Chi tiết payload phản hồi khi cần xác nhận chênh lệch diện tích (`AREA_DEVIATION_CONFIRMATION_REQUIRED`):
```json
{
  "success": false,
  "status": 409,
  "message": "Diện tích tính từ ranh giới (1.45 ha) lệch 45.0% so với diện tích khai báo (1.00 ha), vượt ngưỡng cảnh báo 30%. Vui lòng xác nhận để tiếp tục lưu.",
  "errors": {
    "code": "AREA_DEVIATION_CONFIRMATION_REQUIRED",
    "declaredArea": 1.0,
    "calculatedArea": 1.45,
    "deviationPercentage": 45.0,
    "thresholdPercentage": 30.0
  },
  "path": "/api/v1/farm-areas/19001664-577e-4b3b-beda-7218066e2f23/boundary",
  "timestamp": "2026-09-15T10:00:00.123Z"
}
```

---

## 9. Bảo mật & Cô lập dữ liệu (Security & Privacy)

- **Cô lập dữ liệu đa tổ chức (`QTN-01`):**
  - Mọi thao tác cập nhật ranh giới luôn kiểm tra `farmArea.organization.id.equals(currentUser.organizationId)`.
  - Không cho phép người dùng tổ chức A cập nhật hoặc can thiệp vào ranh giới vùng trồng của tổ chức B.
- **An toàn tra cứu công khai (`QTN-12`):**
  - Endpoint công khai chỉ trả về tọa độ phục vụ vẽ bản đồ, không trả về ID người tạo, ghi chú nội bộ, hay dữ liệu quản trị tổ chức.
- **Audit Logging (`CV-02`):**
  - Bắn sự kiện `ActivityLogEvent`:
    - `action`: `"UPDATE_FARM_AREA_BOUNDARY"`
    - `entityType`: `"FARM_AREA"`
    - `entityId`: ID vùng trồng (`farm_areas.id`)
    - `beforeValue`: Snapshot JSON phiên bản cũ; `null` khi thiết lập lần đầu
    - `afterValue`: Snapshot JSON phiên bản mới
  - Snapshot chỉ được phát sau khi transaction cập nhật ranh giới hoàn tất thành công, bảo đảm lịch sử không ghi nhận một phiên bản chưa được lưu.

---

## 10. Tác động Cơ sở dữ liệu & Migration

### Migration SQL dự kiến (`V<timestamp>__add_farm_area_boundary.sql`)

Tên migration thực tế được tạo ở CV-04 theo timestamp tại thời điểm triển khai và phải được kiểm tra không trùng phiên bản trước khi commit.

```sql
ALTER TABLE farm_areas
    ADD COLUMN boundary POLYGON SRID 4326,
    ADD COLUMN calculated_area DECIMAL(10, 4) NULL,
    ADD COLUMN boundary_updated_at DATETIME NULL;
```

| Cột | Kiểu | Null | Ý nghĩa / ràng buộc |
|---|---|---:|---|
| `boundary` | `POLYGON SRID 4326` | Có | Ranh giới hiện hành; chỉ nhận Polygon WGS84. Không suy diễn từ `location`. |
| `calculated_area` | `DECIMAL(10,4)` | Có | Diện tích backend tính, đơn vị hecta, làm tròn `HALF_UP` 4 chữ số. Có giá trị khi `boundary` có giá trị. |
| `boundary_updated_at` | `DATETIME` | Có | Thời điểm thiết lập/cập nhật ranh giới gần nhất. Có giá trị khi `boundary` có giá trị. |

Ràng buộc nhất quán do service duy trì trong cùng transaction:

- `boundary IS NULL` thì `calculated_area IS NULL` và `boundary_updated_at IS NULL`.
- `boundary IS NOT NULL` thì `calculated_area > 0` và `boundary_updated_at IS NOT NULL`.
- Không backfill dữ liệu cũ và không thay đổi `location`, `area`, `area_unit` hiện có.
- Không tạo spatial index trong migration này. MySQL 8.4 yêu cầu geometry được lập spatial index phải là `NOT NULL`, không phù hợp với chiến lược tương thích dữ liệu cũ của Story.

Ánh xạ entity dự kiến ở CV-04:

```java
@Column(name = "boundary", columnDefinition = "POLYGON SRID 4326")
private Polygon boundary;

@Column(name = "calculated_area", precision = 10, scale = 4)
private BigDecimal calculatedArea;

@Column(name = "boundary_updated_at")
private LocalDateTime boundaryUpdatedAt;
```

---

## 11. Ảnh hưởng phía Frontend

### 11.1. Cấu trúc màn hình chỉnh sửa

```text
┌ Chỉnh sửa vùng trồng ─────────────────────────────────────────────────┐
│ [Thông tin chung] [Ranh giới trên bản đồ]                             │
├───────────────────────────────────────────────────────────────────────┤
│ Ranh giới trên bản đồ                              ● Chưa lưu          │
│ Chấm trên bản đồ, kéo đỉnh hoặc dán danh sách tọa độ                  │
│                                                                       │
│ ┌──────────────────────────────────────┐ ┌──────────────────────────┐ │
│ │                                      │ │ Diện tích                │ │
│ │          BẢN ĐỒ LEAFLET              │ │ Khai báo       1,0000 ha │ │
│ │   ①────────②                         │ │ Ranh giới     1,1200 ha  │ │
│ │    ╲      ╱   các đỉnh kéo được      │ │ Chênh lệch       12,00%  │ │
│ │     ④────③                           │ ├──────────────────────────┤ │
│ │                                      │ │ Danh sách đỉnh (4)       │ │
│ └──────────────────────────────────────┘ │ 1. 21.58, 105.82    [X]  │ │
│                                          │ ...                      │ │
│                                          ├──────────────────────────┤ │
│                                          │ Dán tọa độ               │ │
│                                          │ [latitude, longitude...] │ │
│                                          │ [Áp dụng danh sách]       │ │
│                                          └──────────────────────────┘ │
├───────────────────────────────────────────────────────────────────────┤
│ [Khôi phục ranh giới đã lưu]                    [Lưu ranh giới]       │
└───────────────────────────────────────────────────────────────────────┘
```

- `EditFarmAreaPage.tsx`: quản lý tab và cảnh báo rời trang khi có draft chưa lưu.
- `EditFarmAreaForm.tsx`: giữ nguyên trong tab `Thông tin chung`.
- Tạo `FarmAreaBoundaryEditor.tsx`: điều phối tải dữ liệu, draft, validation, lưu và trạng thái lỗi.
- Tạo `BoundaryMapEditor.tsx`: hiển thị `MapContainer`, `TileLayer`, `Polygon` và marker đỉnh kéo được.
- Tạo `BoundaryPastePanel.tsx`: parse textarea và hiển thị lỗi theo dòng.
- Tạo `AreaDeviationConfirmDialog.tsx`: hiển thị dữ liệu từ lỗi `409`, không tự tính lại số liệu trong dialog.
- Mở rộng `farmAreaApi.ts` và `types/farmArea.ts` theo đúng request/response tại Mục 7; không đặt URL API trực tiếp trong component.

### 11.2. Trạng thái màn hình

| Trạng thái | Hiển thị | Hành động |
|---|---|---|
| Đang tải | Skeleton/loader trong vùng bản đồ và khóa nút lưu | Chờ `GET boundary` hoàn tất |
| Chưa có ranh giới | Bản đồ căn giữa theo `farmArea.latitude/longitude`, hướng dẫn chấm tối thiểu 3 đỉnh | Cho phép chấm hoặc dán tọa độ |
| Sẵn sàng | Hiển thị polygon đã lưu, diện tích chính thức và thời điểm cập nhật | Có thể kéo, thêm hoặc xóa đỉnh |
| Draft hợp lệ | Badge `Chưa lưu`, preview diện tích và chênh lệch | Cho phép lưu/khôi phục |
| Draft chưa hợp lệ | Cạnh/đỉnh lỗi được tô đỏ khi xác định được; thông báo cụ thể ở panel | Khóa nút lưu, vẫn cho sửa/xóa/dán lại |
| Đang lưu | Khóa thao tác làm thay đổi draft; nút hiển thị `Đang lưu...` | Chặn gửi lặp request |
| Cần xác nhận | Dialog dùng `declaredArea`, `calculatedArea`, `deviationPercentage`, `thresholdPercentage` do backend trả | Hủy để sửa hoặc xác nhận gửi lại `confirmed=true` |
| Lưu thành công | Toast thành công, cập nhật snapshot/diện tích chính thức, bỏ badge `Chưa lưu` | Tiếp tục chỉnh sửa hoặc rời trang |
| Lỗi server/network | Toast lỗi, giữ nguyên draft và mở lại thao tác | Cho phép thử lưu lại |
| `403` / `404` | Thông báo không có quyền hoặc không tìm thấy; không hiển thị công cụ chỉnh sửa | Quay về danh sách vùng trồng |

### 11.3. Quy tắc tương tác và validation phía client

1. Click bản đồ chỉ thêm đỉnh khi tab ranh giới đang ở chế độ chỉnh sửa; click marker không tạo thêm đỉnh.
2. Kéo marker cập nhật đúng phần tử trong danh sách và preview polygon ngay lập tức.
3. Mỗi dòng paste phải có đúng hai số hữu hạn phân cách bằng dấu phẩy; bỏ qua dòng trắng đầu/cuối, không bỏ qua dòng lỗi ở giữa.
4. Kiểm tra vĩ độ [-90, 90], kinh độ [-180, 180], tối thiểu 3 đỉnh phân biệt, đỉnh liên tiếp không trùng và không lặp điểm đầu ở cuối.
5. Client phát hiện self-intersection để phản hồi sớm, nhưng không thay thế validation backend.
6. Nút `Lưu ranh giới` chỉ bật khi draft đã thay đổi, đạt validation client và không có request đang chạy.
7. Không tự gửi `confirmed=true`; cờ này chỉ được dùng sau thao tác xác nhận rõ ràng trong dialog `409`.
8. Nếu người dùng chỉnh sửa draft sau khi dialog mở, đóng dialog và lần lưu tiếp theo phải bắt đầu lại với `confirmed=false`.

### 11.4. Dialog xác nhận chênh lệch

- Tiêu đề: `Xác nhận chênh lệch diện tích`.
- Nội dung bắt buộc: diện tích khai báo, diện tích backend tính từ ranh giới, tỷ lệ chênh lệch và ngưỡng đang áp dụng.
- Nút phụ: `Quay lại chỉnh sửa` — đóng dialog, không gửi request.
- Nút chính: `Tôi hiểu và đồng ý lưu` — gửi lại chính xác danh sách đỉnh hiện tại với `confirmed=true`.
- Trong lúc xác nhận đang gửi, khóa nút đóng và cả hai hành động để tránh request trùng.

### 11.5. Trang tra cứu công khai

- `TraceLookupPage.tsx` truyền `farmAreaBoundary` vào `RouteMap.tsx` khi response có dữ liệu.
- `RouteMap` vẽ polygon màu xanh lá, fit bounds gồm cả polygon và các sự kiện có tọa độ; popup chỉ gồm tên vùng trồng và diện tích tính toán.
- Không hiển thị marker đỉnh, textarea, nút chỉnh sửa hoặc dữ liệu audit ở chế độ công khai (`QTN-12`).
- Nếu `farmAreaBoundary` không có dữ liệu, bản đồ hành trình hiện tại giữ nguyên hành vi.

---

## 12. Danh mục Kiểm thử (Test Cases)

- [ ] **TC-01 (Backend):** Cập nhật ranh giới thành công với 4 đỉnh hợp lệ, lưu đa giác vào DB, tính đúng diện tích và lưu vào `calculated_area`.
- [ ] **TC-02 (Backend):** Từ chối khi danh sách đỉnh < 3 (`NCL-02-CN-008-TC-02`).
- [ ] **TC-03 (Backend):** Từ chối khi đa giác có cạnh cắt nhau (Self-intersecting polygon).
- [ ] **TC-04 (Backend):** Cảnh báo khi độ lệch vượt ngưỡng cấu hình mặc định 30% và `confirmed == false` (`NCL-02-CN-008-TC-03`).
- [ ] **TC-05 (Backend):** Cho phép lưu khi độ lệch vượt ngưỡng nhưng `confirmed == true`.
- [ ] **TC-06 (Bảo mật/QTN-01):** Quản lý tổ chức khác cố tình cập nhật vùng trồng bị trả về `403 Forbidden`.
- [ ] **TC-07 (Audit Log):** Kiểm tra `activity_logs` được ghi nhận bản ghi thay đổi ranh giới có đủ `beforeValue` và `afterValue`.
- [ ] **TC-08 (Public Trace):** Quét tem lô hàng có vùng trồng đã khoanh ranh giới, kiểm tra API trả về đủ `farmAreaBoundary` (`NCL-02-CN-008-TC-04`).
- [ ] **TC-09 (Frontend UI):** Kiểm tra vẽ ranh giới trên bản đồ, dán danh sách tọa độ, hiển thị cảnh báo khi vượt ngưỡng, và hiển thị trên trang tra cứu công khai.
- [ ] **TC-10 (Boundary contract):** Backend tự khép kín danh sách 3 đỉnh phân biệt; từ chối request lặp điểm đầu ở cuối, đỉnh liên tiếp trùng nhau hoặc polygon có diện tích bằng 0.
- [ ] **TC-11 (Ngưỡng biên):** Chênh lệch bằng đúng ngưỡng được lưu không cần xác nhận; chỉ giá trị lớn hơn ngưỡng mới trả `409`.
- [ ] **TC-12 (Nguồn diện tích):** Frontend hiển thị xem trước nhưng lưu và cảnh báo theo diện tích backend tính lại trên WGS84.
- [ ] **TC-13 (Migration tương thích):** Sau migration, vùng trồng cũ có ba cột mới bằng `NULL` vẫn được đọc/cập nhật bằng các API hiện hành.
- [ ] **TC-14 (SRID và thứ tự đỉnh):** Polygon lưu trong DB có `ST_SRID(boundary) = 4326`; vòng ngoài được khép kín trong geometry nhưng response không lặp đỉnh đầu ở cuối `points`.
- [ ] **TC-15 (Phiên bản đầu tiên):** Thiết lập ranh giới lần đầu tạo ActivityLog với `beforeValue = null` và `afterValue` là snapshot schema version 1.
- [ ] **TC-16 (Phiên bản cập nhật):** Chỉnh sửa thành công tạo ActivityLog chứa đúng snapshot cũ/mới; request bị từ chối không tạo bản ghi phiên bản.
- [ ] **TC-17 (UI vẽ/kéo):** Chấm đủ đỉnh tạo polygon; kéo một marker cập nhật đúng tọa độ, polygon và diện tích preview.
- [ ] **TC-18 (UI paste):** Danh sách hợp lệ thay toàn bộ draft; dòng sai định dạng/ngoài miền hiển thị đúng số dòng và không áp dụng một phần.
- [ ] **TC-19 (UI trạng thái lưu):** Nút lưu bị khóa khi dưới 3 đỉnh, self-intersection, draft chưa thay đổi hoặc đang gửi; draft hợp lệ gửi lần đầu với `confirmed=false`.
- [ ] **TC-20 (UI xác nhận 409):** Dialog hiển thị đúng số liệu backend; hủy không lưu, xác nhận gửi lại cùng points với `confirmed=true`; thay đổi draft bắt buộc quay lại `confirmed=false`.
- [ ] **TC-21 (UI không mất dữ liệu):** Lỗi network/server giữ nguyên draft; chuyển tab hoặc rời trang khi chưa lưu có cảnh báo.
- [ ] **TC-22 (UI responsive/accessibility):** Bố cục dùng được trên desktop/mobile; nhập bằng textarea và danh sách đỉnh không phụ thuộc hoàn toàn vào thao tác chuột; nút icon có accessible name.

---

## 13. Ràng buộc không gây hồi quy (Non-regression constraints)

- Các vùng trồng cũ chưa có ranh giới (`boundary IS NULL`) vẫn hoạt động bình thường trên mọi luồng hiện tại (tạo lô, duyệt lô, cập nhật vùng trồng).
- Các API tạo/sửa vùng trồng hiện tại (`POST /api/v1/farm-areas`, `PUT /api/v1/farm-areas/{id}`) giữ nguyên hợp đồng `latitude`, `longitude`, `area`, `areaUnit`.
- Trang tra cứu công khai đối với lô không có ranh giới vẫn hiển thị bình thường như trước.

---

## 14. Kết luận hợp đồng & câu hỏi tồn đọng

- **Đã chốt:**
  - CV-01 hoàn tất: hai cách nhập cùng tạo danh sách đỉnh có thứ tự; backend tự khép kín polygon.
  - Ngưỡng chênh lệch là cấu hình, mặc định 30%; so sánh theo điều kiện lớn hơn (`>`), không phải lớn hơn hoặc bằng.
  - Số đỉnh tối thiểu: 3 đỉnh phân biệt; không lặp điểm đầu ở cuối request.
  - Backend tính diện tích chính thức theo WGS84 và trả hecta; frontend chỉ tính xem trước.
  - Vượt ngưỡng khi chưa xác nhận trả `409 Conflict` với `errors.code = AREA_DEVIATION_CONFIRMATION_REQUIRED`.
  - Phân quyền: `VT-02` quản lý tổ chức.
  - Audit log: ghi vào `ActivityLog`.
  - Public trace: chỉ xem, không sửa (`QTN-12`).
  - CV-02 hoàn tất: lưu ranh giới hiện hành bằng `POLYGON SRID 4326`, diện tích bằng `DECIMAL(10,4)` theo hecta và thời điểm cập nhật trên `farm_areas`.
  - Ba cột mới nullable, không backfill từ `location POINT`, bảo đảm vùng trồng cũ tiếp tục hoạt động.
  - Phiên bản cũ/mới dùng snapshot JSON schema version 1 trong `ActivityLog.beforeValue/afterValue`; không tạo bảng lịch sử riêng.
  - Chưa tạo spatial index vì `boundary` cần nullable và Story chưa có truy vấn không gian; nếu phát sinh nhu cầu sẽ xử lý bằng migration riêng sau khi có chiến lược backfill.
  - CV-03 hoàn tất: dùng tab riêng trong trang sửa vùng trồng, giữ nguyên form thông tin chung và tách thao tác lưu ranh giới.
  - Công cụ vẽ tái sử dụng React Leaflet hiện có với click để thêm, marker kéo được, danh sách xóa đỉnh và textarea dán tọa độ; không thêm plugin bản đồ.
  - Dialog xác nhận chỉ mở theo lỗi `409` và hiển thị số liệu backend; `confirmed=true` không được gửi tự động.
  - Đã chốt đầy đủ trạng thái loading, empty, invalid, dirty, saving, conflict, success, forbidden/not-found, server error, responsive và cảnh báo mất draft.

- **Còn thuộc các công việc sau CV-03:**
  - CV-04/CV-05 triển khai và kiểm thử theo contract đã chốt.
