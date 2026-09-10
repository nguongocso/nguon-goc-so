package vn.nguongocso.certification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.event.CertificationRejectedEvent;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.notification.service.NotificationService;

/** Gửi thông báo từ chối trong giao dịch độc lập sau khi quyết định đã được lưu. */
@Component
@Slf4j
@RequiredArgsConstructor
public class CertificationRejectionNotificationListener {

    private final CertificationRepository certificationRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CertificationRejectedEvent event) {
        try {
            Certification certification = certificationRepository.findById(event.certificationId())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy chứng nhận sau khi từ chối."));
            int notifiedCount = notificationService.sendCertificationRejectionNotification(
                    certification,
                    event.rejectionReason());
            log.info("Đã gửi {} thông báo từ chối cho chứng nhận {}.", notifiedCount, event.certificationId());
        } catch (Exception ex) {
            log.error("Không thể gửi thông báo từ chối cho chứng nhận {}: {}",
                    event.certificationId(), ex.getMessage(), ex);
        }
    }
}
