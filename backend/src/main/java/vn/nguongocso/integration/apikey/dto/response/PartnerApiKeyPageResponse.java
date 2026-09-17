package vn.nguongocso.integration.apikey.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trang danh sách khóa truy cập với số liệu phân trang tường minh (NCL-12-CN-005).
 * <p>
 * Thay cho việc trả trực tiếp Spring {@code Page} (bị serialize rút gọn thành
 * {@code {content, page: {...}}} khiến FE đọc {@code totalElements = 0}).
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
