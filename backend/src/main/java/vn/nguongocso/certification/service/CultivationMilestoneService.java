package vn.nguongocso.certification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneResponse;

import java.util.UUID;

/**
 * Service quản lý mốc canh tác (bảng hợp nhất).
 * Story: NCL-09-CN-011
 */
public interface CultivationMilestoneService {

    Page<CultivationMilestoneResponse> searchMilestones(
            String keyword, String activityType, UUID categoryId, UUID standardId,
            boolean globalOnly, Pageable pageable, CustomUserDetails currentUser);

    CultivationMilestoneResponse getMilestone(Long id, CustomUserDetails currentUser);

    CultivationMilestoneResponse createMilestone(
            CultivationMilestoneRequest request, CustomUserDetails currentUser);

    CultivationMilestoneResponse updateMilestone(
            Long id, CultivationMilestoneRequest request, CustomUserDetails currentUser);
}
