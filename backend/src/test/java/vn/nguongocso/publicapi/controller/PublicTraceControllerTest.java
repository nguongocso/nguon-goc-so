package vn.nguongocso.publicapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.publicapi.dto.response.PublicTraceResponse;
import vn.nguongocso.publicapi.service.PublicTraceService;

@WebMvcTest(PublicTraceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PublicTraceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicTraceService publicTraceService;

    // Security mocks required by SecurityConfig / JwtAuthenticationFilter.
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private PublicTraceResponse buildResponse(String codeValue) {
        return PublicTraceResponse.builder()
                .codeValue(codeValue)
                .productionLotId(UUID.randomUUID())
                .productName("Che Tan Cuong")
                .shipmentCode(UUID.randomUUID().toString())
                .shipmentStatus("ACTIVATED")
                .recalled(false)
                .recallMessage(null)
                .locked(false)
                .lockReason(null)
                .lockedAt(null)
                .events(Collections.emptyList())
                .build();
    }

    /**
     * GET /public/trace/{codeValue} = lookup đọc thuần túy.
     * Không gọi recordPublicScan (không tạo ScanLog, không kích hoạt đánh giá nghi vấn).
     */
    @Test
    void getPublicTrace_ShouldDelegateToReadOnlyLookup_AndNotRecordScan() throws Exception {
        String codeValue = "TEST123";
        PublicTraceResponse response = buildResponse(codeValue);

        when(publicTraceService.getPublicTrace(
                eq(codeValue), any(), any(), anyString(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/public/trace/{codeValue}", codeValue)
                        .param("latitude", "21.0285")
                        .param("longitude", "105.8542"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value(codeValue));

        verify(publicTraceService, never()).recordPublicScan(any(), any(), any(), any(), any());
    }

    /**
     * POST /public/trace/{codeValue}/scan = quét QR thực tế.
     * Gọi recordPublicScan (tạo ScanLog + kích hoạt đánh giá nghi vấn).
     */
    @Test
    void recordPublicScan_ShouldDelegateToScanService() throws Exception {
        String codeValue = "TEST123";
        PublicTraceResponse response = buildResponse(codeValue);

        when(publicTraceService.recordPublicScan(
                eq(codeValue), eq(21.0285), eq(105.8542), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/public/trace/{codeValue}/scan", codeValue)
                        .param("latitude", "21.0285")
                        .param("longitude", "105.8542"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codeValue").value(codeValue));

        verify(publicTraceService).recordPublicScan(
                eq(codeValue), eq(21.0285), eq(105.8542), anyString(), any());
    }

    /**
     * POST /scan vẫn hoạt động khi không có kinh vĩ độ (các tham số tùy chọn).
     */
    @Test
    void recordPublicScan_ShouldAcceptMissingCoordinates() throws Exception {
        String codeValue = "TEST123";
        PublicTraceResponse response = buildResponse(codeValue);

        when(publicTraceService.recordPublicScan(
                eq(codeValue), isNull(), isNull(), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/public/trace/{codeValue}/scan", codeValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(publicTraceService).recordPublicScan(
                eq(codeValue), isNull(), isNull(), anyString(), any());
    }
}