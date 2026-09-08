# NCL-06-CN-005 - Tra cứu trạng thái xử lý phản ánh đã gửi

## 1. Thông tin công việc

- User Story: `NCL-691` - Tra cứu trạng thái xử lý phản ánh đã gửi.
- Task giao diện: `NCL-921` - Thiết kế màn hình tra cứu trạng thái phản ánh.
- Task triển khai: `NCL-922` - Phát triển sinh mã tra cứu và trang tra cứu phản ánh.
- Nhánh triển khai: `feature/NCL-922-public-feedback-lookup`.
- API và sinh mã tra cứu: `NCL-922`.
- Chống dò mã tra cứu: `NCL-923`.

## 2. Phạm vi công khai

Người gửi phản ánh được tra cứu mà không cần đăng nhập bằng mã nhận được sau khi gửi phản ánh.
Màn hình chỉ được hiển thị:

- Trạng thái xử lý phản ánh.
- Nội dung phản hồi công khai của đơn vị xử lý.

Không trả hoặc hiển thị nội dung xử lý nội bộ, người xử lý, tổ chức xử lý, mã lô, mã tem hay các
thông tin định danh nội bộ khác.

## 3. Mã tra cứu

- Dạng hiển thị: `PA-XXXX-XXXX-XXXX-XXXX`.
- Client chấp nhận chữ thường/chữ hoa và mã có hoặc không có dấu gạch ngang.
- Client trim khoảng trắng và chuyển chữ hoa trước khi gửi.
- Backend lưu hash SHA-256, không lưu mã tra cứu dạng rõ.
- Mã có 16 ký tự ngẫu nhiên từ bảng chữ cái 32 ký tự, tương đương 80 bit entropy.
- Mã chỉ được trả một lần khi tạo phản ánh; hệ thống không có chức năng đọc lại mã rõ.

## 4. Mở rộng response tạo phản ánh

Endpoint hiện hữu:

```http
POST /api/v1/public/production-lots/{productionLotId}/feedbacks
```

Response bổ sung `lookupCode`:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "90fe86ba-596e-4709-b4d5-11f8b9445304",
    "productionLotId": "e28a83cf-3b36-4fd8-b10e-d8d12688f244",
    "status": "NEW",
    "createdAt": "2026-09-08T08:30:00",
    "lookupCode": "PA-7K2M-9Q4X-H8NP-3R5T"
  },
  "timestamp": "2026-09-08T08:30:00Z"
}
```

`lookupCode` không xuất hiện trong API quản trị nội bộ và không được ghi vào log/audit.

## 5. API tra cứu

```http
POST /api/v1/public/product-feedbacks/lookup
Content-Type: application/json
```

Không yêu cầu xác thực.

Request:

```json
{
  "lookupCode": "PA-7K2M-9Q4X-H8NP-3R5T"
}
```

Response `200 OK`:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "status": "IN_PROGRESS",
    "publicResponse": "Đơn vị phụ trách đang xác minh thông tin phản ánh."
  },
  "timestamp": "2026-09-08T08:30:00Z"
}
```

## 6. Ánh xạ trạng thái giao diện

| Giá trị API | Nhãn tiếng Việt |
|---|---|
| `NEW` | Đã tiếp nhận |
| `IN_PROGRESS` | Đang xử lý |
| `ESCALATED_TO_RECALL` | Đã chuyển thu hồi |
| `CLOSED` | Đã đóng |

Nếu `publicResponse` là `null` hoặc rỗng, giao diện hiển thị `Chưa có phản hồi công khai`.

## 7. Trạng thái lỗi

| HTTP | Trạng thái giao diện | Nội dung |
|---:|---|---|
| `404` | Không tìm thấy | Không tìm thấy phản ánh. Vui lòng kiểm tra lại mã tra cứu. |
| `429` | Giới hạn truy cập | Bạn đã tra cứu quá nhiều lần. Vui lòng chờ rồi thử lại. |
| Khác | Lỗi hệ thống | Không thể tra cứu lúc này. Vui lòng thử lại sau. |

- Không tự động gửi lại request khi nhận `429`.
- Error state không được hiển thị đồng thời như một empty state hoặc kết quả có trạng thái mặc định.
- Thông báo `404` không tiết lộ mã có tồn tại ở tenant/tổ chức nào.

## 8. Yêu cầu giao diện NCL-921/NCL-922

- Route công khai: `/public/product-feedbacks/lookup`.
- Có ô nhập mã, hướng dẫn định dạng và nút `Tra cứu trạng thái`.
- Có lối vào từ trang chủ công khai.
- Nút bị khóa trong lúc request đang chạy.
- Kết quả hiển thị nhãn trạng thái và phản hồi công khai, không hiển thị dữ liệu nội bộ.
- Có thao tác `Tra cứu mã khác` để xóa kết quả và đưa focus về ô nhập.
- Bố cục responsive, dùng chung nhận diện Nguồn Gốc Số với các trang công khai hiện hữu.
- Sau khi gửi phản ánh thành công, hiển thị nổi bật mã tra cứu, nút sao chép và cảnh báo người dùng
  lưu mã vì hệ thống không hiển thị lại mã này.

## 9. Phân chia triển khai

NCL-921 triển khai route, bố cục, validation phía client và đầy đủ UI state theo contract này. NCL-922
triển khai sinh mã, lưu hash, trả mã một lần khi tạo phản ánh, endpoint lookup và tích hợp frontend.
Rate limit/chống dò mã không thuộc NCL-922 và được thực hiện trong NCL-923.
