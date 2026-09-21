package vn.nguongocso.organization.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.InvitationResponse;

/**
 * Quản lý thư mời tham gia tổ chức.
 */
public interface InvitationService {

    /**
     * Tạo thư mời mới.
     *
     * @param request     thông tin tạo thư mời
     * @param currentUser người dùng hiện tại
     * @return phản hồi chi tiết thư mời
     */
    InvitationResponse createInvitation(CreateInvitationRequest request, CustomUserDetails currentUser);

    /**
     * Lấy thông tin thư mời công khai.
     *
     * @param token mã token của thư mời
     * @return thông tin công khai của thư mời
     */
    InvitationPublicResponse getInvitationDetails(String token);

    /**
     * Chấp nhận thư mời.
     *
     * @param token   mã token của thư mời
     * @param request thông tin tài khoản chấp nhận thư mời
     * @return kết quả chấp nhận thư mời
     */
    AcceptInvitationResponse acceptInvitation(String token, AcceptInvitationRequest request);
}
