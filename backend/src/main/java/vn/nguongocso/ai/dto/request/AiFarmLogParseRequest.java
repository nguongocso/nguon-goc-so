package vn.nguongocso.ai.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu phân tích giọng nói của nông dân để trích xuất thông tin nhật ký canh tác.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiFarmLogParseRequest {
    @NotBlank(message = "Nội dung giọng nói không được để trống")
    private String voiceText;

    /**
     * Danh sách gợi ý tên các lô sản xuất của tổ chức để AI nhận diện chính xác.
     */
    @Builder.Default
    private List<LotHintDto> availableLots = new ArrayList<>();

    /**
     * Danh sách gợi ý tên vật tư sẵn có trong kho để AI đối chiếu.
     */
    @Builder.Default
    private List<MaterialHintDto> availableMaterials = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LotHintDto {
        private String id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialHintDto {
        private Long id;
        private String name;
        private String unit;
    }
}
