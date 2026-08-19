package vn.nguongocso.farm.controller;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.nguongocso.auth.security.SecurityUtils;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.common.ApiResult;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.ProductionLotImportRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
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
 * Controller quản lý lô sản xuất.
 *
 * <p>
 * Cung cấp các API:
 * <ul>
 * <li>Tạo lô sản xuất</li>
 * <li>Cập nhật lô sản xuất</li>
 * <li>Xem chi tiết lô</li>
 * <li>Xem danh sách lô</li>
 * <li>Submit lô chờ duyệt</li>
 * <li>Duyệt lô</li>
 * <li>Dashboard lô sản xuất</li>
 * <li>Nhập lô sản xuất từ Excel</li>
 * <li>Tải file Excel mẫu</li>
 * <li>Xem lịch sử import</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/production-lots")
@RequiredArgsConstructor
public class ProductionLotController {

        private final ProductionLotService productionLotService;

        private final PermissionChecker permissionChecker;

        private final ProductionLotImportService productionLotImportService;

        private final ProductionLotImportHistoryRepository importHistoryRepository;

        /**
         * API tạo mới lô sản xuất.
         */
        @PostMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<CreateProductionLotResponse>> create(
                        @Valid @RequestBody CreateProductionLotRequest request,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {

                permissionChecker.check(
                                "PRODUCTION_LOT",
                                "CREATE");

                CreateProductionLotResponse response = productionLotService.createProductionLot(
                                request,
                                userDetails);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * API tải file Excel mẫu dùng cho chức năng import lô sản xuất.
         *
         * <p>
         * Người dùng phải chọn trước:
         * <ul>
         * <li>Loại nông sản</li>
         * <li>Vùng trồng</li>
         * </ul>
         *
         * <p>
         * Backend sẽ:
         * <ul>
         * <li>Kiểm tra loại nông sản tồn tại và đang hoạt động.</li>
         * <li>Kiểm tra vùng trồng tồn tại.</li>
         * <li>Kiểm tra vùng trồng thuộc tổ chức hiện tại.</li>
         * <li>Tạo file Excel mẫu.</li>
         * <li>Điền UUID loại nông sản và vùng trồng vào dòng mẫu.</li>
         * <li>Thiết lập format ngày dd/MM/yyyy.</li>
         * <li>Tạo dropdown hoạt động canh tác.</li>
         * </ul>
         */
        @GetMapping("/import-template")
        @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
        public ResponseEntity<Resource> downloadImportTemplate(
                        @RequestParam UUID productCategoryId,
                        @RequestParam UUID farmAreaId,
                        @AuthenticationPrincipal CustomUserDetails userDetails) {

                Resource resource = productionLotImportService.generateImportExcelTemplate(
                                productCategoryId,
                                farmAreaId,
                                userDetails);

                return ResponseEntity.ok()
                                .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"mau_nhap_lo_san_xuat.xlsx\"")
                                .contentType(
                                                MediaType.parseMediaType(
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
                                startDate,
                                endDate,
                                organizationId,
                                groupBy,
                                userDetails,
                                ipAddress);

                return ResponseEntity.ok(
                                ApiResult.success(response));
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
                                .findByOrganization_OrganizationIdOrderByImportedAtDesc(
                                                organizationId)
                                .stream()
                                .map(h -> ProductionLotImportHistoryResponse.builder()
                                                .id(h.getId())
                                                .fileName(h.getFileName())
                                                .totalRows(h.getTotalRows())
                                                .successCount(h.getSuccessCount())
                                                .failedCount(h.getFailedCount())
                                                .status(h.getStatus().name())
                                                .importedAt(
                                                                h.getImportedAt()
                                                                                .toInstant(
                                                                                                ZoneOffset.UTC))
                                                .build())
                                .toList();

                return ResponseEntity.ok(
                                ApiResult.success(history));
        }

        /**
         * API nhập danh sách lô sản xuất từ file Excel.
         *
         * <p>
         * Sử dụng multipart/form-data:
         * <ul>
         * <li>file: file Excel</li>
         * <li>organizationId: tùy chọn, chỉ VT-01 có thể nhập hộ tổ chức khác</li>
         * </ul>
         */
        @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAnyRole('VT-01', 'VT-02')")
        public ResponseEntity<ApiResult<ProductionLotImportResultResponse>> importProductionLots(
                        @RequestParam("file") MultipartFile file,

                        @RequestParam(value = "organizationId", required = false) String organizationIdStr,

                        @AuthenticationPrincipal CustomUserDetails userDetails) {

                ProductionLotImportRequest request = new ProductionLotImportRequest();

                request.setFile(file);

                /*
                 * organizationId được nhận dưới dạng String để tránh
                 * lỗi bind UUID khi FE gửi chuỗi rỗng.
                 */
                if (organizationIdStr != null
                                && !organizationIdStr.isBlank()) {

                        try {
                                request.setOrganizationId(
                                                UUID.fromString(
                                                                organizationIdStr.trim()));

                        } catch (IllegalArgumentException ex) {

                                throw new BusinessException(
                                                "Mã tổ chức không hợp lệ.");
                        }
                }

                /*
                 * Lấy IP tại Controller.
                 *
                 * Service không cần biết HttpServletRequest,
                 * đúng với trách nhiệm của từng tầng.
                 */
                String ipAddress = IpUtils.getClientIp();

                ProductionLotImportResultResponse response = productionLotImportService.importProductionLots(
                                request,
                                userDetails,
                                ipAddress);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * API lấy thông tin chi tiết lô sản xuất.
         *
         * <p>
         * Phải đặt sau các route tĩnh như:
         * /dashboard
         * /import-history
         * /import-template
         */
        @GetMapping("/{id}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<CreateProductionLotResponse>> getById(
                        @PathVariable UUID id) {

                // permissionChecker.check("PRODUCTION_LOT", "READ");

                CreateProductionLotResponse response = productionLotService.getProductionLotById(id);

                return ResponseEntity.ok(
                                ApiResult.success(response));
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

                // permissionChecker.check("PRODUCTION_LOT", "UPDATE");

                UpdateProductionLotResponse response = productionLotService.updateProductionLot(
                                id,
                                request,
                                userDetails);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * API lấy danh sách lô sản xuất của tổ chức hiện tại.
         */
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResult<List<?>>> getAll(
                        @AuthenticationPrincipal CustomUserDetails userDetails) {

                // permissionChecker.check("PRODUCTION_LOT", "READ");

                List<?> response = productionLotService.getAllProductionLots(
                                userDetails);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }

        /**
         * API gửi lô sản xuất lên trạng thái chờ duyệt.
         */
        @PostMapping("/{id}/submit")
        @PreAuthorize("hasRole('VT-02')")
        public ResponseEntity<ApiResult<?>> submitForApproval(
                        @PathVariable UUID id,

                        @AuthenticationPrincipal CustomUserDetails userDetails) {

                permissionChecker.check(
                                "PRODUCTION_LOT",
                                "UPDATE");

                return ResponseEntity.ok(
                                ApiResult.success(
                                                productionLotService.submitForApproval(
                                                                id,
                                                                userDetails)));
        }

        /**
         * API duyệt lô sản xuất.
         */
        @PostMapping("/{id}/approve")
        @PreAuthorize("hasRole('VT-02')")
        public ResponseEntity<ApiResult<CreateProductionLotResponse>> approve(
                        @PathVariable UUID id,

                        @Valid @RequestBody ApproveProductionLotRequest request) {

                permissionChecker.check(
                                "PRODUCTION_LOT",
                                "UPDATE");

                CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();

                CreateProductionLotResponse response = productionLotService.approveProductionLot(
                                id,
                                request,
                                userDetails);

                return ResponseEntity.ok(
                                ApiResult.success(response));
        }
}