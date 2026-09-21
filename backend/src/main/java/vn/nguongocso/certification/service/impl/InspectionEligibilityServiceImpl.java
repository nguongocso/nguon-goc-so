package vn.nguongocso.certification.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.certification.dto.response.InspectionEligibilityResult;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionBlockReasonCode;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.InspectionEligibilityService;
import vn.nguongocso.farm.entity.ProductionLot;

/**
 * Triển khai đánh giá điều kiện kiểm nghiệm của lô sản xuất theo QTN-30
 * (NCL-11-CN-005).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionEligibilityServiceImpl implements InspectionEligibilityService {
        private final InspectionCriterionResultRepository resultRepository;
        private final CategoryCriterionRepository categoryCriterionRepository;
        private final InspectionRequestRepository inspectionRequestRepository;
        private final Clock clock;

        private static final String MSG_INSPECTION_FAILED = "Lô sản xuất chưa đạt kiểm nghiệm, không thể tạo lô hàng.";
        private static final String MSG_INSPECTION_PENDING = "Lô sản xuất đang chờ kết quả kiểm nghiệm, không thể tạo lô hàng.";
        private static final String MSG_INSPECTION_EXPIRED = "Kết quả kiểm nghiệm đã hết hiệu lực, không thể tạo lô hàng.";
        private static final String MSG_INSPECTION_MISSING = "Lô sản xuất chưa có kết quả kiểm nghiệm đạt cho tất cả chỉ tiêu, không thể tạo lô hàng.";

        /**
         * Đánh giá lô sản xuất có đủ điều kiện kiểm nghiệm để tạo lô hàng hoặc kích
         * hoạt tem hay không.
         */
        @Override
        public InspectionEligibilityResult evaluateForShipment(ProductionLot lot) {
                boolean mandatoryInspection = lot.getProductCategory() != null
                                && Boolean.TRUE.equals(
                                                lot.getProductCategory().getRequiresInspection());

                if (!mandatoryInspection) {
                        return InspectionEligibilityResult.builder()
                                        .eligible(true)
                                        .reasonCode(null)
                                        .message(null)
                                        .totalCriteria(0)
                                        .passedCriteria(0)
                                        .failedOrExpiredCriteria(0)
                                        .earliestExpiryDate(null)
                                        .build();
                }

                List<CategoryCriterion> assignments = categoryCriterionRepository
                                .findByCategoryIdAndCriteriaStatus(
                                                lot.getProductCategory().getId(),
                                                "ACTIVE");

                int totalCriteria = assignments.size();

                if (totalCriteria == 0) {
                        return buildBlocked(
                                        InspectionBlockReasonCode.INSPECTION_MISSING,
                                        MSG_INSPECTION_MISSING,
                                        0, 0, 0, null);
                }

                Map<String, InspectionCriterionResult> latestByCode = buildLatestResultsByCode(lot.getId());

                boolean hasAnyFailedCriterionResult = resultRepository.findAllByProductionLotId(lot.getId())
                                .stream()
                                .anyMatch(r -> Boolean.FALSE.equals(r.getPassed()));

                LocalDate today = LocalDate.now(clock);

                int passedCriteria = 0;
                boolean hasFailedResult = false;
                boolean hasExpiredResult = false;
                LocalDate earliestExpiry = null;

                for (CategoryCriterion assignment : assignments) {
                        String code = assignment.getCriterion().getName();
                        InspectionCriterionResult latest = latestByCode.get(code);
                        if (latest == null) {
                                continue;
                        }

                        if (Boolean.TRUE.equals(latest.getPassed())) {
                                if (latest.getExpiryDate() != null
                                                && (earliestExpiry == null
                                                                || latest.getExpiryDate().isBefore(earliestExpiry))) {
                                        earliestExpiry = latest.getExpiryDate();
                                }

                                if (latest.getExpiryDate() == null
                                                || latest.getExpiryDate().isBefore(today)) {
                                        hasExpiredResult = true;
                                        continue;
                                }

                                passedCriteria++;
                        } else {
                                hasFailedResult = true;
                        }
                }

                if (passedCriteria == totalCriteria) {
                        return InspectionEligibilityResult.builder()
                                        .eligible(true)
                                        .reasonCode(null)
                                        .message(null)
                                        .totalCriteria(totalCriteria)
                                        .passedCriteria(passedCriteria)
                                        .failedOrExpiredCriteria(0)
                                        .earliestExpiryDate(earliestExpiry)
                                        .build();
                }

                boolean hasPendingRequest = inspectionRequestRepository
                                .existsByProductionLot_IdAndStatus(
                                                lot.getId(),
                                                InspectionRequestStatus.PENDING_RESULT);

                boolean latestConclusionFailed = hasLatestFailedConclusion(lot);

                if (hasPendingRequest) {
                        return buildBlocked(
                                        InspectionBlockReasonCode.INSPECTION_PENDING,
                                        MSG_INSPECTION_PENDING,
                                        totalCriteria, passedCriteria,
                                        totalCriteria - passedCriteria, earliestExpiry);
                }

                if (hasFailedResult || hasAnyFailedCriterionResult || latestConclusionFailed) {
                        return buildBlocked(
                                        InspectionBlockReasonCode.INSPECTION_FAILED,
                                        MSG_INSPECTION_FAILED,
                                        totalCriteria, passedCriteria,
                                        totalCriteria - passedCriteria, earliestExpiry);
                }

                if (hasExpiredResult) {
                        return buildBlocked(
                                        InspectionBlockReasonCode.INSPECTION_EXPIRED,
                                        MSG_INSPECTION_EXPIRED,
                                        totalCriteria, passedCriteria,
                                        totalCriteria - passedCriteria, earliestExpiry);
                }

                return buildBlocked(
                                InspectionBlockReasonCode.INSPECTION_MISSING,
                                MSG_INSPECTION_MISSING,
                                totalCriteria, passedCriteria,
                                totalCriteria - passedCriteria, earliestExpiry);
        }

        /**
         * Kiểm tra kết luận kiểm nghiệm hoàn thành mới nhất của lô sản xuất có phải là
         * Không đạt hay không.
         */
        @Override
        public boolean hasLatestFailedConclusion(ProductionLot lot) {
                List<InspectionRequest> requests = inspectionRequestRepository
                                .findByProductionLot_IdOrderByCreatedAtDesc(
                                                lot.getId());
                return requests.stream()
                                .filter(request -> request.getStatus() != null)
                                .filter(request -> request.getStatus() == InspectionRequestStatus.PASSED
                                                || request.getStatus() == InspectionRequestStatus.FAILED)
                                .findFirst()
                                .map(request -> request.getStatus() == InspectionRequestStatus.FAILED)
                                .orElse(false);
        }

        /**
         * Tập hợp kết quả kiểm nghiệm mới nhất theo từng mã chỉ tiêu của lô sản xuất.
         */
        private Map<String, InspectionCriterionResult> buildLatestResultsByCode(
                        UUID lotId) {
                Map<String, InspectionCriterionResult> latestByCode = new HashMap<>();

                for (InspectionCriterionResult result : resultRepository
                                .findAllByProductionLotId(lotId)) {

                        if (result.getResultDate() == null) {
                                continue;
                        }

                        String code = result.getInspectionCriterion()
                                        .getCriterionCode();
                        InspectionCriterionResult current = latestByCode.get(code);
                        LocalDateTime resultUpdatedAt = result.getUpdatedAt();
                        LocalDateTime currentUpdatedAt = current == null ? null : current.getUpdatedAt();

                        boolean isNewer = current == null
                                        || result.getResultDate()
                                                        .isAfter(current.getResultDate())
                                        || (result.getResultDate()
                                                        .isEqual(current.getResultDate())
                                                        && resultUpdatedAt != null
                                                        && (currentUpdatedAt == null
                                                                        || resultUpdatedAt.isAfter(
                                                                                        currentUpdatedAt)));

                        if (isNewer) {
                                latestByCode.put(code, result);
                        }
                }

                return latestByCode;
        }

        /**
         * Dựng kết quả bị chặn kèm theo thông tin chi tiết và thống kê.
         */
        private InspectionEligibilityResult buildBlocked(
                        InspectionBlockReasonCode reasonCode,
                        String message,
                        int totalCriteria,
                        int passedCriteria,
                        int failedOrExpiredCriteria,
                        LocalDate earliestExpiryDate) {

                return InspectionEligibilityResult.builder()
                                .eligible(false)
                                .reasonCode(reasonCode)
                                .message(message)
                                .totalCriteria(totalCriteria)
                                .passedCriteria(passedCriteria)
                                .failedOrExpiredCriteria(failedOrExpiredCriteria)
                                .earliestExpiryDate(earliestExpiryDate)
                                .build();
        }
}
