📄 API Docs – So sánh sản lượng giữa các mùa vụ

Tên nhánh: feature/season-yield-comparison

User Story: NCL-10-CN-002

Epic: NCL-10 – Phân tích chuyên sâu, trải nghiệm di động và quản trị dữ liệu

Phụ thuộc: NCL-10-CN-001 (Phân tích sản lượng theo vùng trồng – CropAreaAnalysisController)

1. Thông tin chung

Mục tiêu

Cho phép Cán bộ quản lý ngành (VT-05) chọn từ hai mùa vụ trở lên (xác định bằng năm + mã mùa vụ, có thể kèm vùng trồng cụ thể) để so sánh sản lượng, giúp nhận ra xu hướng tăng/giảm và các bất thường giữa các mùa vụ, phục vụ ra quyết định dựa trên dữ liệu.

Yêu cầu nghiệp vụ

Cán bộ quản lý ngành chọn danh sách năm (years) và danh sách mã mùa vụ (seasons); hệ thống ghép tổ hợp năm × mùa vụ để so sánh, tối thiểu phải tạo ra được 2 tổ hợp; có thể lọc thêm theo vùng trồng (farm area) và/hoặc loại nông sản (product category).

Hệ thống trả về sản lượng tổng hợp theo từng tổ hợp (năm, mùa vụ) đã chọn, cùng chênh lệch (tuyệt đối và phần trăm) so với tổ hợp được chọn làm mốc (baseline – mặc định là tổ hợp sớm nhất trong danh sách).

Dữ liệu trả về ở dạng sẵn sàng cho biểu đồ (chart-ready) để hiển thị cột/đường so sánh giữa các mùa vụ.

Nếu tổ hợp năm × mùa vụ chỉ tạo ra được 1 giá trị (ví dụ chọn 1 năm và 1 mùa vụ), hệ thống từ chối và yêu cầu chọn thêm.

Nếu (các) tổ hợp năm/mùa vụ được chọn không có lô sản xuất nào, hệ thống trả về trạng thái không có dữ liệu để so sánh thay vì báo lỗi.

Dữ liệu so sánh chỉ tính trên phạm vi tổ chức mà người dùng thuộc về, trừ trường hợp VT-01 (Quản trị viên nền tảng) được xem toàn hệ thống.

Đây là API chỉ đọc (read-only), tính trực tiếp từ ProductionLot (không có bảng tổng hợp riêng), tái sử dụng cách xác định mùa vụ đã áp dụng trong NCL-10-CN-001.

2. Vị trí làm việc tại cây thư mục Backend

Lưu ý: Story này không tạo package mới. Chức năng được phát triển mở rộng trên module report của NCL-10-CN-001 (CropAreaAnalysisController / CropAreaAnalysisService) nhằm đảm bảo thống nhất kiến trúc hệ thống, không tạo entity, controller hay service riêng biệt.

3. Cơ sở dữ liệu

Không tạo migration mới. Story tái sử dụng dữ liệu hiện có từ ProductionLot, FarmArea, ProductCategory, Organization — không có bảng Season hay bảng tổng hợp riêng.

Sản lượng được tính trực tiếp từ các lô sản xuất, dựa trên các cột sau của ProductionLot:

actual_quantity — sản lượng thực thu, dùng làm giá trị tổng hợp chính.

expected_quantity — sản lượng dự kiến, có thể hiển thị tham khảo song song nếu cần.

planting_date — dùng để xác định mùa vụ (season code) và năm mùa vụ của lô, theo quy tắc ở mục 5.

harvest_date — mốc thời gian thu hoạch, dùng để lọc/hiển thị bổ sung nếu cần.

status — chỉ tính các lô có status thuộc HARVESTED, PACKAGED, CLOSED (lô đã có sản lượng thực tế).

Vì không có bảng lưu sẵn theo mùa vụ, mỗi lần gọi API sẽ truy vấn ProductionLot theo khoảng năm cần so sánh (đủ bao phủ các years được chọn), rồi nhóm theo (năm mùa vụ, mã mùa vụ) ngay trong service, tái sử dụng logic xác định mùa vụ đã có trong CropAreaAnalysisServiceImpl.

4. API Endpoint

4.1. So sánh sản lượng giữa các mùa vụ

Method: GET

Endpoint:

GET /api/v1/reports/crop-area-analysis/season-yield-comparison

Quyền: report.READ

API dạng GET, không sử dụng Request Body. Toàn bộ tham số được truyền qua @RequestParam.

Query Parameters

Ví dụ request

GET /api/v1/reports/crop-area-analysis/season-yield-comparison?years=2024&years=2025

hoặc

GET /api/v1/reports/crop-area-analysis/season-yield-comparison?years=2024,2025&farmAreaId=2dbf0a

deltaFromBaseline = totalQuantity của tổ hợp − totalQuantity của tổ hợp mốc (baseline). deltaPercent = deltaFromBaseline / totalQuantity(baseline) × 100, làm tròn 2 chữ số thập phân; trả về null nếu totalQuantity(baseline) = 0 (tránh chia cho 0).

Response 200 OK – Có dữ liệu để so sánh (TC-01)

{

"success": true,

"status": 200,

"data": {

"hasData": true,

"message": null,

"baselineYear": 2025,

"baselineSeasonCode": "DONG_XUAN",

"baselineSeasonName": "Vụ Đông Xuân",

"seasons": [

{

"year": 2025,

"seasonCode": "DONG_XUAN",

"seasonName": "Vụ Đông Xuân",

"lotCount": 24,

"totalQuantity": 18500.0,

"delta": 0.0,

"deltaPercent": 0.0

},

{

"year": 2025,

"seasonCode": "HE_THU",

"seasonName": "Vụ Hè Thu",

"lotCount": 20,

"totalQuantity": 19850.0,

"delta": 1350.0,

"deltaPercent": 7.30

},

{

"year": 2026,

"seasonCode": "DONG_XUAN",

"seasonName": "Vụ Đông Xuân",

"lotCount": 27,

"totalQuantity": 21300.0,

"delta": 2800.0,

"deltaPercent": 15.14

}

]

},

"timestamp": "2026-08-02T09:00:00Z"

}

delta = totalQuantity - baseline.totalQuantity

deltaPercent = (delta / baseline.totalQuantity) × 100

Nếu baseline.totalQuantity = 0 thì deltaPercent = null.

Response 200 OK – Không có dữ liệu (TC-03)

{

"success": true,

"status": 200,

"data": {

"hasData": false,

"message": "Không có dữ liệu để so sánh.",

"baselineYear": null,

"baselineSeasonCode": null,

"baselineSeasonName": null,

"seasons": []

},

"timestamp": "2026-08-02T09:00:00Z"

}

Response 403 Forbidden

{

"success": false,

"status": 403,

"message": "Bạn không có quyền truy cập tài nguyên này."

}

5. Business Rules

Người dùng phải có quyền report.READ mới được phép truy cập API. Quyền được kiểm tra thông qua PermissionChecker; mọi lượt truy cập đều được ghi nhận vào ReportAccessLog.

API nhận danh sách years; mỗi lô sản xuất được xác định thuộc một mùa vụ dựa trên ProductionLot.plantingDate, tái sử dụng nguyên vẹn logic trong CropAreaAnalysisServiceImpl:

Tháng 11–4 → DONG_XUAN (Vụ Đông Xuân)

Tháng 5–8 → HE_THU (Vụ Hè Thu)

Tháng 9–10 → THU_DONG (Vụ Thu Đông)

Dữ liệu được truy vấn trực tiếp từ bảng ProductionLot, sau đó được gom nhóm theo tổ hợp (year, seasonCode) trong tầng service để tính toán kết quả so sánh.

Có thể lọc dữ liệu theo farmAreaId và/hoặc productCategoryId. Nếu không truyền các tham số này, hệ thống sẽ tổng hợp trên toàn bộ dữ liệu thuộc phạm vi truy vấn.

Nếu không tìm thấy lô sản xuất phù hợp với điều kiện lọc, API vẫn trả về HTTP 200 OK với:

hasData = false

message = "Không có dữ liệu để so sánh."

seasons = []

Kết quả được sắp xếp theo:

Năm tăng dần.

Thứ tự mùa vụ: DONG_XUAN → HE_THU → THU_DONG.

Baseline được xác định tự động là mùa vụ đầu tiên sau khi sắp xếp. Người dùng không thể chỉ định baseline thông qua request.

Chênh lệch sản lượng được tính như sau:

delta = totalQuantity - baseline.totalQuantity

deltaPercent = (delta / baseline.totalQuantity) × 100

Nếu baseline.totalQuantity = 0 thì deltaPercent = null để tránh chia cho 0.

API là read-only, không tạo, cập nhật hoặc lưu dữ liệu xuống cơ sở dữ liệu. Toàn bộ số liệu được tính toán trực tiếp tại thời điểm gọi API.

Mỗi lần truy cập API đều được ghi nhật ký thông qua ReportAccessLogService, bao gồm cả trường hợp truy cập thành công và bị từ chối quyền.

Quyền yêu cầu: report.READ

Quyền được kiểm tra thông qua:

permissionChecker.check("report", "READ");

Nếu người dùng không có quyền, hệ thống:

ghi nhận log truy cập thất bại qua ReportAccessLogService;

trả về HTTP 403 Forbidden.

Nếu người dùng có quyền, hệ thống:

ghi nhận log truy cập thành công;

tiếp tục xử lý và trả kết quả báo cáo.

6. Repository Methods

ProductionLotRepository (bổ sung)

Service lấy toàn bộ lô trong khoảng năm bao phủ danh sách years được chọn (do một vụ Đông Xuân có thể trải từ tháng 11 năm trước sang tháng 4 năm sau), sau đó nhóm theo (seasonYear, seasonCode) bằng logic xác định mùa vụ đã có, rồi lọc đúng các tổ hợp nằm trong years × seasons mà người dùng yêu cầu.

7. DTOs

Không tạo Request DTO — endpoint là GET thuần, tham số nhận qua @RequestParam và được validate trực tiếp tại controller/service.

SeasonYieldItemResponse

SeasonYieldComparisonResponse

8. Ghi chú Frontend

Màn hình “So sánh sản lượng theo mùa vụ”: cho phép chọn nhiều năm và nhiều mã mùa vụ (multi-select), kèm bộ lọc tùy chọn theo vùng trồng và loại nông sản.

Nếu tổ hợp năm × mùa vụ đã chọn chỉ tạo ra 1 giá trị, vô hiệu hoá nút “So sánh” và hiển thị gợi ý chọn thêm, tránh gọi API rồi mới báo lỗi 400.

Biểu đồ cột hoặc đường thể hiện totalQuantity theo từng tổ hợp (năm, mùa vụ); tổ hợp isBaseline = true có thể đánh dấu riêng (ví dụ viền đậm) làm mốc so sánh.

Hiển thị deltaPercent kèm mũi tên tăng/giảm (▲/▼) và màu sắc tương ứng (xanh tăng, đỏ giảm) cạnh mỗi tổ hợp không phải baseline.

Khi hasData = false, hiển thị trạng thái trống rõ ràng bằng message trả về từ API, kèm gợi ý chọn mùa vụ khác.

Cho phép người dùng đổi baseline bằng cách chọn lại tổ hợp mốc trên biểu đồ, gọi lại API với baselineYear/baselineSeason tương ứng.

9. Kế hoạch triển khai (Backend)

Ánh xạ Test Case ↔ Xử lý backend