package vn.nguongocso.farm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.common.util.IpUtils;
import vn.nguongocso.farm.dto.request.ApproveProductionLotRequest;
import vn.nguongocso.farm.dto.request.CancelProductionLotRequest;
import vn.nguongocso.farm.dto.request.CreateProductionLotRequest;
import vn.nguongocso.farm.dto.request.UpdateProductionLotRequest;
import vn.nguongocso.farm.dto.response.CreateProductionLotResponse;
import vn.nguongocso.auth.entity.User;
import vn.nguongocso.farm.dto.response.UpdateProductionLotResponse;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.auth.repository.UserRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.farm.repository.FarmAreaRepository;
import vn.nguongocso.farm.repository.ProductCategoryRepository;
import vn.nguongocso.farm.service.ProductionLotService;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.exception.ResourceNotFoundException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductCategory;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.report.dto.response.ProductionLotDashboardResponse;
import vn.nguongocso.report.service.ReportAccessLogService;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/** Quản lý vòng đời lô sản xuất và dashboard liên quan. */
public class ProductionLotServiceImpl implements ProductionLotService {

    private static final Logger log = LoggerFactory.getLogger(ProductionLotServiceImpl.class);

    private final ProductionLotRepository productionLotRepository;
    private final FarmAreaRepository farmAreaRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ReportAccessLogService reportAccessLogService;
    private final ShipmentRepository shipmentRepository;

    private final ApplicationEventPublisher eventPublisher;

    /** Tạo lô sản xuất mới. */
    @Override
    @Transactional
    public CreateProductionLotResponse createProductionLot(CreateProductionLotRequest request,
            CustomUserDetails userDetails) {
        log.info("Bắt đầu xử lý tạo lô sản xuất với tên={}", request.getName());

        UUID userId = userDetails.getUserId();
        UUID orgId = userDetails.getOrganizationId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản"));
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tổ chức tương ứng"));

        ProductCategory productCategory = productCategoryRepository.findById(request.getProductCategoryId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy loại nông sản đã chọn"));
        if (Boolean.FALSE.equals(productCategory.getIsActive())) {
            throw new BusinessException("Loại nông sản này hiện đang ngưng hoạt động");
        }

        FarmArea farmArea;
        if (request.getFarmAreaId() == null) {
            throw new BusinessException("Vui lòng chọn vùng trồng");
        }
        farmArea = farmAreaRepository.findById(request.getFarmAreaId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy khu vực canh tác đã sélection"));

        if (!farmArea.getOrganization().getOrganizationId().equals(orgId)) {
            throw new BusinessException("Khu vực canh tác này không thuộc tổ chức của bạn");
        }
        if (Boolean.FALSE.equals(farmArea.getIsActive())) {
            throw new BusinessException("Vùng trồng '" + farmArea.getName() + "' hiện đã ngừng sử dụng, không thể chọn để tạo lô sản xuất mới");
        }

        ProductionLot productionLot = ProductionLot.builder()
                .organization(organization)
                .farmArea(farmArea)
                .productCategory(productCategory)
                .name(request.getName())
                .expectedQuantity(request.getExpectedQuantity())
                .expectedQuantityUnit(request.getExpectedQuantityUnit())
                .plantingDate(request.getPlantingDate())
                .status(ProductionLotStatus.DRAFT)
                .createdBy(user)
                .build();

        ProductionLot savedLot = productionLotRepository.save(productionLot);
        log.info("Đã tạo thành công lô sản xuất với id={}", savedLot.getId());

        publishActivityLog(
                userDetails,
                "CREATE",
                "Tạo lô sản xuất " + savedLot.getName(),
                "ProductionLot",
                savedLot.getId().toString());

        return mapToResponse(savedLot);
    }

    /** Lấy chi tiết lô sản xuất theo ID. */
    @Override
    @Transactional(readOnly = true)
    public CreateProductionLotResponse getProductionLotById(UUID id) {
        log.info("Lấy thông tin lô sản xuất id={}", id);
        ProductionLot lot = productionLotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lô sản xuất không tồn tại"));
        return mapToResponse(lot);
    }

    /** Lấy danh sách lô sản xuất của tổ chức hiện tại. */
    @Override
    @Transactional(readOnly = true)
    public List<CreateProductionLotResponse> getAllProductionLots(CustomUserDetails userDetails) {
        UUID orgId = userDetails.getOrganizationId();

        log.info("Lấy danh sách lô sản xuất cho tổ chức id={}", orgId);

        List<ProductionLot> lots = productionLotRepository.findByOrganization_OrganizationId(orgId);

        return lots.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Phê duyệt hoặc từ chối lô sản xuất. */
    @Override
    @Transactional
    public CreateProductionLotResponse approveProductionLot(UUID lotId, ApproveProductionLotRequest request,
            CustomUserDetails userDetails) {
        log.info("Bắt đầu duyệt lô sản xuất với id={}", lotId);

        UUID orgId = userDetails.getOrganizationId();
        UUID userId = userDetails.getUserId();

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        if (!lot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new BusinessException("Lô sản xuất không thuộc tổ chức của bạn");
        }

        if (lot.getStatus() != ProductionLotStatus.PENDING) {
            throw new BusinessException("Chỉ có thể duyệt lô đang ở trạng thái chờ duyệt");
        }

        User approver = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản "));

        if (request.getApproved()) {
            lot.setStatus(ProductionLotStatus.APPROVED);
            lot.setApprovedBy(approver);
            lot.setApprovalNotes(null);
            log.info("Lô {} đã được duyệt bởi {}", lotId, userId);
        } else {
            lot.setStatus(ProductionLotStatus.DRAFT);
            lot.setApprovedBy(null);
            lot.setApprovalNotes(request.getReason());
            log.info("Lô {} bị từ chối bởi {}, lý do: {}", lotId, userId, request.getReason());
        }

        ProductionLot saved = productionLotRepository.save(lot);

        String action = request.getApproved() ? "APPROVE" : "REJECT";
        String description = request.getApproved()
                ? "Duyệt lô sản xuất " + lot.getName()
                : "Từ chối lô sản xuất " + lot.getName() + " với lý do: " + request.getReason();
        publishActivityLog(
                userDetails,
                action,
                description,
                "ProductionLot",
                saved.getId().toString());

        return mapToResponse(saved);
    }

    /** Hủy lô sản xuất và ghi lý do (NCL-02-CN-006). */
    @Override
    @Transactional
    @Auditable(action = "CANCEL_PRODUCTION_LOT", entityType = "PRODUCTION_LOT", description = "'Hủy lô sản xuất ID: ' + #lotId + ', lý do: ' + #request.reason")
    public CreateProductionLotResponse cancelProductionLot(UUID lotId, CancelProductionLotRequest request,
            CustomUserDetails userDetails) {
        log.info("Bắt đầu hủy lô sản xuất với id={}", lotId);

        UUID orgId = userDetails.getOrganizationId();
        UUID userId = userDetails.getUserId();

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        if (!lot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new BusinessException("Lô sản xuất không thuộc tổ chức của bạn");
        }

        if (lot.getStatus() == ProductionLotStatus.CANCELLED
                || lot.getStatus() == ProductionLotStatus.CLOSED
                || lot.getStatus() == ProductionLotStatus.RECALLED) {
            throw new BusinessException("Lô đã ở trạng thái " + lot.getStatus().name() + ", không thể hủy");
        }

        boolean hasTraceCodes = !shipmentRepository.findByProductionLotId(lotId).isEmpty();
        if (hasTraceCodes) {
            throw new BusinessException("Lô đã sinh mã truy xuất, không thể hủy. Vui lòng sử dụng luồng thu hồi lô");
        }

        User cancelledBy = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin tài khoản"));

        lot.setStatus(ProductionLotStatus.CANCELLED);
        lot.setCancellationReason(request.getReason());
        lot.setCancellationNote(request.getNote());
        lot.setCancelledBy(cancelledBy);
        lot.setCancelledAt(LocalDateTime.now());

        ProductionLot saved = productionLotRepository.save(lot);

        publishActivityLog(
                userDetails,
                "CANCEL",
                "Hủy lô sản xuất " + saved.getName() + " với lý do: " + request.getReason(),
                "ProductionLot",
                saved.getId().toString());

        return mapToResponse(saved);
    }

    /** Gửi lô sản xuất sang trạng thái chờ duyệt. */
    @Override
    @Transactional
    public CreateProductionLotResponse submitForApproval(UUID lotId, CustomUserDetails userDetails) {
        UUID orgId = userDetails.getOrganizationId();

        ProductionLot lot = productionLotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lô sản xuất"));

        if (!lot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new BusinessException("Bạn không có quyền với lô này");
        }

        if (lot.getStatus() != ProductionLotStatus.DRAFT) {
            throw new BusinessException("Chỉ  có thể gửi duyệt lô ở trạng thái DRAFT");
        }

        if (lot.getFarmArea() == null) {
            throw new BusinessException("Vui lòng chọn vùng trồng trước khi gửi duyệt");
        }

        lot.setStatus(ProductionLotStatus.PENDING);
        lot.setUpdatedAt(LocalDateTime.now());
        productionLotRepository.save(lot);

        log.info("Gửi duyệt lô thành công: lotId={}", lotId);

        publishActivityLog(
                userDetails,
                "SUBMIT",
                "Gửi duyệt lô sản xuất " + lot.getName(),
                "ProductionLot",
                lot.getId().toString());

        return mapToResponse(lot);
    }

    /** Chuyển entity lô sản xuất sang response. */
    private CreateProductionLotResponse mapToResponse(ProductionLot lot) {
        return CreateProductionLotResponse.builder()
                .id(lot.getId())
                .farmAreaId(lot.getFarmArea() != null ? lot.getFarmArea().getId() : null)
                .productCategoryId(lot.getProductCategory().getId())
                .organizationName(lot.getOrganization().getName())
                .farmAreaName(lot.getFarmArea() != null ? lot.getFarmArea().getName() : null)
                .productCategoryName(lot.getProductCategory().getName())
                .name(lot.getName())
                .expectedQuantity(lot.getExpectedQuantity())
                .expectedQuantityUnit(lot.getExpectedQuantityUnit())
                .actualQuantity(lot.getActualQuantity())
                .plantingDate(lot.getPlantingDate())
                .harvestDate(lot.getHarvestDate())
                .status(lot.getStatus().name())
                .approvalNotes(lot.getApprovalNotes())
                .createdByName(lot.getCreatedBy() != null ? lot.getCreatedBy().getFullName() : null)
                .approvedByName(lot.getApprovedBy() != null ? lot.getApprovedBy().getFullName() : null)
                .cancellationReason(lot.getCancellationReason())
                .cancellationNote(lot.getCancellationNote())
                .cancelledByName(lot.getCancelledBy() != null ? lot.getCancelledBy().getFullName() : null)
                .cancelledAt(lot.getCancelledAt())
                .createdAt(lot.getCreatedAt())
                .updatedAt(lot.getUpdatedAt())
                .build();
    }

    /** Cập nhật thông tin lô sản xuất. */
    @Override
    @Transactional
    public UpdateProductionLotResponse updateProductionLot(UUID id, UpdateProductionLotRequest request,
            CustomUserDetails userDetails) {
        log.info("Bắt đầu xử lý cập nhật lô sản xuất với id={}", id);

        UUID orgId = userDetails.getOrganizationId();

        ProductionLot productionLot = productionLotRepository.findById(id)
                .orElseThrow(() -> new vn.nguongocso.exception.ResourceNotFoundException("Lô sản xuất không tồn tại"));

        if (!productionLot.getOrganization().getOrganizationId().equals(orgId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền chỉnh sửa lô sản xuất này");
        }

        if (productionLot.getStatus() != ProductionLotStatus.DRAFT) {
            throw new vn.nguongocso.exception.DuplicateResourceException(
                    "Chỉ có thể cập nhật lô sản xuất khi đang ở trạng thái nháp");
        }

        ProductCategory productCategory = productCategoryRepository.findById(request.getProductCategoryId())
                .orElseThrow(
                        () -> new vn.nguongocso.exception.BusinessException("Không tìm thấy loại nông sản đã chọn"));
        if (Boolean.FALSE.equals(productCategory.getIsActive())) {
            throw new vn.nguongocso.exception.BusinessException("Loại nông sản này hiện đang ngưng hoạt động");
        }

        FarmArea farmArea;
        if (request.getFarmAreaId() == null) {
            throw new vn.nguongocso.exception.BusinessException("Vui lòng chọn vùng trồng");
        }
        farmArea = farmAreaRepository.findById(request.getFarmAreaId())
                .orElseThrow(() -> new vn.nguongocso.exception.BusinessException(
                        "Không tìm thấy khu vực canh tác đã chọn"));
        if (!farmArea.getOrganization().getOrganizationId().equals(orgId)) {
            throw new vn.nguongocso.exception.BusinessException("Khu vực canh tác này không thuộc tổ chức của bạn");
        }

        productionLot.setName(request.getName());
        productionLot.setFarmArea(farmArea);
        productionLot.setProductCategory(productCategory);
        productionLot.setExpectedQuantity(request.getExpectedQuantity());
        productionLot.setExpectedQuantityUnit(request.getExpectedQuantityUnit());
        productionLot.setPlantingDate(request.getPlantingDate());

        ProductionLot savedLot = productionLotRepository.save(productionLot);
        log.info("Cập nhật thành công lô sản xuất id={}", savedLot.getId());

        publishActivityLog(
                userDetails,
                "UPDATE",
                "Cập nhật lô sản xuất " + savedLot.getName(),
                "ProductionLot",
                savedLot.getId().toString());

        return UpdateProductionLotResponse.builder()
                .id(savedLot.getId())
                .farmAreaId(savedLot.getFarmArea() != null ? savedLot.getFarmArea().getId() : null)
                .productCategoryId(savedLot.getProductCategory().getId())
                .name(savedLot.getName())
                .expectedQuantity(savedLot.getExpectedQuantity())
                .expectedQuantityUnit(savedLot.getExpectedQuantityUnit())
                .plantingDate(savedLot.getPlantingDate())
                .status(savedLot.getStatus().name())
                .updatedAt(savedLot.getUpdatedAt())
                .build();
    }

    /** Gửi sự kiện nhật ký hoạt động. */
    private void publishActivityLog(CustomUserDetails currentUser, String action, String description, String entityType,
            String entityId) {
        eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(IpUtils.getClientIp())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /** Lấy dashboard thống kê lô sản xuất. */
    @Override
    @Transactional(readOnly = true)
    public ProductionLotDashboardResponse getDashboard(
            LocalDate startDate,
            LocalDate endDate,
            UUID targetOrganizationId,
            String groupBy,
            CustomUserDetails userDetails,
            String ipAddress) {

        UUID userOrgId = userDetails.getOrganizationId();
        UUID userId = userDetails.getUserId();

        // 1. Xác định tổ chức đích được yêu cầu
        UUID finalTargetOrgId = (targetOrganizationId != null) ? targetOrganizationId : userOrgId;

        // 2. Kiểm tra phân quyền cách ly dữ liệu (QTN-01)
        boolean isAdmin = userDetails.getRoleCode().equals("VT-01");
        if (!isAdmin && !finalTargetOrgId.equals(userOrgId)) {
            // Ghi nhật ký truy cập trái phép (success = false)
            reportAccessLogService.logAccess(userId, userOrgId, finalTargetOrgId, "YIELD_AND_LOT_DASHBOARD", false,
                    ipAddress);
            throw new org.springframework.security.access.AccessDeniedException(
                    "Từ chối truy cập: Bạn không có quyền truy cập dữ liệu của tổ chức này.");
        }

        // Ghi nhật ký truy cập hợp lệ (success = true)
        reportAccessLogService.logAccess(userId, userOrgId, finalTargetOrgId, "YIELD_AND_LOT_DASHBOARD", true,
                ipAddress);

        // 3. Lấy dữ liệu summary & byStatus
        List<Object[]> summaryAndStatusList = productionLotRepository.getDashboardSummaryAndStatus(finalTargetOrgId,
                startDate, endDate);

        // Khởi tạo trước tất cả trạng thái về 0L để đảm bảo đầy đủ khóa trong JSON
        // response
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ProductionLotStatus status : ProductionLotStatus.values()) {
            byStatus.put(status.name(), 0L);
        }

        long totalLots = 0L;
        double totalExpectedYield = 0.0;
        double totalActualYield = 0.0;

        for (Object[] row : summaryAndStatusList) {
            ProductionLotStatus status = (ProductionLotStatus) row[0];
            Long count = (Long) row[1];
            Double expected = row[2] != null ? (Double) row[2] : 0.0;
            Double actual = row[3] != null ? (Double) row[3] : 0.0;

            byStatus.put(status.name(), count);

            // NCL-02-CN-006: lô đã hủy không tính vào tổng sản lượng đang canh tác,
            // chỉ thống kê riêng ở bucket byStatus["CANCELLED"].
            if (status == ProductionLotStatus.CANCELLED) {
                continue;
            }

            totalLots += count;
            totalExpectedYield += expected;
            totalActualYield += actual;
        }

        ProductionLotDashboardResponse.SummaryDto summary = ProductionLotDashboardResponse.SummaryDto.builder()
                .totalLots(totalLots)
                .totalExpectedYield(totalExpectedYield)
                .totalActualYield(totalActualYield)
                .build();

        // 4. Lấy dữ liệu timeSeries và gom nhóm trên Java (để DB-agnostic giữa H2 &
        // MySQL)
        List<Object[]> timeSeriesList = productionLotRepository.getDashboardTimeSeriesData(finalTargetOrgId, startDate,
                endDate);

        Map<String, ProductionLotDashboardResponse.TimeSeriesDto> timeSeriesMap = new LinkedHashMap<>();

        for (Object[] row : timeSeriesList) {
            LocalDate plantingDate = (LocalDate) row[0];
            Double expected = row[1] != null ? (Double) row[1] : 0.0;
            Double actual = row[2] != null ? (Double) row[2] : 0.0;

            String period = formatPeriod(plantingDate, groupBy);

            ProductionLotDashboardResponse.TimeSeriesDto tsDto = timeSeriesMap.computeIfAbsent(period,
                    p -> ProductionLotDashboardResponse.TimeSeriesDto.builder()
                            .period(p)
                            .lotCount(0L)
                            .expectedYield(0.0)
                            .actualYield(0.0)
                            .build());

            tsDto.setLotCount(tsDto.getLotCount() + 1);
            tsDto.setExpectedYield(tsDto.getExpectedYield() + expected);
            tsDto.setActualYield(tsDto.getActualYield() + actual);
        }

        return ProductionLotDashboardResponse.builder()
                .summary(summary)
                .byStatus(byStatus)
                .timeSeries(new ArrayList<>(timeSeriesMap.values()))
                .build();
    }

    /** Định dạng khoảng thời gian cho biểu đồ dashboard. */
    private String formatPeriod(LocalDate date, String groupBy) {
        if (groupBy == null) {
            groupBy = "MONTH";
        }
        switch (groupBy.toUpperCase()) {
            case "DAY":
                return date.toString(); // yyyy-MM-dd
            case "WEEK":
                WeekFields weekFields = WeekFields.ISO;
                int week = date.get(weekFields.weekOfWeekBasedYear());
                int year = date.get(weekFields.weekBasedYear());
                return String.format("%d-W%02d", year, week);
            case "YEAR":
                return date.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTH":
            default:
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
    }

}