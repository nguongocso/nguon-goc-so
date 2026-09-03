package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionCatalogRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.service.impl.InspectionCriterionCatalogServiceImpl;
import vn.nguongocso.exception.BusinessException;

/**
 * Tests for InspectionCriterionCatalogServiceImpl — business rules
 * BR-1 (duplicate), BR-2 (threshold > 0), BR-4 (PLATFORM_ADMIN only),
 * BR-5 (referenced delete).
 * Story: NCL-09-CN-009
 */
@ExtendWith(MockitoExtension.class)
class InspectionCriterionCatalogServiceImplTest {

    @Mock
    private InspectionCriterionCatalogRepository catalogRepository;

    @Mock
    private InspectionCriterionRepository inspectionCriterionRepository;

    @Mock
    private vn.nguongocso.certification.repository.CategoryCriterionRepository categoryCriterionRepository;

    @org.mockito.InjectMocks
    private InspectionCriterionCatalogServiceImpl service;

    private InspectionCriterionCatalogRequest request;

    @BeforeEach
    void setUp() {
        request = new InspectionCriterionCatalogRequest();
        request.setName("Dư lượng thuốc bảo vệ thực vật");
        request.setUnit("mg/kg");
        request.setMaxThreshold(new BigDecimal("0.5"));
        request.setReferenceStandard("QCVN 8-2:2011/BYT");
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

    private InspectionCriterionCatalog entity(Long id) {
        return InspectionCriterionCatalog.builder()
                .id(id)
                .name(request.getName())
                .unit(request.getUnit())
                .maxThreshold(request.getMaxThreshold())
                .referenceStandard(request.getReferenceStandard())
                .status("ACTIVE")
                .build();
    }
// BR-1: duplicate name + standard rejected
    @Test
    void createCriterion_shouldRejectDuplicate() {
        when(catalogRepository.existsByNameAndReferenceStandard(
                request.getName(), request.getReferenceStandard())).thenReturn(true);

        assertThatThrownBy(() -> service.createCriterion(request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // BR-2: threshold <= 0 rejected
    @Test
    void createCriterion_shouldRejectZeroThreshold() {
        request.setMaxThreshold(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.createCriterion(request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createCriterion_shouldRejectNegativeThreshold() {
        request.setMaxThreshold(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.createCriterion(request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // BR-4: non-admin rejected
    @Test
    void createCriterion_shouldRejectNonAdmin() {
        assertThatThrownBy(() -> service.createCriterion(request, nonAdmin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).save(any());
    }

    // create success for PLATFORM_ADMIN
    @Test
    void createCriterion_shouldCreateWhenAdminAndUnique() {
        when(catalogRepository.existsByNameAndReferenceStandard(
                request.getName(), request.getReferenceStandard())).thenReturn(false);
        when(inspectionCriterionRepository.existsByCriterionId(any())).thenReturn(false);
        when(catalogRepository.save(any(InspectionCriterionCatalog.class)))
                .thenAnswer(invocation -> {
                    InspectionCriterionCatalog entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        InspectionCriterionCatalogResponse response = service.createCriterion(request, admin());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // update duplicate rejected
    @Test
    void updateCriterion_shouldRejectDuplicate() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(catalogRepository.existsByNameAndReferenceStandardAndIdNot(
                request.getName(), request.getReferenceStandard(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateCriterion(1L, request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // BR-5: delete referenced criterion rejected
    @Test
    void deleteCriterion_shouldRejectWhenReferenced() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(inspectionCriterionRepository.existsByCriterionId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCriterion(1L, admin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).delete(any());
    }

    // BR-3 invariant: delete last criterion of a mandatory category rejected
    @Test
    void deleteCriterion_shouldRejectWhenAssignedToMandatoryCategory() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(inspectionCriterionRepository.existsByCriterionId(1L)).thenReturn(false);
        when(categoryCriterionRepository.existsByCriterion_IdAndCategory_RequiresInspectionTrue(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.deleteCriterion(1L, admin()))
                .isInstanceOf(BusinessException.class);
        verify(catalogRepository, never()).delete(any());
    }

    // delete unused criterion allowed only for admin
    @Test
    void deleteCriterion_shouldAllowWhenNotReferenced() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));
        when(inspectionCriterionRepository.existsByCriterionId(1L)).thenReturn(false);

        service.deleteCriterion(1L, admin());

        verify(catalogRepository).delete(any(InspectionCriterionCatalog.class));
    }

    // BR-5: disable referenced criterion is allowed
    @Test
    void disableCriterion_shouldAllowWhenReferenced() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));

        service.disableCriterion(1L, admin());

        verify(catalogRepository).save(any(InspectionCriterionCatalog.class));
    }
}