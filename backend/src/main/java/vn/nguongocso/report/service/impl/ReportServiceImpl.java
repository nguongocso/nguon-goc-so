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
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.dto.response.IndustryReportResponse;
import vn.nguongocso.report.dto.response.ProductBreakdownItem;
import vn.nguongocso.report.excel.IndustryReportExcelGenerator;
import vn.nguongocso.report.pdf.IndustryReportPdfGenerator;
import vn.nguongocso.report.service.ReportService;
import vn.nguongocso.trace.repository.ShipmentRepository;

/**
 * Service xử lý báo cáo tổng hợp ngành (theo địa bàn và thời gian).
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
            LocalDate fromDate,
            LocalDate toDate) {

        CustomUserDetails currentUser = getCurrentUser();
        validateRole(currentUser);
        validateRequest(region, fromDate, toDate);

        return buildIndustrySummary(region, fromDate, toDate);
    }

    /**
     * Xuất báo cáo tổng hợp ngành sang file PDF (dạng byte[]).
     */
    @Override
    public byte[] exportIndustrySummary(
            String region,
            LocalDate fromDate,
            LocalDate toDate) {

        return exportIndustrySummary(region, fromDate, toDate, "PDF");
    }

    /**
     * Xuất báo cáo tổng hợp ngành theo định dạng PDF hoặc EXCEL.
     */
    @Override
    public byte[] exportIndustrySummary(
            String region,
            LocalDate fromDate,
            LocalDate toDate,
            String format) {

        CustomUserDetails currentUser = getCurrentUser();
        validateRole(currentUser);
        validateRequest(region, fromDate, toDate);

        String normalizedFormat = format == null ? "PDF" : format.trim().toUpperCase();

        long startTime = System.currentTimeMillis();
        try {
            IndustryReportResponse report = buildIndustrySummary(region, fromDate, toDate);

            byte[] file = switch (normalizedFormat) {
                case "PDF" -> pdfGenerator.generate(report);
                case "EXCEL", "XLSX" -> excelGenerator.generate(report);
                default -> throw new BusinessException(INVALID_FORMAT_MESSAGE);
            };

            log.info("Export industry report succeeded. role={}, user={}, region={}, "
                    + "fromDate={}, toDate={}, format={}, sizeBytes={}, durationMs={}",
                    currentUser.getRoleCode(),
                    currentUser.getUsername(),
                    region,
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
     * Kiểm tra người dùng có vai trò VT-05 (được xem báo cáo) hay không.
     */
    private void validateRole(CustomUserDetails currentUser) {
        if (!REGULATOR_ROLE.equals(currentUser.getRoleCode())) {
            throw new BusinessException(VIEW_PERMISSION_MESSAGE);
        }
    }

    /**
     * Kiểm tra đầu vào: region không rỗng, fromDate <= toDate.
     */
    private void validateRequest(String region, LocalDate fromDate, LocalDate toDate) {
        if (region == null || region.isBlank()) {
            throw new BusinessException(EMPTY_REGION_MESSAGE);
        }
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(INVALID_DATE_MESSAGE);
        }
    }

    /**
     * Xây dựng báo cáo: lấy danh sách tổ chức và thống kê từ Shipment.
     */
    private IndustryReportResponse buildIndustrySummary(
            String region,
            LocalDate fromDate,
            LocalDate toDate) {

        List<Organization> organizations = organizationRepository.findByAddressContainingIgnoreCase(region);

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
}