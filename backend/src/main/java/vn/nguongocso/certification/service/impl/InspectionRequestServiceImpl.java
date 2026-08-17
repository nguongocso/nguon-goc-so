package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestDetailCriterionResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestDetailResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestListResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.dto.response.ProductionLotTestCriteriaResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionDefinition;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.entity.Standard;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionCriterionDefinitionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.service.InspectionRequestService;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductionLotRepository;

import java.time.LocalDate;
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

    private static final String MSG_NO_HARVEST =
            "Lô sản xuất chưa có sự kiện thu hoạch.";

    private static final String MSG_NO_CRITERIA =
            "Phải chọn ít nhất một chỉ tiêu kiểm nghiệm.";

    private static final String MSG_CRITERION_NOT_FOUND =
            "Chỉ tiêu kiểm nghiệm không tồn tại.";

    private static final String MSG_CRITERION_NOT_APPLICABLE =
            "Chỉ tiêu kiểm nghiệm không thuộc tiêu chuẩn đã gắn với lô.";

    private static final String MSG_DUPLICATE_CRITERIA =
            "Danh sách chỉ tiêu kiểm nghiệm không được chứa chỉ tiêu trùng lặp.";

    private static final String MSG_SAMPLE_DATE_FUTURE =
            "Ngày gửi mẫu không được lớn hơn ngày hiện tại.";

    private static final String MSG_INVALID_SAMPLE_DATE =
            "Ngày gửi mẫu không được để trống.";

    private static final String MSG_REQUEST_NOT_FOUND =
            "Yêu cầu kiểm nghiệm không tồn tại.";

    private final ProductionLotRepository productionLotRepository;

    private final InspectionRequestRepository inspectionRequestRepository;

    private final InspectionCriterionDefinitionRepository
            inspectionCriterionDefinitionRepository;

    private final ChainEventRepository chainEventRepository;

    private final ProductionLotCertificationRepository
            productionLotCertificationRepository;

    private final InspectionCriterionResultRepository
            inspectionCriterionResultRepository;

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
         */
        String testingUnit = request.getTestingUnit() == null
                ? ""
                : request.getTestingUnit().trim();

        if (testingUnit.isBlank()) {
            throw new BusinessException(
                    "Đơn vị kiểm nghiệm không được để trống.");
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
        Set<Integer> requestedCriterionIds =
                new HashSet<>();

        for (Integer criteriaId : request.getCriteriaIds()) {

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
                .isAfter(LocalDate.now())) {

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
         * Format:
         *
         *     standardId:criterionCode
         *
         * Ví dụ:
         *
         *     UUID:RESIDUE_PESTICIDE
         *     UUID:HEAVY_METAL
         *
         * Dùng criterionCode vì InspectionCriterion snapshot
         * đang lưu criterionCode thay vì criterionDefinitionId.
         */
        Set<String> requestedCriteriaKeys =
                new HashSet<>();

        /*
         * 7. Xử lý từng criterion.
         */
        for (Integer criteriaId : request.getCriteriaIds()) {

            /*
             * 7.1. Lấy định nghĩa criterion.
             */
            InspectionCriterionDefinition criterionDefinition =
                    inspectionCriterionDefinitionRepository
                            .findById(criteriaId)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            MSG_CRITERION_NOT_FOUND));

            /*
             * 7.2. Lấy Standard.
             */
            Standard standard =
                    criterionDefinition.getStandard();

            if (standard == null) {
                throw new BusinessException(
                        MSG_CRITERION_NOT_APPLICABLE);
            }

            /*
             * 7.3. Kiểm tra Standard đã được gắn
             * vào ProductionLot hay chưa.
             */
            boolean standardAttached =
                    productionLotCertificationRepository
                            .existsByProductionLotIdAndStandardId(
                                    lot.getId(),
                                    standard.getId());

            if (!standardAttached) {
                throw new BusinessException(
                        MSG_CRITERION_NOT_APPLICABLE);
            }

            /*
             * 7.4. Tạo khóa criterion.
             *
             * QUAN TRỌNG:
             * Dùng CODE thay vì ID để đồng nhất với
             * resolveCriterionKey() bên dưới.
             */
            String criterionKey =
                    standard.getId()
                            + ":"
                            + criterionDefinition.getCode();

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
             * Code + name lấy từ DB.
             */
            InspectionCriterion criterion =
                    InspectionCriterion.builder()
                            .inspectionRequest(
                                    inspectionRequest)
                            .standard(standard)
                            .criterionCode(
                                    criterionDefinition.getCode())
                            .criterionName(
                                    criterionDefinition.getName())
                            .build();

            inspectionRequest
                    .getCriteria()
                    .add(criterion);
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
         * 10. Mapping entity -> response.
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

        List<ProductionLotCertification> certifications =
                productionLotCertificationRepository
                        .findByProductionLotId(
                                lot.getId());

        if (certifications.isEmpty()) {

            return ProductionLotTestCriteriaResponse.builder()
                    .lotId(lot.getId())
                    .standardId(null)
                    .standardName(null)
                    .criteria(List.of())
                    .build();
        }

        ProductionLotCertification firstCertification =
                certifications.get(0);

        Standard standard =
                firstCertification.getCertification() != null
                        ? firstCertification
                        .getCertification()
                        .getStandard()
                        : null;

        if (standard == null) {

            return ProductionLotTestCriteriaResponse.builder()
                    .lotId(lot.getId())
                    .standardId(null)
                    .standardName(null)
                    .criteria(List.of())
                    .build();
        }

        List<InspectionCriterionDefinition> definitions =
                inspectionCriterionDefinitionRepository
                        .findByStandard_IdOrderByIdAsc(
                                standard.getId());

        return ProductionLotTestCriteriaResponse.builder()
                .lotId(lot.getId())
                .standardId(standard.getId())
                .standardName(standard.getName())
                .criteria(
                        definitions.stream()
                                .map(def ->
                                        ProductionLotTestCriteriaResponse
                                                .TestCriterionItemResponse
                                                .builder()
                                                .criteriaId(
                                                        def.getId())
                                                .code(
                                                        def.getCode())
                                                .name(
                                                        def.getName())
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

        return result.map(request ->
                InspectionRequestListResponse.builder()
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
                                request.getCriteria() == null
                                        ? 0
                                        : request.getCriteria()
                                        .size())
                        .build());
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
                                        && c.getStandard() != null
                                        && c.getCriterionCode() != null)
                        .map(c ->
                                c.getStandard()
                                        .getId()
                                        + ":"
                                        + resolveCriterionKey(c))
                        .collect(Collectors.toSet());

        return existingCriteriaKeys.equals(
                requestedCriteriaKeys);
    }

    /**
     * Lấy khóa criterion từ InspectionCriterion hiện tại.
     *
     * InspectionCriterion lưu criterionCode,
     * vì vậy sử dụng criterionCode để đối chiếu.
     */
    private String resolveCriterionKey(
            InspectionCriterion criterion) {

        return criterion.getCriterionCode();
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
}