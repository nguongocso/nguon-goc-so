# API: Trang tra cứu công khai bằng tiếng Anh

**User Story:** NCL-06-CN-004 — Trang tra cứu công khai bằng tiếng Anh  
**Epic:** NCL-06 — Tra cứu công khai cho người tiêu dùng  
**Nhánh Git:** `feature/NCL-06-CN-004-public-trace-lookup-english`

---

## 1. Thông Tin Chung

### 1.1. Mục Tiêu
Cho phép Người tiêu dùng và đối tác nước ngoài xem trang tra cứu công khai bằng tiếng Anh khi quét mã QR tem truy xuất nguồn gốc trên sản phẩm xuất khẩu, đáp ứng:
* Tự động chọn ngôn ngữ theo cấu hình trình duyệt (`navigator.language`) ở lần truy cập đầu tiên.
* Có nút chuyển đổi ngôn ngữ (VI / EN) và ghi nhớ lựa chọn trong phiên làm việc (`sessionStorage`).
* Toàn bộ nhãn giao diện, tên sự kiện chuỗi cung ứng, trạng thái lô hàng, tiêu chuẩn và cảnh báo thu hồi hiển thị bằng tiếng Anh chuẩn xác.
* Dữ liệu do người dùng nhập (tên lô, tên vùng trồng, ghi chú...) giữ nguyên tiếng Việt và được đánh dấu là nội dung gốc (`[Original]`).
* Danh mục dùng chung (loại nông sản, tiêu chuẩn chất lượng, chỉ tiêu kiểm nghiệm) được bổ sung trường tên tiếng Anh (`nameEn`) do Quản trị viên nền tảng (`PLATFORM_ADMIN`) quản lý.
* Cơ chế dự phòng (Fallback): nếu danh mục chưa có tên tiếng Anh, hệ thống hiển thị tên tiếng Việt thay thế thay vì để trống.

---

## 2. Quy Tắc Nghiệp Vụ (Business Rules)

* **QTN-12 (Tra cứu công khai chỉ được xem):** Người tiêu dùng tra cứu công khai không cần đăng nhập và chỉ xem, không sửa dữ liệu. Chặn mọi thao tác thay đổi dữ liệu từ trang công khai.
* **QTN-17 (Danh mục dùng chung chỉ do quản trị viên nền tảng sửa):** Danh mục loại nông sản, danh mục tiêu chuẩn và danh mục chỉ tiêu kiểm nghiệm là dữ liệu dùng chung, chỉ Quản trị viên nền tảng (`PLATFORM_ADMIN` / vai trò `VT-01`) được phép thêm hoặc sửa trường tên tiếng Anh (`nameEn`).
* **Quy tắc Fallback (NCL-06-CN-004-TC-04):** Khi người dùng xem ở chế độ tiếng Anh, nếu trường `nameEn` của loại nông sản, tiêu chuẩn hoặc chỉ tiêu kiểm nghiệm rỗng (`null` hoặc khoảng trắng), hệ thống tự động fallback sử dụng tên tiếng Việt (`name` / `criterionName`) thay vì để trống.
* **Quy tắc Dữ liệu gốc (NCL-06-CN-004-TC-02):** Dữ liệu do người dùng nhập (tên lô, ghi chú, tên lô hàng do doanh nghiệp đặt) được giữ nguyên bản gốc tiếng Việt và hiển thị kèm chỉ báo `[Original]` / `[Nội dung gốc]`.
* **Quy tắc Cảnh báo thu hồi (NCL-06-CN-004-TC-03):** Khi lô hàng bị thu hồi (`recalled = true`), thông điệp cảnh báo thu hồi hiển thị bằng tiếng Anh và tuyệt đối không bị ẩn hay che khuất.

---

## 3. Danh Sách API Endpoints

### 3.1. Nhóm API Tra Cứu Công Khai (Public Trace APIs)

| Phương thức | Đường dẫn (Path) | Phân quyền | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/public/trace/{codeValue}` | Public (Không đăng nhập) | Tra cứu thông tin hành trình sản phẩm (chế độ đọc, hỗ trợ đa ngôn ngữ) |
| `POST` | `/api/v1/public/trace/{codeValue}/scan` | Public (Không đăng nhập) | Ghi nhận quét tem QR và trả thông tin hành trình công khai |
| `GET` | `/api/v1/public/trace/{codeValue}/certifications` | Public (Không đăng nhập) | Lấy danh sách chứng nhận/tiêu chuẩn công khai gắn với lô |
| `GET` | `/api/v1/public/trace/{codeValue}/inspections` | Public (Không đăng nhập) | Lấy kết quả kiểm nghiệm chất lượng công khai của lô |

### 3.2. Nhóm API Quản Lý Danh Mục Dùng Chung (Master Data APIs)

| Phương thức | Đường dẫn (Path) | Phân quyền | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/product-categories` | `PLATFORM_ADMIN` (`VT-01`) | Thêm mới loại nông sản (hỗ trợ `nameEn`) |
| `PUT` | `/api/v1/product-categories/{id}` | `PLATFORM_ADMIN` (`VT-01`) | Cập nhật loại nông sản (hỗ trợ `nameEn`) |
| `GET` | `/api/v1/product-categories` | Đã đăng nhập (`isAuthenticated`) | Tìm kiếm và lấy danh sách loại nông sản (có `nameEn`) |
| `POST` | `/api/v1/standards` | `PLATFORM_ADMIN` (`VT-01`) | Thêm mới tiêu chuẩn (hỗ trợ `nameEn`) |
| `PUT` | `/api/v1/standards/{standardId}` | `PLATFORM_ADMIN` (`VT-01`) | Cập nhật tiêu chuẩn (hỗ trợ `nameEn`) |
| `GET` | `/api/v1/standards` | Đã đăng nhập (`isAuthenticated`) | Danh sách tiêu chuẩn phân trang (có `nameEn`) |
| `POST` | `/api/v1/inspection-criteria` | `PLATFORM_ADMIN` (`VT-01`) | Thêm mới chỉ tiêu kiểm nghiệm (hỗ trợ `nameEn`) |
| `PUT` | `/api/v1/inspection-criteria/{id}` | `PLATFORM_ADMIN` (`VT-01`) | Cập nhật chỉ tiêu kiểm nghiệm (hỗ trợ `nameEn`) |
| `GET` | `/api/v1/inspection-criteria` | Đã đăng nhập (`isAuthenticated`) | Tìm kiếm chỉ tiêu kiểm nghiệm phân trang (có `nameEn`) |

---

## 4. Chi Tiết API Tra Cứu Công Khai

### 4.1. Tra Cứu Thông Tin Sản Phẩm & Hành Trình
* **Endpoint:** `GET /api/v1/public/trace/{codeValue}`
* **Query Parameters:**
  * `latitude` (Double, optional): Tọa độ vĩ độ người quét.
  * `longitude` (Double, optional): Tọa độ kinh độ người quét.
* **Headers:**
  * `Accept-Language` (String, optional): Ngôn ngữ ưu tiên (`vi` hoặc `en`).

#### Cấu Trúc Response DTO (`PublicTraceResponse`)
```typescript
interface PublicTraceResponse {
  codeValue: string;
  productionLotId: string | null;
  lotName: string | null;           // Tên lô sản xuất gốc (do người dùng nhập)
  lotCode: string | null;
  productName: string;              // Tên loại nông sản tiếng Việt
  productNameEn: string | null;     // Tên loại nông sản tiếng Anh (từ ProductCategory.nameEn)
  shipmentCode: string;             // Tên lô hàng gốc
  shipmentStatus: string;           // DRAFT, CODE_PRINTED, ACTIVATED, RECALLING, RECALLED, SPLIT
  recalled: boolean;
  recallMessage: string | null;     // Thông điệp thu hồi tiếng Việt
  recallMessageEn: string | null;   // Thông điệp thu hồi tiếng Anh chuẩn hóa
  locked: boolean;
  lockReason: string | null;
  lockedAt: string | null;
  verificationNote: string | null;
  unlockedAt: string | null;
  events: PublicChainEventItem[];
  inspections?: PublicInspectionResult[];
}
```

#### Ví Dụ Response (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "codeValue": "HX00000029",
    "productionLotId": "8f0312cb-206e-4401-92ee-90d29ab6e7a2",
    "lotName": "Lô Bưởi Đoan Hùng 2026",
    "lotCode": "8f0312cb-206e-4401-92ee-90d29ab6e7a2",
    "productName": "Bưởi Đoan Hùng",
    "productNameEn": "Doan Hung Pomelo",
    "shipmentCode": "LH-2026-0029",
    "shipmentStatus": "ACTIVATED",
    "recalled": false,
    "recallMessage": null,
    "recallMessageEn": null,
    "locked": false,
    "lockReason": null,
    "lockedAt": null,
    "verificationNote": null,
    "unlockedAt": null,
    "events": [
      {
        "eventType": "HARVEST",
        "eventData": {
          "productionLotName": "Lô Bưởi Đoan Hùng 2026",
          "quantity": 1200,
          "harvestDate": "2026-08-10",
          "earlyHarvest": false
        },
        "recordedAt": "2026-08-10T08:30:00",
        "latitude": 21.584,
        "longitude": 105.184
      }
    ]
  }
}
```

#### Trường Hợp Lô Đang Bị Thu Hồi (TC-03)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "codeValue": "HX00000030",
    "productName": "Thanh long ruột đỏ",
    "productNameEn": "Red Dragon Fruit",
    "shipmentCode": "LH-2026-0030",
    "shipmentStatus": "RECALLED",
    "recalled": true,
    "recallMessage": "Lô hàng này đã bị thu hồi do phát hiện tồn dư thuốc bảo vệ thực vật.",
    "recallMessageEn": "WARNING: This shipment has been recalled due to detected pesticide residue.",
    "events": []
  }
}
```

---

### 4.2. Lấy Danh Sách Chứng Nhận Công Khai
* **Endpoint:** `GET /api/v1/public/trace/{codeValue}/certifications`

#### Cấu Trúc Response DTO (`PublicLotCertificationsResponse` / `PublicCertificationResponse`)
```typescript
interface PublicCertificationResponse {
  certificationId: string;
  certificationName: string;
  certificationNameEn: string | null; // Tên tiếng Anh của tiêu chuẩn/chứng nhận
  certificationCode: string;
  issuedBy: string;
  issueDate: string;
  expiryDate: string;
  verificationStatus: string;
  validityStatus: string;
  publicStatus: string;
  status: string;
  statusLabel: string;
}
```

#### Ví Dụ Response (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "productionLotId": "8f0312cb-206e-4401-92ee-90d29ab6e7a2",
    "lotName": "Lô Bưởi Đoan Hùng 2026",
    "hasCertification": true,
    "certifications": [
      {
        "certificationId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "certificationName": "Tiêu chuẩn VietGAP Trồng trọt",
        "certificationNameEn": "VietGAP Cultivation Standard",
        "certificationCode": "VG-2026-0012",
        "issuedBy": "Trung tâm Chứng nhận Nông nghiệp Phù Đổng",
        "issueDate": "2025-01-01",
        "expiryDate": "2027-01-01",
        "verificationStatus": "VERIFIED",
        "validityStatus": "VALID",
        "publicStatus": "VERIFIED",
        "status": "VALID",
        "statusLabel": "Đã đạt chuẩn"
      }
    ]
  }
}
```

---

### 4.3. Lấy Kết Quả Kiểm Nghiệm Chất Lượng Công Khai
* **Endpoint:** `GET /api/v1/public/trace/{codeValue}/inspections`

#### Cấu Trúc Response DTO (`PublicInspectionCriterionResultDto`)
```typescript
interface PublicInspectionCriterionResultDto {
  id: string;
  criterionName: string;            // Tên chỉ tiêu tiếng Việt
  criterionNameEn: string | null;   // Tên chỉ tiêu tiếng Anh
  standardValue: string;            // Ngưỡng chuẩn tiếng Việt
  standardValueEn: string | null;   // Ngưỡng chuẩn tiếng Anh (nếu có)
  measuredValue: string;
  passed: boolean;
  inspectorName: string | null;
  inspectionDate: string | null;
  expiryDate: string | null;
  laboratoryName: string | null;
}
```

#### Ví Dụ Response (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "productionLotId": "8f0312cb-206e-4401-92ee-90d29ab6e7a2",
    "lotName": "Lô Bưởi Đoan Hùng 2026",
    "hasInspection": true,
    "totalCriteria": 2,
    "passedCriteria": 2,
    "failedCriteriaCount": 0,
    "failedRatio": 0.0,
    "roundCount": 1,
    "inspections": [
      {
        "id": "e4f5a6b7-c8d9-0e1f-2a3b-4c5d6e7f8a9b",
        "criterionName": "Hàm lượng Chì (Pb)",
        "criterionNameEn": "Lead (Pb) Content",
        "standardValue": "QCVN 8-2:2011/BYT (≤ 0.1 mg/kg)",
        "standardValueEn": "QCVN 8-2:2011/BYT (≤ 0.1 mg/kg)",
        "measuredValue": "Đạt chuẩn (Trong ngưỡng an toàn)",
        "passed": true,
        "inspectionDate": "2026-08-05",
        "expiryDate": "2027-08-05",
        "laboratoryName": "Viện Kiểm nghiệm An toàn Vệ sinh Thực phẩm Quốc gia"
      }
    ]
  }
}
```

---

## 5. Chi Tiết API Quản Lý Danh Mục Dùng Chung

### 5.1. Danh Mục Loại Nông Sản (`ProductCategory`)
* **Endpoint Thêm mới:** `POST /api/v1/product-categories`
* **Endpoint Cập nhật:** `PUT /api/v1/product-categories/{id}`
* **Quyền hạn:** `PLATFORM_ADMIN` (VT-01)
* **Payload Request:**
```json
{
  "name": "Bưởi Đoan Hùng",
  "nameEn": "Doan Hung Pomelo",
  "categoryGroup": "Cây ăn quả",
  "description": "Đặc sản bưởi Đoan Hùng, Phú Thọ"
}
```
* **Response DTO (`ProductCategoryResponse`):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "b3e0c7a1-4f2a-4c9d-8e1f-6a7b8c9d0e1f",
    "name": "Bưởi Đoan Hùng",
    "nameEn": "Doan Hung Pomelo",
    "categoryGroup": "Cây ăn quả",
    "description": "Đặc sản bưởi Đoan Hùng, Phú Thọ",
    "isActive": true
  }
}
```

### 5.2. Danh Mục Tiêu Chuẩn Chất Lượng (`Standard`)
* **Endpoint Thêm mới:** `POST /api/v1/standards`
* **Endpoint Cập nhật:** `PUT /api/v1/standards/{standardId}`
* **Quyền hạn:** `PLATFORM_ADMIN` (VT-01)
* **Payload Request:**
```json
{
  "code": "GLOBALGAP",
  "name": "Tiêu chuẩn GlobalGAP",
  "nameEn": "GlobalGAP Standard",
  "description": "Thực hành nông nghiệp tốt toàn cầu"
}
```
* **Response DTO (`StandardResponse`):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "c4d5e6f7-a8b9-0c1d-2e3f-4a5b6c7d8e9f",
    "code": "GLOBALGAP",
    "name": "Tiêu chuẩn GlobalGAP",
    "nameEn": "GlobalGAP Standard",
    "description": "Thực hành nông nghiệp tốt toàn cầu",
    "isActive": true
  }
}
```

### 5.3. Danh Mục Chỉ Tiêu Kiểm Nghiệm (`InspectionCriterionCatalog`)
* **Endpoint Thêm mới:** `POST /api/v1/inspection-criteria`
* **Endpoint Cập nhật:** `PUT /api/v1/inspection-criteria/{id}`
* **Quyền hạn:** `PLATFORM_ADMIN` (VT-01)
* **Payload Request:**
```json
{
  "name": "Hàm lượng Cadmi (Cd)",
  "nameEn": "Cadmium (Cd) Content",
  "code": "CRIT-CD-01",
  "unit": "mg/kg",
  "description": "Chỉ tiêu kim loại nặng Cadmi"
}
```
* **Response DTO (`InspectionCriterionCatalogResponse`):**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 15,
    "name": "Hàm lượng Cadmi (Cd)",
    "nameEn": "Cadmium (Cd) Content",
    "code": "CRIT-CD-01",
    "unit": "mg/kg",
    "description": "Chỉ tiêu kim loại nặng Cadmi",
    "status": "ACTIVE"
  }
}
```

---

## 6. Xử Lý Lỗi Chuẩn Hóa

| HTTP Status | Error Code / Message | Nguyên nhân | Xử lý |
| :--- | :--- | :--- | :--- |
| `400 Bad Request` | `Tên loại nông sản không được để trống.` | Dữ liệu đầu vào thiếu thông tin bắt buộc | Sửa payload hợp lệ |
| `403 Forbidden` | `Chỉ Quản trị viên nền tảng mới có quyền sửa danh mục dùng chung.` | Người dùng không có vai trò VT-01 (QTN-17) | Từ chối thao tác |
| `404 Not Found` | `Mã lô hàng không tồn tại.` | Không tìm thấy mã `codeValue` trên trang tra cứu | Hiển thị thông báo lỗi tra cứu |
| `400 Bad Request` | `Tem chưa có hiệu lực, chưa thể tra cứu hành trình.` | Tem ở trạng thái DRAFT hoặc CODE_PRINTED | Chặn hiển thị chi tiết |
| `400 Bad Request` | `Mã tem này đã được đánh dấu HỦY do sự cố in hỏng/lỗi tem...` | Tem ở trạng thái CANCELLED | Hiển thị màn hình cảnh báo tem hủy |
