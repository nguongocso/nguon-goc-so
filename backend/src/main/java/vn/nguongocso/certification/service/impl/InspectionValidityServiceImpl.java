package vn.nguongocso.certification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.nguongocso.certification.dto.response.CriterionValidityResponse;
import vn.nguongocso.certification.dto.response.InspectionValidityResponse;
import vn.nguongocso.certification.entity.CategoryCriterion;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionCatalog;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.enums.InspectionValidityStatus;
import vn.nguongocso.certification.repository.CategoryCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.InspectionValidityService;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Triển khai tính toán trạng thái hiệu lực kiểm nghiệm của lô sản xuất (NCL-11-CN-004).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InspectionValidityServiceImpl implements InspectionValidityService {

    private final CategoryCriterionRepository categoryCriterionRepository;
    private final InspectionCriterionResultRepository resultRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final TraceCodeRepository traceCodeRepository;

    @Value("${app.inspection.expiry-warning-threshold-days:15}")
    private int warningThresholdDays;

    @Override
    public InspectionValidityResponse calculateValidity(ProductionLot lot) {
        return calculateValidity(lot, LocalDate.now());
    }

    @Override
    public InspectionValidityResponse calculateValidity(ProductionLot lot, LocalDate today) {
        if (lot == null) {
            return null;
        }

        // 1. Kiểm tra loại nông sản có bắt buộc kiểm nghiệm hay không
        boolean requiresInspection = lot.getProductCategory() != null
                && Boolean.TRUE.equals(lot.getProductCategory().getRequiresInspection());

        Long inactiveStampCount = calculateInactiveStampCount(lot.getId());
        Long totalStamps = calculateTotalStamps(lot.getId());
        boolean canCreateNewRequest = canCreateInspectionRequest(lot);

        if (!requiresInspection) {
            return InspectionValidityResponse.builder()
                    .requiresInspection(false)
                    .status(InspectionValidityStatus.NOT_REQUIRED)
                    .earliestExpiryDate(null)
                    .daysRemaining(null)
                    .daysOverdue(null)
                    .canActivate(true)
                    .canCreateNewRequest(false)
                    .latestPassedRequestId(null)
                    .inactiveStampCount(inactiveStampCount)
                    .totalStamps(totalStamps)
                    .build();
        }

        // 2. Lấy danh sách chỉ tiêu ACTIVE được gán cho loại nông sản của lô
        List<CategoryCriterion> assignments = categoryCriterionRepository
                .findByCategoryIdAndCriteriaStatus(lot.getProductCategory().getId(), "ACTIVE");

        UUID latestPassedRequestId = findLatestPassedRequestId(lot.getId());

        if (assignments.isEmpty()) {
            return InspectionValidityResponse.builder()
                    .requiresInspection(true)
                    .status(InspectionValidityStatus.NO_VALID_RESULT)
                    .earliestExpiryDate(null)
                    .daysRemaining(null)
                    .daysOverdue(null)
                    .canActivate(false)
                    .canCreateNewRequest(canCreateNewRequest)
                    .latestPassedRequestId(latestPassedRequestId)
                    .inactiveStampCount(inactiveStampCount)
                    .totalStamps(totalStamps)
                    .build();
        }

        // 3. Lấy kết quả kiểm nghiệm mới nhất theo từng chỉ tiêu (ưu tiên criterionId từ danh mục)
        LatestInspectionResultsHolder resultsHolder = new LatestInspectionResultsHolder();
        for (InspectionCriterionResult result : resultRepository.findAllByProductionLotId(lot.getId())) {
            resultsHolder.addResult(result);
        }

        int totalCriteria = assignments.size();
        int passedCriteria = 0;
        LocalDate earliestExpiry = null;
        boolean hasFailedOrMissing = false;

        List<CriterionValidityResponse> criteriaList = new ArrayList<>();
        List<String> expiringCriteria = new ArrayList<>();
        List<String> expiredCriteria = new ArrayList<>();

        for (CategoryCriterion assignment : assignments) {
            InspectionCriterionCatalog catalog = assignment.getCriterion();
            String baseName = catalog.getName();
            String standard = catalog.getReferenceStandard();
            String displayName = (standard != null && !standard.isBlank())
                    ? baseName + " (" + standard + ")"
                    : baseName;

            InspectionCriterionResult latest = resultsHolder.findLatest(catalog);

            CriterionValidityResponse.CriterionValidityResponseBuilder cBuilder = CriterionValidityResponse.builder()
                    .criterionId(catalog.getId())
                    .criterionCode(standard)
                    .criterionName(displayName);

            if (latest != null && Boolean.TRUE.equals(latest.getPassed()) && latest.getExpiryDate() != null) {
                passedCriteria++;
                LocalDate expiry = latest.getExpiryDate();
                cBuilder.passed(true).expiryDate(expiry);

                if (earliestExpiry == null || expiry.isBefore(earliestExpiry)) {
                    earliestExpiry = expiry;
                }

                if (expiry.isBefore(today)) {
                    long overdue = ChronoUnit.DAYS.between(expiry, today);
                    cBuilder.status(InspectionValidityStatus.EXPIRED)
                            .daysOverdue(overdue)
                            .daysRemaining(null);
                    expiredCriteria.add(displayName);
                } else {
                    long remaining = ChronoUnit.DAYS.between(today, expiry);
                    if (remaining <= warningThresholdDays) {
                        cBuilder.status(InspectionValidityStatus.EXPIRING)
                                .daysRemaining(remaining)
                                .daysOverdue(null);
                        expiringCriteria.add(displayName);
                    } else {
                        cBuilder.status(InspectionValidityStatus.VALID)
                                .daysRemaining(remaining)
                                .daysOverdue(null);
                    }
                }
            } else {
                hasFailedOrMissing = true;
                cBuilder.passed(latest != null ? latest.getPassed() : false)
                        .expiryDate(latest != null ? latest.getExpiryDate() : null)
                        .status(InspectionValidityStatus.NO_VALID_RESULT)
                        .daysRemaining(null)
                        .daysOverdue(null);
            }
            criteriaList.add(cBuilder.build());
        }

        // Nếu chưa đạt đủ tất cả chỉ tiêu bắt buộc
        if (hasFailedOrMissing || passedCriteria < totalCriteria || earliestExpiry == null) {
            return InspectionValidityResponse.builder()
                    .requiresInspection(true)
                    .status(InspectionValidityStatus.NO_VALID_RESULT)
                    .earliestExpiryDate(earliestExpiry)
                    .daysRemaining(null)
                    .daysOverdue(null)
                    .canActivate(false)
                    .canCreateNewRequest(canCreateNewRequest)
                    .latestPassedRequestId(latestPassedRequestId)
                    .inactiveStampCount(inactiveStampCount)
                    .totalStamps(totalStamps)
                    .criteria(criteriaList)
                    .expiringCriteria(expiringCriteria)
                    .expiredCriteria(expiredCriteria)
                    .build();
        }

        // 4. Khi tất cả chỉ tiêu đều đạt: so sánh ngày hết hạn sớm nhất với ngày hiện tại
        if (earliestExpiry.isBefore(today)) {
            long daysOverdue = ChronoUnit.DAYS.between(earliestExpiry, today);
            return InspectionValidityResponse.builder()
                    .requiresInspection(true)
                    .status(InspectionValidityStatus.EXPIRED)
                    .earliestExpiryDate(earliestExpiry)
                    .daysRemaining(null)
                    .daysOverdue(daysOverdue)
                    .canActivate(false)
                    .canCreateNewRequest(canCreateNewRequest)
                    .latestPassedRequestId(latestPassedRequestId)
                    .inactiveStampCount(inactiveStampCount)
                    .totalStamps(totalStamps)
                    .criteria(criteriaList)
                    .expiringCriteria(expiringCriteria)
                    .expiredCriteria(expiredCriteria)
                    .build();
        }

        long daysRemaining = ChronoUnit.DAYS.between(today, earliestExpiry);
        InspectionValidityStatus status = (daysRemaining <= warningThresholdDays)
                ? InspectionValidityStatus.EXPIRING
                : InspectionValidityStatus.VALID;

        return InspectionValidityResponse.builder()
                .requiresInspection(true)
                .status(status)
                .earliestExpiryDate(earliestExpiry)
                .daysRemaining(daysRemaining)
                .daysOverdue(null)
                .canActivate(true)
                .canCreateNewRequest(canCreateNewRequest)
                .latestPassedRequestId(latestPassedRequestId)
                .inactiveStampCount(inactiveStampCount)
                .totalStamps(totalStamps)
                .criteria(criteriaList)
                .expiringCriteria(expiringCriteria)
                .expiredCriteria(expiredCriteria)
                .build();
    }

    /**
     * Container lưu trữ kết quả kiểm nghiệm mới nhất của lô, ưu tiên index theo criterionId danh mục
     * và fallback theo code / name cho dữ liệu legacy.
     */
    private static class LatestInspectionResultsHolder {
        private final Map<Long, InspectionCriterionResult> byCatalogId = new HashMap<>();
        private final Map<String, InspectionCriterionResult> byCodeOrName = new HashMap<>();

        void addResult(InspectionCriterionResult result) {
            if (result.getResultDate() == null || result.getInspectionCriterion() == null) {
                return;
            }

            InspectionCriterion ic = result.getInspectionCriterion();
            Long criterionId = ic.getCriterionId();
            if (criterionId != null) {
                putIfNewer(byCatalogId, criterionId, result);
            }

            String code = ic.getCriterionCode();
            String name = ic.getCriterionName();

            if (code != null) {
                putIfNewer(byCodeOrName, code, result);
            }
            if (name != null && !name.equals(code)) {
                putIfNewer(byCodeOrName, name, result);
            }
        }

        InspectionCriterionResult findLatest(InspectionCriterionCatalog catalog) {
            if (catalog == null) {
                return null;
            }
            if (catalog.getId() != null && byCatalogId.containsKey(catalog.getId())) {
                return byCatalogId.get(catalog.getId());
            }
            if (catalog.getName() != null && byCodeOrName.containsKey(catalog.getName())) {
                return byCodeOrName.get(catalog.getName());
            }
            return null;
        }

        private <K> void putIfNewer(Map<K, InspectionCriterionResult> map, K key, InspectionCriterionResult result) {
            if (key == null) {
                return;
            }

            InspectionCriterionResult current = map.get(key);
            if (current == null) {
                map.put(key, result);
                return;
            }

            LocalDateTime resultUpdatedAt = result.getUpdatedAt();
            LocalDateTime currentUpdatedAt = current.getUpdatedAt();

            boolean isNewer = result.getResultDate().isAfter(current.getResultDate())
                    || (result.getResultDate().isEqual(current.getResultDate())
                    && resultUpdatedAt != null
                    && (currentUpdatedAt == null || resultUpdatedAt.isAfter(currentUpdatedAt)));

            if (isNewer) {
                map.put(key, result);
            }
        }
    }

    /**
     * Tìm ID của yêu cầu kiểm nghiệm PASSED mới nhất theo thời gian tạo.
     */
    private UUID findLatestPassedRequestId(UUID lotId) {
        List<InspectionRequest> requests = inspectionRequestRepository
                .findByProductionLot_IdOrderByCreatedAtDesc(lotId);

        return requests.stream()
                .filter(r -> r.getStatus() == InspectionRequestStatus.PASSED)
                .map(InspectionRequest::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Đếm số lượng mã tem INACTIVE thuộc các lô hàng chưa thu hồi.
     * Trả về null nếu lô sản xuất chưa tạo lô hàng nào.
     */
    private Long calculateInactiveStampCount(UUID lotId) {
        long totalStamps = traceCodeRepository.countTotalByProductionLotId(lotId);
        if (totalStamps == 0) {
            return null;
        }
        return traceCodeRepository.countInactiveByProductionLotId(lotId);
    }

    /**
     * Đếm tổng số lượng mã tem thuộc các lô hàng chưa thu hồi của lô.
     */
    private Long calculateTotalStamps(UUID lotId) {
        if (lotId == null) {
            return 0L;
        }
        return traceCodeRepository.countTotalByProductionLotId(lotId);
    }

    /**
     * Kiểm tra lô sản xuất có đủ điều kiện tạo yêu cầu kiểm nghiệm mới hay không.
     */
    private boolean canCreateInspectionRequest(ProductionLot lot) {
        if (lot.getStatus() == null) {
            return false;
        }
        ProductionLotStatus status = lot.getStatus();

        // Không cho phép tạo với lô đã bị hủy, loại bỏ hoặc thu hồi
        if (status == ProductionLotStatus.CANCELLED
                || status == ProductionLotStatus.DISPOSED
                || status == ProductionLotStatus.RECALLED
                || status == ProductionLotStatus.DRAFT
                || status == ProductionLotStatus.REJECTED
                || status == ProductionLotStatus.PENDING) {
            return false;
        }

        return true;
    }
}
