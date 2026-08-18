package vn.nguongocso.certification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.request.RecordInspectionResultsRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.dto.response.InspectionResultFileUploadResponse;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.common.ApiResult;

import java.util.List;
import java.util.UUID;

/**
 * Controller API cho ghi nhận kết quả kiểm nghiệm.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InspectionCriterionResultController {

    private final InspectionCriterionResultService resultService;

    /**
     * Ghi nhận kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * POST /api/v1/inspection-criteria/{criterionId}/results
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @param request     DTO chứa thông tin kết quả kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Phản hồi chứa thông tin kết quả kiểm nghiệm đã lưu.
     */
    @PostMapping("/inspection-criteria/{criterionId}/results")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<InspectionCriterionResultResponse>>
            recordOrUpdateResult(
                    @PathVariable String criterionId,
                    @Valid @RequestBody InspectionCriterionResultRequest request,
                    @AuthenticationPrincipal CustomUserDetails currentUser) {

        InspectionCriterionResultResponse response =
                resultService.recordOrUpdateResult(
                        criterionId,
                        request,
                        currentUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResult.success(
                        HttpStatus.CREATED.value(),
                        response));
    }

    /**
     * Ghi nhận toàn bộ kết quả kiểm nghiệm của một yêu cầu trong một giao dịch.
     *
     * PUT /api/v1/inspection-requests/{requestId}/results
     *
     * <p>
     * Payload phải chứa kết quả cho tất cả chỉ tiêu của yêu cầu.
     * Nếu có bất kỳ chỉ tiêu nào không hợp lệ, toàn bộ yêu cầu bị từ chối
     * và không có kết quả nào được lưu (all-or-nothing).
     * </p>
     *
     * @param requestId   ID của yêu cầu kiểm nghiệm.
     * @param request     DTO chứa danh sách kết quả kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Danh sách kết quả kiểm nghiệm đã lưu.
     */
    @PutMapping("/inspection-requests/{requestId}/results")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<InspectionCriterionResultResponse>>>
            recordResults(
                    @PathVariable UUID requestId,
                    @Valid @RequestBody RecordInspectionResultsRequest request,
                    @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<InspectionCriterionResultResponse> response =
                resultService.recordResults(
                        requestId,
                        request.getResults(),
                        currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        response));
    }

    /**
     * Lấy danh sách kết quả kiểm nghiệm cho tất cả chỉ tiêu của một yêu cầu.
     *
     * GET /api/v1/inspection-requests/{requestId}/results
     *
     * @param requestId   ID của yêu cầu kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Danh sách kết quả kiểm nghiệm.
     */
    @GetMapping("/inspection-requests/{requestId}/results")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<List<InspectionCriterionResultResponse>>>
            getResultsByRequest(
                    @PathVariable String requestId,
                    @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID requestUUID = UUID.fromString(requestId);
        List<InspectionCriterionResultResponse> response =
                resultService.getResultsByRequest(
                        requestUUID,
                        currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        response));
    }

    /**
     * Lấy kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * GET /api/v1/inspection-criteria/{criterionId}/result
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Kết quả kiểm nghiệm của chỉ tiêu đó.
     */
    @GetMapping("/inspection-criteria/{criterionId}/result")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<InspectionCriterionResultResponse>>
            getResultByCriterion(
                    @PathVariable String criterionId,
                    @AuthenticationPrincipal CustomUserDetails currentUser) {

        InspectionCriterionResultResponse response =
                resultService.getResultByCriterion(
                        criterionId,
                        currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        response));
    }

    /**
     * Xóa kết quả kiểm nghiệm.
     *
     * DELETE /api/v1/inspection-results/{resultId}
     *
     * @param resultId    ID của kết quả kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Phản hồi thành công.
     */
    @DeleteMapping("/inspection-results/{resultId}")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<Void>> deleteResult(
            @PathVariable String resultId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        resultService.deleteResult(resultId, currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        null));
    }

    /**
     * Tải lên phiếu kết quả kiểm nghiệm cho một chỉ tiêu.
     *
     * POST /api/v1/inspection-criteria/{criterionId}/result-file
     *
     * @param criterionId ID của chỉ tiêu kiểm nghiệm.
     * @param file        Tệp phiếu kết quả (JPG/PNG/PDF).
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Đường dẫn tệp đã lưu.
     */
    @PostMapping(value = "/inspection-criteria/{criterionId}/result-file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<InspectionResultFileUploadResponse>>
            uploadResultFile(
                    @PathVariable String criterionId,
                    @RequestParam("file") MultipartFile file,
                    @AuthenticationPrincipal CustomUserDetails currentUser) {

        String filePath = resultService.uploadResultFile(
                criterionId,
                file,
                currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        InspectionResultFileUploadResponse.builder()
                                .filePath(filePath)
                                .build()));
    }

    /**
     * Xem phiếu kết quả kiểm nghiệm đã đính kèm.
     *
     * GET /api/v1/inspection-results/{resultId}/file
     *
     * @param resultId    ID của kết quả kiểm nghiệm.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Nội dung tệp phiếu kết quả.
     */
    @GetMapping("/inspection-results/{resultId}/file")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<Resource> getResultFile(
            @PathVariable String resultId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        InspectionCriterionResultService.ResultFileResource fileResource =
                resultService.getResultFile(resultId, currentUser);

        return ResponseEntity.ok()
                .contentType(fileResource.contentType())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileResource.fileName() + "\"")
                .body(fileResource.resource());
    }

    /**
     * Kiểm tra xem lô sản xuất có thể kích hoạt tem hay không.
     *
     * POST /api/v1/production-lots/{lotId}/can-activate-seal
     *
     * @param lotId       ID của lô sản xuất.
     * @param currentUser Thông tin người dùng hiện tại.
     * @return Phản hồi chứa trạng thái kích hoạt và lý do nếu có.
     */
    @PostMapping("/production-lots/{lotId}/can-activate-seal")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CanActivateSealCheckResponse>>
            checkCanActivateSeal(
                    @PathVariable UUID lotId,
                    @AuthenticationPrincipal CustomUserDetails currentUser) {

        CanActivateSealCheckResponse response =
                resultService.checkCanActivateSeal(lotId, currentUser);

        return ResponseEntity.ok(
                ApiResult.success(
                        HttpStatus.OK.value(),
                        response));
    }
}