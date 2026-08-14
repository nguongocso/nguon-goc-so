package vn.nguongocso.common;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về dữ liệu phân trang.
 *
 * @param <T> Kiểu dữ liệu của từng phần tử trong danh sách.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    /**
     * Danh sách dữ liệu của trang hiện tại.
     */
    private List<T> items;

    /**
     * Trang hiện tại (bắt đầu từ 0).
     */
    private int page;

    /**
     * Số lượng phần tử trên mỗi trang.
     */
    private int size;

    /**
     * Tổng số phần tử.
     */
    private long totalElements;

    /**
     * Tổng số trang.
     */
    private int totalPages;

    /**
     * Có phải trang đầu tiên hay không.
     */
    private boolean first;

    /**
     * Có phải trang cuối cùng hay không.
     */
    private boolean last;

    /**
     * Tạo đối tượng phân trang từ dữ liệu Spring Data.
     *
     * @param page  thông tin phân trang
     * @param items danh sách dữ liệu
     * @return dữ liệu phân trang
     */
    public static <T> PageResponse<T> from(Page<?> page, List<T> items) {
        return PageResponse.<T>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
