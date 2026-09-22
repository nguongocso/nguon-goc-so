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
 * Triển khai dịch vụ quản lý gán bộ chỉ tiêu cho loại nông sản (NCL-09-CN-009).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryCriterionAssignmentServiceImpl implements CategoryCriterionAssignmentService {
    private static final String ADMIN_ROLE = "VT-01";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục dùng chung.";
    private static final String MSG_CATEGORY_NOT_FOUND = "Loại nông sản không tồn tại.";
    private static final String MSG_INACTIVE_CRITERION = "Chỉ tiêu '%s' đã ngừng sử dụng, không thể gán.";
    private static final String MSG_REQUIRED_WITHOUT_CRITERIA = "Không thể bật bắt buộc kiểm nghiệm: loại nông sản chưa có chỉ tiêu kiểm nghiệm nào. "
            + "Vui lòng gán ít nhất một chỉ tiêu.";
    private static final String MSG_CANNOT_REMOVE_ALL_CRITERIA = "Không thể xóa toàn bộ chỉ tiêu của loại nông sản đang bắt buộc kiểm nghiệm. "
            + "Tắt cờ bắt buộc trước.";

    private final ProductCategoryRepository productCategoryRepository;
    private final CategoryCriterionRepository categoryCriterionRepository;
    private final InspectionCriterionCatalogRepository catalogRepository;
    private final InspectionCriterionRepository inspectionCriterionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Lấy danh sách chỉ tiêu đã gán cho loại nông sản.
     */
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

        Set<Long> referencedIds = new HashSet<>(inspectionCriterionRepository.findReferencedCriterionIds());

        return assignments.stream()
                .map(CategoryCriterion::getCriterion)
                .map(criterion -> toResponse(criterion, referencedIds))
                .toList();
    }

    /**
     * Gán danh sách chỉ tiêu cho loại nông sản theo cơ chế idempotent.
     */
    @Override
    public List<InspectionCriterionCatalogResponse> assignCriteria(
            UUID categoryId, CategoryCriteriaRequest request, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND));

        List<Long> criterionIds = request.getCriterionIds();
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

        if (isMandatory && toAssign.isEmpty()) {
            throw new BusinessException(MSG_CANNOT_REMOVE_ALL_CRITERIA);
        }

        Set<Long> existingIds = new HashSet<>();
        for (CategoryCriterion cc : categoryCriterionRepository.findByCategoryIdWithCriteria(categoryId)) {
            existingIds.add(cc.getCriterion().getId());
        }

        Set<Long> desiredIds = toAssign.stream()
                .map(InspectionCriterionCatalog::getId)
                .collect(Collectors.toSet());

        List<Long> toRemove = existingIds.stream()
                .filter(id -> !desiredIds.contains(id))
                .toList();
        if (!toRemove.isEmpty()) {
            categoryCriterionRepository.deleteByCategory_IdAndCriterion_IdIn(categoryId, toRemove);
            entityManager.flush();
        }

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

    /**
     * Bật hoặc tắt cờ bắt buộc kiểm nghiệm cho loại nông sản.
     */
    @Override
    public ProductCategoryResponse setMandatoryInspection(
            UUID categoryId, MandatoryInspectionRequest request, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CATEGORY_NOT_FOUND));

        boolean required = Boolean.TRUE.equals(request.getRequired());

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

    /**
     * Kiểm tra quyền Quản trị viên nền tảng của người dùng.
     */
    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION);
        }
    }

    /**
     * Chuyển đổi thực thể chỉ tiêu kiểm nghiệm sang DTO phản hồi.
     */
    private InspectionCriterionCatalogResponse toResponse(InspectionCriterionCatalog entity) {
        boolean referenced = inspectionCriterionRepository.existsByCriterionId(entity.getId());
        return toResponse(entity, referenced);
    }

    /**
     * Chuyển đổi thực thể chỉ tiêu kiểm nghiệm sang DTO phản hồi kèm cờ tham chiếu đã nạp trước.
     */
    private InspectionCriterionCatalogResponse toResponse(
            InspectionCriterionCatalog entity, Set<Long> referencedIds) {
        return toResponse(entity, referencedIds.contains(entity.getId()));
    }

    /**
     * Chuyển đổi thực thể chỉ tiêu kiểm nghiệm sang DTO phản hồi chi tiết.
     */
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

    /**
     * Chuyển đổi thực thể loại nông sản sang DTO phản hồi.
     */
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