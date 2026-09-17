# Phạm vi dữ liệu tải sẵn về thiết bị — NCL-10-CN-012

*Task: `NCL-10-CN-012-CV-01` — Chốt phạm vi dữ liệu cần tải sẵn về thiết bị*
*Expected Result theo spec: "Danh mục vật tư, loại hoạt động và danh sách lô kèm thời hạn dữ liệu"*

Nguồn: `Bản sao của Nguồn Gốc Số.xlsx` (sheet "Product Backlog", story `NCL-10-CN-012`,
mô tả chi tiết + Precondition) và sheet "Tasks" dòng `NCL-10-CN-012-CV-01`.

---

## 1. Danh mục được tải sẵn

| # | Danh mục | Nguồn khi có mạng | Nơi lưu trên thiết bị | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Danh sách lô sản xuất (chỉ `APPROVED`, `HARVESTED`) | `GET /api/v1/production-lots` | IndexedDB `lo-cache` (`{id, ten, trangThai}`) | Lọc trạng thái ngay trên client đúng như màn hình ghi nhật ký trực tuyến |
| 2 | Danh mục vật tư đang hoạt động | `GET /api/v1/input-materials?isActive=true` | IndexedDB `vat-tu-cache` (`{id, ten, unit, materialGroup, quarantineDays}`) | Dùng để chọn vật tư khi ngoại tuyến; vẫn cho phép nhập vật tư ngoài danh mục |
| 3 | Loại hoạt động canh tác | Enum trong mã nguồn (`FarmActivityType`) + `utils/farmLogActivity.ts` | IndexedDB `hoat-dong-cache` (mã + nhãn) | Enum cố định nên luôn có sẵn; lưu thêm để phục vụ CV-01 và khi danh mục chuyển sang API |

## 2. Thời hạn dữ liệu (TTL)

- TTL chung: **7 ngày** kể từ lần đồng bộ gần nhất, lưu mốc riêng cho từng danh mục
  trong cửa hàng `cau-hinh` (`lan-dong-bo-lo`, `lan-dong-bo-vat-tu`, `lan-dong-bo-hoat-dong`).
- Hạn hiệu lực hiển thị = hạn **ngắn nhất** trong các danh mục bắt buộc.
- Chưa từng tải hoặc quá 7 ngày ⇒ **chặn ghi ngoại tuyến mới** + banner đỏ yêu cầu kết nối
  mạng (đúng Precondition: "đã đồng bộ danh mục khi còn mạng").
- Khi có mạng trở lại, mở màn hình ghi nhật ký sẽ tự làm mới cả ba danh mục và gia hạn TTL.

## 3. Không tải sẵn (ngoài phạm vi)

| Dữ liệu | Lý do |
| :--- | :--- |
| Mốc canh tác / nhắc việc | Là dữ liệu nghiệp vụ theo thời gian, cần online để đảm bảo đúng trạng thái; thẻ nhắc mốc chỉ hiển thị khi có mạng |
| Nhật ký cũ của lô | Chỉ đọc trên màn hình lịch sử khi có mạng |
| Danh sách lô ở trạng thái khác (`DRAFT`, `PENDING`, `CANCELLED`) | Không hợp lệ để ghi nhật ký (QTN-05 / NCL-02-CN-006) |

## 4. Giới hạn thiết bị

| Hạng mục | Giá trị | Nguồn |
| :--- | :--- | :--- |
| Số bản ghi chờ tối đa | 100 | Hằng số client `MAX_BAN_GHI_CHO` |
| Số ảnh mỗi nhật ký | 5 | `MAX_ATTACHMENTS` + `AttachmentService` |
| Dung lượng mỗi ảnh | ≤ 5 MB (nén client-side trước khi lưu) | `app.upload.farm-log.max-size` = 5242880, `ATTACHMENT_MAX_SIZE` |
| Định dạng tệp | JPG, PNG, PDF | `AttachmentService.ALLOWED_TYPES` |
