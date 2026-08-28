# API — Vô hiệu hóa & Kích hoạt lại thành viên tổ chức (QTN-32)

> **Phiên bản 2 — Cập nhật quan trọng:**
> Luồng *chuyển giao lô / chọn người thay thế* khi vô hiệu hóa thành viên đã bị **gỡ bỏ hoàn toàn**
> ở cả backend lẫn frontend, vì hệ thống hiện **chưa có phân quyền ghi sự kiện theo từng lô sản
> xuất** (quyết định D-4). Việc "thay người ghi sự kiện cho lô" không thể thực hiện tự động, nên
> trách nhiệm rà soát công việc chưa hoàn thành và sắp xếp người tiếp nhận được chuyển cho quản lý
> **bằng thao tác thủ công**. Hệ thống hỗ trợ bằng **modal cảnh báo bắt buộc** ở bước xác nhận trên
> frontend (xem [Luồng frontend](#luồng-frontend)).

## Mục lục

1. [Tổng quan](#tổng-quan)
2. [Đầu cuối đã gỡ bỏ](#đầu-cuối-đã-gỡ-bỏ)
3. [Vô hiệu hóa thành viên](#1-vô-hiệu-hóa-thành-viên)
4. [Kích hoạt lại thành viên](#2-kích-hoạt-lại-thành-viên)
5. [Luồng frontend](#luồng-frontend)
6. [Mô hình dữ liệu](#mô-hình-dữ-liệu)
7. [Nhật ký hoạt động](#nhật-ký-hoạt-động)
8. [Nhật ký thay đổi](#nhật-ký-thay-đổi)

## Tổng quan

| | |
|---|---|
| Base URL | `/api/v1/organization/members` |
| Xác thực | Bearer JWT; yêu cầu vai trò `VT-01` (Quản trị viên nền tảng) hoặc `VT-02` (Quản lý HTX) |
| Content-Type | `application/json` |
| Phản hồi | Bọc trong `ApiResult` (`vn.nguongocso.common.ApiResult`) — `{ success, message, data }` |

Phạm vi dữ liệu luôn giới hạn trong tổ chức của JWT (`organizationId` trong token). Truy vấn user
không thuộc tổ chức hiện tại trả về lỗi nghiệp vụ, không lộ thông tin membership chéo tổ chức.

## Đầu cuối đã gỡ bỏ

Các endpoint sau **không còn tồn tại** và sẽ trả về 404/405 nếu gọi:

| Endpoint cũ | Trạng thái |
|---|---|
| `GET /{userId}/unfinished-lots` | Đã gỡ bỏ (precheck lô chưa hoàn thành) |
| `GET /{userId}/replacement-candidates` | Đã gỡ bỏ (danh sách người thay thế) |

Đồng thời:

- `DeactivateMemberRequest` **không còn trường `replacementUserId`** — gửi trường này sẽ bị bỏ qua.
- Lỗi `409` với payload `errors.code = "MEMBER_HAS_UNFINISHED_LOTS"` (`requiresReplacement`,
  `pendingLots`) **không còn được trả về**. Thành viên còn lô chưa hoàn thành vẫn được vô hiệu hóa
  bình thường; các lô đó mất người ghi sự kiện và do quản lý xử lý thủ công.

---

## 1. Vô hiệu hóa thành viên

Đặt trạng thái membership của một thành viên trong tổ chức hiện tại về `INACTIVE`.

```
PATCH /api/v1/organization/members/{userId}/deactivate
```

### Request body

```json
{
  "reason": "Thành viên nghỉ việc từ ngày 01/09"
}
```

| Trường | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `reason` | string | Có | Không rỗng, tối đa 500 ký tự |

### Response 200 — OK

`ApiResult<OrganizationUserResponse>` — bản ghi membership sau khi cập nhật
(`membershipStatus = "INACTIVE"`, `status` của tài khoản và `roleCode` **giữ nguyên**).

```json
{
  "success": true,
  "message": "...",
  "data": {
    "id": "0198b6e4-...",
    "organizationId": "0198a1c0-...",
    "userId": "0198b100-...",
    "username": "nguoighisuken01",
    "fullName": "Trần Người Ghi",
    "email": "ghi@htx.vn",
    "phone": "0900000000",
    "roleId": "0198a5d0-...",
    "roleCode": "VT-03",
    "roleName": "Người ghi sự kiện",
    "status": "ACTIVE",
    "membershipStatus": "INACTIVE",
    "joinedAt": "2026-01-10T08:00:00"
  }
}
```

### Lỗi nghiệp vụ

| HTTP | Thông điệp | Điều kiện |
|---|---|---|
| 400 | `Lý do không được để trống` | `reason` rỗng |
| 400 | `Lý do không được vượt quá 500 ký tự` | `reason` > 500 ký tự |
| 400 | `Thành viên không thuộc tổ chức này` | User không thuộc tổ chức của JWT |
| 400 | `Không thể tự vô hiệu hóa tài khoản của chính mình` | Người gọi tự vô hiệu hóa chính mình |
| 403 | `Bạn không có quyền thực hiện chức năng này` | Người gọi không phải VT-01/VT-02 |
| 403 | `Quản lý hợp tác xã không thể vô hiệu hóa vai trò này` | VT-02 vô hiệu hóa vai trò khác VT-03 |
| 404 | `Thành viên không tồn tại` | `userId` không tồn tại |
| 409 | `Thành viên đã ngừng hoạt động` | Membership đã ở trạng thái `INACTIVE` |
| 409 | `Không thể vô hiệu hóa quản lý duy nhất còn lại của tổ chức` | Target là VT-02 cuối cùng đang ACTIVE |

### Quy tắc nghiệp vụ

- **BR-1** — Cấm tự vô hiệu hóa chính mình.
- **BR-2** — Chỉ VT-01/VT-02 được gọi (tầng 1: `@PreAuthorize` trên controller; tầng 2: kiểm tra
  vai trò trong service).
- **BR-3** — VT-02 chỉ được vô hiệu hóa thành viên vai trò VT-03 (khớp `validateAssignableRole`).
- **BR-4** — Không vô hiệu hóa quản lý duy nhất còn lại của tổ chức.
- **BR-5** — Chỉ membership đang `ACTIVE` mới vô hiệu hóa được.
- **BR-6** — Không xóa bản ghi, không đổi `users.status`, không đổi vai trò — vai trò cũ được giữ
  nguyên để kích hoạt lại đúng quyền ban đầu.
- **BR-7 (mới)** — **Không** chặn cũng như **không** chuyển giao lô chưa hoàn thành. Nếu thành viên
  còn lô đang triển khai, các lô đó sẽ tạm thời không có người ghi sự kiện; quản lý tự bố trí lại
  người (hiện chưa có phân quyền ghi sự kiện theo lô — D-4). Frontend bắt buộc xác nhận modal
  cảnh báo trước khi gọi API.
- **BR-8** — Token JWT đang hành động vẫn hợp lệ đến khi hết hạn; thay đổi có hiệu lực đầy đủ khi
  client làm mới phiên / đăng nhập lại.

---

## 2. Kích hoạt lại thành viên

Đặt membership đã `INACTIVE` trở lại `ACTIVE`.

```
PATCH /api/v1/organization/members/{userId}/reactivate
```

### Request body

```json
{
  "reason": "Thành viên quay lại làm việc"
}
```

| Trường | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---|---|
| `reason` | string | Có | Không rỗng, tối đa 500 ký tự |

### Response 200 — OK

`ApiResult<OrganizationUserResponse>` với `membershipStatus = "ACTIVE"`.

### Lỗi nghiệp vụ

| HTTP | Thông điệp | Điều kiện |
|---|---|---|
| 400/404 | Xem deactivate | Không thuộc tổ chức / user không tồn tại |
| 409 | `Thành viên đang hoạt động, không thể kích hoạt lại` | Membership đang `ACTIVE` |
| 400 | `Lý do không được để trống` | `reason` rỗng |

### Quy tắc nghiệp vụ

- **BR-9** — Chỉ membership `INACTIVE` mới kích hoạt lại được.
- **BR-10** — Vai trò cũ (`organization_users.role_id`) được giữ nguyên, **không** tự cấp lại quyền;
  phân công lô cũ **không** được hồi tố (kể cả các lô đã "mất người" khi vô hiệu hóa).

---

## Luồng frontend

Component: `frontend/src/components/organization/DeactivateMemberDialog.tsx`.

1. **Bước xác nhận** — quản lý nhập lý do vô hiệu hóa rồi bấm **Tiếp tục**.
2. **Modal cảnh báo bắt buộc** hiển thị nội dung:

   > Khi thực hiện vô hiệu hóa thành viên, thành viên sẽ không còn quyền thao tác đến tất cả dữ
   > liệu ghi sự kiện của lô sản xuất. Hãy thực hiện rà soát, kiểm tra lại các công việc chưa hoàn
   > thành và thay thế người để công việc không bị ảnh hưởng.

   với hai nút: **Xác nhận** (mới gọi `PATCH /deactivate`) hoặc **Hủy bỏ** (đóng modal, không gọi API).
3. **Kết quả** — thành công: toast + đóng dialog; lỗi: hiển thị thông điệp từ `ApiResult`.

Không còn bước "chọn người thay thế cho từng lô" (đã gỡ cùng luồng backend).

---

## Mô hình dữ liệu

### OrganizationUserResponse

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | UUID | Khóa `organization_users` |
| `organizationId` | UUID | |
| `userId` | UUID | |
| `username` | string | `users.user_name` |
| `fullName` | string | |
| `email` | string | |
| `phone` | string | |
| `roleId` | UUID | Vai trò hiện tại (giữ nguyên khi INACTIVE) |
| `roleCode` | string | `VT-01`…`VT-03` |
| `roleName` | string | |
| `status` | string | Trạng thái tài khoản toàn cục (`users.status`) |
| `membershipStatus` | string | `ACTIVE` / `INACTIVE` (`organization_users.status`) |
| `joinedAt` | datetime | |

### Các DTO đã xóa

`MemberLotSummary`, `UnfinishedLotsResponse`, `ReplacementCandidateResponse`,
`ReplacementRequiredError` — không còn trong codebase.

---

## Nhật ký hoạt động

| Action | Kích phát | Nội dung `description` |
|---|---|---|
| `DEACTIVATE` | Vô hiệu hóa thành công | Tên + username thành viên, lý do |
| `REACTIVATE` | Kích hoạt lại thành công | Tên + username thành viên, lý do |

(`entityType = "OrganizationUser"`, `entityId` = id membership, kèm IP người thao tác.)

## Nhật ký thay đổi

| Phiên bản | Thay đổi |
|---|---|
| v2 (hiện tại) | **Gỡ bỏ** toàn bộ luồng chuyển giao lô: 2 endpoint precheck/candidates, trường `replacementUserId`, lỗi 409 `MEMBER_HAS_UNFINISHED_LOTS`, service `transferActiveAssignments`/`LotAssignmentService`. Thay bằng modal cảnh báo bắt buộc phía frontend. Lý do: chưa có phân quyền ghi sự kiện theo lô (D-4). |
| v1 | Luồng đầy đủ gồm precheck lô chưa hoàn thành và chọn người thay thế (đã lỗi thời). |
