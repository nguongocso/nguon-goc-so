package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.config.FarmAreaBoundaryProperties;
import vn.nguongocso.farm.dto.request.LatLngDto;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaBoundaryRequest;
import vn.nguongocso.farm.dto.response.FarmAreaBoundaryResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.service.impl.FarmAreaBoundaryServiceImpl;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class FarmAreaBoundaryServiceImplTest {

    @Mock
    private FarmAreaRepository farmAreaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CustomUserDetails currentUser;

    private GeometryFactory geometryFactory;
    private FarmAreaBoundaryServiceImpl service;
    private UUID organizationId;
    private UUID farmAreaId;
    private FarmArea farmArea;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        FarmAreaBoundaryProperties properties = new FarmAreaBoundaryProperties();
        properties.setBoundaryDeviationThresholdPercent(new BigDecimal("30.0"));
        service = new FarmAreaBoundaryServiceImpl(
                farmAreaRepository, geometryFactory, properties, new ObjectMapper(), eventPublisher);

        organizationId = UUID.randomUUID();
        farmAreaId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName("HTX Tân Cương");

        farmArea = FarmArea.builder()
                .id(farmAreaId)
                .name("Vùng chè Tân Cương")
                .organization(organization)
                .area(new BigDecimal("1.2000"))
                .areaUnit(AreaUnit.HA)
                .build();

        when(currentUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);
        lenient().when(currentUser.getUserId()).thenReturn(UUID.randomUUID());
        lenient().when(currentUser.getUsername()).thenReturn("quanly");
        lenient().when(currentUser.getFullName()).thenReturn("Nguyễn Văn Quản Lý");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateBoundary_shouldSavePolygonAndCalculatedArea_whenValid() {
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));
        UpdateFarmAreaBoundaryRequest request = request(validSquare(), false);

        FarmAreaBoundaryResponse response = service.updateBoundary(farmAreaId, request);

        assertThat(farmArea.getBoundary()).isNotNull();
        assertThat(farmArea.getBoundary().getSRID()).isEqualTo(4326);
        assertThat(farmArea.getBoundary().getCoordinates()).hasSize(5);
        assertThat(farmArea.getCalculatedArea()).isPositive().hasScaleOf(4);
        assertThat(farmArea.getBoundaryUpdatedAt()).isNotNull();
        assertThat(response.getPoints()).hasSize(4);
        assertThat(response.getDeclaredAreaUnit()).isEqualTo(AreaUnit.HA);
        verify(farmAreaRepository).save(farmArea);
    }

    @Test
    void updateBoundary_shouldRequireConfirmation_whenDeviationExceedsThreshold() {
        farmArea.setArea(new BigDecimal("0.1000"));
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));

        assertThatThrownBy(() -> service.updateBoundary(farmAreaId, request(validSquare(), false)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getDetails()).isInstanceOf(Map.class);
                    assertThat(((Map<?, ?>) exception.getDetails()).get("code"))
                            .isEqualTo("AREA_DEVIATION_CONFIRMATION_REQUIRED");
                });

        verify(farmAreaRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateBoundary_shouldSave_whenDeviationConfirmed() {
        farmArea.setArea(new BigDecimal("0.1000"));
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));

        FarmAreaBoundaryResponse response = service.updateBoundary(farmAreaId, request(validSquare(), true));

        assertThat(response.getCalculatedArea()).isPositive();
        verify(farmAreaRepository).save(farmArea);
    }

    @Test
    void updateBoundary_shouldRejectSelfIntersectingPolygon() {
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));
        List<LatLngDto> bowTie = List.of(
                point(21.0000, 105.0000), point(21.0010, 105.0010),
                point(21.0000, 105.0010), point(21.0010, 105.0000));

        assertThatThrownBy(() -> service.updateBoundary(farmAreaId, request(bowTie, false)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(((Map<?, ?>) exception.getDetails()).get("code"))
                            .isEqualTo("SELF_INTERSECTING_BOUNDARY");
                });
    }

    @Test
    void updateBoundary_shouldRejectLessThanThreePoints() {
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));

        assertThatThrownBy(() -> service.updateBoundary(farmAreaId,
                request(List.of(point(21.0, 105.0), point(21.1, 105.1)), false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(((Map<?, ?>) exception.getDetails()).get("code"))
                                .isEqualTo("INVALID_BOUNDARY_POINTS"));
    }

    @Test
    void updateBoundary_shouldRejectDifferentOrganization() {
        Organization otherOrganization = new Organization();
        otherOrganization.setOrganizationId(UUID.randomUUID());
        farmArea.setOrganization(otherOrganization);
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));

        assertThatThrownBy(() -> service.updateBoundary(farmAreaId, request(validSquare(), false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(farmAreaRepository, never()).save(any());
    }

    @Test
    void updateBoundary_shouldRejectWrongRole() {
        when(currentUser.getRoleCode()).thenReturn("VT-03");

        assertThatThrownBy(() -> service.updateBoundary(farmAreaId, request(validSquare(), false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(farmAreaRepository, never()).findById(any());
    }

    @Test
    void updateBoundary_shouldPublishOldAndNewSnapshots() {
        farmArea.setBoundary(createPolygon(validSquare()));
        farmArea.setCalculatedArea(new BigDecimal("1.1000"));
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));

        service.updateBoundary(farmAreaId, request(List.of(
                point(21.0000, 105.0000), point(21.0000, 105.0012),
                point(21.0010, 105.0012), point(21.0010, 105.0000)), false));

        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ActivityLogEvent event = captor.getValue();
        assertThat(event.getAction()).isEqualTo("UPDATE_FARM_AREA_BOUNDARY");
        assertThat(event.getActorRole()).isEqualTo("VT-02");
        assertThat(event.getEntityId()).isEqualTo(farmAreaId.toString());
        assertThat(event.getBeforeValue()).contains("\"schemaVersion\":1", "\"calculatedArea\":1.1000");
        assertThat(event.getAfterValue()).contains("\"schemaVersion\":1", "\"points\"");
    }

    @Test
    void getBoundary_shouldReturnEmptyPoints_whenBoundaryDoesNotExist() {
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));

        FarmAreaBoundaryResponse response = service.getBoundary(farmAreaId);

        assertThat(response.getPoints()).isEmpty();
        assertThat(response.getCalculatedArea()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    private List<LatLngDto> validSquare() {
        return List.of(
                point(21.0000, 105.0000), point(21.0000, 105.0010),
                point(21.0010, 105.0010), point(21.0010, 105.0000));
    }

    private LatLngDto point(double latitude, double longitude) {
        return new LatLngDto(latitude, longitude);
    }

    private UpdateFarmAreaBoundaryRequest request(List<LatLngDto> points, boolean confirmed) {
        return new UpdateFarmAreaBoundaryRequest(points, confirmed);
    }

    private Polygon createPolygon(List<LatLngDto> points) {
        Coordinate[] coordinates = new Coordinate[points.size() + 1];
        for (int index = 0; index < points.size(); index++) {
            coordinates[index] = new Coordinate(points.get(index).getLongitude(), points.get(index).getLatitude());
        }
        coordinates[points.size()] = new Coordinate(coordinates[0]);
        Polygon polygon = geometryFactory.createPolygon(coordinates);
        polygon.setSRID(4326);
        return polygon;
    }
}
