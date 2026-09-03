package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.dto.request.CreateFarmAreaRequest;
import vn.nguongocso.farm.dto.response.FarmAreaResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.impl.FarmAreaServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.UpdateFarmAreaRequest;
import vn.nguongocso.farm.repository.ProductionLotRepository;

@ExtendWith(MockitoExtension.class)
class FarmAreaServiceTest {

    @Mock
    private FarmAreaRepository farmAreaRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private FarmAreaServiceImpl farmAreaService;

    private UUID organizationId;
    private Organization organization;
    private ProductCategory cropType;

    @BeforeEach
    void setUp() {
        farmAreaService = new FarmAreaServiceImpl(
                farmAreaRepository, productCategoryRepository, organizationRepository,
                productionLotRepository,
                new GeometryFactory(new PrecisionModel(), 4326), eventPublisher);

        organizationId = UUID.randomUUID();
        organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setName("HTX Nông Nghiệp Xanh");

        cropType = ProductCategory.builder()
                .id(UUID.randomUUID())
                .name("Chè Tân Cương")
                .isActive(true)
                .build();

        CustomUserDetails currentUser = mock(CustomUserDetails.class);
        lenient().when(currentUser.getOrganizationId()).thenReturn(organizationId);

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        lenient().when(productCategoryRepository.findById(cropType.getId())).thenReturn(Optional.of(cropType));
        // Giả lập @PrePersist của JPA: gán ID khi lưu để service ghi được audit log
        lenient().when(farmAreaRepository.save(any(FarmArea.class))).thenAnswer(invocation -> {
            FarmArea farmArea = invocation.getArgument(0);
            if (farmArea.getId() == null) {
                farmArea.setId(UUID.randomUUID());
            }
            return farmArea;
        });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CreateFarmAreaRequest buildRequest(BigDecimal area, AreaUnit areaUnit) {
        CreateFarmAreaRequest request = new CreateFarmAreaRequest();
        request.setName("Vùng chè Tân Cương");
        request.setCropType(cropType.getId());
        request.setLatitude(21.5);
        request.setLongitude(105.8);
        request.setArea(area);
        request.setAreaUnit(areaUnit);
        return request;
    }

    @Test
    void create_shouldKeepAreaUnchanged_whenUnitIsHectare() {
        CreateFarmAreaRequest request = buildRequest(new BigDecimal("5.5"), AreaUnit.HA);

        FarmAreaResponse response = farmAreaService.create(request);

        assertThat(response.getArea()).isEqualByComparingTo("5.5");
        assertThat(response.getAreaUnit()).isEqualTo(AreaUnit.HA);
    }

    @Test
    void create_shouldConvertAreaToHectare_whenUnitIsSquareKilometer() {
        CreateFarmAreaRequest request = buildRequest(new BigDecimal("1.5"), AreaUnit.KM2);

        FarmAreaResponse response = farmAreaService.create(request);

        assertThat(response.getArea()).isEqualByComparingTo("150");
        assertThat(response.getAreaUnit()).isEqualTo(AreaUnit.KM2);
    }

    @Test
    void create_shouldDefaultToHectare_whenAreaUnitIsNotProvided() {
        CreateFarmAreaRequest request = buildRequest(new BigDecimal("3"), null);

        FarmAreaResponse response = farmAreaService.create(request);

        assertThat(response.getArea()).isEqualByComparingTo("3");
        assertThat(response.getAreaUnit()).isEqualTo(AreaUnit.HA);
    }

    @Test
    void create_shouldPersistConvertedAreaAndOriginalUnit() {
        CreateFarmAreaRequest request = buildRequest(new BigDecimal("2"), AreaUnit.KM2);

        farmAreaService.create(request);

        ArgumentCaptor<FarmArea> captor = ArgumentCaptor.forClass(FarmArea.class);
        verify(farmAreaRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getArea()).isEqualByComparingTo("200");
        assertThat(captor.getValue().getAreaUnit()).isEqualTo(AreaUnit.KM2);
    }

    @Test
    void create_shouldPublishActivityLogWithCorrectActionAndOrganization() {
        CreateFarmAreaRequest request = buildRequest(new BigDecimal("5.5"), AreaUnit.HA);

        farmAreaService.create(request);

        // TASK-27: tạo vùng trồng phải ghi nhật ký hoạt động đúng action, đúng đối tượng
        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        ActivityLogEvent event = captor.getValue();
        assertThat(event.getAction()).isEqualTo("CREATE_FARM_AREA");
        assertThat(event.getEntityType()).isEqualTo("FARM_AREA");
        assertThat(event.getEntityId()).isNotBlank();
        assertThat(event.getOrganizationId()).isEqualTo(organizationId);
        assertThat(event.getDescription()).contains("Vùng chè Tân Cương");
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void update_shouldUpdateFieldsAndPublishLogWithOldValues_whenValid() {
        UUID farmAreaId = UUID.randomUUID();
        FarmArea existing = FarmArea.builder()
                .id(farmAreaId)
                .name("Vùng Cũ")
                .organization(organization)
                .cropType(cropType)
                .area(new BigDecimal("2.0"))
                .areaUnit(AreaUnit.HA)
                .isActive(true)
                .build();

        when(farmAreaRepository.findByIdAndOrganization_OrganizationId(farmAreaId, organizationId))
                .thenReturn(Optional.of(existing));
        when(productionLotRepository.countByFarmAreaId(farmAreaId)).thenReturn(3L);

        UpdateFarmAreaRequest updateRequest = new UpdateFarmAreaRequest(
                "Vùng Mới", cropType.getId(), 21.0, 105.0, new BigDecimal("10.0"), AreaUnit.HA
        );

        FarmAreaResponse response = farmAreaService.update(farmAreaId, updateRequest);

        assertThat(response.getName()).isEqualTo("Vùng Mới");
        assertThat(response.getArea()).isEqualByComparingTo("10.0");
        assertThat(response.getAssociatedLotsCount()).isEqualTo(3L);

        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("UPDATE_FARM_AREA");
        assertThat(captor.getValue().getDescription()).contains("Giá trị trước khi sửa").contains("3");
    }

    @Test
    void update_shouldThrowException_whenAreaIsNegativeOrZero() {
        UUID farmAreaId = UUID.randomUUID();
        FarmArea existing = FarmArea.builder()
                .id(farmAreaId)
                .name("Vùng Cũ")
                .organization(organization)
                .cropType(cropType)
                .area(new BigDecimal("2.0"))
                .areaUnit(AreaUnit.HA)
                .isActive(true)
                .build();

        when(farmAreaRepository.findByIdAndOrganization_OrganizationId(farmAreaId, organizationId))
                .thenReturn(Optional.of(existing));

        UpdateFarmAreaRequest invalidRequest = new UpdateFarmAreaRequest(
                "Vùng Mới", cropType.getId(), 21.0, 105.0, new BigDecimal("-1.0"), AreaUnit.HA
        );

        assertThatThrownBy(() -> farmAreaService.update(farmAreaId, invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Diện tích phải là số dương lớn hơn 0");
    }

    @Test
    void toggleStatus_shouldChangeIsActiveAndPublishEvent() {
        UUID farmAreaId = UUID.randomUUID();
        FarmArea existing = FarmArea.builder()
                .id(farmAreaId)
                .name("Vùng Thử Nghiệm")
                .organization(organization)
                .cropType(cropType)
                .isActive(true)
                .build();

        when(farmAreaRepository.findByIdAndOrganization_OrganizationId(farmAreaId, organizationId))
                .thenReturn(Optional.of(existing));

        FarmAreaResponse response = farmAreaService.toggleStatus(farmAreaId, false);

        assertThat(response.getIsActive()).isFalse();

        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("DEACTIVATE_FARM_AREA");
        assertThat(captor.getValue().getDescription()).contains("Ngừng sử dụng");
    }

    @Test
    void delete_shouldThrowException_whenAssociatedLotsExist() {
        UUID farmAreaId = UUID.randomUUID();
        FarmArea existing = FarmArea.builder()
                .id(farmAreaId)
                .name("Vùng Đã Co Lô")
                .organization(organization)
                .cropType(cropType)
                .isActive(true)
                .build();

        when(farmAreaRepository.findByIdAndOrganization_OrganizationId(farmAreaId, organizationId))
                .thenReturn(Optional.of(existing));
        when(productionLotRepository.countByFarmAreaId(farmAreaId)).thenReturn(2L);

        assertThatThrownBy(() -> farmAreaService.delete(farmAreaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể xóa vùng trồng đã có 2 lô sản xuất liên quan");

        verify(farmAreaRepository, never()).delete(any());
    }

    @Test
    void delete_shouldDeleteFarmArea_whenNoAssociatedLotsExist() {
        UUID farmAreaId = UUID.randomUUID();
        FarmArea existing = FarmArea.builder()
                .id(farmAreaId)
                .name("Vùng Rỗng")
                .organization(organization)
                .cropType(cropType)
                .isActive(true)
                .build();

        when(farmAreaRepository.findByIdAndOrganization_OrganizationId(farmAreaId, organizationId))
                .thenReturn(Optional.of(existing));
        when(productionLotRepository.countByFarmAreaId(farmAreaId)).thenReturn(0L);

        farmAreaService.delete(farmAreaId);

        verify(farmAreaRepository, times(1)).delete(existing);
        ArgumentCaptor<ActivityLogEvent> captor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("DELETE_FARM_AREA");
    }

    @Test
    void getAreaUnits_shouldReturnAllDeclaredUnits() {
        assertThat(farmAreaService.getAreaUnits()).containsExactly(AreaUnit.HA, AreaUnit.KM2);
    }
}