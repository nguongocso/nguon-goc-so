package vn.nguongocso.unit.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.controller.LoginMonitoringController;
import vn.nguongocso.auth.dto.request.LockAccountRequest;
import vn.nguongocso.auth.dto.response.AccountLockResponse;
import vn.nguongocso.auth.dto.response.LoginAnomalyResponse;
import vn.nguongocso.auth.dto.response.LoginHistoryResponse;
import vn.nguongocso.auth.service.AccountLockService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.auth.service.LoginMonitoringService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.permission.service.PermissionChecker;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginMonitoringController.class)
@ActiveProfiles("test")
@DisplayName("LoginMonitoringController Integration Tests")
class LoginMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginMonitoringService loginMonitoringService;

    @MockitoBean
    private AccountLockService accountLockService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    @MockitoBean(name = "jwtTokenProvider")
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails userDetails;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        userDetails = mock(CustomUserDetails.class);

        when(userDetails.getUserId()).thenReturn(userId);
        when(userDetails.getUsername()).thenReturn("admin01");
        when(userDetails.getFullName()).thenReturn("Admin User");
        when(userDetails.getRoleCode()).thenReturn("VT-01");
        when(userDetails.getRoleName()).thenReturn("Quản trị viên");
        when(userDetails.getOrganizationId()).thenReturn(organizationId);
        when(userDetails.getOrganizationCode()).thenReturn("ADMIN_ORG");
        when(userDetails.getOrganizationName()).thenReturn("Organization");
        when(userDetails.getOrganizationType()).thenReturn(OrganizationType.SYSTEM);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        // Mock PermissionChecker to allow all operations in tests
        doNothing().when(permissionChecker).check(any(), any());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("should return login history with pagination")
    void getLoginHistory_shouldReturnPaginatedList() throws Exception {
        // Arrange
        List<LoginHistoryResponse> mockHistories = List.of(
                createLoginHistoryResponse(),
                createLoginHistoryResponse()
        );
        PageResponse<LoginHistoryResponse> mockPageResponse = new PageResponse<LoginHistoryResponse>(
                mockHistories,
                0,
                10,
                2,
                1,
                true,
                true
        );

        when(loginMonitoringService.getLoginHistory(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(mockPageResponse);

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        verify(loginMonitoringService, times(1)).getLoginHistory(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("should filter login history by user ID")
    void getLoginHistory_shouldFilterByUserId() throws Exception {
        // Arrange
        UUID filteredUserId = UUID.randomUUID();
        List<LoginHistoryResponse> mockHistories = List.of(
                createLoginHistoryResponse()
        );
        PageResponse<LoginHistoryResponse> mockPageResponse = new PageResponse<LoginHistoryResponse>(
                mockHistories,
                0,
                10,
                1,
                1,
                true,
                true
        );

        when(loginMonitoringService.getLoginHistory(
                eq(filteredUserId),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(mockPageResponse);

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", filteredUserId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loginMonitoringService, times(1)).getLoginHistory(
                eq(filteredUserId),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("should filter login history by result (SUCCESS/FAILED)")
    void getLoginHistory_shouldFilterByResult() throws Exception {
        // Arrange
        String result = "SUCCESS";
        List<LoginHistoryResponse> mockHistories = new ArrayList<>();
        PageResponse<LoginHistoryResponse> mockPageResponse = new PageResponse<LoginHistoryResponse>(
                mockHistories,
                0,
                10,
                0,
                0,
                true,
                true
        );

        when(loginMonitoringService.getLoginHistory(
                any(),
                eq(result),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(mockPageResponse);

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("result", result)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loginMonitoringService, times(1)).getLoginHistory(
                any(),
                eq(result),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("should return login anomalies with pagination")
    void getLoginAnomalies_shouldReturnPaginatedList() throws Exception {
        // Arrange
        List<LoginAnomalyResponse> mockAnomalies = List.of(
                createLoginAnomalyResponse(),
                createLoginAnomalyResponse()
        );
        PageResponse<LoginAnomalyResponse> mockPageResponse = new PageResponse<LoginAnomalyResponse>(
                mockAnomalies,
                0,
                10,
                2,
                1,
                true,
                true
        );

        when(loginMonitoringService.getLoginAnomalies(
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(mockPageResponse);

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2));

        verify(loginMonitoringService, times(1)).getLoginAnomalies(
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("should filter login anomalies by status")
    void getLoginAnomalies_shouldFilterByStatus() throws Exception {
        // Arrange
        String status = "OPEN";
        List<LoginAnomalyResponse> mockAnomalies = List.of(
                createLoginAnomalyResponse()
        );
        PageResponse<LoginAnomalyResponse> mockPageResponse = new PageResponse<LoginAnomalyResponse>(
                mockAnomalies,
                0,
                10,
                1,
                1,
                true,
                true
        );

        when(loginMonitoringService.getLoginAnomalies(
                eq(status),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(mockPageResponse);

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("status", status)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loginMonitoringService, times(1)).getLoginAnomalies(
                eq(status),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("should filter login anomalies by reason code")
    void getLoginAnomalies_shouldFilterByReasonCode() throws Exception {
        // Arrange
        String reasonCode = "REPEATED_FAILED_LOGIN";
        List<LoginAnomalyResponse> mockAnomalies = List.of(
                createLoginAnomalyResponse()
        );
        PageResponse<LoginAnomalyResponse> mockPageResponse = new PageResponse<LoginAnomalyResponse>(
                mockAnomalies,
                0,
                10,
                1,
                1,
                true,
                true
        );

        when(loginMonitoringService.getLoginAnomalies(
                any(),
                eq(reasonCode),
                any(),
                any(),
                any()))
                .thenReturn(mockPageResponse);

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-anomalies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("reasonCode", reasonCode)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loginMonitoringService, times(1)).getLoginAnomalies(
                any(),
                eq(reasonCode),
                any(),
                any(),
                any());
    }

    @Test
    @DisplayName("should lock account successfully")
    void lockAccount_shouldLockAccountSuccessfully() throws Exception {
        // Arrange
        UUID accountId = UUID.randomUUID();
        UUID anomalyId = UUID.randomUUID();

        LockAccountRequest request = new LockAccountRequest();
        request.setAnomalyId(anomalyId);
        request.setReason("Phát hiện đăng nhập bất thường");

        AccountLockResponse mockResponse = AccountLockResponse.builder()
                .accountId(accountId)
                .status("LOCKED")
                .lockedBy("admin")
                .lockedAt(OffsetDateTime.now())
                .build();

        when(loginMonitoringService.lockAccount(
                eq(accountId),
                eq(anomalyId),
                any(String.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(
                patch("/api/v1/auth/security/accounts/{accountId}/lock", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        verify(loginMonitoringService, times(1)).lockAccount(
                eq(accountId),
                eq(anomalyId),
                any(String.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false));
    }

    @Test
    @DisplayName("should lock account without anomaly ID")
    void lockAccount_shouldLockWithoutAnomalyId() throws Exception {
        // Arrange
        UUID accountId = UUID.randomUUID();

        LockAccountRequest request = new LockAccountRequest();
        request.setAnomalyId(null);
        request.setReason("Manual lock - suspicious activity");

        AccountLockResponse mockResponse = AccountLockResponse.builder()
                .accountId(accountId)
                .status("LOCKED")
                .lockedBy("admin")
                .lockedAt(OffsetDateTime.now())
                .build();

        when(loginMonitoringService.lockAccount(
                eq(accountId),
                eq(null),
                any(String.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(
                patch("/api/v1/auth/security/accounts/{accountId}/lock", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(loginMonitoringService, times(1)).lockAccount(
                eq(accountId),
                eq(null),
                any(String.class),
                eq(0),
                eq(0),
                eq(0),
                eq(false));
    }

    @Test
    @DisplayName("should unlock account successfully")
    void unlockAccount_shouldUnlockAccountSuccessfully() throws Exception {
        // Arrange
        UUID accountId = UUID.randomUUID();

        AccountLockResponse mockResponse = AccountLockResponse.builder()
                .accountId(accountId)
                .status("ACTIVE")
                .unlockedBy("admin")
                .unlockedAt(OffsetDateTime.now())
                .build();

        when(loginMonitoringService.unlockAccount(eq(accountId)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(
                patch("/api/v1/auth/security/accounts/{accountId}/unlock", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(loginMonitoringService, times(1)).unlockAccount(eq(accountId));
    }

    @Test
    @DisplayName("should return 401 when not authenticated")
    void getLoginHistory_shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        mockMvc.perform(
                get("/api/v1/auth/security/login-history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // Helper methods
    private LoginHistoryResponse createLoginHistoryResponse() {
        LoginHistoryResponse response = new LoginHistoryResponse();
        response.setId(UUID.randomUUID());
        response.setUserId(UUID.randomUUID());
        response.setUsernameInput("testuser");
        response.setRoleCode("VT-02");
        response.setResult("SUCCESS");
        response.setIpAddress("192.168.1.1");
        response.setCountryCode("VN");
        response.setIsNewCountry(false);
        response.setCreatedAt(OffsetDateTime.now());
        return response;
    }

    private LoginAnomalyResponse createLoginAnomalyResponse() {
        LoginAnomalyResponse response = new LoginAnomalyResponse();
        response.setId(UUID.randomUUID());
        response.setUserId(UUID.randomUUID());
        response.setUsername("testuser");
        response.setOrganizationId(organizationId);
        response.setReasonCode("REPEATED_FAILED_LOGIN");
        response.setAttemptCount(5);
        response.setIpAddress("192.168.1.1");
        response.setCountryCode("VN");
        response.setStatus("OPEN");
        response.setDetectedAt(OffsetDateTime.now());
        return response;
    }
}
