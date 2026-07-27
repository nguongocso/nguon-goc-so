//package vn.nguongocso.productionLotService;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Optional;
//import java.util.UUID;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.context.ApplicationEventPublisher;
//
//import vn.nguongocso.auth.service.CustomUserDetails;
//import vn.nguongocso.exception.BusinessException;
//import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
//import vn.nguongocso.farm.entity.FarmLog;
//import vn.nguongocso.farm.entity.ProductCategory;
//import vn.nguongocso.farm.entity.ProductionLot;
//import vn.nguongocso.farm.enums.FarmActivityType;
//import vn.nguongocso.farm.enums.ProductionLotStatus;
//import vn.nguongocso.farm.event.PackagingValidationFailedEvent;
//import vn.nguongocso.farm.repository.FarmLogRepository;
//import vn.nguongocso.farm.repository.ProductionLotRepository;
//import vn.nguongocso.farm.service.impl.ProductionLotServiceImpl;
//import vn.nguongocso.organization.entity.Organization;
//
//@ExtendWith(MockitoExtension.class)
//class ProductionLotServiceTest {
//
//    @Mock
//    private ProductionLotRepository productionLotRepository;
//
//    @Mock
//    private FarmLogRepository farmLogRepository;
//
//    @Mock
//    private ApplicationEventPublisher eventPublisher;
//
//    @InjectMocks
//    private ProductionLotServiceImpl productionLotService;
//
//    private UUID lotId;
//    private UUID orgId;
//    private CustomUserDetails userDetails;
//    private ProductionLot productionLot;
//
//    @BeforeEach
//    void setUp() {
//        lotId = UUID.randomUUID();
//        orgId = UUID.randomUUID();
//
//        Organization organization = new Organization();
//        organization.setOrganizationId(orgId);
//        organization.setName("HTX Nông Nghiệp Xanh");
//
//        ProductCategory productCategory = new ProductCategory();
//        productCategory.setName("Cải Ngọt");
//
//        productionLot = new ProductionLot();
//        productionLot.setId(lotId);
//        productionLot.setName("Lô Cải Ngọt hữu cơ");
//        productionLot.setOrganization(organization);
//        productionLot.setProductCategory(productCategory);
//        productionLot.setStatus(ProductionLotStatus.HARVESTED);
//
//        userDetails = mock(CustomUserDetails.class);
//        lenient().when(userDetails.getOrganizationId()).thenReturn(orgId);
//    }
//
//    // TC-04: Đóng gói không đạt -> Phát sự kiện gửi thông báo cảnh báo
//    @Test
//    void packageProductionLot_shouldPublishEvent_whenLogsAreMissing() {
//        // 1. Given: Lô sản xuất tồn tại trong DB
//        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
//
//        // 2. Mock trường hợp thiếu nhật ký (trả về danh sách rỗng)
//        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
//                .thenReturn(Collections.emptyList());
//
//        // 3. When & Then: Thực thi đóng gói và kỳ vọng sẽ ném ra BusinessException
//        assertThatThrownBy(() -> productionLotService.packageProductionLot(lotId, userDetails))
//                .isInstanceOf(BusinessException.class);
//
//        // 4. Xác minh sự kiện gửi thông báo (Event) đã được phát đi thành công
//        ArgumentCaptor<PackagingValidationFailedEvent> eventCaptor =
//                ArgumentCaptor.forClass(PackagingValidationFailedEvent.class);
//
//        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
//
//        // 5. Kiểm tra thông tin trong Event
//        PackagingValidationFailedEvent publishedEvent = eventCaptor.getValue();
//        assertThat(publishedEvent.getProductionLotId()).isEqualTo(lotId);
//        assertThat(publishedEvent.getOrganizationId()).isEqualTo(orgId);
//        assertThat(publishedEvent.getLotName()).isEqualTo("Lô Cải Ngọt hữu cơ");
//    }
//
//    // TC-01: Đóng gói thành công khi có đầy đủ nhật ký bắt buộc
//    @Test
//    void packageProductionLot_shouldSuccess_whenAllMandatoryLogsPresent() {
//        // Given
//        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
//
//        FarmLog plantingLog = FarmLog.builder().activityType(FarmActivityType.PLANTING).build();
//        FarmLog fertilizingLog = FarmLog.builder().activityType(FarmActivityType.FERTILIZING).build();
//        FarmLog pesticideLog = FarmLog.builder().activityType(FarmActivityType.PESTICIDE).build();
//        FarmLog harvestingLog = FarmLog.builder().activityType(FarmActivityType.HARVESTING).build();
//
//        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
//                .thenReturn(Arrays.asList(plantingLog, fertilizingLog, pesticideLog, harvestingLog));
//
//        when(productionLotRepository.save(any(ProductionLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        CreateProductionLotResponse response = productionLotService.packageProductionLot(lotId, userDetails);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getStatus()).isEqualTo(ProductionLotStatus.PACKAGED.name());
//        verify(productionLotRepository, times(1)).save(productionLot);
//    }
//
//    // TC-02: Đóng gói thất bại khi thiếu nhật ký bắt buộc (liệt kê nhật ký thiếu)
//    @Test
//    void packageProductionLot_shouldThrow_whenMandatoryLogsAreMissing() {
//        // Given
//        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
//
//        // Chỉ có PLANTING và HARVESTING, thiếu FERTILIZING và PESTICIDE
//        FarmLog plantingLog = FarmLog.builder().activityType(FarmActivityType.PLANTING).build();
//        FarmLog harvestingLog = FarmLog.builder().activityType(FarmActivityType.HARVESTING).build();
//
//        when(farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId))
//                .thenReturn(Arrays.asList(plantingLog, harvestingLog));
//
//        // When & Then
//        assertThatThrownBy(() -> productionLotService.packageProductionLot(lotId, userDetails))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("Không thể đóng gói. Lô thiếu các nhật ký bắt buộc:")
//                .hasMessageContaining("FERTILIZING")
//                .hasMessageContaining("PESTICIDE");
//
//        verify(productionLotRepository, never()).save(any());
//    }
//
//    // TC-03: Đóng gói thất bại do lô sản xuất sai trạng thái (chưa thu hoạch)
//    @Test
//    void packageProductionLot_shouldThrow_whenLotStatusIsNotHarvested() {
//        // Given
//        productionLot.setStatus(ProductionLotStatus.APPROVED); // Trạng thái APPROVED thay vì HARVESTED
//        when(productionLotRepository.findById(lotId)).thenReturn(Optional.of(productionLot));
//
//        // When & Then
//        assertThatThrownBy(() -> productionLotService.packageProductionLot(lotId, userDetails))
//                .isInstanceOf(BusinessException.class)
//                .hasMessageContaining("Lô sản xuất phải ở trạng thái đã thu hoạch (HARVESTED) mới có thể đóng gói");
//
//        verify(farmLogRepository, never()).findByProductionLotId_IdOrderByExecutedDateAsc(any());
//        verify(productionLotRepository, never()).save(any());
//    }
//}
