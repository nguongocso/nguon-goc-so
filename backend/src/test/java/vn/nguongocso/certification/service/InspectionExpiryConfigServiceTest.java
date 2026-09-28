package vn.nguongocso.certification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionExpiryThresholdRequest;
import vn.nguongocso.certification.dto.response.InspectionExpiryThresholdResponse;
import vn.nguongocso.certification.entity.SystemConfiguration;
import vn.nguongocso.certification.repository.SystemConfigurationRepository;
import vn.nguongocso.certification.service.impl.InspectionExpiryConfigServiceImpl;
import vn.nguongocso.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho dịch vụ cấu hình ngưỡng cảnh báo hết hiệu lực kiểm nghiệm (NCL-11-CN-004).
 */
@ExtendWith(MockitoExtension.class)
class InspectionExpiryConfigServiceTest {

    @Mock
    private SystemConfigurationRepository systemConfigurationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InspectionExpiryConfigServiceImpl inspectionExpiryConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inspectionExpiryConfigService, "defaultWarningThresholdDays", 15);
    }

    @Test
    @DisplayName("Lấy ngưỡng cảnh báo: đã có cấu hình hợp lệ trong database -> trả về giá trị từ DB")
    void testGetWarningThresholdDays_fromDatabase() {
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey(InspectionExpiryConfigServiceImpl.CONFIG_KEY)
                .configValue("20")
                .build();
        when(systemConfigurationRepository.findById(InspectionExpiryConfigServiceImpl.CONFIG_KEY))
                .thenReturn(Optional.of(config));

        int threshold = inspectionExpiryConfigService.getWarningThresholdDays();
        assertThat(threshold).isEqualTo(20);
    }

    @Test
    @DisplayName("Lấy ngưỡng cảnh báo: chưa có trong database -> trả về giá trị mặc định 15 ngày")
    void testGetWarningThresholdDays_defaultFallback() {
        when(systemConfigurationRepository.findById(InspectionExpiryConfigServiceImpl.CONFIG_KEY))
                .thenReturn(Optional.empty());

        int threshold = inspectionExpiryConfigService.getWarningThresholdDays();
        assertThat(threshold).isEqualTo(15);
    }

    @Test
    @DisplayName("Lấy ngưỡng cảnh báo: giá trị trong DB không hợp lệ (chuỗi không phải số) -> fallback về mặc định")
    void testGetWarningThresholdDays_invalidStringInDb() {
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey(InspectionExpiryConfigServiceImpl.CONFIG_KEY)
                .configValue("abc")
                .build();
        when(systemConfigurationRepository.findById(InspectionExpiryConfigServiceImpl.CONFIG_KEY))
                .thenReturn(Optional.of(config));

        int threshold = inspectionExpiryConfigService.getWarningThresholdDays();
        assertThat(threshold).isEqualTo(15);
    }

    @Test
    @DisplayName("Lấy chi tiết cấu hình: trả về đầy đủ thông tin số ngày, thời gian và người cập nhật")
    void testGetThresholdConfig() {
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        admin.setFullName("Quản trị viên hệ thống");

        LocalDateTime now = LocalDateTime.now();
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey(InspectionExpiryConfigServiceImpl.CONFIG_KEY)
                .configValue("30")
                .updatedAt(now)
                .updatedBy(admin)
                .build();

        when(systemConfigurationRepository.findById(InspectionExpiryConfigServiceImpl.CONFIG_KEY))
                .thenReturn(Optional.of(config));

        InspectionExpiryThresholdResponse response = inspectionExpiryConfigService.getThresholdConfig();

        assertThat(response).isNotNull();
        assertThat(response.getWarningThresholdDays()).isEqualTo(30);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedByName()).isEqualTo("Quản trị viên hệ thống");
    }

    @Test
    @DisplayName("Cập nhật ngưỡng cảnh báo thành công: lưu vào DB và trả về thông tin cập nhật")
    void testUpdateThresholdConfig_success() {
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setUserId(adminId);
        admin.setFullName("Admin Nền Tảng");

        CustomUserDetails currentUser = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(currentUser.getUserId()).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        SystemConfiguration existing = SystemConfiguration.builder()
                .configKey(InspectionExpiryConfigServiceImpl.CONFIG_KEY)
                .configValue("15")
                .build();
        when(systemConfigurationRepository.findById(InspectionExpiryConfigServiceImpl.CONFIG_KEY))
                .thenReturn(Optional.of(existing));
        when(systemConfigurationRepository.save(any(SystemConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InspectionExpiryThresholdRequest request = InspectionExpiryThresholdRequest.builder()
                .warningThresholdDays(25)
                .build();

        InspectionExpiryThresholdResponse response = inspectionExpiryConfigService.updateThresholdConfig(request, currentUser);

        assertThat(response).isNotNull();
        assertThat(response.getWarningThresholdDays()).isEqualTo(25);
        assertThat(response.getUpdatedByName()).isEqualTo("Admin Nền Tảng");
        verify(systemConfigurationRepository).save(any(SystemConfiguration.class));
    }

    @Test
    @DisplayName("Cập nhật ngưỡng cảnh báo: request null hoặc trường bị thiếu -> ném BusinessException")
    void testUpdateThresholdConfig_nullRequest() {
        assertThatThrownBy(() -> inspectionExpiryConfigService.updateThresholdConfig(null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được để trống");

        InspectionExpiryThresholdRequest emptyReq = new InspectionExpiryThresholdRequest();
        assertThatThrownBy(() -> inspectionExpiryConfigService.updateThresholdConfig(emptyReq, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    @DisplayName("Cập nhật ngưỡng cảnh báo: giá trị nhỏ hơn 1 hoặc lớn hơn 365 -> ném BusinessException")
    void testUpdateThresholdConfig_outOfRange() {
        InspectionExpiryThresholdRequest reqZero = InspectionExpiryThresholdRequest.builder()
                .warningThresholdDays(0)
                .build();
        assertThatThrownBy(() -> inspectionExpiryConfigService.updateThresholdConfig(reqZero, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("từ 1 đến 365 ngày");

        InspectionExpiryThresholdRequest reqTooLarge = InspectionExpiryThresholdRequest.builder()
                .warningThresholdDays(366)
                .build();
        assertThatThrownBy(() -> inspectionExpiryConfigService.updateThresholdConfig(reqTooLarge, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("từ 1 đến 365 ngày");
    }
}
