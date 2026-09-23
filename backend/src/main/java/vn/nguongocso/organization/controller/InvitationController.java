package vn.nguongocso.organization.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.InvitationResponse;
import vn.nguongocso.organization.service.InvitationService;

/** Controller quản lý thư mời tham gia tổ chức. */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;

    /** Quản lý hợp tác xã tạo thư mời gửi tới thành viên mới. */
    @PostMapping("/api/v1/organization/invitations")
    @PreAuthorize("hasAnyRole('VT-02')")
    public ResponseEntity<ApiResult<InvitationResponse>> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        log.info("Nhận yêu cầu gửi thư mời tới email={}, vai trò={} từ quản lý={}",
                request.getEmail(), request.getRoleId(), currentUser.getUsername());

        InvitationResponse response = invitationService.createInvitation(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
    }

    /** Lấy thông tin thư mời chi tiết từ token công khai. */
    @GetMapping("/api/v1/public/organization/invitations/{token}")
    public ResponseEntity<ApiResult<InvitationPublicResponse>> getInvitationDetails(
            @PathVariable String token) {
        log.info("Nhận yêu cầu kiểm tra token thư mời public: token={}", token);
        InvitationPublicResponse response = invitationService.getInvitationDetails(token);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /** Người được mời đồng ý tham gia tổ chức và đăng ký tài khoản. */
    @PostMapping("/api/v1/public/organization/invitations/{token}/accept")
    public ResponseEntity<ApiResult<AcceptInvitationResponse>> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request) {
        log.info("Nhận yêu cầu chấp nhận thư mời: token={}, username đăng ký={}",
                token, request.getUserName());

        AcceptInvitationResponse response = invitationService.acceptInvitation(token, request);
        return ResponseEntity.ok(ApiResult.success(response));
    }
}
