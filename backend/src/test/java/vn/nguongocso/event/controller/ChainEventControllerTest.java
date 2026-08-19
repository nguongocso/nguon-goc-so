package vn.nguongocso.event.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.dto.request.CorrectPackagingEventRequest;
import vn.nguongocso.event.dto.request.RecordHarvestEventRequest;
import vn.nguongocso.event.dto.request.RecordMobileEventRequest;
import vn.nguongocso.event.dto.request.RecordPackagingEventRequest;
import vn.nguongocso.event.dto.response.ChainEventResponse;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.service.ChainEventService;

@ExtendWith(MockitoExtension.class)
class ChainEventControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChainEventService chainEventService;

    @InjectMocks
    private ChainEventController chainEventController;

    private RecordHarvestEventRequest validRequest;
    private RecordMobileEventRequest validRequestMobile;
    private ChainEventResponse successResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chainEventController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mock(CustomUserDetails.class);
                    }
                })
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        UUID productionLotId = UUID.randomUUID();

        validRequest = new RecordHarvestEventRequest();
        validRequest.setProductionLotId(productionLotId);
        validRequest.setHarvestDate(LocalDate.of(2026, 7, 24));
        validRequest.setQuantity(1500.0);
        validRequest.setLatitude(21.0285);
        validRequest.setLongitude(105.8542);

        validRequestMobile = new RecordMobileEventRequest();
        validRequestMobile.setProductionLotId(UUID.randomUUID());
        validRequestMobile.setEventType(ChainEventType.HARVEST);
        validRequestMobile.setRecordedAt(LocalDateTime.now().minusMinutes(10));
        validRequestMobile.setLatitude(21.0285);
        validRequestMobile.setLongitude(105.8542);
        validRequestMobile.setImages(List.of("https://picsum.photos/id/237/400/300.jpg"));
        validRequestMobile.setEventData(Map.of("quantity", 500.0, "harvestDate", "2026-07-31"));

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", productionLotId.toString());
        eventDataMap.put("harvestDate", "2026-07-24");
        eventDataMap.put("quantity", 1500.0);

        successResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.HARVEST)
                .eventData(eventDataMap)
                .latitude(21.0285)
                .longitude(105.8542)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Nguyễn Văn Ghi")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Ghi nhận sự kiện thu hoạch thành công")
    void recordHarvest_Success() throws Exception {
        when(chainEventService.recordHarvestEvent(any(RecordHarvestEventRequest.class), any()))
                .thenReturn(successResponse);

        mockMvc.perform(post("/api/v1/chain-events/harvest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("HARVEST"))
                .andExpect(jsonPath("$.data.eventData.quantity").value(1500.0))
                .andExpect(jsonPath("$.data.recordedByName").value("Nguyễn Văn Ghi"));
    }

    @Test
    @DisplayName("Ghi nhận sự kiện đóng gói thành công")
    void recordPackaging_Success() throws Exception {
        UUID productionLotId = UUID.randomUUID();
        RecordPackagingEventRequest packagingRequest = new RecordPackagingEventRequest();
        packagingRequest.setProductionLotId(productionLotId);
        packagingRequest.setPackagingSpecification("Túi hút chân không 500g");
        packagingRequest.setPackagingDate(LocalDate.of(2026, 7, 25));

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("productionLotId", productionLotId.toString());
        eventDataMap.put("packagingSpecification", "Túi hút chân không 500g");

        ChainEventResponse packagingResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .eventData(eventDataMap)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Nguyễn Văn Ghi")
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventService.recordPackagingEvent(any(RecordPackagingEventRequest.class), any()))
                .thenReturn(packagingResponse);

        mockMvc.perform(post("/api/v1/chain-events/packaging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(packagingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("PACKAGING"));
    }

    @Test
    @DisplayName("Đính chính sự kiện đóng gói thành công")
    void correctPackaging_Success() throws Exception {
        UUID originalEventId = UUID.randomUUID();
        CorrectPackagingEventRequest correctRequest = new CorrectPackagingEventRequest();
        correctRequest.setPackagingSpecification("Túi hút chân không 1kg");
        correctRequest.setPackagingDate(LocalDate.of(2026, 7, 25));
        correctRequest.setCorrectionReason("Đính chính do nhập sai quy cách từ 500g sang 1kg");

        Map<String, Object> eventDataMap = new HashMap<>();
        eventDataMap.put("packagingSpecification", "Túi hút chân không 1kg");

        ChainEventResponse correctResponse = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PACKAGING)
                .eventData(eventDataMap)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Quản Lý HTX")
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventService.correctPackagingEvent(eq(originalEventId), any(), any()))
                .thenReturn(correctResponse);

        mockMvc.perform(post("/api/v1/chain-events/packaging/{id}/correct", originalEventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Ghi nhận sự kiện sơ chế thành công")
    void recordPreprocessing_Success() throws Exception {
        UUID lotId = UUID.randomUUID();
        vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest request = new vn.nguongocso.event.dto.request.RecordPreprocessingEventRequest();
        request.setProductionLotId(lotId);
        request.setInputQuantity(1000.0);
        request.setOutputQuantity(900.0);
        request.setGrade("Hạng A");
        request.setPreprocessingDate(LocalDate.of(2026, 7, 26));

        ChainEventResponse response = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PREPROCESSING)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Nguyễn Văn Ghi")
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventService.recordPreprocessingEvent(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chain-events/preprocessing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("PREPROCESSING"));
    }

    @Test
    @DisplayName("Đính chính sự kiện sơ chế thành công")
    void correctPreprocessing_Success() throws Exception {
        UUID originalEventId = UUID.randomUUID();
        vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest request = new vn.nguongocso.event.dto.request.CorrectPreprocessingEventRequest();
        request.setInputQuantity(1000.0);
        request.setOutputQuantity(920.0);
        request.setPreprocessingDate(LocalDate.of(2026, 7, 26));
        request.setCorrectionReason("Cân lại chính xác là 920kg");

        ChainEventResponse response = ChainEventResponse.builder()
                .id(UUID.randomUUID())
                .eventType(ChainEventType.PREPROCESSING)
                .recordedAt(LocalDateTime.now())
                .recordedByName("Nguyễn Văn Ghi")
                .createdAt(LocalDateTime.now())
                .build();

        when(chainEventService.correctPreprocessingEvent(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chain-events/preprocessing/" + originalEventId + "/correct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("PREPROCESSING"));
    }
}
