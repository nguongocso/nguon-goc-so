# NCL-09-CN-011 — API Design: Cấu hình mốc canh tác (bảng hợp nhất)

> Loại tài liệu: API Contract **đã implement** (tài liệu sau khi code). Dùng làm reference khi bảo trì / mở rộng.
> Người dùng: Backend Agent, Frontend Agent, QA.
> Nhãn trạng thái: tất cả endpoint là `NEW` trong story và đã implement trên nhánh `feature/NCL-675-cultivation-milestone`.
> **Lưu ý:** doc này mô tả **thiết kế đã được gộp lại từ 2 bảng → 1 bảng** theo quyết định nghiệp vụ mới (bỏ status ACTIVE/INACTIVE "ngừng sử dụng" → thay bằng `is_mandatory`; bỏ bước gán theo category riêng; bỏ xóa mốc).

---

## 1. Tổng quan

Story NCL-09-CN-011 yêu cầu (theo mô tả nghiệp vụ được xác nhận lại):

1. Admin khai báo **mốc canh tác** với: tên, mô tả, **loại nông sản áp dụng**, **tiêu chuẩn áp dụng**, **bắt buộc**, **expected days từ ngày gieo trồng**, và **loại hoạt động** (để đối chiếu nhật ký).
2. Bộ mốc áp dụng cho lô mới theo **loại nông sản** + **bộ tiêu chuẩn** gắn cho lô.
3. Kiểm tra pre-packaging đối chiếu nhật ký (`farm_logs`) với mốc **bắt buộc** và **liệt kê mốc còn thiếu**.
4. Thay đổi cấu hình **không hồi tố** với lô đã đóng gói (validate chỉ chạy lúc đóng gói).
5. Chỉ **VT-01** được sửa (đã đúng).
6. **Chặn trùng tên mốc trong cùng loại nông sản + tiêu chuẩn**.
7. **Gộp 2 bảng về 1 bảng** (`cultivation_milestone`).
8. **Thay "ngừng sử dụng" = "bắt buộc"** (bỏ status ACTIVE/INACTIVE + disable/enable).

Phạm vi API trong tài liệu này:

- CRUD (không có DELETE) danh mục mốc canh tác: `/api/v1/cultivation-milestones`
- Hàm validate nội bộ tích hợp vào luồng đóng gói (`recordPackagingEvent`) — không phải REST public.
- **Không còn** endpoint gán mốc theo category riêng (`/api/v1/product-categories/{id}/milestones`).

---

## 2. Kết quả phân tích repository

### 2.1 Entity/domain liên quan

| Thành phần | File | Vai trò | Trạng thái |
|---|---|---|---|
| `CultivationMilestone` | `backend/.../certification/entity/CultivationMilestone.java` | Bảng mốc hợp nhất (PK Long; nullable `productCategory`/`standard` = GLOBAL / mọi tiêu chuẩn; `isMandatory`) | NEW — đã implement |
| `ProductCategory` | `backend/.../farm/entity/ProductCategory.java` | Loại nông sản (NULL = toàn bộ) | EXISTING — REUSE |
| `Standard` | `backend/.../certification/entity/Standard.java` | Tiêu chuẩn chất lượng (NULL = mọi tiêu chuẩn) | EXISTING — REUSE |
| `ProductionLot` | `backend/.../farm/entity/ProductionLot.java` | Lô sản xuất; kiểm tra khi đóng gói | EXISTING — REUSE |
| `ProductionLotCertification` | `backend/.../certification/entity/ProductionLotCertification.java` | Certification của lô → lấy standard_ids | EXISTING — REUSE |
| `FarmLog` | `backend/.../farm/entity/FarmLog.java` | Nhật ký canh tác; so khớp mốc theo `activity_type` | EXISTING — REUSE |
| `FarmActivityType` | `backend/.../farm/enums/FarmActivityType.java` | Enum: `PLANTING, WATERING, FERTILIZING, PESTICIDE, WEEDING, HARVESTING, OTHER` | EXISTING — REUSE |

> Đã **xóa** các class cũ của phiên bản trước: `CultivationMilestoneCatalog`, `ProductCategoryMilestone` (entity); `CultivationMilestoneCatalogRepository`, `ProductCategoryMilestoneRepository`; services/controllers gán theo category.

### 2.2 Migration liên quan

- **Schema** — `backend/src/main/resources/db/migration/schema/V65__merge_cultivation_milestones.sql`:
  - Tạo `cultivation_milestone`: `id BIGINT AUTO_INCREMENT`, `name VARCHAR(150) NOT NULL`, `description VARCHAR(500)`, `activity_type VARCHAR(30) NOT NULL`, `expected_days_from_planting INT`, `product_category_id CHAR(36) NULL FK product_categories (CASCADE)`, `standard_id CHAR(36) NULL FK standards (SET NULL)`, `is_mandatory BOOLEAN NOT NULL DEFAULT TRUE`, `name_key VARCHAR(150) GENERATED ALWAYS AS (LOWER(name)) STORED`, `created_at`, `updated_at`.
  - **UNIQUE `(product_category_id, standard_id, name_key)`** — generated column `name_key` đảm bảo so khớp **không phân biệt hoa/thường** cho trường hợp 2 id không NULL; trường hợp id NULL (GLOBAL) được chặn **ở tầng service** (MySQL UNIQUE cho phép nhiều NULL).
  - Bỏ (`DROP`) 2 bảng cũ: `product_category_milestones`, `cultivation_milestone_catalog`.
- **Seed** — `backend/src/main/resources/db/migration/data/V66__seed_cultivation_milestones.sql`:
  - Cà phê (`...0008`): 4 mốc (Gieo trồng=0d, Bón phân lót=7d, Phun thuốc phòng sâu=21d VietGAP, Thu hoạch=90d) — bắt buộc.
  - Chè (`...0002`): 4 mốc (Gieo trồng=0d, Bón phân đợt 1=10d, Phun thuốc phòng ngừa=25d VietGAP — **tùy chọn**, Thu hoạch=75d).
  - Toàn bộ loại (NULL category, NULL standard): Kiểm tra tưới nước định kỳ (WATERING, 30d), Làm cỏ định kỳ (WEEDING, 45d) — tùy chọn.

---

## 3. Domain và Business Rules

| # | Rule | Trạng thái |
|---|---|---|
| BR-1 | Chặn trùng tên mốc trong cùng `(product_category_id, standard_id)`; so sánh tên **không phân biệt hoa/thường**. NULL (GLOBAL/mọi tiêu chuẩn) được xử lý tường minh bằng điều kiện `IS NULL` trong service | Implement — trả 409 |
| BR-2 | `expectedDaysFromPlanting` (nếu có) ≥ 0 | Implement — `@Min(0)` → 400 |
| BR-3 | Chỉ **VT-01** được thêm/sửa mốc | Implement — `@PreAuthorize("hasRole('VT-01')")` + check lại trong service |
| BR-4 | `activity_type` phải thuộc `FarmActivityType` | Implement — 400 |
| BR-5 | `productCategoryId`/`standardId` nếu có phải tồn tại | Implement — 404 |
| BR-6 | `isMandatory` bắt buộc trong request | Implement — `@NotNull` → 400 |
| BR-7 | `productCategoryId = NULL` = áp dụng **toàn bộ** loại; `standardId = NULL` = áp dụng **mọi** tiêu chuẩn | Implement |
| BR-8 | Validate đóng gói: chỉ xét mốc `is_mandatory = true`, category (NULL hoặc khớp loại lô), standard (NULL hoặc thuộc standard_ids lô) | Implement (`findMandatoryMilestonesForValidation`) |
| BR-9 | Matching **1:1** mốc ↔ FarmLog theo `activity_type`; liệt kê đích danh mốc thiếu | Implement |
| BR-10 | Chặn cứng đóng gói khi thiếu mốc bắt buộc | Implement (`recordPackagingEvent`) |
| BR-11 | Không hồi tố: lô đã `PACKAGED` không bị đánh giá lại khi thay đổi cấu hình | Implement (validate chỉ chạy lúc đóng gói) |
| BR-12 | **Không có xóa mốc** (không có endpoint DELETE) | Implement |
| BR-13 | **Không có disable/enable** ("ngừng sử dụng" đã thay bằng `is_mandatory`) | Implement |

---

## 4. API Contract

Convention chung (EXISTING — bắt buộc tuân theo):

- Base path: `/api/v1`.
- Response wrapper: `ApiResult<T>` — `{ success, status, message, data, errors, path, timestamp }`.
- Pagination: `PageResponse<T>` — `{ content, page, size, totalElements, totalPages, last }` (xem `PageResponse.from`).
- Lỗi nghiệp vụ: `BusinessException(HttpStatus, "message tiếng Việt")`; không tìm thấy: `ResourceNotFoundException` (404).
- Xác thực: `Authorization: Bearer <JWT>`.

Role sử dụng: **`VT-01`** = PLATFORM_ADMIN (chỉ được thêm/sửa).

### 4.1 Danh sách mốc canh tác

- **Method**: `GET`
- **Endpoint**: `/api/v1/cultivation-milestones`
- **Authorization**: mọi user đã đăng nhập (`isAuthenticated()`).
- **Query parameters**:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `keyword` | string | Không | — | Tìm theo tên mốc |
| `activityType` | string | Không | — | Lọc theo loại hoạt động (enum FarmActivityType); sai → 400 |
| `categoryId` | UUID | Không | — | Lọc theo loại nông sản |
| `standardId` | UUID | Không | — | Lọc theo tiêu chuẩn |
| `globalOnly` | boolean | Không | false | `true` → chỉ lấy mốc áp dụng toàn bộ loại (`product_category_id IS NULL`) |
| `page` | int | Không | 0 | 0-based |
| `size` | int | Không | 10 | Số bản ghi/trang |

- **Response 200**: `ApiResult<PageResponse<CultivationMilestoneResponse>>`

```json
{
  "success": true,
  "status": 200,
  "message": "Thành công",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Gieo trồng cà phê",
        "description": "Tiến hành gieo trồng lô cà phê theo quy trình",
        "activityType": "PLANTING",
        "expectedDaysFromPlanting": 0,
        "productCategoryId": "00000000-0000-0000-0000-000800000008",
        "productCategoryName": "Cà phê",
        "standardId": null,
        "standardName": null,
        "isMandatory": true,
        "createdAt": "2026-09-03T07:00:00",
        "updatedAt": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 10,
    "totalPages": 1,
    "last": true
  },
  "errors": null,
  "path": "/api/v1/cultivation-milestones",
  "timestamp": "2026-09-03T07:00:00"
}
```

- **Error cases**: 400 (activityType sai), 401.

> `productCategoryId`/`standardId` = `null` nghĩa là mốc áp dụng **toàn bộ** loại / **mọi** tiêu chuẩn.

### 4.2 Chi tiết mốc canh tác

- **Method**: `GET`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}`
- **Authorization**: mọi user đã đăng nhập.
- **Path parameter**: `id` (Long).
- **Response 200**: `ApiResult<CultivationMilestoneResponse>` (như §4.1).
- **Error cases**: 401, 404 (`"Mốc canh tác không tồn tại."`).

### 4.3 Tạo mốc canh tác

- **Method**: `POST`
- **Endpoint**: `/api/v1/cultivation-milestones`
- **Authorization**: chỉ `VT-01`.
- **Request body** — `CultivationMilestoneRequest`:

```json
{
  "name": "Làm cỏ đợt 2",
  "description": "Làm cỏ lần 2 cho lô",
  "activityType": "WEEDING",
  "expectedDaysFromPlanting": 30,
  "productCategoryId": "00000000-0000-0000-0000-000800000008",
  "standardId": null,
  "isMandatory": true
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `name` | string | Có | `@NotBlank`, ≤ 150 ký tự; trim |
| `description` | string | Không | ≤ 500 ký tự; trim |
| `activityType` | string | Có | `@NotBlank`; thuộc `FarmActivityType` (400 nếu sai) |
| `expectedDaysFromPlanting` | integer | Không | `@Min(0)` (0 = ngay ngày gieo trồng) |
| `productCategoryId` | UUID | Không | **NULL = toàn bộ loại**; nếu có phải tồn tại (404) |
| `standardId` | UUID | Không | **NULL = mọi tiêu chuẩn**; nếu có phải tồn tại (404) |
| `isMandatory` | boolean | Có | `@NotNull` |

- **Response 201**: `ApiResult<CultivationMilestoneResponse>`.
- **Business rule**: BR-1 trùng tên trong cùng `(product_category_id, standard_id)` → **409** `"Mốc canh tác với tên này đã tồn tại trong cùng loại nông sản và tiêu chuẩn."`
- **Error cases**: 400, 401, **403** (`"Bạn không có quyền quản lý danh mục mốc canh tác."`), 404 (category/standard), 409.

### 4.4 Cập nhật mốc canh tác

- **Method**: `PUT`
- **Endpoint**: `/api/v1/cultivation-milestones/{id}`
- **Authorization**: chỉ `VT-01`.
- **Path parameter**: `id` (Long).
- **Request body**: giống §4.3.
- **Response 200**: `ApiResult<CultivationMilestoneResponse>`.
- **Business rule**: trùng tên loại trừ chính nó → 409. Không hồi tố đối với lô đã đóng gói.
- **Error cases**: 400, 401, 403, 404, 409.

> **Không có** endpoint `disable`/`enable`/`delete` — khái niệm "ngừng sử dụng" và xóa mốc đã bị bỏ.

---

## 5. Hàm kiểm tra đóng gói (nội bộ, không phải REST)

Thông qua `MilestoneValidationService.validateMilestoneCompletion(ProductionLot)` được gọi trong `ChainEventServiceImpl.recordPackagingEvent` trước khi đặt lô `PACKAGED`.

Thuật toán (`MilestoneValidationServiceImpl`):

1. Lấy `standardIds` từ `lot.getCertifications()` ⇒ `certification.getStandard().getId()`, distinct (bỏ cert không có standard).
2. Query `CultivationMilestoneRepository.findMandatoryMilestonesForValidation(categoryId, standardIds)` — chỉ `is_mandatory = true`, và:
   - `(m.productCategory IS NULL OR m.productCategory.id = :categoryId)` — GLOBAL loại hoặc đúng loại lô.
   - `(m.standard IS NULL OR m.standard.id IN :standardIds)` — mọi tiêu chuẩn hoặc thuộc standard của lô.
   - Nếu lô **không có certification** → `standardIds` rỗng → chỉ khớp mốc `standard IS NULL`.
3. Lấy FarmLog của lô, lọc bỏ `corrected`.
4. Đếm số FarmLog theo `activity_type`.
5. So khớp **1:1**: mỗi mốc cần đúng 1 FarmLog có `activity_type` trùng; cùng activity_type không satisfy nhiều mốc.
6. Trả danh sách **tên mốc còn thiếu**.

Khi danh sách không rỗng, `recordPackagingEvent` ném `BusinessException`:

```
Lô chưa đủ mốc canh tác bắt buộc: Bón phân đợt 1, Phun thuốc phòng ngừa
```

- Lỗi được ghi vào `failed_event_logs` (`eventValidationService.logFailedAttempt`, ChainEventType.PACKAGING) rồi ném tiếp → client nhận 400.
- Lô giữ trạng thái cũ; chỉ khi đủ mốc mới chuyển sang `PACKAGED`.

> Chỉ một FarmLog không-corrected tương ứng mỗi mốc; FarmLog corrected bị loại (nếu sửa nhật ký thành corrected, mốc tương ứng báo thiếu trở lại).

---

## 6. Error Codes

Theo `GlobalExceptionHandler` (EXISTING — REUSE):

| HTTP | Ý nghĩa | Áp dụng |
|---|---|---|
| 400 | Validation / business | activityType sai, thiếu `isMandatory`, `@Min`, thiếu mốc đóng gói |
| 401 | Chưa xác thực | mọi endpoint |
| 403 | Không đủ quyền | không phải VT-01 |
| 404 | Không tìm thấy | id mốc/standard/category |
| 409 | Xung đột | trùng tên trong cùng (loại, tiêu chuẩn) |

---

## 7. Permission Matrix

| API | VT-01 | VT-02/03/04/05/06 |
|---|---|---|
| `GET /cultivation-milestones` | ✓ | ✓ (đăng nhập) |
| `GET /cultivation-milestones/{id}` | ✓ | ✓ |
| `POST /cultivation-milestones` | ✓ | ✗ (403) |
| `PUT /cultivation-milestones/{id}` | ✓ | ✗ |
| `recordPackagingEvent` (nội bộ) | — | chạy theo luồng VT-02 đóng gói |

Quản trị danh mục mốc: **chỉ VT-01** (QTN-17). `CultivationMilestoneServiceImpl` check lại `validateAdminPermission` (VT-01) trong service (belt-and-suspenders).

---

## 8. Seed & Demo Account

- Seed chuẩn: `V65` (schema) + `V66` (data) — bảng `cultivation_milestone` hợp nhất, 10 mốc (4 Cà phê + 4 Chè + 2 toàn bộ loại), mix bắt buộc/tùy chọn.
- Account test:
  - **VT-01** (Platform Admin): `admin` / `admin123` — để quản lý mốc.
  - **VT-02** (Quản lý HTX): `orgmanager` / `admin123` (org DEMO_HTX) — ghi nhật ký + đóng gói.
- Để test CRUD: login `admin`, mở menu trái **"Mốc canh tác"** → `/admin/cultivation-milestones` (thêm/sửa: khai báo loại nông sản + tiêu chuẩn + bắt buộc).
- Để test chặn đóng gói: `orgmanager` chọn lô thuộc category có mốc bắt buộc, ghi đủ/thiếu nhật ký tương ứng rồi đóng gói (validate liệt kê mốc thiếu).

---

## 9. Backend Implementation Scope (đã hoàn thành)

1. Migration schema `V65` + seed data `V66` (gộp 2 bảng → `cultivation_milestone`).
2. Entity + Repository: `CultivationMilestone` (nullable category/standard + `isMandatory`); `CultivationMilestoneRepository` (`search`, `existsByNameAndCategoryAndStandard(+AndIdNot)`, `findMandatoryMilestonesForValidation`, `findAllWithDetails`).
3. Service + Impl: `CultivationMilestoneService` (search/get/create/update — bỏ delete/disable/enable/assign), `MilestoneValidationService` (validate đóng gói — đọc từ 1 bảng).
4. Controller: `CultivationMilestoneController` (`/api/v1/cultivation-milestones`).
5. DTO: `CultivationMilestoneRequest/Response` (thêm `productCategoryId/standardId/isMandatory`, bỏ `status/referenced`).
6. MODIFY: `ChainEventServiceImpl.recordPackagingEvent` — validate mốc trước khi đặt `PACKAGED` (logic giữ nguyên, đổi nguồn truy vấn sang bảng hợp nhất).

## 10. Frontend Implementation Scope (đã hoàn thành)

1. API client: `frontend/src/api/cultivationMilestoneApi.ts` (getMilestones, getMilestone, create, update — **bỏ** disable/enable/delete/assign).
2. Types: `frontend/src/types/cultivationMilestone.ts` (CultivationMilestone + Request với `productCategoryId/standardId/isMandatory`).
3. Trang: `CultivationMilestoneManagementPage.tsx` (thêm cột Loại nông sản / Tiêu chuẩn / Bắt buộc; bỏ Trạng thái, nút ngừng sử dụng/xóa), `CreateCultivationMilestonePage.tsx`, `CultivationMilestoneFormContent.tsx` (thêm Select loại nông sản + tiêu chuẩn + Switch bắt buộc).
4. **Xóa** `AssignMilestonesPage.tsx`, route `/admin/product-categories/:id/milestones`, nút CalendarCheck gán mốc trong `ProductCategoryList`/`ProductCategoryManagementPage`.
5. Điều hướng: `/admin/cultivation-milestones` + `/admin/cultivation-milestones/create`; Sidebar menu "Mốc canh tác"; `ROLE_ACCESS.cultivationMilestoneManagement`.

## 11. Mapping Acceptance Criteria

| AC | API/Logic | Kết quả |
|---|---|---|
| TC-01 | Danh sách mốc theo loại + tiêu chuẩn + bắt buộc (§4.1) + validate đóng gói (§5) | Implement |
| TC-02 | `recordPackagingEvent` ném 400 liệt kê mốc thiếu (§5) | Implement |
| TC-03 | Validate chỉ chạy lúc đóng gói → không hồi tố lô đã PACKAGED | Implement |
| TC-04 | Trùng tên trong cùng (loại, tiêu chuẩn) → 409 | Implement |
| TC-05 | Không phải VT-01 → 403 + menu ẩn | Implement |
| TC-06 | `expectedDaysFromPlanting = 0` hợp lệ (ngày gieo trồng) | Implement |
| TC-07 | Bỏ disable/enable/delete — không còn endpoint | Implement |

## 12. Quyết định thiết kế

| # | Quyết định | Lý do |
|---|---|---|
| D-1 | PK = BIGINT AUTO_INCREMENT | Bảng khai báo nhẹ, mirror tham chiếu trước đây |
| D-2 | `product_category_id`/`standard_id` NULL = toàn bộ / mọi tiêu chuẩn | Theo mô tả nghiệp vụ (cho phép 1 loại hoặc toàn bộ) |
| D-3 | UNIQUE `(product_category_id, standard_id, name_key)` + chặn ở service cho trường hợp NULL | Chặn trùng tên trong cùng (loại, tiêu chuẩn), không nhạy hoa/thường |
| D-4 | Gộp 2 bảng → 1 bảng; bỏ bảng join | Theo quyết định user (đơn giản hóa) |
| D-5 | Thay "ngừng sử dụng" (status) = `is_mandatory` | Theo quyết định user |
| D-6 | Bỏ endpoint DELETE mốc | Theo quyết định user |
| D-7 | Matching 1:1 mốc ↔ FarmLog theo `activity_type`; validate tại `recordPackagingEvent` | QTN-35; không hồi tố |
| D-8 | Chặn cứng, liệt kê đích danh mốc thiếu | QTN-35 + TC-02 |
| D-9 | Chỉ VT-01 thêm/sửa | QTN-17 |

## 13. Test tự động

- `CultivationMilestoneServiceImplTest` (backend unit): trùng tên theo (loại, tiêu chuẩn), quyền VT-01, category/standard không tồn tại, activityType sai, create/update thành công, search truyền filter.
- `MilestoneValidationServiceImplTest`: đủ log → rỗng; thiếu log → liệt kê tên; không certification → chỉ mốc mọi-tiêu-chuẩn; 1:1; corrected bị loại; activityType sai → missing.
- **Xóa** `CategoryMilestoneAssignmentServiceImplTest` (tính năng gán theo category đã bỏ).
