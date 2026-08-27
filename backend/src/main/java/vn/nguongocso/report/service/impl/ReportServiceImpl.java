package vn.nguongocso.report.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.organization.constant.RoleCode;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.dto.response.ProductBreakdownItem;
import vn.nguongocso.report.excel.IndustryReportExcelGenerator;
import vn.nguongocso.report.pdf.IndustryReportPdfGenerator;
import vn.nguongocso.report.service.ReportService;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Service xử lý báo cáo tổng hợp ngành (theo địa bàn và thời gian).
 *
 * <p>
 * Từ NCL-743: kết quả luôn giao trong phạm vi địa bàn đã gán khi caller là
 * VT-05. Cán bộ chưa được gán địa bàn nào nhận dữ liệu rỗng kèm thông báo —
 * không bao giờ fallback sang toàn bộ dữ liệu.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {
    private final OrganizationRepository organizationRepository;
    private final ShipmentRepository shipmentRepository;
    private final IndustryReportPdfGenerator pdfGenerator;
    private final IndustryReportExcelGenerator excelGenerator;
    private final AreaScopeService areaScopeService;

    private static final String REGULATOR_ROLE = "VT-05";
    private static final String VIEW_PERMISSION_MESSAGE = "Bạn không có quyền xem báo cáo tổng hợp.";
    private static final String INVALID_DATE_MESSAGE = "Khoảng thời gian không hợp lệ.";
    private static final String EMPTY_REGION_MESSAGE = "Địa bàn không được để trống.";
    private static final String INVALID_FORMAT_MESSAGE = "Định dạng xuất không hợp lệ. Chỉ hỗ trợ PDF hoặc EXCEL.";
    private static final String EXPORT_ERROR_MESSAGE = "Không thể xuất báo cáo.";

    /**
     * Lấy báo cáo tổng hợp ngành dưới dạng đối tượng response.
     */
    @Override
    public IndustryReportResponse getIndustrySummary(
            String region,
            List<UUID> unitIds,
            LocalDate fromDate,
            LocalDate toDate) {

        CustomUserDetails currentUser = getCurrentUser();
        validateRole(currentUser);
        validateDates(fromDate, toDate);

        AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(currentUser, unitIds);

        // Chỉ bắt buộc region khi caller xem toàn hệ thống (VT-01 không lọc unitIds).
        if (scope.isAll() && isBlank(region)) {
            throw new BusinessException(EMPTY_REGION_MESSAGE);
        }

        return buildIndustrySummary(region, scope, fromDate, toDate);
    }

    /**
     * Xuất báo cáo tổng hợp ngành sang file PDF (dạng byte[]).
     */
    @Override
    public byte[] exportIndustrySummary(
            String region,
            List<UUID> unitIds,
            LocalDate fromDate,
            LocalDate toDate) {

        return exportIndustrySummary(region, unitIds, fromDate, toDate, "PDF");
    }

    /**
     * Xuất báo cáo tổng hợp ngành theo định dạng PDF hoặc EXCEL.
     */
    @Override
    public byte[] exportIndustrySummary(
            String region,
            List<UUID> unitIds,
            LocalDate fromDate,
            LocalDate toDate,
            String format) {

        CustomUserDetails currentUser = getCurrentUser();
        validateRole(currentUser);
        validateDates(fromDate, toDate);

        String normalizedFormat = format == null ? "PDF" : format.trim().toUpperCase();

        long startTime = System.currentTimeMillis();
        try {
            AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(currentUser, unitIds);

            if (scope.isAll() && isBlank(region)) {
                throw new BusinessException(EMPTY_REGION_MESSAGE);
            }

            IndustryReportResponse report = buildIndustrySummary(region, scope, fromDate, toDate);

            byte[] file = switch (normalizedFormat) {
                case "PDF" -> pdfGenerator.generate(report);
                case "EXCEL", "XLSX" -> excelGenerator.generate(report);
                default -> throw new BusinessException(INVALID_FORMAT_MESSAGE);
            };

            log.info("Export industry report succeeded. role={}, user={}, region={}, "
                    + "unitIds={}, fromDate={}, toDate={}, format={}, sizeBytes={}, durationMs={}",
                    currentUser.getRoleCode(),
                    currentUser.getUsername(),
                    region,
                    unitIds,
                    fromDate,
                    toDate,
                    normalizedFormat,
                    file.length,
                    System.currentTimeMillis() - startTime);

            return file;

        } catch (BusinessException ex) {
            log.warn("Export industry report rejected. reason={}, region={}, fromDate={}, toDate={}, format={}",
                    ex.getMessage(),
                    region,
                    fromDate,
                    toDate,
                    normalizedFormat);
            throw ex;
        } catch (Exception ex) {
            log.error("Export industry report failed unexpectedly. region={}, fromDate={}, toDate={}, format={}",
                    region,
                    fromDate,
                    toDate,
                    normalizedFormat,
                    ex);
            throw new BusinessException(EXPORT_ERROR_MESSAGE);
        }
    }

    /**
     * Lấy thông tin người dùng hiện tại từ SecurityContext.
     */
    private CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * Kiểm tra người dùng có vai trò VT-05 / VT-01 hay không.
     */
    private void validateRole(CustomUserDetails currentUser) {
        if (!REGULATOR_ROLE.equals(currentUser.getRoleCode())
                && !RoleCode.ADMIN.equals(currentUser.getRoleCode())) {
            throw new BusinessException(VIEW_PERMISSION_MESSAGE);
        }
    }

    /**
     * Kiểm tra fromDate <= toDate.
     */
    private void validateDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(INVALID_DATE_MESSAGE);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Xây dựng báo cáo: lấy danh sách tổ chức trong phạm vi được phép và thống
     * kê từ Shipment.
     */
    private IndustryReportResponse buildIndustrySummary(
            String region,
            AreaScopeResult scope,
            LocalDate fromDate,
            LocalDate toDate) {

        // Rule bảo mật số 1 NCL-670: cán bộ chưa được gán địa bàn nào => rỗng hoàn toàn.
        if (scope.isEmptyScope()) {
            return buildUnassignedResponse(region, fromDate, toDate);
        }

        List<Organization> organizations = isBlank(region)
                ? organizationRepository.findAll()
                : organizationRepository.findByAddressContainingIgnoreCase(region);

        if (!scope.isAll()) {
            organizations = organizations.stream()
                    .filter(org -> scope.getOrganizationIds().contains(org.getOrganizationId()))
                    .toList();
        }

        if (organizations.isEmpty()) {
            return buildEmptyResponse(region, fromDate, toDate);
        }

        List<UUID> organizationIds = organizations.stream()
                .map(Organization::getOrganizationId)
                .toList();

        var startDate = fromDate.atStartOfDay();
        var endDate = toDate.plusDays(1).atStartOfDay();

        Long shipmentCountRaw = shipmentRepository.countShipments(
                organizationIds,
                startDate,
                endDate);

        Double totalQuantityRaw = shipmentRepository.getTotalQuantity(
                organizationIds,
                startDate,
                endDate);

        List<ProductBreakdownItem> breakdownRaw = shipmentRepository.getProductBreakdown(
                organizationIds,
                startDate,
                endDate);

        long shipmentCount = shipmentCountRaw == null ? 0L : shipmentCountRaw;
        double totalQuantity = totalQuantityRaw == null ? 0D : totalQuantityRaw;
        List<ProductBreakdownItem> productBreakdown = breakdownRaw == null ? List.of() : breakdownRaw;

        return buildResponse(region, fromDate, toDate, organizations.size(), shipmentCount, totalQuantity,
                productBreakdown);
    }

    /**
     * Tạo response hoàn chỉnh dựa trên dữ liệu đã tính toán.
     */
    private IndustryReportResponse buildResponse(
            String region,
            LocalDate fromDate,
            LocalDate toDate,
            int totalOrganizations,
            long shipmentCount,
            double totalQuantity,
            List<ProductBreakdownItem> productBreakdown) {

        boolean hasData = shipmentCount > 0
                || totalQuantity > 0
                || !productBreakdown.isEmpty();

        IndustryReportResponse response = new IndustryReportResponse();

        response.setRegion(region);
        response.setFromDate(fromDate);
        response.setToDate(toDate);

        response.setHasData(hasData);
        response.setTotalOrganizations(totalOrganizations);
        response.setTotalShipments((int) shipmentCount);
        response.setTotalQuantity(totalQuantity);
        response.setProductBreakdown(productBreakdown);
        response.setMessage(
                hasData
                        ? "Lấy báo cáo tổng hợp thành công."
                        : "Không có dữ liệu trong khoảng thời gian đã chọn.");

        return response;
    }

    /**
     * Trả về response rỗng khi không tìm thấy tổ chức theo địa bàn.
     */
    private IndustryReportResponse buildEmptyResponse(
            String region,
            LocalDate fromDate,
            LocalDate toDate) {

        IndustryReportResponse response = new IndustryReportResponse();
        response.setRegion(region);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setHasData(false);
        response.setTotalOrganizations(0);
        response.setTotalShipments(0);
        response.setTotalQuantity(0D);
        response.setProductBreakdown(List.of());
        response.setMessage("Không có dữ liệu phù hợp.");
        return response;
    }

    /**
     * Response rỗng bắt buộc cho VT-05 chưa được phân công địa bàn (release
     * blocker TC-02): totals 0, breakdown rỗng, message chuẩn hợp đồng.
     */
    private IndustryReportResponse buildUnassignedResponse(
            String region,
            LocalDate fromDate,
            LocalDate toDate) {

        IndustryReportResponse response = new IndustryReportResponse();
        response.setRegion(region);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setHasData(false);
        response.setTotalOrganizations(0);
        response.setTotalShipments(0);
        response.setTotalQuantity(0D);
        response.setProductBreakdown(List.of());
        response.setMessage(AreaScopeService.UNASSIGNED_MESSAGE);
        return response;
    }
}
