package vn.nguongocso.farm.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi thông tin lô sản xuất.
 */
@Getter
@Setter
@Builder
public class UpdateProductionLotResponse {
    private UUID id;

    private UUID farmAreaId;

    private UUID productCategoryId;

    private String name;

    private Double expectedQuantity;

    private String expectedQuantityUnit;

    private LocalDate plantingDate;

    private String status;

    private LocalDateTime updatedAt;
}
