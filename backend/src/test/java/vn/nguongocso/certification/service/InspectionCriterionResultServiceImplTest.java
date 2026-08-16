package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import vn.nguongocso.farm.repository.ProductionLotRepository;

/**
 * Unit tests cho InspectionCriterionResultService.
 *
 * Kiểm tra:
 * - Ghi nhận kết quả kiểm nghiệm.
 * - Cập nhật kết quả kiểm nghiệm.
 * - Validate ngày cấp / ngày hết hiệu lực.
 * - Validate trạng thái yêu cầu kiểm nghiệm.
 * - Lấy danh sách kết quả theo yêu cầu.
 * - Kiểm tra điều kiện kích hoạt tem.
 * - Xóa kết quả kiểm nghiệm.
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

        // =========================
        // Mock user
        // =========================
        user = User.builder()
                .userId(userId)
                .fullName("Test User")
                .build();

        currentUser = org.mockito.Mockito.mock(CustomUserDetails.class);

        lenient()
                .when(currentUser.getUser())
                .thenReturn(user);

        // =========================
        // Mock inspection request
        // =========================
        inspectionRequest = InspectionRequest.builder()
                .id(inspectionRequestId)
                .status(InspectionRequestStatus.PENDING_RESULT)
                .build();

        // =========================
        // Mock criterion
        // =========================
        criterion = InspectionCriterion.builder()
                .id(criterionId)
                .criterionCode("TEST_CODE")
                .criterionName("Test Criterion")
                .inspectionRequest(inspectionRequest)
                .build();

        inspectionRequest.setCriteria(List.of(criterion));

        // =========================
        // Mock result
        // =========================
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

    // ============================================================
    // TC-01
    // ============================================================

    @Test
    @DisplayName("TC-01: Nhập chỉ tiêu đạt + ngày hết hiệu lực → Chốt kết luận đạt")
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

        /*
         * FIX:
         *
         * Entity InspectionCriterionResult có field:
         *
         * private InspectionCriterion inspectionCriterion;
         *
         * nên Spring Data Repository phải dùng:
         *
         * findByInspectionCriterion_Id(...)
         */
        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.empty());

        when(resultRepository.save(any(InspectionCriterionResult.class)))
                .thenReturn(result);

        when(resultRepository.areAllCriteriaPassedAndValid(
                inspectionRequestId,
                LocalDate.now()))
                .thenReturn(true);

        // Act
        InspectionCriterionResultResponse response =
                service.recordOrUpdateResult(
                        criterionId.toString(),
                        request,
                        currentUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResultId()).isNotNull();
        assertThat(response.getPassed()).isTrue();
        assertThat(response.getExpiryDate()).isEqualTo(expiryDate);

        verify(resultRepository)
                .save(any(InspectionCriterionResult.class));

        verify(requestRepository)
                .save(inspectionRequest);
    }

    // ============================================================
    // TC-02
    // ============================================================

    @Test
    @DisplayName("TC-02: Lô chưa có kết quả đạt → Từ chối kích hoạt tem")
    void testCanActivateSealWithoutPassedResult() {

        // Arrange
        UUID lotId = productionLotId;

        when(lotRepository.existsById(lotId))
                .thenReturn(true);

        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(inspectionRequest));

        when(resultRepository.countTotalCriteria(inspectionRequestId))
                .thenReturn(3);

        when(resultRepository.countPassedAndValidCriteria(
                inspectionRequestId,
                LocalDate.now()))
                .thenReturn(0);

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(
                        lotId,
                        currentUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCanActivate()).isFalse();

        assertThat(response.getReason())
                .contains("chưa có kết quả kiểm nghiệm đạt");

        assertThat(response.getTotalCriteria())
                .isEqualTo(3);

        assertThat(response.getPassedCriteria())
                .isZero();
    }

    // ============================================================
    // TC-03
    // ============================================================

    @Test
    @DisplayName("TC-03: Kết quả quá hạn → Từ chối kích hoạt tem")
    void testCanActivateSealWithExpiredResult() {

        // Arrange
        UUID lotId = productionLotId;

        LocalDate expiredDate =
                LocalDate.now().minusDays(1);

        when(lotRepository.existsById(lotId))
                .thenReturn(true);

        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lotId))
                .thenReturn(List.of(inspectionRequest));

        when(resultRepository.countTotalCriteria(inspectionRequestId))
                .thenReturn(3);

        when(resultRepository.countPassedAndValidCriteria(
                inspectionRequestId,
                LocalDate.now()))
                .thenReturn(0);

        when(resultRepository.findEarliestExpiryDateByInspectionRequest(
                inspectionRequestId))
                .thenReturn(Optional.of(expiredDate));

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(
                        lotId,
                        currentUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCanActivate()).isFalse();

        assertThat(response.getReason())
                .contains("quá hạn");
    }

    // ============================================================
    // TC-04
    // ============================================================

    @Test
    @DisplayName("TC-04: Ngày hết hiệu lực sớm hơn ngày cấp → Lỗi validation")
    void testValidateExpiryDateBeforeResultDate() {

        // Arrange
        LocalDate resultDate =
                LocalDate.of(2024, 9, 15);

        LocalDate expiryDate =
                LocalDate.of(2024, 9, 1);

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
                service.recordOrUpdateResult(
                        criterionId.toString(),
                        request,
                        currentUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Ngày hết hiệu lực phải sau ngày cấp");
    }

    // ============================================================
    // TC-05
    // ============================================================

    @Test
    @DisplayName("TC-05: Yêu cầu kiểm nghiệm phải ở trạng thái PENDING_RESULT")
    void testValidateInspectionRequestStatus() {

        // Arrange
        inspectionRequest.setStatus(
                InspectionRequestStatus.PASSED);

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
                service.recordOrUpdateResult(
                        criterionId.toString(),
                        request,
                        currentUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "trạng thái chờ kết quả");
    }

    // ============================================================
    // TC-06
    // ============================================================

    @Test
    @DisplayName("TC-06: Ngày hết hiệu lực phải >= ngày hiện tại")
    void testValidateExpiryDateInFuture() {

        // Arrange
        LocalDate resultDate =
                LocalDate.now().minusDays(1);

        LocalDate expiredDate =
                LocalDate.now().minusDays(1);

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
                service.recordOrUpdateResult(
                        criterionId.toString(),
                        request,
                        currentUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Ngày hết hiệu lực phải >= ngày hiện tại");
    }

    // ============================================================
    // TC-07
    // ============================================================

    @Test
    @DisplayName("TC-07: Lấy danh sách kết quả kiểm nghiệm theo yêu cầu")
    void testGetResultsByRequest() {

        // Arrange
        /*
         * FIX:
         *
         * Entity field:
         * inspectionCriterion
         *
         * nên method đúng là:
         *
         * findByInspectionCriterion_InspectionRequest_Id(...)
         */
        when(resultRepository.findByInspectionCriterion_InspectionRequest_Id(
                inspectionRequestId))
                .thenReturn(List.of(result));

        // Act
        List<InspectionCriterionResultResponse> responses =
                service.getResultsByRequest(
                        inspectionRequestId);

        // Assert
        assertThat(responses)
                .hasSize(1);

        assertThat(responses.get(0).getCriterionCode())
                .isEqualTo("TEST_CODE");
    }

    // ============================================================
    // TC-08
    // ============================================================

    @Test
    @DisplayName("TC-08: Xóa kết quả kiểm nghiệm")
    void testDeleteResult() {

        // Arrange
        UUID resultId = result.getId();

        when(resultRepository.findById(resultId))
                .thenReturn(Optional.of(result));

        when(resultRepository.areAllCriteriaPassedAndValid(
                inspectionRequestId,
                LocalDate.now()))
                .thenReturn(false);

        // Act
        service.deleteResult(resultId.toString());

        // Assert
        verify(resultRepository)
                .delete(result);
    }
}