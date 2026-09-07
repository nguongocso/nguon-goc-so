package vn.nguongocso.farm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.enums.NotificationType;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.CultivationMilestone;
import vn.nguongocso.certification.service.MilestoneValidationService;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.response.MilestoneReminderResponse;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.entity.LotAssignment;
import vn.nguongocso.farm.entity.MilestoneReminder;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.LotAssignmentRepository;
import vn.nguongocso.farm.repository.MilestoneReminderRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.MilestoneReminderService;
import vn.nguongocso.notification.entity.Notification;
import vn.nguongocso.notification.repository.NotificationRepository;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.enums.OrganizationUserStatus;
import vn.nguongocso.organization.repository.OrganizationUserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Triển khai dịch vụ nhắc lịch ghi nhật ký theo mốc canh tác bắt buộc (NCL-03-CN-007).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MilestoneReminderServiceImpl implements MilestoneReminderService {

    private final ProductionLotRepository productionLotRepository;
    private final MilestoneReminderRepository milestoneReminderRepository;
    private final MilestoneValidationService milestoneValidationService;
    private final LotAssignmentRepository lotAssignmentRepository;
    private final NotificationRepository notificationRepository;
    private final OrganizationUserRepository organizationUserRepository;

    private static final List<ProductionLotStatus> SCAN_STATUSES = List.of(
            ProductionLotStatus.APPROVED,
            ProductionLotStatus.HARVESTED
    );

    @Override
    public MilestoneScanResult scanOverdueMilestones() {
        log.info("⏰ Bắt đầu quét mốc canh tác quá hạn trên toàn hệ thống.");
        List<ProductionLot> cultivatingLots = productionLotRepository.findByStatusIn(SCAN_STATUSES);
        return doScanLots(cultivatingLots);
    }

    @Override
    public MilestoneScanResult scanOverdueMilestonesForOrganization(UUID organizationId) {
        log.info("⏰ Bắt đầu quét mốc canh tác quá hạn cho tổ chức: {}", organizationId);
        List<ProductionLot> cultivatingLots = productionLotRepository.findByOrganization_OrganizationIdAndStatusIn(
                organizationId, SCAN_STATUSES);
        return doScanLots(cultivatingLots);
    }

    private MilestoneScanResult doScanLots(List<ProductionLot> lots) {
        LocalDate today = LocalDate.now();
        int scannedLotsCount = 0;
        int remindersCreatedCount = 0;

        for (ProductionLot lot : lots) {
            // Quy tắc: Không nhắc với lô đã hủy, đã thu hồi hoặc đã đóng gói (TC-03)
            if (lot.getStatus() == ProductionLotStatus.CANCELLED
                    || lot.getStatus() == ProductionLotStatus.RECALLED
                    || lot.getStatus() == ProductionLotStatus.PACKAGED
                    || lot.getStatus() == ProductionLotStatus.DISPOSED
                    || lot.getStatus() == ProductionLotStatus.CLOSED) {
                continue;
            }

            // Phải có ngày gieo trồng và loại nông sản để đối chiếu mốc
            if (lot.getPlantingDate() == null || lot.getProductCategory() == null) {
                continue;
            }

            scannedLotsCount++;

            // Tìm các mốc bắt buộc còn thiếu
            List<CultivationMilestone> missingMilestones = milestoneValidationService.findMissingMilestones(lot);
            if (missingMilestones.isEmpty()) {
                continue;
            }

            for (CultivationMilestone milestone : missingMilestones) {
                if (milestone.getExpectedDaysFromPlanting() == null) {
                    continue;
                }

                LocalDate expectedDate = lot.getPlantingDate().plusDays(milestone.getExpectedDaysFromPlanting());

                // Chỉ nhắc khi mốc quá hạn dự kiến
                if (!today.isAfter(expectedDate)) {
                    continue;
                }

                int overdueDays = (int) ChronoUnit.DAYS.between(expectedDate, today);

                // Tìm người nhận nhắc việc: Người được phân công phụ trách lô (LotAssignment) hoặc Người ghi sự kiện (VT-03)
                List<User> recipients = findRecipientsForLot(lot);
                if (recipients.isEmpty()) {
                    log.warn("Không tìm thấy người được phân công hoặc tạo lô {} để gửi nhắc việc.", lot.getId());
                    continue;
                }

                for (User recipient : recipients) {
                    // Chống tạo nhắc trùng trong cùng ngày cho cùng một mốc và người dùng (TC-04)
                    boolean alreadyCreatedToday = milestoneReminderRepository
                            .existsByProductionLot_IdAndMilestone_IdAndUser_UserIdAndReminderDate(
                                    lot.getId(), milestone.getId(), recipient.getUserId(), today);
                    if (alreadyCreatedToday) {
                        log.debug("Đã có nhắc việc cho lô {} mốc {} user {} trong ngày hôm nay. Bỏ qua.",
                                lot.getId(), milestone.getId(), recipient.getUserId());
                        continue;
                    }

                    // Tạo thông báo tái sử dụng cơ chế NCL-08-CN-005
                    Notification notification = new Notification();
                    notification.setUser(recipient);
                    notification.setType(NotificationType.TASK);
                    notification.setTitle("Nhắc việc: Mốc canh tác quá hạn");
                    notification.setContent(String.format("Lô %s thiếu mốc %s quá hạn %d ngày.",
                            lot.getName(), milestone.getName(), overdueDays));
                    notification.setIsRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    Notification savedNotification = notificationRepository.save(notification);

                    // Tạo bản ghi nhắc việc
                    MilestoneReminder reminder = MilestoneReminder.builder()
                            .productionLot(lot)
                            .milestone(milestone)
                            .user(recipient)
                            .notification(savedNotification)
                            .overdueDays(overdueDays)
                            .reminderDate(today)
                            .status(MilestoneReminderStatus.OPEN)
                            .build();

                    milestoneReminderRepository.save(reminder);
                    remindersCreatedCount++;
                }
            }
        }

        String message = String.format("Đã quét %d lô sản xuất, tạo mới %d nhắc việc quá hạn.",
                scannedLotsCount, remindersCreatedCount);
        log.info("⏰ Hoàn thành quét: {}", message);

        return MilestoneScanResult.builder()
                .scannedLotsCount(scannedLotsCount)
                .remindersCreatedCount(remindersCreatedCount)
                .message(message)
                .build();
    }

    private List<User> findRecipientsForLot(ProductionLot lot) {
        List<LotAssignment> assignments = lotAssignmentRepository.findByProductionLot_IdAndActiveTrue(lot.getId());
        List<User> recipients = new ArrayList<>();
        Set<UUID> addedUserIds = new HashSet<>();

        if (!assignments.isEmpty()) {
            for (LotAssignment assignment : assignments) {
                if (assignment.getUser() != null && addedUserIds.add(assignment.getUser().getUserId())) {
                    recipients.add(assignment.getUser());
                }
            }
        }

        // Nếu lô chưa có phân công cụ thể: tìm tất cả Người ghi sự kiện (VT-03) của tổ chức để nhắc
        if (recipients.isEmpty() && lot.getOrganization() != null) {
            List<OrganizationUser> orgUsers = organizationUserRepository.findByOrganization_OrganizationIdAndStatus(
                    lot.getOrganization().getOrganizationId(), OrganizationUserStatus.ACTIVE);
            for (OrganizationUser ou : orgUsers) {
                if (ou.getUser() != null && ou.getRole() != null
                        && RoleCode.EVENT_RECORDER.equals(ou.getRole().getCode())
                        && addedUserIds.add(ou.getUser().getUserId())) {
                    recipients.add(ou.getUser());
                }
            }
        }

        // Thêm người tạo lô (hoặc quản lý HTX) để cùng theo dõi
        if (lot.getCreatedBy() != null && addedUserIds.add(lot.getCreatedBy().getUserId())) {
            recipients.add(lot.getCreatedBy());
        }

        // Fallback: nếu vẫn rỗng, lấy bất kỳ người dùng active nào của tổ chức
        if (recipients.isEmpty() && lot.getOrganization() != null) {
            List<OrganizationUser> orgUsers = organizationUserRepository.findByOrganization_OrganizationIdAndStatus(
                    lot.getOrganization().getOrganizationId(), OrganizationUserStatus.ACTIVE);
            for (OrganizationUser ou : orgUsers) {
                if (ou.getUser() != null && addedUserIds.add(ou.getUser().getUserId())) {
                    recipients.add(ou.getUser());
                    break;
                }
            }
        }

        return recipients;
    }

    @Override
    public void completeRemindersForLotAndActivity(UUID lotId, FarmActivityType activityType) {
        if (lotId == null || activityType == null) {
            return;
        }

        List<MilestoneReminder> openReminders = milestoneReminderRepository.findByProductionLot_IdAndStatus(
                lotId, MilestoneReminderStatus.OPEN);

        LocalDateTime now = LocalDateTime.now();
        for (MilestoneReminder reminder : openReminders) {
            CultivationMilestone milestone = reminder.getMilestone();
            if (milestone != null && activityType.name().equalsIgnoreCase(milestone.getActivityType())) {
                reminder.setStatus(MilestoneReminderStatus.COMPLETED);
                reminder.setCompletedAt(now);

                // Đánh dấu đã đọc trên thông báo nếu có
                if (reminder.getNotification() != null) {
                    reminder.getNotification().setIsRead(true);
                    notificationRepository.save(reminder.getNotification());
                }

                milestoneReminderRepository.save(reminder);
                log.info("✅ Tự động đóng nhắc việc {} cho lô {} mốc {}",
                        reminder.getId(), lotId, milestone.getName());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MilestoneReminderResponse> getReminders(
            MilestoneReminderStatus status,
            UUID lotId,
            Pageable pageable,
            CustomUserDetails currentUser) {

        Page<MilestoneReminder> page;
        String roleCode = currentUser != null ? currentUser.getRoleCode() : "";

        if (lotId != null) {
            if (status != null) {
                page = milestoneReminderRepository.findByProductionLot_IdAndStatus(lotId, status, pageable);
            } else {
                page = milestoneReminderRepository.findByProductionLot_Id(lotId, pageable);
            }
        } else if (RoleCode.EVENT_RECORDER.equals(roleCode)) {
            // VT-03: xem nhắc việc của chính mình hoặc của các lô thuộc tổ chức mình
            UUID orgId = currentUser.getOrganizationId();
            if (status != null) {
                page = milestoneReminderRepository.findRemindersForUserOrOrganizationAndStatus(
                        currentUser.getUserId(), orgId, status, pageable);
            } else {
                page = milestoneReminderRepository.findRemindersForUserOrOrganization(
                        currentUser.getUserId(), orgId, pageable);
            }
        } else if (RoleCode.ORG_MANAGER.equals(roleCode)) {
            // VT-02: xem nhắc việc của tổ chức mình
            UUID orgId = currentUser.getOrganizationId();
            if (status != null) {
                page = milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatus(orgId, status, pageable);
            } else {
                page = milestoneReminderRepository.findByProductionLot_Organization_OrganizationId(orgId, pageable);
            }
        } else {
            // VT-01 hoặc quản trị: xem toàn bộ
            if (status != null) {
                page = milestoneReminderRepository.findByStatus(status, pageable);
            } else {
                page = milestoneReminderRepository.findAll(pageable);
            }
        }

        List<MilestoneReminderResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<MilestoneReminderResponse>builder()
                .items(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneReminderResponse> getMyActiveReminders(CustomUserDetails currentUser) {
        if (currentUser == null) {
            return List.of();
        }

        String roleCode = currentUser.getRoleCode();
        List<MilestoneReminder> list;

        if (RoleCode.ORG_MANAGER.equals(roleCode)) {
            UUID orgId = currentUser.getOrganizationId();
            list = milestoneReminderRepository.findByProductionLot_Organization_OrganizationIdAndStatusOrderByOverdueDaysDesc(
                    orgId, MilestoneReminderStatus.OPEN);
        } else if (RoleCode.EVENT_RECORDER.equals(roleCode)) {
            // VT-03: xem nhắc việc của chính mình hoặc của các lô thuộc tổ chức mình
            UUID orgId = currentUser.getOrganizationId();
            list = milestoneReminderRepository.findActiveRemindersForUserOrOrganization(
                    currentUser.getUserId(), orgId, MilestoneReminderStatus.OPEN);
        } else {
            // VT-01 và các vai trò khác
            list = milestoneReminderRepository.findByUser_UserIdAndStatusOrderByOverdueDaysDesc(
                    currentUser.getUserId(), MilestoneReminderStatus.OPEN);
        }

        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    private MilestoneReminderResponse toResponse(MilestoneReminder reminder) {
        LocalDate expectedDate = null;
        if (reminder.getProductionLot() != null && reminder.getProductionLot().getPlantingDate() != null
                && reminder.getMilestone() != null && reminder.getMilestone().getExpectedDaysFromPlanting() != null) {
            expectedDate = reminder.getProductionLot().getPlantingDate().plusDays(
                    reminder.getMilestone().getExpectedDaysFromPlanting());
        }

        return MilestoneReminderResponse.builder()
                .id(reminder.getId())
                .lotId(reminder.getProductionLot() != null ? reminder.getProductionLot().getId() : null)
                .lotName(reminder.getProductionLot() != null ? reminder.getProductionLot().getName() : null)
                .milestoneId(reminder.getMilestone() != null ? reminder.getMilestone().getId() : null)
                .milestoneName(reminder.getMilestone() != null ? reminder.getMilestone().getName() : null)
                .activityType(reminder.getMilestone() != null ? reminder.getMilestone().getActivityType() : null)
                .overdueDays(reminder.getOverdueDays())
                .expectedDate(expectedDate)
                .status(reminder.getStatus())
                .reminderDate(reminder.getReminderDate())
                .completedAt(reminder.getCompletedAt())
                .createdAt(reminder.getCreatedAt())
                .build();
    }
}
