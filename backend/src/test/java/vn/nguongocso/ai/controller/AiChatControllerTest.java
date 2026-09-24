package vn.nguongocso.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.ai.service.AiChatService;
import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Kiểm thử Controller xử lý API Trợ lý AI Nguồn Gốc Số.
 */
@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AiChatService aiChatService;

    @InjectMocks
    private AiChatController aiChatController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiChatController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {
                        return null; // Giả lập người dùng chưa đăng nhập hoặc mock tùy ý
                    }
                })
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat - Trả lời câu hỏi thành công")
    void testChat_Success() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("Làm sao để xuất mã QR cho lô hàng?")
                .build();

        AiChatResponse response = AiChatResponse.builder()
                .reply("Để xuất mã QR, bạn vào phần Quản lý lô hàng...")
                .timestamp(LocalDateTime.now())
                .suggestedQuestions(List.of("Cách in tem mã QR?"))
                .build();

        when(aiChatService.chat(any(AiChatRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reply").value("Để xuất mã QR, bạn vào phần Quản lý lô hàng..."))
                .andExpect(jsonPath("$.data.suggestedQuestions[0]").value("Cách in tem mã QR?"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat - Báo lỗi 400 khi câu hỏi rỗng")
    void testChat_BlankMessage_ReturnsBadRequest() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .message("   ")
                .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/ai/suggested-prompts - Lấy danh sách câu hỏi mẫu thành công")
    void testGetSuggestedPrompts_Success() throws Exception {
        List<AiPromptSuggestionResponse> suggestions = List.of(
                AiPromptSuggestionResponse.builder()
                        .category("Câu hỏi phổ biến")
                        .prompts(List.of("Nguồn Gốc Số là gì?", "Cách quét mã QR?"))
                        .build()
        );

        when(aiChatService.getSuggestedPrompts(any())).thenReturn(suggestions);

        mockMvc.perform(get("/api/v1/ai/suggested-prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].category").value("Câu hỏi phổ biến"))
                .andExpect(jsonPath("$.data[0].prompts[0]").value("Nguồn Gốc Số là gì?"));
    }

}
