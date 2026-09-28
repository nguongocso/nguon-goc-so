# NCL-701 – Tách lô hàng khi giao cho nhiều đối tác

> Jira Story: [NCL-701](https://tran-phuong-doan.atlassian.net/browse/NCL-701)
>
> Epic: NCL-76 – Ghi sự kiện chuỗi cung ứng
>
> Loại tài liệu: Phân tích nghiệp vụ và API Contract (contract-first)
>
> Task hiện tại: [NCL-781](https://tran-phuong-doan.atlassian.net/browse/NCL-781) – Chốt quy tắc tách lô hàng và phân bổ mã tem
>
> Trạng thái: **Contract CV-01 đã chốt – sẵn sàng chuyển sang thiết kế dữ liệu**
>
> Nhánh tài liệu: `feature/NCL-701-split-shipment-multiple-partners`

## 1. Mục tiêu

Cho phép Quản lý hợp tác xã (VT-02) tách một lô hàng đã sinh mã nhưng chưa kích hoạt thành nhiều lô con để giao cho nhiều doanh nghiệp thu mua. Mỗi lô con:

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
| `NCL-781` | Subtask | Jira | Chốt quy tắc tách lô và phân bổ mã tem | Validation, transaction và quy tắc chọn khoảng mã | Review ma trận quy tắc và unit/integration test |
| `NCL-782` | Subtask | Jira | Thiết kế quan hệ lô cha và lô con | Migration và quan hệ tự tham chiếu của `Shipment` | Migration test |
| `NCL-783` | Subtask | Jira | Thiết kế màn hình tách lô hàng | API preview, danh sách đối tác và kết quả tách | Frontend test/UI QA |
| `NCL-785` | Subtask | Jira | Phát triển tách lô và dựng hành trình theo lô con | API tách, chuyển tem, timeline theo lineage | Backend test và runtime test |
| `NCL-788` | Subtask | Jira | Kiểm thử tách lô hàng | Bộ test thành công, lỗi, quyền và đồng thời | Báo cáo kiểm thử |
| `NCL-05-CN-010` | Story | Excel backlog, dòng 98 | Lô đã sinh mã và chưa thu hồi được tách toàn phần; tem chưa kích hoạt được phân theo khoảng người dùng chọn; lịch sử chung tới điểm tách | Contract trạng thái, khoảng mã và timeline | `TC-01` đến `TC-04` |
| `NCL-05-CN-010-CV-01` | Task | Excel Tasks, dòng 474 | Xác định kế thừa lịch sử và ràng buộc tổng số lượng, tổng số mã | Nội dung chính của NCL-781 | Review contract |
| `NCL-05-CN-010-TC-01` | AC | Excel Acceptance Criteria, dòng 400 | Tách 1.000 mã thành hai lô con 500 mã; giữ liên kết cha và lịch sử tới điểm tách | Luồng thành công | API/integration test |
| `NCL-05-CN-010-TC-02` | AC | Excel Acceptance Criteria, dòng 401 | Chặn khi tổng phân bổ 1.200 mã vượt 1.000 mã của lô cha | Bảo toàn tổng mã | Validation test |
| `NCL-05-CN-010-TC-03` | AC | Excel Acceptance Criteria, dòng 402 | Quét mã lô con chỉ thấy lịch sử chung rồi nhánh của đúng lô con | Public timeline | API/UI test |
| `NCL-05-CN-010-TC-04` | AC | Excel Acceptance Criteria, dòng 403 | Không cho tách lô đang bị thu hồi | Validation trạng thái | Conflict test |
| `NCL-04-CN-002` | Story liên quan | Excel backlog | Lô hàng được sinh mã truy xuất duy nhất | Không tạo hoặc nhân bản mã khi tách | So sánh tập mã trước/sau |
| `QTN-01` | Business rule hỗ trợ | Excel Business Rules | Mỗi tổ chức chỉ thao tác dữ liệu của mình | Chặn truy cập chéo tổ chức | Permission test |
| `QTN-02` | Business rule | Excel Business Rules | Không cấp trùng mã cho hai lô khác nhau | Mỗi mã chỉ thuộc đúng một lô con | Constraint và test tính duy nhất |
| `QTN-08` | Business rule | Excel Business Rules | Sự kiện đã ghi không sửa/xóa; chỉ thêm sự kiện mới | Không sao chép hoặc sửa event gốc khi tách | Timeline/hash test |
| `QTN-24` | Business rule | Excel Business Rules | Thu hồi phải xét toàn bộ lô cùng nguồn | Duyệt lineage cha/con khi xác định phạm vi | Recall regression test |

### 2.2. Khoảng trống trong Jira

Jira Story và năm subtask chỉ có tiêu đề; các trường Description, User Role, Precondition và Postcondition chưa có nội dung. Tuy nhiên workbook **Bản sao của Nguồn Gốc Số (3).xlsx** đã có Story `NCL-05-CN-010`, bốn Acceptance Criteria và năm task tương ứng. Trong contract này, Excel là nguồn chi tiết để bổ sung cho Jira.

Vì vậy:

- tiêu đề Jira và nội dung Story/AC/Task/Business Rule trong Excel được xem là yêu cầu chắc chắn;
- các quy tắc tại mục 5 phân biệt rõ phần đã được Excel chốt và phần còn là quyết định kỹ thuật;
- bốn test case Excel là Acceptance Criteria nguồn; các test bổ sung tại mục 14 dùng để chứng minh bảo mật, transaction và non-regression;
- nếu Product Owner bổ sung AC khác với tài liệu này, phải cập nhật tài liệu trước khi code.

Excel ghi `QTN-24 (cần bổ sung)` tại Story nhưng sheet Business Rules đã có `QTN-24`. Contract sử dụng nội dung QTN-24 hiện hữu và coi ghi chú “cần bổ sung” là dữ liệu tham chiếu chưa được cập nhật.

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
- Tách toàn bộ số lượng và toàn bộ mã tem `INACTIVE` của một lô cha cho ít nhất hai đối tác.
- Tạo quan hệ một cấp giữa lô cha và các lô con.
- Chuyển quyền liên kết của mã tem từ lô cha sang lô con.
- Dựng timeline của lô con gồm lịch sử nguồn và lịch sử riêng.
- Cô lập dữ liệu lô con theo tổ chức nhận cho VT-04.
- Ghi activity log cho thao tác tách.
- Điều chỉnh truy vấn báo cáo để không đếm kép lô cha đã tách.

### 4.2. Không bao gồm

- Sinh mã tem mới hoặc cấp thêm hạn mức.
- Tách một lô con lần thứ hai (tách đa cấp) cho tới khi Product Owner xác nhận yêu cầu này.
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
- đã sinh mã và đang ở trạng thái `CODE_PRINTED`;
- không phải lô con (`parentShipmentId = null`);
- chưa từng tách;
- không bị thu hồi;
- có ít nhất hai mã tem;
- toàn bộ mã đang thuộc lô cha đều ở trạng thái `INACTIVE`.

Excel quy định “các mã tem chưa kích hoạt được phân về các lô con theo khoảng mã do người dùng chọn”. Trong code hiện tại, lô vừa sinh mã có trạng thái `CODE_PRINTED` và tem có trạng thái `INACTIVE`; khi kích hoạt, lô chuyển sang `ACTIVATED` và tem chuyển sang `ACTIVE`. Vì vậy, thao tác tách diễn ra **trước khi kích hoạt**, không nhận lô `ACTIVATED`.

### BR-701-03 – Tách toàn phần

Một request phải có ít nhất hai phần phân bổ. Đồng thời:

- tổng `quantity` của các lô con bằng `Shipment.totalQuantity` của lô cha;
- tổng số mã trong các khoảng được chọn bằng tổng số mã của lô cha;
- số mã của từng lô con bằng `quantity` của chính lô con.

Contract không hỗ trợ tách một phần để tránh lô cha vừa là nguồn lineage vừa tiếp tục lưu hành. Nếu nghiệp vụ cần giữ lại hàng tại HTX, phần giữ lại phải được biểu diễn thành một lô con riêng hoặc Product Owner phải bổ sung quy tắc tách một phần trước khi triển khai.

### BR-701-04 – Đơn vị số lượng

Product Owner đã chốt **phương án A**: `Shipment.totalQuantity` luôn tương ứng một-một với số `TraceCode` được sinh cho lô hàng. Vì vậy, `quantity` trong contract tách lô là **số đơn vị tem/mã truy xuất**, kiểu số nguyên dương; không phải khối lượng vật lý của hàng hóa.

Tại thời điểm lô vừa được tạo, quan hệ bắt buộc là:

```text
Shipment.totalQuantity = tổng số TraceCode của lô = số TraceCode INACTIVE
```

Sau khi trạng thái tem thay đổi, `Shipment.totalQuantity` vẫn giữ nguyên và không phải bộ đếm động của tem `INACTIVE`. Do đó, trước khi tách, backend phải đếm và kiểm tra riêng `totalCodeCount` và `inactiveCodeCount`; chỉ cho phép tách khi cả hai cùng bằng `Shipment.totalQuantity`.

AC “1.000 mã và một tấn hàng” được hiểu là lô có 1.000 đơn vị tem; thông tin một tấn không được suy ra từ `Shipment.totalQuantity`. Hệ thống hiện chưa lưu khối lượng vật lý riêng trên `Shipment`; nếu cần quản lý khối lượng độc lập với số tem thì phải bổ sung `physicalQuantity`, `unit` và `traceCodeCount` trong một User Story khác. Không dùng `ProductionLot.actualQuantity` để chia vì trường đó thuộc lô sản xuất và có thể mang đơn vị khác.

Số lượng có thể phân bổ được tính bằng số `TraceCode` trạng thái `INACTIVE` của lô cha. Nếu tồn tại mã `ACTIVE`, `CANCELLED`, `LOCKED`, `SUSPECT` hoặc `RECALLED`, hệ thống chặn toàn bộ thao tác tách để tránh vi phạm yêu cầu tổng mã lô con phải bằng lô cha.

### BR-701-05 – Đối tác nhận

Mỗi phần phân bổ phải trỏ tới một `Organization`:

- có `type = ENTERPRISE`;
- có `status = ACTIVE`;
- khác tổ chức sở hữu lô cha;
- không bị lặp lại trong cùng request.

Một lô con chỉ có một `recipientOrganization`.

### BR-701-06 – Phân bổ mã tem

Người dùng chọn chính xác khoảng mã giao cho từng lô con bằng `fromCode` và `toCode`. Backend không tự quyết định khoảng mã thay người dùng.

Nguyên tắc:

- hai đầu khoảng mã phải tồn tại, thuộc lô cha và có trạng thái `INACTIVE`;
- số mã thực tế trong khoảng phải bằng `quantity` của lô con;
- các khoảng không được giao nhau, không được bỏ sót mã và hợp của các khoảng phải đúng bằng tập mã của lô cha;
- không tạo `TraceCode` mới;
- không đổi `codeValue`, ảnh QR, trạng thái kích hoạt hoặc thông tin khóa/thu hồi;
- chỉ cập nhật khóa ngoại `trace_codes.shipment_id` sang lô con;
- mỗi mã chỉ được chuyển đúng một lần;
- thứ tự so sánh khoảng dùng cùng quy tắc thứ tự mã mà hệ thống đã dùng khi sinh mã, không so sánh chuỗi tùy ý ở frontend.

### BR-701-07 – Trạng thái sau khi tách

- Lô cha chuyển từ `CODE_PRINTED` sang `SPLIT` và không còn được kích hoạt hoặc dùng cho giao/thu mua.
- Các lô con được tạo ở trạng thái `CODE_PRINTED`; toàn bộ mã của lô con vẫn là `INACTIVE` và được kích hoạt riêng theo luồng hiện có.
- `CodeRange.usedCount` không thay đổi.
- `Shipment.totalQuantity` của lô cha giữ nguyên để audit; báo cáo nghiệp vụ phải loại lô `SPLIT` khỏi tổng lưu hành để tránh đếm kép.
- Lô con `CODE_PRINTED` không được đi vào luồng hủy bản nháp vì thao tác này sẽ xóa mã đã được phân bổ và làm sai `CodeRange.usedCount`.

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

“Kế thừa lịch sử” trong Excel được thực hiện bằng cách tổng hợp lịch sử lô sản xuất + lô cha khi đọc timeline. Không sao chép bản ghi sự kiện sang từng lô con, vì sao chép sẽ vi phạm tinh thần chỉ-thêm-không-sửa của QTN-08 và tạo nhiều bản lịch sử không còn cùng một chuỗi băm.

### BR-701-10 – Cô lập dữ liệu đối tác

Sau khi tách:

- VT-04 chỉ thấy lô con có `recipientOrganizationId` bằng tổ chức đang đăng nhập;
- VT-04 chỉ được ghi `PROCUREMENT`, `WAREHOUSE_RECEIPT` và sự kiện liên quan cho lô được giao cho mình;
- VT-02/VT-03 của tổ chức nguồn vẫn xem được lô cha, các lô con và timeline;
- tra cứu công khai bằng mã tem tiếp tục hoạt động và tự trỏ tới lô con sau khi cập nhật `trace_codes.shipment_id`.

### BR-701-11 – Phạm vi thu hồi sau khi tách

Theo QTN-24, khi truy vết ảnh hưởng từ lô sản xuất, hệ thống phải lần theo quan hệ lô cha/lô con:

- lô cha `SPLIT` vẫn xuất hiện như nút lineage/audit nhưng không phải lô lưu hành để xử lý thu hồi lần hai;
- mọi lô con lá phát sinh từ lô cha phải nằm trong phạm vi ảnh hưởng mặc định;
- nếu loại một lô con khỏi đề nghị thu hồi, người dùng phải nhập lý do theo QTN-24;
- việc tính tổng phạm vi không được cộng đồng thời số lượng lô cha và lô con.
- Các API tạo yêu cầu thu hồi đơn, từ phản ánh, hàng loạt và API thu hồi trực tiếp đều phải từ chối lô cha `SPLIT`.

## 6. Mô hình trạng thái

```text
CODE_PRINTED (lô cha, toàn bộ tem INACTIVE)
    |
    | POST /api/v1/shipments/{id}/split
    v
SPLIT (lô cha, chỉ còn vai trò lineage/audit)
    |
    +-- CODE_PRINTED (lô con A -> đối tác A) -> ACTIVATED
    +-- CODE_PRINTED (lô con B -> đối tác B) -> ACTIVATED
    +-- CODE_PRINTED (lô con N -> đối tác N) -> ACTIVATED
```

Các chuyển trạng thái bị từ chối:

- `DRAFT`, `ACTIVATED` hoặc `RECALLED` → `SPLIT`;
- `SPLIT` → `SPLIT` lần nữa;
- lô con `CODE_PRINTED` hoặc `ACTIVATED` → `SPLIT` trong phạm vi Story hiện tại.

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
    "status": "CODE_PRINTED",
    "productionLotId": "d4d9330f-c68d-4f02-a333-e4044769cb68",
    "productionLotName": "Thanh long vụ tháng 9",
    "declaredQuantity": 1000,
    "assignableQuantity": 1000,
    "nonInactiveQuantity": 0,
    "availableCodeRange": {
      "fromCode": "HTX00000001",
      "toCode": "HTX00001000",
      "quantity": 1000
    },
    "canSplit": true,
    "blockReasonCode": null,
    "blockMessage": null
  },
  "timestamp": "2026-09-10T08:05:00Z"
}
```

`availableCodeRange` là nullable. Khi lô không còn mã, API trả `null`, `canSplit = false` và frontend phải hiển thị trạng thái không đủ điều kiện thay vì đọc trực tiếp `fromCode`/`toCode`.

Khi lô không đủ điều kiện, endpoint vẫn trả `200` với `canSplit = false` và một trong các mã:

| `blockReasonCode` | Ý nghĩa |
| --- | --- |
| `INVALID_STATUS` | Lô chưa ở trạng thái `CODE_PRINTED` hoặc đã kích hoạt/thu hồi |
| `ALREADY_SPLIT` | Lô cha đã được tách |
| `CHILD_SHIPMENT` | Đây là lô con |
| `INSUFFICIENT_CODES` | Có ít hơn hai mã có thể phân bổ |
| `NON_INACTIVE_CODE_EXISTS` | Có ít nhất một mã không còn ở trạng thái `INACTIVE` |

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
      "fromCode": "HTX00000001",
      "toCode": "HTX00000600",
      "packagingInfo": "Thùng 10 kg"
    },
    {
      "recipientOrganizationId": "1c78c8eb-7da3-4e2c-88a1-75fc73783b45",
      "name": "Lô thanh long giao Minh Long",
      "quantity": 400,
      "fromCode": "HTX00000601",
      "toCode": "HTX00001000",
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
| `allocations[].fromCode` | string | Có | Mã đầu khoảng, phải thuộc lô cha và đang `INACTIVE` |
| `allocations[].toCode` | string | Có | Mã cuối khoảng, phải thuộc lô cha và đang `INACTIVE` |
| `allocations[].packagingInfo` | string | Không | Tối đa 500 ký tự |

Tổng `allocations[].quantity` phải bằng cả `sourceShipment.totalQuantity` và `assignableQuantity` tại thời điểm backend khóa và kiểm tra lại lô cha. Các khoảng mã phải không giao nhau, không có khoảng trống và phủ đúng toàn bộ mã `INACTIVE` của lô cha. Frontend không được coi giá trị preview là nguồn sự thật sau khi POST bắt đầu.

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
      "allocatedQuantity": 1000
    },
    "children": [
      {
        "id": "2ff72c65-ef2e-43bd-bda0-157fce51158f",
        "parentShipmentId": "9d7b499f-ea66-459a-8eb5-2ad81447ae61",
        "name": "Lô thanh long giao An Phú",
        "status": "CODE_PRINTED",
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
        "status": "CODE_PRINTED",
        "recipientOrganization": {
          "id": "1c78c8eb-7da3-4e2c-88a1-75fc73783b45",
          "code": "DN-TM-002",
          "name": "Doanh nghiệp Thu mua Minh Long"
        },
        "totalQuantity": 400,
        "firstCode": "HTX00000601",
        "lastCode": "HTX00001000"
      }
    ],
    "totalChildren": 2,
    "totalAllocatedQuantity": 1000,
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
| 400 | `SPLIT_004` | Tổng số lượng không bằng số lượng/tổng mã của lô cha | `Tổng số lượng phân bổ phải bằng {sourceQuantity}.` |
| 400 | `SPLIT_005` | Tổ chức nhận không phải doanh nghiệp ACTIVE | `Đối tác nhận không hợp lệ hoặc đã ngừng hoạt động.` |
| 400 | `SPLIT_006` | Tổ chức nhận trùng tổ chức nguồn | `Không thể chọn tổ chức nguồn làm đối tác nhận.` |
| 400 | `SPLIT_007` | Đầu/cuối khoảng không thuộc lô cha hoặc không phải mã `INACTIVE` | `Khoảng mã không hợp lệ hoặc chứa mã không thể phân bổ.` |
| 400 | `SPLIT_008` | Các khoảng giao nhau, bỏ sót mã hoặc không phủ toàn bộ tập mã | `Các khoảng mã phải liên tục, không trùng và phủ toàn bộ mã của lô cha.` |
| 400 | `SPLIT_009` | Số mã thực tế trong khoảng khác `quantity` | `Số lượng lô con phải bằng số mã trong khoảng đã chọn.` |
| 401 | `AUTHENTICATION_REQUIRED` | Chưa đăng nhập/hết phiên | `Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn.` |
| 403 | `ACCESS_DENIED` | Không phải VT-02/thiếu permission | `Bạn không có quyền tách lô hàng.` |
| 403 | `CROSS_ORGANIZATION_ACCESS` | Lô cha thuộc tổ chức khác | `Bạn không có quyền tách lô hàng của tổ chức khác.` |
| 403 | `RECIPIENT_MISMATCH` | VT-04 thao tác lô không giao cho tổ chức mình | `Lô hàng không được giao cho tổ chức của bạn.` |
| 404 | `SHIPMENT_NOT_FOUND` | Không tìm thấy lô cha | `Không tìm thấy lô hàng.` |
| 404 | `PARTNER_NOT_FOUND` | Không tìm thấy tổ chức nhận | `Không tìm thấy đối tác nhận.` |
| 409 | `INVALID_SHIPMENT_STATUS` | Lô không phải `CODE_PRINTED`, đã kích hoạt hoặc đã thu hồi | `Chỉ có thể tách lô hàng đã sinh mã và chưa kích hoạt.` |
| 409 | `ALREADY_SPLIT` | Lô đã tách | `Lô hàng đã được tách trước đó.` |
| 409 | `CHILD_SPLIT_NOT_ALLOWED` | Cố tách lô con | `Không hỗ trợ tách tiếp một lô con.` |
| 409 | `TRACE_CODE_STATE_CHANGED` | Số lượng hoặc trạng thái tem thay đổi giữa preview và POST | `Thông tin mã tem đã thay đổi. Vui lòng tải lại thông tin lô.` |

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

> `TC-01` đến `TC-04` ánh xạ trực tiếp bốn Acceptance Criteria trong Excel. Các test còn lại bổ sung bằng chứng kỹ thuật và non-regression.

- [ ] **TC-01 – Tách thành công (`NCL-05-CN-010-TC-01`):** VT-02 tách lô `CODE_PRINTED` có 1.000 mã `INACTIVE` thành hai lô con 500 mã; tạo đúng hai lô con, giữ lineage/lịch sử và lô cha thành `SPLIT`.
- [ ] **TC-02 – Bảo toàn mã:** Tổng mã hợp lệ ở các lô con bằng trước khi tách; không có mã mới/trùng/mất; `CodeRange.usedCount` không đổi.
- [ ] **TC-03 – Tổng phân bổ sai (`NCL-05-CN-010-TC-02`):** Lô cha có 1.000 mã nhưng request phân bổ 1.200 mã bị từ chối; không có dữ liệu nào được tạo.
- [ ] **TC-04 – Tra cứu lô con (`NCL-05-CN-010-TC-03`):** Quét mã lô con thứ nhất chỉ hiển thị lịch sử chung tới điểm tách rồi tiếp tục theo lô con thứ nhất.
- [ ] **TC-05 – Lô đang thu hồi (`NCL-05-CN-010-TC-04`):** Lô `RECALLED` bị chặn và thông báo rõ không thể tách lô đang thu hồi.
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
- [ ] **TC-16 – Khoảng mã:** Chặn khoảng giao nhau, bỏ sót mã, mã ngoài lô cha và trường hợp số mã trong khoảng khác `quantity`.
- [ ] **TC-17 – Trạng thái tem:** Chặn toàn bộ thao tác nếu lô cha có ít nhất một mã không phải `INACTIVE`.
- [ ] **TC-18 – Thu hồi theo QTN-24:** Truy vết từ lô sản xuất đưa mọi lô con lá vào phạm vi mặc định, không đếm kép lô cha.
- [ ] **TC-19 – Payload lớn:** Response tách không trả toàn bộ mã tem; API vẫn đáp ứng với lô có nhiều mã.
- [ ] **TC-20 – Bảo vệ lineage:** Không cho hủy nháp lô con `CODE_PRINTED`; không xóa mã, không giảm `CodeRange.usedCount` và không làm mất quan hệ với lô cha.
- [ ] **TC-21 – Lô cha đã tách:** Không cho tạo/duyệt yêu cầu thu hồi hoặc thu hồi trực tiếp lô cha `SPLIT`; các lô con lá vẫn có thể được chọn theo QTN-24.
- [ ] **TC-22 – Preview không có mã:** Frontend xử lý `availableCodeRange = null` và không phát sinh lỗi render.
- [ ] **TC-23 – Nguồn timeline:** Nhãn sự kiện `PRODUCTION_LOT` hiển thị là kế thừa từ lô sản xuất; `SOURCE_SHIPMENT` hiển thị là kế thừa từ lô cha.
- [ ] **TC-24 – Dữ liệu mở:** Kết xuất chỉ chứa lô lưu hành/lô con, không chứa đồng thời parent `SPLIT` và các lô con.

## 15. Tác động frontend

- Chỉ hiển thị nút **Tách lô** với VT-02 có permission và lô `CODE_PRINTED`, toàn bộ tem còn `INACTIVE`, chưa phải lô con.
- Màn hình gọi `split-preview` trước, hiển thị tổng số lượng, tổng mã và khoảng mã hiện có.
- Cho thêm tối thiểu hai dòng đối tác; không cho chọn trùng.
- Mỗi dòng nhập đối tác, số lượng, mã đầu và mã cuối; hiển thị ngay số mã thực tế trong khoảng.
- Luôn hiển thị tổng đã phân bổ, số còn thiếu/thừa và cảnh báo khoảng trùng/không liên tục.
- Vô hiệu hóa nút xác nhận khi tổng chưa bằng số lượng lô cha hoặc các khoảng chưa phủ đúng toàn bộ mã.
- Sau thành công, chuyển tới chi tiết lô cha hoặc danh sách lô con và invalidate các query shipment, timeline, eligible shipment và báo cáo liên quan.
- Hiển thị trạng thái `SPLIT` bằng nhãn tiếng Việt **Đã tách**.
- Timeline phân biệt sự kiện nguồn/kế thừa và sự kiện riêng của lô con nhưng không làm người dùng hiểu rằng event đã bị sao chép.

## 16. Bảo mật và tính toàn vẹn

- Bắt buộc tenant isolation ở service/repository; không tin `organizationId` từ request.
- Không trả danh sách thành viên hoặc dữ liệu nhạy cảm khi tìm đối tác.
- Khóa lô cha và tập mã tem trong transaction để chống double allocation.
- Không nhận trực tiếp `traceCodeId`. Client gửi `fromCode`/`toCode` theo yêu cầu Excel; backend phải dựng tập mã từ lô cha đã khóa và kiểm tra ownership, trạng thái, giao nhau, khoảng trống và số lượng để ngăn IDOR/phân bổ thiếu-trùng.
- Không sửa `codeValue`, trạng thái khóa/thu hồi hoặc file QR.
- Activity log dùng action `SPLIT_SHIPMENT`, entity là lô cha, metadata chứa danh sách lô con và tổng số lượng nhưng không chứa token/thông tin nhạy cảm.
- Các API chi tiết/timeline phải kiểm tra organization scope, không chỉ dựa vào role.

## 17. Non-regression

- `POST /api/v1/shipments` vẫn tạo shipment và mã tem như hiện tại.
- `POST /api/v1/shipments/{id}/activate` không thay đổi với shipment thường và được dùng riêng cho từng lô con `CODE_PRINTED`.
- QR/code value đã phát hành vẫn tra cứu được sau tách.
- Hạn mức dải mã không thay đổi do thao tác tách.
- Thu hồi theo QTN-24 phải duyệt đúng các lô con lá; lô cha `SPLIT` chỉ giữ vai trò lineage và không bị đếm/thu hồi như một lô đang lưu hành.
- Xuất hồ sơ truy xuất của lô con phải bao gồm lineage nguồn nhưng không lặp chứng từ.
- Shipment chưa tách tiếp tục dùng response cũ nhờ các trường mới là nullable/additive.

## 18. Các quyết định đã chốt và giới hạn phạm vi

Các quyết định nghiệp vụ của CV-01 đã được chốt:

- bắt buộc tách toàn phần vì tổng số lượng và tổng mã lô con phải bằng lô cha;
- người dùng chọn khoảng mã cho từng lô con, backend không tự phân bổ.
- Product Owner chọn phương án A: `Shipment.totalQuantity` luôn bằng số `TraceCode` được sinh; mỗi `quantity` tương ứng một mã và không biểu diễn khối lượng vật lý.

Ba nội dung dưới đây chưa được Jira/Excel yêu cầu và không chặn việc chuyển sang thiết kế dữ liệu. Chúng được giữ ngoài phạm vi NCL-701; nếu Product Owner yêu cầu khác thì phải cập nhật contract trước khi mở rộng implementation:

1. Có cho phép một lô con tiếp tục được tách ở mắt xích sau hay chỉ hỗ trợ một cấp?
2. Đối tác có cần nhận notification ngay khi được phân bổ lô không?
3. Với shipment cũ chưa có `recipientOrganizationId`, có backfill từ sự kiện thu mua hay yêu cầu gán thủ công?

Contract đã chốt **tách toàn phần, mỗi `quantity` tương ứng một mã và người dùng chọn khoảng mã**. Phạm vi implementation hiện tại là **một cấp, không notification và không tự động backfill shipment cũ**.

## 19. Thứ tự triển khai sau khi contract được duyệt

1. `NCL-781` – hoàn tất: đã chốt quy tắc tại mục 5 và giới hạn phạm vi tại mục 18.
2. `NCL-782` – migration, entity, repository và dữ liệu tương thích.
3. Backend của `NCL-785` – preview, partner lookup, split transaction, tenant isolation và timeline.
4. `NCL-783` – giao diện tách lô và tích hợp API.
5. Phần còn lại của `NCL-785` – tích hợp hành trình, public lookup, báo cáo và các luồng VT-04.
6. `NCL-788` – unit test, integration test, runtime/UI test và regression.
