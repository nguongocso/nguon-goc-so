package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestDetailCriterionResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestDetailResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestListResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.dto.response.ProductionLotTestCriteriaResponse;
import vn.nguongocso.certification.entity.AccreditationScope;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.entity.TestingUnit;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.AccreditationScopeRepository;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionCatalogRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.repository.TestingUnitRepository;
import vn.nguongocso.certification.service.InspectionRequestService;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý yêu cầu kiểm nghiệm.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InspectionRequestServiceImpl implements InspectionRequestService {

    private static final String MANAGER_ROLE = "VT-02";
    private static final String MSG_NO_PERMISSION = "Chỉ quản lý hợp tác xã được tạo yêu cầu kiểm nghiệm.";
    private static final String MSG_LOT_NOT_FOUND = "Lô sản xuất không tồn tại.";
    private static final String MSG_LOT_NOT_APPROVED = "Lô sản xuất chưa được duyệt.";
    private static final String MSG_LOT_CANCELLED = "Lô sản xuất đã bị hủy, không thể tạo yêu cầu kiểm nghiệm.";
    private static final String MSG_NO_HARVEST = "Lô sản xuất chưa có sự kiện thu hoạch.";
    private static final String MSG_NO_CRITERIA = "Phải chọn ít nhất một chỉ tiêu kiểm nghiệm.";
    private static final String MSG_CRITERION_NOT_FOUND = "Chỉ tiêu kiểm nghiệm không tồn tại.";
    private static final String MSG_CRITERION_NOT_APPLICABLE = "Chỉ tiêu kiểm nghiệm không được gán cho loại nông sản của lô.";
    private static final String MSG_CRITERION_INACTIVE = "Chỉ tiêu kiểm nghiệm đã ngừng sử dụng.";
    private static final String MSG_DUPLICATE_CRITERIA = "Danh sách chỉ tiêu kiểm nghiệm không được chứa chỉ tiêu trùng lặp.";
    private static final String MSG_SAMPLE_DATE_FUTURE = "Ngày gửi mẫu không được lớn hơn ngày hiện tại.";
    private static final String MSG_INVALID_SAMPLE_DATE = "Ngày gửi mẫu không được để trống.";
    private static final String MSG_SAMPLE_DATE_BEFORE_HARVEST = "Ngày gửi mẫu không được trước ngày thu hoạch của lô sản xuất.";
    private static final String MSG_TESTING_UNIT_NOT_FOUND = "Đơn vị kiểm nghiệm không tồn tại trong danh mục.";
    private static final String MSG_TESTING_UNIT_INACTIVE = "Đơn vị kiểm nghiệm đã ngừng hoạt động, vui lòng chọn đơn vị khác.";
    private static final String MSG_TESTING_UNIT_EXPIRED = "Đơn vị kiểm nghiệm đã hết hạn công nhận, vui lòng chọn đơn vị khác.";
    private static final String MSG_REQUEST_NOT_FOUND = "Yêu cầu kiểm nghiệm không tồn tại.";

    private final ProductionLotRepository productionLotRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionCriterionCatalogRepository inspectionCriterionCatalogRepository;
    private final CategoryCriterionRepository categoryCriterionRepository;
    private final ChainEventRepository chainEventRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;
    private final InspectionCriterionResultRepository inspectionCriterionResultRepository;
    private final TestingUnitRepository testingUnitRepository;
    private final AccreditationScopeRepository accreditationScopeRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tạo mới yêu cầu kiểm nghiệm cho lô sản xuất.
     */
    @Override
    public InspectionRequestResponse createInspectionRequest(
            UUID lotId,
            CreateInspectionRequest request,
            CustomUserDetails currentUser) {

        validatePermission(currentUser);

        ProductionLot lot = productionLotRepository
                .findByIdAndOrganization_OrganizationId(lotId, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));

        validateLot(lot);

        if (lot.getHarvestDate() != null && request != null && request.getSampleSentDate() != null
                && request.getSampleSentDate().isBefore(lot.getHarvestDate())) {
            throw new BusinessException(MSG_SAMPLE_DATE_BEFORE_HARVEST);
        }

        if (lot.getStatus() == ProductionLotStatus.DISPOSED) {
            throw new BusinessException("Lô sản xuất đã bị loại bỏ, không thể tạo yêu cầu kiểm nghiệm.");
        }

        if (request == null) {
            throw new BusinessException("Yêu cầu kiểm nghiệm không được để trống.");
        }

        String testingUnit;
        if (request.getTestingUnitId() != null) {
            TestingUnit testingUnitCatalog = testingUnitRepository
                    .findById(request.getTestingUnitId())
                    .orElseThrow(() -> new BusinessException(MSG_TESTING_UNIT_NOT_FOUND));

            if (!Boolean.TRUE.equals(testingUnitCatalog.getIsActive())) {
                throw new BusinessException(MSG_TESTING_UNIT_INACTIVE);
            }

            LocalDate today = LocalDate.now(clock);
            if (testingUnitCatalog.getAccreditationExpiryDate() != null
                    && testingUnitCatalog.getAccreditationExpiryDate().isBefore(today)) {
                throw new BusinessException(MSG_TESTING_UNIT_EXPIRED);
            }

            testingUnit = testingUnitCatalog.getName();
        } else {
            testingUnit = request.getTestingUnit() == null ? "" : request.getTestingUnit().trim();
            if (testingUnit.isBlank()) {
                throw new BusinessException("Đơn vị kiểm nghiệm không được để trống.");
            }
        }

        if (request.getCriteriaIds() == null || request.getCriteriaIds().isEmpty()) {
            throw new BusinessException(MSG_NO_CRITERIA);
        }

        Set<Long> requestedCriterionIds = new HashSet<>();
        for (Long criteriaId : request.getCriteriaIds()) {
            if (criteriaId == null) {
                throw new BusinessException(MSG_CRITERION_NOT_FOUND);
            }
            if (!requestedCriterionIds.add(criteriaId)) {
                throw new BusinessException(MSG_DUPLICATE_CRITERIA);
            }
        }

        if (request.getSampleSentDate() == null) {
            throw new BusinessException(MSG_INVALID_SAMPLE_DATE);
        }
        if (request.getSampleSentDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessException(MSG_SAMPLE_DATE_FUTURE);
        }

        InspectionRequest inspectionRequest = InspectionRequest.builder()
                .productionLot(lot)
                .inspectionUnit(testingUnit)
                .testingUnitId(request.getTestingUnitId())
                .sampleSentDate(request.getSampleSentDate())
                .status(InspectionRequestStatus.PENDING_RESULT)
                .createdBy(currentUser.getUser())
                .criteria(new ArrayList<>())
                .build();

        Set<String> requestedCriteriaKeys = new HashSet<>();
        Map<Long, String> requestedCriterionNames = new LinkedHashMap<>();

        for (Long criteriaId : request.getCriteriaIds()) {
            InspectionCriterionCatalog catalogCriterion = inspectionCriterionCatalogRepository
                    .findById(criteriaId)
                    .orElseThrow(() -> new BusinessException(MSG_CRITERION_NOT_FOUND));

            if (!"ACTIVE".equals(catalogCriterion.getStatus())) {
                throw new BusinessException(MSG_CRITERION_INACTIVE);
            }

            boolean assignedToCategory = categoryCriterionRepository
                    .existsByCategory_IdAndCriterion_Id(lot.getProductCategory().getId(), catalogCriterion.getId());

            if (!assignedToCategory) {
                throw new BusinessException(MSG_CRITERION_NOT_APPLICABLE);
            }

            requestedCriterionNames.put(criteriaId, catalogCriterion.getName());

            String criterionKey = "CAT:" + criteriaId;
            if (!requestedCriteriaKeys.add(criterionKey)) {
                throw new BusinessException(MSG_DUPLICATE_CRITERIA);
            }

            InspectionCriterion criterion = InspectionCriterion.builder()
                    .inspectionRequest(inspectionRequest)
                    .criterionCode(catalogCriterion.getName())
                    .criterionName(catalogCriterion.getName())
                    .criterionId(catalogCriterion.getId())
                    .build();

            inspectionRequest.getCriteria().add(criterion);
        }

        if (request.getTestingUnitId() != null) {
            List<AccreditationScope> scopes = accreditationScopeRepository
                    .findByTestingUnitIdWithCriterion(request.getTestingUnitId());

            if (!scopes.isEmpty()) {
                Set<Long> accreditedIds = scopes.stream()
                        .map(scope -> scope.getCriterion().getId())
                        .collect(Collectors.toSet());

                List<String> outOfScopeNames = requestedCriterionNames.entrySet().stream()
                        .filter(entry -> !accreditedIds.contains(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .toList();

                if (!outOfScopeNames.isEmpty()) {
                    inspectionRequest.setScopeWarning(Boolean.TRUE);
                    inspectionRequest.setScopeWarningDetails(String.join(", ", outOfScopeNames));
                }
            }
        }

        if (!Boolean.TRUE.equals(request.getConfirmDuplicate())) {
            List<InspectionRequest> pendingRequests = inspectionRequestRepository
                    .findByProductionLot_IdAndStatus(lot.getId(), InspectionRequestStatus.PENDING_RESULT);

            InspectionRequest duplicateRequest = pendingRequests.stream()
                    .filter(existingRequest -> isDuplicateCriteria(existingRequest, requestedCriteriaKeys))
                    .findFirst()
                    .orElse(null);

            if (duplicateRequest != null) {
                throw new DuplicateInspectionRequestException(duplicateRequest.getId());
            }
        }

        InspectionRequest saved = inspectionRequestRepository.save(inspectionRequest);

        publishActivityLog(
                currentUser,
                "CREATE_INSPECTION_REQUEST",
                "Tạo yêu cầu kiểm nghiệm cho lô " + lot.getName()
                        + " tại đơn vị '" + testingUnit + "' với "
                        + saved.getCriteria().size() + " chỉ tiêu",
                "INSPECTION_REQUEST",
                saved.getId().toString());

        return toResponse(saved);
    }

    /**
     * Lấy danh sách chỉ tiêu kiểm nghiệm áp dụng cho lô sản xuất.
     */
    @Override
    @Transactional(readOnly = true)
    public ProductionLotTestCriteriaResponse getTestCriteria(
            UUID lotId,
            CustomUserDetails currentUser) {

        ProductionLot lot = productionLotRepository
                .findByIdAndOrganization_OrganizationId(lotId, currentUser.getOrganizationId())
                .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));

        validateLot(lot);

        List<ProductionLotCertification> certifications = productionLotCertificationRepository
                .findByProductionLotId(lot.getId());

        Standard standard = !certifications.isEmpty() && certifications.get(0).getCertification() != null
                ? certifications.get(0).getCertification().getStandard()
                : null;

        List<CategoryCriterion> assignments = categoryCriterionRepository
                .findByCategoryIdAndCriteriaStatus(lot.getProductCategory().getId(), "ACTIVE");

        return ProductionLotTestCriteriaResponse.builder()
                .lotId(lot.getId())
                .standardId(standard != null ? standard.getId() : null)
                .standardName(standard != null ? standard.getName() : null)
                .criteria(assignments.stream()
                        .map(CategoryCriterion::getCriterion)
                        .map(catalogCriterion -> ProductionLotTestCriteriaResponse.TestCriterionItemResponse.builder()
                                .criteriaId(catalogCriterion.getId())
                                .code(catalogCriterion.getName())
                                .name(catalogCriterion.getName())
                                .referenceStandard(catalogCriterion.getReferenceStandard())
                                .build())
                        .toList())
                .build();
    }

    /**
     * Lấy danh sách yêu cầu kiểm nghiệm phân trang theo điều kiện lọc.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InspectionRequestListResponse> getInspectionRequests(
            UUID lotId,
            InspectionRequestStatus status,
            Pageable pageable,
            CustomUserDetails currentUser) {

        if (lotId != null) {
            ProductionLot lot = productionLotRepository
                    .findByIdAndOrganization_OrganizationId(lotId, currentUser.getOrganizationId())
                    .orElseThrow(() -> new BusinessException(MSG_LOT_NOT_FOUND));

            validateLot(lot);
        }

        Page<InspectionRequest> result;
        if (lotId == null) {
            result = status == null
                    ? inspectionRequestRepository.findByProductionLot_Organization_OrganizationId(
                            currentUser.getOrganizationId(), pageable)
                    : inspectionRequestRepository.findByProductionLot_Organization_OrganizationIdAndStatus(
                            currentUser.getOrganizationId(), status, pageable);
        } else if (status == null) {
            result = inspectionRequestRepository.findByProductionLot_Id(lotId, pageable);
        } else {
            result = inspectionRequestRepository.findByProductionLot_IdAndStatus(lotId, status, pageable);
        }

        Map<UUID, Long> failedCountByRequestId = countFailedByRequestIds(result.getContent());

        return result.map(request -> {
            int criteriaCount = request.getCriteria() == null ? 0 : request.getCriteria().size();
            int failedCriteriaCount = failedCountByRequestId.getOrDefault(request.getId(), 0L).intValue();

            return InspectionRequestListResponse.builder()
                    .testRequestId(request.getId())
                    .lotCode(request.getProductionLot() != null ? request.getProductionLot().getName() : null)
                    .status(mapStatus(request.getStatus()))
                    .testingUnit(request.getInspectionUnit())
                    .sampleSentDate(request.getSampleSentDate())
                    .criteriaCount(criteriaCount)
                    .failedCriteriaCount(failedCriteriaCount)
                    .failedRatio(computeFailedRatio(failedCriteriaCount, criteriaCount))
                    .build();
        });
    }

    /**
     * Lấy thông tin chi tiết của một yêu cầu kiểm nghiệm.
     */
    @Override
    @Transactional(readOnly = true)
    public InspectionRequestDetailResponse getDetail(
            UUID requestId,
            CustomUserDetails currentUser) {

        InspectionRequest request = inspectionRequestRepository
                .findDetailById(requestId)
                .orElseThrow(() -> new BusinessException(MSG_REQUEST_NOT_FOUND));

        if (request.getProductionLot() == null
                || request.getProductionLot().getOrganization() == null
                || !request.getProductionLot().getOrganization().getOrganizationId()
                        .equals(currentUser.getOrganizationId())) {
            throw new BusinessException(MSG_REQUEST_NOT_FOUND);
        }

        Map<UUID, InspectionCriterionResult> resultByCriterionId = inspectionCriterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(requestId)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getInspectionCriterion().getId(),
                        r -> r));

        List<InspectionRequestDetailCriterionResponse> criteria = request.getCriteria().stream()
                .map(c -> InspectionRequestDetailCriterionResponse.builder()
                        .criterionId(c.getId())
                        .criterionDefinitionId(c.getCriterionId())
                        .code(c.getCriterionCode())
                        .name(c.getCriterionName())
                        .standardName(c.getStandard() != null ? c.getStandard().getName() : null)
                        .result(toResultResponse(resultByCriterionId.get(c.getId())))
                        .build())
                .toList();

        int totalCriteria = criteria.size();
        int evaluatedCriteria = 0;
        int passedCriteria = 0;
        int failedCriteriaCount = 0;

        for (InspectionRequestDetailCriterionResponse criterion : criteria) {
            if (criterion.getResult() != null) {
                evaluatedCriteria++;
                if (Boolean.TRUE.equals(criterion.getResult().getPassed())) {
                    passedCriteria++;
                } else {
                    failedCriteriaCount++;
                }
            }
        }

        return InspectionRequestDetailResponse.builder()
                .testRequestId(request.getId())
                .lotId(request.getProductionLot().getId())
                .lotCode(request.getProductionLot().getName())
                .status(mapStatus(request.getStatus()))
                .testingUnit(request.getInspectionUnit())
                .sampleSentDate(request.getSampleSentDate())
                .totalCriteria(totalCriteria)
                .evaluatedCriteria(evaluatedCriteria)
                .passedCriteria(passedCriteria)
                .failedCriteriaCount(failedCriteriaCount)
                .failedRatio(computeFailedRatio(failedCriteriaCount, totalCriteria))
                .criteria(criteria)
                .build();
    }

    /**
     * Chuyển kết quả kiểm nghiệm thành DTO (null nếu chưa có kết quả).
     */
    private InspectionCriterionResultResponse toResultResponse(InspectionCriterionResult result) {
        if (result == null) {
            return null;
        }

        return InspectionCriterionResultResponse.builder()
                .resultId(result.getId().toString())
                .criterionId(result.getInspectionCriterion().getId().toString())
                .criterionDefinitionId(result.getInspectionCriterion().getCriterionId())
                .criterionCode(result.getInspectionCriterion().getCriterionCode())
                .criterionName(result.getInspectionCriterion().getCriterionName())
                .resultDate(result.getResultDate())
                .expiryDate(result.getExpiryDate())
                .passed(result.getPassed())
                .filePath(result.getFilePath())
                .entrySource(result.getEntrySource())
                .createdByName(result.getCreatedBy() != null && result.getCreatedBy().getFullName() != null
                        ? result.getCreatedBy().getFullName()
                        : null)
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }

    /**
     * Kiểm tra quyền tạo yêu cầu kiểm nghiệm của người dùng.
     */
    private void validatePermission(CustomUserDetails currentUser) {
        if (!MANAGER_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(MSG_NO_PERMISSION);
        }
    }

    /**
     * Kiểm tra điều kiện hợp lệ của lô sản xuất để tạo yêu cầu kiểm nghiệm.
     */
    private void validateLot(ProductionLot lot) {
        if (lot.getStatus() == null
                || lot.getStatus() == ProductionLotStatus.REJECTED
                || lot.getStatus().ordinal() < ProductionLotStatus.APPROVED.ordinal()) {
            throw new BusinessException(MSG_LOT_NOT_APPROVED);
        }

        if (lot.getStatus() == ProductionLotStatus.CANCELLED) {
            throw new BusinessException(MSG_LOT_CANCELLED);
        }

        boolean hasHarvest = chainEventRepository.existsByProductionLotIdOrUnassignedEventDataAndEventType(
                lot.getId(), lot.getId().toString(), ChainEventType.HARVEST);

        if (!hasHarvest) {
            throw new BusinessException(MSG_NO_HARVEST);
        }
    }

    /**
     * Kiểm tra yêu cầu kiểm nghiệm có trùng lặp bộ chỉ tiêu với yêu cầu đang chờ
     * hay không.
     */
    private boolean isDuplicateCriteria(
            InspectionRequest existingRequest,
            Set<String> requestedCriteriaKeys) {

        if (existingRequest == null || existingRequest.getCriteria() == null) {
            return false;
        }

        Set<String> existingCriteriaKeys = existingRequest.getCriteria().stream()
                .filter(c -> c != null && c.getCriterionCode() != null)
                .map(this::resolveCriterionKey)
                .collect(Collectors.toSet());

        return existingCriteriaKeys.equals(requestedCriteriaKeys);
    }

    /**
     * Xác định khóa định danh cho chỉ tiêu kiểm nghiệm.
     */
    private String resolveCriterionKey(InspectionCriterion criterion) {
        if (criterion.getCriterionId() != null) {
            return "CAT:" + criterion.getCriterionId();
        }

        if (criterion.getStandard() != null) {
            return criterion.getStandard().getId().toString() + ":" + criterion.getCriterionCode();
        }

        return "LEGACY:" + criterion.getCriterionCode();
    }

    /**
     * Ghi nhật ký hoạt động của hệ thống khi có thao tác kiểm nghiệm.
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

    /**
     * Chuyển đổi entity yêu cầu kiểm nghiệm sang DTO phản hồi.
     */
    private InspectionRequestResponse toResponse(InspectionRequest request) {
        if (request == null) {
            throw new IllegalStateException("InspectionRequestRepository.save() không được trả về null.");
        }

        return InspectionRequestResponse.builder()
                .testRequestId(request.getId())
                .lotId(request.getProductionLot().getId())
                .lotCode(request.getProductionLot().getName())
                .testingUnit(request.getInspectionUnit())
                .testingUnitId(request.getTestingUnitId())
                .hasScopeWarning(Boolean.TRUE.equals(request.getScopeWarning()) ? Boolean.TRUE : Boolean.FALSE)
                .scopeWarningDetails(request.getScopeWarningDetails())
                .sampleSentDate(request.getSampleSentDate())
                .status(mapStatus(request.getStatus()))
                .createdBy(request.getCreatedBy() != null && request.getCreatedBy().getFullName() != null
                        ? request.getCreatedBy().getFullName()
                        : "")
                .createdAt(request.getCreatedAt())
                .criteria(request.getCriteria().stream()
                        .map(c -> InspectionCriterionResponse.builder()
                                .criteriaId(null)
                                .code(c.getCriterionCode())
                                .name(c.getCriterionName())
                                .standardId(c.getStandard() != null ? c.getStandard().getId() : null)
                                .standardName(c.getStandard() != null ? c.getStandard().getName() : null)
                                .build())
                        .toList())
                .build();
    }

    /**
     * Ánh xạ trạng thái yêu cầu kiểm nghiệm sang chuỗi phản hồi.
     */
    private String mapStatus(InspectionRequestStatus status) {
        if (status == null) {
            return "PENDING";
        }

        return switch (status) {
            case PENDING_RESULT -> "PENDING";
            case PASSED -> "PASSED";
            case FAILED -> "FAILED";
            case CANCELLED -> "CANCELLED";
        };
    }

    /**
     * Đếm số chỉ tiêu không đạt theo danh sách mã yêu cầu kiểm nghiệm.
     */
    private Map<UUID, Long> countFailedByRequestIds(List<InspectionRequest> requests) {
        List<UUID> requestIds = requests.stream()
                .map(InspectionRequest::getId)
                .toList();

        if (requestIds.isEmpty()) {
            return Map.of();
        }

        return inspectionCriterionResultRepository.countFailedCriteriaByRequestIds(requestIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]));
    }

    /**
     * Tính tỷ lệ phần trăm chỉ tiêu không đạt trên tổng số chỉ tiêu.
     */
    private double computeFailedRatio(int failedCriteriaCount, int totalCriteria) {
        if (totalCriteria <= 0) {
            return 0.0;
        }

        return Math.round(failedCriteriaCount * 1000.0 / totalCriteria) / 10.0;
    }
}
