package vn.nguongocso.farm.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.Role;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.DuplicateResourceException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UpdateProductionLotServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmAreaRepository farmAreaRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductionLotServiceImpl productionLotService;

    private UUID lotId;
    private UUID orgId;
    private UUID farmAreaId;
    private UUID categoryId;

    private CustomUserDetails userDetails;
    private Organization organization;
    private ProductionLot productionLot;
    private FarmArea farmArea;
    private ProductCategory productCategory;
    private UpdateProductionLotRequest request;

    @BeforeEach
    void setUp() {
        lotId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        farmAreaId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        // Mock Organization & User & Role
        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("HTX ABC");

        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Nguyễn Văn A");

        Role role = new Role();
        role.setCode("VT-02");
        role.setName("Quản lý");

        OrganizationUser orgUser = new OrganizationUser();
        orgUser.setOrganization(organization);
        orgUser.setUser(user);
        orgUser.setRole(role);

        userDetails = new CustomUserDetails(user, orgUser, role);

        // Mock FarmArea & ProductCategory & ProductionLot
        farmArea = new FarmArea();
        farmArea.setId(farmAreaId);
        farmArea.setOrganization(organization);

        productCategory = new ProductCategory();
        productCategory.setId(categoryId);
        productCategory.setIsActive(true);

        productionLot = new ProductionLot();
        productionLot.setId(lotId);
        productionLot.setOrganization(organization);
        productionLot.setStatus(ProductionLotStatus.DRAFT);
        productionLot.setFarmArea(farmArea);
        productionLot.setProductCategory(productCategory);

        // Mock Request
        request = new UpdateProductionLotRequest();
        request.setName("Lô cà chua mới");
        request.setFarmAreaId(farmAreaId);
        request.setProductCategoryId(categoryId);
        request.setExpectedQuantity(500.0);
        request.setPlantingDate(LocalDate.now());
    }

    @Test
    void update_shouldSuccess_whenDataIsValid() {
        // Given
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(productCategory));
        when(farmAreaRepository.findById(farmAreaId)).thenReturn(Optional.of(farmArea));
        when(productionLotRepository.save(any(ProductionLot.class))).thenReturn(productionLot);

        // When
        UpdateProductionLotResponse response = productionLotService.updateProductionLot(lotId, request, userDetails);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Lô cà chua mới");
        assertThat(response.getExpectedQuantity()).isEqualTo(500.0);
        verify(productionLotRepository, times(1)).save(any(ProductionLot.class));
        verify(eventPublisher).publishEvent(any(ActivityLogEvent.class));
    }

    @Test
    void update_shouldThrowNotFound_whenLotDoesNotExist() {
        // Given
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productionLotService.updateProductionLot(lotId, request, userDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lô sản xuất không tồn tại");
    }

    @Test
    void update_shouldThrowAccessDenied_whenLotBelongsToOtherOrg() {
        // Given
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        productionLot.setOrganization(otherOrg);

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> productionLotService.updateProductionLot(lotId, request, userDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Bạn không có quyền chỉnh sửa lô sản xuất này");
    }

    @Test
    void update_shouldThrowConflict_whenLotIsNotDraft() {
        // Given
        productionLot.setStatus(ProductionLotStatus.APPROVED);

        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));

        // When & Then
        assertThatThrownBy(() -> productionLotService.updateProductionLot(lotId, request, userDetails))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Chỉ có thể cập nhật lô sản xuất khi đang ở trạng thái nháp");
    }

    @Test
    void update_shouldThrow_whenFarmAreaIdIsMissing() {
        // Given
        request.setFarmAreaId(null);
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
        when(productCategoryRepository.findById(categoryId)).thenReturn(Optional.of(productCategory));

        // When & Then
        assertThatThrownBy(() -> productionLotService.updateProductionLot(lotId, request, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Vui lòng chọn vùng trồng");
    }
}
