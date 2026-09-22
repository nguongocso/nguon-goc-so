package vn.nguongocso.farm.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.dto.response.InspectionScanResult;
import vn.nguongocso.certification.service.InspectionExpiryService;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CancelProductionLotRequest;
import vn.nguongocso.farm.dto.request.CloneProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.DisposeProductionLotRequest;
import vn.nguongocso.farm.dto.request.ProductionLotImportRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.ChainProgressBoardResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotPreviewResponse;
import vn.nguongocso.farm.dto.response.CloneProductionLotResponse;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.farm.dto.response.ProductionLotImportHistoryResponse;
import vn.nguongocso.farm.dto.response.ProductionLotImportResultResponse;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.farm.repository.ProductionLotImportHistoryRepository;
import vn.nguongocso.farm.service.ProductionLotImportService;
import vn.nguongocso.farm.service.ProductionLotService;
import vn.nguongocso.permission.service.PermissionChecker;
import vn.nguongocso.report.dto.response.ProductionLotDashboardResponse;

/**
 * Quản lý lô sản xuất.
*/
@RestController
@RequestMapping("/api/v1/production-lots")
@RequiredArgsConstructor
public class ProductionLotController {
    private final ProductionLotService productionLotService;

    private final PermissionChecker permissionChecker;

    private final ProductionLotImportService productionLotImportService;

    private final ProductionLotImportHistoryRepository importHistoryRepository;

    private final InspectionExpiryService inspectionExpiryService;

    /**
     * API tạo mới lô sản xuất.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<CreateProductionLotResponse>> create(
        @Valid @RequestBody CreateProductionLotRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("PRODUCTION_LOT", "CREATE");
        CreateProductionLotResponse response = productionLotService.createProductionLot(request, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API tải file Excel mẫu dùng cho chức năng import lô sản xuất.
     */
    @GetMapping("/import-template")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<Resource> downloadImportTemplate(
        @RequestParam UUID productCategoryId,
        @RequestParam UUID farmAreaId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Resource resource = productionLotImportService.generateImportExcelTemplate(
            productCategoryId, farmAreaId, userDetails);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau_nhap_lo_san_xuat.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }
    /**
     * API lấy dashboard lô sản xuất.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<ProductionLotDashboardResponse>> getDashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) UUID organizationId,
        @RequestParam(required = false, defaultValue = "MONTH") String groupBy,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        String ipAddress = IpUtils.getClientIp();
        ProductionLotDashboardResponse response = productionLotService.getDashboard(
            startDate, endDate, organizationId, groupBy, userDetails, ipAddress);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API lấy bảng theo dõi tiến độ chuỗi của từng lô.
     */
    @GetMapping("/chain-progress")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<ChainProgressBoardResponse>> getChainProgressBoard(
        @RequestParam(required = false) UUID organizationId,
        @RequestParam(required = false, defaultValue = "10") Integer stagnantThresholdDays,
        @RequestParam(required = false) String search,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChainProgressBoardResponse response = productionLotService.getChainProgressBoard(
            organizationId, stagnantThresholdDays, search, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API lấy lịch sử nhập dữ liệu lô sản xuất.
     */
    @GetMapping("/import-history")
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<List<ProductionLotImportHistoryResponse>>> getImportHistory(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID organizationId = userDetails.getOrganizationId();
        List<ProductionLotImportHistoryResponse> history = importHistoryRepository
            .findByOrganization_OrganizationIdOrderByImportedAtDesc(organizationId)
            .stream()
            .map(h -> ProductionLotImportHistoryResponse.builder()
                .id(h.getId())
                .fileName(h.getFileName())
                .totalRows(h.getTotalRows())
                .successCount(h.getSuccessCount())
                .failedCount(h.getFailedCount())
                .status(h.getStatus().name())
                .importedAt(h.getImportedAt())
                .build())
            .toList();
        return ResponseEntity.ok(ApiResult.success(history));
    }
    /**
     * API nhập danh sách lô sản xuất từ file Excel.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
    public ResponseEntity<ApiResult<ProductionLotImportResultResponse>> importProductionLots(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "organizationId", required = false) String organizationIdStr,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProductionLotImportRequest request = new ProductionLotImportRequest();
        request.setFile(file);
        if (organizationIdStr != null && !organizationIdStr.isBlank()) {
            try {
                request.setOrganizationId(UUID.fromString(organizationIdStr.trim()));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Mã tổ chức không hợp lệ.");
            }
        }
        String ipAddress = IpUtils.getClientIp();
        ProductionLotImportResultResponse response = productionLotImportService.importProductionLots(
            request, userDetails, ipAddress);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API lấy thông tin chi tiết lô sản xuất.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<CreateProductionLotResponse>> getById(@PathVariable UUID id) {
        CreateProductionLotResponse response = productionLotService.getProductionLotById(id);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API cập nhật lô sản xuất.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VT-02', 'VT-03')")
    public ResponseEntity<ApiResult<UpdateProductionLotResponse>> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProductionLotRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        UpdateProductionLotResponse response = productionLotService.updateProductionLot(id, request, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API lấy danh sách lô sản xuất của tổ chức hiện tại.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<?>>> getAll(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<?> response = productionLotService.getAllProductionLots(userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API gửi lô sản xuất lên trạng thái chờ duyệt.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<?>> submitForApproval(
        @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("PRODUCTION_LOT", "UPDATE");
        return ResponseEntity.ok(ApiResult.success(productionLotService.submitForApproval(id, userDetails)));
    }
    /**
     * API duyệt lô sản xuất.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CreateProductionLotResponse>> approve(
        @PathVariable UUID id,
        @Valid @RequestBody ApproveProductionLotRequest request) {
        permissionChecker.check("PRODUCTION_LOT", "UPDATE");
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        CreateProductionLotResponse response = productionLotService.approveProductionLot(id, request, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API hủy lô sản xuất.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CreateProductionLotResponse>> cancel(
        @PathVariable UUID id,
        @Valid @RequestBody CancelProductionLotRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("PRODUCTION_LOT", "UPDATE");
        CreateProductionLotResponse response = productionLotService.cancelProductionLot(id, request, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API loại bỏ lô sản xuất.
     */
    @PostMapping("/{id}/dispose")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CreateProductionLotResponse>> dispose(
        @PathVariable UUID id,
        @Valid @RequestBody DisposeProductionLotRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("PRODUCTION_LOT", "UPDATE");
        CreateProductionLotResponse response = productionLotService.disposeProductionLot(id, request, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API lấy dữ liệu xem trước khi tạo lô sản xuất mới từ mẫu vụ trước.
     */
    @GetMapping("/{sourceLotId}/clone-preview")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CloneProductionLotPreviewResponse>> getClonePreview(
        @PathVariable UUID sourceLotId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("PRODUCTION_LOT", "CREATE");
        CloneProductionLotPreviewResponse response = productionLotService.getClonePreview(sourceLotId, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API tạo lô sản xuất mới từ mẫu vụ trước.
     */
    @PostMapping("/{sourceLotId}/clone")
    @PreAuthorize("hasRole('VT-02')")
    public ResponseEntity<ApiResult<CloneProductionLotResponse>> clone(
        @PathVariable UUID sourceLotId,
        @Valid @RequestBody CloneProductionLotRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        permissionChecker.check("PRODUCTION_LOT", "CREATE");
        CloneProductionLotResponse response = productionLotService.cloneProductionLot(sourceLotId, request, userDetails);
        return ResponseEntity.ok(ApiResult.success(response));
    }
    /**
     * API kích hoạt quét và kiểm tra hạn kết quả kiểm nghiệm của các lô sản xuất.
     */
    @PostMapping("/check-inspection-expiry")
    @PreAuthorize("hasRole('VT-01')")
    public ResponseEntity<ApiResult<InspectionScanResult>> checkInspectionExpiry() {
        InspectionScanResult result = inspectionExpiryService.scanAndAlertExpiringInspections();
        return ResponseEntity.ok(ApiResult.success(result));
    }
}