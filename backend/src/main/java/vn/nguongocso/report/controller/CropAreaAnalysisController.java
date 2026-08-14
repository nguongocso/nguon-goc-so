package vn.nguongocso.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.dto.response.SeasonYieldComparisonResponse;
import vn.nguongocso.report.service.CropAreaAnalysisService;

import java.util.List;
import java.util.UUID;

/**
 * Controller phân tích diện tích canh tác.
 *
 * @author Triệu Văn Đại
 */
@RestController
@RequestMapping("/api/v1/reports/crop-area-analysis")
@RequiredArgsConstructor
public class CropAreaAnalysisController {
    private final CropAreaAnalysisService cropAreaAnalysisService;

    /**
     * Lấy phân tích diện tích canh tác theo năm, khu vực canh tác, loại sản phẩm và
     * tổ chức.
     */
    @GetMapping
    public ResponseEntity<ApiResult<CropAreaAnalysisResponse>> getAnalysis(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) UUID farmAreaId,
            @RequestParam(required = false) UUID productCategoryId,
            @RequestParam(required = false) UUID organizationId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        CropAreaAnalysisResponse response = cropAreaAnalysisService.getAnalysis(
                year,
                farmAreaId,
                productCategoryId,
                organizationId,
                currentUser,
                ipAddress);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * So sánh sản lượng giữa nhiều mùa vụ.
     */
    @GetMapping("/season-yield-comparison")
    public ResponseEntity<ApiResult<SeasonYieldComparisonResponse>> compareSeasonYield(
            @RequestParam List<Integer> years,
            @RequestParam(required = false) UUID farmAreaId,
            @RequestParam(required = false) UUID productCategoryId,
            @RequestParam(required = false) UUID organizationId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request) {

        String ipAddress = getClientIp(request);

        SeasonYieldComparisonResponse response = cropAreaAnalysisService.compareSeasonYield(
                years,
                farmAreaId,
                productCategoryId,
                organizationId,
                currentUser,
                ipAddress);

        return ResponseEntity.ok(ApiResult.success(response));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}