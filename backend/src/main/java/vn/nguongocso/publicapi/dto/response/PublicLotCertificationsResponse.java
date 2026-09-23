package vn.nguongocso.publicapi.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/** Response danh sách chứng nhận của lô sản xuất trên trang tra cứu công khai. */
@Getter
@Builder
public class PublicLotCertificationsResponse {
    /** ID lô sản xuất. */
    private UUID productionLotId;

    /** Tên lô sản xuất. */
    private String lotName;

    /** Lô có chứng nhận hay không. */
    private boolean hasCertification;

    /** Danh sách chứng nhận đã gắn cho lô. */
    private List<PublicCertificationResponse> certifications;
}
