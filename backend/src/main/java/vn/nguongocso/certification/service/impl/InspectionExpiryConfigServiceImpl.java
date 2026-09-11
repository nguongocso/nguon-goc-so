package vn.nguongocso.certification.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionExpiryThresholdRequest;
import vn.nguongocso.certification.dto.response.InspectionExpiryThresholdResponse;
import vn.nguongocso.certification.entity.SystemConfiguration;
import vn.nguongocso.certification.repository.SystemConfigurationRepository;
import vn.nguongocso.certification.service.InspectionExpiryConfigService;
import vn.nguongocso.exception.BusinessException;

/**
 * Triển khai dịch vụ quản lý cấu hình ngưỡng cảnh báo hết hiệu lực kiểm nghiệm (NCL-11-CN-004).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InspectionExpiryConfigServiceImpl implements InspectionExpiryConfigService {

    public static final String CONFIG_KEY = "INSPECTION_EXPIRY_WARNING_THRESHOLD_DAYS";
    public static final int FALLBACK_DEFAULT_DAYS = 15;

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final UserRepository userRepository;

    @Value("${app.inspection.expiry-warning-threshold-days:15}")
    private int defaultWarningThresholdDays;

    @Override
    @Transactional(readOnly = true)
    public int getWarningThresholdDays() {
        return systemConfigurationRepository.findById(CONFIG_KEY)
                .map(config -> {
                    try {
                        int days = Integer.parseInt(config.getConfigValue().trim());
                        if (days > 0) {
                            return days;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Giá trị cấu hình {} không hợp lệ: '{}'. Dùng giá trị mặc định.",
                                CONFIG_KEY, config.getConfigValue());
                    }
                    return defaultWarningThresholdDays > 0 ? defaultWarningThresholdDays : FALLBACK_DEFAULT_DAYS;
                })
                .orElse(defaultWarningThresholdDays > 0 ? defaultWarningThresholdDays : FALLBACK_DEFAULT_DAYS);
    }

    @Override
    @Transactional(readOnly = true)
    public InspectionExpiryThresholdResponse getThresholdConfig() {
        int thresholdDays = getWarningThresholdDays();
        LocalDateTime updatedAt = null;
        String updatedByName = null;

        var optConfig = systemConfigurationRepository.findById(CONFIG_KEY);
        if (optConfig.isPresent()) {
            SystemConfiguration config = optConfig.get();
            updatedAt = config.getUpdatedAt();
            if (config.getUpdatedBy() != null) {
                updatedByName = config.getUpdatedBy().getFullName() != null
                        ? config.getUpdatedBy().getFullName()
                        : config.getUpdatedBy().getUserName();
            }
        }

        return InspectionExpiryThresholdResponse.builder()
                .warningThresholdDays(thresholdDays)
                .updatedAt(updatedAt)
                .updatedByName(updatedByName)
                .build();
    }

    @Override
    @Transactional
    public InspectionExpiryThresholdResponse updateThresholdConfig(
            InspectionExpiryThresholdRequest request,
            CustomUserDetails currentUser) {

        if (request == null || request.getWarningThresholdDays() == null) {
            throw new BusinessException("Ngưỡng cảnh báo không được để trống.");
        }

        int newThreshold = request.getWarningThresholdDays();
        if (newThreshold < 1 || newThreshold > 365) {
            throw new BusinessException("Ngưỡng cảnh báo phải nằm trong khoảng từ 1 đến 365 ngày.");
        }

        User user = null;
        if (currentUser != null && currentUser.getUserId() != null) {
            user = userRepository.findById(currentUser.getUserId()).orElse(null);
        }

        SystemConfiguration config = systemConfigurationRepository.findById(CONFIG_KEY)
                .orElseGet(() -> SystemConfiguration.builder()
                        .configKey(CONFIG_KEY)
                        .description("Ngưỡng số ngày cảnh báo kết quả kiểm nghiệm sắp hết hiệu lực (mặc định 15 ngày)")
                        .build());

        config.setConfigValue(String.valueOf(newThreshold));
        config.setUpdatedBy(user);
        config.setUpdatedAt(LocalDateTime.now());

        SystemConfiguration saved = systemConfigurationRepository.save(config);

        String updatedByName = null;
        if (user != null) {
            updatedByName = user.getFullName() != null ? user.getFullName() : user.getUserName();
        }

        log.info("Quản trị viên {} đã cập nhật ngưỡng cảnh báo kiểm nghiệm: {} ngày",
                updatedByName != null ? updatedByName : "Admin", newThreshold);

        return InspectionExpiryThresholdResponse.builder()
                .warningThresholdDays(newThreshold)
                .updatedAt(saved.getUpdatedAt())
                .updatedByName(updatedByName)
                .build();
    }
}
