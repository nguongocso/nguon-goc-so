package vn.nguongocso.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;

/**
 * Unit tests cho InspectionCriterionResultService.
 *
 * Kiểm tra:
 * - Ghi nhận kết quả kiểm nghiệm.
 * - Cập nhật kết quả kiểm nghiệm.
 * - Ghi nhận batch toàn bộ kết quả của yêu cầu (all-or-nothing).
 * - Organization authorization (org boundary).
 * - Validate ngày cấp / ngày hết hiệu lực.
 * - Validate trạng thái yêu cầu kiểm nghiệm.
 * - Lấy danh sách kết quả theo yêu cầu.
 * - Kiểm tra điều kiện kích hoạt tem (QTN-21).
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

    @Mock
    private Clock clock;

    @InjectMocks
    private InspectionCriterionResultServiceImpl service;

    private UUID criterionId;
    private UUID inspectionRequestId;
    private UUID productionLotId;
    private UUID userId;
    private UUID orgId;

    private User user;
    private CustomUserDetails currentUser;
    private ProductionLot lot;
    private InspectionRequest inspectionRequest;
    private InspectionCriterion criterion;
    private InspectionCriterionResult result;

    @BeforeEach
    void setUp() {
        criterionId = UUID.randomUUID();
        inspectionRequestId = UUID.randomUUID();
        productionLotId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();

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

        lenient()
                .when(currentUser.getOrganizationId())
                .thenReturn(orgId);

        // =========================
        // Business clock: zone hệ thống để khớp LocalDate.now() trong fixture
        // =========================
        lenient().when(clock.instant())
                .thenReturn(Clock.systemDefaultZone().instant());
        lenient().when(clock.getZone())
                .thenReturn(ZoneId.systemDefault());

        // =========================
        // Mock lot thuộc orgId
        // =========================
        lot = new ProductionLot();
        lot.setId(productionLotId);
        lot.setName("Lô 01");
        Organization organization = new Organization();
        organization.setOrganizationId(orgId);
        lot.setOrganization(organization);

        // =========================
        // Mock inspection request
        // =========================
        inspectionRequest = InspectionRequest.builder()
                .id(inspectionRequestId)
                .status(InspectionRequestStatus.PENDING_RESULT)
                .productionLot(lot)
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

        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.empty());

        when(resultRepository.save(any(InspectionCriterionResult.class)))
                .thenReturn(result);

        when(resultRepository.countTotalCriteria(
                inspectionRequestId))
                .thenReturn(1);

        when(resultRepository
                .findByInspectionCriterion_InspectionRequest_Id(
                        inspectionRequestId))
                .thenReturn(List.of(result));

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

        when(lotRepository.findByIdAndOrganization_OrganizationId(
                lotId, orgId))
                .thenReturn(Optional.of(lot));

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

        when(lotRepository.findByIdAndOrganization_OrganizationId(
                lotId, orgId))
                .thenReturn(Optional.of(lot));

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
        when(requestRepository.findById(inspectionRequestId))
                .thenReturn(Optional.of(inspectionRequest));

        when(resultRepository.findByInspectionCriterion_InspectionRequest_Id(
                inspectionRequestId))
                .thenReturn(List.of(result));

        // Act
        List<InspectionCriterionResultResponse> responses =
                service.getResultsByRequest(
                        inspectionRequestId,
                        currentUser);

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

        // Sau khi xóa: vẫn còn 1 chỉ tiêu nhưng không còn kết quả nào
        when(resultRepository.countTotalCriteria(
                inspectionRequestId))
                .thenReturn(1);

        when(resultRepository
                .findByInspectionCriterion_InspectionRequest_Id(
                        inspectionRequestId))
                .thenReturn(List.of());

        // Act
        service.deleteResult(resultId.toString(), currentUser);

        // Assert
        verify(resultRepository)
                .delete(result);

        // Chưa đủ kết quả cho tất cả chỉ tiêu → trạng thái vẫn PENDING_RESULT
        assertThat(inspectionRequest.getStatus())
                .isEqualTo(InspectionRequestStatus.PENDING_RESULT);

        verify(requestRepository, org.mockito.Mockito.never())
                .save(inspectionRequest);
    }

    // ============================================================
    // TC-09
    // ============================================================

    @Test
    @DisplayName("TC-09: Chỉ tiêu không đạt → Yêu cầu chuyển FAILED")
    void testRecordFailedCriteriaSetsRequestFailed() {

        // Arrange
        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(false)
                        .build();

        result.setPassed(false);
        result.setExpiryDate(expiryDate);

        when(criterionRepository.findById(criterionId))
                .thenReturn(Optional.of(criterion));

        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.empty());

        when(resultRepository.save(any(InspectionCriterionResult.class)))
                .thenReturn(result);

        when(resultRepository.countTotalCriteria(
                inspectionRequestId))
                .thenReturn(1);

        when(resultRepository
                .findByInspectionCriterion_InspectionRequest_Id(
                        inspectionRequestId))
                .thenReturn(List.of(result));

        // Act
        service.recordOrUpdateResult(
                criterionId.toString(),
                request,
                currentUser);

        // Assert
        assertThat(inspectionRequest.getStatus())
                .isEqualTo(InspectionRequestStatus.FAILED);

        verify(requestRepository)
                .save(inspectionRequest);
    }

    // ============================================================
    // TC-10
    // ============================================================

    @Test
    @DisplayName("TC-10: Ghi lại kết quả khi yêu cầu FAILED được phép và chuyển PASSED nếu đạt hết")
    void testRecordResultAllowedWhenRequestFailed() {

        // Arrange
        inspectionRequest.setStatus(
                InspectionRequestStatus.FAILED);

        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build();

        result.setPassed(true);
        result.setExpiryDate(expiryDate);

        when(criterionRepository.findById(criterionId))
                .thenReturn(Optional.of(criterion));

        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.empty());

        when(resultRepository.save(any(InspectionCriterionResult.class)))
                .thenReturn(result);

        when(resultRepository.countTotalCriteria(
                inspectionRequestId))
                .thenReturn(1);

        when(resultRepository
                .findByInspectionCriterion_InspectionRequest_Id(
                        inspectionRequestId))
                .thenReturn(List.of(result));

        // Act
        service.recordOrUpdateResult(
                criterionId.toString(),
                request,
                currentUser);

        // Assert
        assertThat(inspectionRequest.getStatus())
                .isEqualTo(InspectionRequestStatus.PASSED);

        verify(requestRepository)
                .save(inspectionRequest);
    }

    // ============================================================
    // TC-11: Organization authorization
    // ============================================================

    @Test
    @DisplayName("TC-11: VT-02 tổ chức khác ghi kết quả chỉ tiêu của tổ chức khác → Bị từ chối, DB không đổi")
    void testRecordResultCrossOrganizationDenied() {

        // Arrange
        UUID otherOrgId = UUID.randomUUID();
        when(currentUser.getOrganizationId())
                .thenReturn(otherOrgId);

        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        InspectionCriterionResultRequest request =
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
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
                .isInstanceOf(BusinessException.class);

        // Không có thay đổi nào được ghi
        verify(resultRepository, never())
                .save(any(InspectionCriterionResult.class));
        verify(resultRepository, never())
                .saveAll(anyList());
        verify(requestRepository, never())
                .save(any(InspectionRequest.class));
    }

    // ============================================================
    // TC-12: Organization authorization
    // ============================================================

    @Test
    @DisplayName("TC-12: GET result của chỉ tiêu thuộc tổ chức khác → Bị từ chối")
    void testGetResultByCriterionCrossOrganizationDenied() {

        // Arrange
        UUID otherOrgId = UUID.randomUUID();
        when(currentUser.getOrganizationId())
                .thenReturn(otherOrgId);

        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.of(result));

        // Act & Assert
        assertThatThrownBy(() ->
                service.getResultByCriterion(
                        criterionId.toString(),
                        currentUser))
                .isInstanceOf(BusinessException.class);
    }

    // ============================================================
    // TC-13: Organization authorization
    // ============================================================

    @Test
    @DisplayName("TC-13: GET results của request thuộc tổ chức khác → Bị từ chối")
    void testGetResultsByRequestCrossOrganizationDenied() {

        // Arrange
        UUID otherOrgId = UUID.randomUUID();
        when(currentUser.getOrganizationId())
                .thenReturn(otherOrgId);

        when(requestRepository.findById(inspectionRequestId))
                .thenReturn(Optional.of(inspectionRequest));

        // Act & Assert
        assertThatThrownBy(() ->
                service.getResultsByRequest(
                        inspectionRequestId,
                        currentUser))
                .isInstanceOf(BusinessException.class);
    }

    // ============================================================
    // TC-14: Organization authorization
    // ============================================================

    @Test
    @DisplayName("TC-14: DELETE result của tổ chức khác → Bị từ chối, không xóa")
    void testDeleteResultCrossOrganizationDenied() {

        // Arrange
        UUID otherOrgId = UUID.randomUUID();
        when(currentUser.getOrganizationId())
                .thenReturn(otherOrgId);

        UUID resultId = result.getId();
        when(resultRepository.findById(resultId))
                .thenReturn(Optional.of(result));

        // Act & Assert
        assertThatThrownBy(() ->
                service.deleteResult(resultId.toString(), currentUser))
                .isInstanceOf(BusinessException.class);

        verify(resultRepository, never())
                .delete(any(InspectionCriterionResult.class));
    }

    // ============================================================
    // TC-15: Organization authorization
    // ============================================================

    @Test
    @DisplayName("TC-15: canActivateSeal của lô thuộc tổ chức khác → Bị từ chối")
    void testCanActivateSealCrossOrganizationDenied() {

        // Arrange
        UUID otherOrgId = UUID.randomUUID();
        when(currentUser.getOrganizationId())
                .thenReturn(otherOrgId);

        when(lotRepository.findByIdAndOrganization_OrganizationId(
                productionLotId, otherOrgId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                service.checkCanActivateSeal(
                        productionLotId,
                        currentUser))
                .isInstanceOf(BusinessException.class);
    }

    // ============================================================
    // TC-16: Batch save
    // ============================================================

    @Test
    @DisplayName("TC-16: Batch ghi 3 chỉ tiêu hợp lệ → Thành công, trạng thái PASSED tính sau toàn bộ batch")
    void testRecordResultsBatchSuccess() {

        // Arrange
        UUID c2 = UUID.randomUUID();
        UUID c3 = UUID.randomUUID();
        InspectionCriterion criterion2 = InspectionCriterion.builder()
                .id(c2)
                .criterionCode("CODE_2")
                .criterionName("Criterion 2")
                .inspectionRequest(inspectionRequest)
                .build();
        InspectionCriterion criterion3 = InspectionCriterion.builder()
                .id(c3)
                .criterionCode("CODE_3")
                .criterionName("Criterion 3")
                .inspectionRequest(inspectionRequest)
                .build();
        inspectionRequest.setCriteria(List.of(criterion, criterion2, criterion3));

        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        InspectionCriterionResult r2 = InspectionCriterionResult.builder()
                .id(UUID.randomUUID())
                .inspectionCriterion(criterion2)
                .resultDate(resultDate)
                .expiryDate(expiryDate)
                .passed(true)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        InspectionCriterionResult r3 = InspectionCriterionResult.builder()
                .id(UUID.randomUUID())
                .inspectionCriterion(criterion3)
                .resultDate(resultDate)
                .expiryDate(expiryDate)
                .passed(true)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<InspectionCriterionResultRequest> items = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(c2.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(c3.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build());

        when(requestRepository.findById(inspectionRequestId))
                .thenReturn(Optional.of(inspectionRequest));

        // Chưa có kết quả nào → tạo mới
        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.empty());
        when(resultRepository.findByInspectionCriterion_Id(c2))
                .thenReturn(Optional.empty());
        when(resultRepository.findByInspectionCriterion_Id(c3))
                .thenReturn(Optional.empty());

        when(resultRepository.saveAll(anyList()))
                .thenReturn(List.of(result, r2, r3));

        // Trạng thái cuối tính sau khi toàn bộ kết quả đã lưu
        when(resultRepository.countTotalCriteria(inspectionRequestId))
                .thenReturn(3);
        when(resultRepository.findByInspectionCriterion_InspectionRequest_Id(
                inspectionRequestId))
                .thenReturn(List.of(result, r2, r3));

        // Act
        List<InspectionCriterionResultResponse> responses =
                service.recordResults(
                        inspectionRequestId,
                        items,
                        currentUser);

        // Assert
        assertThat(responses).hasSize(3);

        // Trạng thái PASSED chỉ được chốt sau toàn bộ batch
        assertThat(inspectionRequest.getStatus())
                .isEqualTo(InspectionRequestStatus.PASSED);

        verify(requestRepository).save(inspectionRequest);
    }

    // ============================================================
    // TC-17: Batch atomicity
    // ============================================================

    @Test
    @DisplayName("TC-17: Batch có chỉ tiêu không hợp lệ → Rollback toàn bộ, không chỉ tiêu nào được lưu")
    void testRecordResultsBatchInvalidCriteriaRollbackAll() {

        // Arrange
        UUID c2 = UUID.randomUUID();
        UUID c3 = UUID.randomUUID();
        InspectionCriterion criterion2 = InspectionCriterion.builder()
                .id(c2)
                .criterionCode("CODE_2")
                .criterionName("Criterion 2")
                .inspectionRequest(inspectionRequest)
                .build();
        InspectionCriterion criterion3 = InspectionCriterion.builder()
                .id(c3)
                .criterionCode("CODE_3")
                .criterionName("Criterion 3")
                .inspectionRequest(inspectionRequest)
                .build();
        inspectionRequest.setCriteria(List.of(criterion, criterion2, criterion3));

        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        // A hợp lệ, B KHÔNG hợp lệ (expiry < resultDate), C hợp lệ
        List<InspectionCriterionResultRequest> items = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(c2.toString())
                        .resultDate(expiryDate)
                        .expiryDate(resultDate)
                        .passed(true)
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(c3.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build());

        when(requestRepository.findById(inspectionRequestId))
                .thenReturn(Optional.of(inspectionRequest));

        // Act & Assert
        assertThatThrownBy(() ->
                service.recordResults(
                        inspectionRequestId,
                        items,
                        currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ngày hết hiệu lực phải sau ngày cấp");

        // KHÔNG có partial save: A, B, C đều không được lưu
        verify(resultRepository, never())
                .save(any(InspectionCriterionResult.class));
        verify(resultRepository, never())
                .saveAll(anyList());

        // Trạng thái không bị thay đổi
        assertThat(inspectionRequest.getStatus())
                .isEqualTo(InspectionRequestStatus.PENDING_RESULT);
    }

    // ============================================================
    // TC-18: Batch validation
    // ============================================================

    @Test
    @DisplayName("TC-18: Batch chứa chỉ tiêu không thuộc yêu cầu → Bị từ chối")
    void testRecordResultsBatchCriterionNotInRequest() {

        // Arrange
        UUID foreignCriterionId = UUID.randomUUID();

        List<InspectionCriterionResultRequest> items = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(LocalDate.now().minusDays(1))
                        .expiryDate(LocalDate.now().plusMonths(6))
                        .passed(true)
                        .build(),
                InspectionCriterionResultRequest.builder()
                        .criterionId(foreignCriterionId.toString())
                        .resultDate(LocalDate.now().minusDays(1))
                        .expiryDate(LocalDate.now().plusMonths(6))
                        .passed(true)
                        .build());

        when(requestRepository.findById(inspectionRequestId))
                .thenReturn(Optional.of(inspectionRequest));

        // Act & Assert
        assertThatThrownBy(() ->
                service.recordResults(
                        inspectionRequestId,
                        items,
                        currentUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(
                        "Chỉ tiêu không thuộc yêu cầu kiểm nghiệm");

        verify(resultRepository, never())
                .saveAll(anyList());
    }

    // ============================================================
    // TC-19: Batch on FAILED request
    // ============================================================

    @Test
    @DisplayName("TC-19: Batch sửa lại kết quả yêu cầu FAILED → Chốt PASSED sau toàn bộ batch")
    void testRecordResultsBatchOnFailedRequest() {

        // Arrange
        inspectionRequest.setStatus(
                InspectionRequestStatus.FAILED);

        LocalDate resultDate = LocalDate.now().minusDays(1);
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        result.setPassed(true);
        result.setExpiryDate(expiryDate);

        List<InspectionCriterionResultRequest> items = List.of(
                InspectionCriterionResultRequest.builder()
                        .criterionId(criterionId.toString())
                        .resultDate(resultDate)
                        .expiryDate(expiryDate)
                        .passed(true)
                        .build());

        when(requestRepository.findById(inspectionRequestId))
                .thenReturn(Optional.of(inspectionRequest));

        when(resultRepository.findByInspectionCriterion_Id(criterionId))
                .thenReturn(Optional.empty());

        when(resultRepository.saveAll(anyList()))
                .thenReturn(List.of(result));

        when(resultRepository.countTotalCriteria(inspectionRequestId))
                .thenReturn(1);
        when(resultRepository.findByInspectionCriterion_InspectionRequest_Id(
                inspectionRequestId))
                .thenReturn(List.of(result));

        // Act
        service.recordResults(
                inspectionRequestId,
                items,
                currentUser);

        // Assert
        assertThat(inspectionRequest.getStatus())
                .isEqualTo(InspectionRequestStatus.PASSED);

        verify(requestRepository).save(inspectionRequest);
    }

    // ============================================================
    // TC-20: QTN-21
    // ============================================================

    @Test
    @DisplayName("TC-20: Request FAILED lịch sử không chặn tem nếu đã có bộ kết quả đạt còn hiệu lực (QTN-21)")
    void testCanActivateSealWithHistoricalFailedRequestAndValidPassedRequest() {

        // Arrange
        UUID failedRequestId = UUID.randomUUID();
        UUID passedRequestId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        LocalDate validExpiry = today.plusMonths(6);

        InspectionRequest failedRequest = InspectionRequest.builder()
                .id(failedRequestId)
                .status(InspectionRequestStatus.FAILED)
                .productionLot(lot)
                .build();

        InspectionRequest passedRequest = InspectionRequest.builder()
                .id(passedRequestId)
                .status(InspectionRequestStatus.PASSED)
                .productionLot(lot)
                .build();

        when(lotRepository.findByIdAndOrganization_OrganizationId(
                productionLotId, orgId))
                .thenReturn(Optional.of(lot));

        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(
                productionLotId))
                .thenReturn(List.of(passedRequest, failedRequest));

        // Request FAILED lịch sử: 3 chỉ tiêu, không chỉ tiêu nào còn hiệu lực
        when(resultRepository.countTotalCriteria(failedRequestId))
                .thenReturn(3);
        when(resultRepository.countPassedAndValidCriteria(
                failedRequestId, today))
                .thenReturn(0);

        // Request PASSED sau đó: 1 chỉ tiêu, đạt và còn hiệu lực
        when(resultRepository.countTotalCriteria(passedRequestId))
                .thenReturn(1);
        when(resultRepository.countPassedAndValidCriteria(
                passedRequestId, today))
                .thenReturn(1);
        when(resultRepository.findEarliestExpiryDateByInspectionRequest(
                passedRequestId))
                .thenReturn(Optional.of(validExpiry));

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(
                        productionLotId,
                        currentUser);

        // Assert
        assertThat(response.getCanActivate()).isTrue();
        assertThat(response.getEarliestExpiryDate())
                .isEqualTo(validExpiry);
    }

    // ============================================================
    // TC-21: QTN-21
    // ============================================================

    @Test
    @DisplayName("TC-21: Kết quả PASSED nhưng hết hạn → Không kích hoạt tem")
    void testCanActivateSealPassedButExpired() {

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiredExpiry = today.minusDays(1);

        when(lotRepository.findByIdAndOrganization_OrganizationId(
                productionLotId, orgId))
                .thenReturn(Optional.of(lot));

        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(
                productionLotId))
                .thenReturn(List.of(inspectionRequest));

        when(resultRepository.countTotalCriteria(inspectionRequestId))
                .thenReturn(1);
        when(resultRepository.countPassedAndValidCriteria(
                inspectionRequestId, today))
                .thenReturn(0);
        when(resultRepository.findEarliestExpiryDateByInspectionRequest(
                inspectionRequestId))
                .thenReturn(Optional.of(expiredExpiry));

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(
                        productionLotId,
                        currentUser);

        // Assert
        assertThat(response.getCanActivate()).isFalse();
        assertThat(response.getReason())
                .contains("quá hạn");
    }

    // ============================================================
    // TC-22: QTN-21
    // ============================================================

    @Test
    @DisplayName("TC-22: Kết quả PASSED còn hiệu lực → Đủ điều kiện kích hoạt tem")
    void testCanActivateSealPassedAndValid() {

        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate validExpiry = today.plusMonths(6);

        when(lotRepository.findByIdAndOrganization_OrganizationId(
                productionLotId, orgId))
                .thenReturn(Optional.of(lot));

        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(
                productionLotId))
                .thenReturn(List.of(inspectionRequest));

        when(resultRepository.countTotalCriteria(inspectionRequestId))
                .thenReturn(1);
        when(resultRepository.countPassedAndValidCriteria(
                inspectionRequestId, today))
                .thenReturn(1);
        when(resultRepository.findEarliestExpiryDateByInspectionRequest(
                inspectionRequestId))
                .thenReturn(Optional.of(validExpiry));

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(
                        productionLotId,
                        currentUser);

        // Assert
        assertThat(response.getCanActivate()).isTrue();
        assertThat(response.getReason())
                .isNull();
        assertThat(response.getEarliestExpiryDate())
                .isEqualTo(validExpiry);
    }

    // ============================================================
    // TC-23: QTN-21
    // ============================================================

    @Test
    @DisplayName("TC-23: Lô không có yêu cầu kiểm nghiệm nào → Không kích hoạt tem")
    void testCanActivateSealWithoutAnyInspectionRequest() {

        // Arrange
        when(lotRepository.findByIdAndOrganization_OrganizationId(
                productionLotId, orgId))
                .thenReturn(Optional.of(lot));

        when(requestRepository.findByProductionLot_IdOrderByCreatedAtDesc(
                productionLotId))
                .thenReturn(List.of());

        // Act
        CanActivateSealCheckResponse response =
                service.checkCanActivateSeal(
                        productionLotId,
                        currentUser);

        // Assert
        assertThat(response.getCanActivate()).isFalse();
        assertThat(response.getReason())
                .contains("chưa có kết quả kiểm nghiệm");
        assertThat(response.getEarliestExpiryDate())
                .isNull();
        assertThat(response.getTotalCriteria())
                .isZero();
        assertThat(response.getPassedCriteria())
                .isZero();
        assertThat(response.getFailedOrExpiredCriteria())
                .isZero();
    }
}