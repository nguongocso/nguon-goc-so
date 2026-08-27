package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import vn.nguongocso.certification.dto.request.CategoryCriteriaRequest;
import vn.nguongocso.certification.dto.request.MandatoryInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.service.CategoryCriterionAssignmentService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.response.ProductCategoryResponse;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CategoryCriterionAssignmentService.
 * Story: NCL-09-CN-009
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryCriterionAssignmentServiceImpl implements CategoryCriterionAssignmentService {

    private static final String ADMIN_ROLE = "VT-01";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục dùng chung.";
    private static final String MSG_CATEGORY_NOT_FOUND = "Loại nông sản không tồn tại.";
    private static final String MSG_INACTIVE_CRITERION =
            "Chỉ tiêu '%s' đã ngừng sử dụng, không thể gán.";
    private static final String MSG_REQUIRED_WITHOUT_CRITERIA =
            "Không thể bật bắt buộc kiểm nghiệm: loại nông sản chưa có chỉ tiêu kiểm nghiệm nào. "
                    + "Vui lòng gán ít nhất một chỉ tiêu.";
    private static final String MSG_CANNOT_REMOVE_ALL_CRITERIA =
            "Không thể xóa toàn bộ chỉ tiêu của loại nông sản đang bắt buộc kiểm nghiệm. "
                    + "Tắt cờ bắt buộc trước.";

    private final ProductCategoryRepository productCategoryRepository;
    private final CategoryCriterionRepository categoryCriterionRepository;
    private final InspectionCriterionCatalogRepository catalogRepository;
    private final InspectionCriterionRepository inspectionCriterionRepository;

    /**
     * Dùng để ép DELETE xuống DB ngay trước khi INSERT trong cùng transaction,
     * tránh xung đột ràng buộc unique (category_id, criterion_id) khi gán.
     */
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<InspectionCriterionCatalogResponse> getCategoryCriteria(
            UUID categoryId, boolean activeOnly, CustomUserDetails currentUser) {

        if (!productCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND);
        }

        List<CategoryCriterion> assignments = activeOnly
                ? categoryCriterionRepository.findByCategoryIdAndCriteriaStatus(categoryId, "ACTIVE")
                : categoryCriterionRepository.findByCategoryIdWithCriteria(categoryId);

        Set<Long> referencedIds =
                new HashSet<>(inspectionCriterionRepository.findReferencedCriterionIds());

        return assignments.stream()
                .map(CategoryCriterion::getCriterion)
                .map(criterion -> toResponse(criterion, referencedIds))
                .toList();
    }
@Override
    public List<InspectionCriterionCatalogResponse> assignCriteria(
            UUID categoryId, CategoryCriteriaRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND));

        List<Long> criterionIds = request.getCriterionIds();
        // De-duplicate while preserving order
        Set<Long> distinctIds = new LinkedHashSet<>(criterionIds);

        List<InspectionCriterionCatalog> toAssign = new ArrayList<>();
        for (Long cid : distinctIds) {
            InspectionCriterionCatalog criterion = catalogRepository.findById(cid)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Chỉ tiêu " + cid + " không tồn tại."));
            if (!"ACTIVE".equals(criterion.getStatus())) {
                throw new BusinessException(
                        String.format(MSG_INACTIVE_CRITERION, criterion.getName()));
            }
            toAssign.add(criterion);
        }

        boolean isMandatory = Boolean.TRUE.equals(category.getRequiresInspection());

        // BR: can't remove all criteria if the category is mandatory-inspection
        if (isMandatory && toAssign.isEmpty()) {
            throw new BusinessException(MSG_CANNOT_REMOVE_ALL_CRITERIA);
        }

        // ---- CẬP NHẬT (upsert) IDEMPOTENT ----
        // Lựa chọn mới = toAssign. So khớp với các chỉ tiêu đang gán hiện tại
        // để chỉ: (1) thêm các chỉ tiêu MỚI, (2) xóa các chỉ tiêu đã bị bỏ,
        // (3) giữ nguyên các chỉ tiêu vốn đã gán và vẫn đang được chọn.
        // → KHÔNG bao giờ insert lại (category_id, criterion_id) đã tồn tại,
        //   nên không thể vi phạm unique uk_category_criteria (fix lỗi 409 duplicate).

        Set<Long> existingIds = new HashSet<>();
        for (CategoryCriterion cc : categoryCriterionRepository.findByCategoryIdWithCriteria(categoryId)) {
            existingIds.add(cc.getCriterion().getId());
        }

        Set<Long> desiredIds = toAssign.stream()
                .map(InspectionCriterionCatalog::getId)
                .collect(Collectors.toSet());

        // (2) Xóa các gán đã bị bỏ khỏi lựa chọn.
        List<Long> toRemove = existingIds.stream()
                .filter(id -> !desiredIds.contains(id))
                .toList();
        if (!toRemove.isEmpty()) {
            categoryCriterionRepository.deleteByCategory_IdAndCriterion_IdIn(categoryId, toRemove);
            // Đảm bảo DELETE chạy xuống DB trước khi INSERT tiếp theo.
            entityManager.flush();
        }

        // (1)+(3) Chỉ insert các chỉ tiêu MỚI; giữ nguyên các chỉ tiêu đã gán.
        for (InspectionCriterionCatalog criterion : toAssign) {
            if (existingIds.contains(criterion.getId())) {
                continue;
            }
            CategoryCriterion assignment = CategoryCriterion.builder()
                    .category(category)
                    .criterion(criterion)
                    .build();
            categoryCriterionRepository.save(assignment);
        }

        log.info("Assigned {} criteria to category {}", toAssign.size(), categoryId);
        return getCategoryCriteria(categoryId, false, currentUser);
    }

    @Override
    public ProductCategoryResponse setMandatoryInspection(
            UUID categoryId, MandatoryInspectionRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND));

        boolean required = Boolean.TRUE.equals(request.getRequired());

        // BR-3 (TC-02): only allow turning ON if there is at least one ACTIVE criterion
        if (required) {
            long activeCount = categoryCriterionRepository
                    .countByCategoryIdAndCriteriaStatus(categoryId, "ACTIVE");
            if (activeCount == 0) {
                throw new BusinessException(MSG_REQUIRED_WITHOUT_CRITERIA);
            }
        }

        category.setRequiresInspection(required);
        ProductCategory updated = productCategoryRepository.save(category);

        log.info("Category {} mandatory-inspection set to {}", categoryId, required);
        return toProductCategoryResponse(updated);
    }

    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION);
        }
    }

    private InspectionCriterionCatalogResponse toResponse(InspectionCriterionCatalog entity) {
        boolean referenced = inspectionCriterionRepository.existsByCriterionId(entity.getId());
        return toResponse(entity, referenced);
    }

    private InspectionCriterionCatalogResponse toResponse(
            InspectionCriterionCatalog entity, Set<Long> referencedIds) {
        return toResponse(entity, referencedIds.contains(entity.getId()));
    }

    private InspectionCriterionCatalogResponse toResponse(
            InspectionCriterionCatalog entity, boolean referenced) {
        return InspectionCriterionCatalogResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .unit(entity.getUnit())
                .maxThreshold(entity.getMaxThreshold())
                .referenceStandard(entity.getReferenceStandard())
                .status(entity.getStatus())
                .referenced(referenced)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ProductCategoryResponse toProductCategoryResponse(ProductCategory category) {
        return ProductCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .group(category.getGroup())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .tempMin(category.getTempMin())
                .tempMax(category.getTempMax())
                .humidityMin(category.getHumidityMin())
                .humidityMax(category.getHumidityMax())
                .requiresInspection(category.getRequiresInspection())
                .build();
    }
}