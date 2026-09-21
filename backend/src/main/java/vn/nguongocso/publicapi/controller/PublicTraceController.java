package vn.nguongocso.publicapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.publicapi.dto.response.PublicInspectionResponse;
import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;

/**
 * Controller truy xuất nguồn gốc công khai.
 */
@RestController
@RequestMapping("/api/v1/public/trace")
@RequiredArgsConstructor
public class PublicTraceController {

    private final PublicTraceService publicTraceService;

    /**
     * Lấy thông tin truy xuất công khai của một mã (đọc thuần túy).
     *
     * <p>Được dùng cho:</p>
     * <ul>
     *   <li>Tra cứu thủ công (người dùng nhập mã và bấm tìm kiếm).</li>
     *   <li>Mở / reload đường dẫn công khai / chia sẻ liên kết.</li>
     *   <li>Xem thông tin hành trình thông thường.</li>
     * </ul>
     * Endpoint này KHÔNG tạo TraceCodeScanLog, KHÔNG tăng lượt quét và
     * KHÔNG kích hoạt phát hiện nghi vấn NCL-08-CN-007.
     *
     * @param codeValue mã tem truy xuất
     * @param latitude  vĩ độ
     * @param longitude kinh độ
     * @param request   HTTP request
     * @return thông tin truy xuất công khai
     */
    @GetMapping("/{codeValue}")
    public ResponseEntity<ApiResult<PublicTraceResponse>> getPublicTrace(
            @PathVariable String codeValue,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            HttpServletRequest request) {

        PublicTraceResponse response = publicTraceService.getPublicTrace(
                codeValue,
                latitude,
                longitude,
                getClientIp(request),
                request.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Ghi nhận một lượt quét mã QR thực tế.
     *
     * <p>Được gọi bởi luồng quét QR ở frontend sau khi giải mã thành công payload QR.</p>
     * Endpoint này:
     * <ul>
     *   <li>Kiểm tra mã tem theo đúng quy tắc tra cứu công khai hiện tại.</li>
     *   <li>Tạo bản ghi TraceCodeScanLog.</li>
     *   <li>Kích hoạt phát hiện quét bất thường (gồm đánh giá nghi vấn NCL-08-CN-007).</li>
     *   <li>Trả về thông tin truy xuất công khai.</li>
     * </ul>
     *
     * @param codeValue mã tem truy xuất
     * @param latitude  vĩ độ
     * @param longitude kinh độ
     * @param request   HTTP request
     * @return thông tin truy xuất công khai
     */
    @PostMapping("/{codeValue}/scan")
    public ResponseEntity<ApiResult<PublicTraceResponse>> recordPublicScan(
            @PathVariable String codeValue,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            HttpServletRequest request) {

        PublicTraceResponse response = publicTraceService.recordPublicScan(
                codeValue,
                latitude,
                longitude,
                getClientIp(request),
                request.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách chứng nhận công khai của lô hàng.
     *
     * @param codeValue mã tem truy xuất
     * @return danh sách chứng nhận công khai
     */
    @GetMapping("/{codeValue}/certifications")
    public ResponseEntity<ApiResult<PublicLotCertificationsResponse>> getPublicCertifications(
            @PathVariable String codeValue) {

        PublicLotCertificationsResponse response = publicTraceService.getPublicCertifications(codeValue);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy danh sách kết quả kiểm nghiệm công khai của lô hàng (TASK-16 / CV-04).
     *
     * @param codeValue mã tem truy xuất
     * @return danh sách kết quả kiểm nghiệm công khai
     */
    @GetMapping("/{codeValue}/inspections")
    public ResponseEntity<ApiResult<PublicInspectionResponse>> getPublicInspections(
            @PathVariable String codeValue) {

        PublicInspectionResponse response = publicTraceService.getPublicInspections(codeValue);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy IP thực của client.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
