package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Thông tin rút gọn của một chứng nhận khi xem trước / tạo lô từ mẫu
 * (NCL-02-CN-007).
 */
@Getter
@Setter
@Builder
public class CloneCertificationInfo {
    private UUID id;

    private String name;

    private String code;

    private LocalDate expiryDate;
}
