package vn.nguongocso.event.service.recorder;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.FailedEventLog;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.FailedEventLogRepository;
import vn.nguongocso.exception.BusinessException;

/**
 * Component chuyên trách ghi nhận vết sự kiện thất bại vào cơ sở dữ liệu.
 *
 * <p>Tham gia vào transaction độc lập do Service gọi tạo ra. Không tự ý mở thêm
 * transaction annotation trên component.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailedEventLogRecorder {
    private final UserRepository userRepository;
    private final FailedEventLogRepository failedEventLogRepository;

    /**
     * Ghi nhận một lần thử sự kiện thất bại vào bảng failed_event_logs.
     *
     * @param lotId       ID lô hàng hoặc lô sản xuất
     * @param lotCode     mã lô hàng hoặc mã lô sản xuất
     * @param eventType   loại sự kiện chuỗi cung ứng
     * @param reason      nguyên nhân thất bại
     * @param currentUser thông tin người dùng đang thao tác
     */
    public void recordFailedAttempt(UUID lotId, String lotCode, ChainEventType eventType, String reason,
            CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng."));

        FailedEventLog logEntry = FailedEventLog.builder()
                .user(user)
                .eventType(eventType)
                .lotId(lotId)
                .lotCode(lotCode)
                .failureReason(reason)
                .attemptedAt(LocalDateTime.now())
                .build();

        failedEventLogRepository.save(logEntry);
        log.warn("Đã ghi nhận sự kiện thất bại: eventType={}, lotCode={}, reason={}",
                eventType, lotCode, reason);
    }
}
