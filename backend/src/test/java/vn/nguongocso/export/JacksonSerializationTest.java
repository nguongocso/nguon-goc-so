package vn.nguongocso.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.nguongocso.export.dto.request.CreateProfileTemplateRequest;
import vn.nguongocso.export.dto.request.UpdateProfileTemplateRequest;
import vn.nguongocso.export.dto.response.ProfileTemplateResponse;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
        System.out.println("createReq.getIsDefault(): " + createReq.getIsDefault());

        // Test ProfileTemplateResponse serialization
        ProfileTemplateResponse resp = ProfileTemplateResponse.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .isDefault(true)
                .build();
        String jsonResp = mapper.writeValueAsString(resp);
        System.out.println("jsonResp: " + jsonResp);

        assertTrue(jsonResp.contains("\"isDefault\":true"), "Response must contain 'isDefault': " + jsonResp);
        assertEquals(Boolean.TRUE, createReq.getIsDefault(), "CreateRequest must parse 'isDefault'");
    }
}
