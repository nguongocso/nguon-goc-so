package vn.nguongocso.ai.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gợi ý câu hỏi mẫu theo nhóm nghiệp vụ hoặc vai trò người dùng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPromptSuggestionResponse {
    private String category;

    @Builder.Default
    private List<String> prompts = new ArrayList<>();

}
