package vn.nguongocso.certification.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateStandardRequest;
import vn.nguongocso.certification.dto.request.UpdateStandardRequest;
import vn.nguongocso.certification.dto.response.StandardResponse;
import vn.nguongocso.certification.service.StandardService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/**
 * Controller quản lý danh mục tiêu chuẩn chất lượng.
 */
@RestController
@RequestMapping("/api/v1/standards")
@RequiredArgsConstructor
public class StandardController {
        private final StandardService standardService;

        /**
         * Thêm mới tiêu chuẩn chất lượng.
         */
        @PostMapping
        public ApiResult<StandardResponse> createStandard(
                        @Valid @RequestBody CreateStandardRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                return ApiResult.success(
                                201,
                                standardService.createStandard(request, currentUser));
        }

        /**
         * Cập nhật thông tin tiêu chuẩn.
         */
        @PutMapping("/{standardId}")
        public ApiResult<StandardResponse> updateStandard(
                        @PathVariable UUID standardId,
                        @Valid @RequestBody UpdateStandardRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                return ApiResult.success(
                                standardService.updateStandard(
                                                standardId,
                                                request,
                                                currentUser));
        }

        /**
         * Lấy danh sách tiêu chuẩn chất lượng.
         */
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ApiResult<PageResponse<StandardResponse>> getStandards(
                        @RequestParam(required = false) Boolean isActive,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                Pageable pageable = PageRequest.of(page, size);

                Page<StandardResponse> result = standardService.getStandards(
                                isActive,
                                pageable,
                                currentUser);

                return ApiResult.success(
                                PageResponse.from(result, result.getContent()));
        }

}