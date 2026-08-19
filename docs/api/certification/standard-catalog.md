API: Quản lý danh mục tiêu chuẩn chất lượng

NCL-09-CN-002 — Epic NCL-09: Quản trị danh mục, chứng nhận và thành viên nâng cao

Nhánh git: feature/standard-catalog

1. Thông tin chung

Mục tiêu

Cho phép Quản trị viên nền tảng quản lý danh mục tiêu chuẩn chất lượng dùng chung (ví dụ: thực hành nông nghiệp tốt VietGAP, hữu cơ...), làm căn cứ để các tổ chức lựa chọn và gắn tiêu chuẩn cho lô sản xuất cũng như cho chứng nhận của tổ chức mình.

Nhật ký này phục vụ:

Cho phép Quản trị viên nền tảng thêm mới tiêu chuẩn chất lượng kèm mô tả và cơ quan ban hành.

Cho phép Quản trị viên nền tảng cập nhật thông tin tiêu chuẩn đã tồn tại trong danh mục.

Đảm bảo tên tiêu chuẩn là duy nhất trong danh mục, tránh trùng lặp gây nhầm lẫn khi gắn chứng nhận.

Đảm bảo dữ liệu bắt buộc (tên tiêu chuẩn) được kiểm tra trước khi lưu.

Cho phép các tổ chức tra cứu danh sách tiêu chuẩn để lựa chọn tiêu chuẩn phù hợp khi gắn cho lô và chứng nhận.

2. Endpoint

POST /api/v1/standards

Tạo mới một tiêu chuẩn chất lượng trong danh mục dùng chung.

PUT /api/v1/standards/{standardId}

Cập nhật thông tin một tiêu chuẩn chất lượng đã tồn tại trong danh mục.

GET /api/v1/standards

Lấy danh sách tiêu chuẩn chất lượng trong danh mục, hỗ trợ phân trang và lọc theo trạng thái hiệu lực; phục vụ Quản trị viên quản lý danh mục và các tổ chức tra cứu khi gắn tiêu chuẩn cho lô/chứng nhận.

Ghi chú: việc gắn tiêu chuẩn cho lô sản xuất (LotCertification) hoặc cho chứng nhận của tổ chức (Certification) được xử lý ở các story riêng; các endpoint trong tài liệu này chỉ xử lý việc quản trị danh mục tiêu chuẩn dùng chung.

3. Điều kiện

Người dùng:

Phải đăng nhập với vai trò Quản trị viên nền tảng (VT-01) để thêm mới hoặc cập nhật tiêu chuẩn.

Các vai trò nội bộ khác chỉ được xem danh sách tiêu chuẩn (GET) để tra cứu khi gắn cho lô hoặc chứng nhận, không được thêm/sửa.

Nếu người dùng không có quyền quản trị mà thực hiện thêm mới hoặc cập nhật, hệ thống từ chối thao tác.

Điều kiện về tiêu chuẩn:

Tên tiêu chuẩn (name) là bắt buộc khi tạo mới hoặc cập nhật; nếu thiếu, hệ thống yêu cầu bổ sung (TC-02).

Tên tiêu chuẩn phải là duy nhất trong danh mục, không phân biệt hoa/thường; nếu trùng, hệ thống từ chối và yêu cầu đổi tên khác (TC-03).

Tiêu chuẩn mới tạo mặc định is_active = true.

Không xoá cứng tiêu chuẩn khỏi danh mục trong phạm vi story này (xem mục 10).

4. Business Rules

Thứ tự kiểm tra dưới đây theo đúng thứ tự thực thi dự kiến trong StandardServiceImpl.

4.1 Xác định trường thông tin của tiêu chuẩn chất lượng (CV-01)

Hệ thống quản lý tiêu chuẩn chất lượng với các trường: name (bắt buộc), description (mô tả, tuỳ chọn), issuing_body (cơ quan ban hành, tuỳ chọn), is_active (trạng thái hiệu lực, mặc định true).

4.2 Kiểm tra dữ liệu đầu vào khi thêm tiêu chuẩn (TC-01, TC-02)

Khi gọi POST /api/v1/standards, hệ thống kiểm tra name không được rỗng; nếu thiếu hoặc rỗng, hệ thống ném BusinessException: "Tên tiêu chuẩn không được để trống."

4.3 Kiểm tra trùng lặp tên tiêu chuẩn (TC-03)

Hệ thống truy vấn Standard theo name (không phân biệt hoa/thường); nếu đã tồn tại tiêu chuẩn cùng tên, hệ thống ném BusinessException: "Tên tiêu chuẩn đã tồn tại, vui lòng chọn tên khác."

4.4 Lưu tiêu chuẩn mới (TC-01, CV-03)

Nếu hợp lệ, hệ thống tạo bản ghi Standard với name, description, issuing_body, is_active = true, created_at = thời điểm hiện tại, trong một transaction.

4.5 Cập nhật tiêu chuẩn (CV-03)

Khi gọi PUT /api/v1/standards/{standardId}, hệ thống tìm Standard theo id; nếu không tồn tại, trả lỗi không tìm thấy. Hệ thống áp dụng lại kiểm tra tên bắt buộc (mục 4.2) và kiểm tra trùng lặp (mục 4.3, loại trừ chính bản ghi đang sửa) trước khi cập nhật name, description, issuing_body, is_active, updated_at.

4.6 Hiển thị danh mục tiêu chuẩn (CV-04)

Khi gọi GET /api/v1/standards, hệ thống truy vấn Standard, áp dụng lọc isActive nếu có và phân trang, trả về danh sách để Quản trị viên quản lý hoặc để tổ chức tra cứu khi gắn tiêu chuẩn cho lô sản xuất/chứng nhận.

5. Response DTO

public class StandardResponse {

private UUID id;

private String name;

private String description;

private String issuingBody;

private Boolean isActive;

private LocalDateTime createdAt;

private LocalDateTime updatedAt;

}

public class StandardListResponse {

private List<StandardResponse> items;

private int page;

private int size;

private long totalElements;

}

public class StandardRequest {

private String name;

private String description;

private String issuingBody;

private Boolean isActive;

}

Ghi chú: trường isActive trong StandardRequest chỉ có ý nghĩa khi cập nhật (PUT); khi tạo mới, hệ thống luôn đặt is_active = true.

6. Response

Ví dụ request (tạo mới)

POST http://localhost:8080/api/v1/standards

{
  "name": "VietGAP",
  "description": "Thực hành nông nghiệp tốt tại Việt Nam",
  "issuingBody": "Bộ Nông nghiệp và Phát triển nông thôn"
}

HTTP 200 OK — tạo tiêu chuẩn thành công (TC-01)

{
  "success": true,
  "status": 200,
  "data": {
    "id": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "name": "VietGAP",
    "description": "Thực hành nông nghiệp tốt tại Việt Nam",
    "issuingBody": "Bộ Nông nghiệp và Phát triển nông thôn",
    "isActive": true,
    "createdAt": "2026-07-31T03:10:05Z",
    "updatedAt": "2026-07-31T03:10:05Z"
  },
  "timestamp": "2026-07-31T03:10:05.001000000Z"
}

HTTP 200 OK — cập nhật tiêu chuẩn thành công

{
  "success": true,
  "status": 200,
  "data": {
    "id": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
    "name": "VietGAP",
    "description": "Thực hành nông nghiệp tốt và bền vững tại Việt Nam",
    "issuingBody": "Bộ Nông nghiệp và Phát triển nông thôn",
    "isActive": true,
    "createdAt": "2026-07-31T03:10:05Z",
    "updatedAt": "2026-07-31T03:25:00Z"
  },
  "timestamp": "2026-07-31T03:25:00.001000000Z"
}

HTTP 200 OK — danh sách tiêu chuẩn

{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "b2c3d4e5-2222-4a2a-9f3d-1a2b3c4d5e6f",
        "name": "VietGAP",
        "description": "Thực hành nông nghiệp tốt tại Việt Nam",
        "issuingBody": "Bộ Nông nghiệp và Phát triển nông thôn",
        "isActive": true,
        "createdAt": "2026-07-31T03:10:05Z",
        "updatedAt": "2026-07-31T03:10:05Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1
  },
  "timestamp": "2026-07-31T03:30:00.001000000Z"
}

7. Error Response

400 Bad Request — thiếu tên tiêu chuẩn (TC-02)

{
  "success": false,
  "status": 400,
  "message": "Tên tiêu chuẩn không được để trống."
}

409 Conflict — tiêu chuẩn đã tồn tại (TC-03)

{
  "success": false,
  "status": 409,
  "message": "Tên tiêu chuẩn đã tồn tại, vui lòng chọn tên khác."
}

403 Forbidden — không có quyền quản trị

{
  "success": false,
  "status": 403,
  "message": "Bạn không có quyền quản lý danh mục tiêu chuẩn."
}

404 Not Found — tiêu chuẩn không tồn tại

{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy tiêu chuẩn."
}

8. Backend xử lý

Luồng POST /api/v1/standards

Xác thực Quản trị viên nền tảng đang đăng nhập

▼

Kiểm tra name không rỗng → 400 nếu thiếu (TC-02)

▼

Kiểm tra name đã tồn tại trong danh mục → 409 nếu trùng (TC-03)

▼

Tạo bản ghi Standard mới, is_active = true (TC-01)

▼

Trả Response (200) với thông tin tiêu chuẩn vừa tạo

Luồng PUT /api/v1/standards/{standardId}

Kiểm tra Standard tồn tại theo id → 404 nếu không có

▼

Kiểm tra name không rỗng và không trùng với tiêu chuẩn khác → 400/409 nếu vi phạm

▼

Cập nhật name, description, issuing_body, is_active, updated_at

▼

Trả Response (200) với thông tin tiêu chuẩn sau cập nhật

Luồng GET /api/v1/standards

Truy vấn Standard, áp dụng lọc isActive nếu có và phân trang

▼

Trả Response (200) kèm danh sách và tổng số bản ghi

9. Repository

StandardRepository

public interface StandardRepository extends JpaRepository<Standard, UUID> {

Optional<Standard> findByNameIgnoreCase(String name);

Optional<Standard> findByNameIgnoreCaseAndIdNot(String name, UUID id);

Page<Standard> findByIsActive(Boolean isActive, Pageable pageable);

}

Ghi chú: findByNameIgnoreCaseAndIdNot được dùng khi cập nhật, để loại trừ chính bản ghi đang sửa khỏi kiểm tra trùng tên (mục 4.5).

10. Phạm vi của Story

Bao gồm

Thêm mới tiêu chuẩn chất lượng kèm mô tả và cơ quan ban hành.

Cập nhật thông tin tiêu chuẩn đã tồn tại trong danh mục.

Kiểm tra bắt buộc nhập tên và kiểm tra trùng lặp tên tiêu chuẩn.

Xem danh sách tiêu chuẩn, có phân trang và lọc theo trạng thái hiệu lực, phục vụ tổ chức tra cứu khi gắn tiêu chuẩn.

Không bao gồm

Gắn tiêu chuẩn cho lô sản xuất (LotCertification) hoặc cho chứng nhận của tổ chức (Certification) — thuộc các story khác.

Xoá cứng tiêu chuẩn khỏi danh mục.

Phân quyền chi tiết theo từng loại tiêu chuẩn.

Nhập/khai báo hàng loạt (bulk import) tiêu chuẩn từ file.

11. User Story liên quan

NCL-09-CN-002 — Quản lý danh mục tiêu chuẩn chất lượng

Là Quản trị viên nền tảng, tôi muốn quản lý danh mục tiêu chuẩn chất lượng như thực hành nông nghiệp tốt và hữu cơ, để tổ chức gắn tiêu chuẩn cho lô và chứng nhận.

Độ ưu tiên: Quan trọng | Phụ trách: Thành viên một | Trạng thái: Chưa thực hiện

12. Danh sách công việc

Chu kỳ áp dụng: Chu kỳ số một.

13. Test Cases

TC-01: Luồng thành công

TC-02: Thiếu dữ liệu

TC-03: Dữ liệu trùng lặp