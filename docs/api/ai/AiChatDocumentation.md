# API DOCUMENTATION: AI CHATBOT NGUỒN GỐC SỐ

## 1. Tổng quan
Module AI Chatbot cung cấp các điểm cuối API phục vụ giao tiếp trực tiếp với Trợ lý AI Nguồn Gốc Số.
- **Base URL:** `/api/v1/ai`
- **Xác thực:** Hỗ trợ cả người dùng đã đăng nhập (sử dụng Header `Authorization: Bearer <JWT_TOKEN>`) và người tiêu dùng (không gửi token).
- **Định dạng dữ liệu:** `application/json` (UTF-8).
- **Chuẩn phản hồi:** Đóng gói trong cấu trúc `ApiResult<T>`.

---

## 2. Danh sách Endpoints

### 2.1. Gửi câu hỏi đến Trợ lý AI
- **URL:** `/api/v1/ai/chat`
- **Method:** `POST`
- **Mô tả:** Nhận câu hỏi từ người dùng, tự động phân tích ngữ cảnh theo vai trò tài khoản (VT-01 Quản trị viên, VT-02 Quản lý HTX, VT-03 Người ghi sự kiện/Nông dân, VT-04 Doanh nghiệp thu mua, VT-05 Cán bộ quản lý ngành, VT-06 Người tiêu dùng) và trả về câu trả lời hướng dẫn nghiệp vụ chuẩn mực.
- **Yêu cầu quyền:** Công khai (`permitAll`), tự động nhận diện `CustomUserDetails` nếu có JWT.

#### Request Body
```json
{
  "message": "Làm thế nào để tạo lô sản xuất mới?",
  "history": [
    {
      "role": "user",
      "content": "Chào trợ lý ảo!"
    },
    {
      "role": "model",
      "content": "Xin chào! Tôi là Trợ lý AI Nguồn Gốc Số. Tôi có thể hỗ trợ gì cho bạn?"
    }
  ]
}
```

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
|:---|:---|:---:|:---|
| `message` | `String` | Có | Câu hỏi người dùng gửi lên (không được rỗng). |
| `history` | `Array<AiChatMessageDto>` | Không | Danh sách tin nhắn trao đổi trước đó trong phiên hội thoại. |
| `history[].role` | `String` | Có | `"user"` hoặc `"model"` / `"assistant"`. |
| `history[].content` | `String` | Có | Nội dung tin nhắn lịch sử. |

#### Response (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": {
    "reply": "Xin chào! Để tạo lô sản xuất mới, bạn thực hiện theo các bước sau:\n1. Vào mục Quản lý Lô sản xuất...\n2. Bấm nút Tạo lô sản xuất mới...",
    "timestamp": "2026-09-24T10:15:30",
    "suggestedQuestions": [
      "Cách xuất dải mã QR cho lô hàng?",
      "Làm sao để ghi nhật ký canh tác chuẩn VietGAP?"
    ]
  }
}
```

#### Response lỗi (400 Bad Request)
```json
{
  "success": false,
  "status": 400,
  "message": "Nội dung câu hỏi không được để trống"
}
```

---

### 2.2. Lấy danh sách câu hỏi mẫu gợi ý
- **URL:** `/api/v1/ai/suggested-prompts`
- **Method:** `GET`
- **Mô tả:** Trả về danh sách câu hỏi gợi ý phù hợp theo vai trò (Role) của người dùng hiện tại (hoặc nhóm câu hỏi chung cho khách).
- **Yêu cầu quyền:** Công khai (`permitAll`).

#### Response (200 OK)
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "category": "Dành cho Quản lý Hợp tác xã (VT-02)",
      "prompts": [
        "Quy trình tạo lô sản xuất mới từ vùng trồng?",
        "Làm sao để cấp phát dải mã QR cho lô hàng xuất xưởng?",
        "Cách lập biên bản bàn giao điện tử cho doanh nghiệp thu mua?",
        "Hệ thống cảnh báo chứng nhận chất lượng hết hạn trước bao nhiêu ngày?"
      ]
    },
    {
      "category": "Câu hỏi phổ biến về Nguồn Gốc Số",
      "prompts": [
        "Nền tảng Nguồn Gốc Số hỗ trợ những tiêu chuẩn chất lượng nào?",
        "Người tiêu dùng quét mã QR có thể xem được những thông tin gì?",
        "Làm thế nào khi mã QR sản phẩm bị cảnh báo quét bất thường?"
      ]
    }
  ]
}
```

---

## 3. Hướng dẫn tích hợp cho Frontend (Dành cho Đoàn)
- **Tạo API Client (`src/api/aiChatApi.ts`):**
  ```typescript
  import axiosInstance from './axiosInstance';
  import { ApiResult } from '../types/common';

  export interface AiChatMessage {
    role: 'user' | 'model';
    content: string;
  }

  export interface AiChatRequest {
    message: string;
    history?: AiChatMessage[];
  }

  export interface AiChatResponse {
    reply: string;
    timestamp: string;
    suggestedQuestions: string[];
  }

  export interface AiPromptSuggestion {
    category: string;
    prompts: string[];
  }

  export const aiChatApi = {
    chat: (data: AiChatRequest) => 
      axiosInstance.post<ApiResult<AiChatResponse>>('/api/v1/ai/chat', data),
    
    getSuggestedPrompts: () => 
      axiosInstance.get<ApiResult<AiPromptSuggestion[]>>('/api/v1/ai/suggested-prompts')
  };
  ```
- **Hỗ trợ Markdown:** Khi render `reply`, sử dụng thư viện `react-markdown` để hiển thị đẹp định dạng gạch đầu dòng, chữ in đậm và các đường dẫn.
