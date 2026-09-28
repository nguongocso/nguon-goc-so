package vn.nguongocso.certification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.certification.dto.response.CertificationResponse;
import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;

/** Kiểm thử contract multipart khi tổ chức nộp chứng nhận. */
@WebMvcTest(CertificationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CertificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CertificationService certificationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-02")
    void createCertificationAcceptsMetadataAndDocument() throws Exception {
        UUID certificationId = UUID.randomUUID();
        MockMultipartFile data = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                ("{\"standardId\":\"" + UUID.randomUUID()
                        + "\",\"code\":\"VGP-001\",\"issuedBy\":\"Trung tâm Chứng nhận\""
                        + ",\"issueDate\":\"2026-01-01\",\"expiryDate\":\"2027-01-01\"}").getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "file", "vietgap.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-test".getBytes());
        when(certificationService.createCertification(any(), any(), any())).thenReturn(
                CertificationResponse.builder()
                        .id(certificationId)
                        .name("VietGAP")
                        .code("VGP-001")
                        .issuedBy("Trung tâm Chứng nhận")
                        .issueDate(LocalDate.of(2026, 1, 1))
                        .expiryDate(LocalDate.of(2027, 1, 1))
                        .isValid(true)
                        .build());

        mockMvc.perform(multipart("/api/v1/certifications")
                        .file(data)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(certificationId.toString()));
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void createCertificationRejectsWrongRole() throws Exception {
        MockMultipartFile data = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                ("{\"standardId\":\"" + UUID.randomUUID()
                        + "\",\"code\":\"VGP-002\",\"issuedBy\":\"Trung tâm Chứng nhận\""
                        + ",\"issueDate\":\"2026-01-01\",\"expiryDate\":\"2027-01-01\"}").getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "vietgap.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF".getBytes());

        mockMvc.perform(multipart("/api/v1/certifications").file(data).file(file).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
