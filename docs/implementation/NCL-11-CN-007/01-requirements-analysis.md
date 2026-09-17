# NCL-11-CN-007 — Phân tích yêu cầu

## 1. Phạm vi và nguồn đối chiếu

Tài liệu này phân tích User Story **“Cổng nhập kết quả dành cho đơn vị kiểm nghiệm”** ở vai trò Planner. Chưa có mã nguồn production nào được triển khai trong giai đoạn này.

| Nguồn | Phiên bản/định vị đã đọc |
|---|---|
| Backlog Excel | `C:\Users\trann\Downloads\Bản sao của Nguồn Gốc Số (4).xlsx`, cập nhật `2026-09-14 08:49:29`, SHA-256 `587AF51F7A301CCEB6C57C35734DACD901DAC1F60C9A6EABC7AF101CE677E341` |
| Product Backlog | Sheet `Product Backlog (User Stories)`, dòng 113 |
| Acceptance Criteria | Sheet `Acceptance Criteria`, dòng 459–462 |
| Tasks | Sheet `Tasks (Công việc)`, dòng 542–546 |
| Business Rules | Sheet `Business Rules (Quy tắc)`, QTN-14, QTN-20, QTN-21 |
| Vai trò | Sheet `User Roles (Vai trò)`, VT-02 |
| Mã nguồn | Nhánh `develop`, commit `bfc19573cb74c12adce2020c1d244d0e7a90726d` |
| API docs liên quan | `docs/api/certification/inspection-request.md`, `inspection-result.md`, `accreditation-scope.md`, `docs/api/organization/MemberInvitation.md`, `docs/api/publicapi/DataPortalDocumentationAndTestKey.md` |

Không có kết nối Jira khả dụng trong phiên làm việc. File Excel do người dùng cung cấp được dùng làm bản mô tả Jira/backlog ngoại tuyến; các ID Story, Task, AC và QTN dưới đây được giữ nguyên, không suy diễn thêm.

## 2. Parent User Story

| Thuộc tính | Nội dung |
|---|---|
| Story ID | `NCL-11-CN-007` |
| Epic | `NCL-11` — Sơ chế và kiểm nghiệm chất lượng |
| Tiêu đề | Cổng nhập kết quả dành cho đơn vị kiểm nghiệm |
| Vai trò yêu cầu | Quản lý hợp tác xã (`VT-02`) |
| User Story | Là Quản lý hợp tác xã, tôi muốn đơn vị kiểm nghiệm tự nhập kết quả qua một đường dẫn được cấp riêng, để kết quả trong hệ thống là do bên kiểm nghiệm khai chứ không phải do chính hợp tác xã tự gõ vào. |
| Giá trị nghiệp vụ | Loại bỏ điểm yếu về độ tin cậy khi bên hưởng lợi từ kết quả đồng thời là bên nhập dữ liệu; tái sử dụng cơ chế truy cập có thời hạn thay vì tạo vai trò mới. |
| Mô tả | Sinh đường dẫn dùng một lần, có thời hạn, gắn đúng yêu cầu; đơn vị không cần tài khoản, xem chỉ tiêu, nhập kết quả/ngày cấp/ngày hết hiệu lực, tải phiếu và gửi. Hệ thống phân biệt nguồn do đơn vị kiểm nghiệm khai với nguồn do HTX tự nhập. Link hết hạn/đã dùng không mở lại; HTX có thể cấp lại. |
| Điều kiện đầu | Yêu cầu kiểm nghiệm đang chờ kết quả và đơn vị kiểm nghiệm có thông tin liên hệ. |
| Hậu điều kiện | Kết quả được ghi nhận với nguồn nhập rõ ràng là đơn vị kiểm nghiệm hoặc hợp tác xã. |
| Ưu tiên / Sprint / SP | Nên có / Sprint 11 / 2 SP |
| Trạng thái backlog | Chưa ghi trạng thái |

## 3. Toàn bộ Tasks của User Story

| Task ID | Công việc | Kết quả cần đạt | Loại | Trạng thái |
|---|---|---|---|---|
| `NCL-11-CN-007-CV-01` | Chốt luồng cấp đường dẫn nhập kết quả cho đơn vị | Xác định thời hạn, phạm vi và cách đánh dấu nguồn nhập | Phân tích nghiệp vụ | Chưa thực hiện |
| `NCL-11-CN-007-CV-02` | Thiết kế màn hình nhập kết quả cho đơn vị kiểm nghiệm | Giao diện không cần đăng nhập, hiển thị đúng chỉ tiêu cần kiểm | Thiết kế giao diện | Chưa thực hiện |
| `NCL-11-CN-007-CV-03` | Phát triển sinh đường dẫn dùng một lần có thời hạn | Link gắn đúng yêu cầu và hết hiệu lực sau khi dùng | Backend | Chưa thực hiện |
| `NCL-11-CN-007-CV-04` | Phát triển ghi nhận kết quả kèm nguồn nhập | Hồ sơ truy xuất phân biệt nguồn đơn vị kiểm nghiệm/HTX | Backend | Chưa thực hiện |
| `NCL-11-CN-007-CV-05` | Kiểm thử cổng nhập kết quả kiểm nghiệm | Chạy đủ bốn AC, gồm link quá hạn và đã dùng | Bảo mật | Chưa thực hiện |

## 4. Acceptance Criteria

| AC | Given | When | Then | Mức độ |
|---|---|---|---|---|
| `NCL-11-CN-007-TC-01` | Yêu cầu đang chờ kết quả và đã cấp link | Đơn vị mở link, nhập đủ kết quả và gửi | Kết quả được ghi nhận và đánh dấu do đơn vị kiểm nghiệm khai | Cao |
| `NCL-11-CN-007-TC-02` | Link đã quá hạn | Đơn vị mở link | Từ chối và hướng dẫn liên hệ HTX để cấp lại | Cao |
| `NCL-11-CN-007-TC-03` | Link đã dùng để gửi kết quả | Đơn vị mở lại link | Từ chối vì link chỉ dùng một lần | Cao |
| `NCL-11-CN-007-TC-04` | Đơn vị không dùng link, gửi phiếu giấy | HTX tự nhập | Ghi nhận kết quả nhưng hồ sơ ghi nguồn là HTX | Cao |

## 5. Business Rules liên quan

### QTN-14 — Thư mời có thời hạn và dùng một lần

- Chỉ chấp nhận token còn hạn và chưa dùng.
- Token đã dùng hoặc hết hạn bị từ chối và phải cấp lại.
- Áp dụng cho Story theo hình thức **tham chiếu cách làm**, không tái sử dụng bản ghi `invitations` vì đối tượng, dữ liệu và hậu quả nghiệp vụ khác.

### QTN-20 — Khóa truy cập bên thứ ba có thời hạn và hạn mức

- Quy tắc gốc yêu cầu khóa gắn tổ chức, có thời hạn, hạn mức, lịch sử gọi và ngừng hiệu lực khi hết hạn/thu hồi.
- Với Story này, token phải gắn đồng thời với tổ chức và một `inspection_request`; dữ liệu trả về chỉ nằm trong phạm vi request đó.
- Token chỉ hiện dạng thô trong link lúc cấp, lưu DB dưới dạng SHA-256, không ghi log; endpoint public phải có throttling và thông điệp không làm lộ tài nguyên tenant khác.
- Không dùng trực tiếp `partner_api_keys`: API key hiện tại dành cho `/partner/**`, có hạn mức tích hợp theo giờ và phạm vi tổ chức rộng hơn mức cần thiết.

### QTN-21 — Kết quả đạt còn hiệu lực trước khi kích hoạt tem

- Cổng public phải gọi chung validation/chốt trạng thái hiện có; không được tạo nhánh logic kết quả riêng.
- Sau submit, `PASSED`/`FAILED`, cảnh báo hiệu lực và điều kiện kích hoạt tem phải giống luồng HTX nhập tay.
- Nguồn nhập chỉ bổ sung provenance, không làm thay đổi cách tính đạt/không đạt hay thời hạn.

## 6. Vai trò và quyền

- `VT-02` chỉ được cấp/cấp lại link cho yêu cầu thuộc tổ chức hiện tại.
- Đơn vị kiểm nghiệm là tác nhân ngoài hệ thống, **không tạo role hoặc user mới**; quyền chỉ đến từ token hẹp, có hạn và dùng một lần.
- `VT-01`, `VT-03`, `VT-04`, `VT-05` không được cấp link theo phạm vi Story.
- Link public không được phép đọc danh sách request, lot khác, organization ID, user nội bộ hoặc dữ liệu ngoài đúng request đã gắn.

## 7. Dependency và trạng thái thực tế

| Dependency/feature | Backlog | Hiện trạng `develop` | Kết luận |
|---|---|---|---|
| `NCL-11-CN-002` — Tạo yêu cầu kiểm nghiệm | Đã thực hiện | Có controller/service/entity/migration/UI/tests; nhánh liên quan đã nằm trong `develop` | Sẵn sàng tái sử dụng |
| `NCL-11-CN-006` — Danh mục đơn vị kiểm nghiệm | Backlog ghi chú “Đã thực hiện”, 4 Task vẫn “Chưa thực hiện” | `origin/feature/NCL-11-CN-006_testing-units` là ancestor của `develop`; có `testing_units`, scope, UI và tests | Code đã merge; dữ liệu Task trong Excel chưa đồng bộ |
| `NCL-12-CN-001` — Khóa truy cập bên thứ ba | Đã thực hiện | Có token ngẫu nhiên, SHA-256, expiry, rate limit, tenant scope | Tái sử dụng pattern, không tái sử dụng bảng/API |
| `NCL-11-CN-003` — Ghi nhận kết quả | Đã thực hiện | Có batch all-or-nothing, upload file, chốt status, audit, UI | Dependency kỹ thuật trực tiếp |
| `NCL-11-CN-004` — Cảnh báo hết hiệu lực | Đã merge | Ghi kết quả kích hoạt quét hiệu lực | Bắt buộc không hồi quy |
| `NCL-11-CN-005` — Xử lý kết quả không đạt | Đã merge vào `develop` | Cho phép cập nhật request `FAILED`, lịch sử kiểm nghiệm lại | Cần bảo toàn semantics chỉnh sửa/audit |
| `NCL-09-CN-009` — Danh mục chỉ tiêu | Đã merge | Chỉ tiêu snapshot và catalog đang dùng | Không tạo mô hình chỉ tiêu mới |
| Public trace/GS1 dossier | Đã có | Hiện hiển thị kết quả nhưng chưa có nguồn nhập | Phải mở rộng additive để thể hiện provenance |

Không phát hiện dependency trực tiếp nào của `NCL-11-CN-007` còn chưa merge vào `develop`. Nhánh `origin/feature/NCL-12-CN-003-gs1-dossier-export` không phải ancestor nhưng một nhánh biến thể `..._export_dossier_gs1_schema` đã merge; Story này không được phụ thuộc vào phần chênh lệch chưa merge đó.

## 8. Ma trận truy vết yêu cầu

| Yêu cầu | Backend | Frontend | DB/API | Kiểm thử |
|---|---|---|---|---|
| TC-01 submit thành công | Validate token + request + tenant, gọi batch result dùng chung | Trang public nhập đủ chỉ tiêu | Token/link, nguồn nhập, public GET/POST | Service/controller/UI happy path |
| TC-02 hết hạn | Từ chối trước khi đọc dữ liệu request | Trang hết hạn, hướng dẫn liên hệ HTX | `expires_at`, HTTP 410 | Clock cố định + API test |
| TC-03 đã dùng | Atomic consume; lần hai không thể ghi | Trang link đã sử dụng | `status/used_at`, HTTP 410 | Test song song/double submit |
| TC-04 HTX nhập tay | Luồng hiện hữu gắn `COOPERATIVE_MANUAL`, vô hiệu link đang active | Trang nội bộ hiện badge nguồn | Additive `entrySource` | Regression manual flow |
| QTN-20 | Hash token, tenant/request scope, throttling, no secret log | Client public không gửi JWT | No-store, generic errors | invalid token, cross-tenant, 429 |
| QTN-21 | Dùng chung validation/status/expiry alert | Không tự tính status | Không đổi enum/status contract | Passed/failed/expiry regression |

## 9. Quyết định nghiệp vụ được khóa cho thiết kế

1. Thời hạn mặc định là **7 ngày**, cho phép VT-02 chọn từ **1 đến 30 ngày**, kế thừa giới hạn thư mời QTN-14.
2. Mỗi lần cấp/cấp lại sẽ vô hiệu link `ACTIVE` cũ của cùng request; chỉ link mới nhất có hiệu lực.
3. Chỉ cấp link khi request là `PENDING_RESULT`, có `testingUnitId`, và email người nhận hợp lệ. Email được nhập/xác nhận khi cấp link vì `TestingUnit.contactInfo` hiện là chuỗi tự do, không đủ tin cậy để tự tách email.
4. Public submit phải bao phủ toàn bộ chỉ tiêu và giữ semantics all-or-nothing hiện tại.
5. Nguồn nhập lưu theo từng kết quả: `TESTING_UNIT_PORTAL` hoặc `COOPERATIVE_MANUAL`; dữ liệu cũ backfill là `COOPERATIVE_MANUAL`.
6. Bất kỳ lần HTX ghi/cập nhật kết quả thủ công nào cũng gắn nguồn `COOPERATIVE_MANUAL` và vô hiệu link còn active để tránh hai bên cùng ghi.
7. Link public không cho sửa sau submit. Muốn sửa/kiểm nghiệm lại phải đi qua luồng nội bộ và các quy tắc hiện hữu.
8. Phiếu kết quả tiếp tục là tệp tùy chọn theo từng chỉ tiêu, loại JPG/PNG/PDF, tối đa 5 MB, để tương thích contract hiện hành.

## 10. Ngoài yêu cầu hoặc chưa được chứng minh

- Không tạo tài khoản/role cho đơn vị kiểm nghiệm.
- Không làm cổng danh sách nhiều yêu cầu cho đơn vị.
- Không ký số, OCR, xác minh chứng thư số hoặc đối soát mã công nhận.
- Không thay đổi cách tính QTN-21, enum trạng thái request, hay nghiệp vụ kiểm nghiệm lại.
- Không thay đổi cấu trúc `testing_units.contact_info` trong Story này.
