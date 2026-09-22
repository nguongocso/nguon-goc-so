package vn.nguongocso.farm.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final Clock clock;

    /** Nhập dữ liệu lô sản xuất từ tệp Excel. */
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
    /** Xác định tổ chức được phép nhập dữ liệu theo vai trò người dùng. */
    private Organization resolveOrganization(
            UUID organizationId,
            CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VT-01"));

        UUID targetOrganizationId;
        if (isAdmin && organizationId != null) {
            targetOrganizationId = organizationId;
        } else {
            targetOrganizationId = userDetails.getOrganizationId();

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
    /** Kiểm tra từng dòng Excel và gom dòng hợp lệ cùng danh sách lỗi. */
    private void validateRows(
            List<ProductionLotImportRow> rows,
            Organization organization,
            CustomUserDetails userDetails,
            List<ValidImportRow> validRows,
            List<ProductionLotImportRowError> rowErrors) {
        for (ProductionLotImportRow row : rows) {
            try {
                validateBasicInformation(row);

                ProductCategory category = validateProductCategory(row);

                FarmArea farmArea = validateFarmArea(row, organization);

                validRows.add(
                        ValidImportRow.builder()
                                .row(row)
                                .productionLot(buildProductionLot(
                                        row,
                                        organization,
                                        userDetails,
                                        category,
                                        farmArea))
                                .build());
            } catch (BusinessException ex) {
                rowErrors.add(
                        ProductionLotImportRowError.builder()
                                .rowNumber(row.getRowNumber())
                                .reason(ex.getMessage())
                                .build());
            }
        }
    }
    /** Lưu danh sách lô sản xuất hợp lệ. */
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
        for (ProductionLot lot : savedLots) {
            savedLotIds.add(lot.getId());
        }
    }
    /** Tạo và lưu nhật ký canh tác cho các dòng Excel có hoạt động. */
    private void saveFarmLogs(
            List<ValidImportRow> validRows,
            CustomUserDetails userDetails) {
        if (validRows.isEmpty()) {
            return;
        }
        List<FarmLog> farmLogs = new ArrayList<>();

        LocalDateTime importedAt = LocalDateTime.now(clock);
        for (ValidImportRow item : validRows) {
            ProductionLotImportRow row = item.getRow();
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
                            .createdAt(importedAt)
                            .build());
        }
        if (!farmLogs.isEmpty()) {
            farmLogRepository.saveAll(farmLogs);
        }
    }
    /** Kiểm tra các thông tin bắt buộc của dòng Excel. */
    private void validateBasicInformation(ProductionLotImportRow row) {
        if (row.getLotName() == null || row.getLotName().isBlank()) {
            throw new BusinessException(
                    "Tên lô (ten_lo) không được để trống.");
        }
        if (row.getProductCategoryId() == null
                || row.getProductCategoryId().isBlank()) {
            throw new BusinessException(
                    "Mã loại nông sản (ma_loai_nong_san) không được để trống.");
        }
        if (row.getFarmAreaId() == null
                || row.getFarmAreaId().isBlank()) {
            throw new BusinessException(
                    "Mã vùng trồng (ma_vung_trong) không được để trống.");
        }
        if (row.getExpectedQuantity() == null) {
            throw new BusinessException(
                    "Sản lượng dự kiến (san_luong_du_kien) không được để trống.");
        }
        if (row.getExpectedQuantity() <= 0) {
            throw new BusinessException(
                    "Sản lượng dự kiến (san_luong_du_kien) phải lớn hơn 0.");
        }
        if (row.getActualQuantity() != null
                && row.getActualQuantity() < 0) {
            throw new BusinessException(
                    "Sản lượng thực thu không được nhỏ hơn 0.");
        }
        if (row.getPlantingDate() != null) {
            throw new BusinessException(
                    "Ngày gieo trồng (ngay_gieo_trong) không đúng định dạng dd/MM/yyyy.");
        }
        if (row.getHarvestDate() != null
                && row.getHarvestDate().isBefore(row.getPlantingDate())) {
            throw new BusinessException(
                    "Ngày thu hoạch phải sau ngày gieo trồng.");
        }
    }
    /** Kiểm tra mã loại nông sản trong dòng Excel. */
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
    /** Kiểm tra mã vùng trồng trong dòng Excel thuộc tổ chức đang nhập. */
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
    /** Tạo lô sản xuất từ một dòng Excel hợp lệ. */
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
    /** Chuyển chuỗi UUID, trả null nếu không hợp lệ. */
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
    /** Lưu lịch sử nhập dữ liệu kèm trạng thái kết quả. */
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
    /** Ghi nhật ký hoạt động sau khi nhập dữ liệu. */
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
    /** Tạo phản hồi kết quả cho API nhập dữ liệu. */
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
                .importedAt(history.getImportedAt())
                .build();
    }
    /** Tạo tệp Excel mẫu nhập lô sản xuất. */
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
        UUID organizationId = userDetails.getOrganizationId();
        if (organizationId == null) {
            throw new BusinessException(
                    "Không xác định được tổ chức hiện tại.");
        }
        ProductCategory category = productCategoryRepository.findById(productCategoryId)
                .orElseThrow(() -> new BusinessException(
                        "Loại nông sản không tồn tại."));
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new BusinessException(
                    "Loại nông sản đã ngừng sử dụng.");
        }
        FarmArea farmArea = farmAreaRepository.findById(farmAreaId)
                .orElseThrow(() -> new BusinessException(
                        "Vùng trồng không tồn tại."));

        if (!farmArea.getOrganization()
                .getOrganizationId()
                .equals(organizationId)) {
            throw new BusinessException(
                    "Vùng trồng không thuộc tổ chức hiện tại.");
        }
        byte[] excelBytes = excelGenerator.generate(
                productCategoryId,
                farmAreaId);

        return new ByteArrayResource(excelBytes);
    }
}