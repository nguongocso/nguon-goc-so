# NCL-09-CN-011 — API Design: Cấu hình mốc canh tác bắt buộc theo loại nông sản và tiêu chuẩn

> Loại tài liệu: API Contract **đã implement** (tài liệu sau khi code). Dùng làm reference khi bảo trì / mở rộng.
> Người dùng: Backend Agent, Frontend Agent, QA.
> Nhãn trạng thái: tất cả endpoint dưới đây là `NEW` trong story và đã được implement trên nhánh `feature/NCL-675-cultivation-milestone`; dữ liệu mô tả bám sát code thực tế hiện tại.

---

## 1. Tổng quan

Story NCL-09-CN-011 yêu cầu:

1. Quản lý danh mục **mốc canh tác** dùng chung (tên, mô tả, loại hoạt động, số ngày dự kiến từ khi gieo, trạng thái): thêm, sửa, ngừng sử dụng, xóa có điều kiện.
2. **Gán mốc canh tác bắt buộc** cho từng loại nông sản (Product Category), **theo phạm vi tiêu chuẩn** (standard) hoặc GLOBAL (standard = NULL).
3. **Đánh dấu bắt buộc/không bắt buộc theo từng mốc** khi gán (`is_mandatory`).
4. **Chặn đóng gói** lô nếu chưa đủ mốc bắt buộc (QTN-35): liệt kê đích danh mốc còn thiếu.
5. Toàn bộ quản lý danh mục dùng chung chỉ do **VT-01 (PLATFORM_ADMIN)** thực hiện (QTN-17).

Phạm vi API trong tài liệu này:

- CRUD danh mục mốc canh tác: `/api/v1/cultivation-milestones`
- Gán/đọc mốc cho phân loại nông sản: `/api/v1/product-categories/{categoryId}/milestones`
- Hàm validate nội bộ tích hợp vào luồng đóng gói (`recordPackagingEvent`) — không phải REST public.

---

## 2. Kết quả phân tích repository

### 2.1 Entity/domain liên quan

| Thành phần | File | Vai trò | Trạng thái |
|---|---|---|---|
| `CultivationMilestoneCatalog` | `backend/.../certification/entity/CultivationMilestoneCatalog.java` | Danh mục mốc canh tác (PK Long) | NEW — đã implement |
| `ProductCategoryMilestone` | `backend/.../certification/entity/ProductCategoryMilestone.java` | Bảng gán mốc ↔ loại nông sản (+ standard scope + is_mandatory) | NEW — đã implement |
| `ProductCategory` | `backend/.../farm/entity/ProductCategory.java` | Loại nông sản | EXISTING — REUSE |
| `Standard` | `backend/.../certification/entity/Standard.java` | Tiêu chuẩn chất lượng (scope gán) | EXISTING — REUSE |
| `ProductionLot` | `backend/.../farm/entity/ProductionLot.java` | Lô sản xuất; kiểm tra khi đóng gói | EXISTING — REUSE |
| `ProductionLotCertification` | `backend/.../certification/entity/ProductionLotCertification.java` | Certification của lô → lấy standard_ids | EXISTING — REUSE |
| `FarmLog` | `backend/.../farm/entity/FarmLog.java` | Nhật ký canh tác; so khớp mốc theo `activity_type` | EXISTING — REUSE |
| `FarmActivityType` | `backend/.../farm/enums/FarmActivityType.java` | Enum: `PLANTING, WATERING, FERTILIZING, PESTICIDE, WEEDING, HARVESTING, OTHER` | EXISTING — REUSE |

### 2.2 Migration liên quan

- **Schema** — `backend/src/main/resources/db/migration/schema/V63__create_cultivation_milestone_tables.sql`:
  - `cultivation_milestone_catalog`: `id BIGINT AUTO_INCREMENT`, `name VARCHAR(255) NOT NULL`, `description`, `activity_type VARCHAR(50) NOT NULL`, `expected_days_from_planting INT`, `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'` (CHECK IN `ACTIVE`/`INACTIVE`), `created_at`, `updated_at`; **UNIQUE `(name, activity_type)`**, index name/activity_type/status.
  - `product_category_milestones`: `id CHAR(36)` UUID, `category_id CHAR(36) NOT NULL FK product_categories`, `milestone_id BIGINT NOT NULL FK cultivation_milestone_catalog`, `standard_id CHAR(36) NULL FK standards`, `is_mandatory BOOLEAN NOT NULL DEFAULT TRUE`, `created_at`; **UNIQUE `(category_id, milestone_id, standard_id)`**, index theo milestone/standard.
- **Seed** — `backend/src/main/resources/db/migration/data/V64__seed_cultivation_milestones.sql`:
  - 4 mốc baseline: Gieo trồng (PLANTING), Bón phân đợt 1 (FERTILIZING), Phun thuốc phòng ngừa (PESTICIDE), Thu hoạch (HARVESTING).
  - Gán GLOBAL (standard = NULL, `is_mandatory = TRUE`) cho **tối đa 2 product category active đầu** (subquery `ORDER BY name LIMIT 2`).

> Quy ước migration (AGENTS.md): `spring.jpa.hibernate.ddl-auto=validate` — entity phải đồng bộ chính xác với bảng; mọi thay đổi lược đồ sau này phải qua migration mới (schema tiếp theo `V65__...`, data tiếp theo sau `V64`).

---

## 3. Domain và Business Rules

| # | Rule | Nguồn | Trạng thái |
|---|---|---|---|
| BR-1 | Không trùng `(name, activity_type)` trong danh mục mốc | Story | Implement — trả 409 |
| BR-2 | `expectedDaysFromPlanting` (nếu có) phải ≥ 1 | Story | Implement — validation `@Min(1)` → 400 |
| BR-3 | Chỉ **VT-01** quản lý danh mục dùng chung | QTN-17 | Implement — `@PreAuthorize("hasRole('VT-01')")` + check lại trong service |
| BR-4 | Mốc đang được gán (referenced) **không xóa được** | Story + QTN-17 §5 | Implement — trả 409 |
| BR-5 | Chỉ gán mốc `ACTIVE`; mốc `INACTIVE` bị từ chối khi gán | Story | Implement — trả 400 |
| BR-6 | `is_mandatory` theo từng mốc; null `mandatoryMilestoneIds` = tất cả bắt buộc | Story ("có bắt buộc hay không") | Implement |
| BR-7 | Quy tắc standard scope: lô có certifications → mốc theo standard_ids + GLOBAL; lô không certifications → chỉ GLOBAL | Story + QTN-35 | Implement (`findMandatoryMilestonesForValidation`) |
| BR-8 | Matching **1:1** mốc ↔ FarmLog theo `activity_type` (1 FarmLog không satisfy nhiều mốc) | User confirm | Implement |
| BR-9 | Chặn cứng đóng gói khi thiếu mốc, liệt kê đích danh | QTN-35, TC-02 | Implement (`recordPackagingEvent`) |
| BR-10 | Không hồi tố: lô đã PACKAGED không bị đánh giá lại khi thay đổi cấu hình | Story | Implement (validate chỉ chạy lúc đóng gói) |

---

## 4. API Contract

Convention chung (EXISTING — bắt buộc tuân theo):

- Base path: `/api/v1`.
- Response wrapper: `ApiResult<T>` — `{ success, status, message, data, errors, path, timestamp }`.
- Pagination: `PageResponse<T>` — `{ content, page, size, totalElements, totalPages, last }`.
- Lỗi nghiệp vụ: `BusinessException(HttpStatus, "message tiếng Việt")` → `GlobalExceptionHandler` map tự động; không tìm thấy: `ResourceNotFoundException` (404).
- Xác thực: `Authorization: Bearer <JWT>` (interceptor frontend tự gắn).

Role sử dụng: **`VT-01`** = PLATFORM_ADMIN.

### 4.1 Danh sách mốc canh tác

- **Method**: `GET`
- **Endpoint**: `/api/v1/cultivation-milestones`
- **Authorization**: mọi user đã đăng nhập (`isAuthenticated()`).
- **Query parameters**:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `keyword` | string | Không | — | Tìm theo tên mốc |
| `status` | string | Không | — | `ACTIVE` hoặc `INACTIVE`; nếu sai → 400 |
| `activityType` | string | Không | — | Lọc theo loại hoạt động (enum FarmActivityType) |
| `page` | int | Không | 0 | 0-based |
| `size` | int | Không | 10 | Số bản ghi/trang |

- **Response 200**: `ApiResult<PageResponse<CultivationMilestoneCatalogResponse>>`

```json
{
  "success": true,
  "status": 200,
  "message": "Thành công",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Gieo trồng ban đầu",
        "description": "Tiến hành gieo trồng lô hàng theo quy trình",
        "activityType": "PLANTING",
        "expectedDaysFromPlanting": 0,
        "status": "ACTIVE",
        "referenced": true,
        "createdAt": "2026-09-03T07:00:00",
        "updatedAt": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 4,
    "totalPages": 1,
    "last": true
  },
  "errors": null,
  "path": "/api/v1/cultivation-milestones",
  "timestamp": "2026-09-03T07:00:00"
}
```

- **Error cases**: 400 (status không hợp lệ: `"Trạng thái không hợp lệ. Giá trị hợp lệ: ACTIVE, INACTIVE."`), 401.

> Trường `referenced`: `true` nếu mốc đã được gán cho ít nhất 1 loại nông sản → frontend ẩn nút Xóa (theo quyết định hướng A, không còn text "Không xóa").

### 4.2 Chi tiết mốc canh tác

- **Method**: `GET`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}`
- **Authorization**: mọi user đã đăng nhập.
- **Path parameter**: `id` (Long).
- **Response 200**: `ApiResult<CultivationMilestoneCatalogResponse>` (như §4.1, kèm `referenced`).
- **Error cases**: 401, 404 (`"Mốc canh tác không tồn tại."`).

### 4.3 Tạo mốc canh tác

- **Method**: `POST`
- **Endpoint**: `/api/v1/cultivation-milestones`
- **Authorization**: chỉ `VT-01` — `@PreAuthorize("hasRole('VT-01')")` + `validateAdminPermission`.
- **Request body** — `CultivationMilestoneCatalogRequest`:

```json
{
  "name": "Làm cỏ đợt 2",
  "description": "Làm cỏ lần 2 cho lô",
  "activityType": "WEEDING",
  "expectedDaysFromPlanting": 30
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `name` | string | Có | `@NotBlank`, ≤ 150 ký tự; trim |
| `description` | string | Không | ≤ 500 ký tự |
| `activityType` | string | Có | `@NotBlank`; giá trị thuộc `FarmActivityType` (nếu không khớp enum sẽ không được matching khi đóng gói) |
| `expectedDaysFromPlanting` | integer | Không | `@Min(1)` nếu có (chú ý: seed Gieo trồng dùng 0) |

- **Response 201**: `ApiResult<CultivationMilestoneCatalogResponse>` — `status` mặc định `ACTIVE`.
- **Business rule**: BR-1 trùng `(name, activityType)` — tên so sánh **không phân biệt hoa thường** (`LOWER(m.name) = LOWER(:name)`), hoạt động so sánh chính xác → **409** `"Mốc canh tác với tên và loại hoạt động này đã tồn tại."`
- **Error cases**: 400 (validation/`@Min`), 401, **403** (`"Bạn không có quyền quản lý danh mục mốc canh tác."`), 409.

### 4.4 Cập nhật mốc canh tác

- **Method**: `PUT`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}`
- **Authorization**: chỉ `VT-01`.
- **Path parameter**: `id` (Long).
- **Request body**: giống §4.3 (`CultivationMilestoneCatalogRequest`).
- **Response 200**: `ApiResult<CultivationMilestoneCatalogResponse>` — dữ liệu sau cập nhật.
- **Business rule**: BR-1 trùng loại trừ chính nó (`existsByNameAndActivityTypeAndIdNot`) → 409. Mốc `INACTIVE` vẫn sửa được. Không hồi tố gán đã có (chỉ đổi dữ liệu danh mục).
- **Error cases**: 400, 401, 403, 404, 409.

### 4.5 Ngừng sử dụng mốc

- **Method**: `PUT`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}/disable`
- **Authorization**: chỉ `VT-01`.
- **Path parameter**: `id` (Long).
- **Response 200**: `ApiResult<Void>` (data null).
- **Business rule**: đổi `status` → `INACTIVE`. **Cho phép ngay cả khi đang referenced** (chỉ chặn xóa). Mốc `INACTIVE` không xuất hiện khi gán (BR-5) và bị bỏ qua khi validate đóng gói (`findMandatoryMilestonesForValidation` lọc `milestone.status = 'ACTIVE'`).
- **Error cases**: 401, 403, 404.

### 4.6 Kích hoạt lại mốc

- **Method**: `PUT`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}/enable`
- **Authorization**: chỉ `VT-01`.
- **Path parameter**: `id` (Long).
- **Response 200**: `ApiResult<CultivationMilestoneCatalogResponse>` — `status = "ACTIVE"`.
- **Error cases**: 401, 403, 404.

### 4.7 Xóa mốc canh tác

- **Method**: `DELETE`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}`
- **Authorization**: chỉ `VT-01`.
- **Path parameter**: `id` (Long).
- **Response 200**: `ApiResult<Void>`.
- **Business rule**: BR-4 — nếu `existsByMilestone_Id(id)` → **409** `"Mốc canh tác đang được gán cho loại nông sản, không thể xóa."`. Xóa cứng khi chưa referenced.
- **Error cases**: 401, 403, 404, **409**.

### 4.8 Lấy mốc theo loại nông sản

- **Method**: `GET`
- **Endpoint**: `/api/v1/product-categories/{categoryId}/milestones`
- **Authorization**: mọi user đã đăng nhập.
- **Path parameter**: `categoryId` (UUID — CHAR(36)).
- **Response 200**: `ApiResult<List<ProductCategoryMilestoneResponse>>` (không phân trang):

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "9f3d...-uuid",
      "milestone": {
        "id": 1,
        "name": "Gieo trồng ban đầu",
        "description": "...",
        "activityType": "PLANTING",
        "expectedDaysFromPlanting": 0,
        "status": "ACTIVE",
        "referenced": true,
        "createdAt": "...",
        "updatedAt": null
      },
      "standardId": null,
      "standardName": null,
      "isMandatory": true
    }
  ],
  "path": "/api/v1/product-categories/{id}/milestones"
}
```

- **Error cases**: 401, 404 (`"Loại nông sản không tồn tại."`).

### 4.9 Gán (thay thế) mốc cho loại nông sản

- **Method**: `PUT`
- **Endpoint**: `/api/v1/product-categories/{categoryId}/milestones`
- **Authorization**: chỉ `VT-01`.
- **Path parameter**: `categoryId` (UUID).
- **Request body** — `CategoryMilestoneRequest`:

```json
{
  "milestoneIds": [1, 2, 3],
  "standardId": "550e8400-...",
  "mandatoryMilestoneIds": [1, 2]
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `milestoneIds` | array[Long] | Có (`@NotNull`) | Được phép mảng rỗng (xóa hết gán cho scope); từng id phải tồn tại + `ACTIVE`; trùng trong mảng được gộp (LinkedHashSet) |
| `standardId` | UUID | Không | NULL = **GLOBAL** scope; nếu có phải tồn tại |
| `mandatoryMilestoneIds` | array[Long] | Không | Id các mốc được đánh dấu **bắt buộc**; **null (không gửi) = tất cả bắt buộc**; id không nằm trong list này → `is_mandatory = false` |

- **Response 200**: `ApiResult<List<ProductCategoryMilestoneResponse>>` — bộ gán **sau** khi thay thế.
- **Semantic**: REPLACE theo phạm vi standard — các mốc hiện có trong cùng standard scope mà không nằm trong `milestoneIds` bị **xóa**; giữ nguyên gán của standard scope khác (so bằng `standard.id` hoặc cả hai NULL).
- **Validation / Business rules**:
    - Category tồn tại → 404 nếu không.
    - Từng `milestoneId` tồn tại → 404 `"Mốc canh tác <id> không tồn tại."`.
    - Mốc `INACTIVE` → 400 `"Mốc canh tác '<name>' đã ngừng sử dụng, không thể gán."` (BR-5).
    - `standardId` tồn tại → 404 `"Tiêu chuẩn <id> không tồn tại."`.
- **Error cases**: 400, 401, **403** (`"Bạn không có quyền quản lý danh mục dùng chung."`), 404.

> Lưu ý (AGENTS.md): controller khác với parallel feature — `assignMilestones` dùng `findById` (không `existsById`) nên category không tồn tại là `ResourceNotFoundException` (404); `getCategoryMilestones` dùng `existsById`.

---

## 5. Hàm kiểm tra đóng gói (nội bộ, không phải REST)

Thông qua `MilestoneValidationService.validateMilestoneCompletion(ProductionLot)` được gọi trong `ChainEventServiceImpl.recordPackagingEvent` trước khi đặt lô `PACKAGED`.

Thuật toán (`MilestoneValidationServiceImpl`):

1. Lấy `standardIds` từ `lot.getCertifications()` ⇒ `certification.getStandard().getId()`, distinct (loại bỏ certification không có standard).
2. Query `findMandatoryMilestonesForValidation(categoryId, standardIds)` — chỉ lấy `isMandatory = true`, `milestone.status = 'ACTIVE'`, và `(pcm.standard IS NULL OR pcm.standard.id IN :standardIds)`.
   - Nếu lô **không có certification** → `standardIds` rỗng → chỉ khớp GLOBAL (standard IS NULL).
3. Lấy FarmLog của lô, lọc bỏ `corrected`.
4. Đếm số FarmLog theo `activity_type`.
5. So khớp **1:1**: mỗi mốc cần đúng 1 FarmLog có `activity_type` trùng; cùng activity_type không satisfy nhiều mốc.
6. Trả danh sách **tên mốc còn thiếu**.

Khi danh sách không rỗng, `recordPackagingEvent` ném `BusinessException`:

```
Lô chưa đủ mốc canh tác bắt buộc: Bón phân đợt 1, Phun thuốc phòng ngừa
```

- Lỗi này được ghi nhận vào `failed_event_logs` (`eventValidationService.logFailedAttempt`, ChainEventType.PACKAGING) rồi ném tiếp → client nhận 400.
- Lô giữ trạng thái cũ; chỉ khi đủ mốc mới chuyển sang `PACKAGED`.

> Chỉ một FarmLog không-corrected tương ứng mỗi mốc; FarmLog corrected bị loại (nên nếu sửa nhật ký thành corrected, mốc tương ứng sẽ báo thiếu trở lại).

---

## 6. Error Codes

Theo `GlobalExceptionHandler` (EXISTING — REUSE):

| HTTP | Ý nghĩa | Nguồn | Áp dụng |
|---|---|---|---|
| 400 | Validation / business | `BusinessException(BAD_REQUEST)` | status sai, mốc INACTIVE, `@Min`, thiếu mốc đóng gói |
| 401 | Chưa xác thực | Spring Security | mọi endpoint |
| 403 | Không đủ quyền | `@PreAuthorize` + `validateAdminPermission` | không phải VT-01 |
| 404 | Không tìm thấy | `ResourceNotFoundException` / `BusinessException(NOT_FOUND)` | id mốc/standard/category |
| 409 | Xung đột | `BusinessException(CONFLICT)` | trùng `(name, activityType)`, xóa mốc referenced |

---

## 7. Permission Matrix

| API | VT-01 | VT-02/03/04/05/06 |
|---|---|---|
| `GET /cultivation-milestones` | ✓ | ✓ (đăng nhập) |
| `GET /cultivation-milestones/{id}` | ✓ | ✓ |
| `POST /cultivation-milestones` | ✓ | ✗ (403) |
| `PUT /cultivation-milestones/{id}` | ✓ | ✗ |
| `PUT /cultivation-milestones/{id}/disable` | ✓ | ✗ |
| `PUT /cultivation-milestones/{id}/enable` | ✓ | ✗ |
| `DELETE /cultivation-milestones/{id}` | ✓ | ✗ |
| `GET /product-categories/{id}/milestones` | ✓ | ✓ |
| `PUT /product-categories/{id}/milestones` | ✓ | ✗ |
| `recordPackagingEvent` (nội bộ) | — | chạy theo luồng VT-02 đóng gói |

Quản trị danh mục mốc + gán mốc: **chỉ VT-01** (QTN-17). `CultivationMilestoneCatalogServiceImpl` và `CategoryMilestoneAssignmentServiceImpl` check lại `validateAdminPermission` (VT-01) trong service (belt-and-suspenders).

---

## 8. Seed & Demo Account

- Seed chuẩn: `V64__seed_cultivation_milestones.sql` — 4 mốc baseline, GLOBAL cho 2 category active đầu.
- Account test:
  - **VT-01** (Platform Admin): `admin` / `admin123` (org SYSTEM, 1 org — tự động hạ cánh `/dashboard` sau login).
  - **VT-02** (Quản lý HTX): `orgmanager` / `admin123` (org DEMO_HTX) — dùng để ghi nhật ký + đóng gói.
- Để test CRUD/gán mốc: login `admin`, mở menu trái **"Mốc canh tác"** (nhóm Quản lý) → `/admin/cultivation-milestones`.
- Để test chặn đóng gói: `orgmanager` chọn lô thuộc category có mốc bắt buộc, ghi đủ/thiếu nhật ký tương ứng rồi đóng gói.

---

## 9. Backend Implementation Scope (đã hoàn thành)

1. Migration schema `V63` + seed data `V64` (`schema/` và `data/`).
2. Entity + Repository: `CultivationMilestoneCatalog`, `ProductCategoryMilestone`; repositories `CultivationMilestoneCatalogRepository`, `ProductCategoryMilestoneRepository` (query `search`, `findByCategoryIdWithMilestones`, `findReferencedMilestoneIds`, `findMandatoryMilestonesForValidation`, `deleteByCategory_IdAndMilestone_IdIn`, `existsByMilestone_Id`).
3. Service + Impl: `CultivationMilestoneCatalogService` (CRUD + disable/enable + delete guard), `CategoryMilestoneAssignmentService` (REPLACE assign + is_mandatory), `MilestoneValidationService` (validate đóng gói).
4. Controller: `CultivationMilestoneCatalogController` (`/api/v1/cultivation-milestones`), `CategoryMilestoneAssignmentController` (`/api/v1/product-categories/{id}/milestones`) — cả hai `@PreAuthorize` + check service.
5. DTO: `CultivationMilestoneCatalogRequest/Response`, `CategoryMilestoneRequest` (milestoneIds + standardId + mandatoryMilestoneIds), `ProductCategoryMilestoneResponse`.
6. MODIFY: `ChainEventServiceImpl.recordPackagingEvent` — chèn validate mốc trước khi đặt `PACKAGED`; inject `MilestoneValidationService`.

## 10. Frontend Implementation Scope (đã hoàn thành)

1. API client: `frontend/src/api/cultivationMilestoneApi.ts` (getMilestones, getMilestone, create, update, disable, enable, delete, getProductCategoryMilestones, assignProductCategoryMilestones).
2. Types: `frontend/src/types/cultivationMilestone.ts` (CultivationMilestone, CultivationMilestoneRequest, ProductCategoryMilestone, CategoryMilestoneRequest).
3. Trang: `CultivationMilestoneManagementPage.tsx` (CRUD + filter + pagination — bỏ text "Không xóa" khi referenced), `CreateCultivationMilestonePage.tsx`, `AssignMilestonesPage.tsx` (dropdown Standard GLOBAL + `getActiveStandards()`, Switch is_mandatory từng mốc, REPLACE-ALL save).
4. Điều hướng: route `/admin/cultivation-milestones` + `/admin/cultivation-milestones/create` + `/admin/product-categories/:id/milestones` (RoleRoute `['VT-01']`), Sidebar menu "Mốc canh tác", `ROLE_ACCESS.cultivationMilestoneManagement`, nút CalendarCheck trong `ProductCategoryList`.

## 11. Mapping Acceptance Criteria

| AC | API/Logic | Kết quả |
|---|---|---|
| TC-01 | POST/PUT gán mốc (§4.3/§4.9) + GET theo category (§4.8) + validate đóng gói (§5) | Implement |
| TC-02 | `recordPackagingEvent` ném 400 liệt kê mốc thiếu (§5) | Implement |
| TC-03 | Validate chỉ chạy lúc đóng gói → không hồi tố lô đã PACKAGED | Implement |
| TC-04 | Trùng `(name, activityType)` → 409 | Implement |
| TC-05 | Không phải VT-01 → 403 + menu ẩn | Implement |

## 12. Quyết định thiết kế

| # | Quyết định | Lý do |
|---|---|---|
| D-1 | PK danh mục = BIGINT AUTO_INCREMENT | Mirror `inspection_criterion_catalog`; bảng tham chiếu nhẹ |
| D-2 | `product_category_milestones.standard_id` NULL = GLOBAL | Phạm vi gán: GLOBAL hoặc theo standard |
| D-3 | UNIQUE `(category_id, milestone_id, standard_id)` | Chống trùng trong cùng scope (TC-04) |
| D-4 | Matching 1:1 mốc ↔ FarmLog theo `activity_type` | Theo xác nhận user |
| D-5 | Validate chỉ tại `recordPackagingEvent` | QTN-35; không chặn lúc ghi nhật ký |
| D-6 | Chặn cứng (không cảnh báo) | TC-02 + QTN-35 |
| D-7 | Cấu hình mới không hồi tố lô đã đóng gói | QTN-35 + story |
| D-8 | `is_mandatory` theo từng mốc qua `mandatoryMilestoneIds` (null = tất cả bắt buộc) | Spec "có bắt buộc hay không" là thuộc tính khai báo riêng từng mốc |
| D-9 | Gán mốc dùng semantic REPLACE theo scope standard | Đơn giản cho frontend, idempotent, giữ gán của scope khác |
