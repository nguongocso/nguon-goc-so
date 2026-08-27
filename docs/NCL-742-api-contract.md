# NCL-742 — API contract: Phân công địa bàn quản lý (UI + mock)

> Story cha: **NCL-670** — Gán địa bàn quản lý cho tài khoản cán bộ quản lý ngành (VT-05).
> Tài liệu này là **nguồn sự thật hợp đồng API** cho giai đoạn 3 (backend NCL-743).
> Frontend giai đoạn 1 (NCL-742) chạy trên mock theo đúng contract dưới đây.

## 0. Quy ước chung

- Mọi response bọc trong `ApiResult<T>` (xem `src/types/member.ts`):

```jsonc
{
  "success": true,
  "status": 200,
  "message": "...",      // tuỳ chọn
  "data": T,
  "errors": null,
  "path": "/api/v1/...",
  "timestamp": "2026-08-26T00:00:00Z"
}
```

- Lỗi nghiệp vụ backend ném `BusinessException(HttpStatus, message)` → `GlobalExceptionHandler`
  map về `ApiResult` với `success=false`, `status` tương ứng, `message` **đúng chuỗi tiếng Việt**
  ghi ở từng endpoint (frontend test khớp chính xác chuỗi này).
- Đơn vị hành chính theo mô hình 2 cấp sau sáp nhập 2025: `PROVINCE` (tỉnh/thành) → `COMMUNE`
  (xã/phường). KHÔNG có cấp huyện.

## 1. Cây đơn vị hành chính

### `GET /api/v1/administrative-units/tree`

Public cho user đã đăng nhập (dùng chung cho màn hình gán + bộ lọc báo cáo).

**Response** `data: AdministrativeUnitNode[]`

```ts
type AdministrativeUnitLevel = 'PROVINCE' | 'COMMUNE';

interface AdministrativeUnitNode {
  id: string;        // UUID CHAR(36)
  code: string;      // mã hành chính mới 2025
  name: string;
  level: AdministrativeUnitLevel;
  children: AdministrativeUnitNode[]; // xã/phường lồng trong tỉnh; [] nếu không có
}
```

Chỉ có 1 mức lồng (tỉnh chứa xã). `children: []` khi không có con.

## 2. Danh sách cán bộ VT-05 để gán

### `GET /api/v1/admin/users?role=VT-05&keyword=<string>&page=<n>&size=<n>`

Quyền: **VT-01** (không phải VT-01 → HTTP 403, message `"Bạn không có quyền thực hiện thao tác này."`).

**Response** `data: PageResponse<UserOption>` (`PageResponse` xem `src/types/common.ts`):

```ts
interface UserOption {
  userId: string;
  username: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  organizationName: string;
}
```

- `keyword`: so khớp không phân biệt hoa thường trên `fullName`/`username`.
- Chỉ trả user có ít nhất 1 membership role `VT-05` trạng thái `ACTIVE`.

## 3. Xem địa bàn đã gán của một user

### `GET /api/v1/admin/users/{userId}/areas`

Quyền: **VT-01**. `userId` không tồn tại → HTTP 404 `"Tài khoản không tồn tại."`.

**Response** `data: AssignedArea[]`

```ts
interface AssignedArea {
  assignmentId: string;   // UUID bản ghi user_area_assignments
  unitId: string;
  unitCode: string;
  unitName: string;
  unitLevel: 'PROVINCE' | 'COMMUNE';
  provinceId: string;     // đơn vị gốc cấp tỉnh tương ứng
  provinceName: string;
  assignedAt: string;     // ISO datetime
}
```

## 4. Gán địa bàn (batch, all-or-nothing)

### `POST /api/v1/admin/users/{userId}/areas`

Body:

```json
{ "unitIds": ["<uuid>", "<uuid>"] }
```

**Success** `data`:

```ts
interface AssignAreasResult {
  assignedCount: number;
  assigned: AssignedArea[];
  message?: string;
}
```

Validate đúng thứ tự (theo NCL-739 §3.4):

| # | Điều kiện | HTTP | Message |
|---|---|---|---|
| V1 | Người thao tác phải là VT-01 | 403 | `Bạn không có quyền thực hiện thao tác này.` |
| V2 | Tài khoản bị gán tồn tại | 404 | `Tài khoản không tồn tại.` |
| V3 | Có membership VT-05 ACTIVE | 400 | `Tài khoản không có vai trò Cán bộ quản lý ngành.` |
| V4 | Mọi unitId tồn tại và active | 400 | `Địa bàn không nằm trong danh mục hành chính.` |
| V5 | Chưa gán trùng (service + UNIQUE DB) | 400 | `Địa bàn đã được gán cho tài khoản này.` |

Gán hàng loạt validate toàn bộ trước khi lưu (all-or-nothing); mỗi unit thành công sinh 1 dòng audit.

## 5. Gỡ một địa bàn

### `DELETE /api/v1/admin/users/{userId}/areas/{unitId}`

Quyền: **VT-01**.

- Success: `data.message = "Đã gỡ địa bàn <unitName> khỏi tài khoản."` (frontend dùng chuỗi này
  cho `toast.success`).
- Bản ghi không tồn tại → HTTP 404 `"Tài khoản chưa được gán địa bàn này."`.

## 6. Cán bộ tự xem địa bàn của mình (VT-05)

### `GET /api/v1/me/areas`

User hiện tại (VT-05). **Response** `data: AssignedArea[]` (cùng shape mục 3).

## 7. Map tổ chức vào địa bàn (phục vụ backfill/lọc báo cáo)

### `PUT /api/v1/admin/organizations/{organizationId}/divisions`

Quyền: **VT-01**. Body:

```json
{ "provinceId": "<uuid|null>", "communeId": "<uuid|null>" }
```

Cập nhật `organizations.province_id` / `organizations.commune_id` (nullable).

## 8. Mở rộng tham số báo cáo — bắt buộc lọc theo địa bàn của VT-05

Thêm query param **lặp được** `unitIds` (UUID, có thể nhiều giá trị) vào:

- `GET /reports/industry-summary`
- `GET /reports/crop-area-analysis`
- `GET /reports/crop-area-analysis/season-yield-comparison`
- `GET /reports/industry-summary/export`
- `GET /export/open-data`

Quy tắc lọc (rule bảo mật số 1 của NCL-670):

1. Caller **VT-05**: server LUÔN giao kết quả với tập địa bàn đã gán cho user
   (param `unitIds` chỉ có thể thu hẹp, không bao giờ mở rộng tập đã gán).
2. VT-05 **CHƯA được gán địa bàn nào** → HTTP **200**, dữ liệu rỗng kèm message
   `"Bạn chưa được phân công địa bàn quản lý nào."`
   (`industry-summary` trả thêm `hasData=false`). **KHÔNG BAO GIỜ** fallback trả toàn bộ dữ liệu.
3. Caller **VT-01**: không bị ràng buộc bởi tập đã gán (xem toàn hệ thống); khi truyền
   `unitIds` thì lọc theo param như bình thường.

## 9. Ma trận quyền tổng hợp

| Endpoint | VT-01 | VT-05 | Khác |
|---|---|---|---|
| GET administrative-units/tree | ✅ | ✅ | ✅ (đã đăng nhập) |
| GET admin/users?role=VT-05 | ✅ | ❌ 403 | ❌ 403 |
| GET admin/users/{id}/areas | ✅ | ❌ 403 | ❌ 403 |
| POST admin/users/{id}/areas | ✅ | ❌ 403 | ❌ 403 |
| DELETE admin/users/{id}/areas/{unitId} | ✅ | ❌ 403 | ❌ 403 |
| GET me/areas | ✅ (rỗng/—) | ✅ | ❌ |
| PUT admin/organizations/{id}/divisions | ✅ | ❌ 403 | ❌ 403 |

## 10. Ghi chú cho giai đoạn 3 (swap mock → thật)

- Chữ ký hàm frontend đã giữ nguyên so với mock (`src/api/administrativeUnitApi.ts`,
  `src/api/areaAssignmentApi.ts`) — chỉ cần thay thân hàm bằng `apiClient.get/post/delete(...)`
  và unwrap `response.data.data`.
- Frontend đọc lỗi từ `err.response?.data?.message`; backend phải trả message tiếng Việt
  đúng chuỗi ở các bảng trên để test luồng lỗi vẫn pass.
- Mock store hiện tại: `src/mocks/areaAssignments.ts` (reset bằng `resetMockAreaAssignments()`).
