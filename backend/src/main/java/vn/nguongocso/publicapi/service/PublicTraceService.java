package vn.nguongocso.publicapi.service;

import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;

/** Cung cấp dữ liệu truy xuất công khai. */
public interface PublicTraceService {

    /**
     * Lấy thông tin truy xuất công khai (chế độ đọc).
     * Không tạo TraceCodeScanLog, không tăng lượt quét,
     * không kích hoạt phát hiện nghi vấn.
     */
    PublicTraceResponse getPublicTrace(String codeValue, Double latitude, Double longitude, String ipAddress, String userAgent);

    /**
     * Ghi nhận một lượt quét mã QR thực tế.
     * Tạo TraceCodeScanLog và kích hoạt phát hiện nghi vấn
     * theo quy tắc NCL-08-CN-007, sau đó trả về thông tin truy xuất công khai.
     */
    PublicTraceResponse recordPublicScan(String codeValue, Double latitude, Double longitude, String ipAddress, String userAgent);

    /** Lấy chứng nhận công khai của lô hàng. */
    PublicLotCertificationsResponse getPublicCertifications(String codeValue);
}