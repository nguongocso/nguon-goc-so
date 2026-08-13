package vn.nguongocso.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.auth.service.CustomUserDetailsService;
import vn.nguongocso.config.JwtTokenProvider;
import vn.nguongocso.config.SecurityConfig;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.report.dto.response.GS1DossierExportResponse;
import vn.nguongocso.report.exception.DossierValidationException;
import vn.nguongocso.report.service.DossierService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller tests for the GS1 dossier export endpoint (NCL-12-CN-003).
 *
 * <p>Follows the existing project controller-test conventions
 * (&#64;WebMvcTest + MockitoBean + SecurityMockMvcRequestPostProcessors.user).</p>
 */
@WebMvcTest(DossierController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
public class Gs1DossierExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DossierService dossierService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PermissionChecker permissionChecker;

    private UUID shipmentId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        SecurityContextHolder.clearContext();

        userDetails = mock(CustomUserDetails.class);
        when(userDetails.getUserId()).thenReturn(UUID.randomUUID());
        when(userDetails.getOrganizationId()).thenReturn(UUID.randomUUID());
        when(userDetails.getRoleCode()).thenReturn("VT-02");

        // Use doReturn to avoid the generic compiler issue (same as existing tests)
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_VT-02")))
                .when(userDetails).getAuthorities();

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private GS1DossierExportResponse buildDossier() {
        GS1DossierExportResponse.ShipmentInfo shipmentInfo = GS1DossierExportResponse.ShipmentInfo.builder()
                .id(shipmentId)
                .name("Lo che Tan Cuong T8/2026")
                .productCategory("Chè")
                .totalQuantity(500L)
                .unit("kg")
                .status("PACKAGED")
                .build();

        GS1DossierExportResponse.OrganizationInfo orgInfo = GS1DossierExportResponse.OrganizationInfo.builder()
                .id(UUID.randomUUID())
                .name("HTX Chè Tân Cương")
                .code("HTX-TC")
                .build();
        shipmentInfo.setOrganization(orgInfo);

        vn.nguongocso.report.dto.response.GS1Event event =
                vn.nguongocso.report.dto.response.GS1Event.builder()
                        .eventId(UUID.randomUUID())
                        .eventType("HARVEST")
                        .eventTypeLabel("Thu hoạch")
                        .recordedAt(LocalDateTime.of(2026, 8, 11, 10, 0))
                        .recordedBy("Nguyễn Văn A")
                        .location(vn.nguongocso.report.dto.response.EventLocation.builder()
                                .latitude(21.0285)
                                .longitude(105.8542)
                                .build())
                        .details(Map.of("productionLotName", "Lo che Tan Cuong T8/2026"))
                        .build();

        return GS1DossierExportResponse.builder()
                .shipment(shipmentInfo)
                .events(List.of(event))
                .mapping(Map.of("ChainEvent.eventType", "eventTypeCode"))
                .warnings(List.of())
                .exportedAt(LocalDateTime.of(2026, 8, 12, 10, 0))
                .exportedBy("Nguyễn Văn C")
                .schemaVersion("1.0.0")
                .schemaDescription("Mô phỏng lược đồ GS1, không phải chứng nhận tuân thủ GS1")
                .build();
    }

    // TC-01 / TC-07: JSON default export success with mapping
    @Test
    void exportGs1Dossier_json_defaults_successWithMapping() throws Exception {
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), eq(true)))
                .thenReturn(buildDossier());

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.shipment.name").value("Lo che Tan Cuong T8/2026"))
                .andExpect(jsonPath("$.data.events[0].eventType").value("HARVEST"))
                .andExpect(jsonPath("$.data.events[0].location.latitude").value(21.0285))
                .andExpect(jsonPath("$.data.events[0].details.productionLotName").value("Lo che Tan Cuong T8/2026"))
                .andExpect(jsonPath("$.data.mapping['ChainEvent.eventType']").value("eventTypeCode"))
                .andExpect(jsonPath("$.data.exportedBy").value("Nguyễn Văn C"))
                .andExpect(jsonPath("$.data.schemaVersion").value("1.0.0"));
    }

    // includeMapping=true
    @Test
    void exportGs1Dossier_json_includeMappingTrue_containsMapping() throws Exception {
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), eq(true)))
                .thenReturn(buildDossier());

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails))
                        .param("format", "json")
                        .param("includeMapping", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mapping['ChainEvent.eventType']").value("eventTypeCode"));
    }

    // includeMapping=false
    @Test
    void exportGs1Dossier_json_includeMappingFalse_noMapping() throws Exception {
        GS1DossierExportResponse dossier = buildDossier();
        dossier.setMapping(null);
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), eq(false)))
                .thenReturn(dossier);

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails))
                        .param("includeMapping", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mapping").doesNotExist())
                .andExpect(jsonPath("$.data.events[0].eventType").value("HARVEST"));
    }

    // TC-08: XML format with correct content type
    @Test
    void exportGs1Dossier_xml_successWithXmlContentType() throws Exception {
        when(dossierService.exportGs1DossierXml(eq(shipmentId), any(CustomUserDetails.class), eq(true)))
                .thenReturn("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gs1Dossier><shipment>"
                        + "<id>" + shipmentId + "</id>"
                        + "<name>Lo che Tan Cuong T8/2026</name>"
                        + "</shipment><events><event><eventType>HARVEST</eventType>"
                        + "<recordedBy>Nguyễn Văn A</recordedBy>"
                        + "</event></events><schemaVersion>1.0.0</schemaVersion></gs1Dossier>");

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails))
                        .param("format", "xml")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<gs1Dossier>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<eventType>HARVEST</eventType>")));
    }

    // TC-09: invalid format -> 400
    @Test
    void exportGs1Dossier_invalidFormat_rejectedWith400() throws Exception {
        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails))
                        .param("format", "pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Định dạng xuất không được hỗ trợ. Chỉ hỗ trợ json hoặc xml."));
    }

    // TC-04: shipment not found -> 404
    @Test
    void exportGs1Dossier_shipmentNotFound_returns404() throws Exception {
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy lô hàng."));

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    // TC-05: QTN-11 failure -> 400 with missing conditions
    @Test
    void exportGs1Dossier_qtn11Failure_returns400() throws Exception {
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), anyBoolean()))
                .thenThrow(new DossierValidationException(
                        "Không đủ điều kiện xuất hồ sơ truy xuất: Lô hàng chưa hoàn tất hoặc thiếu chứng từ bắt buộc.",
                        List.of("Thiếu chứng từ PLANTING", "Thiếu chứng từ FERTILIZING")));

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0]").value("Thiếu chứng từ PLANTING"));
    }

    // TC-06: empty events -> 400
    @Test
    void exportGs1Dossier_noEvents_returns400() throws Exception {
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), anyBoolean()))
                .thenThrow(new BusinessException("Lô chưa có sự kiện nào để xuất hồ sơ."));

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Lô chưa có sự kiện nào để xuất hồ sơ."));
    }

    // TC-03: organization access violation -> 403 (Spring Security exception translation)
    @Test
    void exportGs1Dossier_orgAccessDenied_returns403() throws Exception {
        when(dossierService.exportGs1Dossier(eq(shipmentId), any(CustomUserDetails.class), anyBoolean()))
                .thenThrow(new AccessDeniedException(
                        "Từ chối thao tác: Bạn không có quyền truy cập lô hàng này."));

        mockMvc.perform(get("/api/v1/shipments/{shipmentId}/export-gs1-dossier", shipmentId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());
    }
}