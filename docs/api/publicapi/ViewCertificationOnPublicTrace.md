📄 API Docs – Hiển thị chứng nhận trên trang tra cứu công khai

Tên nhánh: feature/view-certification-on-public-trace

User Story: NCL-09-CN-006

Epic: NCL-09 – Quản trị danh mục, chứng nhận và thành viên nâng cao

Quy tắc: QTN-13 – Chỉ gắn và hiển thị chứng nhận còn hiệu lực

Phụ thuộc: NCL-06-CN-001 (Trang tra cứu công khai), NCL-09-CN-004 (Gắn tiêu chuẩn và chứng nhận cho lô sản xuất)

1. Thông tin chung

Mục tiêu

Cho phép Người tiêu dùng tra cứu (VT-06) khi quét mã QR trên trang tra cứu công khai (/public/trace/{code}) xem được danh sách tiêu chuẩn và chứng nhận đã được gắn cho lô sản xuất tương ứng, kèm trạng thái hiệu lực rõ ràng, nhằm tăng niềm tin và tính minh bạch của sản phẩm.

Yêu cầu nghiệp vụ

Trang tra cứu công khai hiển thị danh sách chứng nhận đã gắn cho lô sản xuất gắn với mã tem (TraceCode) được quét, ở chế độ chỉ xem (read-only).

Mỗi chứng nhận hiển thị kèm trạng thái hiệu lực, được tính tại thời điểm truy vấn: “Còn hiệu lực” nếu expiry_date ≥ ngày hiện tại, “Đã hết hạn” nếu expiry_date < ngày hiện tại (QTN-13).

Nếu lô sản xuất chưa được gắn chứng nhận nào, trang tra cứu hiển thị trạng thái “Chưa có chứng nhận” thay vì báo lỗi hoặc để trống không rõ ràng.

API là public API: không yêu cầu đăng nhập, không áp dụng phân quyền theo VT/tổ chức (QTN-01 không áp dụng ở đây).

Chỉ áp dụng cho mã tem (TraceCode) đang ở trạng thái ACTIVE; mã chưa kích hoạt hoặc đã thu hồi không trả về dữ liệu chứng nhận.

Không cho phép thao tác gắn/gỡ chứng nhận từ trang công khai – chỉ Quản lý HTX (VT-02) mới thao tác được, theo NCL-09-CN-004.

2. Vị trí làm việc tại cây thư mục Backend

Lưu ý: Không tạo package mới; tái sử dụng các entity/repository đã có ở NCL-09-CN-004. Chỉ bổ sung DTO phản hồi công khai và mở rộng service tra cứu công khai đã có ở NCL-06-CN-001.

3. Cơ sở dữ liệu

Không cần migration mới. Story này chỉ đọc dữ liệu từ các bảng certifications và production_lot_certifications đã tạo ở NCL-09-CN-004, thông qua chuỗi liên kết:

Trạng thái hiệu lực (VALID/EXPIRED) được tính động (derived) tại thời điểm truy vấn dựa trên certifications.expiry_date, không lưu cột trạng thái riêng trong DB để tránh dữ liệu bị lệch theo thời gian.

4. API Endpoint

4.1. Lấy danh sách chứng nhận của lô sản xuất trên trang tra cứu công khai

Method: GET

Endpoint: /api/v1/public/trace/{code}/certifications

Quyền: Public – không yêu cầu đăng nhập, không phân quyền VT

Path Parameters

Response 200 OK – có chứng nhận

Ghi chú: certificationId, certificationCode, issuedBy, issueDate là thông tin tham khảo, hiển thị cho cả chứng nhận còn hiệu lực và đã hết hạn để người tiêu dùng có đầy đủ ngữ cảnh; trường status/statusLabel là căn cứ để giao diện hiển thị đúng theo QTN-13 (không hiển thị chứng nhận hết hạn với nhãn “đang đạt”).

Response 200 OK – lô chưa có chứng nhận

Response 404 Not Found – mã tem không tồn tại hoặc chưa kích hoạt

Response 410 Gone – mã tem đã bị thu hồi (Recall)

5. Business Rules (QTN-13)

Không tự động ẩn chứng nhận hết hạn khỏi danh sách trả về; thay vào đó API trả về đầy đủ các chứng nhận đã gắn cho lô kèm trạng thái tính theo thời gian thực (VALID/EXPIRED), để đảm bảo tính minh bạch cho người tiêu dùng.

Chứng nhận hết hạn tuyệt đối không được gắn nhãn/label thể hiện “đang đạt chuẩn”; giao diện phải hiển thị rõ statusLabel = “Đã hết hạn”.

Việc gắn chứng nhận mới vào lô vẫn tuân theo quy tắc chỉ cho gắn chứng nhận còn hiệu lực tại thời điểm gắn (đã áp dụng ở NCL-09-CN-004); story này chỉ xử lý phần hiển thị.

Trạng thái hiệu lực luôn được tính lại (derived) tại mỗi lần gọi API, không cache theo lô để tránh hiển thị sai khi chứng nhận vừa hết hạn.

Không ghi activity_logs cho hành động xem trang tra cứu công khai (đây là thao tác đọc, ẩn danh, khối lượng lớn); có thể ghi nhận qua TraceCodeScanLog nếu cần thống kê lượt quét (đã có ở NCL-06-CN-001).

Chỉ trả dữ liệu khi TraceCode.status = ACTIVE; nếu INACTIVE trả 404, nếu RECALLED trả 410 kèm cảnh báo thu hồi.

6. Repository Methods

ProductionLotCertificationRepository (bổ sung)

TraceCodeRepository (tái sử dụng, có sẵn từ NCL-06-CN-001)

7. DTOs

PublicCertificationResponse

PublicLotCertificationsResponse

CertificationStatus (enum)

8. Ghi chú Frontend

Trang tra cứu công khai (/public/trace/{code}): thêm khối “Tiêu chuẩn & Chứng nhận” bên dưới thông tin lô sản xuất.

Chứng nhận “Còn hiệu lực” hiển thị bằng huy hiệu (badge) màu xanh; chứng nhận “Đã hết hạn” hiển thị bằng huy hiệu màu xám kèm gạch chữ hoặc icon cảnh báo nhẹ, tránh gây hiểu nhầm là sản phẩm không an toàn.

Khi hasCertification = false, hiển thị trạng thái trống rõ ràng, ví dụ: “Lô sản xuất này chưa được gắn chứng nhận.”

Không hiển thị bất kỳ nút thao tác (gắn/gỡ/sửa) nào trên trang công khai.

Xử lý các mã lỗi 404/410 bằng trang thông báo phù hợp (mã không hợp lệ / sản phẩm đã bị thu hồi).

9. Kế hoạch triển khai (Backend)

Ánh xạ Test Case ↔ Xử lý backend