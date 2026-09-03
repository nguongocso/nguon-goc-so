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
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryMilestoneRequest;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import vn.nguongocso.certification.entity.ProductCategoryMilestone;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CultivationMilestoneCatalogRepository;
import vn.nguongocso.certification.repository.ProductCategoryMilestoneRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.impl.CategoryMilestoneAssignmentServiceImpl;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;

/**
 * Tests for CategoryMilestoneAssignmentServiceImpl — GLOBAL vs standard-scoped
 * assignment, REPLACE semantics, INACTIVE milestone, PLATFORM_ADMIN-only,
 * is_mandatory flag. Story: NCL-09-CN-011.
 */
@ExtendWith(MockitoExtension.class)
class CategoryMilestoneAssignmentServiceImplTest {

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductCategoryMilestoneRepository categoryMilestoneRepository;

    @Mock
    private CultivationMilestoneCatalogRepository catalogRepository;

    @Mock
    private StandardRepository standardRepository;

    @Mock
    private EntityManager entityManager;

    @org.mockito.InjectMocks
    private CategoryMilestoneAssignmentServiceImpl service;

    private UUID categoryId;
    private ProductCategory category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = ProductCategory.builder()
                .id(categoryId)
                .name("Rau ăn lá")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
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

    // user mock without stubbing role — for GET (read-only) paths
    private CustomUserDetails plainUser() {
        return Mockito.mock(CustomUserDetails.class);
    }

    private CultivationMilestoneCatalog activeMilestone(Long id, String name) {
        return CultivationMilestoneCatalog.builder()
                .id(id)
                .name(name)
                .activityType("FERTILIZING")
                .status("ACTIVE")
                .build();
    }

    private ProductCategoryMilestone existing(Long mid, Standard standard) {
        CultivationMilestoneCatalog m = activeMilestone(mid, "Mốc " + mid);
        return ProductCategoryMilestone.builder()
                .category(category)
                .milestone(m)
                .standard(standard)
                .isMandatory(true)
                .build();
    }

    // non-admin rejected
    @Test
    void assignMilestones_shouldRejectNonAdmin() {
        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L));

        assertThatThrownBy(() -> service.assignMilestones(categoryId, request, nonAdmin()))
                .isInstanceOf(BusinessException.class);
        verify(categoryMilestoneRepository, never()).save(any());
    }

    // category not found
    @Test
    void assignMilestones_shouldRejectWhenCategoryNotFound() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L));

        assertThatThrownBy(() -> service.assignMilestones(categoryId, request, admin()))
                .isInstanceOf(vn.nguongocso.exception.ResourceNotFoundException.class);
    }

    // INACTIVE milestone rejected
    @Test
    void assignMilestones_shouldRejectInactiveMilestone() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        CultivationMilestoneCatalog inactive = CultivationMilestoneCatalog.builder()
                .id(2L)
                .name("Ngừng sử dụng")
                .activityType("PESTICIDE")
                .status("INACTIVE")
                .build();
        when(catalogRepository.findById(2L)).thenReturn(Optional.of(inactive));

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(2L));

        assertThatThrownBy(() -> service.assignMilestones(categoryId, request, admin()))
                .isInstanceOf(BusinessException.class);
        verify(categoryMilestoneRepository, never()).save(any());
    }

    // milestone not found
    @Test
    void assignMilestones_shouldRejectWhenMilestoneNotFound() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(catalogRepository.findById(99L)).thenReturn(Optional.empty());

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(99L));

        assertThatThrownBy(() -> service.assignMilestones(categoryId, request, admin()))
                .isInstanceOf(BusinessException.class);
    }

    // assign GLOBAL (no standard) — new assignment inserted with isMandatory true
    @Test
    void assignMilestones_shouldInsertGlobalAssignment() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        CultivationMilestoneCatalog m = activeMilestone(1L, "Bón phân đợt 1");
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(m));
        when(categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId))
                .thenReturn(List.of());

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L));
        // standardId null -> GLOBAL

        service.assignMilestones(categoryId, request, admin());

        verify(categoryMilestoneRepository, never()).deleteByCategory_IdAndMilestone_IdIn(any(), any());
        verify(categoryMilestoneRepository).save(any(ProductCategoryMilestone.class));
    }

    // is_mandatory false is respected when milestone not in mandatoryMilestoneIds
    @Test
    void assignMilestones_shouldPersistIsMandatoryFalse() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        CultivationMilestoneCatalog m = activeMilestone(1L, "Mốc không bắt buộc");
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(m));
        when(categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId))
                .thenReturn(List.of());

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L));
        // Không liệt kê 1L trong mandatoryMilestoneIds -> is_mandatory false
        request.setMandatoryMilestoneIds(List.of());

        service.assignMilestones(categoryId, request, admin());

        verify(categoryMilestoneRepository).save(org.mockito.ArgumentMatchers.argThat(
                pcm -> Boolean.FALSE.equals(pcm.getIsMandatory())));
    }

    // milestone in mandatoryMilestoneIds -> is_mandatory true
    @Test
    void assignMilestones_shouldPersistIsMandatoryTrue() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        CultivationMilestoneCatalog m = activeMilestone(1L, "Mốc bắt buộc");
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(m));
        when(categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId))
                .thenReturn(List.of());

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L));
        request.setMandatoryMilestoneIds(List.of(1L));

        service.assignMilestones(categoryId, request, admin());

        verify(categoryMilestoneRepository).save(org.mockito.ArgumentMatchers.argThat(
                pcm -> Boolean.TRUE.equals(pcm.getIsMandatory())));
    }

    // assign standard-scoped — resolves standard
    @Test
    void assignMilestones_shouldResolveStandardScope() {
        Standard standard = Standard.builder().id(UUID.randomUUID()).name("VietGAP").isActive(true).build();
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        CultivationMilestoneCatalog m = activeMilestone(1L, "Bón phân đợt 1");
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(m));
        when(standardRepository.findById(standard.getId())).thenReturn(Optional.of(standard));
        when(categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId))
                .thenReturn(List.of());

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L));
        request.setStandardId(standard.getId());

        service.assignMilestones(categoryId, request, admin());

        verify(categoryMilestoneRepository).save(org.mockito.ArgumentMatchers.argThat(
                pcm -> pcm.getStandard() != null && pcm.getStandard().getId().equals(standard.getId())));
    }

    // REPLACE: bỏ chọn 1 mốc đã gán cùng scope -> xóa đúng mốc đó, không thêm mới
    @Test
    void assignMilestones_shouldRemoveDeselectedMilestone() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        // hiện tại đã gán 1L và 2L (GLOBAL)
        when(categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId))
                .thenReturn(List.of(existing(1L, null), existing(2L, null)));
        // chỉ chọn lại 2L
        CultivationMilestoneCatalog b = activeMilestone(2L, "Mốc 2");
        when(catalogRepository.findById(2L)).thenReturn(Optional.of(b));

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(2L));

        service.assignMilestones(categoryId, request, admin());

        verify(categoryMilestoneRepository).deleteByCategory_IdAndMilestone_IdIn(categoryId, List.of(1L));
        verify(categoryMilestoneRepository, never()).save(any());
    }

    // idempotent: mốc đã gán không được insert lại (tránh 409 unique)
    @Test
    void assignMilestones_shouldNotReinsertAlreadyAssigned() {
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsById(categoryId)).thenReturn(true);
        when(categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId))
                .thenReturn(List.of(existing(1L, null)));
        CultivationMilestoneCatalog a = activeMilestone(1L, "Mốc 1");
        CultivationMilestoneCatalog b = activeMilestone(2L, "Mốc 2");
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(a));
        when(catalogRepository.findById(2L)).thenReturn(Optional.of(b));

        CategoryMilestoneRequest request = new CategoryMilestoneRequest();
        request.setMilestoneIds(List.of(1L, 2L));

        service.assignMilestones(categoryId, request, admin());

        // không xóa gì (1L vẫn giữ, 2L mới), chỉ insert B
        verify(categoryMilestoneRepository, never()).deleteByCategory_IdAndMilestone_IdIn(any(), any());
        verify(categoryMilestoneRepository).save(any(ProductCategoryMilestone.class));
    }

    // get milestones for non-existent category rejected (existsById default false)
    @Test
    void getCategoryMilestones_shouldRejectWhenCategoryNotFound() {
        assertThatThrownBy(() -> service.getCategoryMilestones(categoryId, plainUser()))
                .isInstanceOf(vn.nguongocso.exception.ResourceNotFoundException.class);
    }
}
