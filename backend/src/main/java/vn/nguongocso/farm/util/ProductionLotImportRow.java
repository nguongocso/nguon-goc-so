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
    private String material;
    private String unit;
    private String note;

    private Double expectedQuantity;
    private Double actualQuantity;
    private Double quantity;

    private LocalDate plantingDate;
    private LocalDate harvestDate;
    private LocalDate executedDate;
    
    private FarmActivityType activityType;
}