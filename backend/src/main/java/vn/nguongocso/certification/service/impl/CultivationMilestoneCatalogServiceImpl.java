package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneCatalogRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneCatalogResponse;
import vn.nguongocso.certification.entity.CultivationMilestoneCatalog;
import vn.nguongocso.certification.repository.CultivationMilestoneCatalogRepository;
import vn.nguongocso.certification.repository.ProductCategoryMilestoneRepository;
import vn.nguongocso.certification.service.CultivationMilestoneCatalogService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of CultivationMilestoneCatalogService.
 * Story: NCL-09-CN-011
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CultivationMilestoneCatalogServiceImpl implements CultivationMilestoneCatalogService {

    private static final String ADMIN_ROLE = "VT-01";
    private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục mốc canh tác.";
    private static final String MSG_MILESTONE_NOT_FOUND = "Mốc canh tác không tồn tại.";
    private static final String MSG_DUPLICATE_MILESTONE =
            "Mốc canh tác với tên và loại hoạt động này đã tồn tại.";
    private static final String MSG_REFERENCED_MILESTONE =
            "Mốc canh tác đang được gán cho loại nông sản, không thể xóa.";

    private final CultivationMilestoneCatalogRepository catalogRepository;
    private final ProductCategoryMilestoneRepository categoryMilestoneRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CultivationMilestoneCatalogResponse> searchMilestones(
            String keyword, String status, String activityType, Pageable pageable, CustomUserDetails currentUser) {

        if (status != null) {
            String upperStatus = status.toUpperCase();
            if (!"ACTIVE".equals(upperStatus) && !"INACTIVE".equals(upperStatus)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Trạng thái không hợp lệ. Giá trị hợp lệ: ACTIVE, INACTIVE.");
            }
            status = upperStatus;
        }

        Set<Long> referencedIds = loadReferencedIds();
        return catalogRepository.search(keyword, status, activityType, pageable)
                .map(entity -> toResponse(entity, referencedIds));
    }

    private Set<Long> loadReferencedIds() {
        return new HashSet<>(categoryMilestoneRepository.findReferencedMilestoneIds());
    }

    @Override
    @Transactional(readOnly = true)
    public CultivationMilestoneCatalogResponse getMilestone(Long id, CustomUserDetails currentUser) {
        CultivationMilestoneCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));
        return toResponse(entity);
    }

    @Override
    public CultivationMilestoneCatalogResponse createMilestone(
            CultivationMilestoneCatalogRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        String name = request.getName().trim();
        String activityType = request.getActivityType().trim();

        if (catalogRepository.existsByNameAndActivityType(name, activityType)) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_DUPLICATE_MILESTONE);
        }

        CultivationMilestoneCatalog entity = CultivationMilestoneCatalog.builder()
                .name(name)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .activityType(activityType)
                .expectedDaysFromPlanting(request.getExpectedDaysFromPlanting())
                .status("ACTIVE")
                .build();

        return toResponse(catalogRepository.save(entity));
    }

    @Override
    public CultivationMilestoneCatalogResponse updateMilestone(
            Long id, CultivationMilestoneCatalogRequest request, CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        CultivationMilestoneCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));

        String name = request.getName().trim();
        String activityType = request.getActivityType().trim();

        if (catalogRepository.existsByNameAndActivityTypeAndIdNot(name, activityType, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_DUPLICATE_MILESTONE);
        }

        entity.setName(name);
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        entity.setActivityType(activityType);
        entity.setExpectedDaysFromPlanting(request.getExpectedDaysFromPlanting());

        return toResponse(catalogRepository.save(entity));
    }

    @Override
    public void disableMilestone(Long id, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        CultivationMilestoneCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));

        entity.setStatus("INACTIVE");
        catalogRepository.save(entity);
        log.info("Milestone {} (id={}) disabled", entity.getName(), id);
    }

    @Override
    public CultivationMilestoneCatalogResponse enableMilestone(Long id, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        CultivationMilestoneCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));

        entity.setStatus("ACTIVE");
        return toResponse(catalogRepository.save(entity));
    }

    @Override
    public void deleteMilestone(Long id, CustomUserDetails currentUser) {
        validateAdminPermission(currentUser);

        CultivationMilestoneCatalog entity = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));

        if (categoryMilestoneRepository.existsByMilestone_Id(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, MSG_REFERENCED_MILESTONE);
        }

        catalogRepository.delete(entity);
        log.info("Milestone {} (id={}) deleted", entity.getName(), id);
    }

    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, MSG_NO_PERMISSION);
        }
    }

    private CultivationMilestoneCatalogResponse toResponse(CultivationMilestoneCatalog entity) {
        boolean referenced = categoryMilestoneRepository.existsByMilestone_Id(entity.getId());
        return toResponse(entity, referenced);
    }

    private CultivationMilestoneCatalogResponse toResponse(
            CultivationMilestoneCatalog entity, Set<Long> referencedIds) {
        return toResponse(entity, referencedIds.contains(entity.getId()));
    }

    private CultivationMilestoneCatalogResponse toResponse(
            CultivationMilestoneCatalog entity, boolean referenced) {
        return CultivationMilestoneCatalogResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .activityType(entity.getActivityType())
                .expectedDaysFromPlanting(entity.getExpectedDaysFromPlanting())
                .status(entity.getStatus())
                .referenced(referenced)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
