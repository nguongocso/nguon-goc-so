📄 **API Docs — Nhập dữ liệu lô hàng loạt từ tệp**

**Tên nhánh:** feature/production-lot-bulk-import

**User Story:** NCL-10-CN-006

**Epic:** NCL-10 — Phân tích chuyên sâu, trải nghiệm di động và quản trị dữ liệu

**Phụ thuộc:** NCL-02-CN-002 (tạo lô sản xuất thủ công — module `farm` đã có)

# 1. Thông tin chung

### Mục tiêu

Cho phép Quản lý hợp tác xã (VT-02) tải lên một tệp bảng tính (.xlsx/.csv) theo mẫu chuẩn để nhập hàng loạt danh sách lô sản xuất và nhật ký canh tác kèm theo, giúp rút ngắn thời gian đưa dữ liệu sẵn có (đã ghi chép ngoài hệ thống) vào nền tảng, thay vì phải tạo từng lô thủ công.

### Yêu cầu nghiệp vụ

- Quản lý hợp tác xã tải tệp theo mẫu chuẩn (template) đã quy định sẵn cột bắt buộc và cột tùy chọn.
- Hệ thống kiểm tra tệp có đủ các cột bắt buộc theo mẫu trước khi xử lý bất kỳ dòng nào; nếu thiếu cột bắt buộc, hệ thống từ chối toàn bộ tệp và yêu cầu nhập lại đúng mẫu.
- Nếu tệp đủ cột, hệ thống kiểm tra hợp lệ theo từng dòng (định dạng ngày, số, mã vùng trồng/loại nông sản có tồn tại trong tổ chức...); dòng không hợp lệ bị bỏ qua kèm lý do, các dòng còn lại vẫn được xử lý bình thường.
- Chỉ các dòng hợp lệ mới được lưu thành `ProductionLot` (và `FarmLog` nếu tệp có kèm dữ liệu nhật ký canh tác); dòng lỗi không được lưu.
- Sau khi xử lý xong, người dùng nhận báo cáo tổng hợp gồm số dòng đạt, số dòng lỗi và lý do lỗi theo từng dòng.
- Dữ liệu nhập chỉ áp dụng cho tổ chức của người thực hiện; Quản trị viên nền tảng (VT-01) có thể chỉ định nhập cho tổ chức khác.
- Mỗi lượt nhập tệp được ghi lại thành lịch sử, bao gồm tổng số dòng và số dòng đạt, phục vụ đối soát và theo dõi sau này.
- Đây là thao tác **ghi dữ liệu hàng loạt** (không phải read-only), mỗi dòng hợp lệ được lưu độc lập; lỗi ở một dòng không làm rollback các dòng hợp lệ khác trong cùng tệp.

# 2. Vị trí làm việc tại cây thư mục Backend

```
src/main/java/vn/nguongocso/
└── farm/                              # Package đã có từ NCL-02-CN-002
    ├── controller/
    │   └── ProductionLotController.java        # Sửa đổi - bổ sung endpoint import
    ├── dto/
    │   ├── request/
    │   │   └── ProductionLotImportRequest.java # 📁 Tạo mới
    │   └── response/
    │       ├── ProductionLotImportResultResponse.java  # 📁 Tạo mới
    │       └── ProductionLotImportRowError.java         # 📁 Tạo mới
    ├── entity/
    │   └── ProductionLotImportHistory.java     # 📁 Tạo mới
    ├── repository/
    │   ├── ProductionLotRepository.java        # Sửa đổi - bổ sung saveAll theo lô
    │   └── ProductionLotImportHistoryRepository.java   # 📁 Tạo mới
    ├── service/
    │   ├── ProductionLotImportService.java     # 📁 Tạo mới
    │   └── impl/
    │       └── ProductionLotImportServiceImpl.java     # 📁 Tạo mới
    └── util/
        └── ProductionLotImportFileParser.java  # 📁 Tạo mới - đọc & validate tệp (Apache POI)
```

Lưu ý: Story này không tạo module mới hoàn toàn. Chức năng nhập hàng loạt được phát triển mở rộng trên module `farm` đã có từ NCL-02-CN-002, tái sử dụng entity `ProductionLot`, `FarmLog`, `FarmArea`, `ProductCategory` sẵn có; chỉ bổ sung thêm entity lịch sử nhập (`ProductionLotImportHistory`) và các lớp phục vụ đọc/validate tệp.

# 3. Cơ sở dữ liệu

Có **một migration mới**: tạo bảng `production_lot_import_history` để phục vụ TC-04 (lưu lịch sử nhập). Không tạo bảng lưu chi tiết từng dòng lỗi — báo cáo dòng lỗi chỉ trả về trong response của API, không lưu xuống cơ sở dữ liệu.

**Bảng `production_lot_import_history` (mới)**

- `id` (PK, UUID)
- `organization_id` (FK → Organization.id) — tổ chức được nhập dữ liệu
- `imported_by` (FK → User.id) — người thực hiện nhập
- `file_name` (varchar) — tên tệp đã tải lên
- `total_rows` (int) — tổng số dòng dữ liệu trong tệp (không tính dòng tiêu đề)
- `success_count` (int) — số dòng được lưu thành công
- `failed_count` (int) — số dòng bị bỏ qua do lỗi
- `status` (enum: SUCCESS, PARTIAL_SUCCESS, FAILED) — SUCCESS khi tất cả dòng đạt, PARTIAL_SUCCESS khi có cả dòng đạt và dòng lỗi, FAILED khi tệp bị từ chối toàn bộ (thiếu cột bắt buộc)
- `imported_at` (timestamp)

**Các bảng tái sử dụng (không đổi cấu trúc):**

- `ProductionLot` — mỗi dòng hợp lệ tạo một bản ghi mới, `status` mặc định `DRAFT`.
- `FarmLog` — tạo kèm theo nếu dòng có dữ liệu nhật ký canh tác (hoạt động, vật tư, ngày thực hiện).
- `FarmArea`, `ProductCategory` — dùng để đối chiếu mã vùng trồng / mã loại nông sản khai báo trong tệp với dữ liệu đã có của tổ chức.

# 4. API Endpoint

## 4.1. Nhập dữ liệu lô sản xuất từ tệp

**Method:** POST

**Endpoint:**

```
POST /api/v1/production-lots/import
```

**Content-Type:** `multipart/form-data`

**Quyền:** `production_lot.CREATE`

**Request Parameters (multipart/form-data)**

| Parameter | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| file | File (.xlsx hoặc .csv) | Có | Tệp dữ liệu lô sản xuất theo mẫu chuẩn. |
| organizationId | UUID | Không | Chỉ áp dụng cho Quản trị viên nền tảng (VT-01) khi nhập hộ tổ chức khác; người dùng thông thường mặc định dùng tổ chức trong phiên đăng nhập. |

**Cột trong mẫu tệp (template)**

| Cột | Bắt buộc | Kiểu / Định dạng | Mô tả |
|---|---|---|---|
| ten_lo | Có | String | Tên lô sản xuất. |
| ma_loai_nong_san | Có | String | Mã loại nông sản, đối chiếu với `ProductCategory`. |
| ma_vung_trong | Không | String | Mã vùng trồng, đối chiếu với `FarmArea` của tổ chức. |
| san_luong_du_kien | Có | Số thực | Sản lượng dự kiến (`expected_quantity`). |
| san_luong_thuc_thu | Không | Số thực | Sản lượng thực thu (`actual_quantity`), nếu đã thu hoạch. |
| ngay_gieo_trong | Có | dd/MM/yyyy | Ngày gieo trồng (`planting_date`). |
| ngay_thu_hoach | Không | dd/MM/yyyy | Ngày thu hoạch (`harvest_date`), nếu đã có. |
| hoat_dong_canh_tac | Không | String (enum FarmLog.activity_type) | Loại hoạt động canh tác, nếu muốn nhập kèm nhật ký. |
| vat_tu_va_so_luong | Không | String | Vật tư sử dụng và số lượng (ví dụ: `Phân NPK - 20kg`). |
| ngay_thuc_hien | Không | dd/MM/yyyy | Ngày thực hiện hoạt động canh tác. |
| ghi_chu | Không | String | Ghi chú thêm cho dòng dữ liệu. |

**Ví dụ request**

```
POST /api/v1/production-lots/import
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="lo_san_xuat_2026.xlsx"
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

(binary content)
------WebKitFormBoundary--
```

**Response 200 OK — Toàn bộ dòng hợp lệ (TC-01)**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "importHistoryId": "b6f2a1d0-1234-4a6e-9c11-7d0a2f9b0001",
    "status": "SUCCESS",
    "fileName": "lo_san_xuat_2026.xlsx",
    "totalRows": 20,
    "successCount": 20,
    "failedCount": 0,
    "savedLotIds": [
      "3ac68afc-9d0e-4b2a-8e21-1a2b3c4d5e6f",
      "9b2d5e10-2a3b-4c5d-8e6f-7a8b9c0d1e2f"
    ],
    "errors": []
  },
  "timestamp": "2026-08-02T09:00:00Z"
}
```

**Response 200 OK — Một phần dòng lỗi, các dòng còn lại vẫn được lưu (TC-02)**

```json
{
  "success": true,
  "status": 200,
  "data": {
    "importHistoryId": "b6f2a1d0-1234-4a6e-9c11-7d0a2f9b0002",
    "status": "PARTIAL_SUCCESS",
    "fileName": "lo_san_xuat_2026.xlsx",
    "totalRows": 20,
    "successCount": 17,
    "failedCount": 3,
    "savedLotIds": [
      "3ac68afc-9d0e-4b2a-8e21-1a2b3c4d5e6f"
    ],
    "errors": [
      { "rowNumber": 4, "reason": "Ngày gieo trồng không đúng định dạng dd/MM/yyyy." },
      { "rowNumber": 9, "reason": "Mã loại nông sản không tồn tại trong hệ thống." },
      { "rowNumber": 15, "reason": "Sản lượng dự kiến không phải là số hợp lệ." }
    ]
  },
  "timestamp": "2026-08-02T09:05:00Z"
}
```

**Response 400 Bad Request — Thiếu cột bắt buộc (TC-03)**

```json
{
  "success": false,
  "status": 400,
  "message": "Tệp không đúng mẫu: thiếu cột bắt buộc 'ngay_gieo_trong'. Vui lòng tải lại theo đúng mẫu."
}
```

**Response 400 Bad Request — Tệp rỗng hoặc sai định dạng**

```json
{
  "success": false,
  "status": 400,
  "message": "Tệp không hợp lệ hoặc không có dữ liệu. Chỉ hỗ trợ định dạng .xlsx hoặc .csv."
}
```

**Response 403 Forbidden — Sai vai trò**

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền nhập dữ liệu lô sản xuất."
}
```

**Response 403 Forbidden — Không thuộc tổ chức được chỉ định**

```json
{
  "success": false,
  "status": 403,
  "message": "Bạn không thuộc tổ chức này nên không thể nhập dữ liệu."
}
```

# 5. Business Rules

- **4.1. Kiểm tra Role** — Chỉ Quản lý hợp tác xã (VT-02) mới được phép nhập dữ liệu cho tổ chức của mình; Quản trị viên nền tảng (VT-01) được nhập hộ cho tổ chức bất kỳ qua `organizationId`. Đây là bước kiểm tra đầu tiên, trước khi đọc nội dung tệp. Nếu sai vai trò → trả **403 Forbidden** (case sai vai trò).
- **4.2. Kiểm tra tổ chức** — Nếu người dùng không phải VT-01 mà truyền `organizationId` khác tổ chức của mình, hoặc VT-01 chỉ định một tổ chức không tồn tại → trả **403 Forbidden** riêng biệt (org-mismatch), khác với case sai vai trò ở bước 4.1.
- **4.3. Kiểm tra định dạng và nội dung tệp** — Tệp phải là `.xlsx` hoặc `.csv`, không rỗng. Nếu sai định dạng hoặc không có dữ liệu → trả **400 Bad Request**.
- **4.4. Kiểm tra cột bắt buộc** — Trước khi xử lý bất kỳ dòng nào, hệ thống kiểm tra tệp có đủ các cột bắt buộc theo mẫu (`ten_lo`, `ma_loai_nong_san`, `san_luong_du_kien`, `ngay_gieo_trong`). Nếu thiếu **bất kỳ** cột bắt buộc nào → từ chối **toàn bộ tệp**, trả **400 Bad Request**, không lưu bất kỳ dòng dữ liệu nào, không tạo lịch sử nhập thành công (ghi nhận `status = FAILED` nếu có lưu lịch sử).
- **4.5. Kiểm tra hợp lệ theo từng dòng** — Với tệp đã đủ cột, hệ thống duyệt từng dòng và kiểm tra: định dạng ngày (`dd/MM/yyyy`), định dạng số (sản lượng), sự tồn tại của mã loại nông sản/mã vùng trồng trong phạm vi tổ chức. Dòng không hợp lệ bị **bỏ qua**, ghi nhận số dòng (`rowNumber`) và lý do lỗi (`reason`), việc xử lý tiếp tục với các dòng còn lại.
- **4.6. Lưu dòng hợp lệ** — Mỗi dòng hợp lệ được lưu thành một bản ghi `ProductionLot` mới (`status = DRAFT`); nếu dòng có dữ liệu nhật ký canh tác (hoạt động, vật tư, ngày thực hiện), hệ thống tạo kèm một bản ghi `FarmLog` liên kết với lô vừa tạo.
- **4.7. Lưu lịch sử nhập** — Sau khi xử lý xong toàn bộ tệp, hệ thống lưu một bản ghi `ProductionLotImportHistory` gồm tổng số dòng, số dòng đạt, số dòng lỗi và trạng thái tổng thể (`SUCCESS`, `PARTIAL_SUCCESS` hoặc `FAILED`).
- **4.8. Trả kết quả** — API luôn trả về báo cáo tổng hợp gồm danh sách ID các lô đã lưu (`savedLotIds`) và danh sách dòng lỗi kèm lý do (`errors`), kể cả khi không có dòng nào lỗi.
- Mỗi dòng hợp lệ được lưu **độc lập**; lỗi ở một dòng không rollback các dòng hợp lệ khác trong cùng lần nhập.
- Mọi lượt nhập tệp (thành công toàn phần, một phần hay bị từ chối) đều được ghi nhận qua `ActivityLogService` với `action = "IMPORT_PRODUCTION_LOTS"`.

**Quyền yêu cầu:** `production_lot.CREATE`

- Quyền được kiểm tra thông qua:

```java
permissionChecker.check("production_lot", "CREATE");
```

- Nếu người dùng không có quyền hoặc sai vai trò, hệ thống:
  - ghi nhận log truy cập thất bại qua `ActivityLogService`;
  - trả về **HTTP 403 Forbidden**.
- Nếu người dùng có quyền, hệ thống tiếp tục kiểm tra tổ chức, định dạng tệp và cột bắt buộc theo các bước 4.2–4.4 trước khi xử lý từng dòng.

# 6. Repository Methods

### ProductionLotImportHistoryRepository (mới)

```java
ProductionLotImportHistory save(ProductionLotImportHistory history);

List<ProductionLotImportHistory> findByOrganizationIdOrderByImportedAtDesc(
    @Param("organizationId") UUID organizationId
);
```

### ProductionLotRepository (bổ sung)

```java
List<ProductionLot> saveAll(Iterable<ProductionLot> lots);
```

### FarmAreaRepository / ProductCategoryRepository (đã có, tái sử dụng)

```java
Optional<FarmArea> findByCodeAndOrganizationId(
    @Param("code") String code, @Param("organizationId") UUID organizationId
);

Optional<ProductCategory> findByCode(@Param("code") String code);
```

Service đọc tệp bằng `ProductionLotImportFileParser` (dùng Apache POI cho `.xlsx`, thư viện CSV cho `.csv`), kiểm tra đủ cột bắt buộc trước, sau đó duyệt từng dòng: đối chiếu `ma_loai_nong_san`/`ma_vung_trong` qua các repository trên, validate định dạng ngày/số, gom các dòng hợp lệ để `saveAll` theo lô, gom dòng lỗi để trả về trong response, và lưu `ProductionLotImportHistory` khi kết thúc.

# 7. DTOs

### ProductionLotImportRequest

```java
package vn.nguongocso.farm.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Tham số đi kèm khi nhập dữ liệu lô sản xuất từ tệp.
 * file được nhận riêng qua @RequestPart, các trường còn lại qua @RequestParam.
 */
@Getter
@Setter
public class ProductionLotImportRequest {

    /**
     * Tệp dữ liệu (.xlsx hoặc .csv).
     */
    private MultipartFile file;

    /**
     * Tổ chức được nhập hộ — chỉ áp dụng cho VT-01.
     */
    private UUID organizationId;
}
```

### ProductionLotImportRowError

```java
package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Thông tin lỗi của một dòng dữ liệu khi nhập tệp.
 */
@Getter
@Builder
public class ProductionLotImportRowError {

    private Integer rowNumber; // Số dòng trong tệp xảy ra lỗi.

    private String reason; // Lý do dòng dữ liệu không hợp lệ.

}
```

### ProductionLotImportResultResponse

```java
package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kết quả tổng hợp sau khi nhập dữ liệu lô sản xuất từ tệp.
 */
package vn.nguongocso.farm.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả sau khi hoàn tất nhập dữ liệu lô sản xuất.
 */
@Getter
@Builder
public class ProductionLotImportResultResponse {

    private UUID importHistoryId; // ID lịch sử nhập dữ liệu.

    private String status; // Trạng thái: SUCCESS, PARTIAL_SUCCESS hoặc FAILED.

    private String fileName; // Tên tệp đã nhập.

    private Integer totalRows; // Tổng số dòng dữ liệu.

    private Integer successCount; // Số dòng được lưu thành công.

    private Integer failedCount; // Số dòng bị lỗi.

    private List<UUID> savedLotIds; // Danh sách ID các lô đã được tạo.

    private List<ProductionLotImportRowError> errors; // Danh sách dòng lỗi.

    private Instant importedAt; // Thời điểm hoàn tất nhập dữ liệu.

}
```

# 8. Ghi chú Frontend

- Cung cấp nút "Tải mẫu tệp" để người dùng tải về tệp `.xlsx` chuẩn với đầy đủ cột bắt buộc/tùy chọn và dữ liệu ví dụ, tránh sai định dạng ngay từ đầu.
- Sau khi tải tệp lên, hiển thị trạng thái xử lý (spinner/progress) vì tệp có thể chứa nhiều dòng.
- Khi kết quả trả về `PARTIAL_SUCCESS`, hiển thị rõ số dòng đạt/số dòng lỗi, kèm bảng chi tiết `errors` (số dòng + lý do) để người dùng sửa và nhập lại các dòng lỗi.
- Khi API trả **400 Bad Request** do thiếu cột bắt buộc (TC-03), hiển thị rõ tên cột còn thiếu và gợi ý tải lại mẫu tệp chuẩn, không cho phép nhập lại cho đến khi đúng mẫu.
- Khi trạng thái là `SUCCESS`, hiển thị thông báo hoàn tất kèm số lô đã tạo, có thể điều hướng sang danh sách lô sản xuất.
- Hiển thị màn hình "Lịch sử nhập dữ liệu" liệt kê các lượt nhập gần đây (từ `ProductionLotImportHistory`) kèm tổng số dòng, số dòng đạt và thời điểm nhập.

# 9. Kế hoạch triển khai (Backend)

| Công việc | Package | File |
|---|---|---|
| Tạo entity lịch sử nhập | farm.entity | ProductionLotImportHistory (mới) + migration |
| Tạo Repository lịch sử nhập | farm.repository | ProductionLotImportHistoryRepository (mới) |
| Tạo bộ đọc & validate tệp | farm.util | ProductionLotImportFileParser (mới) |
| Tạo Request/Response DTO | farm.dto | ProductionLotImportRequest, ProductionLotImportResultResponse, ProductionLotImportRowError |
| Bổ sung logic nhập: kiểm tra role/tổ chức/cột bắt buộc, validate từng dòng, lưu lô + nhật ký, lưu lịch sử | farm.service.impl | ProductionLotImportServiceImpl (mới) |
| Thêm endpoint POST import | farm.controller | ProductionLotController.java (sửa) |
| Viết test case TC-01, TC-02, TC-03, TC-04 | farm (test) | ProductionLotControllerTest.java |

### Ánh xạ Test Case ↔ Xử lý backend

| Test Case | Kịch bản | Xử lý tương ứng |
|---|---|---|
| NCL-10-CN-006-TC-01 | Tệp đúng mẫu, có dòng hợp lệ | Trả 200, `status = SUCCESS`, tất cả dòng được lưu thành `ProductionLot`, `errors = []` |
| NCL-10-CN-006-TC-02 | Một số dòng sai định dạng | Trả 200, `status = PARTIAL_SUCCESS`, các dòng hợp lệ vẫn được lưu, dòng lỗi liệt kê kèm lý do trong `errors` |
| NCL-10-CN-006-TC-03 | Tệp thiếu cột bắt buộc | Trả 400 Bad Request, từ chối toàn bộ tệp, không lưu dòng nào, yêu cầu đúng mẫu |
| NCL-10-CN-006-TC-04 | Nhập tệp hoàn tất | Lưu bản ghi `ProductionLotImportHistory` với tổng số dòng và số dòng đạt, bất kể kết quả là toàn phần hay một phần |
