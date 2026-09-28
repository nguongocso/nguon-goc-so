package vn.nguongocso.help.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response chứa nội dung hướng dẫn sử dụng cho một màn hình + vai trò
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpContentResponse {
    private String screenKey;

    private String roleCode;

    private String title;

    private List<String> steps;

    private String exampleData;
}