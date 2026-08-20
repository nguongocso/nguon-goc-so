package vn.nguongocso.publicapi.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Response danh sách chứng nhận của lô sản xuất trên trang tra cứu công khai.
 */
@Getter
@Builder
public class PublicLotCertificationsResponse {
    private UUID productionLotId;// ID lô sản xuất

    private String lotName;// Tên lô sản xuất

    private boolean hasCertification;// Lô có chứng nhận hay không

    private List<PublicCertificationResponse> certifications;// Danh sách chứng nhận đã gắn cho lô
}