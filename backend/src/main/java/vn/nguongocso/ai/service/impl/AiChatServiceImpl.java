package vn.nguongocso.ai.service.impl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import vn.nguongocso.ai.dto.request.AiChatMessageDto;
import vn.nguongocso.ai.dto.request.AiChatRequest;
import vn.nguongocso.ai.dto.response.AiChatResponse;
import vn.nguongocso.ai.dto.response.AiPromptSuggestionResponse;
import vn.nguongocso.ai.service.AiChatService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.constant.RoleCode;

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

    private String systemKnowledge = "";

    public AiChatServiceImpl(
            AiProperties aiProperties,
            @Qualifier("aiRestClient") RestClient aiRestClient,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.aiRestClient = aiRestClient;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
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
                    this.systemKnowledge = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                    log.info("Đã nạp thành công bộ tri thức AI Nguồn Gốc Số ({} ký tự).",
                            this.systemKnowledge.length());
                }
            } else {
                log.warn("Không tìm thấy file classpath:ai/system_knowledge.txt, sử dụng tri thức mặc định.");
                this.systemKnowledge = "Hệ thống Nguồn Gốc Số - Quản lý truy xuất nguồn gốc nông sản chuỗi cung ứng.";
            }
        } catch (Exception e) {
            log.error("Lỗi khi đọc file tri thức AI: {}", e.getMessage(), e);
            this.systemKnowledge = "Hệ thống Nguồn Gốc Số - Quản lý truy xuất nguồn gốc nông sản chuỗi cung ứng.";
        }
    }

    /**
     * Xử lý câu hỏi của người dùng và sinh câu trả lời từ AI theo ngữ cảnh vai trò.
     */
    @Override
    public AiChatResponse chat(AiChatRequest request, CustomUserDetails currentUser) {
        String userMessage = request.getMessage() != null ? request.getMessage().trim() : "";
        List<AiChatMessageDto> history = request.getHistory() != null ? request.getHistory() : new ArrayList<>();

        // Kiểm tra nếu API key chưa cấu hình thì trả về phản hồi hỗ trợ nội bộ mẫu
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            return generateLocalFallbackResponse(userMessage, currentUser);
        }

        List<String> candidateModels = aiProperties.getModelList();
        String systemInstructionText = buildSystemInstruction(currentUser);
        Map<String, Object> requestPayload = buildGeminiPayload(systemInstructionText, history, userMessage);
        String lastErrorDetail = null;

        for (String candidateModel : candidateModels) {
            try {
                String requestUrl = String.format("%s/%s:generateContent?key=%s",
                        aiProperties.getApiUrl(),
                        candidateModel,
                        aiProperties.getApiKey());

                byte[] responseBytes = aiRestClient.post()
                        .uri(requestUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                        .body(requestPayload)
                        .retrieve()
                        .body(byte[].class);

                String responseBody = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "";
                String aiReply = extractTextFromGeminiResponse(responseBody);
                if (aiReply != null && !aiReply.isBlank()) {
                    log.info("Gọi thành công mô hình Google Gemini: [{}]", candidateModel);
                    List<String> dynamicQuestions = extractOrGenerateFollowUpQuestions(currentUser);
                    return AiChatResponse.builder()
                            .reply(aiReply)
                            .timestamp(LocalDateTime.now())
                            .suggestedQuestions(dynamicQuestions)
                            .build();
                }
            } catch (RestClientResponseException ex) {
                lastErrorDetail = String.format("status=%s, body=%s", ex.getStatusCode(), ex.getResponseBodyAsString());
                log.warn(
                        "Mô hình Gemini [{}] gặp lỗi ({}). Đang tự động chuyển sang mô hình tiếp theo trong fallback chain...",
                        candidateModel, ex.getStatusCode());
            } catch (Exception ex) {
                lastErrorDetail = ex.getMessage();
                log.warn("Lỗi kết nối khi gọi mô hình Gemini [{}]: {}. Đang thử mô hình tiếp theo...",
                        candidateModel, ex.getMessage());
            }
        }

        log.error("Tất cả các mô hình Gemini trong chuỗi fallback đều không thành công. Chi tiết lỗi cuối: {}",
                lastErrorDetail);
        return generateLocalFallbackResponse(userMessage, currentUser);
    }

    /**
     * Lấy danh sách các câu hỏi gợi ý phù hợp với vai trò của người dùng hiện tại.
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
     * Xây dựng chỉ dẫn hệ thống kèm thông tin vai trò người dùng hiện tại.
     */
    private String buildSystemInstruction(CustomUserDetails currentUser) {
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

        return sb.toString();
    }

    /**
     * Đóng gói payload gửi sang Google Gemini API.
     */
    private Map<String, Object> buildGeminiPayload(String systemInstructionText, List<AiChatMessageDto> history,
            String userMessage) {
        Map<String, Object> payload = new HashMap<>();

        // System Instruction
        Map<String, Object> systemPart = Map.of("text", systemInstructionText);
        payload.put("systemInstruction", Map.of("parts", List.of(systemPart)));

        // Contents (History + Current Message)
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null) {
            for (AiChatMessageDto msg : history) {
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    String geminiRole = "assistant".equalsIgnoreCase(msg.getRole())
                            || "model".equalsIgnoreCase(msg.getRole())
                                    ? "model"
                                    : "user";
                    contents.add(Map.of(
                            "role", geminiRole,
                            "parts", List.of(Map.of("text", msg.getContent()))));
                }
            }
        }

        // Tin nhắn hiện tại của user
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))));

        payload.put("contents", contents);

        // Generation Config
        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature", 0.3);
        genConfig.put("maxOutputTokens", 2048);
        payload.put("generationConfig", genConfig);

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
            log.error("Lỗi khi parse phản hồi từ Gemini: {}", e.getMessage());
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
     * Phản hồi dự phòng khi chưa cấu hình API Key thực tế (Môi trường Dev / Test).
     */
    private AiChatResponse generateLocalFallbackResponse(String userMessage, CustomUserDetails currentUser) {
        String greeting = currentUser != null
                ? "Xin chào " + currentUser.getFullName() + " (" + currentUser.getRoleName() + ")!"
                : "Xin chào bạn!";

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
    private AiChatResponse generateErrorFallbackResponse(CustomUserDetails currentUser, String detail) {
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

}
