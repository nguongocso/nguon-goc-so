package vn.nguongocso.help.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response chứa nội dung hướng dẫn sử dụng cho một màn hình + vai trò
 * (NCL-01-CN-006).
 */
@Getter
@Setter
@Builder
public class HelpContentResponse {

    /** Mã định danh màn hình (ví dụ: {@code farm-log-create}). */
    private String screenKey;

    /** Mã vai trò (ví dụ: {@code VT-03}) hoặc {@code GENERAL}. */
    private String roleCode;

    /** Tiêu đề hướng dẫn. */
    private String title;

    /** Danh sách các bước hướng dẫn. */
    private List<String> steps;

    /** Ví dụ minh hoạ (tuỳ chọn). */
    private String exampleData;
}