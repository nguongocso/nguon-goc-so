package vn.nguongocso.trace.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.event.ActivityLogEvent;
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
import vn.nguongocso.trace.dto.request.UnlockTraceCodeRequest;
import vn.nguongocso.trace.dto.response.AnomalyDetails;
import vn.nguongocso.trace.dto.response.LockTraceCodeResponse;
import vn.nguongocso.trace.dto.response.ScanLogDetail;
import vn.nguongocso.trace.dto.response.ScoreBreakdown;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeDetailResponse;
import vn.nguongocso.trace.dto.response.SuspectTraceCodeResponse;
import vn.nguongocso.trace.dto.response.UnlockTraceCodeResponse;
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
@RequiredArgsConstructor
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
    private final ApplicationEventPublisher eventPublisher;

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

        SuspicionEvaluation evaluation = evaluate(sortedScans);

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
            if (minScore != null) {
                traceCodePage = traceCodeRepository.findBySuspicionScoreGreaterThanEqualAndStatus(
                        minScore, filterStatus, pageRequest);
            } else {
                traceCodePage = traceCodeRepository.findByStatus(filterStatus, pageRequest);
            }
        } else {
            if (minScore != null) {
                traceCodePage = traceCodeRepository.findBySuspicionScoreGreaterThanEqualAndStatusIn(
                        minScore,
                        List.of(TraceCodeStatus.SUSPECT, TraceCodeStatus.LOCKED, TraceCodeStatus.ACTIVE),
                        pageRequest);
            } else {
                traceCodePage = traceCodeRepository.findByStatusIn(
                        List.of(TraceCodeStatus.SUSPECT, TraceCodeStatus.LOCKED, TraceCodeStatus.ACTIVE),
                        pageRequest);
            }
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
        SuspicionEvaluation evaluation = evaluate(sortedScans);

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
                .unlockedAt(traceCode.getUnlockedAt())
                .unlockedBy(traceCode.getUnlockedBy() != null ? traceCode.getUnlockedBy().getUserId() : null)
                .unlockedByName(traceCode.getUnlockedBy() != null ? traceCode.getUnlockedBy().getFullName() : null)
                .unlockConclusion(traceCode.getUnlockConclusion())
                .unlockEvidence(traceCode.getUnlockEvidence())
                .verificationNote(traceCode.getVerificationNote())
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

        // Ghi nhận nhật ký hoạt động
        try {
            UUID orgId = traceCode.getShipment() != null && traceCode.getShipment().getOrganization() != null
                    ? traceCode.getShipment().getOrganization().getOrganizationId()
                    : null;
            eventPublisher.publishEvent(ActivityLogEvent.builder()
                    .userId(userId)
                    .username(lockingUser.getUserName())
                    .fullName(userName)
                    .organizationId(orgId)
                    .action("LOCK_TRACE_CODE")
                    .description("Khóa mã tem " + traceCode.getCodeValue() + ". Lý do: " + request.getReason())
                    .entityType("TRACE_CODE")
                    .entityId(traceCode.getId().toString())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể phát sự kiện ActivityLogEvent khi khóa mã tem: {}", e.getMessage());
        }

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

        // Sau khi mở khóa, chuyển về ACTIVE kèm kết luận xác minh
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setUnlockedAt(LocalDateTime.now());
        traceCode.setUnlockedBy(unlockingUser);
        traceCode.setUnlockConclusion(reason);
        traceCode.setVerificationNote(reason);

        traceCodeRepository.save(traceCode);

        // Ghi nhận nhật ký hoạt động
        try {
            UUID orgId = traceCode.getShipment() != null && traceCode.getShipment().getOrganization() != null
                    ? traceCode.getShipment().getOrganization().getOrganizationId()
                    : null;
            eventPublisher.publishEvent(ActivityLogEvent.builder()
                    .userId(userId)
                    .username(unlockingUser.getUserName())
                    .fullName(userName)
                    .organizationId(orgId)
                    .action("UNLOCK_TRACE_CODE")
                    .description("Mở khóa mã tem " + traceCode.getCodeValue() + ". Kết luận: " + reason)
                    .entityType("TRACE_CODE")
                    .entityId(traceCode.getId().toString())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Không thể phát sự kiện ActivityLogEvent khi mở khóa mã tem: {}", e.getMessage());
        }

        // Gửi thông báo đến HTX sở hữu
        try {
            notificationService.sendTraceCodeUnlockedNotification(traceCode);
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo mở khóa mã tem {}: {}", traceCode.getCodeValue(), e.getMessage());
        }

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

    @Override
    @Transactional
    public UnlockTraceCodeResponse unlockTraceCodeWithVerification(String codeOrId, UnlockTraceCodeRequest request, UUID userId, String userName) {
        TraceCode traceCode = findTraceCodeByIdOrCodeValue(codeOrId);

        if (traceCode.getStatus() != TraceCodeStatus.LOCKED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Mã tem không ở trạng thái bị khóa.");
        }

        String conclusion = request.getConclusion() != null ? request.getConclusion().trim() : "";
        if (conclusion.isBlank()) {
            throw new BusinessException("Kết luận xác minh không được để trống.");
        }

        // Kiểm tra cùng Quản trị viên khóa: nếu trùng người khóa, yêu cầu kết luận chi tiết hơn (≥ 20 ký tự)
        if (traceCode.getLockedBy() != null && traceCode.getLockedBy().getUserId().equals(userId)) {
            if (conclusion.length() < 20) {
                throw new BusinessException("Quản trị viên đã khóa mã tem cần nhập kết luận xác minh chi tiết hơn (tối thiểu 20 ký tự).");
            }
        }

        User unlockingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        LocalDateTime now = LocalDateTime.now();
        traceCode.setStatus(TraceCodeStatus.ACTIVE);
        traceCode.setUnlockedAt(now);
        traceCode.setUnlockedBy(unlockingUser);
        traceCode.setUnlockConclusion(conclusion);
        traceCode.setUnlockEvidence(request.getEvidence() != null ? request.getEvidence().trim() : null);
        traceCode.setVerificationNote(conclusion);

        traceCodeRepository.save(traceCode);

        // Ghi nhận nhật ký hoạt động
        try {
            UUID orgId = traceCode.getShipment() != null && traceCode.getShipment().getOrganization() != null
                    ? traceCode.getShipment().getOrganization().getOrganizationId()
                    : null;
            eventPublisher.publishEvent(ActivityLogEvent.builder()
                    .userId(userId)
                    .username(unlockingUser.getUserName())
                    .fullName(userName)
                    .organizationId(orgId)
                    .action("UNLOCK_TRACE_CODE")
                    .description("Mở khóa mã tem " + traceCode.getCodeValue() + ". Kết luận xác minh: " + conclusion)
                    .entityType("TRACE_CODE")
                    .entityId(traceCode.getId().toString())
                    .timestamp(now)
                    .build());
        } catch (Exception e) {
            log.warn("Không thể phát sự kiện ActivityLogEvent khi mở khóa mã tem: {}", e.getMessage());
        }

        // Gửi thông báo đến HTX sở hữu
        boolean notificationSent = false;
        try {
            notificationService.sendTraceCodeUnlockedNotification(traceCode);
            notificationSent = true;
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo mở khóa mã tem {}: {}", traceCode.getCodeValue(), e.getMessage());
        }

        log.info("Trace code {} unlocked after verification by {} ({}). Conclusion: {}",
                traceCode.getCodeValue(), userName, userId, conclusion);

        return UnlockTraceCodeResponse.builder()
                .id(traceCode.getId())
                .codeValue(traceCode.getCodeValue())
                .status(traceCode.getStatus().name())
                .unlockedAt(now)
                .unlockedBy(userId)
                .unlockedByName(userName)
                .unlockConclusion(conclusion)
                .unlockEvidence(traceCode.getUnlockEvidence())
                .verificationNote(conclusion)
                .notificationSent(notificationSent)
                .build();
    }

    private TraceCode findTraceCodeByIdOrCodeValue(String codeOrId) {
        try {
            UUID uuid = UUID.fromString(codeOrId);
            return traceCodeRepository.findById(uuid)
                    .orElseGet(() -> traceCodeRepository.findByCodeValue(codeOrId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem.")));
        } catch (IllegalArgumentException e) {
            return traceCodeRepository.findByCodeValue(codeOrId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã tem."));
        }
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
    private SuspicionEvaluation evaluate(List<TraceCodeScanLog> sortedScans) {
        int highFreqScore = 0;
        int impossibleTravelScore = 0;
        int multipleLocationsScore = 0;
        int impossibleTravelCount = 0;
        Double firstImpossibleDistanceKm = null;
        Long firstImpossibleMinutes = null;

        // 1. High frequency: ≥ 10 scans in 24h
        if (sortedScans.size() >= HIGH_FREQUENCY_THRESHOLD) {
            highFreqScore = HIGH_FREQUENCY_SCORE;
        }

        // 2. Impossible travel: > 50km within < 30 min between consecutive scans with coordinates
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

            if (distance > IMPOSSIBLE_TRAVEL_DISTANCE_KM && minutesBetween < IMPOSSIBLE_TRAVEL_MINUTES) {
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
                .unlockedAt(tc.getUnlockedAt())
                .unlockedBy(tc.getUnlockedBy() != null ? tc.getUnlockedBy().getUserId() : null)
                .unlockedByName(tc.getUnlockedBy() != null ? tc.getUnlockedBy().getFullName() : null)
                .unlockConclusion(tc.getUnlockConclusion())
                .unlockEvidence(tc.getUnlockEvidence())
                .verificationNote(tc.getVerificationNote())
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