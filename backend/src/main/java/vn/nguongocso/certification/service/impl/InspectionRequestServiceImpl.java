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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionRequestServiceImpl
        implements InspectionRequestService {

    private static final String MANAGER_ROLE = "VT-02";

    private static final String MSG_NO_PERMISSION =
            "Chỉ quản lý hợp tác xã được tạo yêu cầu kiểm nghiệm.";

    private static final String MSG_LOT_NOT_FOUND =
            "Lô sản xuất không tồn tại.";

    private static final String MSG_LOT_NOT_APPROVED =
            "Lô sản xuất chưa được duyệt.";

    private static final String MSG_LOT_CANCELLED =
            "Lô sản xuất đã bị hủy, không thể tạo yêu cầu kiểm nghiệm.";

    private static final String MSG_NO_HARVEST =
            "Lô sản xuất chưa có sự kiện thu hoạch.";

    private static final String MSG_NO_CRITERIA =
            "Phải chọn ít nhất một chỉ tiêu kiểm nghiệm.";

    private static final String MSG_CRITERION_NOT_FOUND =
            "Chỉ tiêu kiểm nghiệm không tồn tại.";

    private static final String MSG_CRITERION_NOT_APPLICABLE =
            "Chỉ tiêu kiểm nghiệm không được gán cho loại nông sản của lô.";

    private static final String MSG_CRITERION_INACTIVE =
            "Chỉ tiêu kiểm nghiệm đã ngừng sử dụng.";

    private static final String MSG_DUPLICATE_CRITERIA =
            "Danh sách chỉ tiêu kiểm nghiệm không được chứa chỉ tiêu trùng lặp.";

    private static final String MSG_SAMPLE_DATE_FUTURE =
            "Ngày gửi mẫu không được lớn hơn ngày hiện tại.";

    private static final String MSG_INVALID_SAMPLE_DATE =
            "Ngày gửi mẫu không được để trống.";

    private static final String MSG_TESTING_UNIT_NOT_FOUND =
            "Đơn vị kiểm nghiệm không tồn tại trong danh mục.";

    private static final String MSG_TESTING_UNIT_INACTIVE =
            "Đơn vị kiểm nghiệm đã ngừng hoạt động, vui lòng chọn đơn vị khác.";

    private static final String MSG_TESTING_UNIT_EXPIRED =
            "Đơn vị kiểm nghiệm đã hết hạn công nhận, vui lòng chọn đơn vị khác.";

    private static final String MSG_REQUEST_NOT_FOUND =
            "Yêu cầu kiểm nghiệm không tồn tại.";

    private final ProductionLotRepository productionLotRepository;

    private final InspectionRequestRepository inspectionRequestRepository;

    private final InspectionCriterionCatalogRepository
            inspectionCriterionCatalogRepository;

    private final CategoryCriterionRepository categoryCriterionRepository;

    private final ChainEventRepository chainEventRepository;

    private final ProductionLotCertificationRepository
            productionLotCertificationRepository;

    private final InspectionCriterionResultRepository
            inspectionCriterionResultRepository;

    private final TestingUnitRepository testingUnitRepository;

    private final AccreditationScopeRepository accreditationScopeRepository;

    private final Clock clock;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public InspectionRequestResponse createInspectionRequest(
            UUID lotId,
            CreateInspectionRequest request,
            CustomUserDetails currentUser) {

        /*
         * 1. Kiểm tra quyền.
         */
        validatePermission(currentUser);

        /*
         * 2. Lấy ProductionLot thuộc organization hiện tại.
         */
        ProductionLot lot = productionLotRepository
                .findByIdAndOrganization_OrganizationId(
                        lotId,
                        currentUser.getOrganizationId())
                .orElseThrow(() ->
                        new BusinessException(MSG_LOT_NOT_FOUND));

        /*
         * 3. Kiểm tra điều kiện của lot.
         */
        validateLot(lot);

        /*
         * 4. Validate request.
         */
        if (request == null) {
            throw new BusinessException(
                    "Yêu cầu kiểm nghiệm không được để trống.");
        }

        /*
         * 4.1. Validate đơn vị kiểm nghiệm.
         *
         * NCL-11-CN-006 Phase 1: ưu tiên testingUnitId từ danh mục dùng chung.
         * Khi có ID, tra cứu danh mục, kiểm tra trạng thái hiệu lực và ngày
         * hết hạn công nhận, rồi dùng TÊN SNAPSHOT làm inspection_unit.
         * Nếu không có ID, fallback về tên tự do (tương thích ngược).
         */
        String testingUnit;

        if (request.getTestingUnitId() != null) {

            TestingUnit testingUnitCatalog = testingUnitRepository
                    .findById(request.getTestingUnitId())
                    .orElseThrow(() ->
                            new BusinessException(
                                    MSG_TESTING_UNIT_NOT_FOUND));

            if (!Boolean.TRUE.equals(testingUnitCatalog.getIsActive())) {
                throw new BusinessException(
                        MSG_TESTING_UNIT_INACTIVE);
            }

            LocalDate today = LocalDate.now(clock);
            if (testingUnitCatalog.getAccreditationExpiryDate() != null
                    && testingUnitCatalog.getAccreditationExpiryDate()
                            .isBefore(today)) {

                throw new BusinessException(
                        MSG_TESTING_UNIT_EXPIRED);
            }

            testingUnit = testingUnitCatalog.getName();

        } else {

            testingUnit = request.getTestingUnit() == null
                    ? ""
                    : request.getTestingUnit().trim();

            if (testingUnit.isBlank()) {
                throw new BusinessException(
                        "Đơn vị kiểm nghiệm không được để trống.");
            }
        }

        /*
         * 4.2. Validate danh sách criterion.
         */
        if (request.getCriteriaIds() == null
                || request.getCriteriaIds().isEmpty()) {

            throw new BusinessException(MSG_NO_CRITERIA);
        }

        /*
         * Kiểm tra criterion ID bị null hoặc trùng.
         */
        Set<Long> requestedCriterionIds =
                new HashSet<>();

        for (Long criteriaId : request.getCriteriaIds()) {

            if (criteriaId == null) {
                throw new BusinessException(
                        MSG_CRITERION_NOT_FOUND);
            }

            if (!requestedCriterionIds.add(criteriaId)) {
                throw new BusinessException(
                        MSG_DUPLICATE_CRITERIA);
            }
        }

        /*
         * 4.3. Validate ngày gửi mẫu.
         */
        if (request.getSampleSentDate() == null) {
            throw new BusinessException(
                    MSG_INVALID_SAMPLE_DATE);
        }

        if (request.getSampleSentDate()
                .isAfter(LocalDate.now(clock))) {

            throw new BusinessException(
                    MSG_SAMPLE_DATE_FUTURE);
        }

        /*
         * 5. Khởi tạo InspectionRequest.
         */
        InspectionRequest inspectionRequest =
                InspectionRequest.builder()
                        .productionLot(lot)
                        .inspectionUnit(testingUnit)
                        .testingUnitId(request.getTestingUnitId())
                        .sampleSentDate(
                                request.getSampleSentDate())
                        .status(
                                InspectionRequestStatus.PENDING_RESULT)
                        .createdBy(
                                currentUser.getUser())
                        .criteria(
                                new ArrayList<>())
                        .build();

        /*
         * 6. Xây dựng bộ khóa criterion của request hiện tại.
         *
         * Identity của chỉ tiêu dựa trên criterionId (không phải name).
         * Hai chỉ tiêu khác ID nhưng cùng tên KHÔNG bị coi là trùng.
         *
         * Format khóa cho request mới:
         *
         *     "CAT:<criterionId>"
         *
         * Khóa phải đồng nhất với resolveCriterionKey() bên dưới.
         */
        Set<String> requestedCriteriaKeys =
                new HashSet<>();

        /*
         * Map criteriaId -> tên chỉ tiêu đã được duyệt hợp lệ ở vòng lặp dưới.
         * Dùng để mô tả các chỉ tiêu ngoài phạm vi công nhận (NCL-11-CN-006 Phase 2).
         */
        Map<Long, String> requestedCriterionNames =
                new java.util.LinkedHashMap<>();

        /*
         * 7. Xử lý từng criterion.
         */
        for (Long criteriaId : request.getCriteriaIds()) {

            /*
             * 7.1. Lấy chỉ tiêu từ danh mục dùng chung (NCL-09-CN-009).
             */
            InspectionCriterionCatalog catalogCriterion =
                    inspectionCriterionCatalogRepository
                            .findById(criteriaId)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            MSG_CRITERION_NOT_FOUND));

            /*
             * 7.2. Chỉ tiêu phải đang ở trạng thái ACTIVE.
             */
            if (!"ACTIVE".equals(catalogCriterion.getStatus())) {
                throw new BusinessException(
                        MSG_CRITERION_INACTIVE);
            }

            /*
             * 7.3. Chỉ tiêu phải được gán cho loại nông sản
             * của lô (cấu hình do NCL-09-CN-009 quản lý).
             */
            boolean assignedToCategory =
                    categoryCriterionRepository
                            .existsByCategory_IdAndCriterion_Id(
                                    lot.getProductCategory().getId(),
                                    catalogCriterion.getId());

            if (!assignedToCategory) {
                throw new BusinessException(
                        MSG_CRITERION_NOT_APPLICABLE);
            }

            /* Lưu tên chỉ tiêu đã duyệt hợp lệ để mô tả cảnh báo phạm vi. */
            requestedCriterionNames.put(
                    criteriaId,
                    catalogCriterion.getName());

            /*
             * 7.4. Tạo khóa criterion.
             *
             * QUAN TRỌNG:
             * Phải đồng nhất với resolveCriterionKey() bên dưới
             * để so khớp duplicate request chính xác.
             *
             * Identity của chỉ tiêu dựa trên criterionId (không phải name).
             * Hai chỉ tiêu khác ID nhưng cùng tên KHÔNG bị coi là trùng.
             */
            String criterionKey =
                    "CAT:" + criteriaId;

            /*
             * Nếu cùng criterion xuất hiện nhiều lần
             * trong request hiện tại thì từ chối.
             */
            if (!requestedCriteriaKeys.add(
                    criterionKey)) {

                throw new BusinessException(
                        MSG_DUPLICATE_CRITERIA);
            }

            /*
             * 7.5. Tạo snapshot InspectionCriterion.
             *
             * Name lấy từ danh mục; criterionId tham chiếu về
             * bản ghi danh mục (nullable — không hồi tố dữ liệu cũ,
             * BR-5/BR-7 của NCL-09-CN-009). Không gắn Standard vì
             * chỉ tiêu mới không còn sở hữu theo tiêu chuẩn.
             */
            InspectionCriterion criterion =
                    InspectionCriterion.builder()
                            .inspectionRequest(
                                    inspectionRequest)
                            .criterionCode(
                                    catalogCriterion.getName())
                            .criterionName(
                                    catalogCriterion.getName())
                            .criterionId(
                                    catalogCriterion.getId())
                            .build();

            inspectionRequest
                    .getCriteria()
                    .add(criterion);
        }

        /*
         * 7.6. Kiểm tra phạm vi công nhận của đơn vị kiểm nghiệm
         * (NCL-11-CN-006 Phase 2).
         *
         * Chỉ áp dụng khi yêu cầu chọn đơn vị từ danh mục dùng chung
         * (testingUnitId != null). Nếu đơn vị có phạm vi công nhận được
         * cấu hình (VT-01) và có chỉ tiêu được chọn nằm NGOÀI phạm vi,
         * hệ thống KHÔNG chặn tạo yêu cầu mà chỉ đánh dấu cảnh báo để
         * người kiểm định biết kết quả sẽ không được tự động công nhận.
         *
         * Lưu ý: nếu đơn vị chưa được cấu hình phạm vi (danh sách rỗng),
         * không phát sinh cảnh báo để tránh nhiễu cho dữ liệu Phase 1.
         */
        if (request.getTestingUnitId() != null) {

            List<AccreditationScope> scopes =
                    accreditationScopeRepository
                            .findByTestingUnitIdWithCriterion(
                                    request.getTestingUnitId());

            if (!scopes.isEmpty()) {

                Set<Long> accreditedIds = scopes.stream()
                        .map(scope -> scope.getCriterion().getId())
                        .collect(Collectors.toSet());

                List<String> outOfScopeNames = requestedCriterionNames.entrySet()
                        .stream()
                        .filter(entry -> !accreditedIds.contains(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .toList();

                if (!outOfScopeNames.isEmpty()) {
                    inspectionRequest.setScopeWarning(Boolean.TRUE);
                    inspectionRequest.setScopeWarningDetails(
                            String.join(", ", outOfScopeNames));
                }
            }
        }

        /*
         * 8. Kiểm tra duplicate với request PENDING_RESULT.
         *
         * Chỉ kiểm tra khi frontend chưa xác nhận
         * muốn tạo request trùng.
         */
        if (!Boolean.TRUE.equals(
                request.getConfirmDuplicate())) {

            List<InspectionRequest> pendingRequests =
                    inspectionRequestRepository
                            .findByProductionLot_IdAndStatus(
                                    lot.getId(),
                                    InspectionRequestStatus.PENDING_RESULT);

            InspectionRequest duplicateRequest =
                    pendingRequests.stream()
                            .filter(existingRequest ->
                                    isDuplicateCriteria(
                                            existingRequest,
                                            requestedCriteriaKeys))
                            .findFirst()
                            .orElse(null);

            /*
             * Đã tồn tại request cùng bộ criterion.
             */
            if (duplicateRequest != null) {

                throw new DuplicateInspectionRequestException(
                        duplicateRequest.getId());
            }
        }

        /*
         * 9. Lưu request.
         */
        InspectionRequest saved =
                inspectionRequestRepository.save(
                        inspectionRequest);

        /*
         * 10. Ghi nhật ký hoạt động (TASK-27): tạo yêu cầu kiểm nghiệm.
         */
        publishActivityLog(
                currentUser,
                "CREATE_INSPECTION_REQUEST",
                "Tạo yêu cầu kiểm nghiệm cho lô " + lot.getName()
                        + " tại đơn vị '" + testingUnit + "' với "
                        + saved.getCriteria().size() + " chỉ tiêu",
                "INSPECTION_REQUEST",
                saved.getId().toString());

        /*
         * 11. Mapping entity -> response.
         */
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionLotTestCriteriaResponse getTestCriteria(
            UUID lotId,
            CustomUserDetails currentUser) {

        ProductionLot lot = productionLotRepository
                .findByIdAndOrganization_OrganizationId(
                        lotId,
                        currentUser.getOrganizationId())
                .orElseThrow(() ->
                        new BusinessException(
                                MSG_LOT_NOT_FOUND));

        validateLot(lot);

        /*
         * Thông tin tiêu chuẩn (nếu có) vẫn lấy từ chứng nhận đầu tiên
         * của lô — chỉ mang tính hiển thị, không còn quyết định
         * bộ chỉ tiêu áp dụng.
         */
        List<ProductionLotCertification> certifications =
                productionLotCertificationRepository
                        .findByProductionLotId(
                                lot.getId());

        Standard standard =
                !certifications.isEmpty()
                        && certifications.get(0).getCertification() != null
                        ? certifications
                        .get(0)
                        .getCertification()
                        .getStandard()
                        : null;

        /*
         * NCL-09-CN-009: bộ chỉ tiêu áp dụng cho lô lấy từ cấu hình
         * của loại nông sản (chỉ tiêu ACTIVE được gán cho category),
         * thay vì từ InspectionCriterionDefinition theo tiêu chuẩn cũ.
         */
        List<CategoryCriterion> assignments =
                categoryCriterionRepository
                        .findByCategoryIdAndCriteriaStatus(
                                lot.getProductCategory().getId(),
                                "ACTIVE");

        return ProductionLotTestCriteriaResponse.builder()
                .lotId(lot.getId())
                .standardId(
                        standard != null
                                ? standard.getId()
                                : null)
                .standardName(
                        standard != null
                                ? standard.getName()
                                : null)
                .criteria(
                        assignments.stream()
                                .map(CategoryCriterion::getCriterion)
                                .map(catalogCriterion ->
                                        ProductionLotTestCriteriaResponse
                                                .TestCriterionItemResponse
                                                .builder()
                                                .criteriaId(
                                                        catalogCriterion.getId())
                                                .code(
                                                        catalogCriterion.getName())
                                                .name(
                                                        catalogCriterion.getName())
                                                .build())
                                .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InspectionRequestListResponse> getInspectionRequests(
            UUID lotId,
            InspectionRequestStatus status,
            Pageable pageable,
            CustomUserDetails currentUser) {

        /*
         * Nếu có lotId thì kiểm tra lot thuộc organization
         * hiện tại và validate lot.
         */
        if (lotId != null) {

            ProductionLot lot =
                    productionLotRepository
                            .findByIdAndOrganization_OrganizationId(
                                    lotId,
                                    currentUser
                                            .getOrganizationId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            MSG_LOT_NOT_FOUND));

            validateLot(lot);
        }

        Page<InspectionRequest> result;

        /*
         * Không filter theo lot.
         *
         * Luôn scope theo organization hiện tại để ngăn
         * VT-02 đọc yêu cầu kiểm nghiệm của tổ chức khác.
         */
        if (lotId == null) {

            result = status == null
                    ? inspectionRequestRepository
                    .findByProductionLot_Organization_OrganizationId(
                            currentUser
                                    .getOrganizationId(),
                            pageable)
                    : inspectionRequestRepository
                    .findByProductionLot_Organization_OrganizationIdAndStatus(
                            currentUser
                                    .getOrganizationId(),
                            status,
                            pageable);

            /*
             * Có lot nhưng không filter status.
             */
        } else if (status == null) {

            result =
                    inspectionRequestRepository
                            .findByProductionLot_Id(
                                    lotId,
                                    pageable);

            /*
             * Có cả lot và status.
             */
        } else {

            result =
                    inspectionRequestRepository
                            .findByProductionLot_IdAndStatus(
                                    lotId,
                                    status,
                                    pageable);
        }

        /*
         * Đếm số chỉ tiêu không đạt cho toàn bộ trang kết quả
         * bằng một truy vấn nhóm duy nhất (tránh N+1).
         */
        Map<UUID, Long> failedCountByRequestId =
                countFailedByRequestIds(result.getContent());

        return result.map(request -> {
            int criteriaCount =
                    request.getCriteria() == null
                            ? 0
                            : request.getCriteria()
                            .size();
            int failedCriteriaCount = failedCountByRequestId
                    .getOrDefault(request.getId(), 0L)
                    .intValue();

            return InspectionRequestListResponse.builder()
                    .testRequestId(
                            request.getId())
                    .lotCode(
                            request.getProductionLot() != null
                                    ? request.getProductionLot()
                                    .getName()
                                    : null)
                    .status(
                            mapStatus(
                                    request.getStatus()))
                    .testingUnit(
                            request.getInspectionUnit())
                    .sampleSentDate(
                            request.getSampleSentDate())
                    .criteriaCount(
                            criteriaCount)
                    .failedCriteriaCount(
                            failedCriteriaCount)
                    .failedRatio(
                            computeFailedRatio(
                                    failedCriteriaCount,
                                    criteriaCount))
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionRequestDetailResponse getDetail(
            UUID requestId,
            CustomUserDetails currentUser) {

        InspectionRequest request = inspectionRequestRepository
                .findDetailById(requestId)
                .orElseThrow(() ->
                        new BusinessException(
                                MSG_REQUEST_NOT_FOUND));

        /*
         * Org boundary: yêu cầu kiểm nghiệm phải thuộc tổ chức
         * hiện tại của người dùng.
         */
        if (request.getProductionLot() == null
                || request.getProductionLot().getOrganization() == null
                || !request.getProductionLot().getOrganization()
                        .getOrganizationId()
                        .equals(currentUser.getOrganizationId())) {

            throw new BusinessException(
                    MSG_REQUEST_NOT_FOUND);
        }

        Map<UUID, InspectionCriterionResult> resultByCriterionId =
                inspectionCriterionResultRepository
                        .findByInspectionCriterion_InspectionRequest_Id(
                                requestId)
                        .stream()
                        .collect(Collectors.toMap(
                                r -> r.getInspectionCriterion().getId(),
                                r -> r));

        List<InspectionRequestDetailCriterionResponse> criteria =
                request.getCriteria()
                        .stream()
                        .map(c ->
                                InspectionRequestDetailCriterionResponse
                                        .builder()
                                        .criterionId(
                                                c.getId())
                                        .criterionDefinitionId(
                                                c.getCriterionId())
                                        .code(
                                                c.getCriterionCode())
                                        .name(
                                                c.getCriterionName())
                                        .standardName(
                                                c.getStandard() != null
                                                        ? c.getStandard()
                                                        .getName()
                                                        : null)
                                        .result(
                                                toResultResponse(
                                                        resultByCriterionId
                                                                .get(
                                                                        c.getId())))
                                        .build())
                        .toList();

        /*
         * Thống kê tổng hợp kết quả kiểm nghiệm:
         * - Tổng số chỉ tiêu của yêu cầu.
         * - Số chỉ tiêu đã có kết quả / đạt / không đạt.
         * - Tỷ lệ không đạt (%) trên TỔNG số chỉ tiêu.
         *
         * Lưu ý: chỉ tiêu chưa có kết quả không được tính vào
         * evaluated/passed/failed; kết quả hết hạn nhưng passed = true
         * vẫn tính là ĐẠT ở đây (khác với logic can-activate-seal).
         */
        int totalCriteria = criteria.size();
        int evaluatedCriteria = 0;
        int passedCriteria = 0;
        int failedCriteriaCount = 0;

        for (InspectionRequestDetailCriterionResponse criterion : criteria) {
            if (criterion.getResult() != null) {
                evaluatedCriteria++;
                if (Boolean.TRUE.equals(
                        criterion.getResult().getPassed())) {
                    passedCriteria++;
                } else {
                    failedCriteriaCount++;
                }
            }
        }

        return InspectionRequestDetailResponse.builder()
                .testRequestId(
                        request.getId())
                .lotId(
                        request.getProductionLot()
                                .getId())
                .lotCode(
                        request.getProductionLot()
                                .getName())
                .status(
                        mapStatus(
                                request.getStatus()))
                .testingUnit(
                        request.getInspectionUnit())
                .sampleSentDate(
                        request.getSampleSentDate())
                .totalCriteria(
                        totalCriteria)
                .evaluatedCriteria(
                        evaluatedCriteria)
                .passedCriteria(
                        passedCriteria)
                .failedCriteriaCount(
                        failedCriteriaCount)
                .failedRatio(
                        computeFailedRatio(
                                failedCriteriaCount,
                                totalCriteria))
                .criteria(
                        criteria)
                .build();
    }

    /**
     * Chuyển kết quả kiểm nghiệm thành DTO (null nếu chưa có kết quả).
     */
    private InspectionCriterionResultResponse toResultResponse(
            InspectionCriterionResult result) {

        if (result == null) {
            return null;
        }

        return InspectionCriterionResultResponse.builder()
                .resultId(
                        result.getId().toString())
                .criterionId(
                        result.getInspectionCriterion()
                                .getId().toString())
                .criterionDefinitionId(
                        result.getInspectionCriterion()
                                .getCriterionId())
                .criterionCode(
                        result.getInspectionCriterion()
                                .getCriterionCode())
                .criterionName(
                        result.getInspectionCriterion()
                                .getCriterionName())
                .resultDate(
                        result.getResultDate())
                .expiryDate(
                        result.getExpiryDate())
                .passed(
                        result.getPassed())
                .filePath(
                        result.getFilePath())
                .createdByName(
                        result.getCreatedBy() != null
                                && result.getCreatedBy()
                                .getFullName() != null
                                ? result.getCreatedBy()
                                .getFullName()
                                : null)
                .createdAt(
                        result.getCreatedAt())
                .updatedAt(
                        result.getUpdatedAt())
                .build();
    }

    /**
     * Kiểm tra quyền tạo yêu cầu kiểm nghiệm.
     */
    private void validatePermission(
            CustomUserDetails currentUser) {

        if (!MANAGER_ROLE.equals(
                currentUser.getRoleCode())) {

            throw new BusinessException(
                    MSG_NO_PERMISSION);
        }
    }

    /**
     * Kiểm tra điều kiện của lô sản xuất.
     *
     * Điều kiện:
     * - Lot phải từ APPROVED trở lên.
     * - Không được REJECTED.
     * - Không được hủy (CANCELLED) — NCL-02-CN-006.
     * - Phải có HARVEST event.
     */
    private void validateLot(
            ProductionLot lot) {

        if (lot.getStatus() == null
                || lot.getStatus()
                == ProductionLotStatus.REJECTED
                || lot.getStatus().ordinal()
                < ProductionLotStatus.APPROVED.ordinal()) {

            throw new BusinessException(
                    MSG_LOT_NOT_APPROVED);
        }

        if (lot.getStatus() == ProductionLotStatus.CANCELLED) {

            throw new BusinessException(
                    MSG_LOT_CANCELLED);
        }

        boolean hasHarvest =
                chainEventRepository
                        .existsByProductionLotIdOrUnassignedEventDataAndEventType(
                                lot.getId(),
                                lot.getId().toString(),
                                ChainEventType.HARVEST);

        if (!hasHarvest) {

            throw new BusinessException(
                    MSG_NO_HARVEST);
        }
    }

    /**
     * Kiểm tra hai request có cùng chính xác
     * một bộ chỉ tiêu hay không.
     *
     * Không quan tâm thứ tự.
     *
     * Ví dụ:
     *
     * [RESIDUE_PESTICIDE, HEAVY_METAL]
     *
     * và
     *
     * [HEAVY_METAL, RESIDUE_PESTICIDE]
     *
     * được xem là cùng một bộ.
     */
    private boolean isDuplicateCriteria(
            InspectionRequest existingRequest,
            Set<String> requestedCriteriaKeys) {

        if (existingRequest == null
                || existingRequest.getCriteria() == null) {

            return false;
        }

        Set<String> existingCriteriaKeys =
                existingRequest
                        .getCriteria()
                        .stream()
                        .filter(c ->
                                c != null
                                        && c.getCriterionCode() != null)
                        .map(this::resolveCriterionKey)
                        .collect(Collectors.toSet());

        return existingCriteriaKeys.equals(
                requestedCriteriaKeys);
    }

    /**
     * Lấy khóa criterion từ InspectionCriterion hiện tại.
     *
     * Identity của chỉ tiêu dựa trên criterionId (không phải name).
     * Hai chỉ tiêu khác ID nhưng cùng tên KHÔNG bị coi là trùng.
     *
     * Scope theo nguồn gốc dữ liệu để không trùng khóa giữa:
     * - Dữ liệu mới (NCL-09-CN-009): tham chiếu danh mục dùng chung
     *   qua criterion_id → "CAT:{criterionId}".
     * - Dữ liệu legacy (criterion_id null, có Standard):
     *   "{standardId}:{criterionCode}" — fallback cho dữ liệu cũ.
     *
     * Khóa phải đồng nhất với khóa tạo trong createInspectionRequest().
     */
    private String resolveCriterionKey(
            InspectionCriterion criterion) {

        if (criterion.getCriterionId() != null) {
            return "CAT:" + criterion.getCriterionId();
        }

        if (criterion.getStandard() != null) {
            return criterion.getStandard().getId().toString()
                    + ":" + criterion.getCriterionCode();
        }

        return "LEGACY:" + criterion.getCriterionCode();
    }

    /**
     * Ghi nhật ký hoạt động theo convention của hệ thống (TASK-27).
     * <p>
     * Actor lấy từ người dùng đã xác thực trong security context,
     * organization lấy từ organization của người thực hiện.
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
     * Mapping entity -> response.
     */
    private InspectionRequestResponse toResponse(
            InspectionRequest request) {

        if (request == null) {
            throw new IllegalStateException(
                    "InspectionRequestRepository.save() không được trả về null.");
        }

        return InspectionRequestResponse.builder()
                .testRequestId(
                        request.getId())
                .lotId(
                        request.getProductionLot()
                                .getId())
                .lotCode(
                        request.getProductionLot()
                                .getName())
                .testingUnit(
                        request.getInspectionUnit())
                .testingUnitId(
                        request.getTestingUnitId())
                .hasScopeWarning(
                        Boolean.TRUE.equals(
                                request.getScopeWarning())
                                ? Boolean.TRUE
                                : Boolean.FALSE)
                .scopeWarningDetails(
                        request.getScopeWarningDetails())
                .sampleSentDate(
                        request.getSampleSentDate())
                .status(
                        mapStatus(
                                request.getStatus()))
                .createdBy(
                        request.getCreatedBy() != null
                                && request.getCreatedBy()
                                .getFullName() != null
                                ? request.getCreatedBy()
                                .getFullName()
                                : "")
                .createdAt(
                        request.getCreatedAt())
                .criteria(
                        request.getCriteria()
                                .stream()
                                .map(c ->
                                        InspectionCriterionResponse
                                                .builder()
                                                .criteriaId(
                                                        null)
                                                .code(
                                                        c.getCriterionCode())
                                                .name(
                                                        c.getCriterionName())
                                                .standardId(
                                                        c.getStandard() != null
                                                                ? c.getStandard()
                                                                .getId()
                                                                : null)
                                                .standardName(
                                                        c.getStandard() != null
                                                                ? c.getStandard()
                                                                .getName()
                                                                : null)
                                                .build())
                                .toList())
                .build();
    }

    /**
     * Mapping trạng thái domain -> trạng thái response.
     *
     * PENDING_RESULT -> PENDING
     * PASSED         -> PASSED
     * FAILED         -> FAILED
     * CANCELLED      -> CANCELLED
     */
    private String mapStatus(
            InspectionRequestStatus status) {

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
     * Đếm số chỉ tiêu không đạt (passed = false) cho từng yêu cầu
     * kiểm nghiệm trong danh sách, bằng một truy vấn nhóm duy nhất
     * để tránh N+1 khi map trang danh sách.
     *
     * @param requests Danh sách yêu cầu kiểm nghiệm.
     * @return Map giữa requestId và số chỉ tiêu không đạt.
     */
    private Map<UUID, Long> countFailedByRequestIds(
            List<InspectionRequest> requests) {

        List<UUID> requestIds = requests.stream()
                .map(InspectionRequest::getId)
                .toList();

        if (requestIds.isEmpty()) {
            return Map.of();
        }

        return inspectionCriterionResultRepository
                .countFailedCriteriaByRequestIds(requestIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]));
    }

    /**
     * Tính tỷ lệ phần trăm chỉ tiêu không đạt trên TỔNG số chỉ tiêu,
     * làm tròn 1 chữ số thập phân.
     *
     * Trả về 0.0 khi tổng số chỉ tiêu là 0 (tránh chia cho 0).
     *
     * @param failedCriteriaCount Số chỉ tiêu không đạt.
     * @param totalCriteria Tổng số chỉ tiêu của yêu cầu.
     * @return Tỷ lệ không đạt theo %, ví dụ 40.0.
     */
    private double computeFailedRatio(
            int failedCriteriaCount,
            int totalCriteria) {

        if (totalCriteria <= 0) {
            return 0.0;
        }

        return Math.round(
                failedCriteriaCount * 1000.0 / totalCriteria)
                / 10.0;
    }
}