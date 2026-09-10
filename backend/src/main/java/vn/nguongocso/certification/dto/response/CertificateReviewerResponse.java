package vn.nguongocso.certification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Thông tin người phê duyệt/từ chối chứng nhận.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateReviewerResponse {

    /**
     * ID người dùng.
     */
    private UUID userId;

    /**
     * Họ và tên người dùng.
     */
    private String fullName;
}
