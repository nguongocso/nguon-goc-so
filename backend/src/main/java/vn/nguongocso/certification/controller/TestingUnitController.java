package vn.nguongocso.certification.controller;

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
import vn.nguongocso.certification.dto.request.CreateTestingUnitRequest;
import vn.nguongocso.certification.dto.request.UpdateAccreditationScopeRequest;
import vn.nguongocso.certification.dto.request.UpdateTestingUnitRequest;
import vn.nguongocso.certification.dto.response.AccreditationScopeSummaryResponse;
import vn.nguongocso.certification.dto.response.TestingUnitResponse;
import vn.nguongocso.certification.service.AccreditationScopeService;
import vn.nguongocso.certification.service.TestingUnitService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

/**
 * Controller quản lý danh mục đơn vị kiểm nghiệm dùng chung.
 * <p>
 * QTN-17: chỉ Quản trị viên nền tảng (VT-01) được chỉnh sửa danh mục dùng chung;
 * các vai trò khác chỉ được tra cứu.
 */
@RestController
@RequestMapping("/api/v1/testing-units")
@RequiredArgsConstructor
public class TestingUnitController {

        private final TestingUnitService testingUnitService;

        private final AccreditationScopeService accreditationScopeService;

        /**
         * Tạo mới đơn vị kiểm nghiệm trong danh mục dùng chung.
         *
         * POST /api/v1/testing-units
         */
        @PostMapping
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<TestingUnitResponse>> createTestingUnit(
                        @Valid @RequestBody CreateTestingUnitRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                TestingUnitResponse response =
                                testingUnitService.createTestingUnit(request, currentUser);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResult.success(HttpStatus.CREATED.value(), response));
        }

        /**
         * Cập nhật thông tin một đơn vị kiểm nghiệm.
         *
         * PUT /api/v1/testing-units/{testingUnitId}
         */
        @PutMapping("/{testingUnitId}")
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<TestingUnitResponse>> updateTestingUnit(
                        @PathVariable UUID testingUnitId,
                        @Valid @RequestBody UpdateTestingUnitRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                TestingUnitResponse response =
                                testingUnitService.updateTestingUnit(testingUnitId, request, currentUser);

                return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), response));
        }

        /**
         * Lấy danh sách đơn vị kiểm nghiệm (phân trang, lọc theo trạng thái).
         *
         * GET /api/v1/testing-units?isActive=&page=&size=
         */
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<PageResponse<TestingUnitResponse>>> getTestingUnits(
                        @RequestParam(name = "isActive", required = false) Boolean isActive,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                Pageable pageable = PageRequest.of(page, size);

                Page<TestingUnitResponse> result =
                                testingUnitService.getTestingUnits(isActive, pageable, currentUser);

                return ResponseEntity.ok(
                                ApiResult.success(
                                                HttpStatus.OK.value(),
                                                PageResponse.from(result, result.getContent())));
        }

        /**
         * Lấy phạm vi công nhận của một đơn vị kiểm nghiệm.
         *
         * GET /api/v1/testing-units/{testingUnitId}/accreditation-scopes
         */
        @GetMapping("/{testingUnitId}/accreditation-scopes")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<AccreditationScopeSummaryResponse>> getAccreditationScopes(
                        @PathVariable UUID testingUnitId) {

                AccreditationScopeSummaryResponse response =
                                accreditationScopeService.getAccreditationScope(testingUnitId);

                return ResponseEntity.ok(
                                ApiResult.success(HttpStatus.OK.value(), response));
        }

        /**
         * Cập nhật (REPLACE-ALL) phạm vi công nhận của một đơn vị kiểm nghiệm.
         *
         * PUT /api/v1/testing-units/{testingUnitId}/accreditation-scopes
         */
        @PutMapping("/{testingUnitId}/accreditation-scopes")
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<AccreditationScopeSummaryResponse>> updateAccreditationScopes(
                        @PathVariable UUID testingUnitId,
                        @Valid @RequestBody UpdateAccreditationScopeRequest request,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                AccreditationScopeSummaryResponse response =
                                accreditationScopeService.updateAccreditationScope(
                                                testingUnitId,
                                                request.getCriterionDefinitionIds(),
                                                currentUser);

                return ResponseEntity.ok(
                                ApiResult.success(HttpStatus.OK.value(), response));
        }

        /**
         * Vô hiệu hoá đơn vị kiểm nghiệm (soft delete, isActive = false).
         *
         * DELETE /api/v1/testing-units/{testingUnitId}
         */
        @DeleteMapping("/{testingUnitId}")
        @PreAuthorize("hasRole('VT-01')")
        public ResponseEntity<ApiResult<Void>> deactivateTestingUnit(
                        @PathVariable UUID testingUnitId,
                        @AuthenticationPrincipal CustomUserDetails currentUser) {

                testingUnitService.deactivateTestingUnit(testingUnitId, currentUser);

                return ResponseEntity.ok(ApiResult.success(HttpStatus.OK.value(), null));
        }
}