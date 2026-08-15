package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CreateInspectionRequest;
import vn.nguongocso.certification.dto.response.InspectionRequestListResponse;
import vn.nguongocso.certification.dto.response.InspectionRequestResponse;
import vn.nguongocso.certification.dto.response.ProductionLotTestCriteriaResponse;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.service.InspectionRequestService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InspectionRequestController {

    private final InspectionRequestService inspectionRequestService;

    /**
     * Lấy danh sách chỉ tiêu kiểm nghiệm áp dụng cho lô.
     *
     * GET /api/v1/production-lots/{lotId}/test-criteria
     */
    @GetMapping("/production-lots/{lotId}/test-criteria")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<ProductionLotTestCriteriaResponse>> getTestCriteria(
            @PathVariable UUID lotId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ProductionLotTestCriteriaResponse response =
                inspectionRequestService.getTestCriteria(
                        lotId,
                        currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        response));
    }

    /**
     * Tạo yêu cầu kiểm nghiệm cho lô.
     *
     * POST /api/v1/production-lots/{lotId}/test-requests
     */
    @PostMapping("/production-lots/{lotId}/test-requests")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<InspectionRequestResponse>> create(
            @PathVariable UUID lotId,
            @Valid @RequestBody CreateInspectionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        InspectionRequestResponse response =
                inspectionRequestService.createInspectionRequest(
                        lotId,
                        request,
                        currentUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResult.success(
                                HttpStatus.CREATED.value(),
                                response));
    }

    /**
     * Lấy danh sách yêu cầu kiểm nghiệm.
     *
     * GET /api/v1/test-requests
     */
    @GetMapping("/test-requests")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<PageResponse<InspectionRequestListResponse>>> getInspectionRequests(
            @RequestParam(required = false) UUID lotId,
            @RequestParam(required = false) InspectionRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Pageable pageable = PageRequest.of(page, size);

        Page<InspectionRequestListResponse> response =
                inspectionRequestService.getInspectionRequests(
                        lotId,
                        status,
                        pageable,
                        currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        PageResponse.from(
                                response,
                                response.getContent())));
    }
}