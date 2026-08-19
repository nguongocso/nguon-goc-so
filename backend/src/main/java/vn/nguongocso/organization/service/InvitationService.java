package vn.nguongocso.organization.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.response.InvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;

/** Quản lý thư mời tham gia tổ chức. */
public interface InvitationService {
    /** Tạo thư mời mới. */
    InvitationResponse createInvitation(CreateInvitationRequest request, CustomUserDetails currentUser);

    /** Lấy thông tin thư mời công khai. */
    InvitationPublicResponse getInvitationDetails(String token);

    /** Chấp nhận thư mời. */
    AcceptInvitationResponse acceptInvitation(String token, AcceptInvitationRequest request);
}
