package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.AccreditationScopeSummaryResponse;
import vn.nguongocso.certification.entity.AccreditationScope;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.repository.AccreditationScopeRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.AccreditationScopeService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai các phương thức của AccreditationScopeService.
 * <p>
 * Quy tắc nghiệp vụ (theo mẫu TestingUnitServiceImpl / CategoryCriterion):
 * - Chỉ Quản trị viên nền tảng (VT-01) được cập nhật phạm vi công nhận.
 * - Ngữ nghĩa REPLACE-ALL: luôn xoá toàn bộ rồi ghi lại tập chỉ tiêu mới.
 * - Chỉ tiêu phải tồn tại và ACTIVE trong danh mục dùng chung.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccreditationScopeServiceImpl
        implements AccreditationScopeService {

    private static final String ADMIN_ROLE = "VT-01";

    private static final String MSG_NO_PERMISSION =
            "Bạn không có quyền quản lý phạm vi công nhận của đơn vị kiểm nghiệm.";

    private static final String MSG_UNIT_NOT_FOUND =
            "Đơn vị kiểm nghiệm không tồn tại.";

    private static final String MSG_CRITERIA_EMPTY =
            "Danh sách chỉ tiêu không được để trống.";

    private static final String MSG_CRITERION_NOT_FOUND =
            "Một hoặc nhiều chỉ tiêu không tồn tại trong danh mục dùng chung.";

    private static final String MSG_CRITERION_INACTIVE =
            "Một hoặc nhiều chỉ tiêu đã ngừng sử dụng, không thể gán vào phạm vi công nhận.";

    private final TestingUnitRepository testingUnitRepository;

    private final InspectionCriterionCatalogRepository inspectionCriterionCatalogRepository;

    private final AccreditationScopeRepository accreditationScopeRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public AccreditationScopeSummaryResponse getAccreditationScope(
            UUID testingUnitId) {

        TestingUnit unit = testingUnitRepository.findById(testingUnitId)
                .orElseThrow(() -> new BusinessException(MSG_UNIT_NOT_FOUND));

        List<AccreditationScope> scopes =
                accreditationScopeRepository.findByTestingUnitIdWithCriterion(testingUnitId);

        return buildSummary(unit, scopes);
    }

    @Override
    public AccreditationScopeSummaryResponse updateAccreditationScope(
            UUID testingUnitId,
            List<Long> criterionDefinitionIds,
            CustomUserDetails currentUser) {

        validateAdminPermission(currentUser);

        TestingUnit unit = testingUnitRepository.findById(testingUnitId)
                .orElseThrow(() -> new BusinessException(MSG_UNIT_NOT_FOUND));

        if (criterionDefinitionIds == null || criterionDefinitionIds.isEmpty()) {
            throw new BusinessException(MSG_CRITERIA_EMPTY);
        }

        List<Long> distinctIds = criterionDefinitionIds.stream()
                .distinct()
                .toList();

        List<InspectionCriterionCatalog> criteria =
                inspectionCriterionCatalogRepository.findByIdIn(distinctIds);

        if (criteria.size() != distinctIds.size()) {
            throw new BusinessException(MSG_CRITERION_NOT_FOUND);
        }

        for (InspectionCriterionCatalog criterion : criteria) {
            if (!"ACTIVE".equals(criterion.getStatus())) {
                throw new BusinessException(MSG_CRITERION_INACTIVE);
            }
        }

        /*
         * REPLACE-ALL: xoá toàn bộ phạm vi hiện tại rồi ghi lại tập mới.
         */
        accreditationScopeRepository.deleteByTestingUnitId(testingUnitId);

        List<AccreditationScope> savedScopes = new ArrayList<>();

        for (InspectionCriterionCatalog criterion : criteria) {
            savedScopes.add(accreditationScopeRepository.save(
                    AccreditationScope.builder()
                            .testingUnit(unit)
                            .criterion(criterion)
                            .criterionCode(criterion.getName())
                            .criterionName(criterion.getName())
                            .build()));
        }

        publishActivityLog(
                currentUser,
                "UPDATE_ACCREDITATION_SCOPE",
                "Cập nhật phạm vi công nhận của đơn vị kiểm nghiệm '"
                        + unit.getName() + "' với "
                        + savedScopes.size() + " chỉ tiêu",
                "TESTING_UNIT",
                unit.getId().toString());

        return buildSummary(unit, savedScopes);
    }

    /**
     * Kiểm tra người dùng có quyền Quản trị viên nền tảng hay không.
     */
    private void validateAdminPermission(CustomUserDetails currentUser) {
        if (currentUser == null || !ADMIN_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
    }

    private AccreditationScopeSummaryResponse buildSummary(
            TestingUnit unit,
            List<AccreditationScope> scopes) {

        return AccreditationScopeSummaryResponse.builder()
                .testingUnitId(unit.getId())
                .testingUnitName(unit.getName())
                .accreditedCriteria(
                        scopes.stream()
                                .map(scope -> AccreditationScopeSummaryResponse
                                        .AccreditedCriterionItem
                                        .builder()
                                        .id(scope.getCriterion().getId())
                                        .code(scope.getCriterionCode())
                                        .name(scope.getCriterionName())
                                        .build())
                                .collect(Collectors.toList()))
                .build();
    }

    /**
     * Ghi nhật ký hoạt động theo convention của hệ thống (TASK-27).
     */
    private void publishActivityLog(
            CustomUserDetails currentUser,
            String action,
            String description,
            String entityType,
            String entityId) {

        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }
}

