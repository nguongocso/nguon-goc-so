package vn.nguongocso.alert.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.entity.ActivityLog;
import vn.nguongocso.alert.repository.ActivityLogRepository;

/**
 * Lắng nghe sự kiện ghi nhật ký hoạt động của người dùng và lưu vào cơ sở dữ
 * liệu.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityLogListener {
    private final ActivityLogRepository activityLogRepository;

    /**
     * Xử lý sự kiện ActivityLogEvent và lưu thông tin vào cơ sở dữ liệu.
     *
     * @param event sự kiện ghi nhật ký hoạt động
     */
    @Async // Thực thi bất đồng bộ trên một Thread Pool riêng biệt
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Tạo transaction mới hoàn toàn biệt lập
    public void handleActivityLogEvent(ActivityLogEvent event) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .organizationId(event.getOrganizationId())
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .fullName(event.getFullName())
                    .action(event.getAction())
                    .description(event.getDescription())
                    .entityType(event.getEntityType())
                    .entityId(event.getEntityId())
                    .ipAddress(event.getIpAddress())
                    .createdAt(event.getTimestamp())
                    .build();

            activityLogRepository.save(activityLog);
            log.debug("Lưu vết thao tác thành công: {} bởi {}", event.getAction(), event.getUsername());
        } catch (Exception e) {
            log.error("Không thể ghi nhật ký hoạt động vào database: {}", e.getMessage(), e);
        }
    }
}
