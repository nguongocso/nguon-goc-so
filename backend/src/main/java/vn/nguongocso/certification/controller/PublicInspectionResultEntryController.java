package vn.nguongocso.certification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.certification.dto.request.RecordInspectionResultsRequest;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.dto.response.PublicInspectionResultEntryResponse;
import vn.nguongocso.certification.service.InspectionCriterionResultService;
import vn.nguongocso.certification.service.InspectionResultEntryLinkService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.util.IpUtils;

/**
 * Controller công khai dành cho đơn vị kiểm nghiệm nhập kết quả qua liên kết token có thời hạn (NCL-11-CN-007).
 * Không yêu cầu đăng nhập tài khoản / JWT; bảo mật thông qua token băm SHA-256 dùng một lần.
 */
@RestController
@RequestMapping("/api/v1/public/inspection-result-entry")
@RequiredArgsConstructor
public class PublicInspectionResultEntryController {

    private static final String CACHE_CONTROL_VALUE = "no-store, no-cache, must-revalidate";

    private final InspectionResultEntryLinkService linkService;
    private final InspectionCriterionResultService criterionResultService;

    /**
     * Lấy thông tin yêu cầu kiểm nghiệm và danh sách chỉ tiêu cần nhập.
     *
     * @param token   Mã token bí mật từ URL.
     * @param request HttpServletRequest để lấy địa chỉ IP của client.
     * @return DTO thông tin tối thiểu của yêu cầu kiểm nghiệm.
     */
    @GetMapping("/{token}")
    public ResponseEntity<ApiResult<PublicInspectionResultEntryResponse>> getPortalData(
            @PathVariable String token,
            HttpServletRequest request) {

        String clientIp = IpUtils.getClientIp();
        PublicInspectionResultEntryResponse response = linkService.getPublicPortalData(token, clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(ApiResult.success(HttpStatus.OK.value(), response));
    }

    /**
     * Tải lên phiếu kết quả kiểm nghiệm cho một chỉ tiêu cụ thể.
     *
     * @param token       Mã token bí mật.
     * @param criterionId ID của chỉ tiêu kiểm nghiệm thuộc yêu cầu.
     * @param file        Tệp phiếu kết quả (JPG/PNG/PDF).
     * @param request     HttpServletRequest.
     * @return Đường dẫn tệp đã lưu để đưa vào payload submit.
     */
    @PostMapping("/{token}/criteria/{criterionId}/file")
    public ResponseEntity<ApiResult<Map<String, String>>> uploadFile(
            @PathVariable String token,
            @PathVariable String criterionId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        String clientIp = IpUtils.getClientIp();
        String fileHandle = criterionResultService.uploadPortalResultFile(token, criterionId, file, clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(ApiResult.success(HttpStatus.OK.value(), Map.of(
                        "fileHandle", fileHandle,
                        "filePath", fileHandle)));
    }

    /**
     * Gửi toàn bộ kết quả kiểm nghiệm từ đơn vị kiểm nghiệm (dùng một lần, atomic consume).
     *
     * @param token       Mã token bí mật.
     * @param requestBody Payload chứa danh sách kết quả cho toàn bộ chỉ tiêu.
     * @param request     HttpServletRequest.
     * @return Danh sách kết quả kiểm nghiệm đã được ghi nhận.
     */
    @PutMapping("/{token}/results")
    public ResponseEntity<ApiResult<List<InspectionCriterionResultResponse>>> recordResults(
            @PathVariable String token,
            @Valid @RequestBody RecordInspectionResultsRequest requestBody,
            HttpServletRequest request) {

        String clientIp = IpUtils.getClientIp();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        List<InspectionCriterionResultResponse> response = criterionResultService.recordPortalResults(
                token,
                requestBody.getResults(),
                clientIp,
                userAgent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(ApiResult.success(HttpStatus.OK.value(), response));
    }
}
