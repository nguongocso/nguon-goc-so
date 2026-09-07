package vn.nguongocso.farm.service;

import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.farm.dto.response.MilestoneReminderResponse;
import vn.nguongocso.farm.dto.response.MilestoneScanResult;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.MilestoneReminderStatus;

import java.util.List;
import java.util.UUID;

/**
 * Dịch vụ quản lý nhắc lịch ghi nhật ký theo mốc canh tác bắt buộc (NCL-03-CN-007).
 */
public interface MilestoneReminderService {

    /**
     * Quét các lô đang canh tác trên toàn hệ thống và tạo nhắc việc cho mốc quá hạn.
     *
     * @return kết quả quét
     */
    MilestoneScanResult scanOverdueMilestones();

    /**
     * Quét các lô đang canh tác của một tổ chức cụ thể.
     *
     * @param organizationId ID tổ chức
     * @return kết quả quét
     */
    MilestoneScanResult scanOverdueMilestonesForOrganization(UUID organizationId);

    /**
     * Tự động đóng các nhắc việc đang mở cho mốc khi người ghi nhập nhật ký canh tác (TC-02).
     *
     * @param lotId        ID của lô sản xuất
     * @param activityType loại hoạt động canh tác vừa ghi
     */
    void completeRemindersForLotAndActivity(UUID lotId, FarmActivityType activityType);

    /**
     * Lấy danh sách nhắc việc có phân trang và lọc theo quyền của người dùng.
     *
     * @param status      trạng thái lọc (tùy chọn)
     * @param lotId       ID lô sản xuất (tùy chọn)
     * @param pageable    thông tin phân trang
     * @param currentUser thông tin người dùng hiện tại
     * @return danh sách phân trang nhắc việc
     */
    PageResponse<MilestoneReminderResponse> getReminders(
            MilestoneReminderStatus status,
            UUID lotId,
            Pageable pageable,
            CustomUserDetails currentUser);

    /**
     * Lấy danh sách nhắc việc đang mở (OPEN) của người dùng hiện tại (dành cho mobile & dashboard).
     *
     * @param currentUser thông tin người dùng hiện tại
     * @return danh sách nhắc việc đang mở
     */
    List<MilestoneReminderResponse> getMyActiveReminders(CustomUserDetails currentUser);
}
