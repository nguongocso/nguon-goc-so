package vn.nguongocso.certification.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO cho phản hồi thông tin chứng nhận gắn vào lô sản xuất.
 */
@Getter
@Setter
@Builder
public class ProductionLotCertificationResponse {
    private UUID id;

    private UUID certificationId;

    private String certificationName;

    private String certificationCode;

    private String issuedBy;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    private Boolean isValid;

    private LocalDateTime attachedAt;

    private String attachedBy;
    
    private String note;
}