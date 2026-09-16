package vn.nguongocso.farm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.farm.dto.request.LatLngDto;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaBoundaryRequest;
import vn.nguongocso.farm.dto.response.FarmAreaBoundaryResponse;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.service.FarmAreaBoundaryService;
import vn.nguongocso.farm.service.FarmAreaService;
import vn.nguongocso.permission.service.PermissionChecker;

@WebMvcTest(FarmAreaController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class FarmAreaBoundaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FarmAreaService farmAreaService;

    @MockitoBean
    private FarmAreaBoundaryService farmAreaBoundaryService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VT-02")
    void updateBoundary_shouldReturnOk_forManager() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateFarmAreaBoundaryRequest request = validRequest();
        when(farmAreaBoundaryService.updateBoundary(eq(id), any(UpdateFarmAreaBoundaryRequest.class)))
                .thenReturn(response(id));

        mockMvc.perform(put("/api/v1/farm-areas/{id}/boundary", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.thresholdPercentage").value(30.0))
                .andExpect(jsonPath("$.data.points.length()").value(3));

        verify(permissionChecker).check("FARM_AREA", "UPDATE");
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void updateBoundary_shouldReturnForbidden_forEventRecorder() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/farm-areas/{id}/boundary", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verify(farmAreaBoundaryService, never()).updateBoundary(any(), any());
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void updateBoundary_shouldReturnBadRequest_whenLessThanThreePoints() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateFarmAreaBoundaryRequest request = new UpdateFarmAreaBoundaryRequest(
                List.of(new LatLngDto(21.0, 105.0), new LatLngDto(21.1, 105.1)), false);

        mockMvc.perform(put("/api/v1/farm-areas/{id}/boundary", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.points").exists());

        verify(farmAreaBoundaryService, never()).updateBoundary(any(), any());
    }

    @Test
    @WithMockUser(roles = "VT-02")
    void updateBoundary_shouldReturnBadRequest_whenMoreThanFiveHundredPoints() throws Exception {
        UUID id = UUID.randomUUID();
        List<LatLngDto> points = new java.util.ArrayList<>();
        for (int index = 0; index < 501; index++) {
            points.add(new LatLngDto(21.0 + index * 0.000001, 105.0 + index * 0.000001));
        }
        UpdateFarmAreaBoundaryRequest request = new UpdateFarmAreaBoundaryRequest(points, false);

        mockMvc.perform(put("/api/v1/farm-areas/{id}/boundary", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.points").exists());

        verify(farmAreaBoundaryService, never()).updateBoundary(any(), any());
    }

    @Test
    @WithMockUser(roles = "VT-03")
    void getBoundary_shouldReturnOk_forReadableRole() throws Exception {
        UUID id = UUID.randomUUID();
        when(farmAreaBoundaryService.getBoundary(id)).thenReturn(response(id));

        mockMvc.perform(get("/api/v1/farm-areas/{id}/boundary", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calculatedArea").value(1.1));

        verify(permissionChecker).check("FARM_AREA", "READ");
    }

    private UpdateFarmAreaBoundaryRequest validRequest() {
        return new UpdateFarmAreaBoundaryRequest(List.of(
                new LatLngDto(21.0, 105.0),
                new LatLngDto(21.0, 105.001),
                new LatLngDto(21.001, 105.0)), false);
    }

    private FarmAreaBoundaryResponse response(UUID id) {
        return FarmAreaBoundaryResponse.builder()
                .id(id)
                .name("Vùng chè")
                .organizationId(UUID.randomUUID())
                .declaredArea(new BigDecimal("1.0"))
                .declaredAreaUnit(AreaUnit.HA)
                .calculatedArea(new BigDecimal("1.1"))
                .points(validRequest().getPoints())
                .areaDeviationPercentage(new BigDecimal("10.0"))
                .thresholdPercentage(new BigDecimal("30.0"))
                .build();
    }
}
