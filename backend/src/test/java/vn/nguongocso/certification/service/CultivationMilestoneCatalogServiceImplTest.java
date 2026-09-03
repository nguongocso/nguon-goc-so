package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneCatalogRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneCatalogResponse;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import vn.nguongocso.certification.repository.CultivationMilestoneCatalogRepository;
import vn.nguongocso.certification.repository.ProductCategoryMilestoneRepository;
import vn.nguongocso.certification.service.impl.CultivationMilestoneCatalogServiceImpl;
import vn.nguongocso.exception.BusinessException;

/**
 * Tests for CultivationMilestoneCatalogServiceImpl — duplicate (name + activity),
 * PLATFORM_ADMIN-only, referenced delete. Story: NCL-09-CN-011.
 */
@ExtendWith(MockitoExtension.class)
class CultivationMilestoneCatalogServiceImplTest {

    @Mock
    private CultivationMilestoneCatalogRepository catalogRepository;

    @Mock
    private ProductCategoryMilestoneRepository categoryMilestoneRepository;

    @org.mockito.InjectMocks
    private CultivationMilestoneCatalogServiceImpl service;

    private CultivationMilestoneCatalogRequest request;

    @BeforeEach
    void setUp() {
        request = new CultivationMilestoneCatalogRequest();
        request.setName("Bón phân đợt 1");
        request.setActivityType("FERTILIZING");
        request.setExpectedDaysFromPlanting(7);
    }

    private CustomUserDetails admin() {
        CustomUserDetails user = Mockito.mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-01");
        return user;
    }

    private CustomUserDetails nonAdmin() {
        CustomUserDetails user = Mockito.mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-02");
        return user;
    }

    private CultivationMilestoneCatalog entity(Long id) {
        return CultivationMilestoneCatalog.builder()
                .id(id)
                .name(request.getName())
                .activityType(request.getActivityType())
                .expectedDaysFromPlanting(request.getExpectedDaysFromPlanting())
                .status("ACTIVE")
                .build();
    }

    // user mock without stubbing role — for GET (read-only) paths
    private CustomUserDetails plainUser() {
        return Mockito.mock(CustomUserDetails.class);
    }

    // TC-04: duplicate name + activity rejected
    @Test
    void createMilestone_shouldRejectDuplicate() {
        when(catalogRepository.existsByNameAndActivityType(
                request.getName(), request.getActivityType())).thenReturn(true);

        assertThatThrownBy(() -> service.createMilestone(request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).save(any());
    }

    // non-admin rejected
    @Test
    void createMilestone_shouldRejectNonAdmin() {
        assertThatThrownBy(() -> service.createMilestone(request, nonAdmin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).save(any());
    }

    // create success for PLATFORM_ADMIN
    @Test
    void createMilestone_shouldCreateWhenAdminAndUnique() {
        when(catalogRepository.existsByNameAndActivityType(
                request.getName(), request.getActivityType())).thenReturn(false);
        when(catalogRepository.save(any(CultivationMilestoneCatalog.class)))
                .thenAnswer(invocation -> {
                    CultivationMilestoneCatalog entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        CultivationMilestoneCatalogResponse response = service.createMilestone(request, admin());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getActivityType()).isEqualTo("FERTILIZING");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // update duplicate rejected
    @Test
    void updateMilestone_shouldRejectDuplicate() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(catalogRepository.existsByNameAndActivityTypeAndIdNot(
                request.getName(), request.getActivityType(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateMilestone(1L, request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // delete referenced milestone rejected
    @Test
    void deleteMilestone_shouldRejectWhenReferenced() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(categoryMilestoneRepository.existsByMilestone_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteMilestone(1L, admin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).delete(any());
    }

    // delete unused milestone allowed only for admin
    @Test
    void deleteMilestone_shouldAllowWhenNotReferenced() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(categoryMilestoneRepository.existsByMilestone_Id(1L)).thenReturn(false);

        service.deleteMilestone(1L, admin());

        verify(catalogRepository).delete(any(CultivationMilestoneCatalog.class));
    }

    // delete non-admin rejected
    @Test
    void deleteMilestone_shouldRejectNonAdmin() {
        assertThatThrownBy(() -> service.deleteMilestone(1L, nonAdmin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).delete(any());
    }

    // disable updates status to INACTIVE
    @Test
    void disableMilestone_shouldSetInactive() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(catalogRepository.save(any(CultivationMilestoneCatalog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.disableMilestone(1L, admin());

        verify(catalogRepository).save(any(CultivationMilestoneCatalog.class));
    }

    // enable updates status to ACTIVE
    @Test
    void enableMilestone_shouldSetActive() {
        CultivationMilestoneCatalog inactive = entity(1L);
        inactive.setStatus("INACTIVE");
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(catalogRepository.save(any(CultivationMilestoneCatalog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CultivationMilestoneCatalogResponse response = service.enableMilestone(1L, admin());

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // search passes status filter through
    @Test
    void searchMilestones_shouldLowercaseValidStatus() {
        when(categoryMilestoneRepository.findReferencedMilestoneIds()).thenReturn(java.util.List.of());
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(catalogRepository.search(org.mockito.ArgumentMatchers.nullable(String.class), eq("ACTIVE"),
                org.mockito.ArgumentMatchers.nullable(String.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(), pageable, 0));

        var result = service.searchMilestones(null, "active", null, pageable, plainUser());

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        verify(catalogRepository).search(org.mockito.ArgumentMatchers.nullable(String.class), eq("ACTIVE"),
                org.mockito.ArgumentMatchers.nullable(String.class), eq(pageable));
    }
}
