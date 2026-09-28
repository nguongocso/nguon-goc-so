package vn.nguongocso.trace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.UnlockTraceCodeRequest;
import vn.nguongocso.trace.dto.response.UnlockTraceCodeResponse;
import vn.nguongocso.trace.service.SuspectDetectionService;

@WebMvcTest(TraceCodeAdminController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TraceCodeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SuspectDetectionService suspectDetectionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private CustomUserDetails vt01User;
    private CustomUserDetails vt02User;
    private UUID adminUserId;

    private CustomUserDetails createCustomUserDetails(UUID userId, String username, String roleCode) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName(username);
        user.setFullName("Test User " + username);
        user.setPasswordHash("password");

        Organization org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        org.setName("Test Organization");
        org.setCode("TEST");
        org.setType(OrganizationType.ENTERPRISE);

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(org);
        orgUser.setUser(user);

        Role role = new Role();
        role.setRoleId(1);
        role.setCode(roleCode);
        role.setName("Role " + roleCode);

        return new CustomUserDetails(user, orgUser, role);
    }

    @BeforeEach
    void setUp() {
        adminUserId = UUID.randomUUID();
        vt01User = createCustomUserDetails(adminUserId, "admin", "VT-01");
        vt02User = createCustomUserDetails(UUID.randomUUID(), "manager", "VT-02");
    }

    @Test
    void unlockTraceCode_happyPath_shouldReturnOk() throws Exception {
        UUID traceCodeId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh vận đơn và đối soát thực tế hợp lệ.");
        request.setEvidence("Biên bản đối soát 01/2026");

        UnlockTraceCodeResponse response = UnlockTraceCodeResponse.builder()
                .id(traceCodeId)
                .codeValue("NCL0001")
                .status("ACTIVE")
                .unlockedAt(LocalDateTime.now())
                .unlockedBy(adminUserId)
                .unlockedByName("Test User admin")
                .unlockConclusion(request.getConclusion())
                .unlockEvidence(request.getEvidence())
                .verificationNote(request.getConclusion())
                .notificationSent(true)
                .build();

        when(suspectDetectionService.unlockTraceCodeWithVerification(
                eq(traceCodeId.toString()), any(UnlockTraceCodeRequest.class), eq(adminUserId), eq("Test User admin")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/trace-codes/" + traceCodeId + "/unlock")
                        .with(user(vt01User))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value("NCL0001"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.unlockConclusion").value(request.getConclusion()))
                .andExpect(jsonPath("$.data.verificationNote").value(request.getConclusion()));
    }

    @Test
    void unlockTraceCode_byCodeValue_shouldReturnOk() throws Exception {
        String codeValue = "NCL0001";
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh vận đơn và đối soát thực tế hợp lệ.");

        UnlockTraceCodeResponse response = UnlockTraceCodeResponse.builder()
                .id(UUID.randomUUID())
                .codeValue(codeValue)
                .status("ACTIVE")
                .unlockedAt(LocalDateTime.now())
                .unlockedBy(adminUserId)
                .unlockedByName("Test User admin")
                .unlockConclusion(request.getConclusion())
                .verificationNote(request.getConclusion())
                .notificationSent(true)
                .build();

        when(suspectDetectionService.unlockTraceCodeWithVerification(
                eq(codeValue), any(UnlockTraceCodeRequest.class), eq(adminUserId), eq("Test User admin")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/trace-codes/" + codeValue + "/unlock")
                        .with(user(vt01User))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value(codeValue))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void unlockTraceCode_forbiddenForNonVT01() throws Exception {
        UUID traceCodeId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh đầy đủ thông tin hợp lệ.");

        mockMvc.perform(post("/api/v1/admin/trace-codes/" + traceCodeId + "/unlock")
                        .with(user(vt02User))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unlockTraceCode_whenValidationFails_shouldReturnBadRequest() throws Exception {
        UUID traceCodeId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Ngắn");

        mockMvc.perform(post("/api/v1/admin/trace-codes/" + traceCodeId + "/unlock")
                        .with(user(vt01User))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unlockTraceCode_whenNotLocked_shouldReturnConflict() throws Exception {
        UUID traceCodeId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh vận đơn và đối soát thực tế hợp lệ.");

        when(suspectDetectionService.unlockTraceCodeWithVerification(
                eq(traceCodeId.toString()), any(UnlockTraceCodeRequest.class), eq(adminUserId), eq("Test User admin")))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "Mã tem không ở trạng thái bị khóa."));

        mockMvc.perform(post("/api/v1/admin/trace-codes/" + traceCodeId + "/unlock")
                        .with(user(vt01User))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mã tem không ở trạng thái bị khóa."));
    }

    @Test
    void unlockTraceCode_whenNotFound_shouldReturnNotFound() throws Exception {
        UUID traceCodeId = UUID.randomUUID();
        UnlockTraceCodeRequest request = new UnlockTraceCodeRequest();
        request.setConclusion("Đã xác minh vận đơn và đối soát thực tế hợp lệ.");

        when(suspectDetectionService.unlockTraceCodeWithVerification(
                eq(traceCodeId.toString()), any(UnlockTraceCodeRequest.class), eq(adminUserId), eq("Test User admin")))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy mã tem."));

        mockMvc.perform(post("/api/v1/admin/trace-codes/" + traceCodeId + "/unlock")
                        .with(user(vt01User))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy mã tem."));
    }
}