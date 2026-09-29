package vn.nguongocso.ai.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.ai.config.AiProperties;
import vn.nguongocso.ai.dto.query.CertificationStatusDto;
import vn.nguongocso.ai.dto.query.OrganizationAnalyticsDataDto;
import vn.nguongocso.ai.dto.query.ProductionLotSummaryDto;
import vn.nguongocso.ai.dto.request.AiChatMessageDto;
import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.request.AiFarmLogParseRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiFarmLogParseResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.ai.service.AiChatService;
import vn.nguongocso.ai.service.AiDataQueryService;
import vn.nguongocso.ai.util.AiIntentDetector;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;

/**
 * Hiện thực dịch vụ xử lý hội thoại với Trợ lý AI Nguồn Gốc Số.
 */
@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    private final AiProperties aiProperties;
    private final RestClient aiRestClient;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final AiDataQueryService aiDataQueryService;
    private final AreaScopeService areaScopeService;

    private String systemKnowledge = "";

    public AiChatServiceImpl(
            AiProperties aiProperties,
            @Qualifier("aiRestClient") RestClient aiRestClient,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            AiDataQueryService aiDataQueryService,
            AreaScopeService areaScopeService) {
        this.aiProperties = aiProperties;
        this.aiRestClient = aiRestClient;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.aiDataQueryService = aiDataQueryService;
        this.areaScopeService = areaScopeService;
    }

    /**
     * Tải bộ tri thức nghiệp vụ từ tài nguyên hệ thống khi khởi động service.
     */
    @PostConstruct
    public void initKnowledgeBase() {
        try {
            Resource resource = resourceLoader.getResource("classpath:ai/system_knowledge.txt");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    this.systemKnowledge = StreamUtils.copyToString(inputStream,
                            StandardCharsets.UTF_8);
                    log.info("Đã nạp thành công bộ tri thức AI Nguồn Gốc Số ({} ký tự).",
                            this.systemKnowledge.length());
                }
            } else {
                log.warn("Không tìm thấy file classpath:ai/system_knowledge.txt, sử dụng tri thức mặc định.");
                this.systemKnowledge = "Hệ thống Nguồn Gốc Số - Quản lý truy xuất nguồn gốc nông sản chuỗi cung ứng.";
            }
        } catch (Exception e) {
            log.error("Lỗi khi đọc file tri thức AI: {}",
                    e.getMessage(),
                    e);
            this.systemKnowledge = "Hệ thống Nguồn Gốc Số - Quản lý truy xuất nguồn gốc nông sản chuỗi cung ứng.";
        }
    }

    /**
     * Xử lý câu hỏi của người dùng và sinh câu trả lời từ AI theo ngữ cảnh vai
     * trò.
     */
    @Override
    public AiChatResponse chat(AiChatRequest request,
            CustomUserDetails currentUser) {
        String userMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        List<AiChatMessageDto> history = request.getHistory() != null ? request.getHistory() : new ArrayList<>();

        // TASK-AI-07: Chống truy vấn trái phép dữ liệu của tổ chức khác
        // (Multi-Tenant Isolation)
        if (currentUser != null && currentUser.getOrganizationId() != null) {
            String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode() : "";
            if (!RoleCode.ADMIN.equalsIgnoreCase(roleCode) && !RoleCode.REGULATOR.equalsIgnoreCase(roleCode)
                    && AiIntentDetector.hasCrossOrgInquiryIntent(userMessage,
                            currentUser.getOrganizationName(),
                            currentUser.getOrganizationCode())) {
                String notice = "Theo chính sách bảo mật cô lập dữ liệu đa tổ chức của Nguồn Gốc Số, bạn chỉ có quyền tra cứu số liệu nội bộ thuộc tổ chức của mình ("
                        + (currentUser.getOrganizationName() != null ? currentUser.getOrganizationName()
                                : "Tổ chức hiện tại")
                        + "). Hệ thống không cung cấp thông tin của tổ chức khác.";
                return AiChatResponse.builder()
                        .reply(notice)
                        .timestamp(LocalDateTime.now())
                        .suggestedQuestions(extractOrGenerateFollowUpQuestions(currentUser))
                        .build();
            }
        }

        // TASK-AI-06: Nhận diện ý định tra cứu / thống kê số liệu thực tế
        // (Intent Detection)
        OrganizationAnalyticsDataDto analyticsData = null;
        boolean hasAnalytics = AiIntentDetector.hasAnalyticsIntent(userMessage);
        log.info("Phân tích ý định thống kê: hasAnalytics={}, user={}, orgId={}",
                hasAnalytics,
                currentUser != null ? currentUser.getUsername() : "anonymous",
                currentUser != null ? currentUser.getOrganizationId() : "null");

        if (hasAnalytics) {
            if (currentUser == null) {
                String publicNotice = "Tính năng tra cứu và thống kê số liệu nội bộ chỉ dành cho các thành viên Hợp tác xã, Doanh nghiệp hoặc Cơ quan quản lý đã đăng nhập. Bạn vui lòng đăng nhập tài khoản để xem số liệu.";
                return AiChatResponse.builder()
                        .reply(publicNotice)
                        .timestamp(LocalDateTime.now())
                        .suggestedQuestions(extractOrGenerateFollowUpQuestions(currentUser))
                        .build();
            }

            String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode() : "";
            if (RoleCode.REGULATOR.equalsIgnoreCase(roleCode)
                    || "REGULATOR".equalsIgnoreCase(currentUser.getRoleName())) {
                AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(currentUser,
                        null);
                if (scope.isEmptyScope()) {
                    return AiChatResponse.builder()
                            .reply(
                                    "Bạn là Cán bộ Quản lý ngành nhưng hiện chưa được phân công địa bàn quản lý nào trong hệ thống. Vui lòng liên hệ Quản trị viên để được gán địa bàn.")
                            .timestamp(LocalDateTime.now())
                            .suggestedQuestions(extractOrGenerateFollowUpQuestions(currentUser))
                            .build();
                } else if (scope.isFiltered()) {
                    analyticsData = aiDataQueryService.getTerritoryAnalytics(scope.getOrganizationIds(),
                            "Địa bàn phân công");
                }
            } else if (currentUser.getOrganizationId() != null) {
                analyticsData = aiDataQueryService.getFullOrganizationAnalytics(currentUser.getOrganizationId());
            }
        }

        if (analyticsData != null && analyticsData.getLotSummary() != null) {
            log.info(
                    "Đã trích xuất số liệu thực tế: totalLots={}, activeLots={}, harvestedLots={}, packagedLots={}, totalArea={}ha, recentLotsCount={}",
                    analyticsData.getLotSummary().getTotalLotsCount(),
                    analyticsData.getLotSummary().getActiveLotsCount(),
                    analyticsData.getLotSummary().getHarvestedLotsCount(),
                    analyticsData.getLotSummary().getPackagedLotsCount(),
                    analyticsData.getLotSummary().getTotalAreaHectares(),
                    analyticsData.getLotSummary().getRecentLotDetails() != null
                            ? analyticsData.getLotSummary().getRecentLotDetails().size()
                            : 0);
        }

        // Kiểm tra nếu API key chưa cấu hình thì trả về phản hồi hỗ trợ nội bộ
        // mẫu (kèm số liệu nếu có)
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            return generateLocalFallbackResponse(userMessage,
                    currentUser,
                    analyticsData);
        }

        List<String> candidateModels = aiProperties.getModelList();
        String systemInstructionText = buildSystemInstruction(currentUser,
                analyticsData);
        Map<String, Object> requestPayload = buildGeminiPayload(systemInstructionText,
                history,
                userMessage,
                analyticsData);
        String lastErrorDetail = null;

        for (String candidateModel : candidateModels) {
            try {
                String requestUrl = String.format("%s/%s:generateContent?key=%s",
                        aiProperties.getApiUrl(),
                        candidateModel,
                        aiProperties.getApiKey());

                long startTime = System.currentTimeMillis();
                byte[] responseBytes = aiRestClient.post()
                        .uri(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON,
                                MediaType.ALL)
                        .body(requestPayload)
                        .retrieve()
                        .body(byte[].class);

                long durationMs = System.currentTimeMillis() - startTime;
                String responseBody = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "";
                String aiReply = extractTextFromGeminiResponse(responseBody);
                if (aiReply != null && !aiReply.isBlank()) {
                    log.info("Gọi thành công mô hình Google Gemini [{}] sau {}ms",
                            candidateModel,
                            durationMs);
                    List<String> dynamicQuestions = extractOrGenerateFollowUpQuestions(currentUser);
                    return AiChatResponse.builder()
                            .reply(aiReply)
                            .timestamp(LocalDateTime.now())
                            .suggestedQuestions(dynamicQuestions)
                            .build();
                }
            } catch (RestClientResponseException ex) {
                lastErrorDetail = String.format("status=%s, body=%s",
                        ex.getStatusCode(),
                        ex.getResponseBodyAsString());
                log.warn(
                        "Mô hình Gemini [{}] gặp lỗi ({}). Đang tự động chuyển sang mô hình tiếp theo trong fallback chain...",
                        candidateModel,
                        ex.getStatusCode());
            } catch (Exception ex) {
                lastErrorDetail = ex.getMessage();
                log.warn("Lỗi kết nối khi gọi mô hình Gemini [{}]: {}. Đang thử mô hình tiếp theo...",
                        candidateModel,
                        ex.getMessage());
            }
        }

        log.error("Tất cả các mô hình Gemini trong chuỗi fallback đều không thành công. Chi tiết lỗi cuối: {}",
                lastErrorDetail);
        return generateLocalFallbackResponse(userMessage,
                currentUser,
                analyticsData);
    }

    /**
     * Lấy danh sách các câu hỏi gợi ý phù hợp với vai trò của người dùng hiện
     * tại.
     */
    @Override
    public List<AiPromptSuggestionResponse> getSuggestedPrompts(CustomUserDetails currentUser) {
        List<AiPromptSuggestionResponse> suggestions = new ArrayList<>();

        if (currentUser != null && currentUser.getRoleCode() != null) {
            String roleCode = currentUser.getRoleCode();
            String roleName = currentUser.getRoleName() != null ? currentUser.getRoleName() : "";

            if (RoleCode.ADMIN.equalsIgnoreCase(roleCode) || "ADMIN".equalsIgnoreCase(roleName)) {
                suggestions.add(AiPromptSuggestionResponse.builder()
                        .category("Dành cho Quản trị viên Nền tảng (VT-01)")
                        .prompts(List.of(
                                "Làm thế nào để khởi tạo tổ chức mới và cấp dải mã truy xuất?",
                                "Quy trình xác thực giấy chứng nhận chất lượng của tổ chức?",
                                "Cách theo dõi nhật ký hoạt động và xử lý cảnh báo quét bất thường?",
                                "Quy định dải mã GS1 GTIN, GLN, SSCC trong hệ thống?"))
                        .build());
            } else if (RoleCode.ORG_MANAGER.equalsIgnoreCase(roleCode)
                    || "COOPERATIVE_MANAGER".equalsIgnoreCase(roleCode)
                    || "ORG_MANAGER".equalsIgnoreCase(roleName)) {
                suggestions.add(AiPromptSuggestionResponse.builder()
                        .category("Dành cho Quản lý Hợp tác xã (VT-02)")
                        .prompts(List.of(
                                "Quy trình tạo lô sản xuất mới và duyệt lô để nông dân ghi nhật ký?",
                                "Làm sao để cấp phát dải mã QR và kích hoạt tem cho lô hàng xuất xưởng?",
                                "Cách lập biên bản bàn giao điện tử cho doanh nghiệp thu mua?",
                                "Hệ thống cảnh báo chứng nhận chất lượng hết hạn trước bao nhiêu ngày?"))
                        .build());
            } else if (RoleCode.EVENT_RECORDER.equalsIgnoreCase(roleCode)
                    || "FARMER".equalsIgnoreCase(roleCode)
                    || "EVENT_RECORDER".equalsIgnoreCase(roleName)) {
                suggestions.add(AiPromptSuggestionResponse.builder()
                        .category("Dành cho Người ghi sự kiện & Nông hộ (VT-03)")
                        .prompts(List.of(
                                "Làm thế nào để ghi nhật ký bón phân đúng chuẩn VietGAP?",
                                "Thời gian cách ly thuốc bảo vệ thực vật (PHI) tính như thế nào?",
                                "Cách đính kèm ảnh thực tế vào nhật ký canh tác?",
                                "Khi gặp sâu bệnh bất thường trên cây trồng tôi cần báo cáo ai?"))
                        .build());
            } else if (RoleCode.PROCUREMENT.equalsIgnoreCase(roleCode)
                    || "PURCHASING_ENTERPRISE".equalsIgnoreCase(roleCode)
                    || "PROCUREMENT".equalsIgnoreCase(roleName)) {
                suggestions.add(AiPromptSuggestionResponse.builder()
                        .category("Dành cho Doanh nghiệp Thu mua (VT-04)")
                        .prompts(List.of(
                                "Cách quét mã QR để xác nhận nhận bàn giao lô hàng?",
                                "Làm thế nào để chia tách một lô hàng lớn thành các lô con phân phối?",
                                "Cách cấu hình khóa API đối tác (Partner API Key) để đồng bộ dữ liệu?",
                                "Hệ thống gửi webhook khi có sự cố thu hồi như thế nào?"))
                        .build());
            } else if (RoleCode.REGULATOR.equalsIgnoreCase(roleCode)
                    || "REGULATOR".equalsIgnoreCase(roleName)) {
                suggestions.add(AiPromptSuggestionResponse.builder()
                        .category("Dành cho Cán bộ Quản lý ngành (VT-05)")
                        .prompts(List.of(
                                "Hệ thống phát hiện cảnh báo quét mã QR bất thường dựa trên tiêu chí nào?",
                                "Quy trình kích hoạt và giám sát lệnh thu hồi lô hàng?",
                                "Cách tra cứu thống kê diện tích và sản lượng trên địa bàn phụ trách?",
                                "Làm sao để kiểm tra tính hợp lệ của giấy chứng nhận VietGAP?"))
                        .build());
            }
        }

        // Nhóm câu hỏi chung luôn khả dụng
        suggestions.add(AiPromptSuggestionResponse.builder()
                .category("Câu hỏi phổ biến về Nguồn Gốc Số")
                .prompts(List.of(
                        "Nền tảng Nguồn Gốc Số hỗ trợ những tiêu chuẩn chất lượng nào?",
                        "Người tiêu dùng quét mã QR có thể xem được những thông tin gì?",
                        "Làm thế nào khi mã QR sản phẩm bị cảnh báo quét bất thường?",
                        "Quy chuẩn mã hóa dải mã theo tiêu chuẩn GS1 được áp dụng ra sao?"))
                .build());

        return suggestions;
    }

    /**
     * Xây dựng chỉ dẫn hệ thống kèm thông tin vai trò người dùng và dữ liệu
     * thực tế (nếu có).
     */
    private String buildSystemInstruction(CustomUserDetails currentUser,
            OrganizationAnalyticsDataDto analyticsData) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.systemKnowledge);
        sb.append("\n\n---\n");

        if (currentUser != null) {
            sb.append("[NGỮ CẢNH NGƯỜI DÙNG HIỆN TẠI]:\n");
            sb.append("- Họ và tên: ")
                    .append(currentUser.getFullName() != null ? currentUser.getFullName() : "Người dùng").append("\n");
            sb.append("- Vai trò: ")
                    .append(currentUser.getRoleName() != null ? currentUser.getRoleName() : "Thành viên")
                    .append(" (Mã vai trò: ").append(currentUser.getRoleCode()).append(")\n");
            if (currentUser.getOrganizationName() != null) {
                sb.append("- Tổ chức/Hợp tác xã: ").append(currentUser.getOrganizationName())
                        .append(" (Mã: ").append(currentUser.getOrganizationCode()).append(")\n");
            }
            sb.append(
                    "- Hãy xưng hô lịch sự, gọi người dùng bằng tên và ưu tiên hướng dẫn các thao tác phù hợp với vai trò ")
                    .append(currentUser.getRoleName()).append(" của họ.\n");
        } else {
            sb.append("[NGỮ CẢNH NGƯỜI DÙNG HIỆN TẠI]:\n");
            sb.append("- Người dùng là người tiêu dùng chưa đăng nhập tài khoản.\n");
            sb.append(
                    "- Hãy hướng dẫn thân thiện về cách tra cứu sản phẩm công khai hoặc giới thiệu các tính năng của Nguồn Gốc Số.\n");
        }

        // TASK-AI-06: Inject dữ liệu thực tế vào System Context
        if (analyticsData != null) {
            sb.append(formatAnalyticsPromptBlock(analyticsData));
        }

        return sb.toString();
    }

    /**
     * Định dạng khối ngữ cảnh số liệu thực tế để đưa vào Prompt cho AI
     * (TASK-AI-06).
     */
    private String formatAnalyticsPromptBlock(OrganizationAnalyticsDataDto analytics) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[DỮ LIỆU THỰC TẾ CỦA TỔ CHỨC HIỆN TẠI (Tổ chức: ")
                .append(analytics.getOrganizationName() != null ? analytics.getOrganizationName() : "Nội bộ")
                .append(analytics.getOrganizationCode() != null && !analytics.getOrganizationCode().isBlank()
                        ? ", Mã: " + analytics.getOrganizationCode()
                        : "")
                .append(")]:\n");

        if (analytics.getLotSummary() != null) {
            ProductionLotSummaryDto lotSummary = analytics.getLotSummary();
            sb.append("- Tổng số lô sản xuất của tổ chức: ").append(lotSummary.getTotalLotsCount()).append(" lô.\n");
            sb.append("- Tổng diện tích đất canh tác (Vùng trồng): ").append(lotSummary.getTotalAreaHectares())
                    .append(" ha.\n");
            sb.append("- Số lô đang trong vụ canh tác chăm sóc ngoài đồng (APPROVED): ")
                    .append(lotSummary.getActiveLotsCount()).append(" lô.\n");
            sb.append("- Số lô đã thu hoạch hoặc đóng gói thành phẩm (HARVESTED, PACKAGED...): ")
                    .append(lotSummary.getHarvestedLotsCount()).append(" lô");
            if (lotSummary.getPackagedLotsCount() > 0) {
                sb.append(" (Trong đó có ").append(lotSummary.getPackagedLotsCount())
                        .append(" lô đã đóng gói thành phẩm sẵn sàng cấp tem QR xuất bán)");
            }
            sb.append(".\n");

            if (lotSummary.getRecentLotDetails() != null && !lotSummary.getRecentLotDetails().isEmpty()) {
                sb.append("- Danh sách cụ thể các lô sản xuất của đơn vị:\n");
                for (String detail : lotSummary.getRecentLotDetails()) {
                    sb.append("  + ").append(detail).append("\n");
                }
            }

            if (lotSummary.getUpcomingHarvestLotNames() != null
                    && !lotSummary.getUpcomingHarvestLotNames().isEmpty()) {
                sb.append("- Lô sắp thu hoạch gần nhất: ")
                        .append(String.join(", ",
                                lotSummary.getUpcomingHarvestLotNames()))
                        .append(".\n");
            }
        }

        if (analytics.getExpiringCertifications() != null && !analytics.getExpiringCertifications().isEmpty()) {
            sb.append("- Chứng nhận sắp hết hạn trong 30 ngày: ");
            List<String> certStrs = new ArrayList<>();
            for (CertificationStatusDto c : analytics.getExpiringCertifications()) {
                certStrs.add(c.getName() + " (Mã: " + c.getCode() + ", Tiêu chuẩn: " + c.getStandardName()
                        + ", Hết hạn: " + c.getExpiryDate() + ", Còn: " + c.getDaysRemaining() + " ngày)");
            }
            sb.append(String.join("; ",
                    certStrs)).append(".\n");
        } else {
            sb.append("- Chứng nhận sắp hết hạn trong 30 ngày: Không có chứng nhận nào sắp hết hạn.\n");
        }

        if (analytics.getAlertsSummary() != null) {
            sb.append("- Cảnh báo gần đây: ").append(analytics.getAlertsSummary().getPendingScanAnomalyCount())
                    .append(" cảnh báo quét bất thường đang chờ xử lý, ")
                    .append(analytics.getAlertsSummary().getActiveRecallCasesCount())
                    .append(" vụ việc thu hồi đang mở.\n");
        }

        if (analytics.getShipmentSummary() != null) {
            sb.append("- Tình hình lô hàng: ").append(analytics.getShipmentSummary().getInTransitShipmentsCount())
                    .append(" lô hàng đang lưu thông/vận chuyển, ")
                    .append(analytics.getShipmentSummary().getPendingHandoverCount())
                    .append(" biên bản bàn giao chờ đối tác tiếp nhận.\n");
        }

        sb.append(
                "[HƯỚNG DẪN AI]: Hãy sử dụng chính xác các số liệu thực tế ở trên để phân tích và trả lời câu hỏi của người dùng một cách chuyên nghiệp, trung thực và chính xác.\n");
        sb.append(
                "- Khi người dùng hỏi về số lượng lô, tình hình canh tác hoặc diện tích, hãy nêu rõ ràng: tổng số lô hiện có, diện tích vùng trồng, số lô đang canh tác chăm sóc ngoài đồng (APPROVED) và số lô đã thu hoạch/đóng gói thành phẩm (như PACKAGED - Đã đóng gói). Hãy liệt kê cụ thể tên các lô sản xuất (ví dụ: Lô Trồng Vải) kèm trạng thái thực tế để người dùng dễ dàng đối chiếu với danh sách hiển thị trên giao diện quản trị.\n");
        return sb.toString();
    }

    /**
     * Đóng gói payload gửi sang Google Gemini API.
     */
    private Map<String, Object> buildGeminiPayload(String systemInstructionText,
            List<AiChatMessageDto> history,
            String userMessage,
            OrganizationAnalyticsDataDto analyticsData) {
        Map<String, Object> payload = new HashMap<>();

        // System Instruction
        Map<String, Object> systemPart = Map.of("text",
                systemInstructionText);
        payload.put("systemInstruction",
                Map.of("parts",
                        List.of(systemPart)));

        // Contents (History + Current Message)
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null && !history.isEmpty()) {
            // Gemini API yêu cầu lượt chat đầu tiên phải là của user, bỏ qua
            // tin nhắn mở đầu của model nếu có
            int firstUserIndex = -1;
            for (int i = 0; i < history.size(); i++) {
                String role = history.get(i).getRole();
                if ("user".equalsIgnoreCase(role)
                        || (!"assistant".equalsIgnoreCase(role) && !"model".equalsIgnoreCase(role))) {
                    firstUserIndex = i;
                    break;
                }
            }

            if (firstUserIndex >= 0) {
                String lastRole = null;
                for (int i = firstUserIndex; i < history.size(); i++) {
                    AiChatMessageDto msg = history.get(i);
                    if (msg.getContent() != null && !msg.getContent().isBlank()) {
                        String geminiRole = "assistant".equalsIgnoreCase(msg.getRole())
                                || "model".equalsIgnoreCase(msg.getRole())
                                        ? "model"
                                        : "user";
                        // Đảm bảo không có hai lượt liên tiếp cùng một role
                        if (!geminiRole.equals(lastRole)) {
                            contents.add(Map.of(
                                    "role",
                                    geminiRole,
                                    "parts",
                                    List.of(Map.of("text",
                                            msg.getContent()))));
                            lastRole = geminiRole;
                        }
                    }
                }
            }
        }

        // Tin nhắn hiện tại của user kèm dữ liệu thực tế (nếu có)
        if (!contents.isEmpty() && "user".equals(contents.get(contents.size() - 1).get("role"))) {
            contents.add(Map.of(
                    "role",
                    "model",
                    "parts",
                    List.of(Map.of("text",
                            "..."))));
        }

        String finalUserMessage = userMessage;
        if (analyticsData != null) {
            finalUserMessage = userMessage + "\n\n" + formatAnalyticsPromptBlock(analyticsData);
        }

        contents.add(Map.of(
                "role",
                "user",
                "parts",
                List.of(Map.of("text",
                        finalUserMessage))));

        payload.put("contents",
                contents);

        // Generation Config
        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature",
                0.3);
        genConfig.put("maxOutputTokens",
                2048);
        payload.put("generationConfig",
                genConfig);

        return payload;
    }

    /**
     * Trích xuất văn bản trả lời từ cấu trúc JSON phản hồi của Gemini.
     */
    private String extractTextFromGeminiResponse(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode candidatesNode = rootNode.path("candidates");
            if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                JsonNode partsNode = candidatesNode.get(0).path("content").path("parts");
                if (partsNode.isArray() && !partsNode.isEmpty()) {
                    return partsNode.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi parse phản hồi từ Gemini: {}",
                    e.getMessage());
        }
        return "Xin lỗi, Trợ lý AI chưa thể phân tích được câu trả lời phù hợp. Bạn vui lòng thử lại câu hỏi khác.";
    }

    /**
     * Sinh gợi ý câu hỏi liên quan tiếp theo dựa theo vai trò người dùng.
     */
    private List<String> extractOrGenerateFollowUpQuestions(CustomUserDetails currentUser) {
        if (currentUser != null && currentUser.getRoleCode() != null) {
            String roleCode = currentUser.getRoleCode();
            String roleName = currentUser.getRoleName() != null ? currentUser.getRoleName() : "";

            if (RoleCode.EVENT_RECORDER.equalsIgnoreCase(roleCode)
                    || "FARMER".equalsIgnoreCase(roleCode)
                    || "EVENT_RECORDER".equalsIgnoreCase(roleName)) {
                return List.of(
                        "Làm sao để xem lại nhật ký đã ghi chép?",
                        "Thời gian cách ly thuốc trừ sâu quy định ra sao?");
            } else if (RoleCode.ORG_MANAGER.equalsIgnoreCase(roleCode)
                    || "COOPERATIVE_MANAGER".equalsIgnoreCase(roleCode)
                    || "ORG_MANAGER".equalsIgnoreCase(roleName)) {
                return List.of(
                        "Làm thế nào để tạo biên bản bàn giao lô hàng?",
                        "Cách kích hoạt tem QR và quản lý chứng nhận VietGAP?");
            } else if (RoleCode.PROCUREMENT.equalsIgnoreCase(roleCode)
                    || "PURCHASING_ENTERPRISE".equalsIgnoreCase(roleCode)
                    || "PROCUREMENT".equalsIgnoreCase(roleName)) {
                return List.of(
                        "Quy trình xác nhận phiếu bàn giao điện tử?",
                        "Cách chia tách lô hàng (SPLIT) để phân phối?");
            } else if (RoleCode.REGULATOR.equalsIgnoreCase(roleCode)
                    || "REGULATOR".equalsIgnoreCase(roleName)) {
                return List.of(
                        "Cách tra cứu thống kê sản lượng theo địa bàn?",
                        "Quy trình giám sát vụ việc thu hồi lô hàng?");
            } else if (RoleCode.ADMIN.equalsIgnoreCase(roleCode)
                    || "ADMIN".equalsIgnoreCase(roleName)) {
                return List.of(
                        "Cách cấp dải mã truy xuất cho tổ chức mới?",
                        "Quy trình xác thực chứng nhận chất lượng?");
            }
        }
        return List.of(
                "Quy trình quét mã QR tem truy xuất nguồn gốc?",
                "Các bước xử lý khi phát hiện cảnh báo bất thường?");
    }

    /**
     * Phản hồi dự phòng khi chưa cấu hình API Key thực tế (Môi trường Dev /
     * Test).
     */
    private AiChatResponse generateLocalFallbackResponse(String userMessage,
            CustomUserDetails currentUser,
            OrganizationAnalyticsDataDto analytics) {
        String greeting = currentUser != null
                ? "Xin chào " + currentUser.getFullName() + " (" + currentUser.getRoleName() + ")!"
                : "Xin chào bạn!";

        // TASK-AI-06: Nếu phát hiện ý định thống kê và có số liệu thực tế, định
        // dạng báo cáo số liệu thực
        if (analytics != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(greeting).append("\n\n");
            sb.append("📊 **Báo cáo số liệu thực tế của ").append(analytics.getOrganizationName()).append(":**\n\n");

            if (analytics.getLotSummary() != null) {
                sb.append("• **Lô sản xuất & Vùng trồng:**\n");
                sb.append("  - Đang canh tác: **").append(analytics.getLotSummary().getActiveLotsCount())
                        .append(" lô** (Tổng diện tích: **")
                        .append(analytics.getLotSummary().getTotalAreaHectares()).append(" ha**)\n");
                sb.append("  - Đã thu hoạch: **").append(analytics.getLotSummary().getHarvestedLotsCount())
                        .append(" lô**\n");
                if (analytics.getLotSummary().getUpcomingHarvestLotNames() != null
                        && !analytics.getLotSummary().getUpcomingHarvestLotNames().isEmpty()) {
                    sb.append("  - Lô sắp thu hoạch: ")
                            .append(String.join(", ",
                                    analytics.getLotSummary().getUpcomingHarvestLotNames()))
                            .append("\n");
                }
            }

            if (analytics.getExpiringCertifications() != null && !analytics.getExpiringCertifications().isEmpty()) {
                sb.append("• **Chứng nhận chất lượng sắp hết hạn trong 30 ngày:**\n");
                for (CertificationStatusDto c : analytics.getExpiringCertifications()) {
                    sb.append("  - ").append(c.getName()).append(" (Mã: `").append(c.getCode())
                            .append("`, Tiêu chuẩn: ")
                            .append(c.getStandardName()).append(") - Hết hạn ngày ").append(c.getExpiryDate())
                            .append(" (còn ").append(c.getDaysRemaining()).append(" ngày)\n");
                }
            } else {
                sb.append("• **Chứng nhận chất lượng:** Hiện không có chứng nhận nào sắp hết hạn trong 30 ngày tới.\n");
            }

            if (analytics.getAlertsSummary() != null) {
                sb.append("• **Cảnh báo & Rủi ro:**\n");
                sb.append("  - Cảnh báo quét bất thường: **")
                        .append(analytics.getAlertsSummary().getPendingScanAnomalyCount())
                        .append(" cảnh báo** chờ xử lý\n");
                sb.append("  - Vụ việc thu hồi: **").append(analytics.getAlertsSummary().getActiveRecallCasesCount())
                        .append(" vụ việc** đang xử lý\n");
            }

            if (analytics.getShipmentSummary() != null) {
                sb.append("• **Lưu thông & Bàn giao:**\n");
                sb.append("  - Lô hàng đang lưu thông: **")
                        .append(analytics.getShipmentSummary().getInTransitShipmentsCount())
                        .append(" lô**\n");
                sb.append("  - Biên bản bàn giao chờ xác nhận: **")
                        .append(analytics.getShipmentSummary().getPendingHandoverCount())
                        .append(" biên bản**\n");
            }

            sb.append("\n> *Dữ liệu được trích xuất thời gian thực trực tiếp từ cơ sở dữ liệu hệ thống Nguồn Gốc Số.*");
            return AiChatResponse.builder()
                    .reply(sb.toString())
                    .timestamp(LocalDateTime.now())
                    .suggestedQuestions(extractOrGenerateFollowUpQuestions(currentUser))
                    .build();
        }

        String content;
        String lowerMsg = userMessage.toLowerCase();

        if (lowerMsg.contains("lô") || lowerMsg.contains("tạo lô") || lowerMsg.contains("sản xuất")) {
            content = greeting + "\n\n**Hướng dẫn tạo Lô sản xuất mới trên Nguồn Gốc Số:**\n"
                    + "1. **Bước 1:** Vào thanh điều hướng chọn mục **Lô sản xuất** (dành cho Quản lý HTX VT-02).\n"
                    + "2. **Bước 2:** Nhấn nút **Tạo lô sản xuất mới** ở góc trên bên phải.\n"
                    + "3. **Bước 3:** Chọn **Vùng trồng**, **Loại giống cây**, nhập diện tích, ngày bắt đầu mùa vụ và ngày dự kiến thu hoạch.\n"
                    + "4. **Bước 4:** Bấm **Lưu** để hệ thống tạo lô ở trạng thái **Nháp (DRAFT)**.\n"
                    + "5. **Bước 5:** Quản lý HTX phê duyệt lô chuyển sang trạng thái **Đã duyệt (APPROVED)**. Khi đó, người ghi sự kiện (VT-03) mới bắt đầu được ghi Nhật ký canh tác (Farm Log).\n\n";
        } else if (lowerMsg.contains("nhật ký") || lowerMsg.contains("canh tác") || lowerMsg.contains("bón phân")
                || lowerMsg.contains("thuốc")) {
            content = greeting + "\n\n**Quy trình ghi Nhật ký canh tác chuẩn VietGAP:**\n"
                    + "1. Chọn Lô sản xuất đang canh tác cần ghi nhật ký.\n"
                    + "2. Nhấn **Thêm nhật ký canh tác**.\n"
                    + "3. Chọn loại hoạt động (Gieo giống, Bón phân, Phun thuốc BVTV, Tưới nước, Thu hoạch).\n"
                    + "4. Điền cụ thể tên vật tư, liều lượng sử dụng và thời gian cách ly (PHI) bắt buộc.\n"
                    + "5. Đính kèm hình ảnh bao bì vật tư hoặc hiện trường để phục vụ kiểm tra thẩm định.\n\n";
        } else if (lowerMsg.contains("bàn giao") || lowerMsg.contains("thu mua") || lowerMsg.contains("vận chuyển")) {
            content = greeting + "\n\n**Quy trình Bàn giao Lô hàng cho Doanh nghiệp Thu mua:**\n"
                    + "1. Quản lý HTX tạo **Phiếu bàn giao** từ các Lô sản xuất đã thu hoạch đạt chuẩn.\n"
                    + "2. Hệ thống sinh mã QR bàn giao điện tử duy nhất.\n"
                    + "3. Doanh nghiệp thu mua dùng ứng dụng quét mã QR để kiểm tra thực tế và xác nhận ký nhận bàn giao điện tử.\n"
                    + "4. Dữ liệu bàn giao được lưu vết bất biến trên chuỗi sự kiện hành trình (Chain of Custody).\n\n";
        } else {
            content = greeting + "\n\nTôi là **Trợ lý AI Nguồn Gốc Số**, sẵn sàng hỗ trợ bạn về:\n"
                    + "- **Quy trình canh tác & ghi nhật ký** theo chuẩn VietGAP/GlobalGAP.\n"
                    + "- **Quản lý lô sản xuất, tạo biên bản bàn giao** thu mua.\n"
                    + "- **Quét mã QR truy xuất nguồn gốc** và xử lý cảnh báo bất thường.\n"
                    + "- **Quản lý chứng nhận chất lượng** và tích hợp cổng dữ liệu đối tác.\n\n"
                    + "Bạn có thể nhập câu hỏi chi tiết hơn hoặc chọn các gợi ý bên dưới để được hướng dẫn cụ thể!\n\n";
        }

        String note;
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            note = "> *(Lưu ý: Hệ thống đang chạy ở chế độ tri thức cục bộ do chưa nạp biến GEMINI_API_KEY. Trợ lý phản hồi theo quy trình chuẩn).*";
        } else {
            note = "> *(Lưu ý: Dịch vụ AI đám mây Google đang quá tải tạm thời. Trợ lý tự động kích hoạt bộ tri thức chuẩn của hệ thống để hỗ trợ bạn kịp thời).*";
        }

        return AiChatResponse.builder()
                .reply(content + note)
                .timestamp(LocalDateTime.now())
                .suggestedQuestions(extractOrGenerateFollowUpQuestions(currentUser))
                .build();
    }

    /**
     * Phản hồi thân thiện khi gặp sự cố mạng hoặc lỗi từ máy chủ AI.
     */
    private AiChatResponse generateErrorFallbackResponse(CustomUserDetails currentUser,
            String detail) {
        String greeting = currentUser != null ? "Xin chào " + currentUser.getFullName() + "! " : "Xin chào! ";
        String msg = greeting + "Rất tiếc, kết nối đến dịch vụ Trợ lý AI đang gặp sự cố gián đoạn tạm thời (" + detail
                + ").\n\n"
                + "Bạn vui lòng thử gửi lại câu hỏi sau giây lát, hoặc tham khảo tài liệu hướng dẫn sử dụng trên thanh công cụ của hệ thống.";

        return AiChatResponse.builder()
                .reply(msg)
                .timestamp(LocalDateTime.now())
                .suggestedQuestions(extractOrGenerateFollowUpQuestions(currentUser))
                .build();
    }

    /**
     * Phân tích câu nói tiếng Việt của nông dân để trích xuất cấu trúc nhật ký canh tác.
     */
    @Override
    public AiFarmLogParseResponse parseFarmLogVoice(AiFarmLogParseRequest request, CustomUserDetails currentUser) {
        String voiceText = request.getVoiceText() != null ? request.getVoiceText().trim() : "";
        if (voiceText.isEmpty()) {
            return AiFarmLogParseResponse.builder()
                    .activityType("OTHER")
                    .activityLabel("Khác")
                    .executedDate(LocalDate.now())
                    .summaryText("Chưa nhận được nội dung giọng nói. Bác vui lòng thử nói lại nhé!")
                    .rawVoiceText("")
                    .confidence(0.0)
                    .build();
        }

        // 1. Thử gọi mô hình Gemini AI nếu có cấu hình API Key
        if (aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank()) {
            try {
                AiFarmLogParseResponse aiResponse = callGeminiForFarmLog(voiceText, request.getAvailableLots(), request.getAvailableMaterials());
                if (aiResponse != null) {
                    aiResponse.setRawVoiceText(voiceText);
                    return aiResponse;
                }
            } catch (Exception e) {
                log.warn("Gọi Gemini bóc tách nhật ký nông dân không thành công, chuyển sang bộ phân tích quy tắc: {}", e.getMessage());
            }
        }

        // 2. Dự phòng bằng bộ phân tích quy tắc tiếng Việt chuẩn hóa (Offline / No Key Fallback)
        return fallbackParseFarmLog(voiceText, request.getAvailableLots(), request.getAvailableMaterials());
    }

    /**
     * Gọi Gemini AI với chỉ dẫn trích xuất cấu trúc JSON cho nhật ký canh tác.
     */
    private AiFarmLogParseResponse callGeminiForFarmLog(String voiceText,
            List<AiFarmLogParseRequest.LotHintDto> availableLots,
            List<AiFarmLogParseRequest.MaterialHintDto> availableMaterials) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là Trợ lý AI nông nghiệp chuyên trích xuất thông tin nhật ký canh tác từ lời nói tiếng Việt của người nông dân.\n");
        prompt.append("Câu nói của nông dân: \"").append(voiceText).append("\"\n\n");
        prompt.append("Hãy phân tích và trả về DUY NHẤT một chuỗi JSON hợp lệ (không kèm markdown ```json hay giải thích) theo cấu trúc sau:\n");
        prompt.append("{\n");
        prompt.append("  \"activityType\": \"PLANTING|WATERING|FERTILIZING|PESTICIDE|WEEDING|HARVESTING|OTHER\",\n");
        prompt.append("  \"activityLabel\": \"Tên hiển thị tiếng Việt (ví dụ: Bón phân, Tưới nước, Phun thuốc...)\",\n");
        prompt.append("  \"material\": \"Tên vật tư sử dụng (nếu có, ví dụ: Phân NPK, Thuốc Sherpa, Phân hữu cơ...)\",\n");
        prompt.append("  \"quantity\": 20.0,\n");
        prompt.append("  \"unit\": \"kg|lít|bao|bình|chai|gói|tấn\",\n");
        prompt.append("  \"productionLotName\": \"Tên lô sản xuất nếu được nhắc đến\",\n");
        prompt.append("  \"executedDate\": \"YYYY-MM-DD (ngày thực hiện, nếu nói hôm qua thì lùi 1 ngày, nếu có ngày cụ thể thì ghi YYYY-MM-DD, mặc định hôm nay)\",\n");
        prompt.append("  \"notes\": \"Ghi chú thêm về thời tiết hoặc hiện trường nếu có\",\n");
        prompt.append("  \"summaryText\": \"Câu tóm tắt ngắn gọn, xưng hô 'bác' thân thiện để loa điện thoại đọc lại xác nhận (ví dụ: Đã ghi nhận: Bón 20 kilôgam phân NPK cho Lô Nho 01. Bác kiểm tra lại nhé!)\"\n");
        prompt.append("}\n");

        if (availableLots != null && !availableLots.isEmpty()) {
            prompt.append("\nDanh sách lô sản xuất hợp lệ trong hệ thống:\n");
            for (AiFarmLogParseRequest.LotHintDto lot : availableLots) {
                prompt.append("- ID: ").append(lot.getId()).append(", Tên: ").append(lot.getName()).append("\n");
            }
        }

        if (availableMaterials != null && !availableMaterials.isEmpty()) {
            prompt.append("\nDanh sách vật tư hợp lệ trong kho:\n");
            for (AiFarmLogParseRequest.MaterialHintDto mat : availableMaterials) {
                prompt.append("- Tên: ").append(mat.getName()).append(" (Đơn vị: ").append(mat.getUnit()).append(")\n");
            }
        }

        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt.toString())))),
                "generationConfig", Map.of("temperature", 0.1, "maxOutputTokens", 1024)
        );

        for (String modelName : aiProperties.getModelList()) {
            try {
                String url = aiProperties.getApiUrl() + "/" + modelName + ":generateContent?key=" + aiProperties.getApiKey();
                String responseBody = aiRestClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                if (responseBody != null && !responseBody.isBlank()) {
                    String rawText = extractTextFromGeminiResponse(responseBody).trim();
                    if (rawText.startsWith("```json")) {
                        rawText = rawText.substring(7);
                    } else if (rawText.startsWith("```")) {
                        rawText = rawText.substring(3);
                    }
                    if (rawText.endsWith("```")) {
                        rawText = rawText.substring(0, rawText.length() - 3);
                    }
                    rawText = rawText.trim();

                    JsonNode json = objectMapper.readTree(rawText);
                    String activityType = json.path("activityType").asText("OTHER");
                    String activityLabel = json.path("activityLabel").asText("Khác");
                    String material = json.path("material").asText("");
                    Double quantity = json.has("quantity") && !json.path("quantity").isNull() ? json.path("quantity").asDouble() : null;
                    String unit = json.path("unit").asText("");
                    String lotName = json.path("productionLotName").asText("");
                    String notes = json.path("notes").asText("");
                    String summaryText = json.path("summaryText").asText("");

                    LocalDate executedDate = LocalDate.now();
                    String dateStr = json.path("executedDate").asText("");
                    if (!dateStr.isBlank()) {
                        try {
                            executedDate = LocalDate.parse(dateStr);
                        } catch (Exception ignored) {
                        }
                    }

                    // Tìm ID của lô nếu khớp tên
                    String matchedLotId = null;
                    if (availableLots != null && !lotName.isBlank()) {
                        for (AiFarmLogParseRequest.LotHintDto lot : availableLots) {
                            if (lot.getName() != null && lot.getName().toLowerCase().contains(lotName.toLowerCase())) {
                                matchedLotId = lot.getId();
                                lotName = lot.getName();
                                break;
                            }
                        }
                    }

                    return AiFarmLogParseResponse.builder()
                            .activityType(activityType)
                            .activityLabel(activityLabel)
                            .material(material)
                            .quantity(quantity != null ? BigDecimal.valueOf(quantity) : null)
                            .unit(unit)
                            .productionLotId(matchedLotId)
                            .productionLotName(lotName)
                            .executedDate(executedDate)
                            .notes(notes)
                            .summaryText(summaryText)
                            .confidence(0.95)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Thử mô hình {} thất bại: {}", modelName, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Bộ phân tích quy tắc tiếng Việt chuẩn hóa (chạy độc lập, không phụ thuộc API ngoài).
     */
    private AiFarmLogParseResponse fallbackParseFarmLog(String voiceText,
            List<AiFarmLogParseRequest.LotHintDto> availableLots,
            List<AiFarmLogParseRequest.MaterialHintDto> availableMaterials) {
        String lower = voiceText.toLowerCase();

        // 1. Nhận diện loại hoạt động
        String activityType = "OTHER";
        String activityLabel = "Khác";

        if (lower.contains("bón") || lower.contains("phân") || lower.contains("đạm") || lower.contains("lân")
                || lower.contains("kali") || lower.contains("npk") || lower.contains("hữu cơ")
                || lower.contains("vi sinh") || lower.contains("chuồng")) {
            activityType = "FERTILIZING";
            activityLabel = "Bón phân";
        } else if (lower.contains("tưới") || lower.contains("nước") || lower.contains("nhỏ giọt")
                || lower.contains("bơm nước") || lower.contains("phun sương")) {
            activityType = "WATERING";
            activityLabel = "Tưới nước";
        } else if (lower.contains("phun") || lower.contains("xịt") || lower.contains("thuốc")
                || lower.contains("trừ sâu") || lower.contains("nấm") || lower.contains("rầy")
                || lower.contains("diệt cỏ") || lower.contains("bảo vệ thực vật") || lower.contains("bvtv")) {
            activityType = "PESTICIDE";
            activityLabel = "Phun thuốc";
        } else if (lower.contains("cỏ") || lower.contains("làm cỏ") || lower.contains("nhổ cỏ")
                || lower.contains("xới đất") || lower.contains("làm đất")) {
            activityType = "WEEDING";
            activityLabel = "Làm cỏ";
        } else if (lower.contains("thu hoạch") || lower.contains("hái") || lower.contains("cắt chùm")
                || lower.contains("thu hái") || lower.contains("thu gom")) {
            activityType = "HARVESTING";
            activityLabel = "Thu hoạch";
        } else if (lower.contains("gieo") || lower.contains("trồng") || lower.contains("xuống giống")
                || lower.contains("cây con") || lower.contains("ươm")) {
            activityType = "PLANTING";
            activityLabel = "Gieo trồng";
        }

        // 2. Nhận diện số lượng và đơn vị tính
        BigDecimal quantity = null;
        String unit = "";

        // Chuyển một số từ chữ số tiếng Việt thông dụng
        String normalized = lower
                .replace("hai mươi lăm", "25")
                .replace("hai mươi", "20")
                .replace("mười lăm", "15")
                .replace("ba mươi", "30")
                .replace("năm mươi", "50")
                .replace("mười", "10")
                .replace("một trăm", "100")
                .replace("nửa", "0.5")
                .replace("một", "1")
                .replace("hai", "2")
                .replace("ba", "3")
                .replace("bốn", "4")
                .replace("năm", "5")
                .replace("sáu", "6")
                .replace("bảy", "7")
                .replace("tám", "8")
                .replace("chín", "9");

        Pattern numPattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(kg|kí|ký|kilôgam|cân|lít|lit|l|bao|bình|binh|chai|gói|tấn|tạ)?");
        Matcher matcher = numPattern.matcher(normalized);
        if (matcher.find()) {
            try {
                String numStr = matcher.group(1).replace(",", ".");
                quantity = new BigDecimal(numStr);
                String rawUnit = matcher.group(2);
                if (rawUnit != null) {
                    rawUnit = rawUnit.toLowerCase();
                    if (rawUnit.contains("cân") || rawUnit.contains("kí") || rawUnit.contains("ký") || rawUnit.contains("kg") || rawUnit.contains("kilo")) {
                        unit = "kg";
                    } else if (rawUnit.contains("lit") || rawUnit.contains("lít") || rawUnit.equals("l")) {
                        unit = "lít";
                    } else if (rawUnit.contains("binh") || rawUnit.contains("bình")) {
                        unit = "bình";
                    } else if (rawUnit.contains("bao")) {
                        unit = "bao";
                    } else if (rawUnit.contains("chai")) {
                        unit = "chai";
                    } else if (rawUnit.contains("gói")) {
                        unit = "gói";
                    } else if (rawUnit.contains("tấn")) {
                        unit = "tấn";
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Nếu chưa có đơn vị nhưng biết hoạt động
        if (unit.isEmpty() && quantity != null) {
            if ("FERTILIZING".equals(activityType)) unit = "kg";
            else if ("WATERING".equals(activityType)) unit = "lít";
            else if ("PESTICIDE".equals(activityType)) unit = "bình";
            else if ("HARVESTING".equals(activityType)) unit = "kg";
        }

        // 3. Khớp tên lô sản xuất
        String matchedLotId = null;
        String matchedLotName = null;
        if (availableLots != null && !availableLots.isEmpty()) {
            for (AiFarmLogParseRequest.LotHintDto lot : availableLots) {
                if (lot.getName() != null) {
                    String lotLower = lot.getName().toLowerCase();
                    if (lower.contains(lotLower) || lotLower.contains("lô 1") && lower.contains("lô 1")
                            || lotLower.contains("lô nho 01") && (lower.contains("nho 1") || lower.contains("lô 1") || lower.contains("vườn 1"))) {
                        matchedLotId = lot.getId();
                        matchedLotName = lot.getName();
                        break;
                    }
                }
            }
            if (matchedLotId == null && !availableLots.isEmpty()) {
                // Tự động gán lô nếu đề cập chung
                for (AiFarmLogParseRequest.LotHintDto lot : availableLots) {
                    if (lower.contains("lô") && lot.getName() != null && lower.contains(lot.getName().toLowerCase())) {
                        matchedLotId = lot.getId();
                        matchedLotName = lot.getName();
                        break;
                    }
                }
            }
        }

        // 4. Nhận diện tên vật tư
        String material = "";
        if (availableMaterials != null && !availableMaterials.isEmpty()) {
            for (AiFarmLogParseRequest.MaterialHintDto mat : availableMaterials) {
                if (mat.getName() != null && lower.contains(mat.getName().toLowerCase())) {
                    material = mat.getName();
                    if (unit.isEmpty() && mat.getUnit() != null) {
                        unit = mat.getUnit();
                    }
                    break;
                }
            }
        }

        if (material.isEmpty()) {
            if (lower.contains("hữu cơ")) material = "Phân hữu cơ vi sinh";
            else if (lower.contains("npk")) material = "Phân bón NPK";
            else if (lower.contains("đạm")) material = "Phân đạm Urê";
            else if (lower.contains("lân")) material = "Phân lân";
            else if (lower.contains("kali")) material = "Phân kali";
            else if (lower.contains("thuốc trừ sâu")) material = "Thuốc trừ sâu sinh học";
            else if (lower.contains("nấm")) material = "Thuốc trừ nấm bệnh";
            else if (lower.contains("nước")) material = "Nước sạch tưới tiêu";
        }

        // 5. Nhận diện ngày thực hiện
        LocalDate executedDate = LocalDate.now();
        if (lower.contains("hôm qua") || lower.contains("hôm trc") || lower.contains("hôm trước")) {
            executedDate = LocalDate.now().minusDays(1);
        } else {
            Pattern datePattern1 = Pattern.compile("(?:ngày\\s*)?(\\d{1,2})[\\/\\-](\\d{1,2})(?:[\\/\\-](\\d{4}))?");
            Pattern datePattern2 = Pattern.compile("ngày\\s*(\\d{1,2})\\s*tháng\\s*(\\d{1,2})");
            Matcher dm1 = datePattern1.matcher(lower);
            Matcher dm2 = datePattern2.matcher(lower);
            if (dm1.find()) {
                try {
                    int day = Integer.parseInt(dm1.group(1));
                    int month = Integer.parseInt(dm1.group(2));
                    int year = dm1.group(3) != null ? Integer.parseInt(dm1.group(3)) : LocalDate.now().getYear();
                    if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
                        executedDate = LocalDate.of(year, month, day);
                    }
                } catch (Exception ignored) {}
            } else if (dm2.find()) {
                try {
                    int day = Integer.parseInt(dm2.group(1));
                    int month = Integer.parseInt(dm2.group(2));
                    int year = LocalDate.now().getYear();
                    if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
                        executedDate = LocalDate.of(year, month, day);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 6. Tạo câu tóm tắt (TTS Text)
        StringBuilder summary = new StringBuilder("Đã ghi nhận: ");
        summary.append(activityLabel);
        if (!material.isEmpty()) {
            summary.append(" ").append(material);
        }
        if (quantity != null) {
            summary.append(", số lượng ").append(quantity.stripTrailingZeros().toPlainString());
            if (!unit.isEmpty()) {
                summary.append(" ").append(unit.equals("kg") ? "kilôgam" : unit);
            }
        }
        if (matchedLotName != null) {
            summary.append(" cho ").append(matchedLotName);
        }
        if (executedDate.equals(LocalDate.now().minusDays(1))) {
            summary.append(" ngày hôm qua");
        } else if (!executedDate.equals(LocalDate.now())) {
            summary.append(" ngày ").append(executedDate.getDayOfMonth()).append(" tháng ").append(executedDate.getMonthValue());
        }
        summary.append(". Bác kiểm tra lại trên màn hình nhé!");

        return AiFarmLogParseResponse.builder()
                .activityType(activityType)
                .activityLabel(activityLabel)
                .material(material)
                .quantity(quantity)
                .unit(unit)
                .productionLotId(matchedLotId)
                .productionLotName(matchedLotName)
                .executedDate(executedDate)
                .notes(voiceText)
                .summaryText(summary.toString())
                .rawVoiceText(voiceText)
                .confidence(0.9)
                .build();
    }

}
