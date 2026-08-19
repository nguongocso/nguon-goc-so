API: Hướng dẫn sử dụng trong ứng dụng (In-App User Guidance)

NCL-01-CN-006 — Epic NCL-01: Nền tảng truy xuất nguồn gốc

Nhánh git: feature/NCL-01-CN-006_in-app_guidance

1. Thông tin chung

Mục tiêu

Cho phép người dùng nội bộ (VT-01 → VT-05) xem hướng dẫn sử dụng theo từng màn hình ngay trong ứng dụng, phù hợp với vai trò hiện tại của mình. Nội dung hướng dẫn được khởi tạo tĩnh qua Flyway seed (chưa có quản trị CRUD trong story này) và được lấy về từ một endpoint duy nhất theo mã màn hình (screenKey).

Nhật ký này phục vụ:

•  Hiển thị nút "Hướng dẫn" trên từng màn hình trong ứng dụng, mở drawer chứa các bước sử dụng.

•  Hiển thị nội dung hướng dẫn phù hợp với vai trò đang đăng nhập (role-specific).

•  Nếu chưa có nội dung riêng cho vai trò, hiển thị nội dung chung (GENERAL) của màn hình đó.

•  Nếu chưa có nội dung nào cho màn hình, frontend hiển thị thông báo mặc định "Chưa có hướng dẫn cho màn hình này."

•  Chặn người dùng chưa đăng nhập truy cập nội dung hướng dẫn (401 từ JWT filter).

2. Endpoint

GET /api/v1/help?screenKey={screenKey}

Lấy nội dung hướng dẫn cho một màn hình theo vai trò của người dùng đang đăng nhập.

Tham số:

•  screenKey (bắt buộc): mã định danh màn hình (ví dụ: farm-log-create, dashboard, recall-request-list...).

Không có request body.

3. Điều kiện

Người dùng:

•  Phải đăng nhập (có ACCESS JWT hợp lệ). Mọi đường dẫn /api/v1/** ngoại trừ login/public/partner đều bị chặn bởi SecurityConfig → người dùng chưa đăng nhập nhận 401.

•  Vai trò được suy ra từ JWT; người dùng chỉ nhận được nội dung hướng dẫn của chính vai trò mình (hoặc nội dung chung GENERAL).

Điều kiện về nội dung:

•  Nội dung hướng dẫn (help_content) được seed qua Flyway; story này chưa có quản trị viên thêm/sửa nội dung.

•  Mỗi màn hình có thể có nhiều dòng: dòng riêng cho từng vai trò (role_code = VT-xx) và dòng chung (role_code = GENERAL).

•  Nếu screenKey không tồn tại hoặc rỗng, hệ thống trả về data = null kèm success = true (không phải lỗi) — frontend hiển thị thông báo mặc định.

4. Business Rules

4.1 Xác định vai trò người dùng

Khi gọi GET /api/v1/help, hệ thống lấy CustomUserDetails từ SecurityContextHolder và đọc roleCode (ví dụ VT-03).

4.2 Ưu tiên nội dung hướng dẫn

Thứ tự tra cứu trong HelpServiceImpl:

1.  Tìm nội dung khớp đúng screenKey + roleCode (findByScreenKeyAndRoleCodeOrderBySortOrderAsc).

2.  Nếu không có, tìm nội dung chung screenKey + roleCode = "GENERAL" (findByScreenKeyAndRoleCodeOrderBySortOrderAsc).

3.  Nếu vẫn không có, trả về null (controller trả ApiResult.success với data = null).

Lấy phần tử đầu tiên theo sortOrder tăng dần nếu có nhiều dòng trùng khớp.

4.3 Parse danh sách bước

Trường steps lưu dưới dạng JSON array string trong DB; service parse bằng Jackson ObjectMapper sang List<String>. Nếu parse thất bại, trả về danh sách rỗng và ghi log warning.

5. Response DTO

public class HelpContentResponse {

    private String screenKey;

    private String roleCode;      // "VT-03" hoặc "GENERAL"

    private String title;

    private List<String> steps;

    private String exampleData;   // tuỳ chọn, nullable

}

6. Response

Ví dụ request

GET http://localhost:8080/api/v1/help?screenKey=farm-log-create

HTTP 200 OK — có nội dung riêng theo vai trò

{

  "success": true,

  "status": 200,

  "data": {

    "screenKey": "farm-log-create",

    "roleCode": "VT-03",

    "title": "Hướng dẫn ghi nhật ký canh tác",

    "steps": [

      "Chọn lô sản xuất phù hợp từ danh sách",

      "Nhập hoạt động canh tác (bón phân, tưới, phòng trừ...)",

      "Tải lên ảnh minh chứng",

      "Bấm Lưu để gửi nhật ký cho quản lý duyệt"

    ],

    "exampleData": null

  },

  "timestamp": "2026-08-17T14:00:00.001000000Z"

}

HTTP 200 OK — rơi xuống nội dung chung (GENERAL)

{

  "success": true,

  "status": 200,

  "data": {

    "screenKey": "recall-request-create",

    "roleCode": "GENERAL",

    "title": "Hướng dẫn tạo yêu cầu thu hồi",

    "steps": [

      "Chọn lô sản xuất cần thu hồi",

      "Nhập lý do thu hồi rõ ràng",

      "Đính kèm bằng chứng (ảnh/tài liệu)",

      "Bấm Gửi — yêu cầu chuyển đến quản lý duyệt"

    ],

    "exampleData": null

  },

  "timestamp": "2026-08-17T14:01:00.001000000Z"

}

HTTP 200 OK — chưa có nội dung (data = null)

{

  "success": true,

  "status": 200,

  "data": null,

  "timestamp": "2026-08-17T14:02:00.001000000Z"

}

7. Error Response

401 Unauthorized — chưa đăng nhập

{

  "success": false,

  "status": 401,

  "message": "Unauthorized"

}

400 Bad Request — thiếu tham số screenKey

{

  "success": false,

  "status": 400,

  "message": "Required request parameter 'screenKey' for method parameter type String is not present"

}

8. Backend xử lý

Luồng GET /api/v1/help

Xác thực ACCESS JWT qua JwtAuthenticationFilter (SecurityConfig)

▼

HelpController nhận screenKey

▼

HelpServiceImpl: lấy roleCode từ SecurityContextHolder (SecurityUtils.getCurrentUserDetails)

▼

Tìm nội dung screenKey + roleCode → nếu có trả về

▼

Nếu không → tìm screenKey + "GENERAL" → nếu có trả về

▼

Nếu không → trả về null (frontend hiển thị "Chưa có hướng dẫn cho màn hình này.")

9. Repository

HelpContentRepository

public interface HelpContentRepository extends JpaRepository<HelpContent, UUID> {

    List<HelpContent> findByScreenKeyAndRoleCodeOrderBySortOrderAsc(String screenKey, String roleCode);

    List<HelpContent> findByScreenKeyOrderBySortOrderAsc(String screenKey);

}

Bảng help_content:

•  id CHAR(36) PK

•  screen_key VARCHAR(100) NOT NULL

•  role_code VARCHAR(20) NOT NULL (ví dụ VT-03, GENERAL)

•  title VARCHAR(255) NOT NULL

•  steps TEXT NOT NULL (JSON array string)

•  example_data TEXT NULL

•  sort_order INT NOT NULL DEFAULT 0

•  created_at, updated_at DATETIME NOT NULL

•  Index idx_help_content_screen_role (screen_key, role_code)

10. Phạm vi của Story

Bao gồm

•  Endpoint GET /api/v1/help trả nội dung hướng dẫn theo màn hình + vai trò.

•  Seed nội dung hướng dẫn tiếng Việt cho 45 màn hình (mỗi màn hình ít nhất 1 dòng GENERAL; một số màn hình có thêm dòng riêng theo vai trò).

•  Frontend: nút "Hướng dẫn" trên từng màn hình, drawer hiển thị các bước + ví dụ (nếu có).

•  Fallback "Chưa có hướng dẫn cho màn hình này." khi chưa có nội dung.

Không bao gồm

•  Quản trị CRUD nội dung hướng dẫn (admin quản lý) — được hoãn sang story sau; nội dung khởi tạo tĩnh qua Flyway.

•  Hướng dẫn đa ngôn ngữ — nội dung mặc định bằng tiếng Việt.

11. User Story liên quan

NCL-01-CN-006 — Hướng dẫn sử dụng trong ứng dụng

Là người dùng nội bộ, tôi muốn xem hướng dẫn sử dụng ngay trên màn hình đang dùng, phù hợp với vai trò của mình, để thao tác chính xác mà không cần tài liệu bên ngoài.

Độ ưu tiên: Bắt buộc | Trạng thái: Chưa thực hiện

12. Danh sách công việc

•  Backend: help module (entity, repository, dto, service, controller).

•  Flyway: V28__create_help_content_table.sql (schema) + V29__seed_help_content.sql (seed 45 màn hình).

•  Frontend: types/help.ts, api/helpApi.ts, hooks/useHelp.ts, components/ui/sheet.tsx, components/help/HelpButton.tsx + HelpDrawer.tsx.

•  Tích hợp HelpButton vào 45 trang trong guidance matrix.

13. Test Cases

TC-01: Vai trò có nội dung riêng → trả nội dung riêng (roleCode = VT-xx).

TC-02: Vai trò không có nội dung riêng → rơi xuống nội dung chung GENERAL.

TC-03: Màn hình không có nội dung → trả data = null, frontend hiển thị thông báo mặc định.

TC-04: Người dùng chưa đăng nhập → 401.

TC-05: Thiếu tham số screenKey → 400.

TC-06: steps JSON sai định dạng → trả steps = [] và log warning, không gây lỗi 500.