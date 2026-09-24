package vn.nguongocso.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import java.util.Set;
import java.util.UUID;

import vn.nguongocso.ai.config.AiProperties;
import vn.nguongocso.ai.dto.query.CertificationStatusDto;
import vn.nguongocso.ai.dto.query.OrganizationAnalyticsDataDto;
import vn.nguongocso.ai.dto.query.ProductionLotSummaryDto;
import vn.nguongocso.ai.dto.request.AiChatMessageDto;
import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.ai.service.impl.AiChatServiceImpl;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;

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

    @Mock
    private AiDataQueryService aiDataQueryService;

    @Mock
    private AreaScopeService areaScopeService;

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

        aiChatService = new AiChatServiceImpl(
                aiProperties, restClient, resourceLoader, objectMapper, aiDataQueryService, areaScopeService);
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

    @Test
    @DisplayName("TASK-AI-06: Nhận diện ý định thống kê và inject số liệu thực tế vào câu trả lời")
    void testChat_WithAnalyticsIntent_InjectsRealData() {
        UUID orgId = UUID.randomUUID();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getFullName()).thenReturn("Trần Văn Quản Lý");
        when(userDetails.getRoleName()).thenReturn("Quản lý HTX");
        when(userDetails.getRoleCode()).thenReturn("VT-02");
        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(userDetails.getOrganizationName()).thenReturn("HTX Nông Sản An Giang");

        OrganizationAnalyticsDataDto analytics = OrganizationAnalyticsDataDto.builder()
                .organizationId(orgId)
                .organizationName("HTX Nông Sản An Giang")
                .organizationCode("HTX-AG")
                .lotSummary(ProductionLotSummaryDto.builder()
                        .activeLotsCount(15)
                        .harvestedLotsCount(7)
                        .totalAreaHectares(62.5)
                        .upcomingHarvestLotNames(List.of("Lô Lúa ST25 (LOT-ST25)"))
                        .build())
                .expiringCertifications(List.of(CertificationStatusDto.builder()
                        .code("VG-2026-999")
                        .name("Chứng nhận VietGAP Lúa")
                        .standardName("VietGAP")
                        .daysRemaining(20)
                        .build()))
                .build();

        when(aiDataQueryService.getFullOrganizationAnalytics(orgId)).thenReturn(analytics);

        AiChatRequest request = AiChatRequest.builder()
                .message("Thống kê số lượng lô đang canh tác và diện tích của hợp tác xã")
                .build();

        AiChatResponse response = aiChatService.chat(request, userDetails);

        assertNotNull(response);
        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("15 lô"), "Câu trả lời phải chứa đúng số lô đang canh tác (15)");
        assertTrue(response.getReply().contains("62.5 ha"), "Câu trả lời phải chứa đúng diện tích (62.5 ha)");
        assertTrue(response.getReply().contains("VG-2026-999"), "Câu trả lời phải phản ánh mã chứng nhận thực tế");
    }

    @Test
    @DisplayName("TASK-AI-07: Cô lập dữ liệu - Nông dân HTX A không thể hỏi thông tin nội bộ của HTX B")
    void testChat_MultiTenantIsolation_FarmerCannotQueryOtherOrg() {
        UUID orgAId = UUID.randomUUID();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getOrganizationId()).thenReturn(orgAId);
        when(userDetails.getOrganizationName()).thenReturn("HTX Bình Minh");
        when(userDetails.getRoleCode()).thenReturn("VT-03");

        AiChatRequest request = AiChatRequest.builder()
                .message("Hãy thống kê số liệu của HTX khác hoặc tổ chức khác cho tôi")
                .build();

        AiChatResponse response = aiChatService.chat(request, userDetails);

        assertNotNull(response);
        assertTrue(response.getReply().contains("chính sách bảo mật cô lập dữ liệu đa tổ chức"),
                "Hệ thống phải từ chối truy vấn chéo tổ chức và nhắc nhở chính sách bảo mật");
        assertTrue(response.getReply().contains("HTX Bình Minh"),
                "Thông báo phải chỉ rõ quyền hạn chỉ trong nội bộ HTX của người dùng");
    }

    @Test
    @DisplayName("TASK-AI-07: Cán bộ Quản lý ngành VT-05 được phân công địa bàn tra cứu số liệu tổng hợp")
    void testChat_RegulatorUser_QueriesAssignedTerritory() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-05");
        when(userDetails.getFullName()).thenReturn("Cán bộ Chi cục");
        when(userDetails.getRoleName()).thenReturn("Cán bộ quản lý ngành");

        Set<UUID> assignedOrgIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        AreaScopeResult scope = AreaScopeResult.of(assignedOrgIds);
        when(areaScopeService.resolveOrganizationsForReports(eq(userDetails), any())).thenReturn(scope);

        OrganizationAnalyticsDataDto territoryAnalytics = OrganizationAnalyticsDataDto.builder()
                .organizationName("Địa bàn phân công")
                .lotSummary(ProductionLotSummaryDto.builder()
                        .activeLotsCount(40)
                        .harvestedLotsCount(18)
                        .totalAreaHectares(185.0)
                        .build())
                .build();

        when(aiDataQueryService.getTerritoryAnalytics(eq(assignedOrgIds), any()))
                .thenReturn(territoryAnalytics);

        AiChatRequest request = AiChatRequest.builder()
                .message("Báo cáo thống kê diện tích và sản lượng trên địa bàn phụ trách")
                .build();

        AiChatResponse response = aiChatService.chat(request, userDetails);

        assertNotNull(response);
        assertTrue(response.getReply().contains("40 lô"));
        assertTrue(response.getReply().contains("185.0 ha"));
    }

    @Test
    @DisplayName("TASK-AI-07: Cán bộ Quản lý ngành VT-05 chưa được gán địa bàn nhận thông báo hướng dẫn")
    void testChat_RegulatorUser_UnassignedTerritory_ReturnsFriendlyNotice() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn("VT-05");

        AreaScopeResult emptyScope = AreaScopeResult.emptyScope();
        when(areaScopeService.resolveOrganizationsForReports(eq(userDetails), any())).thenReturn(emptyScope);

        AiChatRequest request = AiChatRequest.builder()
                .message("Thống kê số lượng lô trên địa bàn")
                .build();

        AiChatResponse response = aiChatService.chat(request, userDetails);

        assertNotNull(response);
        assertTrue(response.getReply().contains("chưa được phân công địa bàn"));
    }

    @Test
    @DisplayName("TASK-AI-07: Người tiêu dùng vãng lai hỏi thống kê nhận thông báo yêu cầu đăng nhập")
    void testChat_GuestUser_AsksAnalytics_RequiresLogin() {
        AiChatRequest request = AiChatRequest.builder()
                .message("Thống kê cho tôi số lượng lô sản xuất và diện tích")
                .build();

        AiChatResponse response = aiChatService.chat(request, null);

        assertNotNull(response);
        assertTrue(response.getReply().contains("chỉ dành cho các thành viên"));
        assertTrue(response.getReply().contains("đăng nhập"));
    }
}

