package vn.nguongocso.trace.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.trace.dto.request.CancelTraceCodesRequest;
import vn.nguongocso.trace.dto.response.CancelTraceCodesResponse;
import vn.nguongocso.trace.entity.CodeRange;
import vn.nguongocso.trace.entity.LabelCancellationHistory;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.CodeRangeRepository;
import vn.nguongocso.trace.repository.LabelCancellationHistoryRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.impl.LabelCancellationServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LabelCancellationServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private TraceCodeRepository traceCodeRepository;

    @Mock
    private CodeRangeRepository codeRangeRepository;

    @Mock
    private LabelCancellationHistoryRepository labelCancellationHistoryRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LabelCancellationServiceImpl labelCancellationService;

    private UUID shipmentId;
    private UUID orgId;
    private UUID userId;
    private Shipment shipment;
    private CodeRange codeRange;
    private CustomUserDetails currentUserDetails;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Organization org = new Organization();
        org.setOrganizationId(orgId);

        codeRange = new CodeRange();
        codeRange.setId(UUID.randomUUID());
        codeRange.setTotalLimit(1000L);
        codeRange.setUsedCount(100L);

        shipment = new Shipment();
        shipment.setId(shipmentId);
        shipment.setName("Lô Xoài Cát Hòa Lộc 01");
        shipment.setOrganization(org);
        shipment.setCodeRange(codeRange);

        currentUserDetails = mock(CustomUserDetails.class);
        when(currentUserDetails.getUserId()).thenReturn(userId);
        when(currentUserDetails.getOrganizationId()).thenReturn(orgId);
        when(currentUserDetails.getFullName()).thenReturn("Quản lý HTX");
        when(currentUserDetails.getUsername()).thenReturn("manager");
        when(currentUserDetails.getRoleCode()).thenReturn("VT-02");

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-01: Hủy 10 tem chưa kích hoạt thành công & hoàn hạn mức đúng 10")
    void testCancelLabels_Success_TC01() {
        when(authentication.getPrincipal()).thenReturn(currentUserDetails);
        doNothing().when(permissionChecker).check("shipment", "UPDATE");
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        codeRange.setPrefix("PREFIX-");

        TraceCode tc1 = new TraceCode();
        tc1.setCodeValue("PREFIX-0001");
        tc1.setStatus(TraceCodeStatus.INACTIVE);

        TraceCode tc2 = new TraceCode();
        tc2.setCodeValue("PREFIX-0002");
        tc2.setStatus(TraceCodeStatus.INACTIVE);

        List<TraceCode> mockCodes = List.of(tc1, tc2);

        CancelTraceCodesRequest request = CancelTraceCodesRequest.builder()
                .cancelType("RANGE")
                .fromCode("PREFIX-0001")
                .toCode("PREFIX-0002")
                .reasonType("PRINT_ERROR")
                .reasonNote("Mờ mực QR")
                .build();

        when(traceCodeRepository.findByShipmentIdAndCodeValue(shipmentId, "PREFIX-0001")).thenReturn(Optional.of(tc1));
        when(traceCodeRepository.findByShipmentIdAndCodeValue(shipmentId, "PREFIX-0002")).thenReturn(Optional.of(tc2));
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(mockCodes);
        when(codeRangeRepository.refundQuota(eq(codeRange.getId()), eq(2L))).thenReturn(1);

        CancelTraceCodesResponse response = labelCancellationService.cancelTraceCodes(shipmentId, request);

        assertNotNull(response);
        assertEquals(2, response.getTotalCancelled());
        assertEquals(2L, response.getRefundedQuota());
        assertEquals(TraceCodeStatus.CANCELLED, tc1.getStatus());
        assertEquals(TraceCodeStatus.CANCELLED, tc2.getStatus());

        verify(labelCancellationHistoryRepository).save(any(LabelCancellationHistory.class));
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-02: Hủy tem đã kích hoạt -> Hệ thống chặn và hướng dẫn khóa mã / thu hồi")
    void testCancelLabels_BlockedWhenActive_TC02() {
        when(authentication.getPrincipal()).thenReturn(currentUserDetails);
        doNothing().when(permissionChecker).check("shipment", "UPDATE");
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        TraceCode tc1 = new TraceCode();
        tc1.setCodeValue("PREFIX-0001");
        tc1.setStatus(TraceCodeStatus.ACTIVE); // Tem đã kích hoạt!

        CancelTraceCodesRequest request = CancelTraceCodesRequest.builder()
                .cancelType("SINGLE")
                .codeValues(List.of("PREFIX-0001"))
                .reasonType("PRINT_ERROR")
                .build();

        when(traceCodeRepository.findByShipmentIdAndCodeValueIn(shipmentId, List.of("PREFIX-0001")))
                .thenReturn(List.of(tc1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> labelCancellationService.cancelTraceCodes(shipmentId, request));

        assertTrue(exception.getMessage().contains("Không thể hủy tem đã kích hoạt"));
        assertTrue(exception.getMessage().contains("Khóa mã (LOCKED) hoặc Thu hồi (RECALLED)"));
    }

    @Test
    @DisplayName("NCL-04-CN-006-TC-03: Nhập sai tiền tố hoặc mã không tồn tại -> Chặn giao dịch với lỗi rõ ràng")
    void testCancelLabels_InvalidPrefixOrNonExistentCode_TC03() {
        when(authentication.getPrincipal()).thenReturn(currentUserDetails);
        doNothing().when(permissionChecker).check("shipment", "UPDATE");
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        codeRange.setPrefix("GGJ");

        // Request with wrong prefix "GG" instead of "GGJ"
        CancelTraceCodesRequest requestWrongPrefix = CancelTraceCodesRequest.builder()
                .cancelType("RANGE")
                .fromCode("GG00001259")
                .toCode("GGJ000012510")
                .reasonType("PRINT_ERROR")
                .build();

        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> labelCancellationService.cancelTraceCodes(shipmentId, requestWrongPrefix));
        assertTrue(ex1.getMessage().contains("không đúng tiền tố dải mã 'GGJ'"));

        // Request with non-existent start code
        CancelTraceCodesRequest requestNonExistent = CancelTraceCodesRequest.builder()
                .cancelType("RANGE")
                .fromCode("GGJ00009999")
                .toCode("GGJ00009999")
                .reasonType("PRINT_ERROR")
                .build();

        when(traceCodeRepository.findByShipmentIdAndCodeValue(shipmentId, "GGJ00009999"))
                .thenReturn(Optional.empty());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> labelCancellationService.cancelTraceCodes(shipmentId, requestNonExistent));
        assertTrue(ex2.getMessage().contains("không tồn tại trong lô hàng này"));
    }
}
