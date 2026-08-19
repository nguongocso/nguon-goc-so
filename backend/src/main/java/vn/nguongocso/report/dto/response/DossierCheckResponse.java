package vn.nguongocso.report.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi kiểm tra hồ sơ.
 *
 * @author Triệu Văn Đại
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierCheckResponse {
    // ID lô hàng
    private UUID shipmentId;

    // Có đủ điều kiện xuất hồ sơ không
    private boolean eligible;

    // Danh sách chứng từ còn thiếu
    private List<String> missingDocuments;
}