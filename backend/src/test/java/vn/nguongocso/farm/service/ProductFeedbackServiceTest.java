package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.dto.response.PublicProductFeedbackCreatedResponse;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.event.ProductFeedbackSubmittedEvent;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductFeedbackServiceImpl;
import vn.nguongocso.farm.service.ProductFeedbackLookupCodeGenerator;
import vn.nguongocso.farm.service.ProductFeedbackLookupCodeGenerator.GeneratedLookupCode;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.recall.service.RecallRequestService;
import vn.nguongocso.trace.repository.TraceCodeRepository;

@ExtendWith(MockitoExtension.class)
class ProductFeedbackServiceTest {

    @Mock
    private ProductFeedbackRepository productFeedbackRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private RecallRequestRepository recallRequestRepository;

    @Mock
    private RecallRequestService recallRequestService;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductFeedbackLookupCodeGenerator lookupCodeGenerator;

    @InjectMocks
    private ProductFeedbackServiceImpl productFeedbackService;

    private UUID lotId;
    private ProductionLot productionLot;
    private CreateProductFeedbackRequest request;

    @BeforeEach
    void setUp() {
        lotId = UUID.randomUUID();
        Organization organization = Organization.builder()
                .organizationId(UUID.randomUUID())
                .name("HTX Chè Long Cốc")
                .build();

        productionLot = ProductionLot.builder()
                .id(lotId)
                .name("Lô chè xuân 2026")
                .organization(organization)
                .build();

        request = new CreateProductFeedbackRequest();
        request.setContent("Nghi ngờ tem giả");
    }

    @Test
    void createFeedback_shouldSuccess_whenLotExists() {
        // Given
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
        when(lookupCodeGenerator.generate()).thenReturn(new GeneratedLookupCode(
                "PA-7K2M-9Q4X-H8NP-3R5T",
                "lookup-hash"));
        when(productFeedbackRepository.existsByLookupCodeHash("lookup-hash")).thenReturn(false);

        ProductFeedback mockSaved = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(productionLot)
                .content(request.getContent())
                .build();
        when(productFeedbackRepository.save(any(ProductFeedback.class))).thenReturn(mockSaved);

        // When
        PublicProductFeedbackCreatedResponse response = productFeedbackService.createFeedback(lotId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getProductionLotId()).isEqualTo(lotId);
        assertThat(response.getLookupCode()).isEqualTo("PA-7K2M-9Q4X-H8NP-3R5T");

        ArgumentCaptor<ProductFeedback> feedbackCaptor = ArgumentCaptor.forClass(ProductFeedback.class);
        verify(productFeedbackRepository).save(feedbackCaptor.capture());
        assertThat(feedbackCaptor.getValue().getLookupCodeHash()).isEqualTo("lookup-hash");

        // Verify Event
        ArgumentCaptor<ProductFeedbackSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ProductFeedbackSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ProductFeedbackSubmittedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getProductionLotId()).isEqualTo(lotId);
        assertThat(publishedEvent.getContent()).isEqualTo(request.getContent());
    }

    @Test
    void createFeedback_shouldThrowNotFound_whenLotNotExists() {
        // Given
        when(productionLotRepository.findById(lotId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productFeedbackService.createFeedback(lotId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất");

        verify(productFeedbackRepository, never()).save(any(ProductFeedback.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void lookupPublicFeedback_shouldReturnOnlyPublicFields() {
        String lookupCode = "PA-7K2M-9Q4X-H8NP-3R5T";
        ProductFeedback feedback = ProductFeedback.builder()
                .productionLot(productionLot)
                .content("Nghi ngờ tem giả")
                .status(vn.nguongocso.farm.enums.ProductFeedbackStatus.IN_PROGRESS)
                .publicResponse("Đơn vị phụ trách đang xác minh.")
                .processingContent("Nội dung nội bộ")
                .build();
        when(lookupCodeGenerator.hash(lookupCode)).thenReturn("lookup-hash");
        when(productFeedbackRepository.findByLookupCodeHash("lookup-hash"))
                .thenReturn(Optional.of(feedback));

        var response = productFeedbackService.lookupPublicFeedback(lookupCode);

        assertThat(response.getStatus())
                .isEqualTo(vn.nguongocso.farm.enums.ProductFeedbackStatus.IN_PROGRESS);
        assertThat(response.getPublicResponse()).isEqualTo("Đơn vị phụ trách đang xác minh.");
    }

    @Test
    void lookupPublicFeedback_shouldReturnGenericNotFound_whenCodeIsInvalid() {
        when(lookupCodeGenerator.hash("invalid"))
                .thenThrow(new IllegalArgumentException("Mã tra cứu không hợp lệ"));

        assertThatThrownBy(() -> productFeedbackService.lookupPublicFeedback("invalid"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy phản ánh");

        verify(productFeedbackRepository, never()).findByLookupCodeHash(any());
    }

    @Test
    void lookupPublicFeedback_shouldReturnGenericNotFound_whenHashDoesNotExist() {
        when(lookupCodeGenerator.hash("PA-7K2M-9Q4X-H8NP-3R5T")).thenReturn("missing-hash");
        when(productFeedbackRepository.findByLookupCodeHash("missing-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productFeedbackService
                .lookupPublicFeedback("PA-7K2M-9Q4X-H8NP-3R5T"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy phản ánh");
    }

    @Test
    void getFeedbackById_shouldUseGlobalLookup_forPlatformAdmin() {
        UUID feedbackId = UUID.randomUUID();
        ProductFeedback feedback = ProductFeedback.builder()
                .id(feedbackId)
                .productionLot(productionLot)
                .content("Nghi ngờ tem giả")
                .build();
        CustomUserDetails currentUser = mock(CustomUserDetails.class);

        when(currentUser.getRoleCode()).thenReturn("VT-01");
        when(productFeedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);

            ProductFeedbackResponse response = productFeedbackService.getFeedbackById(feedbackId);

            assertThat(response.getId()).isEqualTo(feedbackId);
            verify(productFeedbackRepository).findById(feedbackId);
            verify(productFeedbackRepository, never())
                    .findByIdAndProductionLot_Organization_OrganizationId(any(), any());
        }
    }

    @Test
    void getFeedbackById_shouldUseOrganizationScopedLookup_forCooperativeManager() {
        UUID feedbackId = UUID.randomUUID();
        UUID organizationId = productionLot.getOrganization().getOrganizationId();
        ProductFeedback feedback = ProductFeedback.builder()
                .id(feedbackId)
                .productionLot(productionLot)
                .content("Nghi ngờ chất lượng")
                .build();
        CustomUserDetails currentUser = mock(CustomUserDetails.class);

        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(productFeedbackRepository.findByIdAndProductionLot_Organization_OrganizationId(
                feedbackId, organizationId)).thenReturn(Optional.of(feedback));

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);

            ProductFeedbackResponse response = productFeedbackService.getFeedbackById(feedbackId);

            assertThat(response.getId()).isEqualTo(feedbackId);
            verify(productFeedbackRepository, never()).findById(feedbackId);
        }
    }

    @Test
    void getFeedbackById_shouldHideFeedbackFromAnotherOrganization() {
        UUID feedbackId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CustomUserDetails currentUser = mock(CustomUserDetails.class);

        when(currentUser.getRoleCode()).thenReturn("VT-02");
        when(currentUser.getOrganizationId()).thenReturn(organizationId);
        when(productFeedbackRepository.findByIdAndProductionLot_Organization_OrganizationId(
                feedbackId, organizationId)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserDetails).thenReturn(currentUser);

            assertThatThrownBy(() -> productFeedbackService.getFeedbackById(feedbackId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Không tìm thấy phản ánh");

            verify(productFeedbackRepository, never()).findById(feedbackId);
        }
    }
}
