// PublicTraceResponse.java
package vn.nguongocso.publicapi.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response thông tin tra cứu công khai.
 */
@Getter
@Setter
@Builder
public class PublicTraceResponse {
    private String codeValue;

    private UUID productionLotId;

    private String lotName;

    private String lotCode;

    private String productName;

    private String productNameEn;

    private String shipmentCode;

    private String shipmentStatus;

    private Boolean recalled;

    private String recallMessage;

    private String recallMessageEn;

    private Boolean locked;

    private String lockReason;

    private LocalDateTime lockedAt;

    private String verificationNote;

    private LocalDateTime unlockedAt;

    private List<PublicChainEventItem> events;

    private List<PublicInspectionCriterionResultDto> inspections;

    /** Đánh dấu dữ liệu thử nghiệm (Sandbox). */
    private Boolean isTest;

    /** Thông điệp thông báo dữ liệu thử nghiệm. */
    private String testNotice;

    /**
     * Ranh giới vùng trồng hiển thị công khai (QTN-12).
     * Null khi lô sản xuất chưa gắn vùng trồng hoặc vùng trồng chưa được
     * khoanh ranh giới.
     */
    private PublicFarmAreaBoundaryDto farmAreaBoundary;
}