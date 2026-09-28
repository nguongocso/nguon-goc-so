package vn.nguongocso.ai.dto.query;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO chứa thông tin trạng thái giấy chứng nhận chất lượng của tổ chức.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificationStatusDto {
    /** Số hiệu giấy chứng nhận. */
    private String code;

    /** Tên giấy chứng nhận. */
    private String name;

    /** Tên tiêu chuẩn áp dụng (VietGAP, GlobalGAP, OCOP...). */
    private String standardName;

    /** Ngày hết hiệu lực của chứng nhận. */
    private LocalDate expiryDate;

    /** Số ngày còn lại trước khi hết hiệu lực. */
    private long daysRemaining;
}
