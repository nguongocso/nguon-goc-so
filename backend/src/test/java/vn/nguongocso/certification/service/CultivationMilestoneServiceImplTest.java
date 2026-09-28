package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneResponse;
import vn.nguongocso.certification.dto.response.MilestoneEligibilityResponse;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CultivationMilestoneRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.MilestoneValidationService;
import vn.nguongocso.certification.service.impl.CultivationMilestoneServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;

/**
 * Tests for CultivationMilestoneServiceImpl — trùng tên theo (loại, tiêu chuẩn),
 * PLATFORM_ADMIN-only, hợp lệ loại/tiêu chuẩn/hoạt động. Story: NCL-09-CN-011.
 */
@ExtendWith(MockitoExtension.class)
class CultivationMilestoneServiceImplTest {

    @Mock
    private CultivationMilestoneRepository milestoneRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private StandardRepository standardRepository;

    @Mock
    private MilestoneValidationService milestoneValidationService;

    @Mock
    private ProductionLotRepository productionLotRepository;

    private CultivationMilestoneServiceImpl service;

    private CultivationMilestoneRequest request;

    @BeforeEach
    void setUp() {
        service = new CultivationMilestoneServiceImpl(
                milestoneRepository, productCategoryRepository, standardRepository,
                milestoneValidationService, productionLotRepository);
        request = new CultivationMilestoneRequest();
        request.setName("Bón phân đợt 1");
        request.setActivityType("FERTILIZING");
        request.setExpectedDaysFromPlanting(7);
        request.setIsMandatory(true);
    }

    private CustomUserDetails admin() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-01");
        return user;
    }

    private CustomUserDetails nonAdmin() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-02");
        return user;
    }

    private CultivationMilestone entity(Long id) {
        return CultivationMilestone.builder()
                .id(id)
                .name(request.getName())
                .activityType(request.getActivityType())
                .expectedDaysFromPlanting(request.getExpectedDaysFromPlanting())
                .isMandatory(request.getIsMandatory())
                .build();
    }

    // user mock without stubbing role — for GET (read-only) paths
    private CustomUserDetails plainUser() {
        return mock(CustomUserDetails.class);
    }

    // TC: trùng tên trong cùng (loại, tiêu chuẩn) bị chặn
    @Test
    void createMilestone_shouldRejectDuplicateInSameCategoryAndStandard() {
        UUID categoryId = UUID.randomUUID();
        UUID standardId = UUID.randomUUID();
        request.setProductCategoryId(categoryId);
        request.setStandardId(standardId);

        // resolveCategory/resolveStandard chạy trước duplicate check
        ProductCategory category = ProductCategory.builder().id(categoryId).name("Cà phê").isActive(true).build();
        Standard standard = Standard.builder().id(standardId).name("VietGAP").isActive(true).build();
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(standardRepository.findById(standardId)).thenReturn(Optional.of(standard));
        when(milestoneRepository.existsByNameAndCategoryAndStandard(
                request.getName(), categoryId, standardId)).thenReturn(true);

        assertThatThrownBy(() -> service.createMilestone(request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // TC: non-admin bị chặn
    @Test
    void createMilestone_shouldRejectNonAdmin() {
        assertThatThrownBy(() -> service.createMilestone(request, nonAdmin()))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // TC: tạo GLOBAL (loại=null, tiêu chuẩn=null) thành công cho admin
    @Test
    void createMilestone_shouldCreateWhenAdminAndUnique() {
        when(milestoneRepository.existsByNameAndCategoryAndStandard(
                request.getName(), null, null)).thenReturn(false);
        when(milestoneRepository.save(any(CultivationMilestone.class)))
                .thenAnswer(invocation -> {
                    CultivationMilestone e = invocation.getArgument(0);
                    e.setId(1L);
                    return e;
                });

        CultivationMilestoneResponse response = service.createMilestone(request, admin());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getActivityType()).isEqualTo("FERTILIZING");
        assertThat(response.isMandatory()).isTrue();
    }

    // TC: loại nông sản không tồn tại bị chặn
    @Test
    void createMilestone_shouldRejectWhenCategoryNotFound() {
        UUID categoryId = UUID.randomUUID();
        request.setProductCategoryId(categoryId);

        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMilestone(request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // TC: tiêu chuẩn không tồn tại bị chặn
    @Test
    void createMilestone_shouldRejectWhenStandardNotFound() {
        UUID standardId = UUID.randomUUID();
        request.setStandardId(standardId);

        when(standardRepository.findById(standardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMilestone(request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // TC: loại hoạt động không hợp lệ bị chặn
    @Test
    void createMilestone_shouldRejectInvalidActivityType() {
        request.setActivityType("KHONG_HOP_LE");

        assertThatThrownBy(() -> service.createMilestone(request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(milestoneRepository, never()).save(any());
    }

    // TC: cập nhật trùng tên bị chặn (loại trừ chính nó)
    @Test
    void updateMilestone_shouldRejectDuplicate() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(milestoneRepository.existsByNameAndCategoryAndStandardAndIdNot(
                request.getName(), null, null, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateMilestone(1L, request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // TC: cập nhật thành công khi hợp lệ
    @Test
    void updateMilestone_shouldUpdateWhenUnique() {
        CultivationMilestone existing = entity(1L);
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(milestoneRepository.existsByNameAndCategoryAndStandardAndIdNot(
                request.getName(), null, null, 1L)).thenReturn(false);
        when(milestoneRepository.save(any(CultivationMilestone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CultivationMilestoneResponse response = service.updateMilestone(1L, request, admin());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo(request.getName());
    }

    // TC: search truyền bộ lọc qua repo
    @Test
    void searchMilestones_shouldPassFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(milestoneRepository.search(nullable(String.class), nullable(String.class),
                nullable(UUID.class), nullable(UUID.class), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(), pageable, 0));

        var result = service.searchMilestones(null, null, null, null, false, pageable, plainUser());

        assertThat(result.getTotalElements()).isZero();
        verify(milestoneRepository).search(nullable(String.class), nullable(String.class),
                nullable(UUID.class), nullable(UUID.class), org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(pageable));
    }

    // ===== NCL-09-CN-011: getPackagingEligibility =====

    private ProductionLot lotOfOrg(UUID lotId, UUID orgId) {
        Organization organization = Organization.builder()
                .organizationId(orgId)
                .name("HTX Nông sản sạch")
                .build();
        return ProductionLot.builder().id(lotId).organization(organization).build();
    }

    // TC: lô không tồn tại -> ResourceNotFoundException (404)
    @Test
    void getPackagingEligibility_shouldThrowWhenLotNotFound() {
        UUID lotId = UUID.randomUUID();
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPackagingEligibility(lotId, plainUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy lô sản xuất.");
        verify(milestoneValidationService, never()).findMissingMilestones(any());
    }

    // TC: user khác tổ chức -> 403, không gọi validate
    @Test
    void getPackagingEligibility_shouldRejectForeignOrganization() {
        UUID lotId = UUID.randomUUID();
        ProductionLot lot = lotOfOrg(lotId, UUID.randomUUID());
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-02");
        when(user.getOrganizationId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> service.getPackagingEligibility(lotId, user))
                .isInstanceOf(BusinessException.class);
        verify(milestoneValidationService, never()).findMissingMilestones(any());
    }

    // TC: VT-01 bỏ qua ranh giới tổ chức
    @Test
    void getPackagingEligibility_shouldBypassOrganizationForAdmin() {
        UUID lotId = UUID.randomUUID();
        ProductionLot lot = lotOfOrg(lotId, UUID.randomUUID());
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));
        when(milestoneValidationService.findMissingMilestones(lot)).thenReturn(List.of());

        MilestoneEligibilityResponse response =
                service.getPackagingEligibility(lotId, admin());

        assertThat(response.isEligible()).isTrue();
        assertThat(response.getProductionLotId()).isEqualTo(lotId);
        assertThat(response.getMissingMilestones()).isEmpty();
    }

    // TC: cùng tổ chức, đủ mốc -> eligible
    @Test
    void getPackagingEligibility_shouldReturnEligibleWhenNoMissing() {
        UUID lotId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        ProductionLot lot = lotOfOrg(lotId, orgId);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-02");
        when(user.getOrganizationId()).thenReturn(orgId);
        when(milestoneValidationService.findMissingMilestones(lot)).thenReturn(List.of());

        MilestoneEligibilityResponse response = service.getPackagingEligibility(lotId, user);

        assertThat(response.isEligible()).isTrue();
        assertThat(response.getMissingMilestones()).isEmpty();
    }

    // TC: thiếu mốc -> trả đúng tên + activityType
    @Test
    void getPackagingEligibility_shouldReturnMissingMilestones() {
        UUID lotId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        ProductionLot lot = lotOfOrg(lotId, orgId);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(lot));

        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-02");
        when(user.getOrganizationId()).thenReturn(orgId);

        CultivationMilestone missingFertilizing = CultivationMilestone.builder()
                .id(2L).name("Bón phân đợt 2").activityType("FERTILIZING")
                .isMandatory(true).build();
        when(milestoneValidationService.findMissingMilestones(lot))
                .thenReturn(List.of(missingFertilizing));

        MilestoneEligibilityResponse response = service.getPackagingEligibility(lotId, user);

        assertThat(response.isEligible()).isFalse();
        assertThat(response.getMissingMilestones()).hasSize(1);
        assertThat(response.getMissingMilestones().get(0).getName()).isEqualTo("Bón phân đợt 2");
        assertThat(response.getMissingMilestones().get(0).getActivityType())
                .isEqualTo("FERTILIZING");
    }
}