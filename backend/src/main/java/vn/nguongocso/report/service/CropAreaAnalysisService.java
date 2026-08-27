package vn.nguongocso.report.service;

import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.dto.response.SeasonYieldComparisonResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service phân tích diện tích canh tác.
 *
 * @author Triệu Văn Đại
 */
public interface CropAreaAnalysisService {
        /**
         * Lấy báo cáo phân tích diện tích canh tác.
         *
         * @param unitIds địa bàn lọc theo mapping tổ chức (NCL-743; có thể null)
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
         *
         * @param unitIds địa bàn lọc theo mapping tổ chức (NCL-743; có thể null)
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
