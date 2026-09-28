package vn.nguongocso.ai.service;

import java.util.List;

import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Giao diện dịch vụ xử lý hội thoại và tương tác với Trợ lý AI Nguồn Gốc Số.
 */
public interface AiChatService {
    /**
     * Xử lý câu hỏi của người dùng và sinh câu trả lời từ AI theo ngữ cảnh vai trò.
     */
    AiChatResponse chat(AiChatRequest request, CustomUserDetails currentUser);

    /**
     * Lấy danh sách các câu hỏi gợi ý phù hợp với vai trò của người dùng hiện tại.
     */
    List<AiPromptSuggestionResponse> getSuggestedPrompts(CustomUserDetails currentUser);

}
