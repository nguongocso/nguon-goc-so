package vn.nguongocso.trace.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.trace.service.HandoverExpiryService;

/** Scheduler tự động hết hạn phiếu bàn giao khi quá thời hạn xác nhận (NCL-05-CN-009). */
@Component
@Slf4j
@RequiredArgsConstructor
public class HandoverExpiryScheduler {
    private final HandoverExpiryService handoverExpiryService;

    @Value("${app.handover.expiry-check-cron:0 0 * * * ?}")
    private String expiryCheckCron;

    /** Quét và xử lý phiếu bàn giao hết hạn theo lịch cấu hình. */
    @Scheduled(cron = "${app.handover.expiry-check-cron:0 0 * * * ?}")
    public void processExpiredHandovers() {
        log.info("Bắt đầu chạy Scheduled Job: Quét phiếu bàn giao hết hạn");
        int count = handoverExpiryService.expireOverdueHandovers();
        log.info("Hoàn thành: {} phiếu bàn giao hết hạn đã được xử lý", count);
    }
}
