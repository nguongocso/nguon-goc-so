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
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.enums.UserStatus;
import vn.nguongocso.auth.repository.RoleRepository;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.dto.request.OrganizationUpdateRequest;
import vn.nguongocso.organization.dto.response.OrganizationProfileResponse;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import org.springframework.context.ApplicationEventPublisher;
import vn.nguongocso.organization.entity.AdministrativeUnit;
import vn.nguongocso.organization.enums.AdministrativeUnitLevel;
import vn.nguongocso.organization.repository.AdministrativeUnitRepository;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AdministrativeUnitRepository administrativeUnitRepository;

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
    void updateCurrentOrganizationProfile_withDivisions_shouldUpdateAndReturn() {
        mockLogin();

        UUID provinceId = UUID.randomUUID();
        UUID communeId = UUID.randomUUID();

        AdministrativeUnit province = AdministrativeUnit.builder()
                .id(provinceId)
                .code("36")
                .name("Ninh Bình")
                .level(AdministrativeUnitLevel.PROVINCE)
                .active(true)
                .build();

        AdministrativeUnit commune = AdministrativeUnit.builder()
                .id(communeId)
                .code("04098")
                .name("Hoa Lư")
                .level(AdministrativeUnitLevel.COMMUNE)
                .province(province)
                .active(true)
                .build();

        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("HTX Nông nghiệp Hoa Lư");
        request.setAddress("Thôn 1");
        request.setProvinceId(provinceId);
        request.setCommuneId(communeId);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));
        when(organizationRepository.save(any(Organization.class))).thenReturn(existingOrg);
        when(administrativeUnitRepository.findById(provinceId)).thenReturn(Optional.of(province));
        when(administrativeUnitRepository.findById(communeId)).thenReturn(Optional.of(commune));

        OrganizationProfileResponse response = organizationServiceImpl.updateCurrentOrganization(request);

        assertThat(response.getName()).isEqualTo("HTX Nông nghiệp Hoa Lư");
        assertThat(response.getProvinceId()).isEqualTo(provinceId);
        assertThat(response.getProvinceName()).isEqualTo("Ninh Bình");
        assertThat(response.getCommuneId()).isEqualTo(communeId);
        assertThat(response.getCommuneName()).isEqualTo("Hoa Lư");
    }

    @Test
    void updateCurrentOrganizationProfile_invalidCommuneParent_shouldThrow() {
        mockLogin();

        UUID provinceId = UUID.randomUUID();
        UUID otherProvinceId = UUID.randomUUID();
        UUID communeId = UUID.randomUUID();

        AdministrativeUnit province = AdministrativeUnit.builder()
                .id(provinceId)
                .level(AdministrativeUnitLevel.PROVINCE)
                .active(true)
                .build();

        AdministrativeUnit otherProvince = AdministrativeUnit.builder()
                .id(otherProvinceId)
                .level(AdministrativeUnitLevel.PROVINCE)
                .active(true)
                .build();

        AdministrativeUnit commune = AdministrativeUnit.builder()
                .id(communeId)
                .level(AdministrativeUnitLevel.COMMUNE)
                .province(otherProvince)
                .active(true)
                .build();

        OrganizationUpdateRequest request = new OrganizationUpdateRequest();
        request.setName("HTX Lỗi");
        request.setProvinceId(provinceId);
        request.setCommuneId(communeId);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));
        when(administrativeUnitRepository.findById(provinceId)).thenReturn(Optional.of(province));
        when(administrativeUnitRepository.findById(communeId)).thenReturn(Optional.of(commune));

        assertThatThrownBy(() -> organizationServiceImpl.updateCurrentOrganization(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Xã/phường không thuộc tỉnh/thành phố đã chọn");
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
    void getOrganizationDetail_shouldUseUserStatusForMemberDisplay() {
        UUID memberUserId = UUID.randomUUID();
        User user = new User();
        user.setUserId(memberUserId);
        user.setUserName("member01");
        user.setFullName("Member One");
        user.setEmail("member@example.com");
        user.setPhone("0909090909");
        user.setStatus(UserStatus.INACTIVE);

        Role role = new Role();
        role.setRoleId(3);
        role.setCode("VT-03");
        role.setName("Người ghi sự kiện");

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setId(UUID.randomUUID());
        orgUser.setOrganization(existingOrg);
        orgUser.setUser(user);
        orgUser.setRole(role);
        orgUser.setStatus(OrganizationUserStatus.ACTIVE);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(existingOrg));
        when(organizationUserRepository.findByOrganization_OrganizationIdAndStatus(orgId, OrganizationUserStatus.ACTIVE))
                .thenReturn(java.util.List.of(orgUser));

        var response = organizationServiceImpl.getOrganizationDetail(orgId);

        assertThat(response.getMembers()).hasSize(1);
        assertThat(response.getMembers().get(0).getStatus()).isEqualTo(UserStatus.INACTIVE);
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

    /**
     * Dropdown tổ chức nhận của phiếu bàn giao chỉ cho chọn Doanh nghiệp thu mua
     * (VT-04 / ENTERPRISE): tổ chức COOPERATIVE/GOVERNMENT/SYSTEM bị lọc bỏ dù
     * ACTIVE và khác tổ chức hiện tại.
     */
    @Test
    void getRecipientOrganizations_shouldOnlyReturnEnterpriseOrgs() {
        mockLogin();

        Organization enterpriseOrg = new Organization();
        enterpriseOrg.setOrganizationId(UUID.randomUUID());
        enterpriseOrg.setName("DN Thu mua Xanh");
        enterpriseOrg.setCode("DN001");
        enterpriseOrg.setType(OrganizationType.ENTERPRISE);
        enterpriseOrg.setStatus(OrganizationStatus.ACTIVE);

        Organization cooperativeOrg = new Organization();
        cooperativeOrg.setOrganizationId(UUID.randomUUID());
        cooperativeOrg.setName("HTX Khác");
        cooperativeOrg.setCode("HTX002");
        cooperativeOrg.setType(OrganizationType.COOPERATIVE);
        cooperativeOrg.setStatus(OrganizationStatus.ACTIVE);

        when(organizationRepository.findByStatusAndOrganizationIdNot(OrganizationStatus.ACTIVE, orgId))
                .thenReturn(java.util.List.of(enterpriseOrg, cooperativeOrg));

        var response = organizationServiceImpl.getRecipientOrganizations();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getCode()).isEqualTo("DN001");
        assertThat(response.get(0).getType()).isEqualTo(OrganizationType.ENTERPRISE);
    }
}
