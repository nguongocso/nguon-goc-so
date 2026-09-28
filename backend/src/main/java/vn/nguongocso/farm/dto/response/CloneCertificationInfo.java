package vn.nguongocso.farm.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Thông tin rút gọn của một chứng nhận khi xem trước và tạo lô từ mẫu.
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
