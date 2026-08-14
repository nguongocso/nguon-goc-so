package vn.nguongocso.certification.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO cho yêu cầu gắn chứng nhận vào lô sản xuất.
 */
@Getter
@Setter
public class AttachCertificationRequest {
    @NotNull(message = "ID chứng nhận không được để trống")
    private UUID certificationId;

    @Size(max = 500, message = "Ghi chú không vượt quá 500 ký tự")
    private String note;
}