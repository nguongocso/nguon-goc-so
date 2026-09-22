package vn.nguongocso.report.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;
import vn.nguongocso.report.dto.response.CropAreaAnalysisResponse;
import vn.nguongocso.report.dto.response.SeasonYieldComparisonResponse;
import vn.nguongocso.report.dto.response.SeasonYieldItemResponse;
import vn.nguongocso.report.service.CropAreaAnalysisService;
import vn.nguongocso.report.service.ReportAccessLogService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Triển khai dịch vụ phân tích diện tích canh tác. */
@Service
@RequiredArgsConstructor
public class CropAreaAnalysisServiceImpl implements CropAreaAnalysisService {
    private final ProductionLotRepository productionLotRepository;
    private final ReportAccessLogService reportAccessLogService;
    private final AreaScopeService areaScopeService;

    /** Lấy dữ liệu phân tích diện tích canh tác theo năm, vùng trồng, loại sản phẩm và tổ chức. */
    @Override
    @Transactional(readOnly = true)
    public CropAreaAnalysisResponse getAnalysis(
        Integer year,
        UUID farmAreaId,
        UUID productCategoryId,
        UUID organizationId,
        List<UUID> unitIds,
        CustomUserDetails currentUser,
        String ipAddress) {

        String role = currentUser.getRoleCode();
        boolean isAllowed = "VT-01".equals(role) || "VT-05".equals(role);
        UUID targetOrg = (organizationId != null) ? organizationId : currentUser.getOrganizationId();

        if (!isAllowed) {
            reportAccessLogService.logAccess(
                currentUser.getUserId(),
                currentUser.getOrganizationId(),
                targetOrg,
                "CROP_AREA_ANALYSIS",
                false,
                ipAddress);
            throw new AccessDeniedException("Bạn không có quyền truy cập báo cáo phân tích ngành.");
        }

        reportAccessLogService.logAccess(
            currentUser.getUserId(),
            currentUser.getOrganizationId(),
            targetOrg,
            "CROP_AREA_ANALYSIS",
            true,
            ipAddress);

        AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(
            currentUser,
            unitIds);

        if (scope.isEmptyScope()) {
            return buildEmptyAnalysis(AreaScopeService.UNASSIGNED_MESSAGE);
        }

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(targetYear - 1, 11, 1);
        LocalDate endDate = LocalDate.of(targetYear, 10, 31);

        List<ProductionLot> lots;
        if (scope.isAll()) {
            lots = productionLotRepository.findLotsForAnalysis(
                startDate,
                endDate,
                farmAreaId,
                productCategoryId,
                organizationId);
        } else {
            lots = productionLotRepository.findLotsForAnalysisInOrganizations(
                startDate,
                endDate,
                farmAreaId,
                productCategoryId,
                scope.getOrganizationIds());
            if (organizationId != null) {
                lots = lots.stream()
                    .filter(lot -> organizationId.equals(lot.getOrganization().getOrganizationId()))
                    .toList();
            }
        }

        List<ProductionLot> validLots = lots.stream()
            .filter(lot -> lot.getFarmArea() != null)
            .collect(Collectors.toList());

        if (validLots.isEmpty()) {
            return buildEmptyAnalysis(null);
        }

        long totalLots = validLots.size();
        double totalExpectedYield = validLots.stream().mapToDouble(ProductionLot::getExpectedQuantity).sum();
        double totalActualYield = validLots.stream()
            .mapToDouble(lot -> lot.getActualQuantity() != null ? lot.getActualQuantity() : 0.0)
            .sum();

        double totalArea = validLots.stream()
            .map(ProductionLot::getFarmArea)
            .distinct()
            .mapToDouble(area -> area.getArea() != null ? area.getArea().doubleValue() : 0.0)
            .sum();

        CropAreaAnalysisResponse.SummaryStats summary = CropAreaAnalysisResponse.SummaryStats.builder()
            .totalLots(totalLots)
            .totalExpectedYield(totalExpectedYield)
            .totalActualYield(totalActualYield)
            .totalArea(totalArea)
            .build();

        Map<FarmArea, List<ProductionLot>> lotsByArea = validLots.stream()
            .collect(Collectors.groupingBy(ProductionLot::getFarmArea));

        List<CropAreaAnalysisResponse.AreaAnalysisStats> byArea = new ArrayList<>();
        for (Map.Entry<FarmArea, List<ProductionLot>> entry : lotsByArea.entrySet()) {
            FarmArea area = entry.getKey();
            List<ProductionLot> areaLots = entry.getValue();

            double areaExpected = areaLots.stream().mapToDouble(ProductionLot::getExpectedQuantity).sum();
            double areaActual = areaLots.stream()
                .mapToDouble(lot -> lot.getActualQuantity() != null ? lot.getActualQuantity() : 0.0)
                .sum();

            Map<String, List<ProductionLot>> areaLotsBySeason = areaLots.stream()
                .collect(Collectors.groupingBy(lot -> getSeasonCode(lot.getPlantingDate())));

            List<CropAreaAnalysisResponse.AreaSeasonStats> areaSeasons = new ArrayList<>();
            for (Map.Entry<String, List<ProductionLot>> seasonEntry : areaLotsBySeason.entrySet()) {
                String seasonCode = seasonEntry.getKey();
                List<ProductionLot> seasonLots = seasonEntry.getValue();

                double sExpected = seasonLots.stream().mapToDouble(ProductionLot::getExpectedQuantity).sum();
                double sActual = seasonLots.stream()
                    .mapToDouble(lot -> lot.getActualQuantity() != null ? lot.getActualQuantity() : 0.0)
                    .sum();

                areaSeasons.add(CropAreaAnalysisResponse.AreaSeasonStats.builder()
                    .seasonCode(seasonCode)
                    .seasonName(getSeasonName(seasonCode))
                    .year(targetYear)
                    .lotCount(seasonLots.size())
                    .expectedYield(sExpected)
                    .actualYield(sActual)
                    .build());
            }

            byArea.add(CropAreaAnalysisResponse.AreaAnalysisStats.builder()
                .farmAreaId(area.getId())
                .farmAreaName(area.getName())
                .areaSize(area.getArea() != null ? area.getArea().doubleValue() : 0.0)
                .organizationName(area.getOrganization() != null ? area.getOrganization().getName() : "N/A")
                .totalLots(areaLots.size())
                .expectedYield(areaExpected)
                .actualYield(areaActual)
                .seasons(areaSeasons)
                .build());
        }

        Map<String, List<ProductionLot>> lotsBySeason = validLots.stream()
            .collect(Collectors.groupingBy(lot -> getSeasonCode(lot.getPlantingDate())));

        List<CropAreaAnalysisResponse.SeasonAnalysisStats> bySeason = new ArrayList<>();
        for (Map.Entry<String, List<ProductionLot>> entry : lotsBySeason.entrySet()) {
            String seasonCode = entry.getKey();
            List<ProductionLot> seasonLots = entry.getValue();

            double seasonExpected = seasonLots.stream().mapToDouble(ProductionLot::getExpectedQuantity).sum();
            double seasonActual = seasonLots.stream()
                .mapToDouble(lot -> lot.getActualQuantity() != null ? lot.getActualQuantity() : 0.0)
                .sum();

            Map<FarmArea, List<ProductionLot>> seasonLotsByArea = seasonLots.stream()
                .collect(Collectors.groupingBy(ProductionLot::getFarmArea));

            List<CropAreaAnalysisResponse.SeasonAreaStats> seasonAreas = new ArrayList<>();
            for (Map.Entry<FarmArea, List<ProductionLot>> areaEntry : seasonLotsByArea.entrySet()) {
                FarmArea area = areaEntry.getKey();
                List<ProductionLot> areaLots = areaEntry.getValue();

                double aExpected = areaLots.stream().mapToDouble(ProductionLot::getExpectedQuantity).sum();
                double aActual = areaLots.stream()
                    .mapToDouble(lot -> lot.getActualQuantity() != null ? lot.getActualQuantity() : 0.0)
                    .sum();

                seasonAreas.add(CropAreaAnalysisResponse.SeasonAreaStats.builder()
                    .farmAreaId(area.getId())
                    .farmAreaName(area.getName())
                    .lotCount(areaLots.size())
                    .expectedYield(aExpected)
                    .actualYield(aActual)
                    .build());
            }

            bySeason.add(CropAreaAnalysisResponse.SeasonAnalysisStats.builder()
                .seasonCode(seasonCode)
                .seasonName(getSeasonName(seasonCode))
                .year(targetYear)
                .totalLots(seasonLots.size())
                .expectedYield(seasonExpected)
                .actualYield(seasonActual)
                .areas(seasonAreas)
                .build());
        }

        return CropAreaAnalysisResponse.builder()
            .summary(summary)
            .byArea(byArea)
            .bySeason(bySeason)
            .build();
    }

    /** Xác định mã mùa vụ từ ngày gieo trồng. */
    private String getSeasonCode(LocalDate plantingDate) {
        if (plantingDate == null) {
            return "UNKNOWN";
        }
        int month = plantingDate.getMonthValue();
        if (month >= 11 || month <= 4) {
            return "DONG_XUAN";
        } else if (month >= 5 && month <= 8) {
            return "HE_THU";
        } else {
            return "THU_DONG";
        }
    }

    /** Tạo cấu trúc rỗng chuẩn hoá kèm thông báo. */
    private CropAreaAnalysisResponse buildEmptyAnalysis(String message) {
        return CropAreaAnalysisResponse.builder()
            .summary(CropAreaAnalysisResponse.SummaryStats.builder()
                .totalLots(0L)
                .totalExpectedYield(0.0)
                .totalActualYield(0.0)
                .totalArea(0.0)
                .build())
            .byArea(Collections.emptyList())
            .bySeason(Collections.emptyList())
            .message(message)
            .build();
    }

    /** Lấy tên mùa vụ hiển thị theo mã. */
    private String getSeasonName(String code) {
        switch (code) {
        case "DONG_XUAN":
            return "Vụ Đông Xuân";
        case "HE_THU":
            return "Vụ Hè Thu";
        case "THU_DONG":
            return "Vụ Thu Đông";
        default:
            return "Không xác định";
        }
    }

    /** So sánh sản lượng mùa vụ giữa các năm. */
    @Override
    @Transactional(readOnly = true)
    public SeasonYieldComparisonResponse compareSeasonYield(
        List<Integer> years,
        UUID farmAreaId,
        UUID productCategoryId,
        UUID organizationId,
        List<UUID> unitIds,
        CustomUserDetails currentUser,
        String ipAddress) {

        UUID targetOrg = organizationId != null
            ? organizationId
            : currentUser.getOrganizationId();

        String role = currentUser.getRoleCode();
        boolean isAllowed = "VT-01".equals(role) || "VT-05".equals(role);

        if (!isAllowed) {
            reportAccessLogService.logAccess(
                currentUser.getUserId(),
                currentUser.getOrganizationId(),
                targetOrg,
                "SEASON_YIELD_COMPARISON",
                false,
                ipAddress);

            throw new AccessDeniedException("Bạn không có quyền xem báo cáo so sánh mùa vụ.");
        }

        reportAccessLogService.logAccess(
            currentUser.getUserId(),
            currentUser.getOrganizationId(),
            targetOrg,
            "SEASON_YIELD_COMPARISON",
            true,
            ipAddress);

        AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(
            currentUser,
            unitIds);

        if (scope.isEmptyScope()) {
            return SeasonYieldComparisonResponse.builder()
                .hasData(false)
                .message(AreaScopeService.UNASSIGNED_MESSAGE)
                .baselineYear(null)
                .baselineSeasonCode(null)
                .baselineSeasonName(null)
                .seasons(Collections.emptyList())
                .build();
        }

        List<ProductionLot> lots;
        if (scope.isAll()) {
            lots = productionLotRepository.findLotsForSeasonYieldComparison(
                years,
                farmAreaId,
                productCategoryId,
                organizationId);
        } else {
            lots = productionLotRepository.findLotsForSeasonYieldComparisonInOrganizations(
                years,
                farmAreaId,
                productCategoryId,
                scope.getOrganizationIds());
            if (organizationId != null) {
                lots = lots.stream()
                    .filter(lot -> organizationId.equals(lot.getOrganization().getOrganizationId()))
                    .toList();
            }
        }

        if (lots.isEmpty()) {
            return SeasonYieldComparisonResponse.builder()
                .hasData(false)
                .message("Không có dữ liệu để so sánh.")
                .baselineYear(null)
                .baselineSeasonCode(null)
                .baselineSeasonName(null)
                .seasons(Collections.emptyList())
                .build();
        }

        Map<String, List<ProductionLot>> grouped = lots.stream()
            .collect(Collectors.groupingBy(lot -> lot.getPlantingDate().getYear() + "_" + getSeasonCode(lot.getPlantingDate())));

        List<SeasonYieldItemResponse> seasonItems = new ArrayList<>();

        for (Map.Entry<String, List<ProductionLot>> entry : grouped.entrySet()) {
            List<ProductionLot> seasonLots = entry.getValue();
            ProductionLot sample = seasonLots.get(0);

            int year = sample.getPlantingDate().getYear();
            String seasonCode = getSeasonCode(sample.getPlantingDate());

            long lotCount = seasonLots.size();

            double totalQuantity = seasonLots.stream()
                .mapToDouble(lot -> lot.getActualQuantity() == null ? 0D : lot.getActualQuantity())
                .sum();

            seasonItems.add(
                SeasonYieldItemResponse.builder()
                    .year(year)
                    .seasonCode(seasonCode)
                    .seasonName(getSeasonName(seasonCode))
                    .lotCount(lotCount)
                    .totalQuantity(totalQuantity)
                    .build());
        }

        Map<String, Integer> seasonOrder = Map.of(
            "DONG_XUAN", 1,
            "HE_THU", 2,
            "THU_DONG", 3);

        seasonItems.sort(
            Comparator.comparing(SeasonYieldItemResponse::getYear)
                .thenComparing(item -> seasonOrder.getOrDefault(item.getSeasonCode(), 99)));

        if (seasonItems.size() < 2) {
            return SeasonYieldComparisonResponse.builder()
                .hasData(false)
                .message("Không đủ dữ liệu để so sánh.")
                .baselineYear(null)
                .baselineSeasonCode(null)
                .baselineSeasonName(null)
                .seasons(Collections.emptyList())
                .build();
        }

        SeasonYieldItemResponse baseline = seasonItems.get(0);
        double baselineQuantity = baseline.getTotalQuantity();

        List<SeasonYieldItemResponse> result = new ArrayList<>();

        for (SeasonYieldItemResponse item : seasonItems) {
            double delta = item.getTotalQuantity() - baselineQuantity;

            Double deltaPercent = null;
            if (baselineQuantity != 0) {
                deltaPercent = delta * 100 / baselineQuantity;
            }

            result.add(
                SeasonYieldItemResponse.builder()
                    .year(item.getYear())
                    .seasonCode(item.getSeasonCode())
                    .seasonName(item.getSeasonName())
                    .lotCount(item.getLotCount())
                    .totalQuantity(item.getTotalQuantity())
                    .delta(delta)
                    .deltaPercent(deltaPercent)
                    .build());
        }

        return SeasonYieldComparisonResponse.builder()
            .hasData(true)
            .message(null)
            .baselineYear(baseline.getYear())
            .baselineSeasonCode(baseline.getSeasonCode())
            .baselineSeasonName(baseline.getSeasonName())
            .seasons(result)
            .build();
    }
}