package vn.nguongocso.auth.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin tóm tắt một lô sản xuất đang phân công cho thành viên,
 * dùng trong precheck lô chưa hoàn thành và payload 409 của API
 * vô hiệu hóa (NCL-01-CN-009).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLotSummary {

    private UUID lotId;

    private String lotName;

    private String lotStatus;

    private LocalDate plantingDate;

    private LocalDate harvestDate;
}
