package vn.nguongocso.certification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneResponse;
import vn.nguongocso.certification.dto.response.MilestoneEligibilityResponse;

import java.util.UUID;

/**
 * Service quản lý mốc canh tác (bảng hợp nhất).
 * Story: NCL-09-CN-011
 */
public interface CultivationMilestoneService {
        /**
         * Tìm kiếm mốc canh tác theo từ khóa, loại hoạt động, loại sản phẩm và tiêu chuẩn.
         */
        Page<CultivationMilestoneResponse> searchMilestones(
                        String keyword, String activityType, UUID categoryId, UUID standardId,
                        boolean globalOnly, Pageable pageable, CustomUserDetails currentUser);

        /**
         * Lấy chi tiết mốc canh tác theo ID.
         */
        CultivationMilestoneResponse getMilestone(
                        Long id, CustomUserDetails currentUser);

        /**
         * Tạo mới mốc canh tác.
         */
        CultivationMilestoneResponse createMilestone(
                        CultivationMilestoneRequest request, CustomUserDetails currentUser);

        /**
         * Cập nhật mốc canh tác theo ID.
         */
        CultivationMilestoneResponse updateMilestone(
                        Long id, CultivationMilestoneRequest request, CustomUserDetails currentUser);

        /**
         * Kiểm tra lô sản xuất đã đủ mốc canh tác bắt buộc để ghi sự kiện đóng gói chưa (NCL-09-CN-011).
         */
        MilestoneEligibilityResponse getPackagingEligibility(
                        UUID productionLotId, CustomUserDetails currentUser);
}
