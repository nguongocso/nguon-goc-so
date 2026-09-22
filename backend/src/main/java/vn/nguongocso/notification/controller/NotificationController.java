package vn.nguongocso.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.notification.dto.response.MarkAllReadResponse;
import vn.nguongocso.notification.dto.response.NotificationResponse;
import vn.nguongocso.notification.dto.response.UnreadCountResponse;
import vn.nguongocso.notification.service.NotificationService;

/**
 * API quản lý hộp thông báo của người dùng.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lấy danh sách thông báo của người dùng.
     */
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                ApiResult.success(
                        notificationService.getNotifications(
                                isRead,
                                pageable)));
    }

    /**
     * Lấy số lượng thông báo chưa đọc.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResult<UnreadCountResponse>> getUnreadCount() {
        return ResponseEntity.ok(
                ApiResult.success(
                        notificationService.getUnreadCount()));
    }

    /**
     * Đánh dấu tất cả thông báo chưa đọc của người dùng là đã đọc.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResult<MarkAllReadResponse>> markAllAsRead() {
        return ResponseEntity.ok(
                ApiResult.success(
                        MarkAllReadResponse.builder()
                                .markedReadCount(notificationService.markAllAsRead())
                                .build()));
    }

    /**
     * Đánh dấu thông báo là đã đọc.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResult<NotificationResponse>> markAsRead(
            @PathVariable UUID notificationId) {
        return ResponseEntity.ok(
                ApiResult.success(
                        notificationService.markAsRead(notificationId)));
    }
}
