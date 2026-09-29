import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type {
  AiChatRequest,
  AiChatResponse,
  AiFarmLogParseRequest,
  AiFarmLogParseResponse,
  AiPromptSuggestion,
} from '@/types/aiChat';

/**
 * Gửi tin nhắn trò chuyện tới dịch vụ AI và nhận phản hồi.
 * POST /api/v1/ai/chat
 *
 * @param data Dữ liệu yêu cầu gửi tin nhắn gồm nội dung và lịch sử chat
 * @returns Phản hồi từ trợ lý AI
 */
export const sendMessage = async (
  data: AiChatRequest,
): Promise<AiChatResponse> => {
  const response = await apiClient.post<ApiResponse<AiChatResponse>>(
    '/ai/chat',
    data,
  );
  return response.data.data;
};

/**
 * Lấy danh sách câu hỏi / câu lệnh gợi ý theo chuyên mục cho trợ lý AI.
 * GET /api/v1/ai/suggested-prompts
 *
 * @returns Danh sách gợi ý câu hỏi theo nhóm chức năng
 */
export const getSuggestedPrompts = async (): Promise<AiPromptSuggestion[]> => {
  const response = await apiClient.get<ApiResponse<AiPromptSuggestion[]>>(
    '/ai/suggested-prompts',
  );
  return response.data.data;
};

/**
 * Phân tích giọng nói của nông dân để trích xuất thông tin nhật ký canh tác.
 * POST /api/v1/ai/parse-farm-log
 *
 * @param data Nội dung giọng nói và gợi ý danh sách lô/vật tư
 * @returns Thông tin nhật ký canh tác trích xuất và câu đọc tóm tắt (TTS)
 */
export const parseFarmLogVoice = async (
  data: AiFarmLogParseRequest,
): Promise<AiFarmLogParseResponse> => {
  const response = await apiClient.post<ApiResponse<AiFarmLogParseResponse>>(
    '/ai/parse-farm-log',
    data,
  );
  return response.data.data;
};

/**
 * Đối tượng client API cho các chức năng trợ lý AI.
 */
export const aiChatApi = {
  sendMessage,
  getSuggestedPrompts,
  parseFarmLogVoice,
};

