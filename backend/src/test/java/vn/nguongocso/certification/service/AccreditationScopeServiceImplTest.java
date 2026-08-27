package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.AccreditationScopeSummaryResponse;
import vn.nguongocso.certification.entity.AccreditationScope;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.repository.AccreditationScopeRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.impl.AccreditationScopeServiceImpl;
import vn.nguongocso.exception.BusinessException;

/**
 * Test cho AccreditationScopeServiceImpl (NCL-11-CN-006 Phase 2).
 */
@ExtendWith(MockitoExtension.class)
class AccreditationScopeServiceImplTest {

    @Mock
    private TestingUnitRepository testingUnitRepository;

    @Mock
    private InspectionCriterionCatalogRepository inspectionCriterionCatalogRepository;

    @Mock
    private AccreditationScopeRepository accreditationScopeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccreditationScopeServiceImpl accreditationScopeService;

    private UUID unitId;

    private TestingUnit unit;

    @BeforeEach
    void setUp() {
        unitId = UUID.randomUUID();

        unit = TestingUnit.builder()
                .id(unitId)
                .name("Lab ABC")
                .accreditationCode("VILAS-001")
                .isActive(true)
                .build();
    }

    private CustomUserDetails adminUser() {
        CustomUserDetails user = Mockito.mock(CustomUserDetails.class);
        lenient().when(user.getRoleCode()).thenReturn("VT-01");
        lenient().when(user.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(user.getUsername()).thenReturn("admin");
        lenient().when(user.getFullName()).thenReturn("Quản trị viên");
        return user;
    }

    private CustomUserDetails nonAdminUser() {
        CustomUserDetails user = Mockito.mock(CustomUserDetails.class);
        lenient().when(user.getRoleCode()).thenReturn("VT-02");
        return user;
    }

    private InspectionCriterionCatalog criterion(Long id) {
        return InspectionCriterionCatalog.builder()
                .id(id)
                .name("Chỉ tiêu " + id)
                .unit("mg/kg")
                .maxThreshold(new java.math.BigDecimal("0.5"))
                .status("ACTIVE")
                .build();
    }

    @Test
    void getAccreditationScope_shouldReturnEmptySummary_whenUnitHasNoScopes() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));
        when(accreditationScopeRepository.findByTestingUnitIdWithCriterion(unitId))
                .thenReturn(List.of());

        AccreditationScopeSummaryResponse result =
                accreditationScopeService.getAccreditationScope(unitId);

        assertThat(result.getTestingUnitId()).isEqualTo(unitId);
        assertThat(result.getTestingUnitName()).isEqualTo("Lab ABC");
        assertThat(result.getAccreditedCriteria()).isEmpty();
    }

    @Test
    void getAccreditationScope_shouldReturnAccreditedCriteria_whenScopesExist() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));
        when(accreditationScopeRepository.findByTestingUnitIdWithCriterion(unitId))
                .thenReturn(List.of(scope(101L), scope(102L)));

        AccreditationScopeSummaryResponse result =
                accreditationScopeService.getAccreditationScope(unitId);

        assertThat(result.getAccreditedCriteria())
                .extracting(AccreditationScopeSummaryResponse.AccreditedCriterionItem::getId)
                .containsExactly(101L, 102L);
    }

    @Test
    void getAccreditationScope_shouldThrow_whenUnitNotFound() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accreditationScopeService.getAccreditationScope(unitId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Đơn vị kiểm nghiệm không tồn tại");
    }

    @Test
    void updateAccreditationScope_shouldThrow_whenNotAdmin() {
        assertThatThrownBy(() ->
                accreditationScopeService.updateAccreditationScope(
                        unitId,
                        List.of(101L),
                        nonAdminUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không có quyền");
    }

    @Test
    void updateAccreditationScope_shouldThrow_whenCriteriaListEmpty() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));

        assertThatThrownBy(() ->
                accreditationScopeService.updateAccreditationScope(
                        unitId,
                        List.of(),
                        adminUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    void updateAccreditationScope_shouldThrow_whenCriterionNotFound() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));
        when(inspectionCriterionCatalogRepository.findByIdIn(List.of(101L)))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                accreditationScopeService.updateAccreditationScope(
                        unitId,
                        List.of(101L),
                        adminUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không tồn tại");
    }


    private AccreditationScope scope(Long id) {
        return AccreditationScope.builder()
                .testingUnit(unit)
                .criterion(criterion(id))
                .criterionCode("Chỉ tiêu " + id)
                .criterionName("Chỉ tiêu " + id)
                .build();
    }

    @Test
    void updateAccreditationScope_shouldReplaceAllScopes_whenValid() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));
        when(inspectionCriterionCatalogRepository.findByIdIn(List.of(101L, 102L)))
                .thenReturn(List.of(criterion(101L), criterion(102L)));
        when(accreditationScopeRepository.save(any(AccreditationScope.class)))
                .thenAnswer(invocation -> {
                    AccreditationScope s = invocation.getArgument(0);
                    if (s.getId() == null) {
                        s.setId(UUID.randomUUID());
                    }
                    return s;
                });

        AccreditationScopeSummaryResponse result =
                accreditationScopeService.updateAccreditationScope(
                        unitId,
                        List.of(101L, 102L),
                        adminUser());

        verify(accreditationScopeRepository).deleteByTestingUnitId(unitId);
        ArgumentCaptor<AccreditationScope> captor =
                ArgumentCaptor.forClass(AccreditationScope.class);
        verify(accreditationScopeRepository, Mockito.times(2))
                .save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(s -> s.getTestingUnit().getId())
                .allMatch(unitId::equals);

        assertThat(result.getAccreditedCriteria())
                .extracting(AccreditationScopeSummaryResponse.AccreditedCriterionItem::getId)
                .containsExactly(101L, 102L);
    }

    @Test
    void updateAccreditationScope_shouldDeduplicateIds() {
        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));
        when(inspectionCriterionCatalogRepository.findByIdIn(List.of(101L)))
                .thenReturn(List.of(criterion(101L)));
        when(accreditationScopeRepository.save(any(AccreditationScope.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accreditationScopeService.updateAccreditationScope(
                unitId,
                List.of(101L, 101L),
                adminUser());

        verify(accreditationScopeRepository, Mockito.times(1))
                .save(any(AccreditationScope.class));
    }

    @Test
    void updateAccreditationScope_shouldThrow_whenCriterionInactive() {
        InspectionCriterionCatalog inactive = criterion(101L);
        inactive.setStatus("INACTIVE");

        when(testingUnitRepository.findById(unitId))
                .thenReturn(Optional.of(unit));
        when(inspectionCriterionCatalogRepository.findByIdIn(List.of(101L)))
                .thenReturn(List.of(inactive));

        assertThatThrownBy(() ->
                accreditationScopeService.updateAccreditationScope(
                        unitId,
                        List.of(101L),
                        adminUser()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã ngừng sử dụng");

        verify(accreditationScopeRepository, never())
                .deleteByTestingUnitId(unitId);
        verify(accreditationScopeRepository, never())
                .save(any(AccreditationScope.class));
    }
}

