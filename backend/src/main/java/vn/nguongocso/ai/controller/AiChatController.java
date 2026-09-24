package vn.nguongocso.ai.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.ai.service.AiChatService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;

/**
 * Điều khiển API cung cấp dịch vụ tương tác với Trợ lý AI Nguồn Gốc Số.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {
    private final AiChatService aiChatService;

    /**
     * Gửi câu hỏi đến Trợ lý AI và nhận phản hồi nghiệp vụ theo ngữ cảnh vai trò.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResult<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận câu hỏi AI từ người dùng: [{}], độ dài câu hỏi: {}",
                currentUser != null ? currentUser.getUsername() : "Người tiêu dùng",
                request.getMessage() != null ? request.getMessage().length() : 0);

        AiChatResponse response = aiChatService.chat(request, currentUser);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách câu hỏi mẫu gợi ý phù hợp với vai trò của người dùng hiện tại.
     */
    @GetMapping("/suggested-prompts")
    public ResponseEntity<ApiResult<List<AiPromptSuggestionResponse>>> getSuggestedPrompts(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Lấy danh sách gợi ý câu hỏi AI cho vai trò: [{}]",
                currentUser != null ? currentUser.getRoleCode() : "PUBLIC");

        List<AiPromptSuggestionResponse> suggestions = aiChatService.getSuggestedPrompts(currentUser);
        return ResponseEntity.ok(ApiResult.success(suggestions));
    }
}
