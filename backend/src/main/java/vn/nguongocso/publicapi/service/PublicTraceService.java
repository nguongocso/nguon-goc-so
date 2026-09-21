package vn.nguongocso.publicapi.service;

import vn.nguongocso.publicapi.dto.response.PublicInspectionResponse;
import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;

/**
 * Cung cấp dữ liệu truy xuất công khai.
 */
public interface PublicTraceService {

    /**
     * Lấy thông tin truy xuất công khai (chế độ đọc).
     * Không tạo TraceCodeScanLog, không tăng lượt quét, không kích hoạt phát hiện nghi vấn.
     *
     * @param codeValue mã tem truy xuất
     * @param latitude  vĩ độ
     * @param longitude kinh độ
     * @param ipAddress địa chỉ IP của client
     * @param userAgent User-Agent của client
     * @return thông tin truy xuất công khai
     */
    PublicTraceResponse getPublicTrace(
            String codeValue,
            Double latitude,
            Double longitude,
            String ipAddress,
            String userAgent);

    /**
     * Ghi nhận một lượt quét mã QR thực tế.
     * Tạo TraceCodeScanLog và kích hoạt phát hiện nghi vấn theo quy tắc NCL-08-CN-007,
     * sau đó trả về thông tin truy xuất công khai.
     *
     * @param codeValue mã tem truy xuất
     * @param latitude  vĩ độ
     * @param longitude kinh độ
     * @param ipAddress địa chỉ IP của client
     * @param userAgent User-Agent của client
     * @return thông tin truy xuất công khai
     */
    PublicTraceResponse recordPublicScan(
            String codeValue,
            Double latitude,
            Double longitude,
            String ipAddress,
            String userAgent);

    /**
     * Lấy chứng nhận công khai của lô hàng.
     *
     * @param codeValue mã tem truy xuất
     * @return danh sách chứng nhận công khai của lô
     */
    PublicLotCertificationsResponse getPublicCertifications(String codeValue);

    /**
     * Lấy kết quả kiểm nghiệm công khai của lô hàng.
     *
     * @param codeValue mã tem truy xuất
     * @return danh sách kết quả kiểm nghiệm công khai
     */
    PublicInspectionResponse getPublicInspections(String codeValue);
}
