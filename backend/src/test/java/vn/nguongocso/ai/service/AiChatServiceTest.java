package vn.nguongocso.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.ai.config.AiProperties;
import vn.nguongocso.ai.dto.request.AiChatMessageDto;
import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.ai.service.impl.AiChatServiceImpl;
import vn.nguongocso.auth.service.CustomUserDetails;

/**
 * Kiểm thử đơn vị cho dịch vụ AiChatService.
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    private AiProperties aiProperties;

    @Mock
    private RestClient restClient;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Resource resource;

    private ObjectMapper objectMapper;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() throws Exception {
        aiProperties = new AiProperties();
        objectMapper = new ObjectMapper();

        when(resourceLoader.getResource("classpath:ai/system_knowledge.txt")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(
                "Bộ tri thức Nguồn Gốc Số kiểm thử.".getBytes(StandardCharsets.UTF_8)));

        aiChatService = new AiChatServiceImpl(aiProperties, restClient, resourceLoader, objectMapper);
        aiChatService.initKnowledgeBase();
    }

    @Test
    @DisplayName("Khi chưa cấu hình API Key, trả về phản hồi hỗ trợ cục bộ thân thiện")
    void testChat_WithoutApiKey_ReturnsLocalFallback() {
        aiProperties.setApiKey(""); // Không có API key

        AiChatRequest request = AiChatRequest.builder()
                .message("Làm sao để tạo lô sản xuất mới?")
                .build();

        AiChatResponse response = aiChatService.chat(request, null);

        assertNotNull(response);
        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Hướng dẫn tạo Lô sản xuất mới"));
        assertFalse(response.getSuggestedQuestions().isEmpty());
    }

    @Test
    @DisplayName("Khi người dùng là nông dân đăng nhập hỏi về nhật ký canh tác")
    void testChat_WithFarmerUser_ReturnsTailoredFallback() {
        aiProperties.setApiKey("");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getFullName()).thenReturn("Nguyễn Văn Nông Dân");
        when(userDetails.getRoleName()).thenReturn("Nông dân");
        when(userDetails.getRoleCode()).thenReturn("FARMER");

        AiChatRequest request = AiChatRequest.builder()
                .message("Hướng dẫn tôi cách ghi nhật ký bón phân")
                .history(List.of(new AiChatMessageDto("user", "Chào trợ lý")))
                .build();

        AiChatResponse response = aiChatService.chat(request, userDetails);

        assertNotNull(response);
        assertTrue(response.getReply().contains("Nguyễn Văn Nông Dân"));
        assertTrue(response.getReply().contains("Quy trình ghi Nhật ký canh tác"));
    }

    @Test
    @DisplayName("Lấy danh sách câu hỏi gợi ý cho vai trò FARMER / VT-03")
    void testGetSuggestedPrompts_Farmer() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-03");

        List<AiPromptSuggestionResponse> suggestions = aiChatService.getSuggestedPrompts(userDetails);

        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.get(0).getCategory().contains("VT-03")
                || suggestions.get(0).getCategory().contains("Nông hộ"));
        assertFalse(suggestions.get(0).getPrompts().isEmpty());
    }

    @Test
    @DisplayName("Lấy danh sách câu hỏi gợi ý cho vai trò COOPERATIVE_MANAGER / VT-02")
    void testGetSuggestedPrompts_CooperativeManager() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-02");

        List<AiPromptSuggestionResponse> suggestions = aiChatService.getSuggestedPrompts(userDetails);

        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.get(0).getCategory().contains("Quản lý Hợp tác xã"));
    }

    @Test
    @DisplayName("Lấy danh sách câu hỏi gợi ý cho vai trò Quản trị viên ADMIN / VT-01")
    void testGetSuggestedPrompts_Admin() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-01");

        List<AiPromptSuggestionResponse> suggestions = aiChatService.getSuggestedPrompts(userDetails);

        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.get(0).getCategory().contains("VT-01")
                || suggestions.get(0).getCategory().contains("Quản trị viên"));
    }

    @Test
    @DisplayName("Lấy danh sách câu hỏi gợi ý cho người tiêu dùng")
    void testGetSuggestedPrompts_GuestUser() {
        List<AiPromptSuggestionResponse> suggestions = aiChatService.getSuggestedPrompts(null);

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals("Câu hỏi phổ biến về Nguồn Gốc Số", suggestions.get(0).getCategory());
    }

    @Test
    @DisplayName("Kiểm tra phân tách danh sách model fallback chain trong AiProperties")
    void testAiProperties_GetModelList() {
        AiProperties properties = new AiProperties();
        properties.setModel("gemini-1.5-flash, gemini-2.0-flash, gemini-1.5-pro");

        List<String> models = properties.getModelList();
        assertEquals(3, models.size());
        assertEquals("gemini-1.5-flash", models.get(0));
        assertEquals("gemini-2.0-flash", models.get(1));
        assertEquals("gemini-1.5-pro", models.get(2));
    }

}
