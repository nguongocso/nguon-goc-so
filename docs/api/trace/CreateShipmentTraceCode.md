# API: Tạo lô hàng và sinh mã truy xuất

*NCL-04-CN-002 — Epic NCL-04: Cấp mã truy xuất và kiểm soát tem*

## 1. Thông tin chung

**Mục tiêu**

Cho phép Quản lý hợp tác xã tạo Lô hàng (Shipment) từ một Lô sản xuất (ProductionLot) đã đóng gói, đồng thời sinh các mã truy xuất (TraceCode) duy nhất cho lô hàng đó, trong phạm vi hạn mức dải mã (CodeRange) đã được cấp cho tổ chức.

**Nhật ký này phục vụ:**

- Chống nhân bản tem (mỗi mã truy xuất là duy nhất trên toàn hệ thống).
- Kiểm soát số lượng tem được sinh không vượt quá sản lượng/hạn mức đã cấp cho tổ chức.
- Chuẩn bị dữ liệu (mã QR) phục vụ tra cứu công khai nguồn gốc lô hàng ở các chức năng tiếp theo.

## 2. Endpoint

**POST /api/v1/shipments**

Request Body

```json
{
  "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
  "name": "Lô hàng chè Long Cốc T7/2026",
  "totalQuantity": 200,
  "packagingInfo": "Túi 500g, đóng thùng 20 túi/thùng"
}
```

**Ghi chú tham số**

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| productionLotId | UUID | Có | Lô sản xuất nguồn, phải ở trạng thái PACKAGED. |
| name | string | Có | Tên/định danh hiển thị của lô hàng. |
| totalQuantity | number | Có | Số lượng đơn vị sản phẩm cần gắn mã (đồng thời là số mã truy xuất sẽ được sinh). |
| packagingInfo | string | Không | Thông tin quy cách đóng gói. |

Ví dụ đường dẫn truy xuất chi tiết lô hàng sau khi tạo:

```
GET http://localhost:8080/files/qr/{organizationId}/{productionLotId}/{shipmentId}/{codeValue}.png
```

## 3. Điều kiện

**Người dùng phải:**

- Đăng nhập thành công.
- Có role VT-02 (Quản lý hợp tác xã / Org Manager).
- Thuộc cùng Organization với ProductionLot được chọn.

**Lô sản xuất phải:**

- Đang ở trạng thái PACKAGED (đã đóng gói).

**Tổ chức phải:**

- Có ít nhất một CodeRange (dải mã) còn hạn mức đủ cho totalQuantity yêu cầu (total_limit − used_count ≥ totalQuantity).

## 4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong `ShipmentServiceImpl.createShipment()`.

### 4.1 Kiểm tra Role

Chỉ người dùng có role VT-02 (Quản lý hợp tác xã) được phép tạo lô hàng và sinh mã. Đây là bước kiểm tra đầu tiên, thực hiện trước khi tìm ProductionLot.

Nếu không đúng role:

```
"Bạn không có quyền tạo lô hàng và sinh mã truy xuất."
```

### 4.2 Kiểm tra tồn tại ProductionLot

Nếu productionLotId không tồn tại, hệ thống ném BusinessException:

```
"Không tìm thấy lô sản xuất"
```

### 4.3 Kiểm tra Organization

Organization của người đăng nhập phải trùng Organization của ProductionLot (thông qua ProductionLot → FarmArea → Organization).

Nếu không:

```
"Bạn không thuộc tổ chức của lô sản xuất."
```

### 4.4 Kiểm tra trạng thái ProductionLot (TC-02)

Lô sản xuất phải ở trạng thái PACKAGED. Nếu lô đang ở trạng thái khác (DRAFT, PENDING, APPROVED, HARVESTED, CLOSED), hệ thống chặn thao tác và báo lỗi sai trạng thái:

```
"Lô sản xuất chưa được đóng gói, không thể tạo lô hàng."
```

### 4.5 Kiểm tra hạn mức dải mã (TC-03)

Hệ thống tìm CodeRange đang hoạt động (active) thuộc Organization, khóa dòng (pessimistic lock) để tránh tranh chấp khi nhiều yêu cầu chạy song song, rồi kiểm tra:

```
total_limit - used_count >= totalQuantity
```

Nếu số lượng yêu cầu vượt hạn mức còn lại, hệ thống chặn sinh mã và báo lỗi:

```
"Số lượng vượt quá hạn mức dải mã còn lại."
```

### 4.6 Tạo Lô hàng (Shipment)

Hệ thống tạo bản ghi Shipment với trạng thái ban đầu DRAFT, liên kết đến ProductionLot và Organization.

### 4.7 Sinh mã truy xuất (TraceCode) (TC-01, TC-04)

Với mỗi đơn vị trong totalQuantity, hệ thống sinh một mã truy xuất duy nhất theo quy tắc:

```
code_value = CodeRange.prefix + số_thứ_tự_kế_tiếp (from CodeRange.used_count)
```

Trước khi lưu, hệ thống kiểm tra mã vừa sinh chưa tồn tại trong bảng TraceCode (kết hợp ràng buộc UNIQUE ở tầng cơ sở dữ liệu làm lớp bảo vệ cuối). Nếu phát hiện trùng (do va chạm dữ liệu), hệ thống tự sinh lại một mã khác và ghi log cảnh báo trùng mã, không dừng toàn bộ tiến trình:

```
"Phát hiện mã trùng, hệ thống đã tự động sinh lại mã mới."
```

Sau khi có code_value hợp lệ, hệ thống sinh ảnh mã phản hồi nhanh (QR) tương ứng và lưu đường dẫn ảnh (qr_image), trạng thái tem ban đầu là INACTIVE (chưa kích hoạt — việc kích hoạt tem thuộc phạm vi story khác).

### 4.8 Cập nhật hạn mức đã dùng

Sau khi sinh mã thành công cho toàn bộ totalQuantity, hệ thống cập nhật `CodeRange.used_count += totalQuantity` trong cùng giao dịch (transaction) với bước 4.6–4.7 để đảm bảo tính toàn vẹn.

### 4.9 Cập nhật trạng thái Lô hàng

Shipment chuyển trạng thái từ DRAFT sang CODEPRINTED (đã sinh mã, chưa kích hoạt).

## 5. Response DTO

```java
public class ShipmentResponse {
    private UUID id;
    private UUID productionLotId;
    private String productionLotName;
    private String name;
    private Double totalQuantity;
    private String packagingInfo;
    private ShipmentStatus status;
    private List<TraceCodeResponse> traceCodes;
    private String createdByName;
    private LocalDateTime createdAt;
}

public class TraceCodeResponse {
    private UUID id;
    private String codeValue;
    private String qrImage;
    private TraceCodeStatus status;
}
```

Ghi chú: dữ liệu Shipment và danh sách TraceCode được map ở tầng service sau khi giao dịch tạo lô hàng và sinh mã hoàn tất; không trả về entity trực tiếp.

## 6. Response

Ví dụ request

```
POST http://localhost:8080/api/v1/shipments
Content-Type: application/json
```

HTTP 201 Created
```json
{
  "productionLotId": "f86a4deb-0ba3-4f36-9f52-7c8de90a854b",
  "name": "Lô hàng chè Long Cốc T7/2026",
  "totalQuantity": 2,
  "packagingInfo": "Túi 500g, đóng thùng 20 túi/thùng"
}
```

Response

```json
{
    "success": true,
    "status": 200,
    "data": {
        "id": "ecde1d21-18e3-437a-9695-ffb4d2a4c23a",
        "productionLotId": "f86a4deb-0ba3-4f36-9f52-7c8de90a854b",
        "productionLotName": "Lô chè xuân 2026",
        "name": "Lô hàng chè Long Cốc T7/2026",
        "totalQuantity": 2,
        "packagingInfo": "Túi 500g, đóng thùng 20 túi/thùng",
        "status": "CODE_PRINTED",
        "traceCodes": [
            {
                "id": "0007084a-a41e-4c44-a958-23e8a233ab92",
                "codeValue": "HX00000029",
                "qrImage": "/files/qr/4d2f480f-5e0f-4530-b325-2506cb41ebf8/f86a4deb-0ba3-4f36-9f52-7c8de90a854b/ecde1d21-18e3-437a-9695-ffb4d2a4c23a/HX00000029.png",
                "status": "INACTIVE"
            },
            {
                "id": "2a34e24d-e0ed-4ce7-984e-b4bd4ec77ae4",
                "codeValue": "HX00000030",
                "qrImage": "/files/qr/4d2f480f-5e0f-4530-b325-2506cb41ebf8/f86a4deb-0ba3-4f36-9f52-7c8de90a854b/ecde1d21-18e3-437a-9695-ffb4d2a4c23a/HX00000030.png",
                "status": "INACTIVE"
            }
        ],
        "createdByName": "Trần Trọng Nghĩa",
        "createdAt": "2026-07-24T08:27:31.9445502"
    },
    "timestamp": "2026-07-24T01:27:31.999946500Z"
}
```

Ghi chú: mảng traceCodes ở ví dụ trên chỉ minh hoạ 2/200 bản ghi để rút gọn nội dung.

## 7. Error Response

**400 Bad Request**

Trường hợp thiếu trường bắt buộc (productionLotId, name, totalQuantity):

```json
{
    "success": false,
    "status": 400,
    "message": "Vui lòng nhập đầy đủ thông tin lô hàng."
}
```

**403 Forbidden**

Trường hợp sai role (không phải VT-02):

```json
{
    "success": false,
    "status": 403,
    "message": "Bạn không có quyền tạo lô hàng và sinh mã truy xuất."
}
```

Hoặc trường hợp khác Organization:

```json
{
    "success": false,
    "status": 403,
    "message": "Bạn không thuộc tổ chức của lô sản xuất."
}
```

**404 Not Found**

```json
{
    "success": false,
    "status": 404,
    "message": "Không tìm thấy lô sản xuất"
}
```

**409 Conflict — sai trạng thái (TC-02)**

```json
{
    "success": false,
    "status": 409,
    "message": "Lô sản xuất chưa được đóng gói, không thể tạo lô hàng."
}
```

**409 Conflict — vượt hạn mức dải mã (TC-03)**

```json
{
    "success": false,
    "status": 409,
    "message": "Số lượng vượt quá hạn mức dải mã còn lại."
}
```

## 8. Backend xử lý

```
Client
    │
    ▼
POST /api/v1/shipments  { productionLotId, name, totalQuantity, packagingInfo }
    │
    ▼
Lấy currentUser (SecurityContext)
    │
    ▼
Kiểm tra Role (VT-02)                       -> 403 nếu sai
    │
    ▼
Tìm ProductionLot                            -> 404 nếu không có
    │
    ▼
Kiểm tra Organization                        -> 403 nếu khác tổ chức
    │
    ▼
Kiểm tra trạng thái = PACKAGED                -> 409 nếu sai trạng thái
    │
    ▼
Khóa CodeRange active của Organization (FOR UPDATE)
    │
    ▼
Kiểm tra hạn mức: total_limit - used_count >= totalQuantity  -> 409 nếu vượt hạn mức
    │
    ▼
Tạo Shipment (status = DRAFT)
    │
    ▼
Vòng lặp sinh TraceCode (x totalQuantity):
    │  ├─ Sinh code_value kế tiếp từ CodeRange
    │  ├─ Kiểm tra trùng (existsByCodeValue) -> nếu trùng, sinh lại + log cảnh báo
    │  └─ Sinh ảnh QR, lưu TraceCode (status = INACTIVE)
    ▼
Cập nhật CodeRange.used_count += totalQuantity
    │
    ▼
Cập nhật Shipment.status = CODEPRINTED
    │
    ▼
Map sang ShipmentResponse & Trả Response (201)
```

## 9. Repository

**ProductionLotRepository**

```java
public interface ProductionLotRepository extends JpaRepository<ProductionLot, UUID> {
    Optional<ProductionLot> findById(UUID id);
}
```

**CodeRangeRepository**

Sử dụng khóa bi quan (pessimistic lock) trên dòng CodeRange đang active của tổ chức để tránh nhiều request tạo lô hàng đồng thời cùng vượt hạn mức (race condition trên used_count).

```java
public interface CodeRangeRepository extends JpaRepository<CodeRange, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM CodeRange cr WHERE cr.organization.id = :organizationId " +
           "AND cr.usedCount < cr.totalLimit ORDER BY cr.createdAt ASC")
    Optional<CodeRange> findActiveForUpdate(UUID organizationId);
}
```

**TraceCodeRepository**

```java
public interface TraceCodeRepository extends JpaRepository<TraceCode, UUID> {

    boolean existsByCodeValue(String codeValue);

    List<TraceCode> findByShipmentId(UUID shipmentId);
}
```

Ghi chú: cột code_value trên bảng TraceCode có ràng buộc UNIQUE ở tầng cơ sở dữ liệu; existsByCodeValue là lớp kiểm tra ở tầng ứng dụng trước khi insert, ràng buộc UNIQUE là lớp bảo vệ cuối cùng chống trùng khi có tranh chấp đồng thời.

## 10. Phạm vi của Story

**Bao gồm**

- Tạo lô hàng (Shipment) từ một lô sản xuất đã đóng gói (PACKAGED).
- Kiểm tra Role (VT-02 – Quản lý hợp tác xã) và Organization.
- Kiểm tra trạng thái lô sản xuất và hạn mức dải mã còn lại của tổ chức.
- Sinh mã truy xuất duy nhất (TraceCode) cho từng đơn vị trong lô hàng, trong hạn mức dải mã.
- Sinh và lưu ảnh mã phản hồi nhanh (QR) tương ứng cho mỗi mã truy xuất.
- Cập nhật hạn mức đã sử dụng của dải mã (CodeRange.used_count).

**Không bao gồm**

- Cấp dải mã (CodeRange) mới cho tổ chức — thuộc chức năng khác (NCL-04-CN-001).
- Kích hoạt tem (TraceCode.status → ACTIVE).
- Thu hồi tem (TraceCode/Shipment.status → RECALLED).
- Tra cứu công khai theo mã truy xuất.
- Đóng gói lô sản xuất (chuyển ProductionLot sang trạng thái PACKAGED) — thuộc chức năng khác.

## 11. User Story liên quan

**NCL-04-CN-002 — Tạo lô hàng và sinh mã truy xuất**

Là Quản lý hợp tác xã, tôi muốn tạo lô hàng từ lô sản xuất đã đóng gói và sinh mã truy xuất duy nhất, để mỗi lô hàng có mã phản hồi nhanh riêng.

*Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên bốn | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-02*

## 12. Danh sách công việc

*Chu kỳ áp dụng: Chu kỳ số hai.*

| Mã công việc | Tên công việc | Loại | Phụ trách | Trạng thái |
|---|---|---|---|---|
| NCL-04-CN-002-CV-01 | Làm rõ quy tắc sinh mã duy nhất | Phân tích nghiệp vụ | Thành viên một | Chưa thực hiện |
| NCL-04-CN-002-CV-02 | Thiết kế dữ liệu lô hàng và tem | Thiết kế dữ liệu | Thành viên bốn | Chưa thực hiện |
| NCL-04-CN-002-CV-03 | Phát triển sinh mã truy xuất | Phát triển phần máy chủ | Thành viên bốn | Chưa thực hiện |
| NCL-04-CN-002-CV-04 | Tạo và hiển thị mã phản hồi nhanh | Phát triển phần giao diện | Thành viên hai | Chưa thực hiện |
| NCL-04-CN-002-CV-05 | Kiểm tra tính duy nhất của mã | Kiểm thử | Thành viên năm | Chưa thực hiện |

## 13. Test Cases

### TC-01: Luồng thành công

| Mục | Nội dung |
|---|---|
| Điều kiện đầu vào | Lô sản xuất ở trạng thái PACKAGED, dải mã còn hạn mức đủ cho totalQuantity. |
| Hành động | Quản lý hợp tác xã gửi yêu cầu tạo lô hàng với productionLotId, name, totalQuantity hợp lệ. |
| Kết quả mong đợi | Lô hàng được tạo với trạng thái CODEPRINTED, danh sách mã truy xuất duy nhất được sinh đủ số lượng, mỗi mã có ảnh QR tương ứng. |
| Dữ liệu liên quan | ProductionLot, totalQuantity, CodeRange. |
| Mức độ ưu tiên | Cao |

### TC-02: Sai trạng thái

| Mục | Nội dung |
|---|---|
| Điều kiện đầu vào | Lô sản xuất chưa đóng gói (trạng thái khác PACKAGED, ví dụ HARVESTED). |
| Hành động | Quản lý hợp tác xã yêu cầu tạo lô hàng và sinh mã cho lô sản xuất này. |
| Kết quả mong đợi | Hệ thống chặn thao tác và trả lỗi 409 kèm thông báo sai trạng thái; không tạo Shipment, không sinh mã, không thay đổi used_count. |
| Dữ liệu liên quan | Trạng thái lô sản xuất. |
| Mức độ ưu tiên | Cao |

### TC-03: Vượt hạn mức

| Mục | Nội dung |
|---|---|
| Điều kiện đầu vào | Lô sản xuất PACKAGED, nhưng totalQuantity yêu cầu vượt quá (total_limit − used_count) của dải mã đang hoạt động. |
| Hành động | Quản lý hợp tác xã yêu cầu tạo lô hàng với totalQuantity vượt hạn mức còn lại. |
| Kết quả mong đợi | Hệ thống chặn sinh mã, trả lỗi 409 kèm thông báo vượt hạn mức dải mã; không tạo Shipment, không thay đổi used_count. |
| Dữ liệu liên quan | Hạn mức dải mã (total_limit, used_count). |
| Mức độ ưu tiên | Cao |

### TC-04: Dữ liệu trùng lặp

| Mục | Nội dung |
|---|---|
| Điều kiện đầu vào | Mã truy xuất vừa sinh ra trùng với một mã đã tồn tại trong bảng TraceCode. |
| Hành động | Hệ thống tự động phát hiện trùng trong quá trình sinh mã (existsByCodeValue hoặc vi phạm ràng buộc UNIQUE khi insert). |
| Kết quả mong đợi | Hệ thống tự sinh lại một mã khác cho đơn vị đó, ghi log cảnh báo trùng mã, và tiếp tục hoàn tất lô hàng bình thường (không làm hỏng toàn bộ giao dịch). |
| Dữ liệu liên quan | Bảng TraceCode, cột code_value (UNIQUE). |
| Mức độ ưu tiên | Cao |
