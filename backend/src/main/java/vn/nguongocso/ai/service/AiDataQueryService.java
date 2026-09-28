package vn.nguongocso.ai.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import vn.nguongocso.ai.dto.query.CertificationStatusDto;
import vn.nguongocso.ai.dto.query.OrganizationAnalyticsDataDto;
import vn.nguongocso.ai.dto.query.ProductionLotSummaryDto;
import vn.nguongocso.ai.dto.query.RecentAlertsSummaryDto;
import vn.nguongocso.ai.dto.query.ShipmentSummaryDto;

/**
 * Giao diện dịch vụ trích xuất và tổng hợp số liệu nghiệp vụ phục vụ Trợ lý AI (TASK-AI-05).
 */
public interface AiDataQueryService {
    /**
     * Tổng hợp số liệu về lô sản xuất và diện tích canh tác của một tổ chức.
     */
    ProductionLotSummaryDto getProductionLotSummary(UUID organizationId);

    /**
     * Lấy danh sách các chứng nhận chất lượng sắp hết hiệu lực trong 30 ngày tới của một tổ chức.
     */
    List<CertificationStatusDto> getCertificationStatus(UUID organizationId);

    /**
     * Tổng hợp số lượng cảnh báo quét bất thường và vụ việc thu hồi gần đây của một tổ chức.
     */
    RecentAlertsSummaryDto getRecentAlertsSummary(UUID organizationId);

    /**
     * Tổng hợp số lượng lô hàng đang lưu thông và biên bản bàn giao đang chờ xử lý của một tổ chức.
     */
    ShipmentSummaryDto getShipmentSummary(UUID organizationId);

    /**
     * Trích xuất toàn bộ dữ liệu thống kê nghiệp vụ của một tổ chức (đóng gói 4 khối dữ liệu trên).
     */
    OrganizationAnalyticsDataDto getFullOrganizationAnalytics(UUID organizationId);

    /**
     * Tổng hợp số liệu nghiệp vụ theo danh sách tổ chức thuộc địa bàn quản lý (dành cho Cán bộ Quản lý ngành VT-05).
     */
    OrganizationAnalyticsDataDto getTerritoryAnalytics(Set<UUID> organizationIds, String territoryName);
}
