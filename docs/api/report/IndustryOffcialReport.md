API: Báo cáo cho cán bộ quản lý ngành

NCL-07-CN-003 --- Epic NCL-07: Báo cáo và xuất hồ sơ truy xuất

Nhánh git: feature/industry-official-report

1.  Thông tin chung

Mục tiêu

Cho phép Cán bộ quản lý ngành xem báo cáo tổng hợp sản lượng và lô hàng
(Shipment) theo địa bàn và khoảng thời gian, tổng hợp dữ liệu từ nhiều
tổ chức (HTX, doanh nghiệp), phục vụ giám sát tình hình truy xuất nông
sản của ngành, đồng thời cho phép xuất báo cáo và lưu lại lịch sử xuất.

Nhật ký này phục vụ:

Hỗ trợ Cán bộ quản lý ngành giám sát và tổng hợp số liệu cho quản lý nhà
nước.

Tổng hợp dữ liệu lô hàng và sản lượng của nhiều tổ chức theo địa bàn,
không giới hạn trong phạm vi một tổ chức.

Hiển thị rõ ràng khi địa bàn được chọn chưa có dữ liệu, tránh gây hiểu
nhầm là lỗi hệ thống.

Ghi nhận lịch sử mỗi lần báo cáo được xuất, phục vụ truy vết và kiểm
toán sau này.

2.  Endpoint

GET /api/v1/reports/industry-summary

Xem báo cáo tổng hợp theo địa bàn và khoảng thời gian.

Ghi chú tham số

GET /api/v1/reports/industry-summary/export

Xuất báo cáo tổng hợp (cùng tham số như trên) và ghi lịch sử xuất báo
cáo vào AuditLog.

Cả hai endpoint chỉ hỗ trợ xem/xuất theo địa bàn nhiều tổ chức, không có
thao tác ghi hay sửa dữ liệu nghiệp vụ gốc.

3.  Điều kiện

Người dùng:

Phải đăng nhập và có vai trò VT-05 (Cán bộ quản lý ngành).

Nếu người dùng không có vai trò VT-05, hệ thống từ chối truy cập
(TC-03).

Dữ liệu tổng hợp:

Nếu địa bàn được chọn có tổ chức và lô hàng phát sinh trong khoảng thời
gian, hệ thống trả về số liệu tổng hợp (TC-01).

Nếu địa bàn chưa có dữ liệu phù hợp, hệ thống vẫn trả về thành công
nhưng ở trạng thái rỗng, không phải lỗi (TC-02).

Mỗi lần xuất báo cáo thành công, hệ thống phải lưu lại lịch sử xuất
(TC-04).

4.  Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong
IndustryReportServiceImpl.

4.1 Kiểm tra Role (TC-03)

Hệ thống kiểm tra người dùng hiện tại có vai trò VT-05 (Cán bộ quản lý
ngành) hay không. Đây là bước kiểm tra đầu tiên, thực hiện trước mọi
truy vấn dữ liệu. Nếu người dùng không có vai trò VT-05, hệ thống ném
BusinessException:

"Bạn không có quyền truy cập báo cáo này."

4.2 Kiểm tra tham số đầu vào

Hệ thống kiểm tra region không rỗng và fromDate \<= toDate. Nếu tham số
không hợp lệ, hệ thống ném BusinessException:

"Tham số địa bàn hoặc khoảng thời gian không hợp lệ."

4.3 Tổng hợp dữ liệu theo địa bàn (TC-01, CV-02)

Hệ thống truy vấn tất cả Organization có address khớp với region, sau đó
tổng hợp ProductionLot (sản lượng) và Shipment (số lô hàng) của các tổ
chức đó phát sinh trong khoảng \[fromDate, toDate\].

4.4 Kiểm tra dữ liệu rỗng (TC-02)

Nếu không có tổ chức hoặc không có lô hàng/sản lượng nào phù hợp với
region và khoảng thời gian, hệ thống vẫn trả HTTP 200 nhưng đặt hasData
= false và totalOrganizations/totalShipments/totalQuantity = 0, kèm
thông báo:

"Chưa có dữ liệu cho địa bàn và khoảng thời gian đã chọn."

4.5 Xuất báo cáo và lưu lịch sử (TC-04, CV-03)

Khi gọi endpoint export, sau khi tổng hợp dữ liệu thành công (bước
4.3--4.4), hệ thống sinh file báo cáo theo format yêu cầu và ghi một bản
ghi AuditLog với action = EXPORT_INDUSTRY_REPORT, resource_type =
"IndustryReport", new_values chứa {region, fromDate, toDate, format},
user_id là cán bộ thực hiện xuất.

5.  Response DTO

public class IndustryReportResponse { private String region; private
LocalDate fromDate; private LocalDate toDate; private Boolean hasData;
private Integer totalOrganizations; private Integer totalShipments;
private Double totalQuantity; private
List`<ProductBreakdownItem>`{=html} productBreakdown; private String
message; }

public class ProductBreakdownItem { private String productCategoryName;
private Integer shipmentCount; private Double totalQuantity; }

public class IndustryReportExportResponse { private String fileUrl;
private String format; private LocalDateTime exportedAt; private UUID
auditLogId; }

Ghi chú: productBreakdown được sắp xếp theo totalQuantity giảm dần. Khi
hasData = false, productBreakdown trả về danh sách rỗng.

6.  Response

Ví dụ request

GET
http://localhost:8080/api/v1/reports/industry-summary?region=Ph%C3%BA%20Th%E1%BB%8D&fromDate=2026-01-01&toDate=2026-07-31

HTTP 200 OK --- có dữ liệu (TC-01)

{ "success": true, "status": 200, "data": { "region": "Phú Thọ",
"fromDate": "2026-01-01", "toDate": "2026-07-31", "hasData": true,
"totalOrganizations": 4, "totalShipments": 37, "totalQuantity": 15230.5,
"productBreakdown": \[ { "productCategoryName": "Chè", "shipmentCount":
21, "totalQuantity": 9100.0 }, { "productCategoryName": "Bưởi",
"shipmentCount": 16, "totalQuantity": 6130.5 } \], "message": null },
"timestamp": "2026-07-31T02:05:12.551123400Z" }

HTTP 200 OK --- chưa có dữ liệu (TC-02)

{ "success": true, "status": 200, "data": { "region": "Điện Biên",
"fromDate": "2026-01-01", "toDate": "2026-07-31", "hasData": false,
"totalOrganizations": 0, "totalShipments": 0, "totalQuantity": 0,
"productBreakdown": \[\], "message": "Chưa có dữ liệu cho địa bàn và
khoảng thời gian đã chọn." }, "timestamp":
"2026-07-31T02:05:12.551123400Z" }

HTTP 200 OK --- xuất báo cáo thành công (TC-04)

{ "success": true, "status": 200, "data": { "fileUrl":
"/files/reports/industry-summary-2026-07-31-8f3a.pdf", "format": "PDF",
"exportedAt": "2026-07-31T02:06:40.001000000Z", "auditLogId":
"b3f1a2e0-6c1a-4e2a-9f3d-1a2b3c4d5e6f" }, "timestamp":
"2026-07-31T02:06:40.001000000Z" }

7.  Error Response

403 Forbidden --- không có quyền (TC-03)

{ "success": false, "status": 403, "message": "Bạn không có quyền truy
cập báo cáo này." }

400 Bad Request --- tham số không hợp lệ

{ "success": false, "status": 400, "message": "Tham số địa bàn hoặc
khoảng thời gian không hợp lệ." }

8.  Backend xử lý

Client │ ▼ GET /api/v1/reports/industry-summary(/export) │ ▼ Kiểm tra
Role = VT-05 (Cán bộ quản lý ngành) -\> 403 nếu sai vai trò │ ▼ Kiểm tra
region, fromDate, toDate hợp lệ -\> 400 nếu sai │ ▼ Tìm Organization
theo region (address) │ ▼ Tổng hợp ProductionLot + Shipment theo tổ
chức, trong khoảng thời gian │ ▼ Không có dữ liệu? -\> hasData = false +
message, vẫn trả 200 │ ▼ (Nếu là endpoint export) Sinh file báo cáo +
ghi AuditLog (EXPORT_INDUSTRY_REPORT) │ ▼ Trả Response (200)

9.  Repository

OrganizationRepository

public interface OrganizationRepository extends
JpaRepository\<Organization, UUID\> { List`<Organization>`{=html}
findByAddressContainingIgnoreCase(String region); }

ProductionLotRepository

public interface ProductionLotRepository extends
JpaRepository\<ProductionLot, UUID\> { @Query("SELECT p FROM
ProductionLot p WHERE p.organizationId IN :orgIds" + "AND p.harvestDate
BETWEEN :fromDate AND :toDate") List`<ProductionLot>`{=html}
findByOrganizationIdsAndHarvestDateBetween( @Param("orgIds")
List`<UUID>`{=html} orgIds, @Param("fromDate") LocalDate fromDate,
@Param("toDate") LocalDate toDate); }

ShipmentRepository

public interface ShipmentRepository extends JpaRepository\<Shipment,
UUID\> { @Query("SELECT s FROM Shipment s WHERE s.organizationId IN
:orgIds" + "AND s.createdAt BETWEEN :fromDate AND :toDate")
List`<Shipment>`{=html} findByOrganizationIdsAndCreatedAtBetween(
@Param("orgIds") List`<UUID>`{=html} orgIds, @Param("fromDate")
LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate); }

AuditLogRepository

public interface AuditLogRepository extends JpaRepository\<AuditLog,
UUID\> { AuditLog save(AuditLog auditLog); }

Ghi chú: truy vấn tổng hợp sản lượng theo ProductCategory
(productBreakdown) được thực hiện ở tầng service, nhóm (group by)
ProductionLot.productCategoryId từ tập kết quả
findByOrganizationIdsAndHarvestDateBetween.

10. Phạm vi của Story

Bao gồm

Cán bộ quản lý ngành xem báo cáo tổng hợp sản lượng và lô hàng theo địa
bàn, khoảng thời gian.

Tổng hợp dữ liệu của nhiều tổ chức trong cùng địa bàn.

Hiển thị trạng thái chưa có dữ liệu khi địa bàn không có số liệu phù
hợp.

Xuất báo cáo và lưu lịch sử xuất báo cáo (AuditLog).

Kiểm soát chỉ Cán bộ quản lý ngành (VT-05) mới truy cập được.

Không bao gồm

Bảng điều khiển sản lượng (dashboard) thời gian thực --- thuộc chức năng
khác trong Epic NCL-07.

Thống kê lượt tra cứu (ScanLog) --- thuộc chức năng khác trong Epic
NCL-07.

Báo cáo dành cho Quản lý hợp tác xã hoặc Doanh nghiệp thu mua (phạm vi
một tổ chức) --- thuộc story khác.

Chỉnh sửa hoặc xóa dữ liệu ProductionLot/Shipment --- chỉ xem và xuất
báo cáo.

11. User Story liên quan

NCL-07-CN-003 --- Báo cáo cho cán bộ quản lý ngành

Là Cán bộ quản lý ngành, tôi muốn xem báo cáo tổng hợp sản lượng và lô
hàng theo địa bàn, để giám sát tình hình truy xuất nông sản.

Độ ưu tiên: Bắt buộc \| Phụ trách: Thành viên bốn \| Trạng thái: Chưa
thực hiện \| Tham chiếu: QTN-01

12. Danh sách công việc

Chu kỳ áp dụng: Chu kỳ số bốn.

13. Test Cases

TC-01: Luồng thành công

TC-02: Dữ liệu rỗng

TC-03: Không có quyền

TC-04: Lưu lịch sử
