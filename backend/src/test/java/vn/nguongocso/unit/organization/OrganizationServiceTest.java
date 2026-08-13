package vn.nguongocso.unit.organization;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.organization.service.OrganizationService;
import vn.nguongocso.organization.service.impl.OrganizationServiceImpl;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrganizationServiceImpl organizationServiceImpl;

    private final UUID orgId = UUID.randomUUID();
    private Organization existingOrg;

    @BeforeEach
    void setUp() {

        existingOrg = new Organization();
        existingOrg.setOrganizationId(orgId);
        existingOrg.setName("HTX Xanh");
        existingOrg.setCode("HTX001");
        existingOrg.setType(OrganizationType.COOPERATIVE);
        existingOrg.setStatus(OrganizationStatus.ACTIVE);
        existingOrg.setAddress("Số 1, đường A");
        existingOrg.setPhone("0900000000");
        existingOrg.setEmail("htx@example.com");
    }

    private void mockLogin() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(userDetails.getOrganizationId()).thenReturn(orgId);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentOrganizationProfile_shouldReturnProfile_whenExists() {
        mockLogin();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));

        OrganizationProfileResponse response = organizationServiceImpl.getCurrentOrganizationProfile();
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("HTX Xanh");
        assertThat(response.getCode()).isEqualTo("HTX001");
    }

    @Test
    void getCurrentOrganizationProfile_shouldThrow_whenNotFound() {
        mockLogin();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationServiceImpl.getCurrentOrganizationProfile())
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tổ chức không tồn tại");
    }

    @Test
    void updateCurrentOrganizationProfile_shouldUpdateAndReturn() {
        mockLogin();

        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("HTX Xanh mới");
        request.setAddress("Số 2, đường B");
        request.setPhone("0987654321");
        request.setEmail("new@htx.com");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(existingOrg);

        OrganizationProfileResponse response = organizationServiceImpl.updateCurrentOrganization(request);

        assertThat(response.getName()).isEqualTo("HTX Xanh mới");
        assertThat(response.getAddress()).isEqualTo("Số 2, đường B");
        assertThat(response.getPhone()).isEqualTo("0987654321");
        assertThat(response.getEmail()).isEqualTo("new@htx.com");

        verify(organizationRepository).save(existingOrg);
    }

    @Test
    void updateCurrentOrganization_shouldThrow_whenOrgNotFound() {
        mockLogin();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("Bất kỳ");

        assertThatThrownBy(() -> organizationServiceImpl.updateCurrentOrganization(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tổ chức không tồn tại");
    }

    @Test
    void updateOrganizationById_shouldUpdate_whenAdmin() {
        UUID targetOrgId = UUID.randomUUID();
        Organization targetOrg = new Organization();
        targetOrg.setOrganizationId(targetOrgId);

        when(organizationRepository.findById(targetOrgId)).thenReturn(Optional.of(targetOrg));

        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("Tên do admin sửa");

        OrganizationProfileResponse response = organizationServiceImpl.updateOrganizationById(targetOrgId, request);

        assertThat(response.getName()).isEqualTo("Tên do admin sửa");
        verify(organizationRepository).save(targetOrg);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
