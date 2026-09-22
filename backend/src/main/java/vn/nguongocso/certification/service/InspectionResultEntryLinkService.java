package vn.nguongocso.certification.service;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryResponse;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;

import java.util.UUID;

/**
 * Service quản lý vòng đời liên kết nhập kết quả kiểm nghiệm (NCL-11-CN-007).
 */
public interface InspectionResultEntryLinkService {
        /**
         * Cấp mới hoặc cấp lại liên kết nhập kết quả kiểm nghiệm cho một yêu cầu (VT-02).
         */
        InspectionResultEntryLinkResponse issueLink(
                        UUID requestId,
                        IssueInspectionResultEntryLinkRequest request,
                        CustomUserDetails currentUser);

        /**
         * Lấy thông tin liên kết mới nhất của yêu cầu kiểm nghiệm (không trả secret token/URL).
         */
        InspectionResultEntryLinkResponse getLatestLink(
                        UUID requestId,
                        CustomUserDetails currentUser);

        /**
         * Lấy thông tin công khai của yêu cầu kiểm nghiệm qua token bí mật cho đơn vị kiểm nghiệm.
         */
        PublicInspectionResultEntryResponse getPublicPortalData(
                        String rawToken,
                        String clientIp);

        /**
         * Xác thực token và lấy thực thể liên kết đang ACTIVE còn hạn.
         */
        InspectionResultEntryLink validateAndGetActiveLink(
                        String rawToken,
                        String clientIp);

        /**
         * Thu hồi tất cả các liên kết ACTIVE của một yêu cầu kiểm nghiệm khi HTX ghi kết quả thủ công hoặc cấp lại.
         */
        void revokeActiveLinksForRequest(
                        UUID requestId,
                        User actor);

        /**
         * Băm chuỗi token thô bằng thuật toán SHA-256.
         */
        String hashToken(
                        String rawToken);
}
