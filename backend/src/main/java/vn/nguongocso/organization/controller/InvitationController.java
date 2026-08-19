package vn.nguongocso.organization.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.organization.dto.request.AcceptInvitationRequest;
import vn.nguongocso.organization.dto.request.CreateInvitationRequest;
import vn.nguongocso.organization.dto.response.AcceptInvitationResponse;
import vn.nguongocso.organization.dto.response.InvitationPublicResponse;
import vn.nguongocso.organization.dto.response.InvitationResponse;
import vn.nguongocso.organization.service.InvitationService;

@Slf4j
@RestController
@RequestMapping
public class InvitationController {
        private final InvitationService invitationService;

        public InvitationController(InvitationService invitationService) {
                this.invitationService = invitationService;
        }

        /**
         * Quản lý hợp tác xã tạo thư mời gửi tới thành viên mới.
         *
         * @param request     thông tin người nhận và vai trò gán
         * @param currentUser thông tin quản lý đang đăng nhập
         * @return thông tin thư mời đã tạo thành công
         */
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

        /**
         * Lấy thông tin thư mời chi tiết từ Token (Public API).
         *
         * @param token mã thư mời trên đường link
         * @return thông tin cơ bản của thư mời
         */
        @GetMapping("/api/v1/public/organization/invitations/{token}")
        public ResponseEntity<ApiResult<InvitationPublicResponse>> getInvitationDetails(
                        @PathVariable String token) {
                log.info("Nhận yêu cầu kiểm tra token thư mời public: token={}", token);

                InvitationPublicResponse response = invitationService.getInvitationDetails(token);

                return ResponseEntity.ok(ApiResult.success(response));
        }

        /**
         * Người được mời đồng ý tham gia tổ chức, đăng ký tài khoản (Public API).
         *
         * @param token   mã thư mời
         * @param request thông tin tài khoản đăng ký mới
         * @return thông tin tài khoản và liên kết tổ chức thành công
         */
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
