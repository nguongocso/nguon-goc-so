package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.enums.CertificationVerificationStatus;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.certification.service.InspectionEligibilityService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.CloneProductionLotRequest;
import vn.nguongocso.farm.dto.response.CloneProductionLotPreviewResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.HarvestEligibilityService;
import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.service.ReportAccessLogService;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.event.repository.ChainEventRepository;

/**
 * Test cho tạo lô sản xuất mới từ mẫu vụ trước (NCL-02-CN-007).
 */
@ExtendWith(MockitoExtension.class)
public class CloneProductionLotServiceTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private FarmAreaRepository farmAreaRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private ProductionLotCertificationRepository productionLotCertificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private InspectionEligibilityService inspectionEligibilityService;

    @Mock
    private ReportAccessLogService reportAccessLogService;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private InspectionRequestRepository inspectionRequestRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private HarvestEligibilityService harvestEligibilityService;

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @InjectMocks
    private ProductionLotServiceImpl productionLotService;

    private CustomUserDetails userDetails;
    private UUID orgId;
    private UUID userId;
    private UUID sourceLotId;
    private Organization org;
    private User user;
    private FarmArea farmArea;
    private ProductCategory productCategory;
    private ProductionLot sourceLot;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sourceLotId = UUID.randomUUID();
        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getOrganizationId()).thenReturn(orgId);
        lenient().when(userDetails.getUserId()).thenReturn(userId);

        org = new Organization();
        org.setOrganizationId(orgId);
        org.setName("HTX Test");

        user = new User();
        user.setUserId(userId);
        user.setFullName("Nguyen Van A");

        farmArea = new FarmArea();
        farmArea.setId(UUID.randomUUID());
        farmArea.setOrganization(org);
        farmArea.setName("Vung trong so 1");
        farmArea.setIsActive(true);

        productCategory = new ProductCategory();
        productCategory.setId(UUID.randomUUID());
        productCategory.setName("Lua");
        productCategory.setIsActive(true);

        sourceLot = ProductionLot.builder()
                .id(sourceLotId)
                .organization(org)
                .farmArea(farmArea)
                .productCategory(productCategory)
                .name("Lo lua vu he 2025")
                .expectedQuantity(1000.0)
                .expectedQuantityUnit("kg")
                .plantingDate(LocalDate.now().minusMonths(6))
                .status(ProductionLotStatus.APPROVED)
                .actualQuantity(900.0)
                .harvestDate(LocalDate.now().minusMonths(1))
                .createdBy(user)
                .build();
    }

    private void mockCurrentUserAndOrg() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
    }

    private Certification validCertification() {
        return Certification.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .name("VietGAP")
                .code("VG-001")
                .expiryDate(LocalDate.now().plusMonths(6))
                .verificationStatus(CertificationVerificationStatus.VERIFIED)
                .build();
    }

    private ProductionLotCertification association(UUID id, ProductionLot lot, Certification cert) {
        return ProductionLotCertification.builder()
                .id(id)
                .productionLot(lot)
                .certification(cert)
                .attachedBy(user)
                .note("ghi chu goc")
                .build();
    }

    private CloneProductionLotRequest cloneRequest() {
        CloneProductionLotRequest request = new CloneProductionLotRequest();
        request.setName("Lo lua vu dong xuan 2026");
        request.setExpectedQuantity(1200.0);
        request.setExpectedQuantityUnit("kg");
        request.setPlantingDate(LocalDate.now());
        return request;
    }

    @Test
    void cloneProductionLot_shouldCreateNewDraftLot_whenSourceIsValid() {
        // Given
        mockCurrentUserAndOrg();
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));
        Certification validCert = validCertification();
        UUID oldAssociationId = UUID.randomUUID();
        when(productionLotCertificationRepository.findByProductionLotId(sourceLotId))
                .thenReturn(List.of(association(oldAssociationId, sourceLot, validCert)));
        when(productionLotRepository.save(any(ProductionLot.class))).thenAnswer(invocation -> {
            ProductionLot lot = invocation.getArgument(0);
            lot.setId(UUID.randomUUID());
            return lot;
        });
        when(productionLotCertificationRepository.save(any(ProductionLotCertification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CloneProductionLotResponse response = productionLotService.cloneProductionLot(
                sourceLotId, cloneRequest(), userDetails);

        // Then
        assertThat(response.getLot().getStatus()).isEqualTo(ProductionLotStatus.DRAFT.name());
        assertThat(response.getLot().getId()).isNotEqualTo(sourceLotId);
        assertThat(response.getLot().getName()).isEqualTo("Lo lua vu dong xuan 2026");
        assertThat(response.getLot().getFarmAreaName()).isEqualTo("Vung trong so 1");
        assertThat(response.getLot().getProductCategoryName()).isEqualTo("Lua");
        assertThat(response.getLot().getCreatedByName()).isEqualTo("Nguyen Van A");
        assertThat(response.getCopiedCertifications()).hasSize(1);
        assertThat(response.getCopiedCertifications().get(0).getName()).isEqualTo("VietGAP");
        assertThat(response.getSkippedCertifications()).isEmpty();

        ArgumentCaptor<ProductionLot> lotCaptor = ArgumentCaptor.forClass(ProductionLot.class);
        verify(productionLotRepository).save(lotCaptor.capture());
        ProductionLot savedLot = lotCaptor.getValue();
        assertThat(savedLot.getId()).isNotEqualTo(sourceLotId);
        assertThat(savedLot.getStatus()).isEqualTo(ProductionLotStatus.DRAFT);
        assertThat(savedLot.getOrganization().getOrganizationId()).isEqualTo(orgId);
        assertThat(savedLot.getCreatedBy().getUserId()).isEqualTo(userId);
        assertThat(savedLot.getFarmArea()).isSameAs(farmArea);
        assertThat(savedLot.getProductCategory()).isSameAs(productCategory);
        assertThat(savedLot.getActualQuantity()).isNull();
        assertThat(savedLot.getHarvestDate()).isNull();

        // Lot mẫu không bị thay đổi
        assertThat(sourceLot.getId()).isEqualTo(sourceLotId);
        assertThat(sourceLot.getStatus()).isEqualTo(ProductionLotStatus.APPROVED);
        assertThat(sourceLot.getName()).isEqualTo("Lo lua vu he 2025");
        assertThat(sourceLot.getActualQuantity()).isEqualTo(900.0);

        // Liên kết chứng nhận mới trỏ tới certification hiện có, không tái sử dụng id cũ
        ArgumentCaptor<ProductionLotCertification> certCaptor = ArgumentCaptor
                .forClass(ProductionLotCertification.class);
        verify(productionLotCertificationRepository).save(certCaptor.capture());
        ProductionLotCertification newAssociation = certCaptor.getValue();
        assertThat(newAssociation.getId()).isNotEqualTo(oldAssociationId);
        assertThat(newAssociation.getCertification().getId()).isEqualTo(validCert.getId());
        assertThat(newAssociation.getAttachedBy().getUserId()).isEqualTo(userId);
        assertThat(newAssociation.getProductionLot().getId()).isEqualTo(response.getLot().getId());

        // Audit log cho lô mới
        ArgumentCaptor<ActivityLogEvent> eventCaptor = ArgumentCaptor.forClass(ActivityLogEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getAction()).isEqualTo("CREATE");
        assertThat(eventCaptor.getValue().getEntityType()).isEqualTo("ProductionLot");
        assertThat(eventCaptor.getValue().getEntityId()).isEqualTo(response.getLot().getId().toString());
    }

    @Test
    void cloneProductionLot_shouldReject_whenSourceBelongsToAnotherOrganization() {
        // Given
        mockCurrentUserAndOrg();
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(UUID.randomUUID());
        otherOrg.setName("HTX Khac");
        sourceLot.setOrganization(otherOrg);
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));

        // When / Then
        assertThatThrownBy(() -> productionLotService.cloneProductionLot(sourceLotId, cloneRequest(), userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thuộc tổ chức của bạn");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
        verify(productionLotCertificationRepository, never()).save(any(ProductionLotCertification.class));
    }

    @Test
    void cloneProductionLot_shouldReject_whenFarmAreaIsInactive() {
        // Given
        mockCurrentUserAndOrg();
        farmArea.setIsActive(false);
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));

        // When / Then
        assertThatThrownBy(() -> productionLotService.cloneProductionLot(sourceLotId, cloneRequest(), userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng sử dụng");
        verify(productionLotRepository, never()).save(any(ProductionLot.class));
    }

    @Test
    void getClonePreview_shouldReject_whenFarmAreaIsInactive() {
        // Given
        farmArea.setIsActive(false);
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));

        // When / Then
        assertThatThrownBy(() -> productionLotService.getClonePreview(sourceLotId, userDetails))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng sử dụng");
    }

    @Test
    void cloneProductionLot_shouldSkipExpiredCertification_butCopyValidOne() {
        // Given
        mockCurrentUserAndOrg();
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));

        Certification validCert = validCertification();
        Certification expiredCert = Certification.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .name("GlobalGAP")
                .code("GG-002")
                .expiryDate(LocalDate.now().minusDays(1))
                .verificationStatus(CertificationVerificationStatus.VERIFIED)
                .build();
        Certification rejectedCert = Certification.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .name("Organic")
                .code("OG-003")
                .expiryDate(LocalDate.now().plusMonths(3))
                .verificationStatus(CertificationVerificationStatus.REJECTED)
                .build();
        List<ProductionLotCertification> sourceAssociations = List.of(
                association(UUID.randomUUID(), sourceLot, validCert),
                association(UUID.randomUUID(), sourceLot, expiredCert),
                association(UUID.randomUUID(), sourceLot, rejectedCert));
        when(productionLotCertificationRepository.findByProductionLotId(sourceLotId))
                .thenReturn(sourceAssociations);
        when(productionLotRepository.save(any(ProductionLot.class))).thenAnswer(invocation -> {
            ProductionLot lot = invocation.getArgument(0);
            lot.setId(UUID.randomUUID());
            return lot;
        });
        when(productionLotCertificationRepository.save(any(ProductionLotCertification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CloneProductionLotResponse response = productionLotService.cloneProductionLot(
                sourceLotId, cloneRequest(), userDetails);

        // Then
        assertThat(response.getCopiedCertifications()).hasSize(1);
        assertThat(response.getCopiedCertifications().get(0).getName()).isEqualTo("VietGAP");
        assertThat(response.getSkippedCertifications())
                .extracting("name")
                .containsExactlyInAnyOrder("GlobalGAP", "Organic");
        assertThat(response.getWarnings()).hasSize(2);
        verify(productionLotCertificationRepository, times(1)).save(any(ProductionLotCertification.class));

        // Liên kết của lô mẫu không thay đổi
        assertThat(sourceAssociations).hasSize(3);
        verify(productionLotCertificationRepository, never()).delete(any(ProductionLotCertification.class));
    }

    @Test
    void cloneProductionLot_shouldNotCopyBusinessHistory() {
        // Given
        mockCurrentUserAndOrg();
        sourceLot.setApprovalNotes("da duyet");
        sourceLot.setCancellationReason("ly do cu");
        sourceLot.setDisposalReason("loai bo cu");
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));
        when(productionLotCertificationRepository.findByProductionLotId(sourceLotId))
                .thenReturn(List.of());
        when(productionLotRepository.save(any(ProductionLot.class))).thenAnswer(invocation -> {
            ProductionLot lot = invocation.getArgument(0);
            lot.setId(UUID.randomUUID());
            return lot;
        });

        // When
        CloneProductionLotResponse response = productionLotService.cloneProductionLot(
                sourceLotId, cloneRequest(), userDetails);

        // Then
        ArgumentCaptor<ProductionLot> lotCaptor = ArgumentCaptor.forClass(ProductionLot.class);
        verify(productionLotRepository).save(lotCaptor.capture());
        ProductionLot savedLot = lotCaptor.getValue();
        assertThat(savedLot.getId()).isNotEqualTo(sourceLotId);
        assertThat(savedLot.getStatus()).isEqualTo(ProductionLotStatus.DRAFT);
        assertThat(savedLot.getActualQuantity()).isNull();
        assertThat(savedLot.getHarvestDate()).isNull();
        assertThat(savedLot.getApprovalNotes()).isNull();
        assertThat(savedLot.getApprovedBy()).isNull();
        assertThat(savedLot.getCancellationReason()).isNull();
        assertThat(savedLot.getCancellationNote()).isNull();
        assertThat(savedLot.getCancelledBy()).isNull();
        assertThat(savedLot.getCancelledAt()).isNull();
        assertThat(savedLot.getDisposalReason()).isNull();
        assertThat(savedLot.getHandlingMeasure()).isNull();
        assertThat(savedLot.getDisposalNote()).isNull();
        assertThat(savedLot.getDisposedBy()).isNull();
        assertThat(savedLot.getDisposedAt()).isNull();
        assertThat(response.getLot().getId()).isNotEqualTo(sourceLotId);

        // Không chạm tới kho vận hành của lô cũ (lô hàng, sự kiện chuỗi)
        verifyNoInteractions(farmAreaRepository);
    }

    @Test
    void getClonePreview_shouldReturnPrefillData_withCertificationWarnings() {
        // Given
        when(productionLotRepository.findById(sourceLotId)).thenReturn(Optional.of(sourceLot));
        Certification validCert = validCertification();
        Certification expiredCert = Certification.builder()
                .id(UUID.randomUUID())
                .organization(org)
                .name("GlobalGAP")
                .code("GG-002")
                .expiryDate(LocalDate.now().minusDays(1))
                .verificationStatus(CertificationVerificationStatus.VERIFIED)
                .build();
        when(productionLotCertificationRepository.findByProductionLotId(sourceLotId))
                .thenReturn(List.of(
                        association(UUID.randomUUID(), sourceLot, validCert),
                        association(UUID.randomUUID(), sourceLot, expiredCert)));

        // When
        CloneProductionLotPreviewResponse preview = productionLotService.getClonePreview(sourceLotId, userDetails);

        // Then
        assertThat(preview.getSourceLotId()).isEqualTo(sourceLotId);
        assertThat(preview.getSourceLotName()).isEqualTo("Lo lua vu he 2025");
        assertThat(preview.getFarmAreaId()).isEqualTo(farmArea.getId());
        assertThat(preview.getFarmAreaName()).isEqualTo("Vung trong so 1");
        assertThat(preview.getProductCategoryId()).isEqualTo(productCategory.getId());
        assertThat(preview.getExpectedQuantity()).isEqualTo(1000.0);
        assertThat(preview.getActiveCertifications()).hasSize(1);
        assertThat(preview.getSkippedCertifications()).hasSize(1);
        assertThat(preview.getWarnings()).hasSize(1);
        assertThat(preview.getWarnings().get(0)).contains("GlobalGAP");
    }
}
