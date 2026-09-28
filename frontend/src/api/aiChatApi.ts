import apiClient from './axiosConfig';
import type { ApiResponse } from '@/types/api';
import type {
  AiChatRequest,
  AiChatResponse,
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
 * Đối tượng client API cho các chức năng trợ lý AI.
 */
export const aiChatApi = {
  sendMessage,
  getSuggestedPrompts,
};
