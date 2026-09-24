package vn.nguongocso.ai.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Cấu hình tham số kết nối dịch vụ trí tuệ nhân tạo (LLM).
 */
@Component
@ConfigurationProperties(prefix = "app.ai.gemini")
@Getter
@Setter
public class AiProperties {
    /**
     * Khóa xác thực API Google Gemini.
     */
    private String apiKey = "";

    /**
     * Chuỗi danh sách mô hình ngôn ngữ ưu tiên (phân cách bằng dấu phẩy).
     */
    private String model = "gemini-3-flash-preview,gemini-3.5-flash";

    /**
     * Địa chỉ cơ sở của API Google Gemini.
     */
    private String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models";

    /**
     * Thời gian chờ thiết lập kết nối (giây).
     */
    private int connectTimeoutSeconds = 10;

    /**
     * Thời gian chờ đọc dữ liệu phản hồi từ AI (giây).
     */
    private int readTimeoutSeconds = 60;

    /**
     * Lấy danh sách các mô hình theo thứ tự ưu tiên fallback.
     *
     * @return Danh sách tên mô hình
     */
    public List<String> getModelList() {
        if (model == null || model.isBlank()) {
            return List.of("gemini-3-flash-preview", "gemini-3.5-flash");
        }
        return Arrays.stream(model.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
