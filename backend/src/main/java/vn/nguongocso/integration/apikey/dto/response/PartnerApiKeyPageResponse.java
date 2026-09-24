package vn.nguongocso.integration.apikey.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response trang danh sách khóa truy cập với số liệu phân trang tường minh.
*/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerApiKeyPageResponse {
    private List<PartnerApiKeyResponse> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}
