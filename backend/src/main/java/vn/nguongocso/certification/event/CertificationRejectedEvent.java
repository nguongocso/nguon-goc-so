package vn.nguongocso.certification.event;

import java.util.UUID;

/** Sự kiện yêu cầu gửi thông báo sau khi quyết định từ chối đã commit. */
public record CertificationRejectedEvent(UUID certificationId, String rejectionReason) {
}
