## 📌 Mô Tả Thay Đổi (Description)
<!-- Tóm tắt ngắn gọn mục tiêu của PR này, tính năng mới, lỗi đã sửa hoặc cải tiến -->
- Mã User Story / Task (VD: `NCL-08-CN-014`): 
- Mô tả chi tiết:

---

## 🛠 Phân Loại Thay Đổi (Type of Change)
- [ ] 🚀 Tính năng mới (New feature)
- [ ] 🐛 Sửa lỗi (Bug fix)
- [ ] ♻️ Tái cấu trúc mã nguồn (Refactoring)
- [ ] 🎨 Định dạng mã nguồn / Clean Code (Formatting, styling)
- [ ] 📝 Cập nhật tài liệu (Documentation)
- [ ] ⚡ Tối ưu hiệu năng (Performance improvement)

---

## ✅ Bảng Tự Kiểm Tra Của Lập Trình Viên (Self-Review Checklist)
*Vui lòng tích chọn đầy đủ các mục dưới đây trước khi yêu cầu AI Agent và Team Lead review:*

### 1. Nguyên Tắc Chung & Clean Code
- [ ] Đã Format code toàn bộ file (thụt lề 4 spaces cho Java, 2 spaces cho TS/React).
- [ ] Không có dòng nào vượt quá **120 ký tự/dòng** (đã xuống dòng hợp lý trước toán tử / builder / JSX props).
- [ ] Đúng quy chuẩn cách dòng trống (1 dòng giữa các methods, giữa các fields có annotation; **không có 2 dòng trống liên tiếp**; không có dòng trống đầu/cuối ngoặc).
- [ ] **Sạch mã nguồn**: Tuyệt đối không còn `System.out.println()`, `console.log()`, `debugger`, `TODO`/`FIXME`.
- [ ] **Clean Commenting**: Không có comment giải thích cú pháp hiển nhiên (noise comments); không có mã cũ bị comment lại (dead code).
- [ ] Javadoc / JSDoc súc tích ngắn gọn (**≤ 3 - 5 dòng**), bằng tiếng Việt có dấu, không viết javadoc cho getter/setter.

### 2. Backend Checklist (Nếu PR có code Backend)
- [ ] **Entity**: Có Javadoc tiếng Việt; không dùng `@Data`; không chứa logic tính toán; không trả Entity ra ngoài API.
- [ ] **DTO**: Có Javadoc kèm Story ID; đầy đủ validation Jakarta (`@NotNull`, `@Min`,...) kèm `message` tiếng Việt; có cách dòng trống giữa các field có annotation.
- [ ] **Service**: Interface & Impl tách bạch; method ≤ 30 dòng; xử lý exception cụ thể; không có khối `catch` rỗng.
- [ ] **Controller**: Có Swagger `@Operation`; nhận `@Valid`; trả về `ResponseEntity<ApiResponse<T>>`; không chứa business logic.
- [ ] **Bảo mật & Hiệu năng**: Không nối chuỗi SQL; truy vấn danh sách có `Pageable`; dùng `@Transactional(readOnly = true)` cho thao tác đọc.

### 3. Frontend Checklist (Nếu PR có code Frontend)
- [ ] **TypeScript**: Không sử dụng kiểu `any`; Props và State có interface rõ ràng.
- [ ] **Component**: Bố cục chuẩn (Imports -> Props -> Hooks -> State -> Effects -> Handlers -> JSX); kích thước ≤ 200 dòng.
- [ ] **JSX Props**: Component có ≥ 3 props đã được xuống dòng riêng biệt cho từng prop.
- [ ] **Responsive & A11y**: Không vỡ layout hoặc tràn ngang (`overflow-x`) trên mobile; thẻ `<img>` có `alt`.
- [ ] **API Service**: Định kiểu `Promise<ApiResponse<T>>`; có xử lý lỗi tránh treo UI.

### 4. Kiểm Thử Cục Bộ (Local Testing)
- [ ] Backend build thành công (`mvn clean compile`) và test pass 100% (`mvn test`).
- [ ] Frontend build thành công (`npm run build` hoặc `tsc --noEmit`), không có lỗi type.
- [ ] Đã tự kiểm tra trực tiếp giao diện và API trên môi trường local.

---

> 🤖 *Sau khi PR được tạo, **AI PR Reviewer Agent** sẽ tự động phân tích Git Diff và đối chiếu với bộ tiêu chuẩn tại `docs/standards/`. Hãy theo dõi các phản hồi inline của Agent để kịp thời điều chỉnh nếu có vi phạm.*
