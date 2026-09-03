package vn.nguongocso.certification.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CategoryMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneCatalogResponse;
import vn.nguongocso.certification.dto.response.ProductCategoryMilestoneResponse;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import vn.nguongocso.certification.entity.ProductCategoryMilestone;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CultivationMilestoneCatalogRepository;
import vn.nguongocso.certification.repository.ProductCategoryMilestoneRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.CategoryMilestoneAssignmentService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of CategoryMilestoneAssignmentService.
 * Story: NCL-09-CN-011
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryMilestoneAssignmentServiceImpl implements CategoryMilestoneAssignmentService {

    private static final String ADMIN_ROLE = "VT-01";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục dùng chung.";
    private static final String MSG_CATEGORY_NOT_FOUND = "Loại nông sản không tồn tại.";
    private static final String MSG_INACTIVE_MILESTONE =
            "Mốc canh tác '%s' đã ngừng sử dụng, không thể gán.";

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryMilestoneRepository categoryMilestoneRepository;
    private final CultivationMilestoneCatalogRepository catalogRepository;
    private final StandardRepository standardRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryMilestoneResponse> getCategoryMilestones(
            UUID categoryId, CustomUserDetails currentUser) {

        if (!productCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND);
        }

        List<ProductCategoryMilestone> assignments =
                categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId);

        return assignments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ProductCategoryMilestoneResponse> assignMilestones(
            UUID categoryId, CategoryMilestoneRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND));

        List<Long> milestoneIds = request.getMilestoneIds();
        Set<Long> distinctIds = new LinkedHashSet<>(milestoneIds);

        List<CultivationMilestoneCatalog> toAssign = new ArrayList<>();
        for (Long mid : distinctIds) {
            CultivationMilestoneCatalog milestone = catalogRepository.findById(mid)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Mốc canh tác " + mid + " không tồn tại."));
            if (!"ACTIVE".equals(milestone.getStatus())) {
                throw new BusinessException(
                        String.format(MSG_INACTIVE_MILESTONE, milestone.getName()));
            }
            toAssign.add(milestone);
        }

        // Resolve standard if provided
        Standard standard = null;
        if (request.getStandardId() != null) {
            standard = standardRepository.findById(request.getStandardId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Tiêu chuẩn " + request.getStandardId() + " không tồn tại."));
        }

        // Idempotent upsert: only insert NEW, remove OLD, keep existing
        Set<Long> existingIds = new HashSet<>();
        for (ProductCategoryMilestone pcm :
                categoryMilestoneRepository.findByCategoryIdWithMilestones(categoryId)) {
            // Only consider assignments with the same standard scope
            boolean sameStandard = (pcm.getStandard() == null && standard == null)
                    || (pcm.getStandard() != null && standard != null
                        && pcm.getStandard().getId().equals(standard.getId()));
            if (sameStandard) {
                existingIds.add(pcm.getMilestone().getId());
            }
        }

        Set<Long> desiredIds = toAssign.stream()
                .map(CultivationMilestoneCatalog::getId)
                .collect(Collectors.toSet());

        // Remove OLD milestones for this standard scope
        List<Long> toRemove = existingIds.stream()
                .filter(id -> !desiredIds.contains(id))
                .toList();
        if (!toRemove.isEmpty()) {
            categoryMilestoneRepository.deleteByCategory_IdAndMilestone_IdIn(categoryId, toRemove);
            entityManager.flush();
        }

        // Insert NEW milestones
        Set<Long> mandatoryIds = request.getMandatoryMilestoneIds() == null
                ? null
                : new HashSet<>(request.getMandatoryMilestoneIds());
        for (CultivationMilestoneCatalog milestone : toAssign) {
            if (existingIds.contains(milestone.getId())) {
                continue;
            }
            ProductCategoryMilestone assignment = ProductCategoryMilestone.builder()
                    .category(category)
                    .milestone(milestone)
                    .standard(standard)
                    .isMandatory(mandatoryIds == null || mandatoryIds.contains(milestone.getId()))
                    .build();
            categoryMilestoneRepository.save(assignment);
        }

        log.info("Assigned {} milestones to category {} (standard={})",
                toAssign.size(), categoryId, standard != null ? standard.getId() : "GLOBAL");
        return getCategoryMilestones(categoryId, currentUser);
    }

    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION);
        }
    }

    private ProductCategoryMilestoneResponse toResponse(ProductCategoryMilestone entity) {
        CultivationMilestoneCatalogResponse milestoneResp = CultivationMilestoneCatalogResponse.builder()
                .id(entity.getMilestone().getId())
                .name(entity.getMilestone().getName())
                .description(entity.getMilestone().getDescription())
                .activityType(entity.getMilestone().getActivityType())
                .expectedDaysFromPlanting(entity.getMilestone().getExpectedDaysFromPlanting())
                .status(entity.getMilestone().getStatus())
                .createdAt(entity.getMilestone().getCreatedAt())
                .updatedAt(entity.getMilestone().getUpdatedAt())
                .build();

        return ProductCategoryMilestoneResponse.builder()
                .id(entity.getId().toString())
                .milestone(milestoneResp)
                .standardId(entity.getStandard() != null ? entity.getStandard().getId().toString() : null)
                .standardName(entity.getStandard() != null ? entity.getStandard().getName() : null)
                .isMandatory(Boolean.TRUE.equals(entity.getIsMandatory()))
                .build();
    }
}
