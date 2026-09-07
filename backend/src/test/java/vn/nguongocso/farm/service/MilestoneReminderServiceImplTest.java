package vn.nguongocso.farm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.certification.service.MilestoneValidationService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.response.MilestoneReminderResponse;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.LotAssignment;
import vn.nguongocso.farm.entity.MilestoneReminder;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.LotAssignmentRepository;
import vn.nguongocso.farm.repository.MilestoneReminderRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.impl.MilestoneReminderServiceImpl;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cho dịch vụ nhắc việc mốc canh tác (NCL-03-CN-007).
 * Bao gồm đầy đủ 4 Tiêu chí chấp nhận (TC-01 -> TC-04).
 */
@ExtendWith(MockitoExtension.class)
class MilestoneReminderServiceImplTest {

    @Mock
    private ProductionLotRepository productionLotRepository;

    @Mock
    private MilestoneReminderRepository milestoneReminderRepository;

    @Mock
    private MilestoneValidationService milestoneValidationService;

    @Mock
    private LotAssignmentRepository lotAssignmentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @InjectMocks
    private MilestoneReminderServiceImpl reminderService;

    private ProductionLot activeLot;
    private CultivationMilestone milestoneBonPhan;
    private User assignedUser;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        organization.setName("Hợp tác xã Nông nghiệp Xanh");

        assignedUser = new User();
        assignedUser.setUserId(UUID.randomUUID());
        assignedUser.setFullName("Nguyễn Văn Ghi");
        assignedUser.setEmail("recorder@example.com");

        ProductCategory category = new ProductCategory();
        category.setId(UUID.randomUUID());
        category.setName("Lúa gạo");

        activeLot = new ProductionLot();
        activeLot.setId(UUID.randomUUID());
        activeLot.setName("Lô Lúa ST25 - Vụ Đông Xuân");
        activeLot.setStatus(ProductionLotStatus.APPROVED);
        activeLot.setOrganization(organization);
        activeLot.setProductCategory(category);
        activeLot.setCreatedBy(assignedUser);

        milestoneBonPhan = CultivationMilestone.builder()
                .id(101L)
                .name("Bón phân đợt một")
                .activityType("FERTILIZING")
                .expectedDaysFromPlanting(10)
                .isMandatory(true)
                .build();
    }

    @Test
    @DisplayName("TC-01: Luồng thành công - Lô thiếu mốc bón phân đợt một quá hạn ba ngày nhận nhắc việc")
    void testScanOverdueMilestones_TC01_Success() {
        // Gieo trồng ngày 13 ngày trước -> Ngày dự kiến = 13 - 10 = 3 ngày trước (quá hạn 3 ngày)
        LocalDate today = LocalDate.now();
        activeLot.setPlantingDate(today.minusDays(13));

        when(productionLotRepository.findByStatusIn(any()))
                .thenReturn(List.of(activeLot));
        when(milestoneValidationService.findMissingMilestones(activeLot))
                .thenReturn(List.of(milestoneBonPhan));
        when(milestoneReminderRepository.existsByProductionLot_IdAndMilestone_IdAndUser_UserIdAndReminderDate(
                any(), any(), any(), any()))
                .thenReturn(false);

        LotAssignment assignment = LotAssignment.builder()
                .id(UUID.randomUUID())
                .productionLot(activeLot)
                .user(assignedUser)
                .active(true)
                .build();
        when(lotAssignmentRepository.findByProductionLot_IdAndActiveTrue(activeLot.getId()))
                .thenReturn(List.of(assignment));

        Notification mockSavedNotification = new Notification();
        mockSavedNotification.setId(UUID.randomUUID());
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(mockSavedNotification);

        // Thực thi quét
        MilestoneScanResult result = reminderService.scanOverdueMilestones();

        // Kiểm tra kết quả
        assertThat(result.getScannedLotsCount()).isEqualTo(1);
        assertThat(result.getRemindersCreatedCount()).isEqualTo(1);

        // Xác minh lưu thông báo với đúng nội dung NCL-08-CN-005
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getUser()).isEqualTo(assignedUser);
        assertThat(capturedNotification.getType()).isEqualTo(NotificationType.TASK);
        assertThat(capturedNotification.getContent()).contains("Lô Lúa ST25 - Vụ Đông Xuân");
        assertThat(capturedNotification.getContent()).contains("Bón phân đợt một");
        assertThat(capturedNotification.getContent()).contains("quá hạn 3 ngày");

        // Xác minh lưu nhắc việc với trạng thái OPEN và số ngày quá hạn 3
        ArgumentCaptor<MilestoneReminder> reminderCaptor = ArgumentCaptor.forClass(MilestoneReminder.class);
        verify(milestoneReminderRepository).save(reminderCaptor.capture());
        MilestoneReminder capturedReminder = reminderCaptor.getValue();
        assertThat(capturedReminder.getProductionLot()).isEqualTo(activeLot);
        assertThat(capturedReminder.getMilestone()).isEqualTo(milestoneBonPhan);
        assertThat(capturedReminder.getUser()).isEqualTo(assignedUser);
        assertThat(capturedReminder.getOverdueDays()).isEqualTo(3);
        assertThat(capturedReminder.getStatus()).isEqualTo(MilestoneReminderStatus.OPEN);
        assertThat(capturedReminder.getReminderDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("TC-02: Luồng thành công - Nhắc việc đang mở tự chuyển sang đã hoàn thành khi ghi nhật ký")
    void testCompleteRemindersForLotAndActivity_TC02_Success() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setIsRead(false);

        MilestoneReminder openReminder = MilestoneReminder.builder()
                .id(UUID.randomUUID())
                .productionLot(activeLot)
                .milestone(milestoneBonPhan)
                .user(assignedUser)
                .notification(notification)
                .overdueDays(3)
                .reminderDate(LocalDate.now())
                .status(MilestoneReminderStatus.OPEN)
                .build();

        when(milestoneReminderRepository.findByProductionLot_IdAndStatus(activeLot.getId(), MilestoneReminderStatus.OPEN))
                .thenReturn(List.of(openReminder));

        // Người ghi nhập nhật ký cho mốc bón phân (FERTILIZING)
        reminderService.completeRemindersForLotAndActivity(activeLot.getId(), FarmActivityType.FERTILIZING);

        // Nhắc việc tự chuyển sang COMPLETED và cập nhật completedAt
        assertThat(openReminder.getStatus()).isEqualTo(MilestoneReminderStatus.COMPLETED);
        assertThat(openReminder.getCompletedAt()).isNotNull();
        assertThat(notification.getIsRead()).isTrue();

        verify(milestoneReminderRepository).save(openReminder);
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("TC-03: Ngoại lệ - Lô đã bị hủy thì hệ thống không tạo nhắc việc")
    void testScanOverdueMilestones_TC03_CancelledLotIgnored() {
        ProductionLot cancelledLot = new ProductionLot();
        cancelledLot.setId(UUID.randomUUID());
        cancelledLot.setName("Lô đã hủy");
        cancelledLot.setStatus(ProductionLotStatus.CANCELLED);
        cancelledLot.setPlantingDate(LocalDate.now().minusDays(20));
        cancelledLot.setProductCategory(activeLot.getProductCategory());

        // findByStatusIn trả về rỗng vì lô đã hủy
        when(productionLotRepository.findByStatusIn(any()))
                .thenReturn(Collections.emptyList());

        MilestoneScanResult result = reminderService.scanOverdueMilestones();

        assertThat(result.getScannedLotsCount()).isEqualTo(0);
        assertThat(result.getRemindersCreatedCount()).isEqualTo(0);
        verifyNoInteractions(notificationRepository);
        verify(milestoneReminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Dữ liệu trùng lặp - Không tạo nhắc việc trùng cho mốc đó trong cùng ngày")
    void testScanOverdueMilestones_TC04_DuplicateCheckSameDay() {
        LocalDate today = LocalDate.now();
        activeLot.setPlantingDate(today.minusDays(13));

        when(productionLotRepository.findByStatusIn(any()))
                .thenReturn(List.of(activeLot));
        when(milestoneValidationService.findMissingMilestones(activeLot))
                .thenReturn(List.of(milestoneBonPhan));
        // Đã tạo nhắc việc cho mốc đó trong ngày hôm nay cho người dùng
        when(milestoneReminderRepository.existsByProductionLot_IdAndMilestone_IdAndUser_UserIdAndReminderDate(
                any(), any(), any(), any()))
                .thenReturn(true);

        MilestoneScanResult result = reminderService.scanOverdueMilestones();

        assertThat(result.getScannedLotsCount()).isEqualTo(1);
        assertThat(result.getRemindersCreatedCount()).isEqualTo(0); // Không tạo thêm nhắc việc
        verify(notificationRepository, never()).save(any());
        verify(milestoneReminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mốc chưa đến hạn (chưa quá hạn) thì không tạo nhắc việc")
    void testScanOverdueMilestones_NotOverdueYet() {
        LocalDate today = LocalDate.now();
        // Mới gieo trồng 5 ngày trước, mốc dự kiến sau 10 ngày -> chưa quá hạn (còn 5 ngày)
        activeLot.setPlantingDate(today.minusDays(5));

        when(productionLotRepository.findByStatusIn(any()))
                .thenReturn(List.of(activeLot));
        when(milestoneValidationService.findMissingMilestones(activeLot))
                .thenReturn(List.of(milestoneBonPhan));

        MilestoneScanResult result = reminderService.scanOverdueMilestones();

        assertThat(result.getScannedLotsCount()).isEqualTo(1);
        assertThat(result.getRemindersCreatedCount()).isEqualTo(0);
        verify(milestoneReminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lô không có ngày gieo trồng thì bỏ qua không quét")
    void testScanOverdueMilestones_NoPlantingDate() {
        activeLot.setPlantingDate(null);

        when(productionLotRepository.findByStatusIn(any()))
                .thenReturn(List.of(activeLot));

        MilestoneScanResult result = reminderService.scanOverdueMilestones();

        assertThat(result.getScannedLotsCount()).isEqualTo(0);
        assertThat(result.getRemindersCreatedCount()).isEqualTo(0);
        verifyNoInteractions(milestoneValidationService);
    }

    @Test
    @DisplayName("Lấy danh sách nhắc việc đang mở của người dùng hiện tại (VT-03)")
    void testGetMyActiveReminders_VT03() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleCode()).thenReturn(RoleCode.EVENT_RECORDER);
        when(userDetails.getUserId()).thenReturn(assignedUser.getUserId());
        when(userDetails.getOrganizationId()).thenReturn(activeLot.getOrganization().getOrganizationId());

        MilestoneReminder reminder = MilestoneReminder.builder()
                .id(UUID.randomUUID())
                .productionLot(activeLot)
                .milestone(milestoneBonPhan)
                .user(assignedUser)
                .overdueDays(5)
                .status(MilestoneReminderStatus.OPEN)
                .reminderDate(LocalDate.now())
                .build();

        when(milestoneReminderRepository.findActiveRemindersForUserOrOrganization(
                eq(assignedUser.getUserId()), eq(activeLot.getOrganization().getOrganizationId()), eq(MilestoneReminderStatus.OPEN)))
                .thenReturn(List.of(reminder));

        List<MilestoneReminderResponse> responses = reminderService.getMyActiveReminders(userDetails);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMilestoneName()).isEqualTo("Bón phân đợt một");
        assertThat(responses.get(0).getOverdueDays()).isEqualTo(5);
        assertThat(responses.get(0).getStatus()).isEqualTo(MilestoneReminderStatus.OPEN);
    }
}
