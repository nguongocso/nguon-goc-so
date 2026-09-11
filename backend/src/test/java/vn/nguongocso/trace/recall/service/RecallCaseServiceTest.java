package vn.nguongocso.trace.recall.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.notification.service.NotificationService;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.trace.recall.dto.request.CloseRecallCaseRequest;
import vn.nguongocso.trace.recall.entity.RecallCase;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.recall.repository.RecallLotResultRepository;
import vn.nguongocso.trace.recall.service.impl.RecallCaseServiceImpl;
import vn.nguongocso.trace.repository.ShipmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecallCaseServiceTest {

    @Mock
    private RecallCaseRepository recallCaseRepository;

    @Mock
    private RecallLotResultRepository recallLotResultRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ChainEventRepository chainEventRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private vn.nguongocso.trace.repository.TraceCodeRepository traceCodeRepository;

    @Mock
    private vn.nguongocso.trace.repository.CodeRangeRepository codeRangeRepository;

    @InjectMocks
    private RecallCaseServiceImpl recallCaseService;

    private CustomUserDetails mockManagerUser() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-02");
        when(user.getOrganizationId()).thenReturn(UUID.randomUUID());
        return user;
    }

    @Test
    void testCloseForbiddenForNonManager() {
        // TC: NT-02 — chỉ Quản lý hợp tác xã (VT-02) mới được đóng vụ việc
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getRoleCode()).thenReturn("VT-03");

        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setRemediationMeasures("Biện pháp khắc phục phòng ngừa.");

        assertThrows(BusinessException.class,
                () -> recallCaseService.close(UUID.randomUUID(), req, user));
        verifyNoInteractions(recallCaseRepository);
    }

    @Test
    void testCloseCaseNotFound() {
        // TC-01: case không thuộc tổ chức hiện tại hoặc không tồn tại → lỗi 404
        CustomUserDetails user = mockManagerUser();
        when(recallCaseRepository.findByIdAndOrganizationId(any(), any()))
                .thenReturn(Optional.empty());

        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setRemediationMeasures("Biện pháp khắc phục phòng ngừa.");

        assertThrows(BusinessException.class,
                () -> recallCaseService.close(UUID.randomUUID(), req, user));
    }

    @Test
    void testCloseRequiresRemediationMeasures() {
        // TC-02: thiếu biện pháp khắc phục phòng ngừa → chặn đóng vụ việc (QTN-27)
        CustomUserDetails user = mockManagerUser();
        RecallCase recallCase = new RecallCase();
        recallCase.setId(UUID.randomUUID());
        recallCase.setStatus(RecallCaseStatus.OPEN);
        when(recallCaseRepository.findByIdAndOrganizationId(any(), any()))
                .thenReturn(Optional.of(recallCase));

        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setRemediationMeasures("   ");

        assertThrows(BusinessException.class,
                () -> recallCaseService.close(recallCase.getId(), req, user));
    }

    @Test
    void testCloseAlreadyClosed() {
        // TC: vụ việc đã đóng → không cho đóng lại (một chiều)
        CustomUserDetails user = mockManagerUser();
        RecallCase recallCase = new RecallCase();
        recallCase.setId(UUID.randomUUID());
        recallCase.setStatus(RecallCaseStatus.CLOSED);
        when(recallCaseRepository.findByIdAndOrganizationId(any(), any()))
                .thenReturn(Optional.of(recallCase));

        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setRemediationMeasures("Biện pháp khắc phục phòng ngừa.");

        assertThrows(BusinessException.class,
                () -> recallCaseService.close(recallCase.getId(), req, user));
    }

    @Test
    void testCloseSuccess_TransitionsRecallingToRecalled() {
        // TC-03: Đóng vụ việc thành công, chuyển lô hàng từ RECALLING sang RECALLED
        CustomUserDetails user = mockManagerUser();
        UUID caseId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        vn.nguongocso.organization.entity.Organization org = new vn.nguongocso.organization.entity.Organization();
        org.setOrganizationId(user.getOrganizationId());

        vn.nguongocso.farm.entity.ProductionLot lot = new vn.nguongocso.farm.entity.ProductionLot();
        lot.setId(lotId);
        lot.setName("Lô Test");
        lot.setExpectedQuantityUnit("kg");
        lot.setOrganization(org);

        RecallCase recallCase = new RecallCase();
        recallCase.setId(caseId);
        recallCase.setCaseCode("RC-20260911-001");
        recallCase.setStatus(RecallCaseStatus.OPEN);
        recallCase.setProductionLot(lot);
        recallCase.setOrganizationId(user.getOrganizationId());

        vn.nguongocso.trace.entity.Shipment shipment = new vn.nguongocso.trace.entity.Shipment();
        shipment.setId(shipmentId);
        shipment.setName("SHIP-01");
        shipment.setStatus(vn.nguongocso.trace.enums.ShipmentStatus.RECALLING);
        shipment.setTotalQuantity(100L);
        shipment.setOrganization(org);
        shipment.setProductionLot(lot);

        when(recallCaseRepository.findByIdAndOrganizationId(caseId, user.getOrganizationId()))
                .thenReturn(Optional.of(recallCase));
        when(shipmentRepository.findByProductionLotIdAndStatus(
                lotId, vn.nguongocso.trace.enums.ShipmentStatus.RECALLING))
                .thenReturn(List.of(shipment));
        when(recallLotResultRepository.findByRecallCaseId(caseId)).thenReturn(List.of());
        when(traceCodeRepository.findByShipmentId(shipmentId)).thenReturn(List.of());
        when(chainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(any()))
                .thenReturn(List.of());

        CloseRecallCaseRequest req = new CloseRecallCaseRequest();
        req.setRemediationMeasures("Biện pháp khắc phục chi tiết");
        CloseRecallCaseRequest.LotResultItem item = new CloseRecallCaseRequest.LotResultItem();
        item.setShipmentId(shipmentId);
        item.setResolution(vn.nguongocso.trace.recall.enums.LotResolution.DESTROYED);
        item.setRecoveredQuantity(java.math.BigDecimal.valueOf(80.0));
        req.setLotResults(List.of(item));

        vn.nguongocso.trace.recall.dto.response.RecallCaseResponse response =
                recallCaseService.close(caseId, req, user);

        assertNotNull(response);
        assertEquals(RecallCaseStatus.CLOSED, response.getStatus());
        assertEquals(vn.nguongocso.trace.enums.ShipmentStatus.RECALLED, shipment.getStatus());
        verify(shipmentRepository).save(shipment);
        verify(recallCaseRepository).save(recallCase);
        verify(recallLotResultRepository).saveAll(any());
    }
}