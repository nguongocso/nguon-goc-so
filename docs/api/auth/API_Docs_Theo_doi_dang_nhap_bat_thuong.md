# API Docs - Theo doi dang nhap bat thuong

## 1. Pham vi hien tai

Chuc nang nam trong package `auth`, ghi nhan lan dang nhap, tao su kien bat thuong, gom su kien theo tai khoan nghi van, cho phep quan tri vien xu ly va khoa/mo khoa tai khoan.

Nguon trang thai:

- `AccountLock.status` (`LOCKED`/`UNLOCKED`) la nguon trang thai khoa thuc te.
- `User.status` chi phan anh vong doi tai khoan: `ACTIVE`/`INACTIVE`; khong con gia tri `LOCKED`.
- `LoginAnomaly.status` va `SuspiciousCase.status` chi phan anh xu ly su kien: `OPEN`/`DISMISSED`.
- `SuspiciousCase` dai dien cho mot tai khoan nghi van va duoc dung cho tab **Tai khoan nghi van**.

## 2. Quy tac phat hien va khoa

- Ap dung cho tai khoan ton tai trong he thong. Username khong tim thay user thi luong hien tai bo qua ghi nhan.
- Sai mat khau tu 5 lan tro len trong cua so 2 phut tao `REPEATED_FAILED_LOGIN`.
- Moi lan dang nhap co IP client duoc luu vao `login_attempts`; IP public duoc resolver `https://ipwho.is` chuyen thanh `countryCode`.
- Dang nhap thanh cong tu IP moi hoac quoc gia moi so voi cac lan dang nhap thanh cong truoc do tao `UNUSUAL_COUNTRY`, tru lan dang nhap thanh cong dau tien.
- IP private, loopback va link-local khong goi dich vu GeoIP; khi resolver loi, dang nhap van tiep tuc va `countryCode` de trong.
- Moi anomaly duoc luu vao `login_anomalies` va cap nhat/gom vao `suspicious_cases` theo tai khoan.
- Khoa tai khoan cap nhat `User.status = INACTIVE`, tao `AccountLock.status = LOCKED` va vo hieu hoa token hien tai.
- Khoa tam co thoi han; scheduler tu dong chuyen lock het han sang `UNLOCKED` va user ve `ACTIVE`.
- Khoa vinh vien chi mo lai khi goi API unlock.

Trong moi truong trien khai, chi chap nhan `X-Forwarded-For` tu dia chi proxy nam trong bien moi truong `TRUSTED_PROXY_IPS` (phan tach bang dau phay, ho tro ca IP don va CIDR). Mac dinh tin cac IP loopback (`127.0.0.1`, `::1`) va cac dai mang noi bo pho bien cua Docker/Kubernetes (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`). Khi peer truc tiep la proxy tin cay, `IpUtils` duyet chuoi `X-Forwarded-For` tu phai sang trai, bo qua cac proxy tin cay va tra ve IP khong tin cay dau tien (IP client thuc) de chong gia mao header.

## 3. Phan quyen thuc te

Controller yeu cau dang nhap. Service hien tai chi cho phep role `VT-01` thuc hien cac thao tac xem, khoa, mo khoa va xu ly; cac role khac bi tu choi. Frontend cung gioi han route va menu Theo doi dang nhap bat thuong cho `VT-01`.

> Tai lieu backlog cu mo ta VT-02 co quyen theo pham vi to chuc, nhung day khong phai contract cua code hien tai.

## 4. Base URL va response chung

```text
/api/v1/auth/security
```

Response thanh cong co dang `ApiResult<T>`:

```json
{
  "success": true,
  "status": 200,
  "data": {},
  "timestamp": "2026-08-19T09:15:00Z"
}
```

## 5. API endpoints

### 5.1. Lich su dang nhap

```http
GET /api/v1/auth/security/login-history
```

Query parameters:

| Ten | Kieu | Mac dinh | Mo ta |
| --- | --- | --- | --- |
| `userId` | UUID | - | Loc theo tai khoan |
| `result` | String | - | `SUCCESS` hoac `FAILED` |
| `organizationId` | UUID | - | Loc theo to chuc neu duoc phep |
| `startDate` | String | - | Dinh dang `yyyy-MM-dd` |
| `endDate` | String | - | Dinh dang `yyyy-MM-dd` |
| `page` | int | `0` | So trang |
| `size` | int | `10` | Kich thuoc trang |

Response item `LoginHistoryResponse` gom: `id`, `userId`, `usernameInput`, `roleCode`, `result`, `ipAddress`, `countryCode`, `isNewCountry`, `createdAt`.

### 5.2. Danh sach su kien bat thuong

```http
GET /api/v1/auth/security/login-anomalies
```

Query parameters:

| Ten | Kieu | Mac dinh | Mo ta |
| --- | --- | --- | --- |
| `status` | String | - | `OPEN` hoac `DISMISSED` |
| `reasonCode` | String | - | `REPEATED_FAILED_LOGIN` hoac `UNUSUAL_COUNTRY` |
| `username` | String | - | Tim gan dung theo username hoac ho ten |
| `organizationId` | UUID | - | Loc theo to chuc |
| `page` | int | `0` | So trang |
| `size` | int | `10` | Kich thuoc trang |

Response item `LoginAnomalyResponse`:

```json
{
  "id": "a71f4e2b-0021-4a3f-9c2b-1e6f8a4d5c7b",
  "userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
  "username": "field_staff01",
  "fullName": "Tran Van B",
  "roleCode": "VT-03",
  "organizationId": "b1e0c9a2-0011-4a3f-9c2b-1e6f8a4d5c7b",
  "organizationName": "HTX Cat Tuong",
  "reasonCode": "REPEATED_FAILED_LOGIN",
  "attemptCount": 5,
  "ipAddress": "203.0.113.5",
  "countryCode": "VN",
  "detectedAt": "2026-08-19T09:12:10Z",
  "status": "OPEN",
  "accountLocked": false,
  "lockUntil": null,
  "permanentLock": false,
  "notificationId": null
}
```

`accountLocked`, `lockUntil` va `permanentLock` duoc tinh tu lock hien tai cua user, khong phai tu `LoginAnomaly.status`.

### 5.3. Danh sach tai khoan nghi van

```http
GET /api/v1/auth/security/suspicious-cases
```

Day la endpoint cua tab **Tai khoan nghi van**. Moi ban ghi dai dien cho mot tai khoan, khong phai mot anomaly row.

Query parameters:

| Ten | Kieu | Mac dinh | Mo ta |
| --- | --- | --- | --- |
| `status` | String | - | `OPEN` hoac `DISMISSED` |
| `username` | String | - | Tim gan dung theo username hoac ho ten |
| `organizationId` | UUID | - | Loc theo to chuc |
| `page` | int | `0` | So trang |
| `size` | int | `10` | Kich thuoc trang |

Response item `SuspiciousCaseResponse`:

```json
{
  "id": "f71f4e2b-0021-4a3f-9c2b-1e6f8a4d5c7b",
  "userId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
  "username": "field_staff01",
  "fullName": "Tran Van B",
  "organizationId": "b1e0c9a2-0011-4a3f-9c2b-1e6f8a4d5c7b",
  "organizationName": "HTX Cat Tuong",
  "status": "OPEN",
  "anomalyCount": 1,
  "firstDetectedAt": "2026-08-19T09:12:10Z",
  "lastDetectedAt": "2026-08-19T09:12:10Z",
  "createdAt": "2026-08-19T09:12:10Z",
  "resolvedAt": null
}
```

Danh sach sap xep theo `lastDetectedAt` giam dan. Frontend dung `userId` de unique khi tinh thong ke, tranh dem trung tai khoan.

### 5.4. Khoa tai khoan

```http
PATCH /api/v1/auth/security/accounts/{accountId}/lock
Content-Type: application/json
```

Request body:

```json
{
  "anomalyId": "a71f4e2b-0021-4a3f-9c2b-1e6f8a4d5c7b",
  "reason": "Khoa do phat hien dang nhap bat thuong",
  "days": 0,
  "hours": 1,
  "minutes": 0,
  "permanent": false
}
```

| Truong | Kieu | Bat buoc | Mo ta |
| --- | --- | --- | --- |
| `anomalyId` | UUID | Khong | Su kien lien quan |
| `reason` | String | Khong | Toi da 500 ky tu |
| `days` | Integer | Khong | So ngay khoa tam, mac dinh 0 |
| `hours` | Integer | Khong | So gio khoa tam, mac dinh 0 |
| `minutes` | Integer | Khong | So phut khoa tam, mac dinh 0 |
| `permanent` | boolean | Khong | `true` de khoa vinh vien |

Neu `permanent = false` va tong thoi gian bang 0, service mac dinh khoa 60 phut.

Response item `AccountLockResponse`:

```json
{
  "accountId": "3c907154-1b15-46c8-bc4a-93df383a8b27",
  "status": "LOCKED",
  "lockedBy": "admin01",
  "lockedAt": "2026-08-19T09:20:00Z",
  "lockUntil": "2026-08-19T10:20:00Z",
  "permanent": false,
  "unlockedBy": null,
  "unlockedAt": null,
  "reason": "Khoa do phat hien dang nhap bat thuong",
  "notificationSent": true
}
```

### 5.5. Mo khoa tai khoan

```http
PATCH /api/v1/auth/security/accounts/{accountId}/unlock
```

Khong co request body. Khi thanh cong, lock hien tai chuyen sang `UNLOCKED`, `User.status` chuyen sang `ACTIVE`, va response dung `status = "UNLOCKED"`.

### 5.6. Danh dau da giai quyet anomaly cua tai khoan

```http
PATCH /api/v1/auth/security/accounts/{accountId}/resolve-anomalies
```

Khong co request body. Service chuyen cac `LoginAnomaly` va `SuspiciousCase` dang `OPEN` cua tai khoan sang `DISMISSED`, dong thoi cap nhat `resolvedAt` cho suspicious case. Response thanh cong co `data: null`.

## 6. Phan trang va loc

Tat ca API danh sach tra ve `PageResponse<T>`:

```json
{
  "items": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Backend hien tai doc du lieu, loc `status`/`reasonCode`/`username`, sau do phan trang ket qua da loc. Username duoc trim va tim khong phan biet hoa thuong theo username hoac full name.

## 7. Thong bao

Luot phat hien anomaly va thao tac khoa tai khoan goi `NotificationService` hien co. Khong tao REST endpoint thong bao moi; cac API thong bao hien co van duoc dung de doc thong bao va unread count.

## 8. Dong bo voi giao dien

- Tab **Su kien bat thuong** goi `/login-anomalies`.
- Tab **Tai khoan nghi van** goi `/suspicious-cases`.
- `IP_GEOLOCATION_ENABLED=false` co the dung de tat resolver; `IP_GEOLOCATION_BASE_URL` dung de thay endpoint GeoIP tuong thich response `success` va `country_code`.
- Thong ke tai khoan nghi van tinh tren tap tai khoan unique theo `userId`, khong cong don cac anomaly rows.
- Trang theo doi hien duoc gioi han tren frontend cho `VT-01`; thay doi quyen backend can cap nhat dong thoi voi Sidebar va route.
- Giao dien cap nhat ngam sau thao tac, khong reload toan trang.

## 9. Ma nguon tham chieu

- `backend/src/main/java/vn/nguongocso/auth/controller/LoginMonitoringController.java`
- `backend/src/main/java/vn/nguongocso/auth/service/impl/LoginMonitoringServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/auth/service/impl/AccountLockServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/auth/dto/request/LockAccountRequest.java`
- `backend/src/main/java/vn/nguongocso/auth/dto/response/LoginAnomalyResponse.java`
- `backend/src/main/java/vn/nguongocso/auth/dto/response/SuspiciousCaseResponse.java`
- `frontend/src/api/loginAnomalyApi.ts`
- `frontend/src/types/loginAnomaly.ts`
