package vn.nguongocso.ai.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.ai.dto.query.CertificationStatusDto;
import vn.nguongocso.ai.dto.query.OrganizationAnalyticsDataDto;
import vn.nguongocso.ai.dto.query.ProductionLotSummaryDto;
import vn.nguongocso.ai.dto.query.RecentAlertsSummaryDto;
import vn.nguongocso.ai.dto.query.ShipmentSummaryDto;
import vn.nguongocso.ai.service.AiDataQueryService;
import vn.nguongocso.alert.enums.AlertStatus;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.certification.entity.Certification;
import vn.nguongocso.certification.repository.CertificationRepository;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.trace.enums.ShipmentHandoverStatus;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.recall.enums.RecallCaseStatus;
import vn.nguongocso.trace.recall.repository.RecallCaseRepository;
import vn.nguongocso.trace.repository.ShipmentHandoverRepository;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Hiện thực dịch vụ trích xuất và tổng hợp số liệu nghiệp vụ nhanh cho AI (TASK-AI-05).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiDataQueryServiceImpl implements AiDataQueryService {

    private final ProductionLotRepository productionLotRepository;
    private final FarmAreaRepository farmAreaRepository;
    private final CertificationRepository certificationRepository;
    private final AlertRepository alertRepository;
    private final RecallCaseRepository recallCaseRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentHandoverRepository shipmentHandoverRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductionLotSummaryDto getProductionLotSummary(UUID organizationId) {
        if (organizationId == null) {
            return ProductionLotSummaryDto.builder()
                    .totalLotsCount(0)
                    .activeLotsCount(0)
                    .harvestedLotsCount(0)
                    .packagedLotsCount(0)
                    .totalAreaHectares(0.0)
                    .upcomingHarvestLotNames(List.of())
                    .recentLotDetails(List.of())
                    .build();
        }

        long activeLots = 0;
        long harvestedLots = 0;
        long totalLots = 0;
        long packagedLots = 0;
        double totalArea = 0.0;

        List<Object[]> rows = productionLotRepository.getLotAggregateSummaryByOrgId(organizationId);
        if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            activeLots = row[0] != null ? ((Number) row[0]).longValue() : 0;
            harvestedLots = row[1] != null ? ((Number) row[1]).longValue() : 0;
            totalArea = row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 100.0) / 100.0 : 0.0;
            totalLots = row.length > 3 && row[3] != null ? ((Number) row[3]).longValue() : (activeLots + harvestedLots);
            packagedLots = row.length > 4 && row[4] != null ? ((Number) row[4]).longValue() : 0;
        }

        // Ưu tiên diện tích thực tế từ các vùng trồng đang hoạt động nếu có
        java.math.BigDecimal farmAreaSum = farmAreaRepository.sumAreaByOrganizationId(organizationId);
        if (farmAreaSum != null && farmAreaSum.doubleValue() > 0.0) {
            totalArea = Math.round(farmAreaSum.doubleValue() * 100.0) / 100.0;
        }

        List<String> upcomingLots = productionLotRepository.findUpcomingHarvestLotNames(
                organizationId, LocalDate.now(), PageRequest.of(0, 3));

        List<vn.nguongocso.farm.entity.ProductionLot> recentLots = productionLotRepository.findRecentLotsByOrgId(
                organizationId, PageRequest.of(0, 5));
        List<String> recentLotDetails = new ArrayList<>();
        if (recentLots != null) {
            for (vn.nguongocso.farm.entity.ProductionLot pl : recentLots) {
                String faName = pl.getFarmArea() != null ? pl.getFarmArea().getName() : "Chưa gắn vùng";
                java.math.BigDecimal faArea = pl.getFarmArea() != null ? pl.getFarmArea().getArea() : null;
                String catName = pl.getProductCategory() != null ? pl.getProductCategory().getName() : "Nông sản";
                String statusVi = formatLotStatusVietnamese(pl.getStatus());
                String detail = String.format("%s (Nông sản: %s, Vùng trồng: %s%s, Trạng thái: %s%s%s)",
                        pl.getName(),
                        catName,
                        faName,
                        faArea != null ? " - " + faArea + " ha" : "",
                        statusVi,
                        pl.getPlantingDate() != null ? ", Ngày xuống giống: " + pl.getPlantingDate() : "",
                        pl.getHarvestDate() != null ? ", Ngày thu hoạch: " + pl.getHarvestDate() : "");
                recentLotDetails.add(detail);
            }
        }

        return ProductionLotSummaryDto.builder()
                .totalLotsCount(totalLots)
                .activeLotsCount(activeLots)
                .harvestedLotsCount(harvestedLots)
                .packagedLotsCount(packagedLots)
                .totalAreaHectares(totalArea)
                .upcomingHarvestLotNames(upcomingLots != null ? upcomingLots : List.of())
                .recentLotDetails(recentLotDetails)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificationStatusDto> getCertificationStatus(UUID organizationId) {
        if (organizationId == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        List<Certification> certs = certificationRepository.findExpiringCertifications(organizationId, today, threshold);
        List<CertificationStatusDto> result = new ArrayList<>();

        if (certs != null) {
            for (Certification cert : certs) {
                long daysRemaining = cert.getExpiryDate() != null
                        ? Math.max(0, ChronoUnit.DAYS.between(today, cert.getExpiryDate()))
                        : 0;

                result.add(CertificationStatusDto.builder()
                        .code(cert.getCode())
                        .name(cert.getName())
                        .standardName(cert.getStandard() != null ? cert.getStandard().getName() : "Tiêu chuẩn")
                        .expiryDate(cert.getExpiryDate())
                        .daysRemaining(daysRemaining)
                        .build());
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RecentAlertsSummaryDto getRecentAlertsSummary(UUID organizationId) {
        if (organizationId == null) {
            return RecentAlertsSummaryDto.builder()
                    .pendingScanAnomalyCount(0)
                    .activeRecallCasesCount(0)
                    .build();
        }

        long anomalyCount = alertRepository.countByOrganizationOrganizationIdAndTypeAndStatus(
                organizationId, AlertType.SCAN_ANOMALY, AlertStatus.PENDING);
        long recallCount = recallCaseRepository.countByOrganizationIdAndStatus(
                organizationId, RecallCaseStatus.OPEN);

        return RecentAlertsSummaryDto.builder()
                .pendingScanAnomalyCount(anomalyCount)
                .activeRecallCasesCount(recallCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentSummaryDto getShipmentSummary(UUID organizationId) {
        if (organizationId == null) {
            return ShipmentSummaryDto.builder()
                    .inTransitShipmentsCount(0)
                    .pendingHandoverCount(0)
                    .build();
        }

        long inTransit = shipmentRepository.countByOrganization_OrganizationIdAndStatus(
                organizationId, ShipmentStatus.ACTIVATED);
        long pendingHandover = shipmentHandoverRepository.countByFromOrganizationOrganizationIdAndStatus(
                organizationId, ShipmentHandoverStatus.PENDING_CONFIRMATION);

        return ShipmentSummaryDto.builder()
                .inTransitShipmentsCount(inTransit)
                .pendingHandoverCount(pendingHandover)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationAnalyticsDataDto getFullOrganizationAnalytics(UUID organizationId) {
        if (organizationId == null) {
            return null;
        }

        String orgName = "Tổ chức";
        String orgCode = "";

        Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
        if (orgOpt.isPresent()) {
            Organization org = orgOpt.get();
            orgName = org.getName() != null ? org.getName() : "Tổ chức";
            orgCode = org.getCode() != null ? org.getCode() : "";
        }

        return OrganizationAnalyticsDataDto.builder()
                .organizationId(organizationId)
                .organizationName(orgName)
                .organizationCode(orgCode)
                .lotSummary(getProductionLotSummary(organizationId))
                .expiringCertifications(getCertificationStatus(organizationId))
                .alertsSummary(getRecentAlertsSummary(organizationId))
                .shipmentSummary(getShipmentSummary(organizationId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationAnalyticsDataDto getTerritoryAnalytics(Set<UUID> organizationIds, String territoryName) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return OrganizationAnalyticsDataDto.builder()
                    .organizationId(null)
                    .organizationName(territoryName != null ? territoryName : "Địa bàn quản lý")
                    .organizationCode("TERRITORY")
                    .lotSummary(ProductionLotSummaryDto.builder()
                            .activeLotsCount(0)
                            .harvestedLotsCount(0)
                            .totalAreaHectares(0.0)
                            .upcomingHarvestLotNames(List.of())
                            .build())
                    .expiringCertifications(List.of())
                    .alertsSummary(RecentAlertsSummaryDto.builder()
                            .pendingScanAnomalyCount(0)
                            .activeRecallCasesCount(0)
                            .build())
                    .shipmentSummary(ShipmentSummaryDto.builder()
                            .inTransitShipmentsCount(0)
                            .pendingHandoverCount(0)
                            .build())
                    .build();
        }

        // 1. Lot summary aggregate
        long activeLots = 0;
        long harvestedLots = 0;
        long totalLots = 0;
        long packagedLots = 0;
        double totalArea = 0.0;
        List<Object[]> lotRows = productionLotRepository.getLotAggregateSummaryByOrgIds(organizationIds);
        if (lotRows != null && !lotRows.isEmpty() && lotRows.get(0) != null) {
            Object[] row = lotRows.get(0);
            activeLots = row[0] != null ? ((Number) row[0]).longValue() : 0;
            harvestedLots = row[1] != null ? ((Number) row[1]).longValue() : 0;
            totalArea = row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 100.0) / 100.0 : 0.0;
            totalLots = row.length > 3 && row[3] != null ? ((Number) row[3]).longValue() : (activeLots + harvestedLots);
            packagedLots = row.length > 4 && row[4] != null ? ((Number) row[4]).longValue() : 0;
        }

        java.math.BigDecimal territoryFarmAreaSum = farmAreaRepository.sumAreaByOrganizationIds(organizationIds);
        if (territoryFarmAreaSum != null && territoryFarmAreaSum.doubleValue() > 0.0) {
            totalArea = Math.round(territoryFarmAreaSum.doubleValue() * 100.0) / 100.0;
        }

        List<vn.nguongocso.farm.entity.ProductionLot> recentTerritoryLots = productionLotRepository.findRecentLotsByOrgIds(
                organizationIds, PageRequest.of(0, 5));
        List<String> recentTerritoryLotDetails = new ArrayList<>();
        if (recentTerritoryLots != null) {
            for (vn.nguongocso.farm.entity.ProductionLot pl : recentTerritoryLots) {
                String faName = pl.getFarmArea() != null ? pl.getFarmArea().getName() : "Chưa gắn vùng";
                java.math.BigDecimal faArea = pl.getFarmArea() != null ? pl.getFarmArea().getArea() : null;
                String catName = pl.getProductCategory() != null ? pl.getProductCategory().getName() : "Nông sản";
                String orgName = pl.getOrganization() != null ? pl.getOrganization().getName() : "Tổ chức";
                String statusVi = formatLotStatusVietnamese(pl.getStatus());
                String detail = String.format("%s - Đơn vị: %s (Nông sản: %s, Vùng: %s%s, Trạng thái: %s)",
                        pl.getName(),
                        orgName,
                        catName,
                        faName,
                        faArea != null ? " - " + faArea + " ha" : "",
                        statusVi);
                recentTerritoryLotDetails.add(detail);
            }
        }

        // 2. Expiring certs
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);
        List<Certification> certs = certificationRepository.findExpiringCertificationsByOrgIds(
                organizationIds, today, threshold);
        List<CertificationStatusDto> certDtos = new ArrayList<>();
        if (certs != null) {
            for (Certification cert : certs) {
                long days = cert.getExpiryDate() != null
                        ? Math.max(0, ChronoUnit.DAYS.between(today, cert.getExpiryDate()))
                        : 0;
                certDtos.add(CertificationStatusDto.builder()
                        .code(cert.getCode())
                        .name(cert.getName())
                        .standardName(cert.getStandard() != null ? cert.getStandard().getName() : "Tiêu chuẩn")
                        .expiryDate(cert.getExpiryDate())
                        .daysRemaining(days)
                        .build());
            }
        }

        // 3. Alerts aggregate
        long anomalyCount = alertRepository.countByOrganizationOrganizationIdInAndTypeAndStatus(
                organizationIds, AlertType.SCAN_ANOMALY, AlertStatus.PENDING);
        long recallCount = recallCaseRepository.countByOrganizationIdInAndStatus(
                organizationIds, RecallCaseStatus.OPEN);

        // 4. Shipments aggregate
        long inTransit = shipmentRepository.countByOrganization_OrganizationIdInAndStatus(
                organizationIds, ShipmentStatus.ACTIVATED);
        long pendingHandover = shipmentHandoverRepository.countByFromOrganizationOrganizationIdInAndStatus(
                organizationIds, ShipmentHandoverStatus.PENDING_CONFIRMATION);

        return OrganizationAnalyticsDataDto.builder()
                .organizationId(null)
                .organizationName(territoryName != null ? territoryName : "Địa bàn quản lý")
                .organizationCode("TERRITORY")
                .lotSummary(ProductionLotSummaryDto.builder()
                        .totalLotsCount(totalLots)
                        .activeLotsCount(activeLots)
                        .harvestedLotsCount(harvestedLots)
                        .packagedLotsCount(packagedLots)
                        .totalAreaHectares(totalArea)
                        .upcomingHarvestLotNames(List.of())
                        .recentLotDetails(recentTerritoryLotDetails)
                        .build())
                .expiringCertifications(certDtos)
                .alertsSummary(RecentAlertsSummaryDto.builder()
                        .pendingScanAnomalyCount(anomalyCount)
                        .activeRecallCasesCount(recallCount)
                        .build())
                .shipmentSummary(ShipmentSummaryDto.builder()
                        .inTransitShipmentsCount(inTransit)
                        .pendingHandoverCount(pendingHandover)
                        .build())
                .build();
    }

    /**
     * Định dạng tên trạng thái vòng đời của lô sản xuất sang tiếng Việt chuẩn.
     */
    private String formatLotStatusVietnamese(vn.nguongocso.farm.enums.ProductionLotStatus status) {
        if (status == null) {
            return "Chưa xác định";
        }
        return switch (status) {
            case DRAFT -> "Nháp";
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đang canh tác (Đã duyệt)";
            case REJECTED -> "Bị từ chối";
            case HARVESTED -> "Đã thu hoạch";
            case PREPROCESSED -> "Đã sơ chế";
            case PACKAGED -> "Đã đóng gói";
            case CLOSED -> "Đã đóng (Hoàn tất)";
            case RECALLED -> "Đã thu hồi";
            case CANCELLED -> "Đã hủy";
            case DISPOSED -> "Đã tiêu hủy";
        };
    }
}
