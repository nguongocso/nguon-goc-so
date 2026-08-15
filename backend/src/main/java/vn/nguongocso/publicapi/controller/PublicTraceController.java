// PublicTraceController.java
package vn.nguongocso.publicapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.nguongocso.common.ApiResult;
import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;

@RestController
@RequestMapping("/api/v1/public/trace")
@RequiredArgsConstructor
public class PublicTraceController {

    private final PublicTraceService publicTraceService;

    /**
     * Lấy thông tin truy xuất công khai của một mã (đọc thuần túy).
     * <p>
     * Được dùng cho:
     * <ul>
     * <li>tra cứu thủ công (người dùng nhập mã và bấm tìm kiếm);</li>
     * <li>mở / reload đường dẫn công khai / chia sẻ liên kết;</li>
     * <li>xem thông tin hành trình thông thường.</li>
     * </ul>
     * Endpoint này KHÔNG tạo TraceCodeScanLog, KHÔNG tăng lượt quét và
     * KHÔNG kích hoạt phát hiện nghi vấn NCL-08-CN-007.
     *
     * FE chỉ gửi latitude và longitude.
     * Backend tự reverse geocoding để lấy location.
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
     * <p>
     * Được gọi bởi luồng quét QR ở frontend sau khi giải mã thành công payload QR.
     * Endpoint này:
     * <ul>
     * <li>kiểm tra mã tem theo đúng quy tắc tra cứu công khai hiện tại;</li>
     * <li>tạo bản ghi TraceCodeScanLog;</li>
     * <li>kích hoạt phát hiện quét bất thường (gồm đánh giá nghi vấn NCL-08-CN-007);</li>
     * <li>trả về thông tin truy xuất công khai.</li>
     * </ul>
     * Endpoint này KHÔNG phải bằng chứng mật mã xác thực việc quét mã vật lý;
     * nó đại diện cho hợp đồng ứng dụng của luồng quét QR.
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
     */
    @GetMapping("/{codeValue}/certifications")
    public ResponseEntity<ApiResult<PublicLotCertificationsResponse>> getPublicCertifications(
            @PathVariable String codeValue) {

        PublicLotCertificationsResponse response =
                publicTraceService.getPublicCertifications(codeValue);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * Lấy IP thực của client.
     */
    private String getClientIp(HttpServletRequest request) {

        String xForwardedFor =
                request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}