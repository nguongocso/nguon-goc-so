package vn.nguongocso.trace.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.service.HandoverExpiryService;

/**
 * Triển khai xử lý phiếu bàn giao hết hạn.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HandoverExpiryServiceImpl implements HandoverExpiryService {

    private final ShipmentHandoverRepository handoverRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int expireOverdueHandovers() {
        List<ShipmentHandover> expired = handoverRepository.findExpiredPending(
                ShipmentHandoverStatus.PENDING_CONFIRMATION,
                LocalDateTime.now());

        for (ShipmentHandover handover : expired) {
            handover.setStatus(ShipmentHandoverStatus.EXPIRED);
            handoverRepository.save(handover);
            notifyExpired(handover);
            log.info("Phieu ban giao {} da duoc chuyen sang EXPIRED vi het han", handover.getId());
        }

        log.info("Hoàn thành: {} phieu ban giao het han da duoc xu ly", expired.size());
        return expired.size();
    }

    /**
     * Thông báo phiếu hết hạn tới cả hai tổ chức (AC NCL-05-CN-009 TC-03:
     * "quá hạn → EXPIRED, cả 2 bên nhận notification").
     */
    private void notifyExpired(ShipmentHandover handover) {
        String content = String.format(
                "Phiếu bàn giao %s kg lô hàng \"%s\" từ %s đã hết hạn chờ xác nhận.",
                handover.getQuantity(),
                handover.getShipment().getName(),
                handover.getFromOrganization().getName());

        notificationService.sendHandoverNotification(
                "Phiếu bàn giao đã hết hạn",
                content,
                handover.getId(),
                handover.getFromOrganization().getOrganizationId());

        notificationService.sendHandoverNotification(
                "Phiếu bàn giao đã hết hạn",
                String.format("%s Bên nhận chưa xác nhận trong thời hạn, số lượng được trả về tổ chức giao.",
                        content),
                handover.getId(),
                handover.getToOrganization().getOrganizationId());
    }
}