package vn.nguongocso.certification.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryCriterionResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryResponse;
import vn.nguongocso.certification.enums.InspectionResultEntrySource;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;

/**
 * Kiểm thử controller công khai cho cổng nhập kết quả của đơn vị kiểm nghiệm (PublicInspectionResultEntryController).
 */
@WebMvcTest(PublicInspectionResultEntryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PublicInspectionResultEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InspectionResultEntryLinkService linkService;

    @MockitoBean
    private InspectionCriterionResultService resultService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET cổng nhập thành công trả về HTTP 200 và header Cache-Control no-store")
    void testGetPortalData_Success_Returns200AndNoStore() throws Exception {
        String token = "valid-token-123456789012345678901234";

        PublicInspectionResultEntryResponse response = PublicInspectionResultEntryResponse.builder()
                .testingUnit("Trung tâm Kiểm nghiệm Quốc gia")
                .lotCode("LOT-2026-001")
                .lotName("Lô Xoài Cát Chu")
                .sampleSentDate(LocalDate.of(2026, 9, 10))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .criteria(List.of(
                        PublicInspectionResultEntryCriterionResponse.builder()
                                .criterionId(UUID.randomUUID())
                                .name("Hàm lượng chì")
                                .code("PB-01")
                                .build()))
                .build();

        when(linkService.getPublicPortalData(eq(token), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/inspection-result-entry/{token}", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.testingUnit").value("Trung tâm Kiểm nghiệm Quốc gia"))
                .andExpect(jsonPath("$.data.lotCode").value("LOT-2026-001"))
                .andExpect(jsonPath("$.data.criteria").isArray())
                .andExpect(jsonPath("$.data.criteria[0].name").value("Hàm lượng chì"));
    }

    @Test
    @DisplayName("GET cổng nhập trả về HTTP 410 GONE khi liên kết hết hạn (TC-02)")
    void testGetPortalData_Expired_Returns410() throws Exception {
        String token = "expired-token";

        when(linkService.getPublicPortalData(eq(token), anyString()))
                .thenThrow(new BusinessException(HttpStatus.GONE, "Liên kết nhập kết quả đã hết hạn. Vui lòng liên hệ hợp tác xã để được cấp liên kết mới."));

        mockMvc.perform(get("/api/v1/public/inspection-result-entry/{token}", token))
                .andExpect(status().isGone())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("GET cổng nhập trả về HTTP 410 GONE khi liên kết đã sử dụng (TC-03)")
    void testGetPortalData_Used_Returns410() throws Exception {
        String token = "used-token";

        when(linkService.getPublicPortalData(eq(token), anyString()))
                .thenThrow(new BusinessException(HttpStatus.GONE, "Liên kết này đã được sử dụng để nhập kết quả trước đó."));

        mockMvc.perform(get("/api/v1/public/inspection-result-entry/{token}", token))
                .andExpect(status().isGone())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("POST upload tệp chứng minh thành công trả về HTTP 200")
    void testUploadResultFile_Success_Returns200() throws Exception {
        String token = "valid-token";
        UUID criterionId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file", "result.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-sample".getBytes());

        when(resultService.uploadPortalResultFile(eq(token), eq(criterionId.toString()), any(), any()))
                .thenReturn("https://storage.nguongocso.vn/results/result.pdf");

        mockMvc.perform(multipart("/api/v1/public/inspection-result-entry/{token}/criteria/{criterionId}/file", token, criterionId)
                .file(file)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.filePath").value("https://storage.nguongocso.vn/results/result.pdf"));
    }

    @Test
    @DisplayName("PUT nộp kết quả thành công trả về HTTP 200 và nguồn TESTING_UNIT_PORTAL (TC-01)")
    void testSubmitResults_Success_Returns200() throws Exception {
        String token = "valid-token";
        UUID criterionId = UUID.randomUUID();

        InspectionCriterionResultResponse resultResponse = InspectionCriterionResultResponse.builder()
                .resultId(UUID.randomUUID().toString())
                .criterionId(criterionId.toString())
                .criterionName("Hàm lượng chì")
                .passed(true)
                .entrySource(InspectionResultEntrySource.TESTING_UNIT_PORTAL)
                .resultDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusMonths(6))
                .build();

        when(resultService.recordPortalResults(eq(token), any(), any(), any()))
                .thenReturn(List.of(resultResponse));

        String requestJson = "{\"results\":[{\"criterionId\":\"" + criterionId + "\",\"passed\":true,\"resultDate\":\"2026-09-16\",\"expiryDate\":\"2027-03-16\"}]}";

        mockMvc.perform(put("/api/v1/public/inspection-result-entry/{token}/results", token)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].entrySource").value("TESTING_UNIT_PORTAL"))
                .andExpect(jsonPath("$.data[0].passed").value(true));
    }

    @Test
    @DisplayName("PUT nộp kết quả lần hai bị HTTP 410 GONE chống double-submit (TC-03)")
    void testSubmitResults_DoubleSubmit_Returns410() throws Exception {
        String token = "already-used-token";
        UUID criterionId = UUID.randomUUID();

        when(resultService.recordPortalResults(eq(token), any(), any(), any()))
                .thenThrow(new BusinessException(HttpStatus.GONE, "Liên kết này đã được sử dụng để nhập kết quả trước đó."));

        String requestJson = "{\"results\":[{\"criterionId\":\"" + criterionId + "\",\"passed\":true,\"resultDate\":\"2026-09-16\",\"expiryDate\":\"2027-03-16\"}]}";

        mockMvc.perform(put("/api/v1/public/inspection-result-entry/{token}/results", token)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isGone())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
