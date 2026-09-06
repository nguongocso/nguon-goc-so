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
 *
 * <p>
 * Logic tính "kết quả mới nhất theo mã chỉ tiêu trên toàn bộ yêu cầu
 * kiểm nghiệm của lô" đồng bộ với {@code checkCanActivateSeal} (QTN-21
 * pre-check) để đảm bảo cùng một nguồn sự thật (quyết định thiết kế
 * D-8 của tài liệu {@code docs/api/certification/failed-lot-handling.md}).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InspectionEligibilityServiceImpl
        implements InspectionEligibilityService {

    private final InspectionCriterionResultRepository resultRepository;
    private final CategoryCriterionRepository categoryCriterionRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final Clock clock;

    private static final String MSG_INSPECTION_FAILED =
            "Lô sản xuất chưa đạt kiểm nghiệm, không thể tạo lô hàng.";

    private static final String MSG_INSPECTION_PENDING =
            "Lô sản xuất đang chờ kết quả kiểm nghiệm, không thể tạo lô hàng.";

    private static final String MSG_INSPECTION_EXPIRED =
            "Kết quả kiểm nghiệm đã hết hiệu lực, không thể tạo lô hàng.";

    private static final String MSG_INSPECTION_MISSING =
            "Lô sản xuất chưa có kết quả kiểm nghiệm đạt cho tất cả chỉ tiêu, không thể tạo lô hàng.";

    @Override
    public InspectionEligibilityResult evaluateForShipment(ProductionLot lot) {

        /*
         * NCL-09-CN-009: loại nông sản KHÔNG bắt buộc kiểm nghiệm thì
         * luôn đủ điều kiện — không phụ thuộc kết quả kiểm nghiệm.
         */
        boolean mandatoryInspection =
                lot.getProductCategory() != null
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

        /*
         * Bắt buộc kiểm nghiệm: điều kiện xét trên TỔNG BỘ CHỈ TIÊU
         * ACTIVE được gán cho loại nông sản của lô (cùng nguồn dữ liệu
         * với GET /production-lots/{lotId}/test-criteria).
         */
        List<CategoryCriterion> assignments =
                categoryCriterionRepository
                        .findByCategoryIdAndCriteriaStatus(
                                lot.getProductCategory().getId(),
                                "ACTIVE");

        int totalCriteria = assignments.size();

        /*
         * Chưa cấu hình chỉ tiêu cho loại nông sản bắt buộc kiểm nghiệm
         * → lô không thể có kết quả hợp lệ cho mọi chỉ tiêu.
         */
        if (totalCriteria == 0) {
            return buildBlocked(
                    InspectionBlockReasonCode.INSPECTION_MISSING,
                    MSG_INSPECTION_MISSING,
                    0, 0, 0, null);
        }

        // Kết quả mới nhất theo mã chỉ tiêu trên toàn bộ yêu cầu của lô
        Map<String, InspectionCriterionResult> latestByCode =
                buildLatestResultsByCode(lot.getId());

        // Kiểm tra có chỉ tiêu nào có kết quả Không đạt (passed=false) không
        // — những kết quả này có resultDate=null nên không nằm trong latestByCode.
        boolean hasAnyFailedCriterionResult =
                resultRepository.findAllByProductionLotId(lot.getId())
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

            /*
             * Kết quả KHÔNG ĐẠT (passed = false, resultDate/expiryDate null)
             * bị loại khỏi map "latest" (cùng logic checkCanActivateSeal).
             * hasAnyFailedCriterionResult được tính riêng ở trên để phân biệt
             * "kết luận Không đạt" so với "chưa có kết quả".
             */
            if (latest == null) {
                continue;
            }

            if (Boolean.TRUE.equals(latest.getPassed())) {
                // Chỉ tiêu đạt luôn có expiryDate khác null
                // (validate ở tầng service khi ghi kết quả)
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

        /*
         * Thứ tự ưu tiên khi trùng nhiều tình trạng (QTN-30 §5.1):
         * INSPECTION_PENDING → INSPECTION_FAILED →
         * INSPECTION_EXPIRED → INSPECTION_MISSING.
         *
         * INSPECTION_PENDING được ưu tiên hơn INSPECTION_FAILED vì
         * khi đang có yêu cầu kiểm nghiệm lại chờ kết quả, thông báo
         * "đang chờ kết quả" phản ánh đúng trạng thái thực tế và
         * actionable hơn "không đạt".
         *
         * INSPECTION_FAILED = lô từng bị kết luận Không đạt (kết luận
         * hoàn thành mới nhất là FAILED) HOẶC còn chỉ tiêu có kết quả
         * mới nhất là Không đạt.
         */
        boolean hasPendingRequest =
                inspectionRequestRepository
                        .existsByProductionLot_IdAndStatus(
                                lot.getId(),
                                InspectionRequestStatus.PENDING_RESULT);

        boolean latestConclusionFailed =
                hasLatestFailedConclusion(lot);

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

    @Override
    public boolean hasLatestFailedConclusion(ProductionLot lot) {

        List<InspectionRequest> requests =
                inspectionRequestRepository
                        .findByProductionLot_IdOrderByCreatedAtDesc(
                                lot.getId());

        /*
         * Kết luận mới nhất = yêu cầu hoàn thành (PASSED/FAILED) mới nhất
         * theo thời gian tạo. Yêu cầu PENDING_RESULT / CANCELLED không
         * phải kết luận nên bị bỏ qua.
         */
        return requests.stream()
                .filter(request -> request.getStatus() != null)
                .filter(request -> request.getStatus()
                        == InspectionRequestStatus.PASSED
                        || request.getStatus()
                        == InspectionRequestStatus.FAILED)
                .findFirst()
                .map(request ->
                        request.getStatus()
                                == InspectionRequestStatus.FAILED)
                .orElse(false);
    }

    /**
     * Chọn kết quả MỚI NHẤT theo từng mã chỉ tiêu trên toàn bộ yêu cầu
     * kiểm nghiệm của lô — cùng logic {@code checkCanActivateSeal}:
     * mới hơn theo {@code resultDate}, phụ theo {@code updatedAt}.
     *
     * <p>
     * Kết quả Không đạt (resultDate null) bị bỏ qua khi so sánh ngày,
     * đúng như hành vi hiện có của QTN-21 pre-check.
     * </p>
     */
    private Map<String, InspectionCriterionResult> buildLatestResultsByCode(
            UUID lotId) {

        Map<String, InspectionCriterionResult> latestByCode =
                new HashMap<>();

        for (InspectionCriterionResult result : resultRepository
                .findAllByProductionLotId(lotId)) {

            // Bỏ qua kết quả không đạt — resultDate/expiryDate là null
            if (result.getResultDate() == null) {
                continue;
            }

            String code = result.getInspectionCriterion()
                    .getCriterionCode();
            InspectionCriterionResult current = latestByCode.get(code);
            LocalDateTime resultUpdatedAt = result.getUpdatedAt();
            LocalDateTime currentUpdatedAt =
                    current == null ? null : current.getUpdatedAt();

            boolean isNewer =
                    current == null
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
     * Dựng kết quả bị chặn với thống kê đi kèm.
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
