package vn.nguongocso.ai.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu trò chuyện và đặt câu hỏi cho Trợ lý AI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequest {
    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String message;

    @Builder.Default
    private List<AiChatMessageDto> history = new ArrayList<>();

}
