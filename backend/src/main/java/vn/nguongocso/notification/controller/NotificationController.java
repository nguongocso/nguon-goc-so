package vn.nguongocso.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;
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
        private final NotificationService alertNotificationService;

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
                                                alertNotificationService.getNotifications(
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
                                                alertNotificationService.getUnreadCount()));
        }

        /**
         * Đánh dấu thông báo là đã đọc.
         */
        @PatchMapping("/{notificationId}/read")
        public ResponseEntity<ApiResult<NotificationResponse>> markAsRead(
                        @PathVariable UUID notificationId) {

                return ResponseEntity.ok(
                                ApiResult.success(
                                                alertNotificationService.markAsRead(notificationId)));
        }
}