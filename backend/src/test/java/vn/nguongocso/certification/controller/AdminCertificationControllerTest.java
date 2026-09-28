package vn.nguongocso.certification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;

/** Kiểm thử lớp phân quyền HTTP cho màn hình xác thực chứng nhận. */
@WebMvcTest(AdminCertificationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AdminCertificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CertificationService certificationService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-02")
    void listRejectsNonPlatformAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/certifications"))
                .andExpect(status().isForbidden());
    }
}
