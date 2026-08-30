package vn.nguongocso.trace.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.service.AnomalyThresholdService;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.common.util.GeoDistanceUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.dto.request.LockTraceCodeRequest;
import vn.nguongocso.trace.dto.response.AnomalyDetails;
import vn.nguongocso.trace.dto.response.LockTraceCodeResponse;
import vn.nguongocso.trace.dto.response.ScanLogDetail;
import vn.nguongocso.trace.dto.response.ScoreBreakdown;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeDetailResponse;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeResponse;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.SuspectDetectionService;

/**
 * Triển khai phát hiện nghi vấn và quản lý khóa mã tem (NCL-08-CN-007).
 *
 * <p>
 * Service này chỉ chịu trách nhiệm chấm điểm nghi vấn, cập nhật trạng thái
 * {@code SUSPECT}, gửi cảnh báo nghi vấn và cho phép VT-01 khóa/mở khóa thủ công.
 * Nó KHÔNG tạo cảnh báo quét bất thường (Alert) — trách nhiệm đó thuộc về
 * NCL-08-CN-001.
 * </p>
 */
@Slf4j
@Service
public class SuspectDetectionServiceImpl implements SuspectDetectionService {

    // --- Thresholds per API doc ---
    private static final int HIGH_FREQUENCY_THRESHOLD = 10; // ≥ 10 scans in 24h → +30 points
    private static final int HIGH_FREQUENCY_SCORE = 30;
    private static final double IMPOSSIBLE_TRAVEL_DISTANCE_KM = 50.0; // > 50km
    private static final int IMPOSSIBLE_TRAVEL_MINUTES = 30; // < 30 min → +40 points
    private static final int IMPOSSIBLE_TRAVEL_SCORE = 40;
    private static final int MULTIPLE_LOCATIONS_THRESHOLD = 5; // ≥ 5 unique locations in 24h → +15 points
    private static final int MULTIPLE_LOCATIONS_SCORE = 15;
    private static final int SUSPECT_THRESHOLD = 50; // ≥ 50 → SUSPECT
    private static final int MAX_SCORE = 100;
    private static final double LOCATION_EPSILON_KM = 0.5; // Coi là cùng vị trí nếu khoảng cách < 0.5km

    private final TraceCodeRepository traceCodeRepository;
    private final TraceCodeScanLogRepository scanLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AnomalyThresholdService anomalyThresholdService;

    public SuspectDetectionServiceImpl(
            TraceCodeRepository traceCodeRepository,
            TraceCodeScanLogRepository scanLogRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this(traceCodeRepository, scanLogRepository, userRepository, notificationService, null);
    }

    public SuspectDetectionServiceImpl(
            TraceCodeRepository traceCodeRepository,
            TraceCodeScanLogRepository scanLogRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AnomalyThresholdService anomalyThresholdService) {
        this.traceCodeRepository = traceCodeRepository;
        this.scanLogRepository = scanLogRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.anomalyThresholdService = anomalyThresholdService;
    }

    @Override
    @Transactional
    public void evaluateSuspicion(UUID traceCodeId) {
        TraceCode traceCode = traceCodeRepository.findById(traceCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem."));

        // Only evaluate if ACTIVE or SUSPECT
        if (traceCode.getStatus() != TraceCodeStatus.ACTIVE
                && traceCode.getStatus() != TraceCodeStatus.SUSPECT) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        // Get all scans in the last 24 hours
        List<TraceCodeScanLog> recentScans = scanLogRepository
                .findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(traceCodeId, twentyFourHoursAgo);

        if (recentScans.size() < 2) {
            // Not enough scans to evaluate
            return;
        }

        // Sort ascending by time for travel analysis
        List<TraceCodeScanLog> sortedScans = recentScans.stream()
                .sorted(Comparator.comparing(TraceCodeScanLog::getScannedAt))
                .collect(Collectors.toList());

        AnomalyThresholdResponse threshold = getEffectiveThreshold(traceCode);
        SuspicionEvaluation evaluation = evaluate(sortedScans, threshold, traceCode);

        // Build suspicion reason
        StringBuilder reasonBuilder = new StringBuilder();
        if (evaluation.highFreqScore() > 0) {
            reasonBuilder.append("Số lượt quét cao (").append(recentScans.size())
                    .append(" lượt trong 24 giờ); ");
        }
        if (evaluation.impossibleTravelScore() > 0) {
            reasonBuilder.append("Khoảng cách không hợp lý: ")
                    .append(String.format("%.0f", evaluation.firstImpossibleDistanceKm())).append("km trong ")
                    .append(evaluation.firstImpossibleMinutes()).append(" phút; ");
        }
        if (evaluation.multipleLocationsScore() > 0) {
            reasonBuilder.append(evaluation.uniqueLocations()).append(" địa điểm khác nhau trong 24 giờ; ");
        }

        String reason = reasonBuilder.length() > 0
                ? reasonBuilder.substring(0, reasonBuilder.length() - 2) // remove trailing "; "
                : null;

        // Update trace code
        traceCode.setSuspicionScore(evaluation.totalScore());
        traceCode.setSuspicionReason(reason);

        if (evaluation.totalScore() >= SUSPECT_THRESHOLD) {
            if (traceCode.getStatus() == TraceCodeStatus.ACTIVE) {
                traceCode.setStatus(TraceCodeStatus.SUSPECT);
                log.info("Trace code {} marked as SUSPECT with score {}", traceCode.getCodeValue(),
                        evaluation.totalScore());

                // Send notification to VT-01
                try {
                    notificationService.sendSuspectTraceCodeNotification(traceCode);
                } catch (Exception e) {
                    log.warn("Failed to send suspect notification for trace code {}: {}",
                            traceCode.getCodeValue(), e.getMessage());
                }
            }
        }

        traceCodeRepository.save(traceCode);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SuspectTraceCodeResponse> getSuspectTraceCodes(Integer minScore, String statusStr,
            int page, int size) {
        int effectiveMinScore = minScore != null ? minScore : 30;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "suspicionScore"));

        Page<TraceCode> traceCodePage;

        if (statusStr != null && !statusStr.isBlank()) {
            TraceCodeStatus filterStatus;
            try {
                filterStatus = TraceCodeStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Trạng thái không hợp lệ: " + statusStr);
            }
            traceCodePage = traceCodeRepository.findBySuspicionScoreGreaterThanEqualAndStatus(
                    effectiveMinScore, filterStatus, pageRequest);
        } else {
            traceCodePage = traceCodeRepository.findBySuspicionScoreGreaterThanEqualAndStatusIn(
                    effectiveMinScore,
                    List.of(TraceCodeStatus.SUSPECT, TraceCodeStatus.LOCKED),
                    pageRequest);
        }

        List<SuspectTraceCodeResponse> items = traceCodePage.getContent().stream()
                .map(this::toSuspectResponse)
                .collect(Collectors.toList());

        return PageResponse.from(traceCodePage, items);
    }

    @Override
    @Transactional(readOnly = true)
    public SuspectTraceCodeDetailResponse getSuspectDetail(UUID traceCodeId) {
        TraceCode traceCode = traceCodeRepository.findById(traceCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem."));

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<TraceCodeScanLog> recentScans = scanLogRepository
                .findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(traceCodeId, twentyFourHoursAgo);

        List<TraceCodeScanLog> sortedScans = recentScans.stream()
                .sorted(Comparator.comparing(TraceCodeScanLog::getScannedAt))
                .collect(Collectors.toList());

        // Build scan log details
        List<ScanLogDetail> scanLogDetails = sortedScans.stream()
                .map(scan -> ScanLogDetail.builder()
                        .scannedAt(scan.getScannedAt())
                        .latitude(scan.getLatitude() != null ? scan.getLatitude().doubleValue() : null)
                        .longitude(scan.getLongitude() != null ? scan.getLongitude().doubleValue() : null)
                        .location(scan.getLocation())
                        .userAgent(scan.getUserAgent())
                        .build())
                .collect(Collectors.toList());

        // Reuse the exact same scoring engine as evaluateSuspicion so the
        // breakdown always matches the persisted suspicionScore.
        AnomalyThresholdResponse threshold = getEffectiveThreshold(traceCode);
        SuspicionEvaluation evaluation = evaluate(sortedScans, threshold, traceCode);

        AnomalyDetails anomalyDetails = AnomalyDetails.builder()
                .totalScans(recentScans.size())
                .uniqueLocations(evaluation.uniqueLocations())
                .impossibleTravelCount(evaluation.impossibleTravelCount())
                .scoreBreakdown(ScoreBreakdown.builder()
                        .highFrequency(evaluation.highFreqScore())
                        .impossibleTravel(evaluation.impossibleTravelScore())
                        .multipleLocations(evaluation.multipleLocationsScore())
                        .build())
                .build();

        return SuspectTraceCodeDetailResponse.builder()
                .id(traceCode.getId())
                .codeValue(traceCode.getCodeValue())
                .shipmentName(traceCode.getShipment() != null ? traceCode.getShipment().getName() : null)
                .status(traceCode.getStatus().name())
                .suspicionScore(traceCode.getSuspicionScore())
                .suspicionReason(traceCode.getSuspicionReason())
                .scanCount(recentScans.size())
                .uniqueLocations(evaluation.uniqueLocations())
                .firstScannedAt(sortedScans.isEmpty() ? null : sortedScans.get(0).getScannedAt())
                .lastScannedAt(sortedScans.isEmpty() ? null : sortedScans.get(sortedScans.size() - 1).getScannedAt())
                .lockedAt(traceCode.getLockedAt())
                .lockedBy(traceCode.getLockedBy() != null ? traceCode.getLockedBy().getUserId() : null)
                .lockedByName(traceCode.getLockedBy() != null ? traceCode.getLockedBy().getFullName() : null)
                .lockReason(traceCode.getLockReason())
                .scanLogs(scanLogDetails)
                .anomalyDetails(anomalyDetails)
                .build();
    }

    @Override
    @Transactional
    public LockTraceCodeResponse lockTraceCode(UUID traceCodeId, LockTraceCodeRequest request, UUID userId,
            String userName) {
        TraceCode traceCode = traceCodeRepository.findById(traceCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem."));

        if (traceCode.getStatus() == TraceCodeStatus.LOCKED) {
            throw new BusinessException("Mã tem đã bị khóa bởi Quản trị viên khác.");
        }

        if (traceCode.getStatus() != TraceCodeStatus.SUSPECT) {
            throw new BusinessException("Chỉ có thể khóa mã tem đang ở trạng thái nghi vấn.");
        }

        User lockingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        traceCode.setStatus(TraceCodeStatus.LOCKED);
        traceCode.setLockedAt(LocalDateTime.now());
        traceCode.setLockedBy(lockingUser);
        traceCode.setLockReason(request.getReason());

        traceCodeRepository.save(traceCode);

        log.info("Trace code {} locked by {} ({}). Reason: {}",
                traceCode.getCodeValue(), userName, userId, request.getReason());

        return LockTraceCodeResponse.builder()
                .id(traceCode.getId())
                .codeValue(traceCode.getCodeValue())
                .status(traceCode.getStatus().name())
                .lockedAt(traceCode.getLockedAt())
                .lockedBy(userId)
                .lockedByName(userName)
                .lockReason(request.getReason())
                .notificationSent(true)
                .build();
    }

    @Override
    @Transactional
    public LockTraceCodeResponse unlockTraceCode(UUID traceCodeId, String reason, UUID userId, String userName) {
        TraceCode traceCode = traceCodeRepository.findById(traceCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem."));

        if (traceCode.getStatus() != TraceCodeStatus.LOCKED) {
            throw new BusinessException("Mã tem không ở trạng thái bị khóa.");
        }

        User unlockingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        // After unlock, go back to SUSPECT state (not ACTIVE) because the suspicion still exists
        traceCode.setStatus(TraceCodeStatus.SUSPECT);
        traceCode.setLockedAt(null);
        traceCode.setLockedBy(null);
        traceCode.setLockReason(null);

        traceCodeRepository.save(traceCode);

        log.info("Trace code {} unlocked by {} ({}). Reason: {}",
                traceCode.getCodeValue(), userName, userId, reason);

        return LockTraceCodeResponse.builder()
                .id(traceCode.getId())
                .codeValue(traceCode.getCodeValue())
                .status(traceCode.getStatus().name())
                .lockedAt(null)
                .lockedBy(userId)
                .lockedByName(userName)
                .lockReason(reason)
                .notificationSent(true)
                .build();
    }

    /**
     * Trung tâm tính điểm nghi vấn NCL-08-CN-007.
     *
     * <p>
     * Cả {@code evaluateSuspicion} lẫn {@code getSuspectDetail} đều dùng duy nhất
     * phương thức này, đảm bảo điểm được lưu và bảng phân tích hiển thị luôn khớp.
     * Quy tắc không thay đổi:
     * </p>
     * <ul>
     * <li>Số lượt quét ≥ 10 trong 24h → +30.</li>
     * <li>Di chuyển bất hợp lý (> 50km, < 30 phút) → tối đa +40 (không cộng dồn).</li>
     * <li>Số địa điểm khác nhau ≥ 5 → +15.</li>
     * </ul>
     *
     * @param sortedScans các lượt quét đã sắp xếp tăng dần theo thời gian
     * @return kết quả đánh giá (từng hạng mục + tổng điểm)
     */
    private SuspicionEvaluation evaluate(List<TraceCodeScanLog> sortedScans, AnomalyThresholdResponse threshold, TraceCode traceCode) {
        int maxPerDay = (threshold != null && threshold.getMaxScansPerDay() != null) ? threshold.getMaxScansPerDay() : HIGH_FREQUENCY_THRESHOLD;
        int maxPerHour = (threshold != null && threshold.getMaxScansPerHour() != null) ? threshold.getMaxScansPerHour() : 5;
        double maxDistanceKm = (threshold != null && threshold.getMaxDistanceKmPer30Min() != null) ? threshold.getMaxDistanceKmPer30Min() : IMPOSSIBLE_TRAVEL_DISTANCE_KM;
        int minTimeMinutes = (threshold != null && threshold.getMinTimeBetweenScansMinutes() != null) ? threshold.getMinTimeBetweenScansMinutes() : IMPOSSIBLE_TRAVEL_MINUTES;

        int highFreqScore = 0;
        int impossibleTravelScore = 0;
        int multipleLocationsScore = 0;
        int impossibleTravelCount = 0;
        Double firstImpossibleDistanceKm = null;
        Long firstImpossibleMinutes = null;

        // 1. High frequency: >= maxPerDay scans in 24h OR >= maxPerHour in any 1h window
        if (sortedScans.size() >= maxPerDay) {
            highFreqScore = HIGH_FREQUENCY_SCORE;
        } else {
            for (int i = 0; i < sortedScans.size(); i++) {
                LocalDateTime windowStart = sortedScans.get(i).getScannedAt();
                LocalDateTime windowEnd = windowStart.plusHours(1);
                long countInHour = sortedScans.stream()
                        .filter(s -> !s.getScannedAt().isBefore(windowStart) && !s.getScannedAt().isAfter(windowEnd))
                        .count();
                if (countInHour >= maxPerHour) {
                    highFreqScore = HIGH_FREQUENCY_SCORE;
                    break;
                }
            }
        }

        // 2. Impossible travel: > maxDistanceKm within < minTimeMinutes between consecutive scans with coordinates
        for (int i = 0; i < sortedScans.size() - 1; i++) {
            TraceCodeScanLog scan1 = sortedScans.get(i);
            TraceCodeScanLog scan2 = sortedScans.get(i + 1);

            if (scan1.getLatitude() == null || scan1.getLongitude() == null
                    || scan2.getLatitude() == null || scan2.getLongitude() == null) {
                continue;
            }

            double distance = GeoDistanceUtils.haversineKm(
                    scan1.getLatitude().doubleValue(),
                    scan1.getLongitude().doubleValue(),
                    scan2.getLatitude().doubleValue(),
                    scan2.getLongitude().doubleValue());

            long minutesBetween = Duration.between(scan1.getScannedAt(), scan2.getScannedAt()).toMinutes();

            if (distance > maxDistanceKm && minutesBetween <= minTimeMinutes) {
                impossibleTravelCount++;
                if (firstImpossibleDistanceKm == null) {
                    firstImpossibleDistanceKm = distance;
                    firstImpossibleMinutes = minutesBetween;
                }
            }
        }

        // Rule contributes at most +40 for the category, regardless of how many pairs match.
        impossibleTravelScore = impossibleTravelCount > 0 ? IMPOSSIBLE_TRAVEL_SCORE : 0;

        // 3. Multiple locations: count unique locations (by distance epsilon)
        int uniqueLocations = countUniqueLocations(sortedScans);
        if (uniqueLocations >= MULTIPLE_LOCATIONS_THRESHOLD) {
            multipleLocationsScore = MULTIPLE_LOCATIONS_SCORE;
        }

        int totalScore = Math.min(MAX_SCORE, highFreqScore + impossibleTravelScore + multipleLocationsScore);

        return new SuspicionEvaluation(
                highFreqScore,
                impossibleTravelScore,
                multipleLocationsScore,
                impossibleTravelCount,
                uniqueLocations,
                totalScore,
                firstImpossibleDistanceKm,
                firstImpossibleMinutes);
    }

    private AnomalyThresholdResponse getEffectiveThreshold(TraceCode traceCode) {
        if (anomalyThresholdService == null || traceCode == null) {
            return null;
        }
        UUID categoryId = null;
        if (traceCode.getShipment() != null && traceCode.getShipment().getProductionLot() != null
                && traceCode.getShipment().getProductionLot().getProductCategory() != null) {
            categoryId = traceCode.getShipment().getProductionLot().getProductCategory().getId();
        }
        return anomalyThresholdService.getEffectiveThreshold(categoryId);
    }

    /**
     * Đếm số địa điểm duy nhất trong danh sách quét.
     * Sử dụng ngưỡng khoảng cách LOCATION_EPSILON_KM để xác định "cùng vị trí".
     */
    private int countUniqueLocations(List<TraceCodeScanLog> scanLogs) {
        List<TraceCodeScanLog> distinct = new ArrayList<>();

        for (TraceCodeScanLog scan : scanLogs) {
            if (scan.getLatitude() == null || scan.getLongitude() == null) {
                continue;
            }
            boolean found = false;
            for (TraceCodeScanLog existing : distinct) {
                double dist = GeoDistanceUtils.haversineKm(
                        scan.getLatitude().doubleValue(),
                        scan.getLongitude().doubleValue(),
                        existing.getLatitude().doubleValue(),
                        existing.getLongitude().doubleValue());
                if (dist < LOCATION_EPSILON_KM) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                distinct.add(scan);
            }
        }
        return distinct.size();
    }

    /**
     * Chuyển TraceCode entity sang SuspectTraceCodeResponse.
     */
    private SuspectTraceCodeResponse toSuspectResponse(TraceCode tc) {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<TraceCodeScanLog> recentScans = scanLogRepository
                .findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(tc.getId(), twentyFourHoursAgo);

        int uniqueLocations = countUniqueLocations(recentScans);

        return SuspectTraceCodeResponse.builder()
                .id(tc.getId())
                .codeValue(tc.getCodeValue())
                .shipmentName(tc.getShipment() != null ? tc.getShipment().getName() : null)
                .status(tc.getStatus().name())
                .suspicionScore(tc.getSuspicionScore())
                .suspicionReason(tc.getSuspicionReason())
                .scanCount(recentScans.size())
                .uniqueLocations(uniqueLocations)
                .firstScannedAt(recentScans.isEmpty() ? null
                        : recentScans.get(recentScans.size() - 1).getScannedAt())
                .lastScannedAt(recentScans.isEmpty() ? null : recentScans.get(0).getScannedAt())
                .lockedAt(tc.getLockedAt())
                .lockedBy(tc.getLockedBy() != null ? tc.getLockedBy().getUserId() : null)
                .lockedByName(tc.getLockedBy() != null ? tc.getLockedBy().getFullName() : null)
                .lockReason(tc.getLockReason())
                .build();
    }

    /**
     * Kết quả đánh giá nghi vấn bất biến (không được sửa đổi bởi người gọi).
     */
    private record SuspicionEvaluation(
            int highFreqScore,
            int impossibleTravelScore,
            int multipleLocationsScore,
            int impossibleTravelCount,
            int uniqueLocations,
            int totalScore,
            Double firstImpossibleDistanceKm,
            Long firstImpossibleMinutes) {
    }
}