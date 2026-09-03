package vn.nguongocso.certification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.request.CultivationMilestoneCatalogRequest;
import vn.nguongocso.certification.dto.response.CultivationMilestoneCatalogResponse;

/**
 * Service for managing the cultivation milestone catalog.
 * Story: NCL-09-CN-011
 */
public interface CultivationMilestoneCatalogService {

    Page<CultivationMilestoneCatalogResponse> searchMilestones(
            String keyword, String status, String activityType, Pageable pageable, CustomUserDetails currentUser);

    CultivationMilestoneCatalogResponse getMilestone(Long id, CustomUserDetails currentUser);

    CultivationMilestoneCatalogResponse createMilestone(
            CultivationMilestoneCatalogRequest request, CustomUserDetails currentUser);

    CultivationMilestoneCatalogResponse updateMilestone(
            Long id, CultivationMilestoneCatalogRequest request, CustomUserDetails currentUser);

    void disableMilestone(Long id, CustomUserDetails currentUser);

    CultivationMilestoneCatalogResponse enableMilestone(Long id, CustomUserDetails currentUser);

    void deleteMilestone(Long id, CustomUserDetails currentUser);
}
