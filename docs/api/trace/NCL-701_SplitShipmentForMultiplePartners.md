# NCL-701 – Tách lô hàng khi giao cho nhiều đối tác

> Jira Story: [NCL-701](https://tran-phuong-doan.atlassian.net/browse/NCL-701)
>
> Epic: NCL-76 – Ghi sự kiện chuỗi cung ứng
>
> Loại tài liệu: Phân tích nghiệp vụ và API Contract (contract-first)
>
> Trạng thái: **Proposed – chờ review trước khi triển khai các task Jira**
>
> Nhánh tài liệu: `feature/NCL-701-split-shipment-multiple-partners`

## 1. Mục tiêu

Cho phép Quản lý hợp tác xã (VT-02) tách một lô hàng đã kích hoạt thành nhiều lô con để giao cho nhiều doanh nghiệp thu mua. Mỗi lô con:

- có đối tác nhận xác định;
- nhận một phần mã tem đang thuộc lô cha;
- giữ được quan hệ truy xuất về lô cha và lô sản xuất nguồn;
- có hành trình riêng sau thời điểm tách;
- chỉ hiển thị cho đúng tổ chức nhận trong các luồng dành cho VT-04.

Thao tác tách chỉ phân bổ lại dữ liệu đã tồn tại. Hệ thống **không sinh mã tem mới, không hoàn hạn mức và không tăng `CodeRange.usedCount`**.

## 2. Nguồn yêu cầu

### 2.1. Ma trận truy vết yêu cầu

| ID | Loại | Nguồn | Yêu cầu chắc chắn | Tác động dự kiến | Bằng chứng cần có |
| --- | --- | --- | --- | --- | --- |
| `NCL-701` | Story | Jira | Tách lô hàng khi giao cho nhiều đối tác | Nghiệp vụ tách lô theo đối tác | Kiểm thử API và UI end-to-end |
| `NCL-781` | Subtask | Jira | Chốt quy tắc tách lô và phân bổ mã tem | Validation, transaction và thuật toán phân bổ | Unit/integration test |
| `NCL-782` | Subtask | Jira | Thiết kế quan hệ lô cha và lô con | Migration và quan hệ tự tham chiếu của `Shipment` | Migration test |
| `NCL-783` | Subtask | Jira | Thiết kế màn hình tách lô hàng | API preview, danh sách đối tác và kết quả tách | Frontend test/UI QA |
| `NCL-785` | Subtask | Jira | Phát triển tách lô và dựng hành trình theo lô con | API tách, chuyển tem, timeline theo lineage | Backend test và runtime test |
| `NCL-788` | Subtask | Jira | Kiểm thử tách lô hàng | Bộ test thành công, lỗi, quyền và đồng thời | Báo cáo kiểm thử |
| `NCL-04-CN-002` | Story liên quan | Excel backlog | Lô hàng được sinh mã truy xuất duy nhất | Không tạo hoặc nhân bản mã khi tách | So sánh tập mã trước/sau |
| `QTN-02` | Business rule | Excel backlog | Không cấp trùng mã cho hai lô khác nhau | Mỗi mã chỉ thuộc đúng một lô con | Constraint và test tính duy nhất |
| `QTN-03` | Business rule | Excel backlog | Số tem không vượt sản lượng/hạn mức | Tách không làm thay đổi tổng số tem đã cấp | Test bảo toàn số lượng và hạn mức |
| `QTN-04` | Business rule | Excel backlog | Chỉ kích hoạt tem sau khi đóng gói | Chỉ tách lô đã `ACTIVATED` | Test sai trạng thái |

### 2.2. Khoảng trống trong Jira

Tại thời điểm lập tài liệu, Story và năm subtask trên Jira chỉ có tiêu đề; các trường Description, User Role, Precondition và Postcondition đều chưa có nội dung. Jira cũng chưa có Acceptance Criteria chính thức cho NCL-701.

Vì vậy:

- các tiêu đề Jira trong bảng trên được xem là yêu cầu chắc chắn;
- các quy tắc tại mục 5 là **quyết định contract đề xuất** dựa trên kiến trúc đang chạy và các business rule hiện có;
- các test case tại mục 14 là tiêu chí kiểm thử đề xuất, chưa thay thế AC chính thức trên Jira;
- nếu Product Owner bổ sung AC khác với tài liệu này, phải cập nhật tài liệu trước khi code.

## 3. Hiện trạng hệ thống

### 3.1. Những thành phần có thể tái sử dụng

- `Shipment` đã liên kết với `ProductionLot`, tổ chức sở hữu, `CodeRange`, người tạo và trạng thái.
- `TraceCode` đã liên kết trực tiếp với một `Shipment`.
- `ChainEvent` đã liên kết với `Shipment` và hỗ trợ chuỗi băm theo từng lô hàng.
- `ShipmentServiceImpl` đã kiểm tra VT-02, cùng tổ chức, trạng thái lô sản xuất, điều kiện kiểm nghiệm và hạn mức mã.
- `GET /api/v1/shipments/{shipmentId}/chain-events` đã tổng hợp sự kiện của lô sản xuất và lô hàng.
- `Organization.type = ENTERPRISE` biểu diễn tổ chức doanh nghiệp/VT-04.

### 3.2. Khoảng trống cần xử lý

| Khu vực | Hiện trạng | Khoảng trống đối với NCL-701 |
| --- | --- | --- |
| Quan hệ lô | `Shipment` không có lô cha/lô con | Không dựng được lineage khi tách |
| Đối tác nhận | `Shipment` không có tổ chức nhận | Không biết lô con giao cho doanh nghiệp nào |
| Phân bổ tem | Tem chỉ trỏ tới shipment hiện tại | Chưa có thao tác chuyển một phần tem sang từng lô con |
| Trạng thái | Chỉ có `DRAFT`, `CODE_PRINTED`, `ACTIVATED`, `RECALLED` | Lô cha sau tách vẫn có thể bị dùng nhầm nếu không có trạng thái kết thúc |
| Danh sách VT-04 | `/shipments/eligible` trả mọi lô `ACTIVATED` | Có nguy cơ lộ hoặc ghi thu mua nhầm lô của đối tác khác |
| Thu mua | `ProcurementEventServiceImpl` chưa kiểm tra tổ chức nhận | VT-04 bất kỳ có thể ghi sự kiện cho lô `ACTIVATED` |
| Timeline | Chỉ lấy event của shipment hiện tại và lô sản xuất | Lô con không thấy lịch sử lô cha trước khi tách |
| Sản lượng | Chưa có khóa/validation dành riêng cho thao tác tách | Hai request đồng thời có thể phân bổ trùng tem |
| Báo cáo | Tổng hợp trực tiếp `SUM(shipment.totalQuantity)` | Có thể đếm kép lô cha và lô con sau tách |

## 4. Phạm vi

### 4.1. Bao gồm

- Xem trước khả năng tách của một lô hàng.
- Tra cứu danh sách đối tác doanh nghiệp đang hoạt động.
- Tách toàn bộ phần mã tem hợp lệ của một lô cha cho ít nhất hai đối tác.
- Tạo quan hệ một cấp giữa lô cha và các lô con.
- Chuyển quyền liên kết của mã tem từ lô cha sang lô con.
- Dựng timeline của lô con gồm lịch sử nguồn và lịch sử riêng.
- Cô lập dữ liệu lô con theo tổ chức nhận cho VT-04.
- Ghi activity log cho thao tác tách.
- Điều chỉnh truy vấn báo cáo để không đếm kép lô cha đã tách.

### 4.2. Không bao gồm

- Sinh mã tem mới hoặc cấp thêm hạn mức.
- Tách một lô con lần thứ hai (tách đa cấp).
- Gộp các lô con trở lại lô cha.
- Sửa hoặc hủy kết quả tách sau khi giao dịch thành công.
- Chia sẻ một lô con cho nhiều đối tác.
- Tự động gửi thông báo cho đối tác; Jira chưa có yêu cầu notification.
- Thay đổi dữ liệu lịch sử của lô sản xuất hoặc lô cha.

## 5. Quyết định nghiệp vụ đề xuất

### BR-701-01 – Chủ thể thực hiện

Chỉ VT-02 thuộc tổ chức sở hữu lô cha được tách lô. VT-01 có thể xem dữ liệu phục vụ quản trị nhưng không thực hiện thay VT-02 trong contract hiện tại.

Backend phải kiểm tra đồng thời role, permission `shipment:SPLIT` và `organizationId`; không chỉ ẩn nút trên giao diện.

### BR-701-02 – Điều kiện của lô cha

Lô cha phải:

- tồn tại và thuộc tổ chức hiện tại;
- đang ở trạng thái `ACTIVATED`;
- không phải lô con (`parentShipmentId = null`);
- chưa từng tách;
- không bị thu hồi;
- có ít nhất hai mã tem có thể phân bổ.

Việc chỉ nhận `ACTIVATED` phù hợp với ngữ cảnh “khi giao cho đối tác” và bảo toàn QTN-04.

### BR-701-03 – Tách toàn phần

Một request phải có ít nhất hai phần phân bổ và tổng `quantity` phải bằng số mã tem có thể phân bổ của lô cha.

Contract không hỗ trợ tách một phần để tránh lô cha vừa là nguồn lineage vừa tiếp tục lưu hành. Nếu nghiệp vụ cần giữ lại hàng tại HTX, phần giữ lại phải được biểu diễn thành một lô con riêng hoặc Product Owner phải bổ sung quy tắc tách một phần trước khi triển khai.

### BR-701-04 – Đơn vị số lượng

`quantity` là **số đơn vị tem/mã truy xuất**, kiểu số nguyên dương. Không dùng `ProductionLot.actualQuantity` vì trường này là số thực và có thể mang đơn vị kg, tấn, gói hoặc đơn vị khác.

Số lượng có thể phân bổ được tính bằng số `TraceCode` của lô cha có trạng thái khác `CANCELLED`. Mã đã hủy giữ nguyên trên lô cha để bảo toàn lịch sử và không được chuyển sang lô con.

### BR-701-05 – Đối tác nhận

Mỗi phần phân bổ phải trỏ tới một `Organization`:

- có `type = ENTERPRISE`;
- có `status = ACTIVE`;
- khác tổ chức sở hữu lô cha;
- không bị lặp lại trong cùng request.

Một lô con chỉ có một `recipientOrganization`.

### BR-701-06 – Phân bổ mã tem

Backend lấy các mã có thể phân bổ, sắp xếp tăng dần theo `codeValue`, sau đó cấp tuần tự theo thứ tự phần tử trong `allocations`.

Nguyên tắc:

- không tạo `TraceCode` mới;
- không đổi `codeValue`, ảnh QR, trạng thái kích hoạt hoặc thông tin khóa/thu hồi;
- chỉ cập nhật khóa ngoại `trace_codes.shipment_id` sang lô con;
- mỗi mã chỉ được chuyển đúng một lần;
- tổng mã ở các lô con bằng tổng mã có thể phân bổ trước khi tách.

### BR-701-07 – Trạng thái sau khi tách

- Lô cha chuyển từ `ACTIVATED` sang `SPLIT` và không còn xuất hiện trong danh sách đủ điều kiện giao/thu mua.
- Các lô con được tạo ở trạng thái `ACTIVATED` vì chỉ nhận các mã đã kích hoạt từ lô cha.
- `CodeRange.usedCount` không thay đổi.
- `Shipment.totalQuantity` của lô cha giữ nguyên để audit; báo cáo nghiệp vụ phải loại lô `SPLIT` khỏi tổng lưu hành để tránh đếm kép.

### BR-701-08 – Giao dịch và đồng thời

Toàn bộ thao tác phải chạy trong một transaction:

1. khóa lô cha bằng `PESSIMISTIC_WRITE`;
2. đọc và khóa các mã tem có thể phân bổ;
3. kiểm tra lại trạng thái, đối tác và tổng số lượng;
4. tạo các lô con;
5. chuyển mã tem;
6. ghi sự kiện tách;
7. đổi trạng thái lô cha;
8. ghi activity log.

Nếu bất kỳ bước nào lỗi, toàn bộ thay đổi rollback. Request thứ hai trên cùng lô sau khi request đầu thành công trả `409 Conflict`.

### BR-701-09 – Hành trình lô con

Không sao chép các `ChainEvent` cũ sang lô con vì việc đó tạo dữ liệu lịch sử trùng và làm sai chuỗi băm.

Timeline lô con được dựng theo thứ tự:

1. sự kiện cấp lô sản xuất (`HARVEST`, `PREPROCESSING`, `PACKAGING`);
2. sự kiện của lô cha trước thời điểm tách;
3. sự kiện `SPLIT` là sự kiện đầu tiên thuộc lô con;
4. các sự kiện phát sinh riêng trên lô con sau khi tách.

Sự kiện `SPLIT` của mỗi lô con chứa `sourceShipmentId`, `sourceShipmentName`, `recipientOrganizationId`, `recipientOrganizationName`, `allocatedQuantity`, `sourceLastEventHash` và thời điểm tách. Chuỗi băm của lô con bắt đầu từ sự kiện `SPLIT`; `sourceLastEventHash` là bằng chứng liên kết lineage, không ghi đè chuỗi băm của lô cha.

### BR-701-10 – Cô lập dữ liệu đối tác

Sau khi tách:

- VT-04 chỉ thấy lô con có `recipientOrganizationId` bằng tổ chức đang đăng nhập;
- VT-04 chỉ được ghi `PROCUREMENT`, `WAREHOUSE_RECEIPT` và sự kiện liên quan cho lô được giao cho mình;
- VT-02/VT-03 của tổ chức nguồn vẫn xem được lô cha, các lô con và timeline;
- tra cứu công khai bằng mã tem tiếp tục hoạt động và tự trỏ tới lô con sau khi cập nhật `trace_codes.shipment_id`.

## 6. Mô hình trạng thái

```text
ACTIVATED (lô cha)
    |
    | POST /api/v1/shipments/{id}/split
    v
SPLIT (lô cha, chỉ còn vai trò lineage/audit)
    |
    +-- ACTIVATED (lô con A -> đối tác A)
    +-- ACTIVATED (lô con B -> đối tác B)
    +-- ACTIVATED (lô con N -> đối tác N)
```

Các chuyển trạng thái bị từ chối:

- `DRAFT`, `CODE_PRINTED` hoặc `RECALLED` → `SPLIT`;
- `SPLIT` → `SPLIT` lần nữa;
- lô con `ACTIVATED` → `SPLIT` trong phạm vi Story hiện tại.

## 7. API danh sách đối tác

### 7.1. Endpoint

```http
GET /api/v1/partner-organizations?keyword=&page=0&size=20
Authorization: Bearer <access-token>
```

### 7.2. Quyền

- Role: VT-02.
- Permission: `organization:READ` hoặc permission đọc đối tác tương đương theo seed RBAC được chốt khi triển khai.
- Chỉ trả tổ chức `ENTERPRISE` và `ACTIVE`.
- Không trả thông tin thành viên, tài khoản hoặc trường quản trị nội bộ.

### 7.3. Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "4f2d3a6e-8e2d-4a70-b2c2-0dfe557a4141",
        "code": "DN-TM-001",
        "name": "Doanh nghiệp Thu mua An Phú"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-09-10T08:00:00Z"
}
```

## 8. API xem trước khả năng tách

### 8.1. Endpoint

```http
GET /api/v1/shipments/{shipmentId}/split-preview
Authorization: Bearer <access-token>
```

### 8.2. Mục đích

Cho giao diện biết lô có đủ điều kiện tách hay không và hiển thị đúng số lượng có thể phân bổ trước khi gửi lệnh ghi dữ liệu.

### 8.3. Response `200 OK`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "shipmentId": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
    "shipmentName": "Lô hàng thanh long tháng 9",
    "status": "ACTIVATED",
    "productionLotId": "d4d9330f-c68d-4f02-a333-e4044769cb68",
    "productionLotName": "Thanh long vụ tháng 9",
    "declaredQuantity": 1000,
    "assignableQuantity": 980,
    "cancelledQuantity": 20,
    "canSplit": true,
    "blockReasonCode": null,
    "blockMessage": null
  },
  "timestamp": "2026-09-10T08:05:00Z"
}
```

Khi lô không đủ điều kiện, endpoint vẫn trả `200` với `canSplit = false` và một trong các mã:

| `blockReasonCode` | Ý nghĩa |
| --- | --- |
| `INVALID_STATUS` | Lô không ở trạng thái `ACTIVATED` |
| `ALREADY_SPLIT` | Lô cha đã được tách |
| `CHILD_SHIPMENT` | Đây là lô con |
| `INSUFFICIENT_CODES` | Có ít hơn hai mã có thể phân bổ |

`403` và `404` vẫn được dùng cho lỗi quyền và không tìm thấy tài nguyên.

## 9. API thực hiện tách lô

### 9.1. Endpoint

```http
POST /api/v1/shipments/{shipmentId}/split
Authorization: Bearer <access-token>
Content-Type: application/json
```

### 9.2. Request body

```json
{
  "allocations": [
    {
      "recipientOrganizationId": "4f2d3a6e-8e2d-4a70-b2c2-0dfe557a4141",
      "name": "Lô thanh long giao An Phú",
      "quantity": 600,
      "packagingInfo": "Thùng 10 kg"
    },
    {
      "recipientOrganizationId": "1c78c8eb-7da3-4e2c-88a1-75fc73783b45",
      "name": "Lô thanh long giao Minh Long",
      "quantity": 380,
      "packagingInfo": "Thùng 10 kg"
    }
  ]
}
```

### 9.3. Validation request

| Trường | Kiểu | Bắt buộc | Quy tắc |
| --- | --- | --- | --- |
| `allocations` | array | Có | Từ 2 phần tử trở lên |
| `allocations[].recipientOrganizationId` | UUID | Có | ENTERPRISE, ACTIVE, không lặp, khác tổ chức nguồn |
| `allocations[].name` | string | Có | Sau trim từ 1 đến 255 ký tự |
| `allocations[].quantity` | integer | Có | Lớn hơn 0 |
| `allocations[].packagingInfo` | string | Không | Tối đa 500 ký tự |

Tổng `allocations[].quantity` phải bằng `assignableQuantity` tại thời điểm backend khóa và kiểm tra lại lô cha. Frontend không được coi giá trị preview là nguồn sự thật sau khi POST bắt đầu.

### 9.4. Response `201 Created`

Response không trả toàn bộ danh sách mã tem để tránh payload rất lớn. Mỗi lô con chỉ trả số lượng và khoảng mã đầu/cuối phục vụ xác nhận.

```json
{
  "success": true,
  "status": 201,
  "data": {
    "sourceShipment": {
      "id": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
      "name": "Lô hàng thanh long tháng 9",
      "status": "SPLIT",
      "declaredQuantity": 1000,
      "allocatedQuantity": 980,
      "cancelledQuantity": 20
    },
    "children": [
      {
        "id": "2ff72c65-ef2e-43bd-bda0-157fce51158f",
        "parentShipmentId": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
        "name": "Lô thanh long giao An Phú",
        "status": "ACTIVATED",
        "recipientOrganization": {
          "id": "4f2d3a6e-8e2d-4a70-b2c2-0dfe557a4141",
          "code": "DN-TM-001",
          "name": "Doanh nghiệp Thu mua An Phú"
        },
        "totalQuantity": 600,
        "firstCode": "HTX00000001",
        "lastCode": "HTX00000600"
      },
      {
        "id": "f532f497-87d4-4352-acbf-7660b779c2fd",
        "parentShipmentId": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
        "name": "Lô thanh long giao Minh Long",
        "status": "ACTIVATED",
        "recipientOrganization": {
          "id": "1c78c8eb-7da3-4e2c-88a1-75fc73783b45",
          "code": "DN-TM-002",
          "name": "Doanh nghiệp Thu mua Minh Long"
        },
        "totalQuantity": 380,
        "firstCode": "HTX00000601",
        "lastCode": "HTX00000980"
      }
    ],
    "totalChildren": 2,
    "totalAllocatedQuantity": 980,
    "splitByName": "Quản lý HTX Demo",
    "splitAt": "2026-09-10T15:10:00"
  },
  "timestamp": "2026-09-10T08:10:00Z"
}
```

## 10. Thay đổi response chi tiết lô hàng

`GET /api/v1/shipments/{id}` giữ nguyên các trường hiện có và bổ sung trường nullable:

```json
{
  "parentShipmentId": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
  "recipientOrganization": {
    "id": "4f2d3a6e-8e2d-4a70-b2c2-0dfe557a4141",
    "code": "DN-TM-001",
    "name": "Doanh nghiệp Thu mua An Phú"
  },
  "childCount": 0,
  "splitAt": "2026-09-10T15:10:00"
}
```

Quy tắc hiển thị:

- lô thường: các trường trên là `null`/`0`;
- lô cha đã tách: `parentShipmentId = null`, `childCount > 0`, `status = SPLIT`;
- lô con: có `parentShipmentId`, `recipientOrganization`, `childCount = 0`.

## 11. Thay đổi API timeline và API VT-04

### 11.1. Timeline

Giữ endpoint:

```http
GET /api/v1/shipments/{shipmentId}/chain-events
```

Mỗi phần tử bổ sung nguồn hiển thị:

```json
{
  "lineageLevel": "SOURCE_SHIPMENT",
  "sourceShipmentId": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
  "inherited": true
}
```

`lineageLevel` nhận một trong:

- `PRODUCTION_LOT`;
- `SOURCE_SHIPMENT`;
- `CHILD_SHIPMENT`.

Các event kế thừa chỉ là kết quả tổng hợp khi đọc; không tạo bản ghi sao chép trong database.

### 11.2. Danh sách lô đủ điều kiện thu mua

`GET /api/v1/shipments/eligible` phải thay đổi từ “mọi shipment `ACTIVATED`” thành:

```text
status = ACTIVATED
AND recipient_organization_id = currentUser.organizationId
```

Trong giai đoạn tương thích, shipment cũ chưa có `recipientOrganizationId` không được tự động hiển thị cho mọi VT-04. Cần chốt dữ liệu backfill hoặc cơ chế gán đối tác trước khi bật migration trên môi trường có dữ liệu thật.

### 11.3. Ghi sự kiện thu mua/nhập kho

Các API dành cho VT-04 phải kiểm tra `shipment.recipientOrganizationId = currentUser.organizationId`. Nếu khác tổ chức, trả `403` và không tạo event.

## 12. Error contract

| HTTP | Mã lỗi | Trường hợp | Thông báo |
| ---: | --- | --- | --- |
| 400 | `SPLIT_001` | Có ít hơn 2 phần phân bổ | `Phải phân bổ lô hàng cho ít nhất hai đối tác.` |
| 400 | `SPLIT_002` | Số lượng không phải số nguyên dương | `Số lượng phân bổ phải lớn hơn 0.` |
| 400 | `SPLIT_003` | Trùng đối tác | `Mỗi đối tác chỉ được xuất hiện một lần trong yêu cầu tách lô.` |
| 400 | `SPLIT_004` | Tổng số lượng không bằng số tem có thể phân bổ | `Tổng số lượng phân bổ phải bằng {assignableQuantity}.` |
| 400 | `SPLIT_005` | Tổ chức nhận không phải doanh nghiệp ACTIVE | `Đối tác nhận không hợp lệ hoặc đã ngừng hoạt động.` |
| 400 | `SPLIT_006` | Tổ chức nhận trùng tổ chức nguồn | `Không thể chọn tổ chức nguồn làm đối tác nhận.` |
| 401 | `AUTHENTICATION_REQUIRED` | Chưa đăng nhập/hết phiên | `Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn.` |
| 403 | `ACCESS_DENIED` | Không phải VT-02/thiếu permission | `Bạn không có quyền tách lô hàng.` |
| 403 | `CROSS_ORGANIZATION_ACCESS` | Lô cha thuộc tổ chức khác | `Bạn không có quyền tách lô hàng của tổ chức khác.` |
| 403 | `RECIPIENT_MISMATCH` | VT-04 thao tác lô không giao cho tổ chức mình | `Lô hàng không được giao cho tổ chức của bạn.` |
| 404 | `SHIPMENT_NOT_FOUND` | Không tìm thấy lô cha | `Không tìm thấy lô hàng.` |
| 404 | `PARTNER_NOT_FOUND` | Không tìm thấy tổ chức nhận | `Không tìm thấy đối tác nhận.` |
| 409 | `INVALID_SHIPMENT_STATUS` | Lô không phải `ACTIVATED` | `Chỉ có thể tách lô hàng đã kích hoạt.` |
| 409 | `ALREADY_SPLIT` | Lô đã tách | `Lô hàng đã được tách trước đó.` |
| 409 | `CHILD_SPLIT_NOT_ALLOWED` | Cố tách lô con | `Không hỗ trợ tách tiếp một lô con.` |
| 409 | `TRACE_CODE_COUNT_CHANGED` | Tập tem thay đổi giữa preview và POST | `Số lượng mã tem đã thay đổi. Vui lòng tải lại thông tin lô.` |

Response lỗi tuân theo `ApiResult` hiện có:

```json
{
  "success": false,
  "status": 409,
  "message": "Lô hàng đã được tách trước đó.",
  "errors": {
    "code": "ALREADY_SPLIT"
  },
  "path": "/api/v1/shipments/9d7b499f-ea66-459a-8eb5-2ad81447ae61/split",
  "timestamp": "2026-09-10T08:10:00Z"
}
```

## 13. Tác động dữ liệu và migration

### 13.1. Bảng `shipments`

Đề xuất bổ sung:

| Cột | Kiểu | Nullable | Ý nghĩa |
| --- | --- | --- | --- |
| `parent_shipment_id` | `CHAR(36)` | Có | Lô cha của lô con |
| `recipient_organization_id` | `CHAR(36)` | Có | Doanh nghiệp nhận lô con |
| `split_at` | `DATETIME` | Có | Thời điểm tách đối với lô cha/lô con |
| `split_by` | `CHAR(36)` | Có | Người thực hiện tách |

Ràng buộc/index:

- self foreign key `parent_shipment_id -> shipments.id`;
- foreign key `recipient_organization_id -> organizations.organization_id`;
- foreign key `split_by -> users.user_id`;
- index `idx_shipments_parent_shipment_id`;
- index `idx_shipments_recipient_status (recipient_organization_id, status)`;
- check ở service: lô con phải cùng `production_lot_id`, `organization_id` và `code_range_id` với lô cha.

### 13.2. Enum

- Bổ sung `ShipmentStatus.SPLIT`.
- Bổ sung `ChainEventType.SPLIT`.

### 13.3. Dữ liệu cũ

- Các shipment hiện có giữ `parent_shipment_id = null` và `recipient_organization_id = null`.
- Không tự suy đoán đối tác nhận từ event nếu có nhiều `PROCUREMENT` event thuộc nhiều tổ chức.
- Trước khi lọc chặt `/shipments/eligible`, phải có quyết định backfill shipment cũ hoặc chấp nhận chúng không còn xuất hiện cho VT-04.

### 13.4. Báo cáo

Mọi truy vấn tổng số lượng shipment phải loại `status = SPLIT` hoặc chỉ tính shipment lá (`NOT EXISTS child`) để tránh cộng cả lô cha và lô con.

## 14. Test case đề xuất

> Đây là test case contract-first do Jira chưa có Acceptance Criteria chính thức.

- [ ] **TC-01 – Tách thành công:** VT-02 tách lô `ACTIVATED` cho hai đối tác; tạo đúng hai lô con, lô cha thành `SPLIT`.
- [ ] **TC-02 – Bảo toàn mã:** Tổng mã hợp lệ ở các lô con bằng trước khi tách; không có mã mới/trùng/mất; `CodeRange.usedCount` không đổi.
- [ ] **TC-03 – Mã đã hủy:** Tem `CANCELLED` không chuyển sang lô con và không được tính vào `assignableQuantity`.
- [ ] **TC-04 – Tổng phân bổ sai:** Tổng nhỏ hơn hoặc lớn hơn `assignableQuantity` bị từ chối; không có dữ liệu nào được tạo.
- [ ] **TC-05 – Sai trạng thái:** `DRAFT`, `CODE_PRINTED`, `RECALLED`, `SPLIT` bị chặn.
- [ ] **TC-06 – Lô con:** Không cho tách tiếp lô có `parentShipmentId`.
- [ ] **TC-07 – Đối tác sai:** Tổ chức không tồn tại, không ACTIVE, không phải ENTERPRISE, trùng nhau hoặc là tổ chức nguồn bị chặn.
- [ ] **TC-08 – Cross-tenant:** VT-02 không thể preview/tách lô của HTX khác.
- [ ] **TC-09 – Sai quyền:** VT-01, VT-03, VT-04 hoặc user thiếu `shipment:SPLIT` không thể tách.
- [ ] **TC-10 – Đồng thời:** Hai request tách cùng lô chỉ một request thành công; request còn lại trả `409`.
- [ ] **TC-11 – Rollback:** Giả lập lỗi khi chuyển mã/ghi event; lô cha, lô con, tem và trạng thái giữ nguyên.
- [ ] **TC-12 – Timeline:** Timeline lô con hiển thị sự kiện lô sản xuất, lô cha, sự kiện `SPLIT` và event riêng đúng thứ tự; database không có event bị sao chép.
- [ ] **TC-13 – Cô lập đối tác:** VT-04 chỉ thấy và ghi event cho lô con được giao cho tổ chức mình.
- [ ] **TC-14 – Tra cứu công khai:** Quét mã cũ sau tách trả đúng lô con và vẫn có lịch sử nguồn.
- [ ] **TC-15 – Báo cáo:** Tổng sản lượng không tăng sau khi tách; lô cha `SPLIT` không bị đếm kép.
- [ ] **TC-16 – Payload lớn:** Response tách không trả toàn bộ mã tem; API vẫn đáp ứng với lô có nhiều mã.

## 15. Tác động frontend

- Chỉ hiển thị nút **Tách lô** với VT-02 có permission và lô `ACTIVATED` chưa phải lô con.
- Màn hình gọi `split-preview` trước, hiển thị số lượng khai báo, có thể phân bổ và đã hủy.
- Cho thêm tối thiểu hai dòng đối tác; không cho chọn trùng.
- Luôn hiển thị tổng đã phân bổ và số còn thiếu/thừa.
- Vô hiệu hóa nút xác nhận khi tổng chưa bằng `assignableQuantity`.
- Sau thành công, chuyển tới chi tiết lô cha hoặc danh sách lô con và invalidate các query shipment, timeline, eligible shipment và báo cáo liên quan.
- Hiển thị trạng thái `SPLIT` bằng nhãn tiếng Việt **Đã tách**.
- Timeline phân biệt sự kiện nguồn/kế thừa và sự kiện riêng của lô con nhưng không làm người dùng hiểu rằng event đã bị sao chép.

## 16. Bảo mật và tính toàn vẹn

- Bắt buộc tenant isolation ở service/repository; không tin `organizationId` từ request.
- Không trả danh sách thành viên hoặc dữ liệu nhạy cảm khi tìm đối tác.
- Khóa lô cha và tập mã tem trong transaction để chống double allocation.
- Không nhận trực tiếp danh sách `traceCodeId` từ client trong contract hiện tại, tránh IDOR và phân bổ thiếu/trùng; backend tự phân bổ xác định theo `codeValue`.
- Không sửa `codeValue`, trạng thái khóa/thu hồi hoặc file QR.
- Activity log dùng action `SPLIT_SHIPMENT`, entity là lô cha, metadata chứa danh sách lô con và tổng số lượng nhưng không chứa token/thông tin nhạy cảm.
- Các API chi tiết/timeline phải kiểm tra organization scope, không chỉ dựa vào role.

## 17. Non-regression

- `POST /api/v1/shipments` vẫn tạo shipment và mã tem như hiện tại.
- `POST /api/v1/shipments/{id}/activate` không thay đổi với shipment thường.
- QR/code value đã phát hành vẫn tra cứu được sau tách.
- Hạn mức dải mã không thay đổi do thao tác tách.
- Thu hồi phải áp dụng đúng trên lô con; lô cha `SPLIT` không được thu hồi như một lô đang lưu hành.
- Xuất hồ sơ truy xuất của lô con phải bao gồm lineage nguồn nhưng không lặp chứng từ.
- Shipment chưa tách tiếp tục dùng response cũ nhờ các trường mới là nullable/additive.

## 18. Các điểm cần Product Owner xác nhận

Các điểm dưới đây chưa có trong Jira và cần được xác nhận trước khi chuyển sang backend implementation:

1. Có bắt buộc tách toàn bộ hay cho phép giữ lại một phần hàng trên lô cha?
2. Có cho phép một lô con tiếp tục được tách ở mắt xích sau hay chỉ hỗ trợ một cấp?
3. Đối tác có cần nhận notification ngay khi được phân bổ lô không?
4. Với shipment cũ chưa có `recipientOrganizationId`, có backfill từ sự kiện thu mua hay yêu cầu gán thủ công?
5. Việc phân bổ tự động theo thứ tự `codeValue` có phù hợp vận hành thực tế, hay người dùng cần quét/chọn chính xác dải tem đã giao?

Cho tới khi có phản hồi khác, contract triển khai mặc định theo các quyết định tại mục 5: **tách toàn phần, một cấp, không notification và backend tự phân bổ mã theo thứ tự tăng dần**.

## 19. Thứ tự triển khai sau khi contract được duyệt

1. `NCL-781` – xác nhận quy tắc tại mục 5 và các câu hỏi mục 18.
2. `NCL-782` – migration, entity, repository và dữ liệu tương thích.
3. Backend của `NCL-785` – preview, partner lookup, split transaction, tenant isolation và timeline.
4. `NCL-783` – giao diện tách lô và tích hợp API.
5. Phần còn lại của `NCL-785` – tích hợp hành trình, public lookup, báo cáo và các luồng VT-04.
6. `NCL-788` – unit test, integration test, runtime/UI test và regression.
