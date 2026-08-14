\*\*API: Xem lịch sử nhật ký canh tác\*\*



\# 1. Thông tin chung



\*\*Mục tiêu\*\*



Cho phép người dùng xem danh sách các hoạt động canh tác đã được ghi nhận của một Lô sản xuất (ProductionLot) theo thứ tự thời gian.



Lịch sử này phục vụ:



\- Theo dõi quá trình canh tác.

\- Kiểm tra hồ sơ truy xuất nguồn gốc.

\- Chuẩn bị cho việc đính kèm chứng từ ở các chức năng tiếp theo.



\# 2. Endpoint



| \*\*Thuộc tính\*\* | \*\*Giá trị\*\* |

| --- | --- |

| Method | GET |

| URL | /api/v1/farm-logs |

| Authentication | Bearer Token |



\## Query Parameters



| \*\*Parameter\*\* | \*\*Type\*\* | \*\*Required\*\* | \*\*Mô tả\*\* |

| --- | --- | --- | --- |

| productionLotId | UUID | ✓ | Lô sản xuất cần xem nhật ký |

| page | int |  | Số trang, bắt đầu từ 0 (mặc định 0) |

| size | int |  | Số bản ghi mỗi trang (mặc định 10) |



Ghi chú: productionLotId không có annotation validate riêng ở tầng controller/service (không phải @NotNull). Nếu bỏ trống, Spring sẽ tự trả lỗi thiếu tham số bắt buộc (MissingServletRequestParameterException) — message cụ thể phụ thuộc vào global exception handler, có thể không phải "Vui lòng chọn lô sản xuất" như tài liệu cũ; cần xác nhận lại.



\## Ví dụ đường dẫn truy xuất



```

GET http://localhost:8080/api/v1/farm-logs?productionLotId=85d91b0c-c3b8-4c1f-bcb0-2b86737d1406\&page=1\&size=10

```



\# 3. Điều kiện



Người dùng phải:



\- Đăng nhập thành công.

\- Có role VT-02 (Quản lý hợp tác xã / Org Manager).

\- Thuộc cùng Organization với ProductionLot.



Thay đổi so với bản trước: mã nguồn hiện chỉ cho phép role VT-02 (Quản lý hợp tác xã) xem lịch sử nhật ký, không mở cho mọi vai trò trong tổ chức như tài liệu cũ đã mô tả. Role ghi nhật ký (EVENT\_RECODER, mã VT-03) không có quyền xem theo API này.



\# 4. Business Rules



Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi trong FarmLogServiceImpl.getFarmLogsByProductionLot().



\## 4.1 Kiểm tra Role



Chỉ người dùng có role VT-02 (Quản lý hợp tác xã) được phép xem lịch sử nhật ký. Đây là bước kiểm tra đầu tiên, thực hiện trước khi tìm ProductionLot.



\*\*Nếu không đúng role\*\*



"Bạn không có quyền xem lịch sử nhật ký canh tác."



\## 4.2 Kiểm tra tồn tại ProductionLot



Nếu productionLotId không tồn tại, hệ thống ném BusinessException:



"Không tìm thấy lô sản xuất"



\## 4.3 Kiểm tra Organization



Organization của người đăng nhập phải trùng Organization của ProductionLot (thông qua ProductionLot → FarmArea → Organization).



\*\*Nếu không\*\*



"Bạn không thuộc tổ chức của lô sản xuất."



\## 4.4 Truy vấn nhật ký (phân trang)



Trả về danh sách FarmLog của ProductionLot theo phân trang (Page/Pageable), không trả toàn bộ danh sách trong một lần như bản trước.



Tham số phân trang:



\- page: mặc định 0 (nếu không truyền)

\- size: mặc định 10 (nếu không truyền)



Sắp xếp (áp dụng cho mọi trang, cấu hình cố định qua FARM\_LOG\_SORT):



\- executedDate DESC



Nếu cùng ngày:



\- createdAt DESC



\## 4.5 Không có dữ liệu



Nếu chưa có nhật ký, trả về trang rỗng (items rỗng, tổng số phần tử = 0), không trả lỗi. Cấu trúc PageResponse cụ thể xem mục 6.



\# 5. Response DTO



```java

public class FarmLogResponse {



&#x20;   private UUID id;



&#x20;   private UUID productionLotId;



&#x20;   private String productionLotName;



&#x20;   private FarmActivityType activityType;



&#x20;   private String material;



&#x20;   private Double quantity;



&#x20;   private String unit;



&#x20;   private LocalDate executedDate;



&#x20;   private String notes;



&#x20;   private String createdByName;



&#x20;   private LocalDateTime createdAt;



}

```



Ghi chú: dữ liệu được lấy qua FarmLogProjection (interface projection ánh xạ trực tiếp từ JPQL, cùng các field như trên), sau đó map sang FarmLogResponse ở tầng service — không phải map từ entity FarmLog như API tạo nhật ký.



\# 6. Response



Thay đổi so với bản trước: data không còn là một mảng đơn giản mà là đối tượng PageResponse<FarmLogResponse> (phân trang), đã xác nhận qua ví dụ response thực tế. Danh sách bản ghi nằm ở field "items" (không phải "content"), kèm các field "page", "size", "totalElements", "totalPages", "first", "last".



\*\*Ví dụ request\*\*



```

GET http://localhost:8080/api/v1/farm-logs?productionLotId=85d91b0c-c3b8-4c1f-bcb0-2b86737d1406\&page=1\&size=10

```



\*\*HTTP 200 OK\*\*



```json

{

&#x20;   "success": true,

&#x20;   "status": 200,

&#x20;   "data": {

&#x20;       "items": \[

&#x20;           {

&#x20;               "id": "ee31ea2e-470f-460a-9c68-0d11a5105ef4",

&#x20;               "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",

&#x20;               "productionLotName": "Chè Long Cốc",

&#x20;               "activityType": "PESTICIDE",

&#x20;               "material": "Antracol 70WP",

&#x20;               "quantity": 2.0,

&#x20;               "unit": "kg",

&#x20;               "executedDate": "2026-07-07",

&#x20;               "notes": "Phun thuốc phòng bệnh",

&#x20;               "createdByName": "Nguyễn Thị Sinh",

&#x20;               "createdAt": "2026-07-22T14:39:02"

&#x20;           },

&#x20;           {

&#x20;               "id": "12c731e1-29ab-4595-a920-eafd8c3ca5cc",

&#x20;               "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",

&#x20;               "productionLotName": "Chè Long Cốc",

&#x20;               "activityType": "WEEDING",

&#x20;               "material": null,

&#x20;               "quantity": null,

&#x20;               "unit": null,

&#x20;               "executedDate": "2026-07-05",

&#x20;               "notes": "Làm cỏ lần 1",

&#x20;               "createdByName": "Nguyễn Thị Sinh",

&#x20;               "createdAt": "2026-07-22T14:37:46"

&#x20;           },

&#x20;           {

&#x20;               "id": "b91cfd22-bc99-4e17-bf97-4cf58c83c729",

&#x20;               "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",

&#x20;               "productionLotName": "Chè Long Cốc",

&#x20;               "activityType": "WATERING",

&#x20;               "material": "Nước",

&#x20;               "quantity": 500.0,

&#x20;               "unit": "L",

&#x20;               "executedDate": "2026-07-03",

&#x20;               "notes": "Tưới nước lần 1",

&#x20;               "createdByName": "Nguyễn Thị Sinh",

&#x20;               "createdAt": "2026-07-22T14:36:57"

&#x20;           },

&#x20;           {

&#x20;               "id": "af53f725-7478-48c2-98f1-22bac8e63a2d",

&#x20;               "productionLotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",

&#x20;               "productionLotName": "Chè Long Cốc",

&#x20;               "activityType": "PLANTING",

&#x20;               "material": null,

&#x20;               "quantity": null,

&#x20;               "unit": null,

&#x20;               "executedDate": "2026-07-01",

&#x20;               "notes": "Gieo trồng lô sản xuất",

&#x20;               "createdByName": "Nguyễn Thị Sinh",

&#x20;               "createdAt": "2026-07-22T14:36:48"

&#x20;           }

&#x20;       ],

&#x20;       "page": 1,

&#x20;       "size": 10,

&#x20;       "totalElements": 14,

&#x20;       "totalPages": 2,

&#x20;       "first": false,

&#x20;       "last": true

&#x20;   },

&#x20;   "timestamp": "2026-07-22T07:41:34.764994Z"

}

```



\# 7. Error Response



\## 400 Bad Request



Trường hợp thiếu productionLotId (do Spring tự phát sinh khi thiếu tham số bắt buộc — message thực tế cần xác nhận lại):



```json

{

&#x20;   "success": false,

&#x20;   "status": 400,

&#x20;   "message": "Vui lòng chọn lô sản xuất"

}

```



\## 403 Forbidden



Trường hợp sai role (không phải VT-02):



```json

{

&#x20;   "success": false,

&#x20;   "status": 403,

&#x20;   "message": "Bạn không có quyền xem lịch sử nhật ký canh tác."

}

```



Hoặc trường hợp khác Organization:



```json

{

&#x20;   "success": false,

&#x20;   "status": 403,

&#x20;   "message": "Bạn không thuộc tổ chức của lô sản xuất."

}

```



\## 404 Not Found



```json

{

&#x20;   "success": false,

&#x20;   "status": 404,

&#x20;   "message": "Không tìm thấy lô sản xuất"

}

```



\# 8. Backend xử lý



```

Client

&#x20;   │

&#x20;   ▼

GET /api/v1/farm-logs?productionLotId=...\&page=\&size=

&#x20;   │

&#x20;   ▼

Lấy currentUser (SecurityContext)

&#x20;   │

&#x20;   ▼

Kiểm tra Role (VT-02)

&#x20;   │

&#x20;   ▼

Tìm ProductionLot (404 nếu không có)

&#x20;   │

&#x20;   ▼

Kiểm tra Organization

&#x20;   │

&#x20;   ▼

Truy vấn FarmLog theo trang (Pageable, sort executedDate/createdAt DESC)

&#x20;   │

&#x20;   ▼

Map từng FarmLogProjection sang FarmLogResponse

&#x20;   │

&#x20;   ▼

Đóng gói PageResponse \& Trả Response

```



Thay đổi so với bản trước: bổ sung bước kiểm tra Role (VT-02) ngay sau khi lấy thông tin người dùng, trước bước tìm ProductionLot; đồng thời bước truy vấn nay thực hiện theo phân trang (Pageable) thay vì lấy toàn bộ danh sách.



\# 9. Repository



Thay đổi so với bản trước: không dùng derived query đơn giản trả về List, mà dùng JPQL tùy chỉnh trả về Page<FarmLogProjection> (có phân trang), join trực tiếp ProductionLot và User để lấy productionLotName, createdByName mà không cần load toàn bộ entity liên quan.



```java

public interface FarmLogRepository extends JpaRepository<FarmLog, UUID> {



&#x20;   @Query("""

&#x20;       SELECT

&#x20;           fl.id AS id,

&#x20;           pl.id AS productionLotId,

&#x20;           pl.name AS productionLotName,

&#x20;           fl.activityType AS activityType,

&#x20;           fl.material AS material,

&#x20;           fl.quantity AS quantity,

&#x20;           fl.unit AS unit,

&#x20;           fl.executedDate AS executedDate,

&#x20;           fl.notes AS notes,

&#x20;           u.fullName AS createdByName,

&#x20;           fl.createdAt AS createdAt

&#x20;       FROM FarmLog fl

&#x20;       JOIN fl.productionLotId pl

&#x20;       JOIN fl.createdBy u

&#x20;       WHERE pl = :productionLot

&#x20;       """)

&#x20;   Page<FarmLogProjection> findByProductionLot(

&#x20;           ProductionLot productionLot,

&#x20;           Pageable pageable);

}

```



Sắp xếp (executedDate DESC, createdAt DESC) được truyền vào qua Pageable (PageRequest.of(page, size, sort)), không nằm trong câu JPQL.



\# 10. Phạm vi của Story



\*\*Bao gồm\*\*



\- API lấy lịch sử nhật ký theo ProductionLot.

\- Kiểm tra Role (VT-02 – Quản lý hợp tác xã).

\- Kiểm tra Organization.

\- Truy vấn danh sách FarmLog theo phân trang (page, size).

\- Sắp xếp theo thời gian (executedDate, createdAt).

\- Trả về danh sách nhật ký theo trang (PageResponse).



\*\*Không bao gồm\*\*



\- Ghi nhật ký canh tác.

\- Chỉnh sửa nhật ký.

\- Xóa nhật ký.

\- Đính kèm chứng từ.

\- Xem chi tiết một bản ghi nhật ký.



\# 11. User Story liên quan



| Mã | Tên | Mô tả | Vai trò | Lợi ích | Kịch bản chính | Điều kiện tiên quyết | Tiêu chí chấp nhận |

| --- | --- | --- | --- | --- | --- | --- | --- |

| NCL-03-CN-003 | Xem lịch sử nhật ký canh tác | Là Quản lý hợp tác xã, tôi muốn xem toàn bộ nhật ký canh tác của một lô theo thời gian, để kiểm tra tính đầy đủ trước khi tạo lô hàng. | Quản lý hợp tác xã | Giúp rà soát nhật ký trước khi đưa lô vào chuỗi. | Người quản lý mở lô và xem danh sách nhật ký sắp theo thời gian kèm chứng từ. | Có lô sản xuất đã có nhật ký. | Danh sách nhật ký hiển thị đầy đủ theo thời gian. |



Độ ưu tiên: Bắt buộc | Phụ trách: Thành viên hai | Trạng thái: Chưa thực hiện | Tham chiếu: QTN-01



\# 12. Danh sách công việc



| Mã công việc | Tên công việc | Mô tả | Loại công việc | Phụ trách | Mức độ | Trạng thái |

| --- | --- | --- | --- | --- | --- | --- |

| NCL-03-CN-003-CV-01 | Thiết kế màn hình lịch sử nhật ký | Dựng giao diện danh sách nhật ký theo thời gian | Thiết kế giao diện | Thành viên hai | Bắt buộc | Chưa thực hiện |

| NCL-03-CN-003-CV-02 | Phát triển hiển thị nhật ký | Xây chức năng truy vấn và hiển thị nhật ký | Phát triển phần giao diện | Thành viên hai | Bắt buộc | Chưa thực hiện |

| NCL-03-CN-003-CV-03 | Tối ưu truy vấn nhật ký | Bảo đảm truy vấn nhanh khi nhiều bản ghi | Phát triển phần máy chủ | Thành viên ba | Bắt buộc | Chưa thực hiện |

| NCL-03-CN-003-CV-04 | Kiểm tra phân quyền xem | Bảo đảm chỉ tổ chức sở hữu mới xem | Kiểm thử | Thành viên năm | Bắt buộc | Chưa thực hiện |



Chu kỳ áp dụng: Chu kỳ số hai.



\# 13. Test Cases



| Mã TC | Tên | Điều kiện đầu vào | Hành động | Kết quả mong đợi | Dữ liệu liên quan | Mức độ |

| --- | --- | --- | --- | --- | --- | --- |

| NCL-03-CN-003-TC-01 | Luồng thành công | Lô có nhật ký | Người quản lý xem lịch sử | Danh sách nhật ký hiển thị theo thời gian | Danh sách nhật ký, chứng từ | Cao |

| NCL-03-CN-003-TC-02 | Dữ liệu rỗng | Lô chưa có nhật ký | Người quản lý xem lịch sử | Hệ thống hiển thị trạng thái chưa có nhật ký | Danh sách nhật ký | Cao |

| NCL-03-CN-003-TC-03 | Không có quyền | Lô của tổ chức khác | Người dùng xem lịch sử | Hệ thống từ chối truy cập | Tổ chức của lô | Cao |

| NCL-03-CN-003-TC-04 | Sai role | Người dùng có role khác VT-02 (vd: EVENT\_RECODER) | Người dùng gọi API xem lịch sử | Hệ thống từ chối, trả 403 | Role người dùng | Cao |



Ghi chú: TC-01 mô tả kỳ vọng hiển thị kèm chứng từ (attachment) — tính năng đính kèm chứng từ thuộc phạm vi một API/story khác (không nằm trong phạm vi của API xem lịch sử nhật ký này), nên cần xác nhận lại phạm vi hiển thị chứng từ khi thực thi test case. TC-04 được bổ sung theo mã nguồn thực tế: chỉ role VT-02 mới được xem lịch sử, khác với mô tả "không giới hạn role" ở tài liệu trước.



