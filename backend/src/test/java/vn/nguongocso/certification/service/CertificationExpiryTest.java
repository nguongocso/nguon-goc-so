package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertSeverity;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.certification.service.impl.CertificationServiceImpl;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class CertificationExpiryTest {

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CertificationServiceImpl certificationService;

    private Certification certExpiring;
    private Certification certExpired;
    private Organization organization;

    @BeforeEach
    void setUp() {
        // Set warning threshold to 30 days
        ReflectionTestUtils.setField(certificationService, "warningThresholdDays", 30);

        organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setName("HTX Nông Nghiệp Sạch");

        certExpiring = Certification.builder()
                .id(UUID.randomUUID())
                .name("GlobalGAP Buoi")
                .code("GG-BUOI-01")
                .expiryDate(LocalDate.now().plusDays(10)) // 10 days left (<= 30 warning threshold)
                .organization(organization)
                .build();

        certExpired = Certification.builder()
                .id(UUID.randomUUID())
                .name("VietGAP Cam")
                .code("VG-CAM-01")
                .expiryDate(LocalDate.now().minusDays(5)) // already expired 5 days ago
                .organization(organization)
                .build();
    }

    @Test
    void checkCertificationExpiry_shouldCreateExpiringAlert_whenCertExpiringAndAlertNotExists() throws Exception {
        // Given
        when(certificationRepository.findAll()).thenReturn(List.of(certExpiring));
        when(alertRepository.existsByRelatedEntityIdAndTypeAndStatus(
                certExpiring.getId(),
                AlertType.CERT_EXPIRING,
                AlertStatus.PENDING
        )).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        certificationService.checkCertificationExpiry();

        // Then
        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(notificationService, times(1)).sendCertificationExpiryNotification(any(Alert.class));
    }

    @Test
    void checkCertificationExpiry_shouldCreateExpiredAlertAndResolveExpiringAlert_whenCertExpired() throws Exception {
        // Given
        when(certificationRepository.findAll()).thenReturn(List.of(certExpired));
        when(alertRepository.existsByRelatedEntityIdAndTypeAndStatus(
                certExpired.getId(),
                AlertType.CERT_EXPIRED,
                AlertStatus.PENDING
        )).thenReturn(false);

        Alert pendingExpiringAlert = new Alert();
        pendingExpiringAlert.setId(UUID.randomUUID());
        pendingExpiringAlert.setType(AlertType.CERT_EXPIRING);
        pendingExpiringAlert.setRelatedEntityId(certExpired.getId());
        pendingExpiringAlert.setStatus(AlertStatus.PENDING);

        when(alertRepository.findByRelatedEntityIdAndTypeAndStatus(
                certExpired.getId(),
                AlertType.CERT_EXPIRING,
                AlertStatus.PENDING
        )).thenReturn(List.of(pendingExpiringAlert));

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        certificationService.checkCertificationExpiry();

        // Then
        // Verify expiring alert was resolved
        verify(alertRepository, times(1)).save(pendingExpiringAlert);
        assertThat(pendingExpiringAlert.getStatus()).isEqualTo(AlertStatus.RESOLVED);

        // Verify expired alert was saved and notification sent
        verify(alertRepository, times(1)).save(argThat(alert -> alert.getType() == AlertType.CERT_EXPIRED));
        verify(notificationService, times(1)).sendCertificationExpiryNotification(argThat(alert -> alert.getType() == AlertType.CERT_EXPIRED));
    }
}
