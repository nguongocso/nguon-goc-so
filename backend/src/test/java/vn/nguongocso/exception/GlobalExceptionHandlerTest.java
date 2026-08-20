package vn.nguongocso.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GlobalExceptionHandlerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GlobalExceptionHandler handler;

    private UUID userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GlobalExceptionHandler(eventPublisher);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CustomUserDetails buildCustomUserDetails() {
        User user = User.builder()
                .userId(userId)
                .userName("admin")
                .fullName("Admin Hệ Thống")
                .build();

        Role role = new Role();
        role.setCode("VT-02");
        role.setName("Quản lý hợp tác xã");

        Organization organization = Organization.builder()
                .organizationId(UUID.randomUUID())
                .name("HTX Nông sản sạch")
                .code("HTX01")
                .type(OrganizationType.COOPERATIVE)
                .build();

        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setOrganization(organization);

        return new CustomUserDetails(user, organizationUser, role);
    }

    private HttpServletRequest monitoringRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }

    @Test
    @DisplayName("TC-03: 403 trả envelope đúng spec với errors=ACCESS_DENIED và path")
    void handleAccessDenied_ReturnsSpecEnvelope() {
        HttpServletRequest request = monitoringRequest("/api/v1/admin/monitoring/system-status");

        ResponseEntity<ApiResult<Void>> response = handler.handleAccessDenied(
                new AccessDeniedException("Access Denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        ApiResult<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.isSuccess());
        assertEquals(403, body.getStatus());
        assertEquals("Bạn không có quyền thực hiện chức năng này", body.getMessage());
        assertEquals("ACCESS_DENIED", body.getErrors());
        assertEquals("/api/v1/admin/monitoring/system-status", body.getPath());
    }

    @Test
    @DisplayName("403: message mặc định framework 'Access is denied' được thay bằng tiếng Việt")
    void handleAccessDenied_ReplacesFrameworkDefaultMessage() {
        HttpServletRequest request = monitoringRequest("/api/v1/admin/monitoring/system-status");

        ResponseEntity<ApiResult<Void>> response = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), request);

        assertEquals("Bạn không có quyền thực hiện chức năng này", response.getBody().getMessage());
    }

    @Test
    @DisplayName("403: message tiếng Việt do service tự ném được giữ nguyên")
    void handleAccessDenied_KeepsServiceSpecificMessage() {
        HttpServletRequest request = monitoringRequest("/api/v1/admin/monitoring/system-status");

        ResponseEntity<ApiResult<Void>> response = handler.handleAccessDenied(
                new AccessDeniedException("Bạn không có quyền truy cập lô hàng này."), request);

        assertEquals("Bạn không có quyền truy cập lô hàng này.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("TC-03: Truy cập trái phép endpoint giám sát được ghi activity_logs")
    void handleAccessDenied_PublishesAuditEvent() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(buildCustomUserDetails(), null,
                        buildCustomUserDetails().getAuthorities()));

        HttpServletRequest request = monitoringRequest("/api/v1/admin/monitoring/system-status");

        handler.handleAccessDenied(new AccessDeniedException("Access Denied"), request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        Object published = captor.getValue();
        assertTrue(published instanceof ActivityLogEvent);
        ActivityLogEvent event = (ActivityLogEvent) published;
        assertEquals("ACCESS_DENIED", event.getAction());
        assertEquals("SYSTEM_MONITORING", event.getEntityType());
        assertEquals(userId, event.getUserId());
        assertTrue(event.getDescription().contains("GET /api/v1/admin/monitoring/system-status"));
    }

    @Test
    @DisplayName("403 cho endpoint không phải giám sát không ghi audit")
    void handleAccessDenied_DoesNotPublishForOtherPaths() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(buildCustomUserDetails(), null,
                        buildCustomUserDetails().getAuthorities()));

        HttpServletRequest request = monitoringRequest("/api/v1/admin/trace-codes");

        handler.handleAccessDenied(new AccessDeniedException("Access Denied"), request);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("403 khi principal không phải CustomUserDetails không ghi audit")
    void handleAccessDenied_DoesNotPublishWithoutUserPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null));

        HttpServletRequest request = monitoringRequest("/api/v1/admin/monitoring/system-status");

        handler.handleAccessDenied(new AccessDeniedException("Access Denied"), request);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}