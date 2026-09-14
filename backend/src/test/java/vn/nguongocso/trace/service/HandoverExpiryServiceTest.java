package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.service.impl.HandoverExpiryServiceImpl;

@ExtendWith(MockitoExtension.class)
class HandoverExpiryServiceTest {

    @Mock
    private ShipmentHandoverRepository handoverRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private HandoverExpiryServiceImpl expiryService;

    private Organization fromOrganization;
    private Organization toOrganization;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        fromOrganization = new Organization();
        fromOrganization.setOrganizationId(UUID.randomUUID());
        fromOrganization.setName("From Org");

        toOrganization = new Organization();
        toOrganization.setOrganizationId(UUID.randomUUID());
        toOrganization.setName("To Org");

        shipment = new Shipment();
        shipment.setId(UUID.randomUUID());
        shipment.setName("Test Shipment");
        shipment.setTotalQuantity(10000L);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
    }

    private ShipmentHandover pendingHandover(UUID id, LocalDateTime expiresAt) {
        return ShipmentHandover.builder()
                .id(id)
                .shipment(shipment)
                .fromOrganization(fromOrganization)
                .toOrganization(toOrganization)
                .quantity(800L)
                .status(ShipmentHandoverStatus.PENDING_CONFIRMATION)
                .expiresAt(expiresAt)
                .createdBy(mock(User.class))
                .build();
    }

    @Test
    void testExpireOverdueHandovers_ConvertsAndNotifiesBoth() {
        UUID expiredId = UUID.randomUUID();
        ShipmentHandover expired = pendingHandover(expiredId, LocalDateTime.now().minusHours(3));
        ShipmentHandover future = pendingHandover(UUID.randomUUID(), LocalDateTime.now().plusHours(24));

        when(handoverRepository.findExpiredPending(any(), any())).thenReturn(List.of(expired));
        when(handoverRepository.save(any(ShipmentHandover.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = expiryService.expireOverdueHandovers();

        assertThat(count).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(ShipmentHandoverStatus.EXPIRED);
        assertThat(future.getStatus()).isEqualTo(ShipmentHandoverStatus.PENDING_CONFIRMATION);
        // Chỉ phiếu quá hạn được lưu lại.
        verify(handoverRepository, times(1)).save(expired);
        // Cả hai tổ chức nhận thông báo với entityId = phiếu (AC TC-03).
        verify(notificationService).sendHandoverNotification(
                any(), any(), eq(expiredId), eq(fromOrganization.getOrganizationId()));
        verify(notificationService).sendHandoverNotification(
                any(), any(), eq(expiredId), eq(toOrganization.getOrganizationId()));
    }

    @Test
    void testExpireOverdueHandovers_NoOverdue_Noop() {
        when(handoverRepository.findExpiredPending(any(), any())).thenReturn(List.of());

        int count = expiryService.expireOverdueHandovers();

        assertThat(count).isZero();
        verify(handoverRepository, never()).save(any());
        verify(notificationService, never()).sendHandoverNotification(any(), any(), any(), any());
    }
}