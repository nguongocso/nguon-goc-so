package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneResponse;
import vn.nguongocso.certification.dto.response.MilestoneEligibilityResponse;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.repository.CultivationMilestoneRepository;
import vn.nguongocso.certification.repository.StandardRepository;
import vn.nguongocso.certification.service.CultivationMilestoneService;
import vn.nguongocso.certification.service.MilestoneValidationService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Triển khai các phương thức của CultivationMilestoneService (NCL-09-CN-011).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CultivationMilestoneServiceImpl implements CultivationMilestoneService {
        private static final String ADMIN_ROLE = "VT-01";
        private static final String MSG_NO_PERMISSION = "Bạn không có quyền quản lý danh mục mốc canh tác.";
        private static final String MSG_MILESTONE_NOT_FOUND = "Mốc canh tác không tồn tại.";
        private static final String MSG_INVALID_CATEGORY = "Loại nông sản không tồn tại.";
        private static final String MSG_INVALID_STANDARD = "Tiêu chuẩn không tồn tại.";
        private static final String MSG_INVALID_ACTIVITY = "Loại hoạt động không hợp lệ.";
        private static final String MSG_DUPLICATE_MILESTONE = "Mốc canh tác với tên này đã tồn tại trong cùng loại nông sản và tiêu chuẩn.";
        private static final String MSG_LOT_NOT_FOUND = "Không tìm thấy lô sản xuất.";
        private static final String MSG_LOT_FORBIDDEN = "Bạn không thuộc tổ chức quản lý của lô sản xuất này.";

        private final CultivationMilestoneRepository milestoneRepository;
        private final ProductCategoryRepository productCategoryRepository;
        private final StandardRepository standardRepository;
        private final MilestoneValidationService milestoneValidationService;
        private final ProductionLotRepository productionLotRepository;

        /**
         * Tìm kiếm mốc canh tác theo bộ lọc và phân trang.
         */
        @Override
        @Transactional(readOnly = true)
        public Page<CultivationMilestoneResponse> searchMilestones(
                        String keyword, String activityType, UUID categoryId, UUID standardId,
                        boolean globalOnly, Pageable pageable, CustomUserDetails currentUser) {
                if (activityType != null && !isValidActivityType(activityType)) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, MSG_INVALID_ACTIVITY);
                }

                return milestoneRepository.search(keyword, activityType, categoryId, standardId, globalOnly, pageable)
                                .map(this::toResponse);
        }

        /**
         * Lấy chi tiết mốc canh tác theo ID.
         */
        @Override
        @Transactional(readOnly = true)
        public CultivationMilestoneResponse getMilestone(Long id, CustomUserDetails currentUser) {
                CultivationMilestone entity = milestoneRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));
                return toResponse(entity);
        }

        /**
         * Tạo mới mốc canh tác vào hệ thống.
         */
        @Override
        public CultivationMilestoneResponse createMilestone(
                        CultivationMilestoneRequest request, CustomUserDetails currentUser) {
                validateAdminPermission(currentUser);
                validateRequest(request);

                ProductCategory category = resolveCategory(request.getProductCategoryId());
                Standard standard = resolveStandard(request.getStandardId());

                String name = request.getName().trim();
                String activityType = request.getActivityType().trim();

                if (milestoneRepository.existsByNameAndCategoryAndStandard(
                                name, request.getProductCategoryId(), request.getStandardId())) {
                        throw new BusinessException(HttpStatus.CONFLICT, MSG_DUPLICATE_MILESTONE);
                }

                CultivationMilestone entity = CultivationMilestone.builder()
                                .name(name)
                                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                                .activityType(activityType)
                                .expectedDaysFromPlanting(request.getExpectedDaysFromPlanting())
                                .productCategory(category)
                                .standard(standard)
                                .isMandatory(Boolean.TRUE.equals(request.getIsMandatory()))
                                .build();

                return toResponse(milestoneRepository.save(entity));
        }

        /**
         * Cập nhật thông tin mốc canh tác.
         */
        @Override
        public CultivationMilestoneResponse updateMilestone(
                        Long id, CultivationMilestoneRequest request, CustomUserDetails currentUser) {
                validateAdminPermission(currentUser);
                validateRequest(request);

                CultivationMilestone entity = milestoneRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(MSG_MILESTONE_NOT_FOUND));

                if (milestoneRepository.existsByNameAndCategoryAndStandardAndIdNot(
                                request.getName().trim(), request.getProductCategoryId(), request.getStandardId(),
                                id)) {
                        throw new BusinessException(HttpStatus.CONFLICT, MSG_DUPLICATE_MILESTONE);
                }

                entity.setName(request.getName().trim());
                entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
                entity.setActivityType(request.getActivityType().trim());
                entity.setExpectedDaysFromPlanting(request.getExpectedDaysFromPlanting());
                entity.setProductCategory(resolveCategory(request.getProductCategoryId()));
                entity.setStandard(resolveStandard(request.getStandardId()));
                entity.setIsMandatory(Boolean.TRUE.equals(request.getIsMandatory()));

                return toResponse(milestoneRepository.save(entity));
        }

        /**
         * Kiểm tra lô sản xuất đã đủ mốc canh tác bắt buộc để ghi sự kiện đóng gói chưa (NCL-09-CN-011).
         */
        @Override
        @Transactional(readOnly = true)
        public MilestoneEligibilityResponse getPackagingEligibility(
                        UUID productionLotId, CustomUserDetails currentUser) {
                ProductionLot lot = productionLotRepository.findById(productionLotId)
                                .orElseThrow(() -> new ResourceNotFoundException(MSG_LOT_NOT_FOUND));

                validateLotOrganization(lot, currentUser);

                List<MilestoneEligibilityResponse.MissingMilestone> missing = milestoneValidationService
                                .findMissingMilestones(lot).stream()
                                .map(m -> MilestoneEligibilityResponse.MissingMilestone.builder()
                                                .name(m.getName())
                                                .activityType(m.getActivityType())
                                                .build())
                                .toList();

                return MilestoneEligibilityResponse.builder()
                                .productionLotId(lot.getId())
                                .eligible(missing.isEmpty())
                                .missingMilestones(missing)
                                .build();
        }

        /**
         * Kiểm tra quyền sở hữu lô sản xuất của người dùng.
         */
        private void validateLotOrganization(ProductionLot lot, CustomUserDetails currentUser) {
                if (ADMIN_ROLE.equals(currentUser.getRoleCode())) {
                        return;
                }
                if (!lot.getOrganization().getOrganizationId().equals(currentUser.getOrganizationId())) {
                        throw new BusinessException(HttpStatus.FORBIDDEN, MSG_LOT_FORBIDDEN);
                }
        }

        /**
         * Kiểm tra tính hợp lệ của yêu cầu tạo hoặc cập nhật mốc canh tác.
         */
        private void validateRequest(CultivationMilestoneRequest request) {
                if (!isValidActivityType(request.getActivityType())) {
                        throw new BusinessException(HttpStatus.BAD_REQUEST, MSG_INVALID_ACTIVITY);
                }
        }

        /**
         * Kiểm tra loại hoạt động canh tác có hợp lệ trong hệ thống hay không.
         */
        private boolean isValidActivityType(String activityType) {
                if (activityType == null) {
                        return false;
                }
                return Arrays.stream(FarmActivityType.values())
                                .anyMatch(t -> t.name().equalsIgnoreCase(activityType.trim()));
        }

        /**
         * Tìm thực thể loại nông sản theo ID.
         */
        private ProductCategory resolveCategory(UUID categoryId) {
                if (categoryId == null) {
                        return null;
                }
                return productCategoryRepository.findById(categoryId)
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, MSG_INVALID_CATEGORY));
        }

        /**
         * Tìm thực thể tiêu chuẩn theo ID.
         */
        private Standard resolveStandard(UUID standardId) {
                if (standardId == null) {
                        return null;
                }
                return standardRepository.findById(standardId)
                                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, MSG_INVALID_STANDARD));
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
         * Chuyển đổi thực thể mốc canh tác sang DTO phản hồi.
         */
        private CultivationMilestoneResponse toResponse(CultivationMilestone entity) {
                return CultivationMilestoneResponse.builder()
                                .id(entity.getId())
                                .name(entity.getName())
                                .description(entity.getDescription())
                                .activityType(entity.getActivityType())
                                .expectedDaysFromPlanting(entity.getExpectedDaysFromPlanting())
                                .productCategoryId(entity.getProductCategory() != null
                                                ? entity.getProductCategory().getId().toString()
                                                : null)
                                .productCategoryName(entity.getProductCategory() != null
                                                ? entity.getProductCategory().getName()
                                                : null)
                                .standardId(entity.getStandard() != null
                                                ? entity.getStandard().getId().toString()
                                                : null)
                                .standardName(entity.getStandard() != null
                                                ? entity.getStandard().getName()
                                                : null)
                                .isMandatory(Boolean.TRUE.equals(entity.getIsMandatory()))
                                .createdAt(entity.getCreatedAt())
                                .updatedAt(entity.getUpdatedAt())
                                .build();
        }
}
