# NCL-739 — Phân tích nghiệp vụ: Cấu trúc địa bàn quản lý cho cán bộ quản lý ngành

> Story cha: **NCL-670** — Gán địa bàn quản lý cho tài khoản cán bộ quản lý ngành (VT-05).
> Tài liệu này là đầu vào bắt buộc cho NCL-740 (data layer), NCL-742 (UI) và NCL-743 (API + lọc báo cáo).

## 1. Mục tiêu

Cán bộ quản lý ngành (VT-05) hiện xem báo cáo tổng hợp đa tổ chức nhưng phải tự nhập
địa bàn dạng chuỗi tự do, không có kiểm soát dữ liệu nào giới hạn phạm vi được phép xem.
Story này bổ sung:

1. Danh mục đơn vị hành chính dùng chung (tỉnh → xã/phường).
2. Quan hệ nhiều-nhiều giữa tài khoản và địa bàn phụ trách.
3. Bộ lọc địa bàn áp vào mọi báo cáo/tổng hợp của VT-05 trước khi trả kết quả.

## 2. Kết quả khảo sát codebase hiện tại

| Hạng mục | Hiện trạng | Vị trí |
|---|---|---|
| Role VT-05 | `REGULATOR` — "Cán bộ quản lý ngành" | `db/migration/data/V14__seed_roles.sql`, `organization/constant/RoleCode.java` |
| Tài khoản | Bảng `users`, UUID CHAR(36), không gắn role trực tiếp; role nằm trên `organization_users` (mỗi membership 1 role) | `auth/entity/User.java`, `organization/entity/OrganizationUser.java` |
| Danh mục hành chính | **Chưa tồn tại** bất kỳ bảng tỉnh/huyện/xã nào | Toàn bộ migration V1–V34 |
| "Địa bàn" hiện nay | Chuỗi free-text, lọc bằng `LIKE` vào `organizations.address` | `report/service/impl/ReportServiceImpl.java:170` (`findByAddressContainingIgnoreCase`) |
| Endpoint VT-05 dùng | `/reports/industry-summary(+/export)`, `/reports/crop-area-analysis`, `/reports/crop-area-analysis/season-yield-comparison`, `/export/open-data` | package `report` |
| Vùng trồng | `farm_areas`: chỉ có toạ độ POINT, chưa liên kết hành chính | `farm/entity/FarmArea.java`, `V3__create_farm_areas.sql` |
| Audit log | `activity_logs` + `@Auditable` + `ActivityLogEvent` (async listener) | `V10__create_logging_tables.sql`, `common/aspect/AuditAspect.java` |
| Migration kế tiếp | **V35** (hiện cao nhất là V34) | `db/migration/{schema,data}/` |
| Pattern join-table mẫu | PK CHAR(36) + UNIQUE ở tầng SQL + join entity tường minh (không dùng `@ManyToMany`) | `organization_users` (V1), `role_permissions` (V13) |

Kết luận: danh mục hành chính phải tạo mới, nhưng chỉ tạo **một danh mục duy nhất dùng chung**
(tuân thủ rule #2 của story — không tạo danh mục địa bàn riêng song song).

## 3. Quyết định thiết kế

### 3.1. Mô hình cấp đơn vị: cây linh hoạt nhiều cấp (self-referencing)

Bảng `administrative_units` tự tham chiếu qua `parent_id` thay vì 3 bảng cứng
provinces/districts/wards.

Lý do:
- Việt Nam từ 01/07/2025 chuyển sang mô hình 2 cấp (34 tỉnh/thành → xã/phường, bỏ cấp huyện).
  Cây linh hoạt phản ánh đúng cấu trúc hiện hành mà vẫn chạy được dữ liệu 3 cấp cũ nếu cần nhập liệu lịch sử.
- Khi hành chính thay đổi lần nữa (sáp nhập/tách), chỉ thêm/sửa dòng dữ liệu — **không phải đổi schema, không đụng code**.
- Lọc báo cáo cần "toàn bộ đơn vị dưới một tỉnh": denormalize thêm `province_id` (trỏ về gốc cấp tỉnh)
  để truy vấn O(1) thay vì duyệt cây.

Các mức hỗ trợ: `PROVINCE` (cấp 1) và `COMMUNE` (cấp 2). Enum `level` lưu VARCHAR
(`@Enumerated(EnumType.STRING)`), cho phép mở rộng `DISTRICT` sau này bằng cách thêm giá trị mới.

> ⚠️ Điểm lệch thuật ngữ so với đề bài: test case gốc ghi "gán 2 huyện". Sau sáp nhập 2025
> không còn cấp huyện — các test thực hiện với 2 đơn vị cấp xã (hoặc 2 tỉnh). Đây là khác biệt
> thuật ngữ, không phải khác biệt nghiệp vụ.

### 3.2. Quan hệ nhiều-nhiều tài khoản ↔ địa bàn

Bảng liên kết `user_area_assignments` (một cán bộ phụ trách NHIỀU địa bàn, một địa bàn
có thể có NHIỀU cán bộ):

```
users (user_id CHAR(36))  ──< user_area_assignments >──  administrative_units (id CHAR(36))
```

Cột: `id` (PK CHAR(36)), `user_id`, `unit_id`, `assigned_at` (DATETIME(6)), `assigned_by`
(CHAR(36), người thao tác). Ràng buộc: **UNIQUE (user_id, unit_id)** chặn gán trùng ngay ở tầng
dữ liệu (chống race-condition), FK hai chiều, index theo từng FK.

Chỉ gán được cho tài khoản **đang có membership role VT-05** (kiểm tra qua `organization_users`
join `roles.code = 'VT-05'`, membership `status = ACTIVE`). Một user thuộc nhiều tổ chức:
chỉ cần tồn tại ít nhất 1 membership VT-05 active là đủ điều kiện được gán.

### 3.3. Xác định "tổ chức thuộc địa bàn nào"

Thêm 2 cột nullable vào `organizations`: `province_id`, `commune_id` (FK → administrative_units).

Lý do chọn map ở mức tổ chức (không phải mức vùng trồng):
- Mọi báo cáo của VT-05 đều tổng hợp theo Organization (industry-summary, open-data,
  crop-area-analysis group theo tổ chức/vùng trồng của tổ chức) → lọc qua tổ chức là điểm chặn
  duy nhất, phủ toàn bộ báo cáo mà không phải viết lại query tổng hợp sâu.
- Backfill: migration data khớp tên tỉnh trong chuỗi `address` hiện có; không khớp chắc chắn →
  để NULL, VT-01 sửa tay qua endpoint cập nhật mapping tổ chức.
- Vùng trồng giữ nguyên toạ độ POINT; khi cần chi tiết hơn sau này chỉ cần thêm cột tham chiếu
  vào `farm_areas` — danh mục dùng chung đã sẵn sàng (rule #2).

### 3.4. Quy tắc validate khi gán/gỡ (thứ tự thực thi bắt buộc)

| # | Quy tắc | Kết quả lỗi |
|---|---|---|
| V1 | Người thao tác phải là VT-01 (`@PreAuthorize hasRole('VT-01')` + kiểm tra lại trong service) | 403 |
| V2 | Tài khoản bị gán phải tồn tại | 404 |
| V3 | Tài khoản bị gán phải có membership VT-05 ACTIVE ("Tài khoản không có vai trò Cán bộ quản lý ngành.") | 400 |
| V4 | Unit phải tồn tại và `active = true` ("Địa bàn không nằm trong danh mục hành chính.") | 400 |
| V5 | Chưa gán trùng (service check + UNIQUE DB backstop) ("Địa bàn đã được gán cho tài khoản này.") | 400/409 |
| V6 | Gỡ: bản ghi phải tồn tại ("Tài khoản chưa được gán địa bàn này.") | 404 |

Gán hàng loạt (POST nhiều unitId): validate toàn bộ trước khi lưu (all-or-nothing),
mỗi unit thành công sinh 1 dòng audit.

### 3.5. Áp bộ lọc địa bàn vào báo cáo (rule bảo mật số 1)

Áp dụng trong service layer, TRƯỚC khi tổng hợp:

1. Lấy tập đơn vị đã gán của user hiện tại (theo `userId`).
2. **Tập rỗng → trả về thành công (HTTP 200) với dữ liệu rỗng + thông báo rõ ràng**
   ("Bạn chưa được phân công địa bàn quản lý nào."). TUYỆT ĐỐI KHÔNG fallback sang
   tra cứu toàn bộ dữ liệu — lỗi bảo mật nghiêm trọng, chặn release nếu vi phạm.
3. Có gán → tính tập `provinceIds`/`communeIds` được phép, giao với filter yêu cầu,
   rồi mới tổng hợp theo các tổ chức có mapping thuộc tập đó.
4. VT-01 không bị ràng buộc (xem toàn hệ thống); VT-05 có thể thu hẹp thêm bằng filter
   nhưng không bao giờ mở rộng vượt tập đã gán.

Phạm vi endpoint phải sửa: `getIndustrySummary` (+ export), `getCropAreaAnalysis`,
`compareSeasonYield`, `exportOpenData`.

### 3.6. Audit (rule #6)

Mỗi thao tác gán/gỡ ghi `activity_logs` đầy đủ: người thao tác (user_id/username/full_name/ip),
tài khoản bị ảnh hưởng, đơn vị hành chính thay đổi (code + tên), thời điểm. Dùng
`@Auditable(action="ASSIGN_AREA"/"UNASSIGN_AREA", entityType="UserAreaAssignment")` —
lưu ý `entityType` ≤ 50 ký tự.

### 3.7. Seed dữ liệu danh mục

- Migration data nạp **toàn bộ quốc gia**: 34 tỉnh/thành + toàn bộ đơn vị cấp xã theo
  mã hành chính mới (Nghị định 2025), tổng ~3.300+ dòng. Nguồn: dataset công khai mã
  hành chính 2025; script sinh SQL giữ lại trong `backend/scripts/` để tái tạo.
- Test (H2, Flyway off, ddl-auto create-drop) **không phụ thuộc seed** — test tự tạo fixture
  riêng, tránh khoá cứng vào dataset quốc gia.

## 4. Sơ đồ dữ liệu đề xuất

```
administrative_units                       user_area_assignments              users
- id            CHAR(36) PK          ────┐ - id           CHAR(36) PK         │
- code          VARCHAR(20) UK NOT NULL  │ - user_id      CHAR(36) FK ────────┘
- name          VARCHAR(255) NOT NULL    │ - unit_id      CHAR(36) FK ───┐
- level         VARCHAR(20) NOT NULL     │ - assigned_at  DATETIME(6)    │
- parent_id     CHAR(36) NULL (self FK) ─┘ - assigned_by  CHAR(36) FK→users
- province_id   CHAR(36) NULL (root)                                    │
- active        BOOLEAN DEFAULT TRUE        organizations               │
                                            - province_id CHAR(36) NULL ─┘ (FK units)
                                            - commune_id  CHAR(36) NULL (FK units)
```

## 5. Phạm vi

**Bao gồm:** danh mục hành chính + seed quốc gia; bảng gán tài khoản↔địa bàn; API gán/gỡ/xem;
API cây danh mục; lọc địa bàn cho 4 endpoint báo cáo của VT-05; audit; UI màn hình gán
(VT-01) + bộ lọc tái sử dụng cho báo cáo; test tự động backend + component test frontend.

**Không bao gồm:** chỉnh sửa danh mục hành chính CRUD tay (chỉ seed/import); gán địa bàn
cho role khác VT-05; phân quyền tinh theo permission table; map địa bàn xuống mức vùng trồng
(farm_areas); E2E Playwright tự động.

## 6. Điều kiện hoàn thành (liên kết test)

| Mã | Kịch bản bắt buộc pass |
|---|---|
| TC-01 | Gán 2 đơn vị cho 1 tài khoản VT-05 → báo cáo chỉ chứa dữ liệu 2 đơn vị đó |
| TC-02 | Tài khoản chưa gán → rỗng kèm thông báo, không lộ dữ liệu (**release blocker**) |
| TC-03 | Vai trò khác (VD VT-02) gọi API/mở màn hình gán → từ chối |
| TC-04 | Gán rồi gỡ → activity_logs ghi đủ người/thời điểm/tài khoản/đơn vị |
| TC-05–08 | Trùng / ngoài danh mục / sai vai trò / race-condition 2 request song song → chặn sạch |
| TC-09–13 | UI: gán OK, cán bộ thấy đúng phạm vi, empty-state thân thiện, URL bị chặn, gỡ cập nhật ngay |
