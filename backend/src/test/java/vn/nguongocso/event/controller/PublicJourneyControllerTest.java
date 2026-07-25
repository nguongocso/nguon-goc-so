package vn.nguongocso.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.event.dto.response.JourneyPointResponse;
import vn.nguongocso.event.dto.response.JourneyResponse;
import vn.nguongocso.event.service.JourneyService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicJourneyController.class)
@Import(SecurityConfig.class)   // Nếu cần, nhưng không bắt buộc
@ActiveProfiles("test")
public class PublicJourneyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JourneyService journeyService;

    // ✅ Thêm mock các bean cần cho filter
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getJourney_shouldReturnOk() throws Exception {
        UUID shipmentId = UUID.randomUUID();

        JourneyPointResponse point = JourneyPointResponse.builder()
                .eventId(UUID.randomUUID())
                .eventType("HARVEST")
                .eventName("Thu hoạch")
                .latitude(10.823)
                .longitude(106.629)
                .recordedAt(LocalDateTime.now())
                .order(1)
                .build();

        JourneyResponse response = JourneyResponse.builder()
                .shipmentId(shipmentId)
                .shipmentName("Lô hàng 1")
                .totalEvents(1)
                .points(List.of(point))
                .build();

        when(journeyService.getJourney(shipmentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/shipments/{shipmentId}/journey", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEvents").value(1))
                .andExpect(jsonPath("$.data.points[0].eventType").value("HARVEST"))
                .andExpect(jsonPath("$.data.points[0].latitude").value(10.823));
    }
}