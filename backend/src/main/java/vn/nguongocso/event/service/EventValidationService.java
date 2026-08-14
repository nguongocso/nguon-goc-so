package vn.nguongocso.event.service;

import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.dto.response.FailedEventLogResponse;
import vn.nguongocso.event.dto.response.LotValidationResponse;
import vn.nguongocso.event.enums.ChainEventType;

import java.util.UUID;

/**
 * Service xác thực sự kiện chuỗi cung ứng.
 *
 * @author Triệu Văn Đại
 */
public interface EventValidationService {

    // Kiểm tra tính hợp lệ của lô trước khi tạo sự kiện.
    LotValidationResponse validateLot(UUID lotId, ChainEventType eventType, CustomUserDetails currentUser);

    // Xóa bản nháp sự kiện/lô hàng sai lô.
    void deleteDraft(UUID draftId, CustomUserDetails currentUser);

    // Lấy danh sách log sự kiện bị chặn.
    PageResponse<FailedEventLogResponse> getFailedLogs(Pageable pageable);

    // Ghi log lần thử sự kiện bị thất bại.
    void logFailedAttempt(UUID lotId, String lotCode, ChainEventType eventType, String reason,
            CustomUserDetails currentUser);
}