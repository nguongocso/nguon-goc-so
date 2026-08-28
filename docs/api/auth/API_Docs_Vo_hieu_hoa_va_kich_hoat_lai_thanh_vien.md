# NCL-01-CN-009 — API Design: Vô hiệu hóa thành viên rời tổ chức và thu hồi quyền

> Loại tài liệu: Phân tích nghiệp vụ + Thiết kế API (KHÔNG kèm implement).
> Người dùng: Backend Agent, Frontend Agent.
> Nhãn trạng thái: `EXISTING` (đã có) · `REUSE` (dùng lại) · `MODIFY` (cần mở rộng) · `NEW` (cần tạo) · `PROPOSED` (đề xuất tên mới) · `UNKNOWN` (chưa đủ bằng chứng).

---

## 1. Tổng quan

Story NCL-01-CN-009 (Epic NCL-01) yêu cầu:

1. Quản lý hợp tác xã (VT-02) **vô hiệu hóa** membership của thành viên đã nghỉ trong tổ chức hiện tại.
2. Thành viên **mất toàn bộ quyền ngay lập tức**; các phiên đăng nhập đang mở phải bị chấm dứt.
3. Thành viên chuyển sang trạng thái **NGỪNG HOẠT ĐỘNG**.
4. Dữ liệu đã ghi trước đó **không được xóa**; tên người ghi trong dữ liệu lịch sử **giữ nguyên**.
5. Nếu thành viên còn được **phân công vào các lô chưa hoàn thành**: không vô hiệu hóa ngay — API trả về danh sách lô đang phân công và yêu cầu chọn **người thay thế hợp lệ** trước.
6. Quản lý có thể **kích hoạt lại** thành viên sau này (bắt buộc nhập lý do).
7. Mọi thao tác ghi vào **lịch sử hoạt động (audit log)** theo cơ chế hiện tại của hệ thống.
8. Người ghi sự kiện (VT-03) **không được phép** thực hiện thao tác này.

Hiện trạng repository: đã có bảng `organization_users` với cột `status` (`ACTIVE`/`INACTIVE`), đã có cơ chế audit `ActivityLogEvent`, đã có JWT stateless ACCESS token. **Chưa có**: API vô hiệu hóa/kích hoạt lại membership, cơ chế thu hồi token tức thời, và entity/bảng **phân công lô sản xuất** (lot assignment).

---

## 2. Kết quả phân tích repository

### 2.1 Entity/domain liên quan

| Thành phần | File | Vai trò | Trạng thái |
|---|---|---|---|
| `OrganizationUser` | `backend/src/main/java/vn/nguongocso/organization/entity/OrganizationUser.java` | Membership tổ chức ↔ user ↔ vai trò; có `status` (`ACTIVE`/`INACTIVE`) — đây là trạng thái cần đổi khi vô hiệu hóa | EXISTING |
| `OrganizationUserStatus` | `backend/src/main/java/vn/nguongocso/organization/enums/OrganizationUserStatus.java` | `ACTIVE`, `INACTIVE` | EXISTING |
| `User` | `backend/src/main/java/vn/nguongocso/auth/entity/User.java` | Tài khoản toàn cục; 1 user có thể thuộc **nhiều tổ chức** → khi vô hiệu hóa membership **KHÔNG được** đổi `User.status` | EXISTING |
| `Role` / `RoleCode` | `.../auth/entity/Role.java`, `.../organization/constant/RoleCode.java` | `VT-01` ADMIN, `VT-02` ORG_MANAGER, `VT-03` EVENT_RECORDER, `VT-04` PROCUREMENT, `VT-05` REGULATOR, `VT-06` CONSUMER | EXISTING |
| `ActivityLog` + `ActivityLogEvent` + `ActivityLogListener` | `.../alert/entity/ActivityLog.java`, `.../alert/event/ActivityLogEvent.java` | Audit log qua event publisher → bảng `activity_logs` | EXISTING |
| `ProductionLot` | `.../farm/entity/ProductionLot.java` | Lô sản xuất; `status` (`DRAFT`→`PENDING`→`APPROVED`→`HARVESTED`→`PREPROCESSED`→`PACKAGED`→`CLOSED`, và `REJECTED`/`RECALLED`); chỉ có `createdBy`/`approvedBy` | EXISTING |
| `ChainEvent` | `.../event/entity/ChainEvent.java` | Sự kiện ghi vào chuỗi; FK `recorded_by` → `users` (không cascade delete) | EXISTING |
| Phân công lô (lot assignment) | — | Không tồn tại bảng/entity nào gán thành viên vào lô | NOT_FOUND → NEW (xem §11, điểm D-3) |

### 2.2 Service/controller/repository có thể reuse

| Thành phần | File | Khả năng reuse |
|---|---|---|
| `OrganizationMemberController` | `.../organization/controller/OrganizationMemberController.java` | REUSE — base path `/api/v1/organization/members`, đã có `@PreAuthorize("hasAnyRole('VT-01','VT-02')")` |
| `OrganizationMemberService` | `.../organization/service/OrganizationMemberService.java` | MODIFY — thêm `deactivate`/`reactivate`; reuse helper `getCurrentOrganizationId()`, `publishActivityLog()`, `toResponse()` |
| `OrganizationUserRepository` | `.../organization/repository/OrganizationUserRepository.java` | REUSE — đã có `findByOrganization_OrganizationIdAndUser_UserId`, `findByOrganization_OrganizationIdAndStatus`, `findByUser_UserIdAndStatus`, `findAllByOrganization_OrganizationIdAndRole_Code` |
| `OrganizationUserResponse` | `.../auth/dto/response/OrganizationUserResponse.java` | MODIFY — cần thêm `membershipStatus` (trường `status` hiện đang map `users.status` toàn cục) |
| Cơ chế audit | `OrganizationMemberService.publishActivityLog()` → `ActivityLogEvent` | REUSE — ghi audit cho deactivate/reactivate |
| `BusinessException` / `ResourceNotFoundException` / `GlobalExceptionHandler` | `.../exception/` | REUSE — convention lỗi, KHÔNG tạo error code mới |
| `ApiResult` | `.../common/ApiResult.java` | REUSE — envelope response `{success, status, message, data, errors, path, timestamp}` |
| `AccountLockService.invalidateAllTokens()` | `.../auth/service/impl/AccountLockServiceImpl.java` | MODIFY — hiện là **placeholder TODO**, cần cài đặt thật để chấm dứt phiên (§11, D-1) |
| API khóa tài khoản `PATCH /api/v1/auth/monitoring/accounts/{id}/lock` | `.../auth/controller/LoginMonitoringController.java` | KHÔNG reuse cho story này — khác nghiệp vụ (khóa do bất thường đăng nhập, tác động toàn cục `users.status`) |

### 2.3 Schema DB hiện có (bằng chứng từ Flyway migration + entity)

- `organization_users`: `id CHAR(36) PK, organization_id FK, user_id FK, role_id FK, custom_permissions, joined_at, status ('ACTIVE'|'INACTIVE')` — trạng thái membership là **cột sẵn có**, không cần migration mới cho luồng chính.
- `users`: `user_id, user_name UK, password_hash, full_name, phone, email, status, created_at, updated_at`.
- `production_lot`: có `status`, `created_by`, `approved_by` — **không có cột/bảng phân công thành viên vào lô**.
- `activity_logs`: `id, organization_id, user_id, username, full_name, action, description, entity_type, entity_id, ip_address, created_at`.
- `chain_events`: `recorded_by` FK → `users.user_id` (không cascade) — cơ sở của rule "bảo toàn dữ liệu đã ghi".

### 2.4 Nhận xét quan trọng (bằng chứng code)

1. **JWT stateless, không có blacklist**: ACCESS token có hạn 1 giờ. `JwtAuthenticationFilter` reload membership theo `userId + organizationId` ở **mỗi request** qua `CustomUserDetailsService.loadUserByUserIdAndOrganizationId()` — nhưng hiện **chưa kiểm tra** `OrganizationUser.status` hay `User.status`. Hệ quả: nếu chỉ set `OrganizationUser.status = INACTIVE`, ACCESS token cũ **vẫn còn authenticate được** cho đến khi hết hạn → vi phạm yêu cầu "mất quyền ngay lập tức". Backend bắt buộc phải bổ sung kiểm tra trạng thái trong luồng này và/hoặc cài đặt thật `invalidateAllTokens()` (§11, D-1).
2. **Đường đăng nhập đã có hàng rào một phần**: `my-organizations` lọc chỉ membership `ACTIVE`; `switchOrganization` chặn INACTIVE với message `"Tổ chức không còn hoạt động với tài khoản này"`. Riêng `selectOrganization` chỉ kiểm tra `status == null` → cần siết thêm (§11, D-2).
3. `assignRole` đã chặn gán quyền cho member INACTIVE: `"Thành viên đã bị vô hiệu hóa. Vui lòng kích hoạt lại trước khi cấp quyền"` → tái sử dụng đúng hành vi này, không đổi.
4. `GET /api/v1/organization/members` hiện trả về **chỉ** membership `ACTIVE` → UI không tìm được thành viên đã nghỉ để kích hoạt lại → cần MODIFY thêm filter `status`.
5. **Multi-tenant**: mọi lookup membership đều scope theo `organizationId` lấy từ JWT (`CustomUserDetails`), không nhận từ request → truyền ID tổ chức khác vào request không có tác dụng (QTN-01).
6. **Không có optimistic locking** (`@Version`) trên `OrganizationUser` → concurrent update chưa được xử lý (§11, D-6).
---

## 3. Ánh xạ quy tắc nghiệp vụ QTN-32

| # | Yêu cầu QTN-32 | Thiết kế đáp ứng |
|---|---|---|
| 1 | Mất toàn bộ quyền trong tổ chức ngay lập tức | Đổi `organization_users.status = 'INACTIVE'` + backend kiểm tra trạng thái khi xác thực từng request + thu hồi token (D-1) |
| 2 | Phiên đăng nhập đang mở bị chấm dứt | Như trên; client không cần chủ động logout |
| 3 | Trạng thái NGỪNG HOẠT ĐỘNG | `OrganizationUserStatus.INACTIVE` (không đổi `users.status` toàn cục) |
| 4 | Không ghi thêm sự kiện/thao tác yêu cầu quyền | API nghiệp vụ yêu cầu membership `ACTIVE` qua luồng xác thực (D-1); FE ẩn form nhưng **backend là nơi enforce** |
| 5 | Dữ liệu đã ghi không bị xóa | Không thực hiện DELETE/cascade; FK `recorded_by`, `created_by` giữ nguyên |
| 6 | Tên người ghi cũ hiển thị đúng | Không đổi `users.full_name`, không đổi username; hiển thị lịch sử qua join hiện có |
| 7 | Còn phân công lô chưa hoàn thành → chặn + yêu cầu thay thế | `deactivate` trả **409** kèm `errors.pendingLots`; chỉ thực hiện khi có `replacementUserId` hợp lệ |
| 8 | Người ghi sự kiện (VT-03) không được thao tác | `@PreAuthorize("hasAnyRole('VT-01','VT-02')")` → **403** |
| 9 | Kích hoạt lại phải nhập lý do | `reactivate` với `reason` bắt buộc (`@NotBlank`, ≤500 ký tự) |
| 10 | Ghi audit log | REUSE cơ chế `ActivityLogEvent` (§8) |

---

## 4. Danh sách API

| # | Method | Endpoint | Mục đích | Trạng thái |
|---|---|---|---|---|
| 1 | GET | `/api/v1/organization/members` | Danh sách thành viên; thêm filter `status` phục vụ UI vô hiệu hóa/kích hoạt lại | MODIFY |
| 2 | GET | `/api/v1/organization/members/{userId}/unfinished-lots` | Precheck: lô chưa hoàn thành đang gắn với thành viên | NEW |
| 3 | GET | `/api/v1/organization/members/{userId}/replacement-candidates` | Danh sách thành viên đủ điều kiện thay thế | NEW |
| 4 | PATCH | `/api/v1/organization/members/{userId}/deactivate` | Vô hiệu hóa thành viên (+ người thay thế nếu bắt buộc) | NEW |
| 5 | PATCH | `/api/v1/organization/members/{userId}/reactivate` | Kích hoạt lại thành viên (bắt buộc lý do) | NEW |
| 6 | GET | `/api/v1/roles` | Danh mục vai trò cho màn hình quản lý thành viên | REUSE (EXISTING) |
| 7 | GET | `/api/v1/organizations/activity-logs` | Xem lại lịch sử vô hiệu hóa/kích hoạt lại | REUSE (EXISTING) |
| 8 | PUT | `/api/v1/organization/members/roles` | Gán vai trò — đã tự chặn member INACTIVE | REUSE (EXISTING) |

**Lý do chọn PATCH + sub-path** (`deactivate`/`reactivate`): khớp convention trạng thái hiện có của hệ thống — `PATCH /api/v1/auth/monitoring/accounts/{accountId}/lock` và `/unlock`. Không tạo `PUT /status` chung vì hai chiều chuyển trạng thái có validation và side-effect khác nhau.

**Không tạo endpoint riêng** cho "gỡ/cập nhật phân công lô": việc chuyển giao lô được thực hiện trong chính API #4 qua `replacementUserId` (§5.4, BR-6). Nếu sau này hệ thống có module phân công lô độc lập (D-3), bổ sung tài liệu riêng khi đó.

---

## 5. Đặc tả chi tiết từng API

> Common: mọi API yêu cầu header `Authorization: Bearer <ACCESS JWT>` (token nhận sau `POST /api/v1/auth/select-organization`). Response luôn bọc trong `ApiResult`. `organizationId` của người thao tác luôn lấy từ JWT, không nhận từ request (QTN-01).

### 5.1 Danh sách thành viên — MODIFY

- **Method**: `GET`
- **Endpoint**: `/api/v1/organization/members`
- **Mục đích**: Lấy danh sách thành viên của tổ chức hiện tại; phục vụ màn hình vô hiệu hóa (lọc `ACTIVE`) và kích hoạt lại (lọc `INACTIVE`).
- **Authentication**: Bắt buộc (ACCESS JWT).
- **Authorization**: `@PreAuthorize("hasAnyRole('VT-01','VT-02')")` — các vai trò khác nhận 403.
- **Query parameters**:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `status` | string | Không | `ACTIVE` | `ACTIVE` \| `INACTIVE` \| bỏ trống = tất cả. *(PROPOSED — hiện code luôn trả về membership ACTIVE)* |

- **Response 200**: `ApiResult<List<OrganizationUserResponse>>` — không phân trang (giữ nguyên hành vi hiện tại; số thành viên của một HTX nhỏ).

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "a1b2c3d4-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "organizationId": "05c43400-8ae5-44c2-ad71-70b80dc98410",
      "userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
      "username": "nong_dan_01",
      "fullName": "Nguyễn Văn Bình",
      "email": "binh@example.com",
      "phone": "0912345678",
      "roleId": 3,
      "roleCode": "VT-03",
      "roleName": "Người ghi sự kiện",
      "status": "ACTIVE",
      "membershipStatus": "ACTIVE",
      "joinedAt": "2026-06-01T08:00:00"
    }
  ],
  "timestamp": "2026-08-27T07:00:00Z"
}
```

| Trường | Kiểu | Mô tả | Trạng thái |
|---|---|---|---|
| `id` | UUID | ID bản ghi `organization_users` | EXISTING |
| `userId` | UUID | ID tài khoản — dùng làm path param cho các API #2–#5 | EXISTING |
| `roleCode`, `roleName` | string | Vai trò hiện tại trong tổ chức | EXISTING |
| `status` | string | `users.status` toàn cục (ACTIVE/INACTIVE) — **giữ nguyên để không phá hợp đồng hiện có** | EXISTING |
| `membershipStatus` | string | `organization_users.status` (`ACTIVE`/`INACTIVE`) — trạng thái đúng của membership | MODIFY (PROPOSED thêm trường) |
| `joinedAt` | datetime | Thời gian gia nhập tổ chức | EXISTING |

- **Error cases**: 401, 403.
- **Ghi chú_backend**: `toResponse()` hiện map `status` từ `users.status` — cần thêm `membershipStatus` từ `orgUser.getStatus()`; không xóa trường cũ (D-8).

### 5.2 Lô chưa hoàn thành đang phân công — NEW

- **Method**: `GET`
- **Endpoint**: `/api/v1/organization/members/{userId}/unfinished-lots`
- **Mục đích**: Precheck trước khi vô hiệu hóa — cho UI biết thành viên còn lô nào chưa hoàn thành để yêu cầu chọn người thay thế (TC-02). API `deactivate` (§5.4) cũng tự kiểm tra; endpoint này dùng để hiển thị cảnh báo sớm.
- **Authentication**: Bắt buộc (ACCESS JWT).
- **Authorization**: `@PreAuthorize("hasAnyRole('VT-01','VT-02')")`; membership phải thuộc tổ chức hiện tại.
- **Path parameters**:

| Param | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `userId` | UUID | Có | ID tài khoản của thành viên cần kiểm tra |

- **Response 200**: `ApiResult<UnfinishedLotsResponse>` (PROPOSED DTO):

```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
    "hasUnfinishedLots": true,
    "total": 2,
    "replacementRequired": true,
    "lots": [
      {
        "lotId": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
        "lotName": "Lô xoài cát chu 2026-08",
        "lotStatus": "APPROVED",
        "plantingDate": "2026-05-10",
        "harvestDate": "2026-09-05"
      },
      {
        "lotId": "c8d9e0f1-2a3b-4c5d-6e7f-8a9b0c1d2e3f",
        "lotName": "Lô rau sạch khu A",
        "lotStatus": "PENDING",
        "plantingDate": "2026-06-01",
        "harvestDate": "2026-08-30"
      }
    ]
  },
  "timestamp": "2026-08-27T07:05:00Z"
}
```

| Trường | Kiểu | Mô tả |
|---|---|---|
| `hasUnfinishedLots` | boolean | `true` nếu còn lô chưa hoàn thành → bắt buộc chọn người thay thế khi vô hiệu hóa |
| `replacementRequired` | boolean | Bằng `hasUnfinishedLots` — trường tường minh cho FE |
| `lots[].lotId` / `lotName` / `lotStatus` | — | Định danh lô + trạng thái hiện tại |
| `lots[].plantingDate` / `harvestDate` | date | Thông tin hiển thị giúp quản lý quyết định |

- **Business rules**:
    - Lô tính là **chưa hoàn thành** khi `lotStatus` thuộc {`DRAFT`, `PENDING`, `APPROVED`, `HARVESTED`, `PREPROCESSED`, `PACKAGED`} (PROPOSED — loại `CLOSED`, `REJECTED`, `RECALLED`; chờ backend xác nhận, xem D-4).
- **Error cases**: 401, 403, 404 (user không tồn tại), 400 (`"Thành viên không thuộc tổ chức này"` — khớp message hiện có của `assignRole`).
- **LƯU Ý**: hệ thống hiện **chưa có bảng phân công lô** — dữ liệu nguồn của endpoint này phụ thuộc quyết định D-3 (§11). Trước khi D-3 được triển khai, backend có thể trả `hasUnfinishedLots=false`.

### 5.3 Danh sách người thay thế hợp lệ — NEW

- **Method**: `GET`
- **Endpoint**: `/api/v1/organization/members/{userId}/replacement-candidates`
- **Mục đích**: Trả về danh sách thành viên đủ điều kiện tiếp nhận các lô của thành viên sắp bị vô hiệu hóa (group C trong yêu cầu story).
- **Authentication**: Bắt buộc (ACCESS JWT).
- **Authorization**: `@PreAuthorize("hasAnyRole('VT-01','VT-02')")`; chỉ thao tác trong tổ chức hiện tại.
- **Query parameters**:

| Param | Kiểu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `lotId` | UUID | Không | — | Nếu truyền, lọc ứng viên đủ quyền cho lô cụ thể đó |
| `keyword` | string | Không | — | Tìm theo tên/username, không phân biệt hoa thường |

- **Tiêu chí ứng viên** (server-side, không để FE tự lọc):
    1. Membership `ACTIVE` **trong cùng tổ chức** với người thao tác (`organization_users.status = 'ACTIVE'`).
    2. Khác thành viên sắp bị vô hiệu hóa (`userId` trên path).
    3. Có quyền phù hợp: mặc định vai trò `VT-03` (Người ghi sự kiện) hoặc vai trò được cấp quyền `chain_event:CREATE` theo cấu hình phân quyền theo tổ chức (REUSE `OrganizationRolePermissionConfig`, QTN-01).
    4. Tài khoản không bị khóa toàn cục (`users.status = 'ACTIVE'`).
- **Response 200**: `ApiResult<List<ReplacementCandidateResponse>>` (PROPOSED DTO):

```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "userId": "7d8e9f0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a",
      "username": "nong_dan_02",
      "fullName": "Trần Thị Cẩm",
      "roleCode": "VT-03",
      "roleName": "Người ghi sự kiện",
      "eligibleLotIds": ["bf57bca1-628d-4a11-8f92-bd12d1b74291"]
    }
  ],
  "timestamp": "2026-08-27T07:06:00Z"
}
```

- Danh sách rỗng là **response 200 hợp lệ** (`data: []`) — FE hiển thị "Không có ứng viên thay thế" và không cho gửi request vô hiệu hóa kèm replacement; đây không phải lỗi.
- **Error cases**: 401, 403, 404, 400 — như §5.2.

### 5.4 Vô hiệu hóa thành viên — NEW

- **Method**: `PATCH`
- **Endpoint**: `/api/v1/organization/members/{userId}/deactivate`
- **Mục đích**: Vô hiệu hóa membership của thành viên trong tổ chức hiện tại; thu hồi toàn bộ quyền và chấm dứt phiên đang mở; chuyển giao lô cho người thay thế nếu bắt buộc (TC-01, TC-02).
- **Authentication**: Bắt buộc (ACCESS JWT).
- **Authorization**: `@PreAuthorize("hasAnyRole('VT-01','VT-02')")` — VT-03 gọi sẽ bị chặn bởi Spring Security → **403** (TC-04). Backend enforce, FE chỉ ẩn nút.
- **Path parameters**:

| Param | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `userId` | UUID | Có | ID tài khoản của thành viên cần vô hiệu hóa |

- **Request body** — `DeactivateMemberRequest` (PROPOSED):

```json
{
  "reason": "Thành viên nghỉ việc từ 01/09/2026",
  "replacementUserId": "7d8e9f0a-1b2c-3d4e-5f6a-7b8c9d0e1f2a"
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `reason` | string | Có | `@NotBlank`, tối đa 500 ký tự (khớp `LockAccountRequest.reason` hiện có) |
| `replacementUserId` | UUID | Có điều kiện | Bắt buộc khi thành viên còn lô chưa hoàn thành (BR-3); phải là ứng viên hợp lệ theo §5.3 (BR-4) |

- **Business rules** (thứ tự kiểm tra, tất cả enforce phía backend):

| BR | Rule | Kết quả nếu vi phạm |
|---|---|---|
| BR-1 | Load membership theo `(organizationId từ JWT, userId)` — REUSE `findByOrganization_OrganizationIdAndUser_UserId` | User không tồn tại → **404** `"Thành viên không tồn tại"`; không thuộc tổ chức hiện tại → **400** `"Thành viên không thuộc tổ chức này"` (khớp message hiện có của `assignRole`) |
| BR-2 | Membership phải đang `ACTIVE` | Đã INACTIVE → **409** `"Thành viên đã ngừng hoạt động"` |
| BR-3 | Nếu còn lô chưa hoàn thành (theo §5.2) và `replacementUserId` **thiếu/null** → KHÔNG vô hiệu hóa, trả danh sách lô kèm yêu cầu chọn người thay thế (TC-02) | **409** + `errors.pendingLots` (mẫu bên dưới) |
| BR-4 | Nếu có `replacementUserId`: phải là membership `ACTIVE` cùng tổ chức, khác `userId` đang vô hiệu hóa, đủ quyền ghi sự kiện | **400** `"Người thay thế không hợp lệ"` |
| BR-5 | Set `organization_users.status = 'INACTIVE'`; **không** xóa bản ghi, **không** đổi `users.status`, **không** đổi role (giữ vai trò cũ để kích hoạt lại đúng) | — |
| BR-6 | Chuyển toàn bộ phân công lô còn hiệu lực sang `replacementUserId` (PROPOSED — phụ thuộc D-3) | — |
| BR-7 | Chấm dứt phiên đang mở: kiểm tra trạng thái membership trong luồng xác thực từng request và/hoặc `invalidateAllTokens(userId)` (D-1) | — |
| BR-8 | Publish `ActivityLogEvent` (§8) | — |

- **PROPOSED rules chờ backend/PO xác nhận** (không có bằng chứng trong code hiện tại):
    - Không cho vô hiệu hóa **chính mình** (BR-9): `userId == người thao tác` → **400** `"Không thể tự vô hiệu hóa tài khoản của chính mình"`.
    - Phạm vi vai trò đích: VT-02 chỉ vô hiệu hóa thành viên `VT-03` (khớp `validateAssignableRole` hiện có); thao tác vào membership `VT-02`/`VT-01` → **403** `"Quản lý hợp tác xã không thể vô hiệu hóa vai trò này"`.
    - Không vô hiệu hóa quản lý duy nhất còn lại của tổ chức (tránh tổ chức mất người quản trị).

- **Response 200** — thành công:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "a1b2c3d4-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "organizationId": "05c43400-8ae5-44c2-ad71-70b80dc98410",
    "userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
    "username": "nong_dan_01",
    "fullName": "Nguyễn Văn Bình",
    "email": "binh@example.com",
    "phone": "0912345678",
    "roleId": 3,
    "roleCode": "VT-03",
    "roleName": "Người ghi sự kiện",
    "status": "ACTIVE",
    "membershipStatus": "INACTIVE",
    "joinedAt": "2026-06-01T08:00:00"
  },
  "timestamp": "2026-08-27T07:10:00Z"
}
```

> `data` tái sử dụng `OrganizationUserResponse` hiện có; `membershipStatus = 'INACTIVE'` là kết quả chính. Trường `status` (toàn cục) giữ giá trị cũ vì tài khoản vẫn còn trong các tổ chức khác.

- **Response 409** — còn lô chưa hoàn thành, chưa chọn người thay thế (TC-02):

```json
{
  "success": false,
  "status": 409,
  "message": "Thành viên đang được phân công vào 2 lô chưa hoàn thành. Vui lòng chọn người thay thế",
  "errors": {
    "code": "MEMBER_HAS_UNFINISHED_LOTS",
    "requiresReplacement": true,
    "pendingLots": [
      {
        "lotId": "bf57bca1-628d-4a11-8f92-bd12d1b74291",
        "lotName": "Lô xoài cát chu 2026-08",
        "lotStatus": "APPROVED",
        "harvestDate": "2026-09-05"
      },
      {
        "lotId": "c8d9e0f1-2a3b-4c5d-6e7f-8a9b0c1d2e3f",
        "lotName": "Lô rau sạch khu A",
        "lotStatus": "PENDING",
        "harvestDate": "2026-08-30"
      }
    ]
  },
  "path": "/api/v1/organization/members/3c907154-1b15-46c8-bc4a-93df383a8b27/deactivate",
  "timestamp": "2026-08-27T07:10:30Z"
}
```

> `errors` trong payload 409 được trả qua `BusinessException(status, message, details)` — khớp `GlobalExceptionHandler.handleBusiness` hiện có (details gắn vào trường `errors`). Tên `code` dùng chuỗi mô tả cố định để FE phân nhánh; **không** thêm hệ thống error-code mới.

- **Error cases**: 400 (validation lý do / người thay thế không hợp lệ / không thuộc tổ chức), 401, 403, 404, 409 (đã INACTIVE / còn lô chưa hoàn thành) — tổng hợp tại §9.

### 5.5 Kích hoạt lại thành viên — NEW

- **Method**: `PATCH`
- **Endpoint**: `/api/v1/organization/members/{userId}/reactivate`
- **Mục đích**: Kích hoạt lại membership của thành viên đã ngừng hoạt động, bắt buộc nhập lý do (QTN-32 mục 9).
- **Authentication**: Bắt buộc (ACCESS JWT).
- **Authorization**: `@PreAuthorize("hasAnyRole('VT-01','VT-02')")`; chỉ trong tổ chức hiện tại.
- **Request body** — `ReactivateMemberRequest` (PROPOSED):

```json
{
  "reason": "Thành viên quay lại làm việc từ 01/10/2026"
}
```

| Field | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `reason` | string | Có | `@NotBlank`, tối đa 500 ký tự |

- **Business rules**:
    - BR-10: Load membership scope theo JWT (như BR-1) → 404/400 nếu không tồn tại/không thuộc tổ chức.
    - BR-11: Chỉ thực hiện khi membership đang `INACTIVE` → nếu `ACTIVE` trả **409** `"Thành viên đang hoạt động, không thể kích hoạt lại"`.
    - BR-12: Set `organization_users.status = 'ACTIVE'`; **giữ nguyên vai trò cũ** đã lưu trong `organization_users.role_id` (không tự cấp lại quyền — nếu cần đổi vai trò, quản lý dùng `PUT /organization/members/roles` vốn đã hoạt động lại bình thường sau khi membership ACTIVE).
    - BR-13: **Không** khôi phục/khôi hồi phân công lô cũ; nếu cần gán lại lô, thực hiện bằng thao tác phân công riêng (khi module D-3 tồn tại).
    - BR-14: Không thay đổi bất kỳ dữ liệu lịch sử nào (đúng QTN-32 mục 5–6).
    - BR-15: Publish `ActivityLogEvent` (§8).
- **Response 200**: `ApiResult<OrganizationUserResponse>` — như §5.4 với `membershipStatus: "ACTIVE"`.
- **Error cases**: 400 (validation lý do; không thuộc tổ chức), 401, 403, 404, 409 (đang ACTIVE).

### 5.6 API dùng lại không đổi — REUSE

| API | Endpoint | Ghi chú liên quan story |
|---|---|---|
| Danh mục vai trò | `GET /api/v1/roles` | `@PreAuthorize("hasAnyRole('VT-01','VT-02')")`; dùng cho màn hình thành viên. (Hiện trả `List<Role>` thô chưa bọc `ApiResult` — khuyết điểm hiện hữu, không thuộc phạm vi story) |
| Lịch sử hoạt động | `GET /api/v1/organizations/activity-logs` | Tra cứu thao tác vô hiệu hóa/kích hoạt lại (§8), filter `action`, `actorName`, `startDate`, `endDate` |
| Gán vai trò | `PUT /api/v1/organization/members/roles` | Đã chặn sẵn khi membership INACTIVE: `"Thành viên đã bị vô hiệu hóa. Vui lòng kích hoạt lại trước khi cấp quyền"` — không đổi |
| Đăng nhập / chọn tổ chức | `POST /api/v1/auth/login`, `GET /api/v1/auth/my-organizations`, `POST /api/v1/auth/switch-organization` | Sau khi vô hiệu hóa: tổ chức không còn hiển thị trong `my-organizations` (lọc ACTIVE); `switch-organization` chặn với `"Tổ chức không còn hoạt động với tài khoản này"`. Tài khoản vẫn đăng nhập được nếu còn membership ACTIVE ở tổ chức khác (đúng bản chất multi-organization) |
| Thêm/mời thành viên | `POST /api/v1/organization/members`, `POST /api/v1/organization/invitations` | Không đổi; vô hiệu hóa không xóa membership nên email/username không tái sử dụng được cho luồng mời nếu user đã tồn tại — hành vi hiện tại giữ nguyên |

---

## 6. Multi-tenant & Authorization

- **Nguồn sự thật của tổ chức**: `organizationId` luôn đọc từ ACCESS JWT (`CustomUserDetails.getOrganizationId()`), không nhận qua path/query/body. Lookup membership dùng `findByOrganization_OrganizationIdAndUser_UserId(orgId, userId)` — thành viên thuộc tổ chức khác **không thể** bị vô hiệu hóa bằng cách truyền `userId` trực tiếp (kết quả: 400 `"Thành viên không thuộc tổ chức này"`, không lộ thông tin membership chéo tổ chức).
- **Enforce phía backend**:
    - Tầng 1: `@PreAuthorize("hasAnyRole('VT-01','VT-02')")` trên controller (đã có sẵn trên `OrganizationMemberController`) → VT-03/04/05/06 nhận 403 với `errors = "ACCESS_DENIED"`, message `"Bạn không có quyền thực hiện chức năng này"` (`GlobalExceptionHandler.handleAccessDenied`).
    - Tầng 2: service tự kiểm tra scope tổ chức (BR-1, BR-10) và các business rule BR-2…BR-15.
    - **Không dựa vào frontend**: FE chỉ ẩn nút; mọi case 403/400/404 phải được backend trả về độc lập.
- **VT-02 chỉ thao tác trong tổ chức của mình** (QTN-01) — khớp hành vi hiện có của `assignRole`, `addMember`, `lockAccount` (VT-02 chỉ khóa tài khoản trong tổ chức mình).
- **Sau khi vô hiệu hóa**, mọi API ghi sự kiện/sự vụ của thành viên đó sẽ thất bại ở tầng xác thực với 401/403 (sau khi D-1 được cài đặt) — minh chứng yêu cầu "không thể tiếp tục ghi dữ liệu truy xuất".

## 7. Bảo toàn dữ liệu lịch sử (QTN-32 mục 5–6)

- **Không DELETE, không cascade**: `chain_events.recorded_by`, `production_lot.created_by`, `farm_log` và các bảng ghi nhận actor khác giữ nguyên FK. Vô hiệu hóa chỉ đổi **1 cột** `organization_users.status`.
- **Không đổi nhận diện**: `users.full_name`, `users.user_name` không bị sửa → các màn hình "Người ghi" trong dữ liệu cũ (join hiện có) tiếp tục hiển thị đúng tên tại thời điểm truy vấn.
- **Kích hoạt lại không hồi tố**: không sửa/generate lại event, không đổi hash/previous_hash của `chain_events`.
- **Không đổi trạng thái toàn cục**: `users.status` giữ nguyên vì user có thể còn membership ACTIVE ở tổ chức khác (multi-organization). Khóa toàn cục thuộc nghiệp vụ khác (`AccountLock` — story NCL-01 về giám sát đăng nhập).
- **Ghi chú (không thuộc phạm vi story)**: hiển thị tên lịch sử hiện lấy qua join `users` hiện thời; nếu user đổi `full_name` sau này, dữ liệu cũ hiển thị theo tên mới — hành vi join hiện có của toàn hệ thống, không thay đổi trong story này.

## 8. Audit log (QTN-32 mục 10)

REUSE cơ chế hiện tại: service publish `ActivityLogEvent` (userId, username, fullName, organizationId, action, description, entityType, entityId, ipAddress, timestamp) → `ActivityLogListener` ghi vào `activity_logs`. Tra cứu qua `GET /api/v1/organizations/activity-logs` (VT-02).

| entityType | action | Khi nào | Description mẫu |
|---|---|---|---|
| `OrganizationUser` | `DEACTIVATE` (PROPOSED) | Vô hiệu hóa thành công | `"Vô hiệu hóa thành viên [fullName] ([username]). Lý do: [reason]. Người thay thế: [replacementFullName / 'Không']"` |
| `OrganizationUser` | `REACTIVATE` (PROPOSED) | Kích hoạt lại thành công | `"Kích hoạt lại thành viên [fullName] ([username]). Lý do: [reason]"` |
| `OrganizationUser` | `DEACTIVATE_BLOCKED` (tùy chọn, PROPOSED) | Người quản lý cố vô hiệu hóa khi còn lô | `"Từ chối vô hiệu hóa thành viên ... vì còn [N] lô chưa hoàn thành"` |

- `entityId` = ID bản ghi `organization_users` (khớp cách `addMember` hiện log với entityType `"OrganizationUser"`).
- Định dạng log **tương tự 100%** các log sẵn có (`CREATE`, `UPDATE`, `ACCESS_DENIED`, `CREATE_PRODUCTION_LOT`) — không thêm bảng/cơ chế mới.

---

## 9. Bảng error cases tổng hợp

Toàn bộ dùng convention hiện có (`ApiResult` + `GlobalExceptionHandler`); **không tạo error code mới**.

| # | Tình huống | HTTP | `message` | `errors` |
|---|---|---|---|---|
| 1 | Chưa đăng nhập / token hết hạn | 401 | `Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn` | — |
| 2 | VT-03 (hoặc VT-04…VT-06) gọi API (TC-04) | 403 | `Bạn không có quyền thực hiện chức năng này` | `"ACCESS_DENIED"` |
| 3 | VT-02 thao tác vai trò vượt phạm vi (PROPOSED) | 403 | `Quản lý hợp tác xã không thể vô hiệu hóa vai trò này` | — |
| 4 | User không tồn tại | 404 | `Thành viên không tồn tại` | — |
| 5 | Member không thuộc tổ chức hiện tại (chống chéo tổ chức) | 400 | `Thành viên không thuộc tổ chức này` | — |
| 6 | Member đã INACTIVE khi gọi deactivate | 409 | `Thành viên đã ngừng hoạt động` | — |
| 7 | Member đang ACTIVE khi gọi reactivate | 409 | `Thành viên đang hoạt động, không thể kích hoạt lại` | — |
| 8 | Còn lô chưa hoàn thành, chưa chọn người thay thế (TC-02) | 409 | `Thành viên đang được phân công vào N lô chưa hoàn thành. Vui lòng chọn người thay thế` | `{code, requiresReplacement: true, pendingLots: [...]}` |
| 9 | `replacementUserId` không hợp lệ (không ACTIVE / khác tổ chức / trùng member bị vô hiệu hóa / thiếu quyền) | 400 | `Người thay thế không hợp lệ` | `{replacementUserId: "..."}` |
| 10 | Tự vô hiệu hóa chính mình (PROPOSED BR-9) | 400 | `Không thể tự vô hiệu hóa tài khoản của chính mình` | — |
| 11 | Validation lý do (trống, > 500 ký tự) | 400 | `Dữ liệu không hợp lệ` | `{reason: "Lý do không được để trống" / "Lý do không được vượt quá 500 ký tự"}` |
| 12 | Body JSON sai định dạng | 400 | `Dữ liệu gửi lên không hợp lệ` | — |
| 13 | Concurrent update (2 quản lý vô hiệu hóa cùng lúc) | — | Hiện **chưa có** cơ chế (`@Version` không tồn tại) — xem D-6 | UNKNOWN |

Mẫu lỗi 400 validation (khớp `handleValidation` hiện có):

```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "reason": "Lý do không được để trống"
  },
  "path": "/api/v1/organization/members/3c907154-1b15-46c8-bc4a-93df383a8b27/deactivate",
  "timestamp": "2026-08-27T07:11:00Z"
}
```

Mẫu lỗi 403 (khớp `handleAccessDenied` hiện có):

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền thực hiện chức năng này",
  "errors": "ACCESS_DENIED",
  "path": "/api/v1/organization/members/3c907154-1b15-46c8-bc4a-93df383a8b27/deactivate",
  "timestamp": "2026-08-27T07:11:30Z"
}
```

## 10. Ánh xạ Acceptance Criteria

| TC | Kịch bản | API/logic đảm bảo |
|---|---|---|
| TC-01 | Luồng thành công: không còn lô, VT-02 thao tác, nhập lý do, xác nhận → mất quyền ngay, phiên chấm dứt, INACTIVE, có audit | §5.4 (BR-1, BR-2, BR-5, BR-7, BR-8) — **điều kiện tiên quyết D-1** |
| TC-02 | Còn 2 lô chưa hoàn thành → không vô hiệu hóa; trả danh sách 2 lô; yêu cầu chọn thay thế; chỉ thực hiện sau khi chọn hợp lệ | §5.2 (precheck) + §5.4 BR-3/BR-4/BR-6 + payload 409 `pendingLots` |
| TC-03 | Bảo toàn dữ liệu cũ: sự kiện cũ còn nguyên, tên người ghi giữ nguyên, không cascade delete | §7 — chỉ đổi 1 cột `organization_users.status` |
| TC-04 | Người ghi sự kiện cố vô hiệu hóa → bị từ chối theo authorization hiện tại | §6 tầng 1 (`@PreAuthorize`) → 403 `ACCESS_DENIED` |

## 11. Các điểm phụ thuộc Backend cần quyết định

| Mã | Điểm cần quyết định | Phương án đề xuất |
|---|---|---|
| **D-1** | **Chấm dứt phiên tức thời** (bắt buộc cho TC-01): hiện JWT ACCESS còn sống 1 giờ sau khi membership INACTIVE vì filter xác thực không kiểm tra trạng thái. | (a) *Khuyến nghị*: thêm kiểm tra `OrganizationUserStatus.INACTIVE` / `UserStatus.INACTIVE` trong `CustomUserDetailsService.loadUserByUserIdAndOrganizationId()` (ném exception → request bị chặn ngay). (b) Cài đặt thật `AccountLockService.invalidateAllTokens()` (token versioning hoặc blacklist). Cần ít nhất (a). |
| **D-2** | `selectOrganization` hiện chỉ kiểm tra `status == null`, chưa chặn INACTIVE như `switchOrganization` — cần đồng bộ để chặn đường vào tổ chức. | Thêm `if (orgUser.getStatus() != ACTIVE) throw BusinessException("Tổ chức không còn hoạt động với tài khoản này")`. |
| **D-3** | **Data model phân công lô** chưa tồn tại → API #2, BR-3, BR-6 chưa có nguồn dữ liệu. | (i) Coi `production_lot.created_by` là người phụ trách (ít khớp "phân công"); (ii) *Khuyến nghị khi triển khai module phân công*: bảng `lot_assignments (lot_id, user_id, active, assigned_by, assigned_at)`. Trước khi có D-3, backend trả `hasUnfinishedLots = false` và bỏ qua BR-3/BR-6 — cần PO xác nhận. |
| **D-4** | Định nghĩa "lô chưa hoàn thành" (danh sách `ProductionLotStatus`). | PROPOSED: mọi trạng thái trừ `CLOSED`, `REJECTED`, `RECALLED`. |
| **D-5** | Rule tự vô hiệu hóa / vô hiệu hóa VT-02 khác / vô hiệu hóa quản lý cuối cùng. | PROPOSED tại §5.4 — chờ PO xác nhận. |
| **D-6** | Concurrent update: `organization_users` chưa có `@Version`. | Thêm `@Version` (khuyến nghị, đổi schema nhẹ) hoặc chấp nhận last-write-wins + audit log đầy đủ. |
| **D-7** | Thông báo cho thành viên bị vô hiệu hóa (email/notification inbox) — story không yêu cầu. | Nếu cần, REUSE `NotificationService` (đã dùng cho lock/unlock). |
| **D-8** | Thêm trường `membershipStatus` vào `OrganizationUserResponse` và filter `status` cho GET members. | Thống nhất tên trường BE/FE trước khi implement (đề xuất đã nêu tại §5.1). |

## 12. Changelog

| Ngày | Phiên bản | Người thực hiện | Mô tả thay đổi |
| :--- | :--- | :--- | :--- |
| 2026-08-27 | v1.0.0 | AI Agent (Cline) | Khởi tạo tài liệu thiết kế API cho User Story NCL-01-CN-009, quy tắc QTN-32: vô hiệu hóa thành viên, thu hồi quyền, bảo toàn dữ liệu, phân công người thay thế, kích hoạt lại |
