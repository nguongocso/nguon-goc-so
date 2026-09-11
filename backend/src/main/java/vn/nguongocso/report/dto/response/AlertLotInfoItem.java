package vn.nguongocso.report.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thông tin chi tiết lô sản xuất dùng trong màn hình chi tiết lô cảnh báo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertLotInfoItem {
    private UUID lotId;
    private String lotCode;
    private String lotName;
    private String status;
    private Double expectedQuantity;
    private Double actualQuantity;
    private String quantityUnit;
    private LocalDate plantingDate;
    private LocalDate harvestDate;
    private String productCategoryName;
    private String farmAreaName;
    private String farmAreaAddress;
    private LocalDateTime createdAt;
}
