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
}