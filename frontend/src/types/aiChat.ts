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

/**
 * Gợi ý tên lô sản xuất cho AI nhận diện.
 */
export interface LotHintItem {
  id: string;
  name: string;
}

/**
 * Gợi ý tên vật tư cho AI nhận diện.
 */
export interface MaterialHintItem {
  id?: string | number;
  name: string;
  unit?: string;
}

/**
 * Yêu cầu phân tích giọng nói nông dân để trích xuất nhật ký canh tác.
 */
export interface AiFarmLogParseRequest {
  voiceText: string;
  availableLots?: LotHintItem[];
  availableMaterials?: MaterialHintItem[];
}

/**
 * Kết quả phân tích thông tin nhật ký canh tác trích xuất từ AI.
 */
export interface AiFarmLogParseResponse {
  productionLotId?: string;
  productionLotName?: string;
  activityType: 'PLANTING' | 'WATERING' | 'FERTILIZING' | 'PESTICIDE' | 'WEEDING' | 'HARVESTING' | 'OTHER';
  activityLabel: string;
  material?: string;
  quantity?: number;
  unit?: string;
  executedDate?: string;
  notes?: string;
  summaryText: string;
  rawVoiceText: string;
  confidence?: number;
}

