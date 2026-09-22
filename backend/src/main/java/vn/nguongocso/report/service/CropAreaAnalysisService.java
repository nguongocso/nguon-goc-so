package vn.nguongocso.report.service;

import java.util.List;
import java.util.UUID;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.dto.response.SeasonYieldComparisonResponse;

/** Service phân tích diện tích canh tác. */
public interface CropAreaAnalysisService {

    /**
     * Lấy báo cáo phân tích diện tích canh tác.
     */
    CropAreaAnalysisResponse getAnalysis(
            Integer year,
            UUID farmAreaId,
            UUID productCategoryId,
            UUID organizationId,
            List<UUID> unitIds,
            CustomUserDetails currentUser,
            String ipAddress);

    /**
     * So sánh sản lượng giữa các mùa vụ.
     */
    SeasonYieldComparisonResponse compareSeasonYield(
            List<Integer> years,
            UUID farmAreaId,
            UUID productCategoryId,
            UUID organizationId,
            List<UUID> unitIds,
            CustomUserDetails currentUser,
            String ipAddress);
}
