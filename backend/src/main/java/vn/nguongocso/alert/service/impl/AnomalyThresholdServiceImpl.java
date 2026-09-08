package vn.nguongocso.alert.service.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.alert.dto.request.CategoryThresholdOverrideRequest;
import vn.nguongocso.alert.dto.request.ImpactEstimationRequest;
import vn.nguongocso.alert.dto.request.UpdateGlobalThresholdRequest;
import vn.nguongocso.alert.dto.response.AllThresholdsResponse;
import vn.nguongocso.alert.dto.response.AnomalyThresholdResponse;
import vn.nguongocso.alert.dto.response.ImpactEstimationResponse;
import vn.nguongocso.alert.entity.AnomalyThreshold;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.repository.AnomalyThresholdRepository;
import vn.nguongocso.alert.service.AnomalyThresholdService;
import vn.nguongocso.alert.util.ScanAnomalyUtils;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.util.GeoDistanceUtils;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.entity.TraceCode;

/**
 * Triển khai dịch vụ cấu hình ngưỡng phát hiện quét bất thường (NCL-08-CN-014).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyThresholdServiceImpl implements AnomalyThresholdService {

    public static final int DEFAULT_MAX_SCANS_PER_HOUR = 5;
    public static final int DEFAULT_MAX_SCANS_PER_DAY = 10;
    public static final BigDecimal DEFAULT_MAX_DISTANCE_KM = new BigDecimal("50.00");
    public static final int DEFAULT_MIN_TIME_BETWEEN_SCANS_MINUTES = 30;
    public static final int DEFAULT_ACTIVATION_AGE_DAYS = 365;

    private final AnomalyThresholdRepository anomalyThresholdRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UserRepository userRepository;
    private final TraceCodeScanLogRepository scanLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public AllThresholdsResponse getAllThresholds() {
        AnomalyThresholdResponse global = getGlobalThreshold();
        List<AnomalyThresholdResponse> overrides = getCategoryOverrides();
        return AllThresholdsResponse.builder()
                .global(global)
                .categoryOverrides(overrides)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyThresholdResponse getGlobalThreshold() {
        return anomalyThresholdRepository.findByProductCategoryIsNullAndIsActiveTrue()
                .or(() -> anomalyThresholdRepository.findByProductCategoryIsNull())
                .map(this::mapToResponse)
                .orElseGet(this::buildDefaultGlobalResponse);
    }

    @Override
    @Transactional
    public AnomalyThresholdResponse updateGlobalThreshold(UpdateGlobalThresholdRequest request,
            CustomUserDetails currentUser) {
        User user = getUser(currentUser);

        AnomalyThreshold entity = anomalyThresholdRepository.findByProductCategoryIsNull()
                .orElseGet(() -> AnomalyThreshold.builder()
                        .productCategory(null)
                        .createdBy(user)
                        .isActive(true)
                        .build());

        entity.setMaxScansPerHour(request.getMaxScansPerHour());
        entity.setMaxScansPerDay(request.getMaxScansPerDay());
        entity.setMaxDistanceKmPer30Min(request.getMaxDistanceKmPer30Min());
        entity.setMinTimeBetweenScansMinutes(request.getMinTimeBetweenScansMinutes());
        entity.setActivationAgeDays(request.getActivationAgeDays());
        entity.setIsActive(true);
        entity.setUpdatedBy(user);

        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(user);
        }

        AnomalyThreshold saved = anomalyThresholdRepository.save(entity);

        publishActivityLog(currentUser, "UPDATE_GLOBAL_ANOMALY_THRESHOLD",
                String.format("Cập nhật cấu hình ngưỡng toàn cục: maxScansPerHour=%d, maxScansPerDay=%d, maxDistanceKm=%s, minTimeMinutes=%d, activationAgeDays=%d",
                        request.getMaxScansPerHour(), request.getMaxScansPerDay(), request.getMaxDistanceKmPer30Min(),
                        request.getMinTimeBetweenScansMinutes(), request.getActivationAgeDays()),
                "ANOMALY_THRESHOLD", saved.getId().toString());

        log.info("Quản trị viên {} đã cập nhật cấu hình ngưỡng toàn cục", user.getUserName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnomalyThresholdResponse> getCategoryOverrides() {
        return anomalyThresholdRepository.findAllActiveCategoryOverrides().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AnomalyThresholdResponse saveCategoryOverride(CategoryThresholdOverrideRequest request,
            CustomUserDetails currentUser) {
        User user = getUser(currentUser);

        ProductCategory category = productCategoryRepository.findById(request.getProductCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại nông sản với ID: " + request.getProductCategoryId()));

        AnomalyThreshold entity = anomalyThresholdRepository.findByProductCategoryId(request.getProductCategoryId())
                .orElseGet(() -> AnomalyThreshold.builder()
                        .productCategory(category)
                        .createdBy(user)
                        .build());

        entity.setProductCategory(category);
        entity.setMaxScansPerHour(request.getMaxScansPerHour());
        entity.setMaxScansPerDay(request.getMaxScansPerDay());
        entity.setMaxDistanceKmPer30Min(request.getMaxDistanceKmPer30Min());
        entity.setMinTimeBetweenScansMinutes(request.getMinTimeBetweenScansMinutes());
        entity.setActivationAgeDays(request.getActivationAgeDays());
        entity.setIsActive(true);
        entity.setUpdatedBy(user);

        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(user);
        }

        AnomalyThreshold saved = anomalyThresholdRepository.save(entity);

        publishActivityLog(currentUser, "SAVE_CATEGORY_THRESHOLD_OVERRIDE",
                String.format("Lưu cấu hình ghi đè ngưỡng cho danh mục [%s]: maxScansPerHour=%d, maxScansPerDay=%d, maxDistanceKm=%s, minTimeMinutes=%d, activationAgeDays=%d",
                        category.getName(), request.getMaxScansPerHour(), request.getMaxScansPerDay(),
                        request.getMaxDistanceKmPer30Min(), request.getMinTimeBetweenScansMinutes(), request.getActivationAgeDays()),
                "ANOMALY_THRESHOLD", saved.getId().toString());

        log.info("Quản trị viên {} đã lưu cấu hình ghi đè ngưỡng cho danh mục {}", user.getUserName(), category.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategoryOverride(UUID idOrCategoryId, CustomUserDetails currentUser) {
        User user = getUser(currentUser);

        AnomalyThreshold entity = anomalyThresholdRepository.findById(idOrCategoryId)
                .or(() -> anomalyThresholdRepository.findByProductCategoryId(idOrCategoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình ghi đè danh mục cần xóa."));

        if (entity.getProductCategory() == null) {
            throw new BusinessException("Không thể xóa cấu hình mặc định toàn cục.");
        }

        String categoryName = entity.getProductCategory().getName();
        entity.setIsActive(false);
        entity.setUpdatedBy(user);
        anomalyThresholdRepository.save(entity);

        publishActivityLog(currentUser, "DELETE_CATEGORY_THRESHOLD_OVERRIDE",
                "Xóa cấu hình ghi đè ngưỡng của danh mục: " + categoryName,
                "ANOMALY_THRESHOLD", entity.getId().toString());

        log.info("Quản trị viên {} đã xóa cấu hình ghi đè ngưỡng danh mục {}", user.getUserName(), categoryName);
    }

    @Override
    @Transactional(readOnly = true)
    public AnomalyThresholdResponse getEffectiveThreshold(UUID productCategoryId) {
        if (productCategoryId != null) {
            Optional<AnomalyThreshold> override = anomalyThresholdRepository
                    .findByProductCategoryIdAndIsActiveTrue(productCategoryId);
            if (override.isPresent()) {
                return mapToResponse(override.get());
            }
        }

        return anomalyThresholdRepository.findByProductCategoryIsNullAndIsActiveTrue()
                .map(this::mapToResponse)
                .orElseGet(this::buildDefaultGlobalResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ImpactEstimationResponse estimateImpact(ImpactEstimationRequest request) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<TraceCodeScanLog> allScans = scanLogRepository.findByScannedAtGreaterThanEqualOrderByScannedAtAsc(since);

        List<TraceCodeScanLog> filteredScans = filterScansByCategory(allScans, request.getProductCategoryId());
        Map<UUID, List<TraceCodeScanLog>> scansByTraceCode = filteredScans.stream()
                .filter(s -> s.getTraceCode() != null && s.getTraceCode().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getTraceCode().getId()));

        long totalScansAnalyzed = filteredScans.size();
        long totalTraceCodesAnalyzed = scansByTraceCode.size();
        long estimatedAnomaliesCount = 0;
        long highFrequencyCount = 0;
        long impossibleTravelCount = 0;
        long activationAgeCount = 0;

        for (List<TraceCodeScanLog> rawScans : scansByTraceCode.values()) {
            List<TraceCodeScanLog> scans = rawScans.stream()
                    .sorted(Comparator.comparing(TraceCodeScanLog::getScannedAt))
                    .collect(Collectors.toList());
            if (scans.isEmpty()) continue;

            TraceCode tc = scans.get(0).getTraceCode();

            // Áp dụng gate thời gian ân hạn: chỉ đánh giá các lượt quét sau thời gian ân hạn
            List<TraceCodeScanLog> evaluatedScans = scans.stream()
                    .filter(s -> !ScanAnomalyUtils.isWithinGracePeriod(tc.getActivatedAt(), s.getScannedAt(), request.getActivationAgeDays()))
                    .collect(Collectors.toList());

            if (evaluatedScans.isEmpty()) {
                // Toàn bộ lượt quét của mã tem nằm trong thời gian ân hạn -> bỏ qua đánh giá
                continue;
            }

            boolean highFreq = checkHighFrequency(evaluatedScans, request.getMaxScansPerHour(), request.getMaxScansPerDay());
            boolean impossibleTravel = checkImpossibleTravel(evaluatedScans, request.getMaxDistanceKmPer30Min().doubleValue(), request.getMinTimeBetweenScansMinutes());
            boolean hasScansAfterGrace = checkActivationAge(tc, scans, request.getActivationAgeDays());

            if (highFreq) highFrequencyCount++;
            if (impossibleTravel) impossibleTravelCount++;

            // Chỉ gắn cờ bất thường khi vi phạm tần suất hoặc di chuyển phi lý trên các lượt quét sau ân hạn
            if (highFreq || impossibleTravel) {
                estimatedAnomaliesCount++;
                if (hasScansAfterGrace) {
                    activationAgeCount++;
                }
            }
        }

        String message = String.format(
                "Dự kiến có %d mã tem sẽ bị gắn cờ bất thường trong 30 ngày qua (trên tổng số %d mã tem và %d lượt quét được phân tích).",
                estimatedAnomaliesCount, totalTraceCodesAnalyzed, totalScansAnalyzed);

        return ImpactEstimationResponse.builder()
                .estimatedAnomaliesCount(estimatedAnomaliesCount)
                .totalScansAnalyzed(totalScansAnalyzed)
                .totalTraceCodesAnalyzed(totalTraceCodesAnalyzed)
                .highFrequencyCount(highFrequencyCount)
                .impossibleTravelCount(impossibleTravelCount)
                .activationAgeCount(activationAgeCount)
                .analysisPeriodDays(30)
                .message(message)
                .build();
    }

    private List<TraceCodeScanLog> filterScansByCategory(List<TraceCodeScanLog> allScans, UUID categoryId) {
        if (categoryId == null) {
            return allScans;
        }
        return allScans.stream()
                .filter(scan -> {
                    TraceCode tc = scan.getTraceCode();
                    if (tc == null || tc.getShipment() == null || tc.getShipment().getProductionLot() == null
                            || tc.getShipment().getProductionLot().getProductCategory() == null) {
                        return false;
                    }
                    return categoryId.equals(tc.getShipment().getProductionLot().getProductCategory().getId());
                })
                .collect(Collectors.toList());
    }

    private boolean checkHighFrequency(List<TraceCodeScanLog> scans, int maxPerHour, int maxPerDay) {
        return ScanAnomalyUtils.isHighFrequency(scans, maxPerHour, maxPerDay);
    }

    private boolean checkImpossibleTravel(List<TraceCodeScanLog> scans, double maxDistanceKm, int minTimeMinutes) {
        return ScanAnomalyUtils.isImpossibleTravel(scans, maxDistanceKm, minTimeMinutes);
    }

    private boolean checkActivationAge(TraceCode tc, List<TraceCodeScanLog> scans, Integer gracePeriodDays) {
        if (tc == null || tc.getActivatedAt() == null || gracePeriodDays == null) {
            return false;
        }
        for (TraceCodeScanLog s : scans) {
            if (!ScanAnomalyUtils.isWithinGracePeriod(tc.getActivatedAt(), s.getScannedAt(), gracePeriodDays)) {
                return true;
            }
        }
        return false;
    }


    private AnomalyThresholdResponse mapToResponse(AnomalyThreshold entity) {
        return AnomalyThresholdResponse.builder()
                .id(entity.getId())
                .productCategoryId(entity.getProductCategory() != null ? entity.getProductCategory().getId() : null)
                .productCategoryName(entity.getProductCategory() != null ? entity.getProductCategory().getName() : null)
                .maxScansPerHour(entity.getMaxScansPerHour())
                .maxScansPerDay(entity.getMaxScansPerDay())
                .maxDistanceKmPer30Min(entity.getMaxDistanceKmPer30Min())
                .minTimeBetweenScansMinutes(entity.getMinTimeBetweenScansMinutes())
                .activationAgeDays(entity.getActivationAgeDays())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null)
                .updatedByName(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getFullName() : null)
                .build();
    }

    private AnomalyThresholdResponse buildDefaultGlobalResponse() {
        return AnomalyThresholdResponse.builder()
                .id(null)
                .productCategoryId(null)
                .productCategoryName(null)
                .maxScansPerHour(DEFAULT_MAX_SCANS_PER_HOUR)
                .maxScansPerDay(DEFAULT_MAX_SCANS_PER_DAY)
                .maxDistanceKmPer30Min(DEFAULT_MAX_DISTANCE_KM)
                .minTimeBetweenScansMinutes(DEFAULT_MIN_TIME_BETWEEN_SCANS_MINUTES)
                .activationAgeDays(DEFAULT_ACTIVATION_AGE_DAYS)
                .isActive(true)
                .createdAt(null)
                .updatedAt(null)
                .createdByName(null)
                .updatedByName(null)
                .build();
    }

    private User getUser(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hệ thống."));
        }
        return userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + currentUser.getUserId()));
    }

    private void publishActivityLog(CustomUserDetails currentUser, String action, String description,
            String entityType, String entityId) {
        if (currentUser == null) {
            return;
        }
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
