# NCL-09-CN-009 — API Design: Quản lý danh mục chỉ tiêu kiểm nghiệm và yêu cầu kiểm nghiệm theo loại nông sản

> Loại tài liệu: Phân tích nghiệp vụ + Thiết kế API (KHÔNG kèm implement).
> Người dùng: Backend Agent, Frontend Agent.
> Nhãn trạng thái: `EXISTING` (đã có) · `REUSE` (dùng lại) · `MODIFY` (cần mở rộng) · `NEW` (cần tạo) · `PROPOSED` (đề xuất tên mới) · `UNKNOWN` (chưa đủ bằng chứng).

---

## 1. Tổng quan

Story NCL-09-CN-009 yêu cầu:

1. Quản lý danh mục **chỉ tiêu kiểm nghiệm** (tên, đơn vị, ngưỡng tối đa, tiêu chuẩn tham chiếu, trạng thái): thêm, sửa, ngừng sử dụng, xóa có điều kiện.
2. **Gán bộ chỉ tiêu mặc định** cho từng loại nông sản (Product Category).
3. **Bật/tắt cờ bắt buộc kiểm nghiệm** cho loại nông sản, có điều kiện (chỉ bật khi đã có ít nhất một chỉ tiêu).
4. Chỉ tiêu khi được chọn lúc **tạo yêu cầu kiểm nghiệm** sẽ tự điền thông tin thay vì nhập tay hoàn toàn.
5. Toàn bộ quản lý danh mục dùng chung chỉ do **PLATFORM_ADMIN** thực hiện theo **QTN-17**.
6. Logic chặn kích hoạt tem (**QTN-21**) tiếp tục chạy trên dữ liệu cấu hình, không hard-code.

Hiện trạng repository: đã có Product Category với cờ `requires_inspection`, Inspection Request lưu chỉ tiêu dạng free-text, logic kích hoạt tem đọc cấu hình từ DB. **Chưa có** entity/bảng/API nào cho danh mục chỉ tiêu kiểm nghiệm.

---

## 2. Kết quả phân tích repository

### 2.1 Entity/domain liên quan

| Thành phần | File | Vai trò | Trạng thái |
|---|---|---|---|
| `ProductCategory` | `backend/src/main/java/com/nguongocso/entity/ProductCategory.java` | Loại nông sản; đã có cờ `requires_inspection` (Boolean, mặc định `false`) | EXISTING |
| `InspectionRequest` | `backend/src/main/java/com/nguongocso/entity/InspectionRequest.java` | Yêu cầu kiểm nghiệm theo lô; lưu `criterionName`, `unit`, `maxThreshold`, `resultValue`, `referenceStandard`, `status` (PENDING/PASSED/FAILED), `inspectedBy`, `inspectedAt`, `decisionNote` | EXISTING |
| `Lot` | `backend/src/main/java/com/nguongocso/entity/Lot.java` | Lô sản xuất; `status` (DRAFT/.../ACTIVATED), `qrActivated`, `activatedAt` | EXISTING |
| `InspectionCriterion` (chỉ tiêu kiểm nghiệm) | — | Tên, đơn vị, ngưỡng tối đa, tiêu chuẩn tham chiếu, trạng thái | NOT_FOUND → NEW |
| `CategoryCriterion` (bảng gán chỉ tiêu ↔ loại nông sản) | — | Gán bộ chỉ tiêu mặc định cho loại nông sản | NOT_FOUND → NEW |

Schema DB hiện có (`V1__init_schema.sql`, `V4__inspection_requests.sql`):

- `product_categories`: `id, code, name, description, requires_inspection BOOLEAN NOT NULL DEFAULT FALSE, created_at, updated_at`.
- `inspection_requests`: `id, lot_id FK lots, criterion_name VARCHAR(150) NOT NULL, unit VARCHAR(30), max_threshold NUMERIC(12,4), result_value NUMERIC(12,4), reference_standard VARCHAR(150), status VARCHAR(20) DEFAULT 'PENDING', inspected_by FK users, inspected_at, decision_note VARCHAR(500), created_at, updated_at`.
- `lots`: có `status`, `qr_activated`, `activated_at`.

Nhận xét quan trọng: `inspection_requests` hiện **không có khóa ngoài** đến danh mục chỉ tiêu (vì danh mục chưa tồn tại) — chỉ tiêu đang được copy thành dữ liệu free-text (`criterion_name`, `unit`, `max_threshold`, `reference_standard`). Đây chính là cơ sở của rule "không hồi tố": dữ liệu yêu cầu kiểm nghiệm là snapshot tại thời điểm tạo.

### 2.2 Service/repository có thể reuse

| Thành phần | File | Khả năng reuse |
|---|---|---|
| `ProductCategoryRepository` | `backend/.../repository/ProductCategoryRepository.java` | REUSE — kiểm tra tồn tại category khi gán chỉ tiêu / bật cờ bắt buộc |
| `ProductCategoryService` | `backend/.../service/ProductCategoryService.java` | MODIFY — thêm nghiệp vụ bật/tắt cờ bắt buộc có điều kiện (BR-3) |
| `InspectionRequestRepository` | `backend/.../repository/InspectionRequestRepository.java` | MODIFY — thêm query đếm/kiểm tra tham chiếu theo `criterion_id` (sau khi thêm cột) |
| `InspectionRequestService` | `backend/.../service/InspectionRequestService.java` | MODIFY — khi tạo request từ criterion thì tự điền thông tin |
| `LotService.activateQR` | `backend/.../service/LotService.java` | REUSE — KHÔNG sửa; logic chặn đã đọc `requires_inspection` từ DB |
| `LotRepository.existsPassedInspection` | `backend/.../repository/LotRepository.java` | REUSE — nguồn dữ liệu kiểm tra "đã có kết quả PASSED" |
| `ApiResponse<T>` / `PageResponse<T>` | `backend/.../dto/ApiResponse.java`, `PageResponse.java` | REUSE — format response chuẩn |
| `GlobalExceptionHandler` / `ApiException` / `ResourceNotFoundException` | `backend/.../exception/*` | REUSE — format lỗi chuẩn |

### 2.3 API hiện có có thể reuse

| API | Method | Endpoint | Status | Action |
|---|---|---|---|---|
| Danh sách loại nông sản | GET | `/api/v1/product-categories` | EXISTING | REUSE |
| Chi tiết loại nông sản | GET | `/api/v1/product-categories/{id}` | EXISTING | REUSE |
| Tạo loại nông sản | POST | `/api/v1/product-categories` | EXISTING | MODIFY (thêm `@PreAuthorize` PLATFORM_ADMIN) |
| Sửa loại nông sản | PUT | `/api/v1/product-categories/{id}` | EXISTING | MODIFY (thêm `@PreAuthorize` + rule BR-3 nếu đổi `requiresInspection`) |
| Tạo yêu cầu kiểm nghiệm | POST | `/api/v1/inspection-requests` | EXISTING | MODIFY (nhận thêm `criterionId` để prefill) |
| Danh sách yêu cầu kiểm nghiệm | GET | `/api/v1/inspection-requests` | EXISTING | REUSE |
| Duyệt kết quả kiểm nghiệm | PUT | `/api/v1/inspection-requests/{id}/decision` | EXISTING | REUSE |
| Kích hoạt tem | POST | `/api/v1/lots/{id}/activate-qr` | EXISTING | REUSE (QTN-21 — không sửa) |
| CRUD chỉ tiêu kiểm nghiệm | — | `/api/v1/inspection-criteria` | NOT_FOUND | NEW |
| Gán chỉ tiêu cho loại nông sản | — | `/api/v1/product-categories/{id}/criteria` | NOT_FOUND | NEW |
| Bật/tắt bắt buộc kiểm nghiệm | — | `/api/v1/product-categories/{id}/mandatory-inspection` | NOT_FOUND | NEW |

### 2.4 Security/permission

- Authentication: JWT Bearer token (`JwtAuthenticationFilter` + `JwtService` + `UserDetailsServiceImpl`) — EXISTING.
- Roles trong hệ thống (seed `V2__seed_data.sql`): `PLATFORM_ADMIN`, `COOPERATIVE_ADMIN`, `INSPECTOR` (và `FARMER` trong tài liệu vai trò QTN-17).
- `SecurityConfig`: các endpoint `/api/v1/auth/**` và `/api/v1/traceability/**` là public; còn lại yêu cầu authenticated. CSRF tắt, stateless session.
- `@EnableMethodSecurity` đã được bật trong `SecurityConfig` → có thể dùng `@PreAuthorize`.
- **Gap quan trọng**: hiện **chưa có annotation `@PreAuthorize` nào** trong toàn bộ controller/service. Nghĩa là TC-04 (chặn Quản lý hợp tác xã sửa danh mục dùng chung) **chưa được enforce** trong code hiện tại — đây là phần bắt buộc phải bổ sung theo QTN-17.
- Cách lấy current user: qua `SecurityContextHolder` / principal từ JWT (pattern hiện có trong `AuthService`, `InspectionRequestService` khi set `inspectedBy`).

---

## 3. Domain và Business Rules

| # | Rule | Nguồn | Trạng thái hiện tại |
|---|---|---|---|
| BR-1 | Không trùng tên chỉ tiêu trong cùng tiêu chuẩn tham chiếu | Story | NEW — chưa có logic (chưa có danh mục chỉ tiêu) |
| BR-2 | Ngưỡng tối đa phải > 0 | Story | NEW — chưa có validation cho danh mục chỉ tiêu |
| BR-3 | Không bật "bắt buộc kiểm nghiệm" nếu loại nông sản chưa có chỉ tiêu nào | Story | NEW — hiện cờ `requires_inspection` bật/tắt tự do, không kiểm tra |
| BR-4 | Chỉ PLATFORM_ADMIN được quản lý danh mục dùng chung | QTN-17 | PARTIAL — role đã có, `@EnableMethodSecurity` đã bật, nhưng chưa có `@PreAuthorize` nào được áp dụng |
| BR-5 | Chỉ tiêu đang được Inspection Request tham chiếu không được xóa | Story | NEW — chưa có logic |
| BR-6 | Chỉ tiêu đang được tham chiếu chỉ được ngừng sử dụng (DISABLE) | Story + QTN-17 | NEW — chưa có logic |
| BR-7 | Cấu hình mới không hồi tố lô đã kích hoạt tem | Story | CONFIRMED — `LotService.activateQR` chỉ kiểm tra cấu hình tại thời điểm kích hoạt; `inspection_requests` lưu snapshot dữ liệu chỉ tiêu, không tham chiếu ngược lại cấu hình |
| BR-8 | Lô tạo mới áp dụng cấu hình mới | Story | CONFIRMED — cờ `requires_inspection` được đọc live từ `product_categories` khi kích hoạt tem |

Quy ước trạng thái chỉ tiêu (PROPOSED, theo pattern trạng thái hiện có trong hệ thống):

- `ACTIVE` — đang sử dụng (mặc định khi tạo).
- `INACTIVE` — ngừng sử dụng.
---

## 4. API Contract

Convention chung (EXISTING — bắt buộc tuân theo):

- Base path: `/api/v1`.
- Response wrapper: `ApiResponse<T>` — `{ success, message, data, timestamp }`.
- Pagination: `PageResponse<T>` — `{ content, page, size, totalElements, totalPages, last }`; tham số `page` (0-based), `size` (mặc định 10), `keyword` (tìm không phân biệt hoa thường).
- Lỗi: `ApiException` (status tùy chọn) / `ResourceNotFoundException` (404) qua `GlobalExceptionHandler`.
- Xác thực: `Authorization: Bearer <JWT>`.

### 4.1 Danh sách chỉ tiêu — NEW

- **Method**: `GET`
- **Endpoint**: `/api/v1/inspection-criteria`
- **Mục đích**: Phân trang danh sách chỉ tiêu kiểm nghiệm, lọc theo từ khóa và trạng thái.
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Mọi user đã đăng nhập có nghiệp vụ kiểm nghiệm (PLATFORM_ADMIN, COOPERATIVE_ADMIN, INSPECTOR).
- **Query parameters**:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `page` | int | Không | 0 | Số trang (0-based) |
| `size` | int | Không | 10 | Số bản ghi/trang |
| `keyword` | string | Không | — | Tìm theo tên chỉ tiêu hoặc tiêu chuẩn tham chiếu (không phân biệt hoa thường) |
| `status` | string | Không | — | `ACTIVE` hoặc `INACTIVE`; bỏ trống = tất cả |

- **Response 200**: `ApiResponse<PageResponse<InspectionCriterionResponse>>`

```json
{
  "success": true,
  "message": "Lấy danh sách chỉ tiêu kiểm nghiệm thành công",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Dư lượng thuốc bảo vệ thực vật nhóm Lân hữu cơ",
        "unit": "mg/kg",
        "maxThreshold": 0.5,
        "referenceStandard": "QCVN 8-2:2011/BYT",
        "status": "ACTIVE",
        "referenced": true,
        "createdAt": "2026-08-26T07:00:00Z",
        "updatedAt": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-08-26T07:00:00Z"
}
```

- **Validation**: `status` nếu truyền phải thuộc {`ACTIVE`, `INACTIVE`}, nếu sai trả 400.
- **Business rules**: Không.
- **Error cases**: 400 (status không hợp lệ), 401 (chưa đăng nhập).

> Trường `referenced` (PROPOSED): cho biết chỉ tiêu đã bị Inspection Request nào tham chiếu chưa — để frontend ẩn nút "Xóa" và chỉ hiện "Ngừng sử dụng" (hỗ trợ TC-05 về UX).

### 4.2 Tạo chỉ tiêu — NEW

- **Method**: `POST`
- **Endpoint**: `/api/v1/inspection-criteria`
- **Mục đích**: Tạo chỉ tiêu kiểm nghiệm mới (TC-01).
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Chỉ `PLATFORM_ADMIN` — `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` (BR-4, TC-04).
- **Request body** — `InspectionCriterionRequest` (PROPOSED):

```json
{
  "name": "Dư lượng thuốc bảo vệ thực vật nhóm Lân hữu cơ",
  "unit": "mg/kg",
  "maxThreshold": 0.5,
  "referenceStandard": "QCVN 8-2:2011/BYT"
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `name` | string | Có | Không rỗng, ≤ 150 ký tự |
| `unit` | string | Có | Không rỗng, ≤ 30 ký tự |
| `maxThreshold` | number | Có | **> 0** (BR-2, TC-03) |
| `referenceStandard` | string | Không | ≤ 150 ký tự |

- **Response 201**: `ApiResponse<InspectionCriterionResponse>` — chỉ tiêu vừa tạo, `status` mặc định `ACTIVE`.
- **Validation**:
    - `name` không rỗng; `maxThreshold` phải > 0 → nếu ≤ 0 trả 400 với message: `"Ngưỡng tối đa phải là số dương"`.
- **Business rules**:
    - BR-1: không được trùng `name` (so sánh không phân biệt hoa thường) trong cùng `referenceStandard` → trả 409: `"Chỉ tiêu '<name>' đã tồn tại trong tiêu chuẩn '<referenceStandard>'"`.
    - Hai chỉ tiêu cùng tên nhưng khác tiêu chuẩn tham chiếu → hợp lệ.
- **Error cases**: 400 (validation), 401, 403 (không phải PLATFORM_ADMIN — TC-04), 409 (trùng).

### 4.3 Chi tiết chỉ tiêu — NEW

- **Method**: `GET`
- **Endpoint**: `/api/v1/inspection-criteria/{id}`
- **Mục đích**: Xem chi tiết một chỉ tiêu.
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Mọi user đã đăng nhập có nghiệp vụ kiểm nghiệm.
- **Path parameter**: `id` (Long) — id chỉ tiêu.
- **Response 200**: `ApiResponse<InspectionCriterionResponse>` (như §4.1, kèm `referenced`).
- **Error cases**: 401, 404 (id không tồn tại — `ResourceNotFoundException`: `"Không tìm thấy chỉ tiêu kiểm nghiệm với id = <id>"`).

### 4.4 Cập nhật chỉ tiêu — NEW

- **Method**: `PUT`
- **Endpoint**: `/api/v1/inspection-criteria/{id}`
- **Mục đích**: Sửa tên, đơn vị, ngưỡng, tiêu chuẩn tham chiếu của chỉ tiêu.
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Chỉ `PLATFORM_ADMIN` — `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`.
- **Path parameter**: `id` (Long).
- **Request body**: giống §4.2 (`InspectionCriterionRequest`).
- **Response 200**: `ApiResponse<InspectionCriterionResponse>` — dữ liệu sau cập nhật.
- **Validation**: như §4.2 (BR-2: `maxThreshold > 0`).
- **Business rules**:
    - BR-1: kiểm tra trùng `name` + `referenceStandard` loại trừ chính nó → 409 nếu trùng.
    - Chỉ tiêu `INACTIVE` vẫn được sửa (để cấu hình lại trước khi dùng lại) — PROPOSED.
    - **Không hồi tố** (BR-7): các Inspection Request đã tạo giữ nguyên snapshot `criterion_name/unit/max_threshold/reference_standard`; cập nhật chỉ tiêu KHÔNG cập nhật ngược các request cũ.
- **Error cases**: 400, 401, 403, 404, 409.

### 4.5 Ngừng sử dụng chỉ tiêu — NEW

- **Method**: `PUT`
- **Endpoint**: `/api/v1/inspection-criteria/{id}/disable`
- **Mục đích**: Chuyển chỉ tiêu sang `INACTIVE` (TC-05 — hành động được phép duy nhất với chỉ tiêu đang được tham chiếu).
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Chỉ `PLATFORM_ADMIN`.
- **Path parameter**: `id` (Long).
- **Request body**: không có.
- **Response 200**: `ApiResponse<InspectionCriterionResponse>` với `status = "INACTIVE"`.
- **Business rules**:
    - BR-6: luôn cho phép disable, **kể cả khi đang được Inspection Request tham chiếu**.
    - Chỉ tiêu `INACTIVE`: không xuất hiện khi chọn chỉ tiêu để tạo Inspection Request mới và không gán được vào bộ chỉ tiêu của loại nông sản; các gán hiện có giữ nguyên nhưng bị bỏ qua khi lọc `activeOnly=true` (§4.7).
    - Idempotent: disable một chỉ tiêu đã `INACTIVE` → vẫn trả 200.
- **Error cases**: 401, 403, 404.

> Kích hoạt lại (PROPOSED): `PUT /api/v1/inspection-criteria/{id}/enable` — chuyển về `ACTIVE`. Backend Agent nên implement cùng disable để hoàn chỉnh vòng đời trạng thái; nếu chưa cần, frontend ẩn nút.

### 4.6 Xóa chỉ tiêu — NEW

- **Method**: `DELETE`
- **Endpoint**: `/api/v1/inspection-criteria/{id}`
- **Mục đích**: Xóa chỉ tiêu **chưa từng được tham chiếu** (TC-05).
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Chỉ `PLATFORM_ADMIN`.
- **Path parameter**: `id` (Long).
- **Response 200**: `ApiResponse<Void>` (hoặc `data: null`) — message: `"Xóa chỉ tiêu kiểm nghiệm thành công"`.
- **Business rules**:
    - BR-5: nếu tồn tại `inspection_requests.criterion_id = id` → **từ chối xóa**, trả **409** với message: `"Chỉ tiêu đang được yêu cầu kiểm nghiệm tham chiếu, không thể xóa. Vui lòng ngừng sử dụng chỉ tiêu thay vì xóa."`
    - Khi xóa thành công, các bản ghi gán trong `category_criteria` của chỉ tiêu này bị xóa theo (CASCADE hoặc xóa thủ công trong service).
- **Error cases**: 401, 403, 404, **409 (đang được tham chiếu — TC-05)**.

> Lưu ý thiết kế: vì `inspection_requests` hiện lưu snapshot free-text và chưa có cột `criterion_id`, Backend Agent cần thêm cột `criterion_id BIGINT NULL REFERENCES inspection_criteria(id)` (xem §10) để kiểm tra tham chiếu chính xác. Các request cũ (trước migration) có `criterion_id = NULL` và không bị tính là tham chiếu — phù hợp BR-7.

### 4.7 Lấy chỉ tiêu theo loại nông sản — NEW

- **Method**: `GET`
- **Endpoint**: `/api/v1/product-categories/{id}/criteria`
- **Mục đích**: Lấy bộ chỉ tiêu đã gán cho loại nông sản — dùng khi (a) quản trị xem/quản lý bộ chỉ tiêu, (b) form tạo Inspection Request chọn chỉ tiêu theo loại nông sản của lô (TC-01: "xuất hiện khi tạo yêu cầu kiểm nghiệm").
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Mọi user đã đăng nhập có nghiệp vụ kiểm nghiệm.
- **Path parameter**: `id` (Long) — id loại nông sản.
- **Query parameter**:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `activeOnly` | boolean | Không | `true` | `true`: chỉ trả chỉ tiêu `ACTIVE` (dùng cho form tạo Inspection Request); `false`: trả cả `INACTIVE` (dùng cho màn hình quản lý) |

- **Response 200**: `ApiResponse<List<InspectionCriterionResponse>>` (không phân trang — bộ chỉ tiêu mỗi loại nông sản nhỏ).

```json
{
  "success": true,
  "message": "Lấy bộ chỉ tiêu của loại nông sản thành công",
  "data": [
    {
      "id": 1,
      "name": "Dư lượng thuốc bảo vệ thực vật nhóm Lân hữu cơ",
      "unit": "mg/kg",
      "maxThreshold": 0.5,
      "referenceStandard": "QCVN 8-2:2011/BYT",
      "status": "ACTIVE",
      "referenced": true,
      "createdAt": "2026-08-26T07:00:00Z",
      "updatedAt": null
    }
  ],
  "timestamp": "2026-08-26T07:00:00Z"
}
```

- **Error cases**: 401, 404 (category không tồn tại).

### 4.8 Gán bộ chỉ tiêu cho loại nông sản — NEW

- **Method**: `PUT`
- **Endpoint**: `/api/v1/product-categories/{id}/criteria`
- **Mục đích**: Thay thế toàn bộ bộ chỉ tiêu mặc định của loại nông sản bằng danh sách gửi lên (TC-01 — gán cho rau ăn lá).
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Chỉ `PLATFORM_ADMIN` — `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` (danh mục dùng chung — QTN-17).
- **Path parameter**: `id` (Long) — id loại nông sản.
- **Request body** — `CategoryCriteriaRequest` (PROPOSED):

```json
{
  "criterionIds": [1, 2, 3]
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `criterionIds` | array[Long] | Có (được phép mảng rỗng) | Không trùng nhau; từng id phải tồn tại |

- **Response 200**: `ApiResponse<List<InspectionCriterionResponse>>` — bộ chỉ tiêu sau khi gán.
- **Validation / Business rules**:
    - Category phải tồn tại → 404 nếu không.
    - Mọi `criterionId` phải tồn tại → 404/400 nếu có id lạ (message liệt kê id không tồn tại).
    - Chỉ được gán chỉ tiêu `ACTIVE`; nếu có id thuộc chỉ tiêu `INACTIVE` → 400: `"Chỉ tiêu '<name>' đã ngừng sử dụng, không thể gán"`.
    - Truyền mảng rỗng = xóa toàn bộ gán hiện có. **Lưu ý BR-3**: nếu category đang `requiresInspection = true` mà xóa hết chỉ tiêu → từ chối (400): `"Không thể xóa toàn bộ chỉ tiêu của loại nông sản đang bắt buộc kiểm nghiệm. Tắt cờ bắt buộc trước."`
    - Nếu category đang `requiresInspection = true` và danh sách mới vẫn còn ≥ 1 chỉ tiêu → hợp lệ.
- **Error cases**: 400, 401, 403, 404.

> Semantic "thay thế toàn bộ" (replace) được chọn thay vì add/remove từng cái để đơn giản cho frontend (1 lần gọi khi lưu). Đây là quyết định PROPOSED — xem §13.

### 4.9 Bật/tắt bắt buộc kiểm nghiệm — NEW

- **Method**: `PUT`
- **Endpoint**: `/api/v1/product-categories/{id}/mandatory-inspection`
- **Mục đích**: Bật/tắt cờ bắt buộc kiểm nghiệm của loại nông sản (TC-02). Endpoint tách riêng (thay vì dùng PUT product-categories/{id} hiện có) để gắn rule BR-3 rõ ràng và tránh sửa contract hiện có.
- **Authentication**: Bắt buộc (JWT).
- **Authorization**: Chỉ `PLATFORM_ADMIN` — `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`.
- **Path parameter**: `id` (Long) — id loại nông sản.
- **Request body** — `MandatoryInspectionRequest` (PROPOSED):

```json
{
  "required": true
}
```

- **Response 200**: `ApiResponse<ProductCategoryResponse>` — category sau cập nhật (`requiresInspection` mới).
- **Validation / Business rules**:
    - Category phải tồn tại → 404.
    - **BR-3 (TC-02)**: nếu `required = true` và category có **0 chỉ tiêu ACTIVE** → từ chối **400**: `"Không thể bật bắt buộc kiểm nghiệm: loại nông sản chưa có chỉ tiêu kiểm nghiệm nào. Vui lòng gán ít nhất một chỉ tiêu."`
    - `required = false`: luôn cho phép (không cần kiểm tra chỉ tiêu).
- **Error cases**: 400 (TC-02), 401, 403 (TC-04), 404.

> Lưu ý: BR-3 cũng phải được enforce trong `ProductCategoryService.update` (PUT `/api/v1/product-categories/{id}` hiện có) nếu request có thay đổi `requiresInspection` thành `true` — tránh bypass qua endpoint cũ.

### 4.10 Các thay đổi MODIFY trên API hiện có

#### 4.10.1 Gắn quyền PLATFORM_ADMIN cho danh mục dùng chung (TC-04) — MODIFY

Thêm `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` cho các endpoint quản lý danh mục dùng chung hiện có:

- `POST /api/v1/product-categories`
- `PUT /api/v1/product-categories/{id}`

(Các controller khác như Cooperative/Facility/User đã thuộc phạm vi quản trị riêng; story này chỉ yêu cầu với danh mục liên quan kiểm nghiệm.)

#### 4.10.2 Tạo yêu cầu kiểm nghiệm nhận `criterionId` — MODIFY

`POST /api/v1/inspection-requests` (EXISTING) — mở rộng `InspectionRequestCreateRequest`:

| Field | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `lotId` | Long | Có | EXISTING |
| `criterionName` | string | Có* | EXISTING — giữ nguyên để tương thích |
| `unit` | string | Không | EXISTING |
| `maxThreshold` | number | Không | EXISTING |
| `referenceStandard` | string | Không | EXISTING |
| `criterionId` | Long | Không | **NEW (PROPOSED)** — nếu truyền, service kiểm tra tồn tại + `ACTIVE`, sau đó tự điền `criterionName/unit/maxThreshold/referenceStandard` từ chỉ tiêu (ghi đè giá trị client gửi nếu xung đột) và lưu `criterion_id` vào bản ghi |

- Nếu `criterionId` truyền vào không tồn tại → 404; nếu `INACTIVE` → 400: `"Chỉ tiêu '<name>' đã ngừng sử dụng"`.
- Nếu không truyền `criterionId`: hành vi EXISTING giữ nguyên (nhập tay — tương thích dữ liệu cũ).
- Response: không đổi (`ApiResponse<InspectionRequestResponse>`).

---

## 5. Error Codes

Theo `GlobalExceptionHandler` hiện có (EXISTING — REUSE):

| HTTP | Ý nghĩa | Nguồn | Áp dụng |
|---|---|---|---|
| 400 | Validation / business rule từ chối | `ApiException(BAD_REQUEST)` | BR-2, BR-3 (TC-02), gán chỉ tiêu INACTIVE |
| 401 | Chưa xác thực | Spring Security | Mọi endpoint |
| 403 | Không đủ quyền | Spring Security (`@PreAuthorize`) | TC-04 — không phải PLATFORM_ADMIN |
| 404 | Không tìm thấy tài nguyên | `ResourceNotFoundException` | id chỉ tiêu/category không tồn tại |
| 409 | Xung đột dữ liệu | `ApiException(CONFLICT)` | BR-1 (trùng tên+tiêu chuẩn); TC-05 (chỉ tiêu đang tham chiếu) |

Body lỗi thống nhất:

```json
{
  "success": false,
  "message": "Chỉ tiêu đang được yêu cầu kiểm nghiệm tham chiếu, không thể xóa. Vui lòng ngừng sử dụng chỉ tiêu thay vì xóa.",
  "timestamp": "2026-08-26T07:00:00Z"
}
```

---

## 6. Permission Matrix

Theo QTN-17 §4 (EXISTING về mặt quy định; enforcement `@PreAuthorize` là NEW):

| API | PLATFORM_ADMIN | COOPERATIVE_ADMIN | INSPECTOR | FARMER |
|---|---|---|---|---|
| GET `/inspection-criteria` | ✓ | ✓ | ✓ | ✗ |
| POST `/inspection-criteria` | ✓ | ✗ | ✗ | ✗ |
| GET `/inspection-criteria/{id}` | ✓ | ✓ | ✓ | ✗ |
| PUT `/inspection-criteria/{id}` | ✓ | ✗ | ✗ | ✗ |
| PUT `/inspection-criteria/{id}/disable` | ✓ | ✗ | ✗ | ✗ |
| DELETE `/inspection-criteria/{id}` | ✓ | ✗ | ✗ | ✗ |
| GET `/product-categories/{id}/criteria` | ✓ | ✓ | ✓ | ✗ |
| PUT `/product-categories/{id}/criteria` | ✓ | ✗ | ✗ | ✗ |
| PUT `/product-categories/{id}/mandatory-inspection` | ✓ | ✗ | ✗ | ✗ |
| POST/PUT `/product-categories*` (EXISTING) | ✓ | ✗ | ✗ | ✗ |
| POST `/inspection-requests` (EXISTING) | ✓ | ✓ | ✗ | ✗ |
| PUT `/inspection-requests/{id}/decision` (EXISTING) | ✓ | ✗ | ✓ | ✗ |

FARMER bị chặn ở `anyRequest().authenticated()` + không có trang nghiệp vụ; truy xuất nguồn gốc công khai là `/api/v1/traceability/**` (không thuộc story này).

---

## 7. Dependency QTN-17

- Tài liệu: `docs/quy-trinh/QTN-17-quan-ly-danh-muc-dung-chung.md` — EXISTING.
- Các điểm story này phụ thuộc:
    - §4.1: chỉ `PLATFORM_ADMIN` được tạo/sửa danh mục dùng chung → áp dụng trực tiếp cho inspection criteria (BR-4).
    - §4.5: enforce bằng `@PreAuthorize` → **gap hiện tại**: chưa có `@PreAuthorize` nào trong codebase; Backend Agent phải thêm (xem §4.10.1, §6).
    - §5: danh mục đang được dữ liệu nghiệp vụ tham chiếu không xóa cứng, chỉ ngừng sử dụng → BR-5/BR-6.
    - §3: bảng vai trò → Permission Matrix §6.
- Không cần sửa tài liệu QTN-17.

---

## 8. Dependency QTN-21

- Tài liệu: `docs/quy-trinh/QTN-21-kich-hoat-tem-va-chan-kich-hoat.md` — EXISTING.
- Nơi thực thi logic: `LotService.activateQR()` (`backend/src/main/java/com/nguongocso/service/LotService.java`) — EXISTING.
- Nguồn dữ liệu cấu hình hiện tại:
    - Cờ bắt buộc: `product_categories.requires_inspection`, đọc live qua `lot.getProductCategory().getRequiresInspection()` tại thời điểm kích hoạt.
    - Kết quả kiểm nghiệm: `LotRepository.existsPassedInspection(lotId)` (native query đếm `inspection_requests` status `PASSED` theo lot).
- Đánh giá: **QTN-21 KHÔNG cần sửa code** cho story này vì:
    - Rule chặn đã chạy trên dữ liệu cấu hình (`requires_inspection`), không hard-code danh sách criterion (QTN-21 §4 xác nhận).
    - Story này cung cấp thêm dữ liệu cấu hình (bật/tắt cờ có điều kiện + bộ chỉ tiêu gợi ý khi tạo Inspection Request); Inspection Request tạo ra vẫn là nguồn dữ liệu mà QTN-21 tiêu thụ.
- BR-7/BR-8 CONFIRMED tại đây: lô đã kích hoạt không bị áp dụng lại cấu hình mới; lô mới kiểm tra cấu hình tại thời điểm kích hoạt.
- Module/file ảnh hưởng nếu có thay đổi sau này: `LotService.activateQR`, `LotRepository.existsPassedInspection`.

---

## 9. Mapping Acceptance Criteria

| AC | API/Logic | Kết quả |
|---|---|---|
| TC-01 | POST `/api/v1/inspection-criteria` (§4.2) + PUT `/api/v1/product-categories/{id}/criteria` (§4.8) + GET `/api/v1/product-categories/{id}/criteria` (§4.7) + MODIFY POST `/api/v1/inspection-requests` (§4.10.2 prefill) | Đáp ứng — NEW/MODIFY theo thiết kế |
| TC-02 | PUT `/api/v1/product-categories/{id}/mandatory-inspection` (§4.9) + BR-3 trong `ProductCategoryService.update` — trả 400 khi chưa có chỉ tiêu | Đáp ứng |
| TC-03 | Validation `maxThreshold > 0` trong `InspectionCriterionRequest` (POST/PUT §4.2/§4.4) — trả 400 | Đáp ứng |
| TC-04 | `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` trên các endpoint quản lý danh mục → COOPERATIVE_ADMIN nhận 403 | Đáp ứng — cần implement enforcement (gap §2.4) |
| TC-05 | DELETE `/api/v1/inspection-criteria/{id}` (§4.6) check `inspection_requests.criterion_id` → 409 + message hướng dẫn; PUT `/disable` (§4.5) vẫn cho phép | Đáp ứng |

---

## 10. Backend Implementation Scope

1. **Migration** — NEW — PROPOSED `V5__inspection_criteria.sql` (theo pattern Flyway hiện có):
    - Bảng `inspection_criteria`: `id`, `name VARCHAR(150) NOT NULL`, `unit VARCHAR(30)`, `max_threshold NUMERIC(12,4) NOT NULL`, `reference_standard VARCHAR(150)`, `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`, `created_at`, `updated_at`; unique `(name, reference_standard)`.
    - Bảng `category_criteria`: `id`, `product_category_id BIGINT NOT NULL REFERENCES product_categories(id)`, `criterion_id BIGINT NOT NULL REFERENCES inspection_criteria(id) ON DELETE CASCADE`; unique `(product_category_id, criterion_id)`.
    - `ALTER TABLE inspection_requests ADD COLUMN criterion_id BIGINT REFERENCES inspection_criteria(id);` (nullable — không hồi tố dữ liệu cũ).
    - (Tùy chọn) seed một vài chỉ tiêu mẫu + gán cho `RAU-AN-LA` phục vụ test TC-01.
2. **Entity/Repository** — NEW — PROPOSED: `InspectionCriterion`, `CategoryCriterion` + `InspectionCriterionRepository`, `CategoryCriterionRepository` (theo pattern entity hiện có: Lombok `@Builder`, `@PrePersist`/`@PreUpdate`, `Instant` timestamps).
3. **Service** — NEW — PROPOSED `InspectionCriterionService`: CRUD, disable, check trùng (BR-1), check ngưỡng (BR-2), check tham chiếu trước khi xóa (BR-5), gán bộ chỉ tiêu.
4. **Controller** — NEW — PROPOSED `InspectionCriterionController` tại `/api/v1/inspection-criteria`; 2 endpoint con của product category (§4.7, §4.8, §4.9) đặt trong `ProductCategoryController` (MODIFY).
5. **DTO** — NEW — PROPOSED: `InspectionCriterionRequest`, `InspectionCriterionResponse`, `CategoryCriteriaRequest`, `MandatoryInspectionRequest`.
6. **MODIFY**:
    - `ProductCategoryService.update` + endpoint tương ứng: thêm rule BR-3.
    - `InspectionRequestCreateRequest`: thêm `criterionId` optional; `InspectionRequestService.create` prefill từ criterion.
    - `InspectionRequestRepository`: thêm `existsByCriterionId(Long)` hoặc `countByCriterionId(Long)`.
    - Thêm `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` cho: InspectionCriterionController (write ops), POST/PUT product-categories, PUT criteria, PUT mandatory-inspection.
7. **Security**: dùng `@EnableMethodSecurity` (đã bật) + `@PreAuthorize`; không tạo cơ chế authorization mới.
8. Không sửa logic `LotService.activateQR` (REUSE).

---

## 11. Frontend Implementation Scope

1. **API client** (`frontend/src/api/index.ts` — MODIFY): thêm interface `InspectionCriterion` và `inspectionCriterionApi` theo pattern `productCategoryApi` hiện có:
    - `list(page, size, keyword?, status?)`, `get(id)`, `create(payload)`, `update(id, payload)`, `disable(id)`, `enable(id)`, `remove(id)`.
    - `productCategoryApi.getCriteria(id, activeOnly?)`, `productCategoryApi.assignCriteria(id, criterionIds)`, `productCategoryApi.setMandatoryInspection(id, required)`.
2. **Trang quản lý chỉ tiêu** (NEW — VD `InspectionCriteriaPage.tsx`, theo pattern `ProductCategoriesPage.tsx`):
    - Bảng danh sách chỉ tiêu (tên, đơn vị, ngưỡng, tiêu chuẩn, trạng thái).
    - Form thêm/sửa với validate client-side ngưỡng > 0.
    - Nút "Ngừng sử dụng"/"Kích hoạt lại"; nút "Xóa" chỉ hiện khi `referenced = false`; khi xóa bị 409 hiển thị message từ API.
    - Chỉ hiển thị chức năng quản trị khi role PLATFORM_ADMIN (theo pattern phân quyền UI hiện có — kiểm tra `Layout.tsx`/auth context khi implement).
3. **Mở rộng ProductCategoriesPage** (MODIFY):
    - Action bật/tắt "Bắt buộc kiểm nghiệm" gọi API §4.9; hiển thị lỗi TC-02.
    - Quản lý bộ chỉ tiêu của category (chọn từ danh sách chỉ tiêu ACTIVE).
4. **Mở rộng InspectionRequestsPage** (MODIFY):
    - Form tạo yêu cầu kiểm nghiệm: thêm select "Chọn chỉ tiêu" nạp từ GET `/product-categories/{lot.productCategoryId}/criteria`; khi chọn, tự điền tên/đơn vị/ngưỡng/tiêu chuẩn; giữ fallback nhập tay.
5. Không thay đổi format response/pagination — REUSE convention hiện có.

---

## 12. Điểm chưa đủ thông tin

| # | Điểm chưa rõ | Xử lý đề xuất |
|---|---|---|
| 1 | `unit` và `referenceStandard` có bắt buộc khi tạo chỉ tiêu không? Story liệt kê là thành phần của chỉ tiêu nhưng không nói rõ bắt buộc | Thiết kế hiện tại: `unit` bắt buộc, `referenceStandard` tùy chọn (BR-1 so sánh theo tiêu chuẩn — NULL được coi là một nhóm riêng). Backend Agent xác nhận lại với PO nếu cần |
| 2 | Có cần endpoint `enable` (kích hoạt lại) không? Story chỉ nói "ngừng sử dụng" | PROPOSED có `enable` để hoàn chỉnh vòng đời; frontend có thể ẩn nếu không dùng |
| 3 | Xóa chỉ tiêu chưa tham chiếu là xóa cứng (DELETE) hay chỉ cho phép disable? Story nói "không được xóa nếu đang tham chiếu" → ngầm định xóa cứng được phép khi chưa tham chiếu | Thiết kế DELETE xóa cứng cho trường hợp chưa tham chiếu; nếu PO muốn an toàn hơn có thể bỏ DELETE và chỉ dùng disable |
| 4 | Inspection Request có bắt buộc chọn từ danh mục chỉ tiêu không, hay vẫn cho nhập tay? | Thiết kế hiện tại: `criterionId` optional — giữ tương thích nhập tay (EXISTING behavior) |
| 5 | Quy ước đặt tên endpoint con của product-category (`/criteria`, `/mandatory-inspection`) | PROPOSED theo REST pattern hiện có của repository; Backend Agent có thể điều chỉnh nếu team có convention khác |

---

## 13. Quyết định thiết kế

| # | Quyết định | Lý do |
|---|---|---|
| D-1 | Tách endpoint `/mandatory-inspection` thay vì chỉ dùng PUT product-categories/{id} | Gắn rule BR-3 rõ ràng, không phá contract hiện có; frontend gọi 1 endpoint chuyên biệt |
| D-2 | Gán bộ chỉ tiêu dùng semantic REPLACE (PUT với danh sách đầy đủ) | Đơn giản cho frontend, tránh race condition khi nhiều admin sửa đồng thời, idempotent |
| D-3 | Thêm cột `inspection_requests.criterion_id` (nullable) thay vì bảng mapping | 1 Inspection Request = 1 chỉ tiêu (theo entity hiện có); nullable để không hồi tố dữ liệu cũ (BR-7) |
| D-4 | Inspection Request giữ snapshot free-text (`criterion_name`, `unit`, `max_threshold`, `reference_standard`) kể cả khi tạo từ criterion | BR-7: cấu hình mới không hồi tố; dữ liệu request là bằng chứng tại thời điểm kiểm tra |
| D-5 | Trạng thái chỉ tiêu: `ACTIVE`/`INACTIVE` (string) | Theo pattern trạng thái hiện có trong hệ thống (`lots.status`, `inspection_requests.status` đều là VARCHAR) |
| D-6 | Trả trường `referenced` trong response chỉ tiêu | Hỗ trợ UX TC-05: frontend biết khi nào được phép xóa |
| D-7 | Không sửa QTN-21 (`LotService.activateQR`) | Logic chặn đã chạy trên dữ liệu cấu hình; story chỉ bổ sung nguồn cấu hình |
| D-8 | Enforce quyền bằng `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` | `@EnableMethodSecurity` đã bật sẵn; đúng yêu cầu QTN-17 §4.5 |