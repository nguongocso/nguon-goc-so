package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryCriteriaRequest;
import vn.nguongocso.certification.dto.request.MandatoryInspectionRequest;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.service.impl.CategoryCriterionAssignmentServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;

/**
 * Tests for CategoryCriterionAssignmentServiceImpl — business rules
 * BR-3 (mandatory inspection requires a criterion), REPLACE semantics,
 * only PLATFORM_ADMIN can assign (BR-4).
 * Story: NCL-09-CN-009
 */
@ExtendWith(MockitoExtension.class)
class CategoryCriterionAssignmentServiceImplTest {

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private CategoryCriterionRepository categoryCriterionRepository;

    @Mock
    private InspectionCriterionCatalogRepository catalogRepository;

    @Mock
    private InspectionCriterionRepository inspectionCriterionRepository;

    @org.mockito.InjectMocks
    private CategoryCriterionAssignmentServiceImpl service;

    private UUID categoryId;
    private ProductCategory category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = ProductCategory.builder()
                .id(categoryId)
                .name("Rau ăn lá")
                .requiresInspection(false)
                .isActive(true)
                .build();
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

    // BR-3 (TC-02): mandatory ON rejected without active criterion
    @Test
    void setMandatoryInspection_shouldRejectWithoutCriteria() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryCriterionRepository.countByCategoryIdAndCriteriaStatus(categoryId, "ACTIVE"))
                .thenReturn(0L);

        MandatoryInspectionRequest request = new MandatoryInspectionRequest();
        request.setRequired(true);

        assertThatThrownBy(() -> service.setMandatoryInspection(categoryId, request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // mandatory ON allowed when category has >= 1 ACTIVE criterion
    @Test
    void setMandatoryInspection_shouldAllowWithCriteria() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryCriterionRepository.countByCategoryIdAndCriteriaStatus(categoryId, "ACTIVE"))
                .thenReturn(1L);
        when(productCategoryRepository.save(any(ProductCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MandatoryInspectionRequest request = new MandatoryInspectionRequest();
        request.setRequired(true);

        service.setMandatoryInspection(categoryId, request, admin());

        assertThat(category.getRequiresInspection()).isTrue();
    }

    // mandatory OFF always allowed
    @Test
    void setMandatoryInspection_shouldAllowOffWithoutCriteria() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.save(any(ProductCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MandatoryInspectionRequest request = new MandatoryInspectionRequest();
        request.setRequired(false);

        service.setMandatoryInspection(categoryId, request, admin());

        assertThat(category.getRequiresInspection()).isFalse();
    }

    // only PLATFORM_ADMIN can set mandatory inspection
    @Test
    void setMandatoryInspection_shouldRejectNonAdmin() {
        assertThatThrownBy(() -> service.setMandatoryInspection(
                categoryId, new MandatoryInspectionRequest(), nonAdmin()))
                .isInstanceOf(BusinessException.class);
    }
// REPLACE: removing all criteria rejected if category is mandatory-inspection
    @Test
    void assignCriteria_shouldRejectRemovingAllWhenMandatory() {
        category.setRequiresInspection(true);
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CategoryCriteriaRequest request = new CategoryCriteriaRequest();
        request.setCriterionIds(List.of());

        assertThatThrownBy(() -> service.assignCriteria(categoryId, request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(categoryCriterionRepository, never()).deleteByCategory_Id(any());
    }

    // only assign ACTIVE criteria; INACTIVE rejected
    @Test
    void assignCriteria_shouldRejectInactiveCriterion() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        InspectionCriterionCatalog inactive = InspectionCriterionCatalog.builder()
                .id(2L)
                .name("Ngừng sử dụng")
                .status("INACTIVE")
                .build();
        when(catalogRepository.findById(2L)).thenReturn(Optional.of(inactive));

        CategoryCriteriaRequest request = new CategoryCriteriaRequest();
        request.setCriterionIds(List.of(2L));

        assertThatThrownBy(() -> service.assignCriteria(categoryId, request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(categoryCriterionRepository, never()).deleteByCategory_Id(any());
    }

    // assigning criteria allowed for admin
    @Test
    void assignCriteria_shouldAssignActiveCriteriaForAdmin() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryCriterionRepository.findByCategoryIdWithCriteria(categoryId))
                .thenReturn(java.util.List.of());
        InspectionCriterionCatalog active = InspectionCriterionCatalog.builder()
                .id(1L)
                .name("Dư lượng")
                .status("ACTIVE")
                .build();
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(active));

        CategoryCriteriaRequest request = new CategoryCriteriaRequest();
        request.setCriterionIds(List.of(1L));

        service.assignCriteria(categoryId, request, admin());

        verify(categoryCriterionRepository).deleteByCategory_Id(categoryId);
        verify(categoryCriterionRepository).save(any());
    }
}