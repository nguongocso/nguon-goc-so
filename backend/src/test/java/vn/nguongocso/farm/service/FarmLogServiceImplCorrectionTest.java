package vn.nguongocso.farm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.dto.request.CorrectFarmLogRequest;
import vn.nguongocso.farm.dto.request.FarmLogCorrectionData;
import vn.nguongocso.farm.dto.response.FarmLogResponse;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.FarmLogServiceImpl;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.repository.TraceCodeRepository;

/**
 * NCL-03-CN-006: kiểm tra nghiệp vụ đính chính nhật ký canh tác.
 */
@ExtendWith(MockitoExtension.class)
class FarmLogServiceImplCorrectionTest {

    @Mock
    private FarmLogRepository farmLogRepository;

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private FarmLogAttachmentRepository attachmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    private FarmLogServiceImpl farmLogService;

    private UUID organizationId;
    private ProductionLot productionLot;
    private CustomUserDetails recorder;  // VT-03 - người ghi gốc
    private CustomUserDetails manager;   // VT-02 - quản lý hợp tác xã
    private User recorderUser;
    private User managerUser;
    private FarmLog originalLog;

    @BeforeEach
    void setUp() {
        farmLogService = new FarmLogServiceImpl(
                farmLogRepository,
                productionLotRepository,
                attachmentRepository,
                traceCodeRepository,
                eventPublisher,
                clock);

        organizationId = UUID.randomUUID();

        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);

        FarmArea farmArea = new FarmArea();
        farmArea.setOrganization(organization);

        productionLot = new ProductionLot();
        productionLot.setId(UUID.randomUUID());
        productionLot.setName("Lô chè 01");
        productionLot.setStatus(ProductionLotStatus.APPROVED);
        productionLot.setFarmArea(farmArea);

        recorderUser = new User();
        recorderUser.setUserId(UUID.randomUUID());
        recorderUser.setFullName("Người ghi nhật ký");
        recorderUser.setUserName("recorder");

        managerUser = new User();
        managerUser.setUserId(UUID.randomUUID());
        managerUser.setFullName("Quản lý HTX");
        managerUser.setUserName("manager");

        recorder = mockUser("VT-03", recorderUser);
        manager = mockUser("VT-02", managerUser);

        originalLog = buildFarmLog(FarmActivityType.FERTILIZING, "NPK 16-16-8",
                25.0, "kg", LocalDate.of(2026, 8, 20), "Bón phân lần 1", recorderUser);
        originalLog.setId(UUID.randomUUID());

        // Clock nghiệp vụ cố định: 2026-08-23 (Asia/Ho_Chi_Minh).
        lenient().when(clock.instant())
                .thenReturn(Instant.parse("2026-08-23T03:00:00Z"));
        lenient().when(clock.getZone())
                .thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));

        // Mô phỏng save: gán ID như @PrePersist của JPA.
        lenient().when(farmLogRepository.save(any(FarmLog.class)))
                .thenAnswer(invocation -> {
                    FarmLog log = invocation.getArgument(0);
                    if (log.getId() == null) {
                        log.setId(UUID.randomUUID());
                    }
                    return log;
                });

        // Mặc định: lô chưa có mã truy xuất nào được kích hoạt.
        lenient().when(traceCodeRepository.existsActivatedByProductionLotId(any(UUID.class)))
                .thenReturn(false);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CustomUserDetails mockUser(String roleCode, User user) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getRoleCode()).thenReturn(roleCode);
        lenient().when(userDetails.getOrganizationId()).thenReturn(organizationId);
        lenient().when(userDetails.getUser()).thenReturn(user);
        lenient().when(userDetails.getUserId()).thenReturn(user.getUserId());
        lenient().when(userDetails.getUsername()).thenReturn(user.getUserName());
        lenient().when(userDetails.getFullName()).thenReturn(user.getFullName());
        return userDetails;
    }

    private void loginAs(CustomUserDetails userDetails) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private FarmLog buildFarmLog(FarmActivityType activityType, String material,
            Double quantity, String unit, LocalDate executedDate, String notes,
            User createdBy) {

        return FarmLog.builder()
                .productionLotId(productionLot)
                .activityType(activityType)
                .material(material)
                .quantity(quantity)
                .unit(unit)
                .executedDate(executedDate)
                .notes(notes)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .build();
    }

    private CorrectFarmLogRequest buildRequest(String reason,
            FarmActivityType activityType, String material, Double quantity,
            String unit, LocalDate executedDate, String notes) {

        FarmLogCorrectionData data = new FarmLogCorrectionData();
        data.setActivityType(activityType);
        data.setMaterial(material);
        data.setQuantity(quantity);
        data.setUnit(unit);
        data.setExecutedDate(executedDate);
        data.setNotes(notes);

        CorrectFarmLogRequest request = new CorrectFarmLogRequest();
        request.setReason(reason);
        request.setCorrectionData(data);
        return request;
    }

    // ===== Happy path =====

    @Test
    @DisplayName("Người ghi gốc (VT-03) đính chính nhật ký của mình thành công")
    void correctFarmLog_happyPath_byOriginalRecorder() {
        loginAs(recorder);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of());

        CorrectFarmLogRequest request = buildRequest(
                "Ghi sai số lượng",
                null, null, 30.0, null, LocalDate.of(2026, 8, 22), null);

        FarmLogResponse response = farmLogService.correctFarmLog(originalLog.getId(), request);

        assertThat(response.getQuantity()).isEqualTo(30.0);
        assertThat(response.getExecutedDate()).isEqualTo(LocalDate.of(2026, 8, 22));
        assertThat(response.getMaterial()).isEqualTo("NPK 16-16-8");
        assertThat(response.getActivityType()).isEqualTo(FarmActivityType.FERTILIZING);
        assertThat(response.getIsCorrection()).isTrue();
        assertThat(response.getOriginalFarmLogId()).isEqualTo(originalLog.getId());
        assertThat(response.getCorrectionReason()).isEqualTo("Ghi sai số lượng");
        assertThat(response.getCorrectedByName()).isEqualTo("Người ghi nhật ký");
        assertThat(response.getIsCorrected()).isFalse();

        // Bản gốc được đánh dấu đã bị đính chính.
        assertThat(originalLog.isCorrected()).isTrue();
    }

    @Test
    @DisplayName("Quản lý HTX (VT-02) được đính chính nhật ký do người khác ghi")
    void correctFarmLog_byManager_differentCreator_allowed() {
        loginAs(manager);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of());

        CorrectFarmLogRequest request = buildRequest(
                "Sửa lại vật tư ghi nhầm",
                null, "NPK 13-13-13", null, null, null, null);

        FarmLogResponse response = farmLogService.correctFarmLog(originalLog.getId(), request);

        assertThat(response.getMaterial()).isEqualTo("NPK 13-13-13");
        assertThat(response.getCreatedByName()).isEqualTo("Quản lý HTX");
        assertThat(originalLog.isCorrected()).isTrue();
    }

    @Test
    @DisplayName("Đính chính bản đã có đính chính: trỏ về bản gốc, dùng giá trị mới nhất")
    void correctFarmLog_correctionOfCorrection_pointsToRootAndUsesLatestValues() {
        // Quản lý HTX đính chính tiếp bản đính chính do người khác tạo.
        loginAs(manager);

        FarmLog correction1 = buildFarmLog(FarmActivityType.FERTILIZING,
                "NPK 20-20-20", 30.0, "kg", LocalDate.of(2026, 8, 21),
                "Bón phân lần 1", managerUser);
        correction1.setId(UUID.randomUUID());
        correction1.setOriginalFarmLogId(originalLog);
        correction1.setIsCorrection(true);
        correction1.setCorrectionReason("Đính chính lần 1");

        when(farmLogRepository.findById(correction1.getId()))
                .thenReturn(Optional.of(correction1));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of(correction1));

        CorrectFarmLogRequest request = buildRequest(
                "Chỉnh tiếp số lượng còn sai",
                null, null, 35.0, null, null, null);

        FarmLogResponse response = farmLogService.correctFarmLog(correction1.getId(), request);

        // Bản mới vẫn liên kết tới bản gốc.
        assertThat(response.getOriginalFarmLogId()).isEqualTo(originalLog.getId());
        // Giá trị giữ theo bản hiệu lực (correction1), chỉ quantity thay đổi.
        assertThat(response.getMaterial()).isEqualTo("NPK 20-20-20");
        assertThat(response.getQuantity()).isEqualTo(35.0);
        // Bản trước đó mất hiệu lực.
        assertThat(correction1.isCorrected()).isTrue();
    }

    // ===== Validation failures =====

    @Test
    @DisplayName("Thiếu lý do đính chính -> lỗi nghiệp vụ")
    void correctFarmLog_shouldFail_whenReasonMissing() {
        loginAs(recorder);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));

        CorrectFarmLogRequest request = buildRequest(
                "   ",
                null, null, 30.0, null, null, null);

        assertThatThrownBy(() -> farmLogService.correctFarmLog(originalLog.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lý do đính chính không được để trống");
    }

    @Test
    @DisplayName("Không có trường nào thay đổi so với bản gốc -> lỗi nghiệp vụ")
    void correctFarmLog_shouldFail_whenNoFieldChanged() {
        loginAs(recorder);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of());

        CorrectFarmLogRequest request = buildRequest(
                "Gửi lại dữ liệu cũ",
                null, "NPK 16-16-8", 25.0, "kg", LocalDate.of(2026, 8, 20),
                "Bón phân lần 1");

        assertThatThrownBy(() -> farmLogService.correctFarmLog(originalLog.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Phải có ít nhất một trường được đính chính so với bản gốc.");
    }

    @Test
    @DisplayName("executedDate ở tương lai -> lỗi dữ liệu không hợp lệ")
    void correctFarmLog_shouldFail_whenExecutedDateInFuture() {
        loginAs(recorder);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of());

        CorrectFarmLogRequest request = buildRequest(
                "Sai ngày thực hiện",
                null, null, 30.0, null, LocalDate.of(2026, 8, 24), null);

        assertThatThrownBy(() -> farmLogService.correctFarmLog(originalLog.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Ngày thực hiện không được là ngày ở tương lai.");
    }

    // ===== Permission failures =====

    @Test
    @DisplayName("VT-03 không phải người ghi gốc -> 403")
    void correctFarmLog_shouldFail_whenRecorderNotOwner() {
        loginAs(recorder);

        User otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setFullName("Người ghi khác");
        FarmLog otherLog = buildFarmLog(FarmActivityType.PLANTING, null, null, null,
                LocalDate.of(2026, 8, 19), null, otherUser);

        when(farmLogRepository.findById(otherLog.getId()))
                .thenReturn(Optional.of(otherLog));

        CorrectFarmLogRequest request = buildRequest(
                "Ghi sai ngày gieo trồng",
                null, null, 10.0, null, null, null);

        assertThatThrownBy(() -> farmLogService.correctFarmLog(otherLog.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Bạn chỉ được đính chính nhật ký do bạn ghi.")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Đính chính nhật ký không tồn tại -> 404")
    void correctFarmLog_shouldFail_whenFarmLogNotFound() {
        loginAs(recorder);
        UUID unknownId = UUID.randomUUID();
        when(farmLogRepository.findById(unknownId)).thenReturn(Optional.empty());

        CorrectFarmLogRequest request = buildRequest(
                "Lý do bất kỳ",
                null, null, 30.0, null, null, null);

        assertThatThrownBy(() -> farmLogService.correctFarmLog(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy nhật ký canh tác");
    }

    // ===== Business rule: mã truy xuất đã kích hoạt =====

    @Test
    @DisplayName("Lô đã kích hoạt mã truy xuất + VT-03 -> bị chặn 409")
    void correctFarmLog_shouldBlock_whenTraceCodesActivated_andRecorder() {
        loginAs(recorder);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of());
        when(traceCodeRepository.existsActivatedByProductionLotId(productionLot.getId()))
                .thenReturn(true);

        CorrectFarmLogRequest request = buildRequest(
                "Ghi sai số lượng",
                null, null, 30.0, null, null, null);

        assertThatThrownBy(() -> farmLogService.correctFarmLog(originalLog.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Lô sản xuất đã kích hoạt mã truy xuất. Bạn không thể đính chính nhật ký này.")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Lô đã kích hoạt mã truy xuất + VT-02 -> vẫn được đính chính")
    void correctFarmLog_managerAllowed_whenTraceCodesActivated() {
        loginAs(manager);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));
        when(farmLogRepository.findByOriginalFarmLogId_IdOrderByCreatedAtDesc(originalLog.getId()))
                .thenReturn(List.of());

        CorrectFarmLogRequest request = buildRequest(
                "Lý do bắt buộc khi đã kích hoạt mã",
                null, null, 40.0, null, null, null);

        FarmLogResponse response = farmLogService.correctFarmLog(originalLog.getId(), request);

        assertThat(response.getQuantity()).isEqualTo(40.0);
        assertThat(response.getIsCorrection()).isTrue();
        assertThat(originalLog.isCorrected()).isTrue();
    }

    // ===== getFarmLog (trang đính chính) =====

    @Test
    @DisplayName("Lấy chi tiết nhật ký theo ID: người ghi sự kiện cùng tổ chức")
    void getFarmLog_shouldReturnLog_whenAuthorized() {
        loginAs(recorder);
        when(farmLogRepository.findById(originalLog.getId()))
                .thenReturn(Optional.of(originalLog));

        FarmLogResponse response = farmLogService.getFarmLog(originalLog.getId());

        assertThat(response.getId()).isEqualTo(originalLog.getId());
        assertThat(response.getActivityType()).isEqualTo(FarmActivityType.FERTILIZING);
    }

    @Test
    @DisplayName("Lấy chi tiết nhật ký theo ID: không tồn tại -> 404")
    void getFarmLog_shouldFail_whenNotFound() {
        loginAs(recorder);
        UUID unknownId = UUID.randomUUID();
        when(farmLogRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmLogService.getFarmLog(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy nhật ký canh tác");
    }
}
