package vn.nguongocso.unit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

import vn.nguongocso.auth.dto.request.LoginRequest;
import vn.nguongocso.auth.dto.request.SelectOrganizationRequest;
import vn.nguongocso.auth.dto.response.LoginResponse;
import vn.nguongocso.auth.dto.response.SelectOrganizationResponse;
import vn.nguongocso.auth.entity.AccountLock;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.AccountLockStatus;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.AccountLockRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.AuthService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.auth.service.IpCountryResolver;
import vn.nguongocso.auth.service.LoginAnomalyDetectionService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountLockRepository accountLockRepository;

    @Mock
    private LoginAnomalyDetectionService loginAnomalyDetectionService;

        @Mock
        private IpCountryResolver ipCountryResolver;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UUID organizationId;

    private User user;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .userName("tpd01")
                .passwordHash("encoded-password")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .build();

        userDetails = org.mockito.Mockito.mock(
                CustomUserDetails.class);

        lenient()
                .when(userDetails.getUserId())
                .thenReturn(userId);

        lenient()
                .when(userDetails.getUsername())
                .thenReturn("tpd01");

        lenient()
                .when(userDetails.getFullName())
                .thenReturn("Test User");

        lenient()
                .when(userDetails.getOrganizationId())
                .thenReturn(organizationId);

        lenient()
                .when(userDetails.getOrganizationCode())
                .thenReturn("ORG-001");

        lenient()
                .when(userDetails.getOrganizationName())
                .thenReturn("Organization One");

        lenient()
                .when(userDetails.getOrganizationType())
                .thenReturn(OrganizationType.COOPERATIVE);

        lenient()
                .when(userDetails.getRoleCode())
                .thenReturn("VT-02");

        lenient()
                .when(userDetails.getRoleName())
                .thenReturn("Quản lý hợp tác xã");
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Test
    void login_shouldIssueSelectionToken_withoutAccessContext() {

        LoginRequest request = new LoginRequest();

        request.setUsername("tpd01");
        request.setPassword("password");

        when(userDetailsService.loadUser("tpd01"))
                .thenReturn(user);

        when(passwordEncoder.matches(
                "password",
                "encoded-password"))
                .thenReturn(true);

        when(tokenProvider.generateSelectionToken(user))
                .thenReturn("selection-token");

        when(tokenProvider.getSelectionTokenExpirationInSeconds())
                .thenReturn(300L);

        LoginResponse response =
                authService.login(request);

        assertThat(response)
                .isNotNull();

        assertThat(response.getSelectionToken())
                .isEqualTo("selection-token");

        assertThat(response.getTokenType())
                .isEqualTo("Bearer");

        assertThat(response.getExpiresIn())
                .isEqualTo(300L);

        assertThat(response.getUser())
                .isNotNull();

        assertThat(response.getUser().getUsername())
                .isEqualTo("tpd01");

        assertThat(response.getUser().getFullName())
                .isEqualTo("Test User");

        verify(userDetailsService)
                .loadUser("tpd01");

        verify(passwordEncoder)
                .matches(
                        "password",
                        "encoded-password");

        verify(tokenProvider)
                .generateSelectionToken(user);
    }

    @Test
    void login_shouldRejectWrongPassword() {

        LoginRequest request = new LoginRequest();

        request.setUsername("tpd01");
        request.setPassword("wrong");

        when(userDetailsService.loadUser("tpd01"))
                .thenReturn(user);

        when(passwordEncoder.matches(
                "wrong",
                "encoded-password"))
                .thenReturn(false);

        assertThatThrownBy(
                () -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Sai mật khẩu");

        org.mockito.Mockito.verify(loginAnomalyDetectionService)
                .recordLoginAttempt(
                        eq(user),
                        eq("tpd01"),
                        eq(false),
                        org.mockito.ArgumentMatchers.anyString(),
                        eq(null));
    }

    @Test
    void login_shouldRejectLockedAccountWithinSixtySeconds() {
        LoginRequest request = new LoginRequest();
        request.setUsername("tpd01");
        request.setPassword("password");

        OffsetDateTime lockTime = OffsetDateTime.now().minusSeconds(30);
        AccountLock lock = AccountLock.builder()
                .user(user)
                .lockedBy(user)
                .lockedAt(lockTime)
                .status(AccountLockStatus.LOCKED)
                .build();

        when(userDetailsService.loadUser("tpd01"))
                .thenReturn(user);
        when(accountLockRepository.findFirstByUser_UserIdAndStatusOrderByLockedAtDesc(
                userId,
                AccountLockStatus.LOCKED))
                .thenReturn(Optional.of(lock));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    String message = ex.getMessage();
                    assertThat(message)
                            .contains("Tài khoản đang bị khóa")
                            .contains("vui lòng thử lại sau ")
                            .doesNotContain("sau 60s");

                    String suffix = message.substring(message.lastIndexOf("sau ") + 4);
                    String numberPart = suffix.replace("s", "").trim();
                    int remainingSeconds = Integer.parseInt(numberPart);
                    assertThat(remainingSeconds).isBetween(1, 59);
                });
    }

    // =========================================================
    // GET ORGANIZATIONS FOR USER
    // =========================================================

    @Test
    void getOrganizationsForUser_shouldReturnActiveMemberships() {

        Organization organization =
                Organization.builder()
                        .organizationId(organizationId)
                        .name("Organization One")
                        .code("ORG-001")
                        .type(OrganizationType.COOPERATIVE)
                        .build();

        OrganizationUser membership =
                new OrganizationUser();

        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setStatus(
                OrganizationUserStatus.ACTIVE);

        vn.nguongocso.auth.entity.Role role =
                new vn.nguongocso.auth.entity.Role();

        role.setCode("VT-02");
        role.setName("Quản lý hợp tác xã");

        membership.setRole(role);

        when(
                organizationUserRepository
                        .findByUser_UserIdAndStatus(
                                userId,
                                OrganizationUserStatus.ACTIVE))
                .thenReturn(List.of(membership));

        var organizations =
                authService.getOrganizationsForUser(userId);

        assertThat(organizations)
                .hasSize(1);

        assertThat(
                organizations.get(0).getOrganizationId())
                .isEqualTo(
                        organizationId.toString());

        assertThat(
                organizations.get(0).getOrganizationCode())
                .isEqualTo("ORG-001");

        assertThat(
                organizations.get(0).getRoleCode())
                .isEqualTo("VT-02");

        assertThat(
                organizations.get(0).getRoleName())
                .isEqualTo("Quản lý hợp tác xã");
    }

    // =========================================================
    // SELECT ORGANIZATION
    // =========================================================

    @Test
    void selectOrganization_shouldIssueAccessTokenForSelectedMembership() {

        SelectOrganizationRequest request =
                new SelectOrganizationRequest();

        request.setOrganizationId(organizationId);

        OrganizationUser membership =
                new OrganizationUser();

        membership.setStatus(
                OrganizationUserStatus.ACTIVE);

        when(tokenProvider.validateToken("selection-token"))
                .thenReturn(true);

        when(tokenProvider.getTokenTypeFromToken(
                "selection-token"))
                .thenReturn(
                        JwtTokenProvider.TOKEN_TYPE_SELECTION);

        when(tokenProvider.getUserIdFromToken(
                "selection-token"))
                .thenReturn(userId);

        when(
                organizationUserRepository
                        .findByUser_UserIdAndOrganization_OrganizationId(
                                userId,
                                organizationId))
                .thenReturn(Optional.of(membership));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(
                userDetailsService
                        .loadUserByUserIdAndOrganizationId(
                                userId,
                                organizationId))
                .thenReturn(userDetails);

        when(tokenProvider.generateAccessToken(userDetails))
                .thenReturn("access-token");

        when(tokenProvider.getExpirationInSeconds())
                .thenReturn(3600L);

        SelectOrganizationResponse response =
                authService.selectOrganization(
                        "selection-token",
                        request);

        assertThat(response)
                .isNotNull();

        assertThat(response.getAccessToken())
                .isEqualTo("access-token");

        assertThat(response.getUser())
                .isNotNull();

        assertThat(
                response.getUser().getOrganizationId())
                .isEqualTo(
                        organizationId.toString());

        assertThat(
                response.getUser().getRoleCode())
                .isEqualTo("VT-02");

        verify(tokenProvider)
                .validateToken("selection-token");

        verify(tokenProvider)
                .generateAccessToken(userDetails);

        org.mockito.Mockito.verify(loginAnomalyDetectionService)
                .recordLoginAttempt(
                        eq(user),
                        eq("tpd01"),
                        eq(true),
                        org.mockito.ArgumentMatchers.anyString(),
                        eq(null));
    }

    // =========================================================
    // SWITCH ORGANIZATION
    // =========================================================

    @Test
    void switchOrganization_shouldIssueNewAccessTokenForAuthenticatedUser() {

        SelectOrganizationRequest request =
                new SelectOrganizationRequest();

        request.setOrganizationId(organizationId);

        OrganizationUser membership =
                new OrganizationUser();

        membership.setStatus(
                OrganizationUserStatus.ACTIVE);

        when(
                organizationUserRepository
                        .findByUser_UserIdAndOrganization_OrganizationId(
                                userId,
                                organizationId))
                .thenReturn(Optional.of(membership));

        when(
                userDetailsService
                        .loadUserByUserIdAndOrganizationId(
                                userId,
                                organizationId))
                .thenReturn(userDetails);

        when(tokenProvider.generateAccessToken(userDetails))
                .thenReturn("new-access-token");

        when(tokenProvider.getExpirationInSeconds())
                .thenReturn(3600L);

        SelectOrganizationResponse response =
                authService.switchOrganization(
                        userId,
                        request);

        assertThat(response)
                .isNotNull();

        assertThat(response.getAccessToken())
                .isEqualTo("new-access-token");

        assertThat(
                response.getUser().getOrganizationName())
                .isEqualTo("Organization One");

        assertThat(
                response.getUser().getOrganizationId())
                .isEqualTo(
                        organizationId.toString());

        assertThat(
                response.getUser().getRoleCode())
                .isEqualTo("VT-02");

        verify(
                tokenProvider)
                .generateAccessToken(userDetails);
    }

    @Test
    void switchOrganization_shouldRejectInactiveMembership() {

        SelectOrganizationRequest request =
                new SelectOrganizationRequest();

        request.setOrganizationId(organizationId);

        OrganizationUser membership =
                new OrganizationUser();

        membership.setStatus(
                OrganizationUserStatus.INACTIVE);

        when(
                organizationUserRepository
                        .findByUser_UserIdAndOrganization_OrganizationId(
                                userId,
                                organizationId))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(
                () -> authService.switchOrganization(
                        userId,
                        request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Tổ chức không còn hoạt động với tài khoản này");
    }
}