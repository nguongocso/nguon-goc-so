package vn.nguongocso.trace.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.trace.entity.ShipmentHandover;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;

/**
 * Scheduler tự động hết hạn phiếu bàn giao khi quá thời hạn xác nhận.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HandoverExpiryScheduler {

    private final ShipmentHandoverRepository handoverRepository;

    @Value("${app.handover.expiry-check-cron:0 0 * * * ?}")
    private String expiryCheckCron;

    @Scheduled(cron = "${app.handover.expiry-check-cron:0 0 * * * ?}")
    public void processExpiredHandovers() {
        log.info("Bat dau chay Scheduled Job: Quet phieu ban giao het han");

        List<ShipmentHandover> expired = handoverRepository.findExpiredPending(
                ShipmentHandoverStatus.PENDING_CONFIRMATION,
                LocalDateTime.now()
        );

        for (ShipmentHandover handover : expired) {
            handover.setStatus(ShipmentHandoverStatus.EXPIRED);
            handoverRepository.save(handover);
            log.info("Phieu ban giao {} da het han", handover.getId());
        }

        log.info("Hoan thanh: {} phieu het han da xu ly", expired.size());
    }
}
