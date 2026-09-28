package vn.nguongocso.publicapi.service;

import java.util.UUID;

import vn.nguongocso.certification.service.CertificationService;
import vn.nguongocso.publicapi.dto.response.PublicInspectionResponse;
import vn.nguongocso.publicapi.dto.response.PublicLotCertificationsResponse;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;

/** Cung cấp dữ liệu truy xuất công khai. */
public interface PublicTraceService {
    /** Lấy thông tin truy xuất công khai (chế độ đọc). */
    PublicTraceResponse getPublicTrace(String codeValue, Double latitude, Double longitude, String ipAddress, String userAgent);

    /** Ghi nhận một lượt quét mã QR thực tế và kích hoạt phát hiện nghi vấn. */
    PublicTraceResponse recordPublicScan(String codeValue, Double latitude, Double longitude, String ipAddress, String userAgent);

    /** Lấy chứng nhận công khai của lô hàng. */
    PublicLotCertificationsResponse getPublicCertifications(String codeValue);

    /** Lấy kết quả kiểm nghiệm công khai của lô hàng. */
    PublicInspectionResponse getPublicInspections(String codeValue);

    /**
     * Lấy tệp tài liệu của một chứng nhận thuộc lô của mã tem đang tra cứu.
     * Chỉ trả về khi chứng nhận thực sự được gắn cho lô đó và chưa bị từ chối.
     */
    CertificationService.DocumentResource getPublicCertificationDocument(String codeValue, UUID certificationId);
}
