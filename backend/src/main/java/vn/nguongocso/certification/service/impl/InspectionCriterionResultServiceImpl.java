package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.farm.repository.ProductionLotRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai service cho kết quả kiểm nghiệm.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InspectionCriterionResultServiceImpl
        implements InspectionCriterionResultService {

    private final InspectionCriterionResultRepository resultRepository;
    private final InspectionCriterionRepository criterionRepository;
    private final InspectionRequestRepository requestRepository;
    private final ProductionLotRepository lotRepository;

    @Override
    public InspectionCriterionResultResponse recordOrUpdateResult(
            String criterionId,
            InspectionCriterionResultRequest request,
            CustomUserDetails currentUser) {

        // Kiểm tra tồn tại chỉ tiêu
        UUID criterionUUID = UUID.fromString(criterionId);
        InspectionCriterion criterion = criterionRepository
                .findById(criterionUUID)
                .orElseThrow(() ->
                    new IllegalArgumentException("Chỉ tiêu kiểm nghiệm không tồn tại"));

        // Kiểm tra yêu cầu kiểm nghiệm ở trạng thái PENDING_RESULT
        InspectionRequest inspectionRequest = criterion.getInspectionRequest();
        if (!inspectionRequest.getStatus().equals(InspectionRequestStatus.PENDING_RESULT)) {
            throw new IllegalStateException(
                    "Yêu cầu kiểm nghiệm phải ở trạng thái chờ kết quả");
        }

        // Validate ngày cấp và ngày hết hiệu lực
        if (request.getExpiryDate().isBefore(request.getResultDate())) {
            throw new IllegalArgumentException(
                    "Ngày hết hiệu lực phải sau ngày cấp");
        }

        // Kiểm tra xem ngày hết hiệu lực có >= hôm nay không
        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Ngày hết hiệu lực phải >= ngày hiện tại");
        }

        // Tìm hoặc tạo kết quả kiểm nghiệm
        InspectionCriterionResult result = resultRepository
                .findByCriterion_Id(criterionUUID)
                .orElse(InspectionCriterionResult.builder()
                        .inspectionCriterion(criterion)
                        .build());

        // Cập nhật thông tin kết quả
        result.setResultDate(request.getResultDate());
        result.setExpiryDate(request.getExpiryDate());
        result.setPassed(request.getPassed());
        result.setFilePath(request.getFilePath());
        result.setCreatedBy(
                currentUser.getUser());

        // Lưu kết quả
        result = resultRepository.save(result);

        // Cập nhật trạng thái yêu cầu kiểm nghiệm nếu tất cả chỉ tiêu đều đạt
        checkAndUpdateRequestStatus(inspectionRequest);

        return toResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InspectionCriterionResultResponse> getResultsByRequest(
            UUID inspectionRequestId) {

        return resultRepository
                .findByCriterion_InspectionRequest_Id(inspectionRequestId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionCriterionResultResponse getResultByCriterion(String criterionId) {

        UUID criterionUUID = UUID.fromString(criterionId);
        InspectionCriterionResult result = resultRepository
                .findByCriterion_Id(criterionUUID)
                .orElseThrow(() ->
                    new IllegalArgumentException("Kết quả kiểm nghiệm không tồn tại"));

        return toResponse(result);
    }

    @Override
    public void deleteResult(String resultId) {

        UUID resultUUID = UUID.fromString(resultId);
        InspectionCriterionResult result = resultRepository
                .findById(resultUUID)
                .orElseThrow(() ->
                    new IllegalArgumentException("Kết quả kiểm nghiệm không tồn tại"));

        InspectionRequest inspectionRequest =
                result.getInspectionCriterion().getInspectionRequest();

        resultRepository.delete(result);

        // Cập nhật trạng thái yêu cầu sau khi xóa
        checkAndUpdateRequestStatus(inspectionRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public CanActivateSealCheckResponse checkCanActivateSeal(
            UUID productionLotId,
            CustomUserDetails currentUser) {

        // Kiểm tra xem lô sản xuất có tồn tại không
        if (!lotRepository.existsById(productionLotId)) {
            throw new IllegalArgumentException("Lô sản xuất không tồn tại");
        }

        // Lấy danh sách yêu cầu kiểm nghiệm của lô
        List<InspectionRequest> requests = requestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(productionLotId);

        // Nếu không có yêu cầu kiểm nghiệm, lô có thể kích hoạt tem
        if (requests.isEmpty()) {
            return CanActivateSealCheckResponse.builder()
                    .productionLotId(productionLotId.toString())
                    .canActivate(true)
                    .reason(null)
                    .earliestExpiryDate(null)
                    .totalCriteria(0)
                    .passedCriteria(0)
                    .failedOrExpiredCriteria(0)
                    .build();
        }

        // Kiểm tra tất cả yêu cầu kiểm nghiệm
        LocalDate today = LocalDate.now();
        boolean canActivate = true;
        String reason = null;
        LocalDate earliestExpiry = null;
        int totalCriteria = 0;
        int passedCriteria = 0;

        for (InspectionRequest request : requests) {
            int requestTotal = resultRepository.countTotalCriteria(request.getId());
            int requestPassed = resultRepository
                    .countPassedAndValidCriteria(request.getId(), today);

            totalCriteria += requestTotal;
            passedCriteria += requestPassed;

            // Kiểm tra nếu không tất cả chỉ tiêu đều đạt
            if (requestTotal > 0 && requestTotal != requestPassed) {
                canActivate = false;
                reason = "Lô chưa có kết quả kiểm nghiệm đạt cho tất cả chỉ tiêu";
            }

            // Lấy ngày hết hiệu lực sớm nhất
            var expiryDate = resultRepository
                    .findEarliestExpiryDateByInspectionRequest(request.getId());
            if (expiryDate.isPresent()) {
                if (expiryDate.get().isBefore(today)) {
                    canActivate = false;
                    reason = "Kết quả kiểm nghiệm đã quá hạn";
                }

                if (earliestExpiry == null || expiryDate.get().isBefore(earliestExpiry)) {
                    earliestExpiry = expiryDate.get();
                }
            }
        }

        int failedOrExpired = totalCriteria - passedCriteria;

        return CanActivateSealCheckResponse.builder()
                .productionLotId(productionLotId.toString())
                .canActivate(canActivate)
                .reason(reason)
                .earliestExpiryDate(earliestExpiry)
                .totalCriteria(totalCriteria)
                .passedCriteria(passedCriteria)
                .failedOrExpiredCriteria(failedOrExpired)
                .build();
    }

    /**
     * Kiểm tra và cập nhật trạng thái yêu cầu kiểm nghiệm.
     * Nếu tất cả chỉ tiêu đều đạt, cập nhật trạng thái thành PASSED.
     */
    private void checkAndUpdateRequestStatus(InspectionRequest inspectionRequest) {

        boolean allPassed = resultRepository
                .areAllCriteriaPassedAndValid(
                        inspectionRequest.getId(),
                        LocalDate.now());

        if (allPassed && inspectionRequest.getCriteria().size() > 0) {
            inspectionRequest.setStatus(InspectionRequestStatus.PASSED);
            requestRepository.save(inspectionRequest);
        }
    }

    /**
     * Chuyển đổi entity thành DTO.
     */
    private InspectionCriterionResultResponse toResponse(
            InspectionCriterionResult result) {

        return InspectionCriterionResultResponse.builder()
                .resultId(result.getId().toString())
                .criterionId(
                        result.getInspectionCriterion().getId().toString())
                .criterionCode(
                        result.getInspectionCriterion().getCriterionCode())
                .criterionName(
                        result.getInspectionCriterion().getCriterionName())
                .resultDate(result.getResultDate())
                .expiryDate(result.getExpiryDate())
                .passed(result.getPassed())
                .filePath(result.getFilePath())
                .createdByName(result.getCreatedBy().getFullName())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
