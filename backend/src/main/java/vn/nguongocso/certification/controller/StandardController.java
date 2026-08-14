package vn.nguongocso.certification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateStandardRequest;
import vn.nguongocso.certification.dto.request.InspectionCriterionRequest;
import vn.nguongocso.certification.dto.request.UpdateStandardRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResponse;
import vn.nguongocso.certification.dto.response.StandardResponse;
import vn.nguongocso.certification.service.InspectionCriterionDefinitionService;
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
        private final InspectionCriterionDefinitionService inspectionCriterionDefinitionService;

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

        @GetMapping("/{standardId}/criteria")
        @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
        public ResponseEntity<ApiResult<List<InspectionCriterionResponse>>> getCriteriaByStandard(
                        @PathVariable UUID standardId,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                List<InspectionCriterionResponse> response =
                                inspectionCriterionDefinitionService.getCriteriaByStandard(standardId, currentUser);

                return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
        }

        @PostMapping("/{standardId}/criteria")
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<InspectionCriterionResponse>> createCriteria(
                        @PathVariable UUID standardId,
                        @Valid @RequestBody InspectionCriterionRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                request.setStandardId(standardId);
                InspectionCriterionResponse response =
                                inspectionCriterionDefinitionService.createCriteria(standardId, request, currentUser);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
        }

        @PutMapping("/{standardId}/criteria/{criteriaId}")
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<InspectionCriterionResponse>> updateCriteria(
                        @PathVariable UUID standardId,
                        @PathVariable Integer criteriaId,
                        @Valid @RequestBody InspectionCriterionRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                request.setStandardId(standardId);
                InspectionCriterionResponse response =
                                inspectionCriterionDefinitionService.updateCriteria(standardId, criteriaId, request, currentUser);

                return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
        }

        @DeleteMapping("/{standardId}/criteria/{criteriaId}")
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<Void>> deleteCriteria(
                        @PathVariable UUID standardId,
                        @PathVariable Integer criteriaId,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                inspectionCriterionDefinitionService.deleteCriteria(standardId, criteriaId, currentUser);

                return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), null));
        }

}