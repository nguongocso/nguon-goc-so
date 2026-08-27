package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionCatalogRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionCatalogResponse;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.service.InspectionCriterionCatalogService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of InspectionCriterionCatalogService.
 * Story: NCL-09-CN-009
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InspectionCriterionCatalogServiceImpl implements InspectionCriterionCatalogService {

    private static final String ADMIN_ROLE = "VT-01";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục chỉ tiêu kiểm nghiệm.";
    private static final String MSG_CRITERION_NOT_FOUND = "Chỉ tiêu kiểm nghiệm không tồn tại.";
    private static final String MSG_DUPLICATE_CRITERION =
            "Chỉ tiêu với tên và tiêu chuẩn tham chiếu này đã tồn tại.";
    private static final String MSG_REFERENCED_CRITERION =
            "Chỉ tiêu đang được yêu cầu kiểm nghiệm tham chiếu, không thể xóa.";
    private static final String MSG_THRESHOLD_INVALID = "Ngưỡng tối đa phải lớn hơn 0.";
    private static final String MSG_INVALID_STATUS =
            "Trạng thái không hợp lệ. Giá trị hợp lệ: ACTIVE, INACTIVE.";

    private final InspectionCriterionCatalogRepository catalogRepository;
    private final InspectionCriterionRepository inspectionCriterionRepository;
    private final CategoryCriterionRepository categoryCriterionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionCriterionCatalogResponse> searchCriteria(
            String keyword, String status, Pageable pageable, CustomUserDetails currentUser) {

        if (status != null) {
            String upperStatus = status.toUpperCase();
            if (!"ACTIVE".equals(upperStatus) && !"INACTIVE".equals(upperStatus)) {
                throw new BusinessException(MSG_INVALID_STATUS);
            }
            status = upperStatus;
        }

        Set<Long> referencedIds = loadReferencedIds();
        return catalogRepository.search(keyword, status, pageable)
                .map(entity -> toResponse(entity, referencedIds));
    }

    /**
     * Load referenced catalog criterion ids once to avoid N+1 queries
     * when mapping lists.
     */
    private Set<Long> loadReferencedIds() {
        return new HashSet<>(inspectionCriterionRepository.findReferencedCriterionIds());
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionCriterionCatalogResponse getCriterion(Long id, CustomUserDetails currentUser) {
        InspectionCriterionCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CRITERION_NOT_FOUND));
        return toResponse(entity);
    }

    @Override
    public InspectionCriterionCatalogResponse createCriterion(
            InspectionCriterionCatalogRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);
        validateThreshold(request.getMaxThreshold());

        String name = request.getName().trim();
        String unit = request.getUnit().trim();
        String referenceStandard = request.getReferenceStandard() != null
                ? request.getReferenceStandard().trim() : null;

        if (catalogRepository.existsByNameAndReferenceStandard(name, referenceStandard)) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_DUPLICATE_CRITERION);
        }

        InspectionCriterionCatalog entity = InspectionCriterionCatalog.builder()
                .name(name)
                .unit(unit)
                .maxThreshold(request.getMaxThreshold())
                .referenceStandard(referenceStandard)
                .status("ACTIVE")
                .build();

        return toResponse(catalogRepository.save(entity));
    }

    @Override
    public InspectionCriterionCatalogResponse updateCriterion(
            Long id, InspectionCriterionCatalogRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);
        validateThreshold(request.getMaxThreshold());

        InspectionCriterionCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CRITERION_NOT_FOUND));

        String name = request.getName().trim();
        String unit = request.getUnit().trim();
        String referenceStandard = request.getReferenceStandard() != null
                ? request.getReferenceStandard().trim() : null;

        if (catalogRepository.existsByNameAndReferenceStandardAndIdNot(name, referenceStandard, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_DUPLICATE_CRITERION);
        }

        entity.setName(name);
        entity.setUnit(unit);
        entity.setMaxThreshold(request.getMaxThreshold());
        entity.setReferenceStandard(referenceStandard);

        return toResponse(catalogRepository.save(entity));
    }

    @Override
    public void disableCriterion(Long id, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        InspectionCriterionCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CRITERION_NOT_FOUND));

        entity.setStatus("INACTIVE");
        catalogRepository.save(entity);
        log.info("Criterion {} (id={}) disabled", entity.getName(), id);
    }

    @Override
    public InspectionCriterionCatalogResponse enableCriterion(Long id, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        InspectionCriterionCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CRITERION_NOT_FOUND));

        entity.setStatus("ACTIVE");
        return toResponse(catalogRepository.save(entity));
    }

    @Override
    public void deleteCriterion(Long id, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        InspectionCriterionCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_CRITERION_NOT_FOUND));

        // BR-5: cannot delete if referenced by any inspection criterion
        if (inspectionCriterionRepository.existsByCriterionId(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_REFERENCED_CRITERION);
        }

        // BR-3 invariant: cannot delete a criterion that is the only criterion
        // of a mandatory-inspection category
        if (categoryCriterionRepository.existsByCriterion_IdAndCategory_RequiresInspectionTrue(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Không thể xóa chỉ tiêu đang được gán cho loại nông sản bắt buộc kiểm nghiệm. "
                            + "Tắt cờ bắt buộc kiểm nghiệm hoặc ngừng sử dụng chỉ tiêu.");
        }

        catalogRepository.delete(entity);
        log.info("Criterion {} (id={}) deleted", entity.getName(), id);
    }

    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION);
        }
    }

    private void validateThreshold(BigDecimal maxThreshold) {
        if (maxThreshold == null || maxThreshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MSG_THRESHOLD_INVALID);
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

    private InspectionCriterionCatalogResponse toResponse(InspectionCriterionCatalog entity, boolean referenced) {
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
}

