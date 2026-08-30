package vn.nguongocso.alert.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi thông tin cấu hình ngưỡng quét bất thường (NCL-08-CN-014).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyThresholdResponse {

    private UUID id;

    private UUID productCategoryId;

    private String productCategoryName;

    private Integer maxScansPerHour;

    private Integer maxScansPerDay;

    private Double maxDistanceKmPer30Min;

    private Integer minTimeBetweenScansMinutes;

    private Integer activationAgeDays;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdByName;

    private String updatedByName;
}
