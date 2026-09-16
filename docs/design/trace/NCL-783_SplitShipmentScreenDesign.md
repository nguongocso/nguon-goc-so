# NCL-783 - Thiết kế màn hình tách lô hàng

## 1. Phạm vi

Màn hình dành cho Quản lý HTX (VT-02), mở từ chi tiết lô hàng ở trạng thái `CODE_PRINTED`. Backend vẫn là nơi quyết định quyền `shipment:SPLIT` và điều kiện nghiệp vụ cuối cùng.

## 2. Bố cục

```text
Tách lô hàng
├── Thông tin lô cha
│   ├── Tên lô, lô sản xuất
│   ├── Số tem có thể phân
│   └── Dải mã hiện có
├── Phương án phân bổ
│   ├── Lô con 1: đối tác, tên, số lượng, từ mã, đến mã, quy cách
│   ├── Lô con 2: đối tác, tên, số lượng, từ mã, đến mã, quy cách
│   └── Thêm lô con
└── Thanh tổng hợp cố định
    ├── Đã phân bổ / tổng số tem
    ├── Danh sách lỗi trực tiếp
    └── Hủy | Xác nhận tách lô
```

## 3. Quy tắc tương tác

- Luôn có tối thiểu 2 lô con; không cho xóa khi chỉ còn 2 dòng.
- Mỗi đối tác chỉ xuất hiện trong một lô con.
- Số lượng là số nguyên dương; tổng phải bằng `assignableQuantity`.
- Khoảng mã của từng lô phải khớp số lượng, nối tiếp nhau, không hở và không chồng lấn.
- Lô đầu bắt đầu tại `availableCodeRange.fromCode`; lô cuối kết thúc tại `availableCodeRange.toCode`.
- Khi `canSplit=false`, khóa toàn bộ biểu mẫu và hiển thị `blockMessage`.
- Chỉ bật nút xác nhận khi phương án hợp lệ; trước khi gửi phải có hộp thoại xác nhận thao tác không thể hoàn tác.
- Thành công quay về chi tiết lô cha; lỗi API giữ nguyên dữ liệu đã nhập và hiển thị thông báo.

## 4. Responsive và khả năng truy cập

- Desktop hiển thị tối đa 3 trường trên một hàng; màn hình nhỏ chuyển thành một cột.
- Tất cả trường có nhãn; trường bắt buộc có dấu `*`; nút xóa có `aria-label` theo số thứ tự lô con.
- Lỗi không chỉ biểu diễn bằng màu mà còn có nội dung tiếng Việt cụ thể.

## 5. Giới hạn của task

NCL-783 cung cấp thiết kế, route, biểu mẫu và validation phía frontend theo API contract NCL-701. API thật và giao dịch ghi dữ liệu thuộc NCL-785; kiểm thử giao diện ở task này dùng mock contract.
