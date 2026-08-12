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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CreateProductFeedbackRequest;
import vn.nguongocso.farm.dto.response.ProductFeedbackResponse;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.event.ProductFeedbackSubmittedEvent;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.ProductFeedbackServiceImpl;
import vn.nguongocso.organization.entity.Organization;

@ExtendWith(MockitoExtension.class)
class ProductFeedbackServiceTest {

    @Mock
    private ProductFeedbackRepository productFeedbackRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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

        ProductFeedback mockSaved = ProductFeedback.builder()
                .id(UUID.randomUUID())
                .productionLot(productionLot)
                .content(request.getContent())
                .build();
        when(productFeedbackRepository.save(any(ProductFeedback.class))).thenReturn(mockSaved);

        // When
        ProductFeedbackResponse response = productFeedbackService.createFeedback(lotId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(request.getContent());
        assertThat(response.getProductionLotName()).isEqualTo(productionLot.getName());

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
}
