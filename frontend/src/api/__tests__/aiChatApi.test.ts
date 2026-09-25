import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../axiosConfig';
import { aiChatApi } from '../aiChatApi';
import type { AiChatRequest, AiChatResponse, AiPromptSuggestion } from '@/types/aiChat';

vi.mock('../axiosConfig', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('aiChatApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('sendMessage', () => {
    it('gọi apiClient.post với endpoint /ai/chat và trả về dữ liệu phản hồi', async () => {
      const mockRequest: AiChatRequest = {
        message: 'Xin chào trợ lý AI',
        history: [
          { role: 'user', content: 'Câu hỏi trước' },
          { role: 'model', content: 'Câu trả lời trước' },
        ],
      };

      const mockResponseData: AiChatResponse = {
        reply: 'Chào bạn, tôi có thể hỗ trợ gì cho bạn?',
        timestamp: '2026-09-24T10:00:00Z',
        suggestedQuestions: ['Làm thế nào để tạo lô sản xuất?'],
      };

      vi.mocked(apiClient.post).mockResolvedValueOnce({
        data: {
          success: true,
          status: 200,
          message: 'Thành công',
          data: mockResponseData,
        },
      });

      const result = await aiChatApi.sendMessage(mockRequest);

      expect(apiClient.post).toHaveBeenCalledWith('/ai/chat', mockRequest);
      expect(result).toEqual(mockResponseData);
    });
  });

  describe('getSuggestedPrompts', () => {
    it('gọi apiClient.get với endpoint /ai/suggested-prompts và trả về danh sách gợi ý', async () => {
      const mockSuggestions: AiPromptSuggestion[] = [
        {
          category: 'Truy xuất nguồn gốc',
          prompts: ['Kiểm tra mã QR', 'Xem lịch sử lô hàng'],
        },
        {
          category: 'Quản lý nông trại',
          prompts: ['Thêm nhật ký canh tác', 'Đăng ký diện tích trồng'],
        },
      ];

      vi.mocked(apiClient.get).mockResolvedValueOnce({
        data: {
          success: true,
          status: 200,
          message: 'Thành công',
          data: mockSuggestions,
        },
      });

      const result = await aiChatApi.getSuggestedPrompts();

      expect(apiClient.get).toHaveBeenCalledWith('/ai/suggested-prompts');
      expect(result).toEqual(mockSuggestions);
    });
  });
});
