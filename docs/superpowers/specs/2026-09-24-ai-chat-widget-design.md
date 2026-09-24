# Thiết Kế Chi Tiết: AI Chatbot Widget Nguồn Gốc Số

- **Ngày tạo:** 2026-09-24
- **Tác giả:** Alolo (Frontend UI/UX)
- **Người phối hợp:** Đoàn (Backend AI Core), Đại (Backend Data Analytics)
- **Tài liệu tham chiếu:** `docs/api/ai/AiChatDocumentation.md`, `Plan_Integrate_AI.md`

---

## 1. Mục tiêu & Phạm vi

Xây dựng Widget Trợ lý AI (Nguồn Gốc Số AI Assistant) dạng nút nổi (Floating Action Button - FAB) tích hợp trên giao diện Web:
1. **Nút nổi cố định (FAB):** Đặt ở góc phải dưới (`fixed bottom-6 right-6 z-50`), màu sắc thương hiệu nông sản (`emerald-600`), hiệu ứng hover/pulse.
2. **Khung hội thoại tương tác (Chat Window):**
   - Kích thước chuẩn Desktop: `380px x 560px`, bo góc `rounded-2xl`, bóng đổ `shadow-2xl`.
   - Responsive trên Mobile (`< 768px`): Chiếm trọn màn hình (`fixed inset-0`) hoặc full-sheet để bà con nông dân dễ thao tác bằng 1 tay.
3. **Thẻ gợi ý câu hỏi nhanh (Quick Suggestion Chips):** Hiển thị các câu hỏi nghiệp vụ tiêu biểu khi khung chat trống hoặc gợi ý tiếp nối từ câu trả lời của AI.
4. **Trình hiển thị định dạng giàu (AiMarkdownRenderer):** Render chữ in đậm, in nghiêng, danh sách gạch đầu dòng, danh sách đánh số, khối trích dẫn, khối mã và đường dẫn liên kết nội bộ (`react-router-dom` `<Link>`) mà không làm tải lại trang.
5. **Tích hợp API Layer & Quản lý Session:**
   - Kết nối trực tiếp API Backend `POST /api/v1/ai/chat` và `GET /api/v1/ai/suggested-prompts`.
   - Tự động đính kèm JWT (nếu đã đăng nhập) hoặc gọi dạng khách (khách tiêu dùng tra cứu công khai).
   - Lưu trữ lịch sử phiên vào `sessionStorage` (`nguongocso_ai_chat_session`) để không mất tin nhắn khi F5 hoặc chuyển trang.

---

## 2. Kiến trúc Thành phần (Component Decomposition)

Phân rã thành các đơn vị độc lập, đơn trách nhiệm:

```text
src/
├── types/
│   └── aiChat.ts                 # Định nghĩa kiểu dữ liệu Message, Request, Response, SuggestionChip
├── api/
│   └── aiChatApi.ts              # Gọi API backend /api/v1/ai/chat và /api/v1/ai/suggested-prompts
├── components/
│   └── ai/
│       ├── AiMarkdownRenderer.tsx # Render Markdown thuần nhẹ (Bold, List, Code, Router Link)
│       ├── QuickPromptChips.tsx   # Danh sách các nút chip câu hỏi gợi ý
│       ├── ChatMessageBubble.tsx  # Bong bóng tin nhắn User/AI kèm avatar, timestamp, nút copy
│       ├── AiChatWidget.tsx       # Component tổng hợp (FAB button + Chat modal window)
│       └── __tests__/
│           ├── AiMarkdownRenderer.test.tsx
│           ├── aiChatApi.test.ts
│           └── AiChatWidget.test.tsx
└── components/layout/
    └── MainLayout.tsx             # Nhúng <AiChatWidget /> vào cuối layout chính
```

---

## 3. Đặc tả Giao diện & Trạng thái (UI States)

### 3.1. Trạng thái Nút nổi (Collapsed / FAB)
- Biểu tượng `Bot` hoặc `Sparkles` từ `lucide-react`.
- Nút tròn `h-14 w-14`, nền gradient xanh ngọc `bg-emerald-600 hover:bg-emerald-700 shadow-lg`.
- Huy hiệu chấm xanh biểu thị trạng thái "Trực tuyến 24/7".
- Tooltip hiển thị "Trợ lý AI Nguồn Gốc Số".

### 3.2. Trạng thái Mở & Khởi tạo (Empty State)
- Header màu trắng tinh tế có logo, tên trợ lý, trạng thái online, nút thu nhỏ/đóng và nút xóa lịch sử.
- Lời chào mừng ban đầu:
  > *"Xin chào! Tôi là Trợ lý AI Nguồn Gốc Số. Tôi có thể hỗ trợ gì cho bạn về quy trình canh tác, quản lý lô hàng hay tra cứu mã QR?"*
- Nhóm Quick Suggestion Chips tải động từ API `/api/v1/ai/suggested-prompts` (hoặc fallback mặc định).

### 3.3. Trạng thái Đang nhập & Chờ phản hồi (Typing / Loading State)
- Hiển thị 3 dấu chấm nhảy nhẹ nhàng (Bouncing Dots Animation) ở góc tin nhắn bot.
- Khóa ô nhập liệu hoặc nút Gửi khi đang chờ phản hồi để chống spam request.

### 3.4. Trạng thái Phản hồi Định dạng Markdown & Action Links
- Tiêu đề, in đậm (`**văn bản**`), danh sách (`1.`, `-`), khối ghi chú (`>`).
- Đường dẫn điều hướng nội bộ (`[Tạo lô sản xuất](/production-lots/create)`): click vào sẽ chuyển trang bằng React Router mà không tải lại toàn bộ ứng dụng.

### 3.5. Trạng thái Ngoại lệ & Ngoại tuyến (Error / Offline)
- Bong bóng thông báo lỗi thân thiện có màu đỏ/cam nhạt.
- Nút "Thử lại" (Retry) để gửi lại câu hỏi vừa lỗi.

---

## 4. Quản lý Dữ liệu & Lưu trữ (Data & Storage Contract)

- **Session Storage Key:** `nguongocso_ai_chat_session_v1`
- **Cấu trúc lưu trữ:**
  ```json
  [
    {
      "id": "msg-1",
      "role": "model",
      "content": "Xin chào! Tôi là Trợ lý AI...",
      "timestamp": "2026-09-24T18:00:00.000Z"
    },
    {
      "id": "msg-2",
      "role": "user",
      "content": "Làm thế nào để tạo lô?",
      "timestamp": "2026-09-24T18:00:05.000Z"
    }
  ]
  ```
- **Nút "Xóa lịch sử":** Xóa cache trong `sessionStorage` và đặt lại khung chat về trạng thái ban đầu.

---

## 5. Chiến lược Kiểm thử (Testing Strategy)

1. **Unit Test API Client (`aiChatApi.test.ts`):** Kiểm tra gọi đúng endpoint `/ai/chat`, `/ai/suggested-prompts` và truyền đúng payload.
2. **Unit Test Markdown Renderer (`AiMarkdownRenderer.test.tsx`):**
   - Render văn bản thường.
   - Render in đậm, in nghiêng, danh sách.
   - Render link nội bộ dạng `<Link to="...">`.
3. **Component Test (`AiChatWidget.test.tsx`):**
   - Kiểm tra mở/đóng widget khi bấm FAB.
   - Hiển thị Quick Suggestion Chips và tự động gửi tin nhắn khi click chip.
   - Nhập câu hỏi và gửi tin nhắn → hiển thị tin nhắn user và bot.
   - Nút sao chép nội dung tin nhắn.
   - Xóa lịch sử trò chuyện.
