package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.InspectionCriterionResultRequest;
import vn.nguongocso.certification.dto.response.CanActivateSealCheckResponse;
import vn.nguongocso.certification.dto.response.InspectionCriterionResultResponse;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionCriterionRepository;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.service.impl.InspectionCriterionResultServiceImpl;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;

/**
 * Unit tests cho InspectionCriterionResultService.
 * Kiểm tra logic ghi nhận kết quả kiểm nghiệm và kiểm tra điều kiện kích hoạt tem.
 */
@ExtendWith(MockitoExtension.class)
class InspectionCriterionResultServiceImplTest {

    @Mock
    private InspectionCriterionResultRepository resultRepository;

    @Mock
    private InspectionCriterionRepository criterionRepository;

    @Mock
    private InspectionRequestRepository requestRepository;

    @Mock
    private ProductionLotRepository lotRepository;

    @InjectMocks
    private InspectionCriterionResultServiceImpl service;

    private UUID criterionId;
    private UUID inspectionRequestId;
    private UUID productionLotId;
    private UUID userId;

    private User user;
    private CustomUserDetails currentUser;
    private InspectionRequest inspectionRequest;
    private InspectionCriterion criterion;
    private InspectionCriterionResult result;

    @BeforeEach
    void setUp() {
        criterionId = UUID.randomUUID();
        inspectionRequestId = UUID.randomUUID();
        productionLotId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Mock user
        user = User.builder()
                .userId(userId)
                .fullName("Test User")
                .build();

        currentUser = org.mockito.Mockito.mock(CustomUserDetails.class);
        lenient().when(currentUser.getUser()).thenReturn(user);

        // Mock inspection request
        inspectionRequest = InspectionRequest.builder()
                .id(inspectionRequestId)
                .status(InspectionRequestStatus.PENDING_RESULT)
                .build();

        // Mock criterion
        criterion = InspectionCriterion.builder()
                .id(criterionId)
                .criterionCode("TEST_CODE")
                .criterionName("Test Criterion")
                .inspectionRequest(inspectionRequest)
                .build();

        inspectionRequest.setCriteria(List.of(criterion));

        // Mock result
        result = InspectionCriterionResult.builder()
                .id(UUID.randomUUID())
                .inspectionCriterion(criterion)
                .resultDate(LocalDate.now().minusDays(1))
                .expiryDate(LocalDate.now().plusMonths(6))
                .passed(true)
                .filePath("/path/to/file.pdf")
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("TC-01: Nhập ba chỉ tiêu đạt + ngày hết hiệu lực → Chốt kết luận đạt")
    void testRecordResultForPassedCriteria() {

        // Arrange
        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .filePath("/path/to/result.pdf")
                        .build();

        when(criterionRepository.findById(criterionId))
                .thenReturn(Optional.of(criterion));
        when(resultRepository.findByCriterion_Id(criterionId))
                .thenReturn(Optional.empty());
        when(resultRepository.save(any(InspectionCriterionResult.class)))
                .thenReturn(result);
        when(resultRepository
                .areAllCriteriaPassedAndValid(
                        inspectionRequestId,
                        LocalDate.now()))
                .thenReturn(true);

        // Act
        InspectionCriterionResultResponse response =
                service.recordOrUpdateResult(criterionId.toString(), request, currentUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResultId()).isNotNull();
        assertThat(response.getPassed()).isTrue();
        assertThat(response.getExpiryDate()).isEqualTo(expiryDate);
        verify(resultRepository).save(any(InspectionCriterionResult.class));
        verify(requestRepository).save(inspectionRequest);
    }

    @Test
    @DisplayName("TC-02: Lô chưa có kết quả đạt → Từ chối kích hoạt tem")
    void testCanActivateSealWithoutPassedResult() {

        // Arrange
        UUID lotId = productionLotId;
        when(lotRepository.existsById(lotId)).thenReturn(true);
        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(inspectionRequest));
        when(resultRepository.countTotalCriteria(inspectionRequestId)).thenReturn(3);
        when(resultRepository
                .countPassedAndValidCriteria(
                        inspectionRequestId,
                        LocalDate.now()))
                .thenReturn(0); // Chưa có chỉ tiêu nào đạt

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(lotId, currentUser);

        // Assert
        assertThat(response.getCanActivate()).isFalse();
        assertThat(response.getReason())
                .contains("chưa có kết quả kiểm nghiệm đạt");
        assertThat(response.getTotalCriteria()).isEqualTo(3);
        assertThat(response.getPassedCriteria()).isZero();
    }

    @Test
    @DisplayName("TC-03: Kết quả quá hạn → Từ chối kích hoạt tem")
    void testCanActivateSealWithExpiredResult() {

        // Arrange
        UUID lotId = productionLotId;
        LocalDate expiredDate = LocalDate.now().minusDays(1); // Quá hạn

        when(lotRepository.existsById(lotId)).thenReturn(true);
        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(inspectionRequest));
        when(resultRepository.countTotalCriteria(inspectionRequestId)).thenReturn(3);
        when(resultRepository
                .countPassedAndValidCriteria(
                        inspectionRequestId,
                        LocalDate.now()))
                .thenReturn(0); // Không có chỉ tiêu còn hiệu lực
        when(resultRepository
                .findEarliestExpiryDateByInspectionRequest(
                        inspectionRequestId))
                .thenReturn(Optional.of(expiredDate));

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(lotId, currentUser);

        // Assert
        assertThat(response.getCanActivate()).isFalse();
        assertThat(response.getReason()).contains("quá hạn");
    }

    @Test
    @DisplayName("TC-04: Ngày hết hiệu lực sớm hơn ngày cấp → Lỗi validation")
    void testValidateExpiryDateBeforeResultDate() {

        // Arrange
        LocalDate resultDate = LocalDate.of(2024, 9, 15);
        LocalDate expiryDate = LocalDate.of(2024, 9, 1); // Sớm hơn ngày cấp

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .filePath("/path/to/result.pdf")
                        .build();

        when(criterionRepository.findById(criterionId))
                .thenReturn(Optional.of(criterion));

        // Act & Assert
        assertThatThrownBy(() ->
                service.recordOrUpdateResult(criterionId.toString(), request, currentUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày hết hiệu lực phải sau ngày cấp");
    }

    @Test
    @DisplayName("Validate: Yêu cầu kiểm nghiệm phải ở trạng thái PENDING_RESULT")
    void testValidateInspectionRequestStatus() {

        // Arrange
        inspectionRequest.setStatus(InspectionRequestStatus.PASSED);

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(LocalDate.now())
                        .expiryDate(LocalDate.now().plusMonths(6))
                        .passed(true)
                        .build();

        when(criterionRepository.findById(criterionId))
                .thenReturn(Optional.of(criterion));

        // Act & Assert
        assertThatThrownBy(() ->
                service.recordOrUpdateResult(criterionId.toString(), request, currentUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trạng thái chờ kết quả");
    }

    @Test
    @DisplayName("Validate: Ngày hết hiệu lực >= ngày hiện tại")
    void testValidateExpiryDateInFuture() {

        // Arrange
        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiredDate = LocalDate.now().minusDays(1);

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiredDate)
                        .passed(true)
                        .build();

        when(criterionRepository.findById(criterionId))
                .thenReturn(Optional.of(criterion));

        // Act & Assert
        assertThatThrownBy(() ->
                service.recordOrUpdateResult(criterionId.toString(), request, currentUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ngày hết hiệu lực phải >= ngày hiện tại");
    }

    @Test
    @DisplayName("Lấy danh sách kết quả kiểm nghiệm theo yêu cầu")
    void testGetResultsByRequest() {

        // Arrange
        when(resultRepository.findByCriterion_InspectionRequest_Id(
                inspectionRequestId))
                .thenReturn(List.of(result));

        // Act
        List<InspectionCriterionResultResponse> responses =
                service.getResultsByRequest(inspectionRequestId);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCriterionCode()).isEqualTo("TEST_CODE");
    }

    @Test
    @DisplayName("Xóa kết quả kiểm nghiệm")
    void testDeleteResult() {

        // Arrange
        UUID resultId = result.getId();
        when(resultRepository.findById(resultId))
                .thenReturn(Optional.of(result));
        when(resultRepository
                .areAllCriteriaPassedAndValid(
                        inspectionRequestId,
                        LocalDate.now()))
                .thenReturn(false);

        // Act
        service.deleteResult(resultId.toString());

        // Assert
        verify(resultRepository).delete(result);
    }
}
