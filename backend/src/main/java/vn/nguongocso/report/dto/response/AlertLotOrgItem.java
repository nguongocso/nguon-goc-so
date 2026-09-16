package vn.nguongocso.report.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin tổ chức sở hữu lô sản xuất dùng trong chi tiết lô cảnh báo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLotOrgItem {
    private UUID organizationId;
    private String organizationName;
    private String taxCode;
    private String address;
    private String communeName;
    private String provinceName;
    private String representativeName;
    private String contactPhone;
}
