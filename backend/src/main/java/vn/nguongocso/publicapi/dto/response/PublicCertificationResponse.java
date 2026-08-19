package vn.nguongocso.publicapi.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.certification.enums.CertificationStatus;

/**
 * Response thông tin chứng nhận hiển thị trên trang tra cứu công khai.
 */
@Getter
@Builder
public class PublicCertificationResponse {
    private UUID certificationId; // ID chứng nhận

    private String certificationName;// Tên chứng nhận

    private String certificationCode;// Mã chứng nhận

    private String issuedBy;// Đơn vị cấp chứng nhận

    private LocalDate issueDate;// Ngày cấp

    private LocalDate expiryDate;// Ngày hết hạn

    private CertificationStatus status;// Trạng thái hiệu lực

    private String statusLabel;// Nhãn hiển thị
}