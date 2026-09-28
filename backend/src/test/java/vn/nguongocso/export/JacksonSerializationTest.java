package vn.nguongocso.export;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.nguongocso.export.dto.request.CreateProfileTemplateRequest;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử tính năng tuần tự hóa và giải tuần tự hóa Jackson cho các DTO mẫu hồ sơ.
 * Đảm bảo thuộc tính isDefault luôn được map chính xác giữa frontend và backend.
 */
public class JacksonSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testSerializationAndDeserialization() throws Exception {
        // Test CreateProfileTemplateRequest deserialization
        String jsonCreate = "{\"name\":\"Test\",\"partnerName\":\"Partner\",\"isDefault\":true,\"selectedFields\":[]}";
        CreateProfileTemplateRequest createReq = mapper.readValue(jsonCreate, CreateProfileTemplateRequest.class);

        // Test ProfileTemplateResponse serialization
        ProfileTemplateResponse resp = ProfileTemplateResponse.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .isDefault(true)
                .build();
        String jsonResp = mapper.writeValueAsString(resp);

        assertThat(jsonResp).contains("\"isDefault\":true");
        assertThat(createReq.getIsDefault()).isTrue();
    }
}
