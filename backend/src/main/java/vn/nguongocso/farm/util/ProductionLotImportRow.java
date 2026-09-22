package vn.nguongocso.farm.util;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

import vn.nguongocso.farm.enums.FarmActivityType;

/**
 * Dòng dữ liệu nhập lô sản xuất đọc từ tệp Excel.
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

    private FarmActivityType activityType;

    private String material;

    private Double quantity;

    private String unit;

    private LocalDate executedDate;

    private String note;
}