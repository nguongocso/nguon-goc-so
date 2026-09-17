package vn.nguongocso.certification.service;

import java.util.UUID;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.IssueInspectionResultEntryLinkRequest;
import vn.nguongocso.certification.dto.response.InspectionResultEntryLinkResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryResponse;
import vn.nguongocso.certification.entity.InspectionResultEntryLink;

/**
 * Giao diện dịch vụ quản lý vòng đời liên kết nhập kết quả kiểm nghiệm (NCL-11-CN-007).
 */
public interface InspectionResultEntryLinkService {

    /**
     * Cấp mới hoặc cấp lại liên kết nhập kết quả kiểm nghiệm cho một yêu cầu (VT-02).
     *
     * @param requestId   ID yêu cầu kiểm nghiệm.
     * @param request     DTO chứa email người nhận và số ngày hiệu lực.
     * @param currentUser Người dùng Quản lý HTX đang đăng nhập.
     * @return DTO chứa thông tin liên kết và đường dẫn bí mật trả một lần duy nhất.
     */
    InspectionResultEntryLinkResponse issueLink(
            UUID requestId,
            IssueInspectionResultEntryLinkRequest request,
            CustomUserDetails currentUser);

    /**
     * Lấy thông tin liên kết mới nhất của yêu cầu kiểm nghiệm (không trả secret token/URL).
     *
     * @param requestId   ID yêu cầu kiểm nghiệm.
     * @param currentUser Người dùng Quản lý HTX đang đăng nhập.
     * @return DTO chứa thông tin liên kết mới nhất.
     */
    InspectionResultEntryLinkResponse getLatestLink(
            UUID requestId,
            CustomUserDetails currentUser);

    /**
     * Lấy thông tin công khai của yêu cầu kiểm nghiệm qua token bí mật cho đơn vị kiểm nghiệm.
     *
     * @param rawToken Mã token bí mật truyền qua URL.
     * @param clientIp Địa chỉ IP của client gọi yêu cầu.
     * @return DTO thông tin tối thiểu của yêu cầu và danh sách chỉ tiêu.
     */
    PublicInspectionResultEntryResponse getPublicPortalData(
            String rawToken,
            String clientIp);

    /**
     * Xác thực token và lấy thực thể liên kết đang ACTIVE còn hạn.
     *
     * @param rawToken Mã token bí mật truyền qua URL.
     * @param clientIp Địa chỉ IP của client gọi yêu cầu.
     * @return Thực thể liên kết hợp lệ.
     */
    InspectionResultEntryLink validateAndGetActiveLink(
            String rawToken,
            String clientIp);

    /**
     * Thu hồi tất cả các liên kết ACTIVE của một yêu cầu kiểm nghiệm khi HTX ghi kết quả thủ công hoặc cấp lại.
     *
     * @param requestId ID yêu cầu kiểm nghiệm.
     * @param actor     Người thực hiện thu hồi.
     */
    void revokeActiveLinksForRequest(UUID requestId, User actor);

    /**
     * Băm chuỗi token thô bằng thuật toán SHA-256.
     *
     * @param rawToken Chuỗi token thô.
     * @return Chuỗi băm hex 64 ký tự.
     */
    String hashToken(String rawToken);
}
