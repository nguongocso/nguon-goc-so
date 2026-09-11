package vn.nguongocso.trace.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.trace.service.HandoverExpiryService;

/**
 * Scheduler tự động hết hạn phiếu bàn giao khi quá thời hạn xác nhận.
 *
 * <p>Ủy quyền xử lý cho {@link HandoverExpiryService} (REQUIRES_NEW) để trạng thái
 * EXPIRED và thông báo tới cả hai tổ chức được ghi nhận bền vững (NCL-05-CN-009 TC-03).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HandoverExpiryScheduler {

    private final HandoverExpiryService handoverExpiryService;

    @Value("${app.handover.expiry-check-cron:0 0 * * * ?}")
    private String expiryCheckCron;

    @Scheduled(cron = "${app.handover.expiry-check-cron:0 0 * * * ?}")
    public void processExpiredHandovers() {
        log.info("Bat dau chay Scheduled Job: Quet phieu ban giao het han");
        int count = handoverExpiryService.expireOverdueHandovers();
        log.info("Hoan thanh: {} phieu ban giao het han da duoc xu ly", count);
    }
}