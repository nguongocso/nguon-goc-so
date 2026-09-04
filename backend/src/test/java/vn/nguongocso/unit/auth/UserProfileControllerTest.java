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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.controller.UserProfileController;
import vn.nguongocso.auth.dto.request.ChangePasswordRequest;
import vn.nguongocso.auth.dto.request.UpdateUserProfileRequest;
import vn.nguongocso.auth.dto.response.AvatarUploadResponse;
import vn.nguongocso.auth.dto.response.UserProfileResponse;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.auth.service.UserService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.permission.service.PermissionChecker;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test cho UserProfileController sử dụng MockMvc (NCL-01-CN-010).
 */
@WebMvcTest(UserProfileController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    @MockitoBean
    private UserRepository userRepository;

    private CustomUserDetails userDetails;
    private UUID userId;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getUserId()).thenReturn(userId);
        lenient().when(userDetails.getUsername()).thenReturn("farmer01");
        lenient().when(userDetails.getFullName()).thenReturn("Nguyễn Văn Nông Dân");
        lenient().when(userDetails.getRoleCode()).thenReturn("VT-03");
        lenient().when(userDetails.getRoleName()).thenReturn("Người ghi sự kiện");
        lenient().when(userDetails.getOrganizationId()).thenReturn(orgId);
        lenient().when(userDetails.getOrganizationCode()).thenReturn("HTX_XANH");
        lenient().when(userDetails.getOrganizationName()).thenReturn("Hợp tác xã Nông nghiệp Xanh");
        lenient().when(userDetails.getOrganizationType()).thenReturn(OrganizationType.COOPERATIVE);

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/users/profile - Đã xác thực trả về 200")
    void testGetProfile_Authenticated_Returns200() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(userId)
                .username("farmer01")
                .fullName("Nguyễn Văn Nông Dân")
                .phone("0987654321")
                .email("farmer01@example.com")
                .roleCode("VT-03")
                .roleName("Người ghi sự kiện")
                .organizationId(orgId)
                .organizationCode("HTX_XANH")
                .organizationName("Hợp tác xã Nông nghiệp Xanh")
                .organizationType(OrganizationType.COOPERATIVE)
                .permissions(List.of("farm_log.CREATE"))
                .build();

        when(userService.getCurrentUserProfile(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("farmer01"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Nông Dân"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/profile - Cập nhật hồ sơ trả về 200")
    void testUpdateProfile_Valid_Returns200() throws Exception {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .fullName("Nguyễn Văn Nông Dân Mới")
                .phone("0912345678")
                .email("new.email@example.com")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(userId)
                .username("farmer01")
                .fullName("Nguyễn Văn Nông Dân Mới")
                .phone("0912345678")
                .email("new.email@example.com")
                .build();

        when(userService.updateUserProfile(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Nông Dân Mới"))
                .andExpect(jsonPath("$.data.phone").value("0912345678"));
    }

    @Test
    @DisplayName("POST /api/v1/users/change-password - Đổi mật khẩu hợp lệ trả về 200")
    void testChangePassword_Valid_Returns200() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("NewPassword@456")
                .confirmNewPassword("NewPassword@456")
                .build();

        doNothing().when(userService).changePassword(any(), any());

        mockMvc.perform(post("/api/v1/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/users/avatar - Tải lên ảnh đại diện trả về 200")
    void testUploadAvatar_Valid_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "dummy image content".getBytes()
        );

        AvatarUploadResponse response = AvatarUploadResponse.builder()
                .avatarUrl("/uploads/avatars/avatar.png")
                .build();

        when(userService.uploadAvatar(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/users/avatar")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").value("/uploads/avatars/avatar.png"));
    }
}
