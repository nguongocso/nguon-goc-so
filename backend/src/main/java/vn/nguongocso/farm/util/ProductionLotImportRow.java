package vn.nguongocso.farm.util;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Đại diện cho một dòng dữ liệu đọc từ tệp nhập.
 */
@Getter
@Builder
public class ProductionLotImportRow {
    private Integer rowNumber;

    private String lotName;

    private String productCategoryId;

    private String farmAreaId;

    private Double expectedQuantity;

    private Double actualQuantity;

    private LocalDate plantingDate;

    private LocalDate harvestDate;

    /*
     * FarmLog
     */
    private FarmActivityType activityType;

    private String material;

    private Double quantity;

    private String unit;

    private LocalDate executedDate;

    private String note;
}