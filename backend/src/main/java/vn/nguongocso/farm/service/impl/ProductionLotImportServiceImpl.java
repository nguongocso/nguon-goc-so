package vn.nguongocso.farm.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.nguongocso.alert.dto.request.ActivityLogRequest;
import vn.nguongocso.alert.service.ActivityLogService;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.dto.request.ProductionLotImportRequest;
import vn.nguongocso.farm.dto.response.ProductionLotImportResultResponse;
import vn.nguongocso.farm.dto.response.ProductionLotImportRowError;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.entity.ProductionLotImportHistory;
import vn.nguongocso.farm.enums.ProductionLotImportStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.repository.ProductionLotImportHistoryRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.farm.service.ProductionLotImportService;
import vn.nguongocso.farm.util.ProductionLotImportExcelGenerator;
import vn.nguongocso.farm.util.ProductionLotImportFileParser;
import vn.nguongocso.farm.util.ProductionLotImportRow;
import vn.nguongocso.farm.util.ValidImportRow;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.permission.service.PermissionChecker;

/**
 * Triển khai chức năng nhập dữ liệu lô sản xuất từ tệp Excel.
 *
 * <p>
 * Service chịu trách nhiệm:
 * <ul>
 * <li>Kiểm tra quyền nhập dữ liệu.</li>
 * <li>Xác định tổ chức đích.</li>
 * <li>Parse và validate từng dòng Excel.</li>
 * <li>Lưu ProductionLot.</li>
 * <li>Lưu FarmLog nếu dòng Excel có hoạt động canh tác.</li>
 * <li>Lưu lịch sử import.</li>
 * <li>Ghi ActivityLog.</li>
 * <li>Tạo file Excel mẫu.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductionLotImportServiceImpl implements ProductionLotImportService {

        private static final String RESOURCE = "production_lot";
        private static final String ACTION_CREATE = "CREATE";

        private final PermissionChecker permissionChecker;

        private final ProductionLotImportFileParser fileParser;

        private final ProductionLotRepository productionLotRepository;

        private final ProductionLotImportHistoryRepository importHistoryRepository;

        private final ProductCategoryRepository productCategoryRepository;

        private final FarmAreaRepository farmAreaRepository;

        private final FarmLogRepository farmLogRepository;

        private final OrganizationRepository organizationRepository;

        private final ActivityLogService activityLogService;

        private final ProductionLotImportExcelGenerator excelGenerator;

        /**
         * Clock nghiệp vụ theo múi giờ cấu hình (app.timezone, mặc định
         * Asia/Ho_Chi_Minh). Dùng để ghi createdAt của FarmLog tạo từ import
         * đúng giờ Việt Nam, không phụ thuộc timezone của JVM/container.
         */
        private final Clock clock;

        /**
         * Nhập dữ liệu lô sản xuất từ tệp Excel.
         *
         * @param request     thông tin file và tổ chức cần nhập
         * @param userDetails người dùng đang đăng nhập
         * @param ipAddress   địa chỉ IP của client
         * @return kết quả nhập dữ liệu
         */
        @Override
        public ProductionLotImportResultResponse importProductionLots(
                        ProductionLotImportRequest request,
                        CustomUserDetails userDetails,
                        String ipAddress) {

                permissionChecker.check(RESOURCE, ACTION_CREATE);

                Organization organization = resolveOrganization(
                                request.getOrganizationId(),
                                userDetails);

                List<ProductionLotImportRow> rows = fileParser.parse(request.getFile());

                // File chỉ có header hoặc không có dữ liệu
                if (rows.isEmpty()) {
                        throw new BusinessException(
                                        "File Excel không có dữ liệu lô sản xuất. "
                                                        + "Vui lòng nhập ít nhất một dòng dữ liệu.");
                }

                List<ValidImportRow> validRows = new ArrayList<>();
                List<UUID> savedLotIds = new ArrayList<>();
                List<ProductionLotImportRowError> rowErrors = new ArrayList<>();

                validateRows(
                                rows,
                                organization,
                                userDetails,
                                validRows,
                                rowErrors);

                saveProductionLots(
                                validRows,
                                savedLotIds);

                saveFarmLogs(
                                validRows,
                                userDetails);

                ProductionLotImportHistory history = saveImportHistory(
                                request.getFile().getOriginalFilename(),
                                organization,
                                userDetails,
                                rows.size(),
                                savedLotIds.size(),
                                rowErrors.size());

                writeActivityLog(
                                organization,
                                userDetails,
                                history,
                                ipAddress);

                return buildResponse(
                                history,
                                savedLotIds,
                                rowErrors);
        }

        /**
         * Xác định tổ chức được phép nhập dữ liệu.
         *
         * <p>
         * VT-01:
         * Có thể nhập dữ liệu cho tổ chức khác nếu truyền organizationId.
         *
         * <p>
         * VT-02:
         * Chỉ được nhập dữ liệu cho tổ chức hiện tại.
         */
        private Organization resolveOrganization(
                        UUID organizationId,
                        CustomUserDetails userDetails) {

                boolean isAdmin = userDetails.getAuthorities()
                                .stream()
                                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VT-01"));

                UUID targetOrganizationId;

                if (isAdmin && organizationId != null) {
                        // VT-01 nhập hộ cho tổ chức được chỉ định
                        targetOrganizationId = organizationId;
                } else {
                        // Người dùng thông thường sử dụng tổ chức hiện tại
                        targetOrganizationId = userDetails.getOrganizationId();

                        // Không cho phép VT-02 truyền organizationId của tổ chức khác
                        if (organizationId != null
                                        && !organizationId.equals(targetOrganizationId)) {

                                throw new BusinessException(
                                                "Bạn không có quyền nhập dữ liệu cho tổ chức này.");
                        }
                }

                if (targetOrganizationId == null) {
                        throw new BusinessException(
                                        "Không xác định được tổ chức để nhập dữ liệu.");
                }

                return organizationRepository.findById(targetOrganizationId)
                                .orElseThrow(() -> new BusinessException("Không tìm thấy tổ chức."));
        }

        /**
         * Validate từng dòng Excel và tạo danh sách ValidImportRow.
         *
         * <p>
         * Một dòng lỗi không làm toàn bộ file import thất bại.
         * Dòng hợp lệ vẫn tiếp tục được lưu.
         */
        private void validateRows(
                        List<ProductionLotImportRow> rows,
                        Organization organization,
                        CustomUserDetails userDetails,
                        List<ValidImportRow> validRows,
                        List<ProductionLotImportRowError> rowErrors) {

                for (ProductionLotImportRow row : rows) {

                        try {
                                // Validate thông tin cơ bản
                                validateBasicInformation(row);

                                // Validate loại nông sản
                                ProductCategory category = validateProductCategory(row);

                                // Validate vùng trồng
                                FarmArea farmArea = validateFarmArea(row, organization);

                                // Tạo ProductionLot
                                ProductionLot lot = buildProductionLot(
                                                row,
                                                organization,
                                                userDetails,
                                                category,
                                                farmArea);

                                // Đưa vào danh sách hợp lệ
                                validRows.add(
                                                ValidImportRow.builder()
                                                                .row(row)
                                                                .productionLot(lot)
                                                                .build());

                        } catch (BusinessException ex) {

                                // Ghi nhận lỗi theo số dòng Excel
                                rowErrors.add(
                                                ProductionLotImportRowError.builder()
                                                                .rowNumber(row.getRowNumber())
                                                                .reason(ex.getMessage())
                                                                .build());
                        }
                }
        }

        /**
         * Lưu danh sách ProductionLot hợp lệ.
         */
        private void saveProductionLots(
                        List<ValidImportRow> validRows,
                        List<UUID> savedLotIds) {

                if (validRows.isEmpty()) {
                        return;
                }

                List<ProductionLot> lots = validRows.stream()
                                .map(ValidImportRow::getProductionLot)
                                .toList();

                List<ProductionLot> savedLots = productionLotRepository.saveAll(lots);

                // JPA đã gán ID cho các entity sau khi saveAll
                for (ProductionLot lot : savedLots) {
                        savedLotIds.add(lot.getId());
                }
        }

        /**
         * Tạo và lưu nhật ký canh tác cho các dòng hợp lệ.
         *
         * <p>
         * Nếu dòng Excel không có hoat_dong_canh_tac
         * thì không tạo FarmLog.
         */
        private void saveFarmLogs(
                        List<ValidImportRow> validRows,
                        CustomUserDetails userDetails) {

                if (validRows.isEmpty()) {
                        return;
                }

                List<FarmLog> farmLogs = new ArrayList<>();

                // Thời gian tạo chung cho cả đợt import, theo múi giờ nghiệp vụ.
                LocalDateTime importedAt = LocalDateTime.now(clock);

                for (ValidImportRow item : validRows) {

                        ProductionLotImportRow row = item.getRow();

                        // Không có hoạt động canh tác thì bỏ qua
                        if (row.getActivityType() == null) {
                                continue;
                        }

                        farmLogs.add(
                                        FarmLog.builder()
                                                        .productionLotId(item.getProductionLot())
                                                        .activityType(row.getActivityType())
                                                        .material(row.getMaterial())
                                                        .quantity(row.getQuantity())
                                                        .unit(row.getUnit())
                                                        .executedDate(row.getExecutedDate())
                                                        .notes(row.getNote())
                                                        .createdBy(userDetails.getUser())
                                                        // Ghi thời gian tạo theo múi giờ nghiệp vụ
                                                        // (Asia/Ho_Chi_Minh), không dùng
                                                        // LocalDateTime.now() mặc định của JVM.
                                                        .createdAt(importedAt)
                                                        .build());
                }

                if (!farmLogs.isEmpty()) {
                        farmLogRepository.saveAll(farmLogs);
                }
        }

        /**
         * Validate thông tin cơ bản của dòng Excel.
         */
        private void validateBasicInformation(ProductionLotImportRow row) {

                // ten_lo - bắt buộc
                if (row.getLotName() == null || row.getLotName().isBlank()) {
                        throw new BusinessException(
                                        "Tên lô (ten_lo) không được để trống.");
                }

                // ma_loai_nong_san - bắt buộc
                if (row.getProductCategoryId() == null
                                || row.getProductCategoryId().isBlank()) {
                        throw new BusinessException(
                                        "Mã loại nông sản (ma_loai_nong_san) không được để trống.");
                }

                // ma_vung_trong - bắt buộc
                if (row.getFarmAreaId() == null
                                || row.getFarmAreaId().isBlank()) {
                        throw new BusinessException(
                                        "Mã vùng trồng (ma_vung_trong) không được để trống.");
                }

                // san_luong_du_kien - bắt buộc
                if (row.getExpectedQuantity() == null) {
                        throw new BusinessException(
                                        "Sản lượng dự kiến (san_luong_du_kien) không được để trống.");
                }

                if (row.getExpectedQuantity() <= 0) {
                        throw new BusinessException(
                                        "Sản lượng dự kiến (san_luong_du_kien) phải lớn hơn 0.");
                }

                // san_luong_thuc_thu - không bắt buộc
                if (row.getActualQuantity() != null
                                && row.getActualQuantity() < 0) {
                        throw new BusinessException(
                                        "Sản lượng thực thu không được nhỏ hơn 0.");
                }

                // ngay_gieo_trong - không bắt buộc theo logic hiện tại
                if (row.getPlantingDate() != null) {
                        throw new BusinessException(
                                        "Ngày gieo trồng (ngay_gieo_trong) không đúng định dạng dd/MM/yyyy.");
                }

                // ngay_thu_hoach - không bắt buộc
                if (row.getHarvestDate() != null
                                && row.getHarvestDate().isBefore(row.getPlantingDate())) {
                        throw new BusinessException(
                                        "Ngày thu hoạch phải sau ngày gieo trồng.");
                }
        }

        /**
         * Validate mã loại nông sản.
         */
        private ProductCategory validateProductCategory(
                        ProductionLotImportRow row) {

                if (row.getProductCategoryId() == null
                                || row.getProductCategoryId().isBlank()) {

                        throw new BusinessException(
                                        "Mã loại nông sản không được để trống.");
                }

                UUID id = parseUuid(row.getProductCategoryId());

                if (id == null) {
                        throw new BusinessException(
                                        "Mã loại nông sản không hợp lệ.");
                }

                ProductCategory category = productCategoryRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(
                                                "Loại nông sản không tồn tại."));

                if (!Boolean.TRUE.equals(category.getIsActive())) {
                        throw new BusinessException(
                                        "Loại nông sản đã ngừng sử dụng.");
                }

                return category;
        }

        /**
         * Validate mã vùng trồng.
         *
         * <p>
         * Vùng trồng có thể bỏ trống.
         * Nếu có thì bắt buộc phải thuộc tổ chức đang import.
         */
        private FarmArea validateFarmArea(
                        ProductionLotImportRow row,
                        Organization organization) {

                if (row.getFarmAreaId() == null
                                || row.getFarmAreaId().isBlank()) {
                        throw new BusinessException(
                                        "Mã vùng trồng (ma_vung_trong) không được để trống.");
                }

                UUID id = parseUuid(row.getFarmAreaId());

                if (id == null) {
                        throw new BusinessException(
                                        "Mã vùng trồng (ma_vung_trong) không hợp lệ.");
                }

                FarmArea farmArea = farmAreaRepository.findById(id)
                                .orElseThrow(() -> new BusinessException(
                                                "Vùng trồng không tồn tại."));

                if (!farmArea.getOrganization()
                                .getOrganizationId()
                                .equals(organization.getOrganizationId())) {

                        throw new BusinessException(
                                        "Vùng trồng không thuộc tổ chức.");
                }

                return farmArea;
        }

        /**
         * Tạo ProductionLot từ một dòng Excel hợp lệ.
         */
        private ProductionLot buildProductionLot(
                        ProductionLotImportRow row,
                        Organization organization,
                        CustomUserDetails userDetails,
                        ProductCategory category,
                        FarmArea farmArea) {

                return ProductionLot.builder()
                                .organization(organization)
                                .farmArea(farmArea)
                                .productCategory(category)
                                .name(row.getLotName())
                                .expectedQuantity(row.getExpectedQuantity())
                                .actualQuantity(row.getActualQuantity())
                                .plantingDate(row.getPlantingDate())
                                .harvestDate(row.getHarvestDate())
                                .status(ProductionLotStatus.DRAFT)
                                .createdBy(userDetails.getUser())
                                .expectedQuantityUnit("kg")
                                .build();
        }

        /**
         * Parse UUID từ chuỗi.
         */
        private UUID parseUuid(String value) {

                if (value == null || value.isBlank()) {
                        return null;
                }

                try {
                        return UUID.fromString(value.trim());

                } catch (IllegalArgumentException ex) {
                        return null;
                }
        }

        /**
         * Lưu lịch sử import.
         */
        private ProductionLotImportHistory saveImportHistory(
                        String fileName,
                        Organization organization,
                        CustomUserDetails userDetails,
                        Integer totalRows,
                        Integer successCount,
                        Integer failedCount) {

                ProductionLotImportStatus status;

                if (failedCount == 0) {
                        status = ProductionLotImportStatus.SUCCESS;

                } else if (successCount == 0) {
                        status = ProductionLotImportStatus.FAILED;

                } else {
                        status = ProductionLotImportStatus.PARTIAL_SUCCESS;
                }

                ProductionLotImportHistory history = ProductionLotImportHistory.builder()
                                .organization(organization)
                                .importedBy(userDetails.getUser())
                                .fileName(fileName)
                                .totalRows(totalRows)
                                .successCount(successCount)
                                .failedCount(failedCount)
                                .status(status)
                                .build();

                return importHistoryRepository.save(history);
        }

        /**
         * Ghi ActivityLog sau khi import.
         *
         * <p>
         * IP được lấy tại Controller và truyền xuống Service,
         * giúp Service không phụ thuộc trực tiếp vào HttpServletRequest.
         */
        private void writeActivityLog(
                        Organization organization,
                        CustomUserDetails userDetails,
                        ProductionLotImportHistory history,
                        String ipAddress) {

                activityLogService.logActivity(
                                ActivityLogRequest.builder()
                                                .organizationId(
                                                                organization.getOrganizationId())
                                                .userId(
                                                                userDetails.getUser().getUserId())
                                                .username(
                                                                userDetails.getUsername())
                                                .fullName(
                                                                userDetails.getUser().getFullName())
                                                .action("IMPORT_PRODUCTION_LOT")
                                                .description(
                                                                String.format(
                                                                                "Nhập dữ liệu lô sản xuất từ tệp '%s'. "
                                                                                                + "Kết quả: %d thành công, %d thất bại.",
                                                                                history.getFileName(),
                                                                                history.getSuccessCount(),
                                                                                history.getFailedCount()))
                                                .entityType(
                                                                "PRODUCTION_LOT_IMPORT_HISTORY")
                                                .entityId(history.getId())
                                                .ipAddress(ipAddress)
                                                .build());
        }

        /**
         * Tạo response trả về cho API import.
         */
        private ProductionLotImportResultResponse buildResponse(
                        ProductionLotImportHistory history,
                        List<UUID> savedLotIds,
                        List<ProductionLotImportRowError> rowErrors) {

                return ProductionLotImportResultResponse.builder()
                                .importHistoryId(history.getId())
                                .status(history.getStatus().name())
                                .fileName(history.getFileName())
                                .totalRows(history.getTotalRows())
                                .successCount(history.getSuccessCount())
                                .failedCount(history.getFailedCount())
                                .savedLotIds(savedLotIds)
                                .errors(rowErrors)
                                .importedAt(
                                                history.getImportedAt()
                                                                .toInstant(java.time.ZoneOffset.UTC))
                                .build();
        }

        /**
         * Tạo file Excel mẫu nhập lô sản xuất.
         *
         * <p>
         * File mẫu sẽ có header và ví dụ dữ liệu.
         */
        @Override
        @Transactional(readOnly = true)
        public Resource generateImportExcelTemplate(
                        UUID productCategoryId,
                        UUID farmAreaId,
                        CustomUserDetails userDetails) {

                permissionChecker.check(RESOURCE, ACTION_CREATE);

                if (productCategoryId == null) {
                        throw new BusinessException(
                                        "Vui lòng chọn loại nông sản.");
                }

                if (farmAreaId == null) {
                        throw new BusinessException(
                                        "Vui lòng chọn vùng trồng.");
                }

                // Lấy tổ chức hiện tại của người dùng
                UUID organizationId = userDetails.getOrganizationId();

                if (organizationId == null) {
                        throw new BusinessException(
                                        "Không xác định được tổ chức hiện tại.");
                }

                // =========================
                // Validate loại nông sản
                // =========================

                ProductCategory category = productCategoryRepository.findById(productCategoryId)
                                .orElseThrow(() -> new BusinessException(
                                                "Loại nông sản không tồn tại."));

                if (!Boolean.TRUE.equals(category.getIsActive())) {
                        throw new BusinessException(
                                        "Loại nông sản đã ngừng sử dụng.");
                }

                // =========================
                // Validate vùng trồng
                // =========================

                FarmArea farmArea = farmAreaRepository.findById(farmAreaId)
                                .orElseThrow(() -> new BusinessException(
                                                "Vùng trồng không tồn tại."));

                if (!farmArea.getOrganization()
                                .getOrganizationId()
                                .equals(organizationId)) {

                        throw new BusinessException(
                                        "Vùng trồng không thuộc tổ chức hiện tại.");
                }

                // =========================
                // Generate Excel
                // =========================

                byte[] excelBytes = excelGenerator.generate(
                                productCategoryId,
                                farmAreaId);

                return new ByteArrayResource(excelBytes);
        }
}