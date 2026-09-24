/**
 * Kiểu vai trò của người gửi tin nhắn trong hội thoại AI.
 */
export type MessageRole = 'user' | 'model' | 'assistant';

/**
 * Cấu trúc tin nhắn gửi tới hoặc nhận từ mô hình AI trong lịch sử chat.
 */
export interface AiChatMessage {
  role: MessageRole;
  content: string;
}

/**
 * Cấu trúc hiển thị một tin nhắn trong giao diện hộp thoại chat.
 */
export interface ChatMessageItem {
  id: string;
  role: 'user' | 'model';
  content: string;
  timestamp: string;
  isError?: boolean;
}

/**
 * Yêu cầu gửi tin nhắn đến trợ lý AI.
 */
export interface AiChatRequest {
  message: string;
  history?: AiChatMessage[];
}

/**
 * Phản hồi từ trợ lý AI cho tin nhắn đã gửi.
 */
export interface AiChatResponse {
  reply: string;
  timestamp: string;
  suggestedQuestions: string[];
}

/**
 * Gợi ý câu lệnh mẫu theo từng danh mục chức năng.
 */
export interface AiPromptSuggestion {
  category: string;
  prompts: string[];
}
