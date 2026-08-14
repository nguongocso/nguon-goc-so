📄 API Docs – Cấu hình phân quyền chi tiết theo tổ chức

Tên nhánh: feature/organization-role-permission-config

User Story: NCL-09-CN-008

Epic: NCL-09 – Quản trị danh mục, chứng nhận và thành viên nâng cao

Quy tắc: QTN-01 – Người dùng chỉ thao tác trong phạm vi tổ chức của mình

Phụ thuộc: NCL-01-CN-003 (Quản lý vai trò & thành viên tổ chức)

1. Thông tin chung

Mục tiêu

Cho phép Quản lý hợp tác xã (VT-02) bật/tắt quyền theo từng nhóm chức năng (resource) cho các vai trò trong tổ chức của mình, thay vì chỉ dùng bộ quyền mặc định theo vai trò toàn hệ thống, để việc phân quyền sát với cách vận hành thực tế của từng hợp tác xã.

Yêu cầu nghiệp vụ

Quản lý HTX (VT-02) có thể xem và cấu hình (bật/tắt) quyền theo từng nhóm chức năng (ví dụ: farm_area, production_lot, chain_event…) cho các vai trò thuộc tổ chức của mình.

Cấu hình chỉ áp dụng trong phạm vi tổ chức thực hiện cấu hình (QTN-01); không ảnh hưởng tới tổ chức khác dù cùng sử dụng chung một vai trò hệ thống (ví dụ VT-03 – Người ghi sự kiện).

Không cấu hình được cho vai trò VT-01 (Quản trị viên nền tảng) vì đây là vai trò vận hành hệ thống, không thuộc một tổ chức cụ thể.

Không cho phép tự hạ quyền của chính vai trò VT-02 (Quản lý HTX) xuống dưới mức tối thiểu để tránh tình trạng tổ chức không còn ai đủ quyền quản trị.

Nếu tổ chức chưa từng cấu hình riêng cho một vai trò, hệ thống áp dụng bộ quyền mặc định của vai trò đó (system default) khi hiển thị và khi áp quyền.

Mỗi lần thay đổi cấu hình quyền đều phải được ghi lại lịch sử (giá trị cũ, giá trị mới, người thực hiện, thời điểm) để phục vụ tra soát.

Chỉ Quản lý HTX (VT-02) của tổ chức mới được thao tác; các vai trò khác (VT-03, VT-04, VT-05, VT-06) không có quyền cấu hình phân quyền.

2. Vị trí làm việc tại cây thư mục Backend

Lưu ý: Role và Permission là danh mục hệ thống đã có (dữ liệu chuẩn hoá dùng chung). Story này chỉ thêm bảng liên kết theo tổ chức (OrganizationRolePermission) để lưu phần override, không sửa cấu trúc Role/Permission gốc.

3. Cơ sở dữ liệu (Migration)

3.1. Bảng organization_role_permissions

Ràng buộc UNIQUE (organization_id, role_id, permission_id) – mỗi tổ hợp tổ chức – vai trò – quyền chỉ có một bản ghi cấu hình.

Nếu chưa có bản ghi tương ứng cho một permission_id, hệ thống hiểu là tổ chức đang dùng giá trị mặc định của vai trò (không phải là “tắt”).

4. API Endpoints

4.1. Lấy danh mục quyền hệ thống (nhóm theo chức năng)

Method: GET

Endpoint: /api/v1/permissions

Quyền: VT-02 (Quản lý HTX) – dùng để dựng màn hình cấu hình

Trả về toàn bộ danh mục quyền hệ thống, nhóm theo resource (nhóm chức năng), làm cơ sở hiển thị các công tắc bật/tắt.

Response 200 OK

4.2. Lấy cấu hình quyền hiện tại của một vai trò trong tổ chức

Method: GET

Endpoint: /api/v1/organizations/{organizationId}/roles/{roleId}/permissions

Quyền: VT-02 (Quản lý HTX) – chỉ xem được cấu hình của tổ chức mình (QTN-01)

Path Parameters

Response 200 OK

Trường isDefault = true nghĩa là tổ chức chưa cấu hình riêng cho quyền này, giá trị isEnabled đang lấy theo mặc định hệ thống của vai trò.

Response 403 Forbidden – không thuộc tổ chức hoặc không phải VT-02

Response 404 Not Found – tổ chức hoặc vai trò không tồn tại

4.3. Cập nhật cấu hình quyền cho một vai trò trong tổ chức

Method: PUT

Endpoint: /api/v1/organizations/{organizationId}/roles/{roleId}/permissions

Quyền: VT-02 (Quản lý HTX) – chỉ cấu hình được cho tổ chức mình (QTN-01)

Request Body

Request Example

Response 200 OK

Response 400 Bad Request – vai trò không được phép cấu hình

Response 400 Bad Request – hạ quyền tối thiểu của VT-02

Response 403 Forbidden – không có quyền cấu hình (TC-02)

Response 404 Not Found – quyền hoặc vai trò không tồn tại

5. Business Rules (QTN-01)

Chỉ Quản lý HTX (VT-02) mới được xem/cấu hình phân quyền, và chỉ trong phạm vi tổ chức mà người đó quản lý; mọi request tới organizationId khác tổ chức của người dùng đều bị từ chối với 403.

Vai trò VT-01 (Quản trị viên nền tảng) không thuộc về một tổ chức cụ thể nên không nằm trong phạm vi cấu hình của story này; request nhắm tới roleId = VT-01 trả về 400.

Vai trò VT-02 (Quản lý HTX) có một tập quyền tối thiểu bắt buộc (ví dụ: quản lý thành viên, quản lý lô sản xuất) không thể bị tắt để tránh tổ chức mất khả năng tự quản trị.

Khi tổ chức chưa cấu hình riêng cho một permission, hệ thống lấy theo bộ quyền mặc định của vai trò (system default), không mặc định là tắt hoàn toàn.

Mỗi lần cập nhật cấu hình phải ghi lại lịch sử vào bảng audit_logs: old_values (trạng thái trước), new_values (trạng thái sau), user_id, organization_id, performed_at, resource_type = "OrganizationRolePermission".

Cấu hình có hiệu lực ngay sau khi lưu và áp dụng cho tất cả thành viên đang giữ vai trò đó trong tổ chức ở lần kiểm tra quyền tiếp theo (không cần đăng nhập lại).

6. Repository Methods

OrganizationRolePermissionRepository

PermissionRepository (tái sử dụng / bổ sung)

7. DTOs

UpdateRolePermissionRequest

PermissionItemResponse

RolePermissionGroupResponse

8. Ghi chú Frontend

Màn hình “Cấu hình phân quyền” trong trang quản trị tổ chức: chọn vai trò (VT-03…VT-06), sau đó hiển thị danh sách nhóm chức năng, mỗi nhóm là một khối với các công tắc bật/tắt theo action.

Quyền đang ở trạng thái mặc định (isDefault = true) hiển thị nhãn phụ “Mặc định hệ thống” để phân biệt với quyền tổ chức đã tự cấu hình.

Vai trò VT-01 không xuất hiện trong danh sách vai trò có thể chọn để cấu hình.

Với vai trò VT-02, các quyền tối thiểu bắt buộc hiển thị công tắc ở trạng thái khoá (disabled), không cho tắt.

Sau khi lưu thành công, hiển thị thông báo xác nhận kèm thời gian và người cập nhật gần nhất.

Có thể bổ sung tab “Lịch sử thay đổi” đọc từ audit_logs để tổ chức tự tra soát (không bắt buộc trong phạm vi story này).

9. Kế hoạch triển khai (Backend)

Ánh xạ Test Case ↔ Xử lý backend