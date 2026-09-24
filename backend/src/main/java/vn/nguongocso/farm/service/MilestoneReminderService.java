package vn.nguongocso.farm.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.response.MilestoneReminderResponse;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;

/**
 * Nghiệp vụ nhắc mốc canh tác.
*/
public interface MilestoneReminderService {
    /** Quét mốc quá hạn. */
    MilestoneScanResult scanOverdueMilestones();

    /** Quét mốc quá hạn của tổ chức. */
    MilestoneScanResult scanOverdueMilestonesForOrganization(UUID organizationId);

    /** Đóng nhắc việc theo hoạt động của lô. */
    void completeRemindersForLotAndActivity(UUID lotId, FarmActivityType activityType);

    /** Đóng nhắc việc theo mốc canh tác. */
    void completeRemindersForLotAndMilestone(UUID lotId, Long milestoneId);

    /** Lấy danh sách nhắc việc. */
    PageResponse<MilestoneReminderResponse> getReminders(
            MilestoneReminderStatus status,
            UUID lotId,
            Pageable pageable,
            CustomUserDetails currentUser);

    /** Lấy nhắc việc đang mở của người dùng. */
    List<MilestoneReminderResponse> getMyActiveReminders(CustomUserDetails currentUser);
}
