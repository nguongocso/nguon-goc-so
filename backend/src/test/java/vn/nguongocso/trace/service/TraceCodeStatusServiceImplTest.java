package vn.nguongocso.trace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.dto.request.ExportTraceCodesRequest;
import vn.nguongocso.trace.dto.response.HistoryEvent;
import vn.nguongocso.trace.dto.response.TraceCodeHistoryResponse;
import vn.nguongocso.trace.dto.response.TraceCodeSummaryResponse;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.TraceCodeStatusServiceImpl;

/**
 * Unit test cho {@link TraceCodeStatusServiceImpl} (NCL-04-CN-008).
 */
@ExtendWith(MockitoExtension.class)
class TraceCodeStatusServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private TraceCodeScanLogRepository traceCodeScanLogRepository;

    @InjectMocks
    private TraceCodeStatusServiceImpl traceCodeStatusService;

    private UUID orgId;
    private UUID shipmentId;
    private Organization organization;
    private Shipment shipment;
    private CustomUserDetails managerUser;
    private CustomUserDetails otherOrgUser;
    private CustomUserDetails wrongRoleUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        shipmentId = UUID.randomUUID();

        organization = new Organization();
        organization.setOrganizationId(orgId);
        organization.setName("HTX Nông Nghiệp Thử Nghiệm");

        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setFullName("Nguyễn Văn Quản Lý");

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô hàng bưởi 01");
        shipment.setOrganization(organization);
        shipment.setStatus(ShipmentStatus.ACTIVATED);
        shipment.setCreatedBy(testUser);
        shipment.setCreatedAt(LocalDateTime.of(2026, 9, 1, 8, 0, 0));

        managerUser = mock(CustomUserDetails.class);
        lenient().when(managerUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(managerUser.getOrganizationId()).thenReturn(orgId);

        otherOrgUser = mock(CustomUserDetails.class);
        lenient().when(otherOrgUser.getRoleCode()).thenReturn("VT-02");
        lenient().when(otherOrgUser.getOrganizationId()).thenReturn(UUID.randomUUID());

        wrongRoleUser = mock(CustomUserDetails.class);
        lenient().when(wrongRoleUser.getRoleCode()).thenReturn("VT-03");
    }

    // ==================== 1. getTraceCodesByShipment ====================

    @Test
    @DisplayName("TC-01: Lấy danh sách mã tem thành công kèm số lượt quét")
    void getTraceCodesByShipment_success() {
        UUID codeId = UUID.randomUUID();
        TraceCode code = new TraceCode();
        code.setId(codeId);
        code.setCodeValue("HTX-001");
        code.setStatus(TraceCodeStatus.ACTIVE);
        code.setShipment(shipment);
        code.setCreatedAt(LocalDateTime.of(2026, 9, 1, 8, 0, 0));
        code.setActivatedAt(LocalDateTime.of(2026, 9, 2, 9, 0, 0));
        code.setPrintedAt(LocalDateTime.of(2026, 9, 1, 10, 0, 0));

        Pageable pageable = PageRequest.of(0, 20);
        Page<TraceCode> page = new PageImpl<>(List.of(code), pageable, 1);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findByShipmentAndFilters(shipmentId, orgId, TraceCodeStatus.ACTIVE, "HTX", pageable))
                .thenReturn(page);
        when(traceCodeScanLogRepository.countScansByTraceCodeIds(List.of(codeId)))
                .thenReturn(List.<Object[]>of(new Object[]{codeId, 7L}));

        PageResponse<TraceCodeSummaryResponse> result = traceCodeStatusService.getTraceCodesByShipment(
                shipmentId, "ACTIVE", "HTX", pageable, managerUser);

        assertThat(result.getItems()).hasSize(1);
        TraceCodeSummaryResponse item = result.getItems().get(0);
        assertThat(item.getCodeValue()).isEqualTo("HTX-001");
        assertThat(item.getStatus()).isEqualTo(TraceCodeStatus.ACTIVE);
        assertThat(item.getScanCount()).isEqualTo(7L);
        assertThat(item.getActivatedAt()).isEqualTo(LocalDateTime.of(2026, 9, 2, 9, 0, 0));
        assertThat(item.getPrintedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 10, 0, 0));
    }

    @Test
    @DisplayName("TC-03: Lấy danh sách rỗng khi chưa có mã tem")
    void getTraceCodesByShipment_empty() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TraceCode> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findByShipmentAndFilters(shipmentId, orgId, null, null, pageable))
                .thenReturn(emptyPage);

        PageResponse<TraceCodeSummaryResponse> result = traceCodeStatusService.getTraceCodesByShipment(
                shipmentId, null, null, pageable, managerUser);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Chặn truy cập nếu người dùng không có vai trò VT-02 (403 FORBIDDEN)")
    void getTraceCodesByShipment_forbidden_whenRoleNotVT02() {
        Pageable pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> traceCodeStatusService.getTraceCodesByShipment(
                shipmentId, null, null, pageable, wrongRoleUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("TC-04: Chặn truy cập lô hàng thuộc tổ chức khác (QTN-01 - 403 FORBIDDEN)")
    void getTraceCodesByShipment_forbidden_whenDifferentOrganization() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        Pageable pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> traceCodeStatusService.getTraceCodesByShipment(
                shipmentId, null, null, pageable, otherOrgUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("Báo lỗi 404 NOT_FOUND khi không tìm thấy lô hàng")
    void getTraceCodesByShipment_shipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> traceCodeStatusService.getTraceCodesByShipment(
                shipmentId, null, null, pageable, managerUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Báo lỗi khi truyền trạng thái lọc không hợp lệ")
    void getTraceCodesByShipment_invalidStatus() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        Pageable pageable = PageRequest.of(0, 20);
        assertThatThrownBy(() -> traceCodeStatusService.getTraceCodesByShipment(
                shipmentId, "INVALID_STATUS", null, pageable, managerUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trạng thái mã tem không hợp lệ");
    }

    // ==================== 2. getTraceCodeHistory ====================

    @Test
    @DisplayName("TC-02: Lấy lịch sử chi tiết mã tem đầy đủ các loại sự kiện")
    void getTraceCodeHistory_success_allEvents() {
        UUID codeId = UUID.randomUUID();
        User adminUser = new User();
        adminUser.setUserId(UUID.randomUUID());
        adminUser.setFullName("Lê Quản Trị");

        TraceCode code = new TraceCode();
        code.setId(codeId);
        code.setCodeValue("HTX-001");
        code.setStatus(TraceCodeStatus.LOCKED);
        code.setShipment(shipment);
        code.setCreatedAt(LocalDateTime.of(2026, 9, 1, 8, 0, 0));
        code.setPrintedAt(LocalDateTime.of(2026, 9, 1, 9, 0, 0));
        code.setPrintBatchId("BATCH-01");
        code.setActivatedAt(LocalDateTime.of(2026, 9, 2, 8, 30, 0));
        code.setActivatedBy(testUser);
        code.setLockedAt(LocalDateTime.of(2026, 9, 3, 10, 0, 0));
        code.setLockedBy(adminUser);
        code.setLockReason("Nghi vấn quét bất thường từ 2 địa bàn");

        TraceCodeScanLog scanLog = TraceCodeScanLog.builder()
                .id(UUID.randomUUID())
                .traceCode(code)
                .scannedAt(LocalDateTime.of(2026, 9, 2, 14, 20, 0))
                .location("TP. Hà Nội")
                .ipAddress("192.168.1.1")
                .isAbnormal(false)
                .build();

        when(traceCodeRepository.findByCodeValue("HTX-001")).thenReturn(Optional.of(code));
        when(traceCodeScanLogRepository.countByTraceCode_Id(codeId)).thenReturn(1L);
        when(traceCodeScanLogRepository.findTop5ByTraceCode_IdOrderByScannedAtDesc(codeId))
                .thenReturn(List.of(scanLog));

        TraceCodeHistoryResponse result = traceCodeStatusService.getTraceCodeHistory("HTX-001", managerUser);

        assertThat(result.getCodeValue()).isEqualTo("HTX-001");
        assertThat(result.getStatus()).isEqualTo(TraceCodeStatus.LOCKED);
        assertThat(result.getShipmentName()).isEqualTo("Lô hàng bưởi 01");
        assertThat(result.getScanCount()).isEqualTo(1L);

        List<HistoryEvent> events = result.getEvents();
        assertThat(events).isNotEmpty();
        // Kiểm tra có các sự kiện: CREATED, PRINTED, ACTIVATED, LOCKED, SCANNED
        List<String> eventTypes = events.stream().map(HistoryEvent::getType).toList();
        assertThat(eventTypes).contains("CREATED", "PRINTED", "ACTIVATED", "LOCKED", "SCANNED");
    }

    @Test
    @DisplayName("Tra cứu lịch sử báo 404 khi mã tem không tồn tại")
    void getTraceCodeHistory_notFound() {
        when(traceCodeRepository.findByCodeValue("NON_EXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> traceCodeStatusService.getTraceCodeHistory("NON_EXIST", managerUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tra cứu lịch sử báo 403 khi mã tem thuộc tổ chức khác (QTN-01)")
    void getTraceCodeHistory_forbidden_whenDifferentOrganization() {
        TraceCode code = new TraceCode();
        code.setId(UUID.randomUUID());
        code.setCodeValue("HTX-001");
        code.setShipment(shipment);

        when(traceCodeRepository.findByCodeValue("HTX-001")).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> traceCodeStatusService.getTraceCodeHistory("HTX-001", otherOrgUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ==================== 3. exportTraceCodes ====================

    @Test
    @DisplayName("Xuất danh sách mã tem ra file CSV thành công kèm BOM UTF-8")
    void exportTraceCodes_success() {
        UUID codeId = UUID.randomUUID();
        TraceCode code = new TraceCode();
        code.setId(codeId);
        code.setCodeValue("HTX-001");
        code.setStatus(TraceCodeStatus.ACTIVE);
        code.setShipment(shipment);
        code.setCreatedAt(LocalDateTime.of(2026, 9, 1, 8, 0, 0));
        code.setPrintedAt(LocalDateTime.of(2026, 9, 1, 9, 0, 0));
        code.setActivatedAt(LocalDateTime.of(2026, 9, 2, 8, 30, 0));

        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(traceCodeRepository.findAllByShipmentAndFilters(shipmentId, orgId, null, null))
                .thenReturn(List.of(code));
        when(traceCodeScanLogRepository.countScansByTraceCodeIds(List.of(codeId)))
                .thenReturn(List.<Object[]>of(new Object[]{codeId, 3L}));

        ExportTraceCodesRequest req = ExportTraceCodesRequest.builder().build();
        byte[] csvBytes = traceCodeStatusService.exportTraceCodes(shipmentId, req, managerUser);

        assertThat(csvBytes).isNotEmpty();
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        // Kiểm tra BOM UTF-8
        assertThat(csvContent.charAt(0)).isEqualTo('﻿');

        // Kiểm tra tiêu đề các cột
        assertThat(csvContent).contains("STT,Mã tem,Trạng thái,Ngày tạo,Ngày in,Ngày kích hoạt,Lượt quét");

        // Kiểm tra nội dung dòng
        assertThat(csvContent).contains("1,HTX-001,Đã kích hoạt,01/09/2026 08:00:00,01/09/2026 09:00:00,02/09/2026 08:30:00,3");
    }

    @Test
    @DisplayName("Xuất danh sách mã tem báo 403 khi lô hàng thuộc tổ chức khác")
    void exportTraceCodes_forbidden_whenDifferentOrganization() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        ExportTraceCodesRequest req = ExportTraceCodesRequest.builder().build();
        assertThatThrownBy(() -> traceCodeStatusService.exportTraceCodes(shipmentId, req, otherOrgUser))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
