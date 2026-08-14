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
public class CreateProductionLotResponse {
        private UUID id;

        private UUID farmAreaId;

        private UUID productCategoryId;

        private String organizationName;

        private String farmAreaName;

        private String productCategoryName;

        private String name;

        private Double expectedQuantity;

        private String expectedQuantityUnit;

        private Double actualQuantity;

        private LocalDate plantingDate;

        private LocalDate harvestDate;

        private String status;

        private String approvalNotes;

        private String createdByName;

        private String approvedByName;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
}
