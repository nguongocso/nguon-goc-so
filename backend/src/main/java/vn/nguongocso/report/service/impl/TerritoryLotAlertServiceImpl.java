package vn.nguongocso.report.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.alert.entity.Alert;
import vn.nguongocso.alert.enums.AlertType;
import vn.nguongocso.alert.event.ActivityLogEvent;
import vn.nguongocso.alert.repository.AlertRepository;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.enums.InspectionRequestStatus;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.common.PageResponse;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductFeedback;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.ProductFeedbackSeverity;
import vn.nguongocso.farm.enums.ProductFeedbackStatus;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.ProductFeedbackRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.service.AreaScopeResult;
import vn.nguongocso.organization.service.AreaScopeService;
import vn.nguongocso.publicapi.service.ReverseGeocodingService;
import vn.nguongocso.recall.entity.RecallRequest;
import vn.nguongocso.recall.enums.RecallRequestStatus;
import vn.nguongocso.recall.repository.RecallRequestRepository;
import vn.nguongocso.report.dto.response.AlertBadgeSummary;
import vn.nguongocso.report.dto.response.AlertLotDetailResponse;
import vn.nguongocso.report.dto.response.AlertLotInfoItem;
import vn.nguongocso.report.dto.response.AlertLotOrgItem;
import vn.nguongocso.report.dto.response.AlertLotSummaryResponse;
import vn.nguongocso.report.dto.response.LotAlertEvidenceDetail;
import vn.nguongocso.report.dto.response.ReadonlyChainEventItem;
import vn.nguongocso.report.enums.LotAlertType;
import vn.nguongocso.report.excel.TerritoryAlertLotExcelGenerator;
import vn.nguongocso.report.pdf.TerritoryAlertLotPdfGenerator;
import vn.nguongocso.report.service.TerritoryLotAlertService;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Triển khai dịch vụ quản lý danh sách và chi tiết lô có cảnh báo theo địa bàn. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TerritoryLotAlertServiceImpl implements TerritoryLotAlertService {

    private final AreaScopeService areaScopeService;
    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final RecallRequestRepository recallRequestRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionCriterionResultRepository inspectionCriterionResultRepository;
    private final ChainEventRepository chainEventRepository;
    private final ProductFeedbackRepository productFeedbackRepository;
    private final AlertRepository alertRepository;
    private final TerritoryAlertLotExcelGenerator excelGenerator;
    private final TerritoryAlertLotPdfGenerator pdfGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private ReverseGeocodingService reverseGeocodingService;

    /** Lấy danh sách lô sản xuất có cảnh báo phân trang theo địa bàn. */
    @Override
    public PageResponse<AlertLotSummaryResponse> getAlertLots(
        CustomUserDetails currentUser,
        LotAlertType alertType,
        UUID organizationId,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        List<UUID> unitIds,
        Pageable pageable) {

        AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(currentUser, unitIds);
        if (scope.isEmptyScope()) {
            log.info("Người dùng [{}] chưa được gán địa bàn quản lý. Trả về danh sách rỗng.", currentUser.getUsername());
            return buildEmptyPageResponse(pageable);
        }

        List<AlertLotSummaryResponse> allAlertLots = resolveAlertLots(scope, alertType, organizationId, fromDate, toDate);

        allAlertLots.sort(Comparator.comparing(AlertLotSummaryResponse::getLatestAlertTriggeredAt,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AlertLotSummaryResponse::getLotCode, Comparator.nullsLast(Comparator.naturalOrder())));

        int total = allAlertLots.size();
        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
        int pageNumber = pageable.getPageNumber();
        int fromIndex = pageNumber * pageSize;

        List<AlertLotSummaryResponse> pageItems;
        if (fromIndex >= total) {
            pageItems = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + pageSize, total);
            pageItems = allAlertLots.subList(fromIndex, toIndex);
        }

        int totalPages = (int) Math.ceil((double) total / pageSize);
        return PageResponse.<AlertLotSummaryResponse>builder()
            .items(pageItems)
            .page(pageNumber)
            .size(pageSize)
            .totalElements(total)
            .totalPages(totalPages)
            .first(pageNumber == 0)
            .last(pageNumber >= totalPages - 1 || totalPages == 0)
            .build();
    }

    /** Lấy chi tiết cảnh báo và bằng chứng của một lô sản xuất. */
    @Override
    public AlertLotDetailResponse getAlertLotDetail(
        CustomUserDetails currentUser,
        UUID lotId) {

        ProductionLot lot = productionLotRepository.findByIdWithDetails(lotId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lô sản xuất với ID: " + lotId));

        AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(currentUser, null);
        if (scope.isEmptyScope()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông tin lô ngoài địa bàn phụ trách.");
        }

        UUID lotOrgId = lot.getOrganization() != null ? lot.getOrganization().getOrganizationId() : null;
        if (scope.isFiltered() && (lotOrgId == null || !scope.getOrganizationIds().contains(lotOrgId))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông tin lô ngoài địa bàn phụ trách.");
        }

        AlertLotInfoItem lotInfo = buildLotInfoItem(lot);
        AlertLotOrgItem orgItem = buildOrgItem(lot.getOrganization());
        List<LotAlertEvidenceDetail> activeAlerts = collectEvidenceDetailsForLot(lot);
        List<ReadonlyChainEventItem> timelineEvents = collectReadonlyTimelineEvents(lot);

        return AlertLotDetailResponse.builder()
            .lotInfo(lotInfo)
            .organization(orgItem)
            .activeAlerts(activeAlerts)
            .timelineEvents(timelineEvents)
            .build();
    }

    /** Xuất báo cáo danh sách lô có cảnh báo theo địa bàn ra tệp PDF. */
    @Override
    public byte[] exportAlertLots(
        CustomUserDetails currentUser,
        LotAlertType alertType,
        UUID organizationId,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        List<UUID> unitIds) {

        AreaScopeResult scope = areaScopeService.resolveOrganizationsForReports(currentUser, unitIds);
        List<AlertLotSummaryResponse> exportLots;

        if (scope.isEmptyScope()) {
            log.info("Người dùng [{}] xuất báo cáo khi chưa có địa bàn, trả về file rỗng.", currentUser.getUsername());
            exportLots = Collections.emptyList();
        } else {
            exportLots = resolveAlertLots(scope, alertType, organizationId, fromDate, toDate);
            exportLots.sort(Comparator.comparing(AlertLotSummaryResponse::getLatestAlertTriggeredAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AlertLotSummaryResponse::getLotCode, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        LocalDate fromLocalDate = fromDate != null ? fromDate.toLocalDate() : null;
        LocalDate toLocalDate = toDate != null ? toDate.toLocalDate() : null;
        byte[] pdfBytes = pdfGenerator.generate(exportLots, currentUser.getFullName(), fromLocalDate, toLocalDate);

        try {
            eventPublisher.publishEvent(ActivityLogEvent.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .organizationId(currentUser.getOrganizationId())
                .action("EXPORT_ALERT_LOTS")
                .description("Xuất danh sách lô có cảnh báo theo địa bàn phụ trách dưới dạng PDF. Số lượng lô: " + exportLots.size())
                .entityType("REPORT")
                .entityId("TERRITORY_ALERT_LOTS")
                .timestamp(LocalDateTime.now())
                .build());
        } catch (Exception e) {
            log.warn("Không thể ghi nhận ActivityLog khi xuất báo cáo: {}", e.getMessage());
        }

        return pdfBytes;
    }

    /** Tập hợp và lọc danh sách lô có cảnh báo theo phạm vi và bộ lọc. */
    private List<AlertLotSummaryResponse> resolveAlertLots(
        AreaScopeResult scope,
        LotAlertType alertTypeFilter,
        UUID organizationIdFilter,
        LocalDateTime fromDate,
        LocalDateTime toDate) {

        Set<UUID> targetOrgIds = null;
        if (scope.isFiltered()) {
            Set<UUID> allowedOrgIds = scope.getOrganizationIds();
            if (allowedOrgIds.isEmpty()) {
                return Collections.emptyList();
            }
            if (organizationIdFilter != null) {
                if (!allowedOrgIds.contains(organizationIdFilter)) {
                    return Collections.emptyList();
                }
                targetOrgIds = Set.of(organizationIdFilter);
            } else {
                targetOrgIds = allowedOrgIds;
            }
        } else if (scope.isAll()) {
            if (organizationIdFilter != null) {
                targetOrgIds = Set.of(organizationIdFilter);
            }
        }

        List<ProductionLot> lots;
        if (targetOrgIds != null) {
            lots = productionLotRepository.findAllInOrganizationsWithDetails(targetOrgIds);
        } else {
            lots = productionLotRepository.findAllWithDetails();
        }

        if (lots.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> lotIds = lots.stream().map(ProductionLot::getId).toList();

        Map<UUID, List<AlertBadgeSummary>> badgesByLot = new HashMap<>();
        Map<UUID, Set<LotAlertType>> alertTypesByLot = new HashMap<>();
        Map<UUID, LocalDateTime> latestTriggeredByLot = new HashMap<>();

        for (UUID lotId : lotIds) {
            badgesByLot.put(lotId, new ArrayList<>());
            alertTypesByLot.put(lotId, new HashSet<>());
        }

        for (ProductionLot pl : lots) {
            if (pl.getStatus() == ProductionLotStatus.RECALLED) {
                LocalDateTime trigAt = pl.getUpdatedAt() != null ? pl.getUpdatedAt() : pl.getCreatedAt();
                registerAlert(pl.getId(), LotAlertType.RECALLING, "Lô đã bị thu hồi", "CRITICAL",
                    trigAt, "Lô sản xuất đang trong diện thu hồi toàn diện.",
                    badgesByLot, alertTypesByLot, latestTriggeredByLot);
            }
        }

        List<Shipment> shipments = new ArrayList<>();
        List<RecallRequest> recallRequests = new ArrayList<>();
        List<TraceCode> lockedCodes = new ArrayList<>();
        List<InspectionRequest> failedInspections = new ArrayList<>();
        List<ChainEvent> harvestEvents = new ArrayList<>();
        List<ProductFeedback> seriousFeedbacks = new ArrayList<>();
        List<Alert> expiredAlerts = new ArrayList<>();

        for (List<UUID> chunk : vn.nguongocso.common.util.QueryChunkUtils.chunkList(lotIds)) {
            shipments.addAll(shipmentRepository.findByProductionLotIdIn(chunk));
            recallRequests.addAll(recallRequestRepository.findByProductionLotIdInAndStatusIn(
                chunk, List.of(RecallRequestStatus.PENDING, RecallRequestStatus.APPROVED)));
            lockedCodes.addAll(traceCodeRepository.findByProductionLotIdsAndStatus(chunk, TraceCodeStatus.LOCKED));
            failedInspections.addAll(inspectionRequestRepository.findByProductionLotIdInAndStatus(
                chunk, InspectionRequestStatus.FAILED));
            harvestEvents.addAll(chainEventRepository.findHarvestEventsByLotIds(chunk));
            seriousFeedbacks.addAll(productFeedbackRepository.findSeriousOpenFeedbacksByLotIds(
                chunk,
                List.of(ProductFeedbackSeverity.QUALITY_SUSPECTED, ProductFeedbackSeverity.COUNTERFEIT_SUSPECTED),
                ProductFeedbackStatus.CLOSED));
            expiredAlerts.addAll(alertRepository.findByRelatedEntityIdInAndType(chunk, AlertType.INSPECTION_EXPIRED));
        }

        Map<UUID, List<Shipment>> shipmentsByLot = shipments.stream()
            .collect(Collectors.groupingBy(s -> s.getProductionLot().getId()));

        for (Map.Entry<UUID, List<Shipment>> entry : shipmentsByLot.entrySet()) {
            UUID lId = entry.getKey();
            List<Shipment> lotShipments = entry.getValue();
            boolean hasRecalledShipment = lotShipments.stream()
                .anyMatch(s -> s.getStatus() == ShipmentStatus.RECALLED);
            if (hasRecalledShipment) {
                Shipment recShipment = lotShipments.stream()
                    .filter(s -> s.getStatus() == ShipmentStatus.RECALLED)
                    .findFirst().orElse(null);
                LocalDateTime trigAt = recShipment != null && recShipment.getUpdatedAt() != null
                    ? recShipment.getUpdatedAt() : LocalDateTime.now();
                registerAlert(lId, LotAlertType.RECALLING, "Lô hàng bị thu hồi", "CRITICAL",
                    trigAt, "Có lô hàng xuất kho thuộc lô này đã bị thu hồi.",
                    badgesByLot, alertTypesByLot, latestTriggeredByLot);
            }
        }

        for (RecallRequest rr : recallRequests) {
            UUID lId = rr.getProductionLot().getId();
            LocalDateTime trigAt = rr.getApprovedAt() != null ? rr.getApprovedAt() : rr.getRequestedAt();
            String note = rr.getStatus() == RecallRequestStatus.APPROVED
                ? "Lệnh thu hồi đã duyệt: " + rr.getReason()
                : "Yêu cầu thu hồi đang chờ duyệt: " + rr.getReason();
            registerAlert(lId, LotAlertType.RECALLING, "Yêu cầu thu hồi", "CRITICAL",
                trigAt, note, badgesByLot, alertTypesByLot, latestTriggeredByLot);
        }

        Map<UUID, List<TraceCode>> lockedCodesByLot = lockedCodes.stream()
            .collect(Collectors.groupingBy(tc -> tc.getShipment().getProductionLot().getId()));
        for (Map.Entry<UUID, List<TraceCode>> entry : lockedCodesByLot.entrySet()) {
            UUID lId = entry.getKey();
            List<TraceCode> codes = entry.getValue();
            LocalDateTime trigAt = codes.stream()
                .map(tc -> tc.getLockedAt() != null ? tc.getLockedAt() : tc.getCreatedAt())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            registerAlert(lId, LotAlertType.LOCKED_LABEL, "Tem bị khóa", "HIGH",
                trigAt, "Có " + codes.size() + " tem truy xuất đã bị tạm khóa do nghi vấn vi phạm.",
                badgesByLot, alertTypesByLot, latestTriggeredByLot);
        }

        for (InspectionRequest ir : failedInspections) {
            UUID lId = ir.getProductionLot().getId();
            LocalDateTime trigAt = ir.getUpdatedAt() != null ? ir.getUpdatedAt() : ir.getCreatedAt();
            registerAlert(lId, LotAlertType.INSPECTION_FAILED, "Kiểm nghiệm không đạt", "HIGH",
                trigAt, "Phiếu kiểm nghiệm có chỉ tiêu an toàn không đạt chuẩn.",
                badgesByLot, alertTypesByLot, latestTriggeredByLot);
        }

        for (ChainEvent ce : harvestEvents) {
            if (isEarlyHarvestEvent(ce)) {
                UUID lId = extractLotIdFromEvent(ce);
                if (lId != null && badgesByLot.containsKey(lId)) {
                    LocalDateTime trigAt = ce.getRecordedAt() != null ? ce.getRecordedAt() : ce.getCreatedAt();
                    registerAlert(lId, LotAlertType.QUARANTINE_OVERWRITTEN, "Ghi đè cách ly", "MEDIUM",
                        trigAt, "Sự kiện thu hoạch ghi đè ngày cách ly bảo vệ thực vật trước thời hạn.",
                        badgesByLot, alertTypesByLot, latestTriggeredByLot);
                }
            }
        }

        for (ProductFeedback pf : seriousFeedbacks) {
            UUID lId = pf.getProductionLot().getId();
            LocalDateTime trigAt = pf.getCreatedAt();
            String note = "Phản ánh mức độ nghiêm trọng (" + pf.getSeverity() + ") đang xử lý.";
            registerAlert(lId, LotAlertType.SERIOUS_FEEDBACK_OPEN, "Phản ánh nghiêm trọng", "HIGH",
                trigAt, note, badgesByLot, alertTypesByLot, latestTriggeredByLot);
        }

        for (Alert al : expiredAlerts) {
            UUID lId = al.getRelatedEntityId();
            LocalDateTime trigAt = al.getCreatedAt();
            registerAlert(lId, LotAlertType.INSPECTION_EXPIRED, "Kiểm nghiệm hết hạn", "MEDIUM",
                trigAt, al.getMessage() != null ? al.getMessage() : "Hiệu lực kiểm nghiệm của lô đã hết hạn.",
                badgesByLot, alertTypesByLot, latestTriggeredByLot);
        }

        List<AlertLotSummaryResponse> result = new ArrayList<>();
        for (ProductionLot lot : lots) {
            UUID lId = lot.getId();
            Set<LotAlertType> activeTypes = alertTypesByLot.getOrDefault(lId, Collections.emptySet());

            if (activeTypes.isEmpty()) {
                continue;
            }

            if (alertTypeFilter != null && !activeTypes.contains(alertTypeFilter)) {
                continue;
            }

            LocalDateTime latestTriggered = latestTriggeredByLot.get(lId);

            if (fromDate != null && latestTriggered != null && latestTriggered.isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && latestTriggered != null && latestTriggered.isAfter(toDate)) {
                continue;
            }

            List<LotAlertType> sortedTypes = new ArrayList<>(activeTypes);
            sortedTypes.sort(Comparator.comparingInt(this::getAlertPriority));

            List<AlertBadgeSummary> summaries = badgesByLot.getOrDefault(lId, Collections.emptyList());

            String lotCode = generateLotCode(lot);
            Organization org = lot.getOrganization();

            AlertLotSummaryResponse item = AlertLotSummaryResponse.builder()
                .lotId(lId)
                .lotCode(lotCode)
                .lotName(lot.getName())
                .organizationId(org != null ? org.getOrganizationId() : null)
                .organizationName(org != null ? org.getName() : null)
                .productCategoryId(lot.getProductCategory() != null ? lot.getProductCategory().getId() : null)
                .productCategoryName(lot.getProductCategory() != null ? lot.getProductCategory().getName() : null)
                .farmAreaName(lot.getFarmArea() != null ? lot.getFarmArea().getName() : null)
                .communeName(org != null && org.getCommune() != null ? org.getCommune().getName() : null)
                .provinceName(org != null && org.getProvince() != null ? org.getProvince().getName() : null)
                .lotStatus(lot.getStatus() != null ? lot.getStatus().name() : null)
                .alertTypes(sortedTypes)
                .primaryAlertType(!sortedTypes.isEmpty() ? sortedTypes.get(0) : null)
                .alertCount(sortedTypes.size())
                .latestAlertTriggeredAt(latestTriggered)
                .alertSummaries(summaries)
                .createdAt(lot.getCreatedAt())
                .build();

            result.add(item);
        }

        return result;
    }

    /** Đăng ký thông tin cảnh báo vào danh sách hiển thị của lô. */
    private void registerAlert(
        UUID lotId,
        LotAlertType type,
        String alertName,
        String severity,
        LocalDateTime triggeredAt,
        String briefNote,
        Map<UUID, List<AlertBadgeSummary>> badgesByLot,
        Map<UUID, Set<LotAlertType>> alertTypesByLot,
        Map<UUID, LocalDateTime> latestTriggeredByLot) {

        Set<LotAlertType> types = alertTypesByLot.get(lotId);
        if (types != null) {
            types.add(type);
        }

        List<AlertBadgeSummary> badges = badgesByLot.get(lotId);
        if (badges != null) {
            boolean exists = badges.stream().anyMatch(b -> b.getAlertType() == type);
            if (!exists) {
                badges.add(AlertBadgeSummary.builder()
                    .alertType(type)
                    .alertName(alertName)
                    .severity(severity)
                    .triggeredAt(triggeredAt)
                    .briefNote(briefNote)
                    .build());
            }
        }

        LocalDateTime currentLatest = latestTriggeredByLot.get(lotId);
        if (triggeredAt != null) {
            if (currentLatest == null || triggeredAt.isAfter(currentLatest)) {
                latestTriggeredByLot.put(lotId, triggeredAt);
            }
        }
    }

    /** Lấy độ ưu tiên hiển thị của từng loại cảnh báo. */
    private int getAlertPriority(LotAlertType type) {
        return switch (type) {
            case RECALLING -> 1;
            case LOCKED_LABEL -> 2;
            case INSPECTION_FAILED -> 3;
            case SERIOUS_FEEDBACK_OPEN -> 4;
            case QUARANTINE_OVERWRITTEN -> 5;
            case INSPECTION_EXPIRED -> 6;
        };
    }

    /** Sinh mã hiển thị chuẩn cho lô sản xuất. */
    private String generateLotCode(ProductionLot lot) {
        if (lot.getId() == null) {
            return "LOT-UNKNOWN";
        }
        return "LOT-" + lot.getId().toString().substring(0, 8).toUpperCase();
    }

    /** Kiểm tra sự kiện thu hoạch có vi phạm thời gian cách ly hay không. */
    private boolean isEarlyHarvestEvent(ChainEvent event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(event.getEventData());
            if (root.has("earlyHarvest") && root.get("earlyHarvest").asBoolean()) {
                return true;
            }
            if (root.has("isEarlyHarvest") && root.get("isEarlyHarvest").asBoolean()) {
                return true;
            }
            if (root.has("quarantineOverwritten") && root.get("quarantineOverwritten").asBoolean()) {
                return true;
            }
        } catch (Exception e) {
            String data = event.getEventData();
            return data.contains("\"earlyHarvest\":true") || data.contains("\"earlyHarvest\": true");
        }
        return false;
    }

    /** Trích xuất ID lô sản xuất từ dữ liệu sự kiện. */
    private UUID extractLotIdFromEvent(ChainEvent event) {
        if (event.getShipment() != null && event.getShipment().getProductionLot() != null) {
            return event.getShipment().getProductionLot().getId();
        }
        if (event.getEventData() != null) {
            try {
                JsonNode root = objectMapper.readTree(event.getEventData());
                if (root.has("productionLotId")) {
                    return UUID.fromString(root.get("productionLotId").asText());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** Trích xuất và giải mã địa chỉ vùng trồng từ tọa độ Point. */
    private String resolveFarmAreaAddress(FarmArea farmArea) {
        if (farmArea == null || farmArea.getLocation() == null) {
            return null;
        }
        double lat = farmArea.getLocation().getY();
        double lon = farmArea.getLocation().getX();
        try {
            if (reverseGeocodingService != null) {
                String geocoded = reverseGeocodingService.reverseGeocode(lat, lon);
                if (geocoded != null && !geocoded.isBlank()) {
                    return geocoded;
                }
            }
        } catch (Exception e) {
            log.warn("Không thể giải mã địa chỉ từ tọa độ vùng trồng: lat={}, lon={}", lat, lon, e);
        }
        return String.format(Locale.US, "Tọa độ: %.5f, %.5f", lat, lon);
    }

    /** Xây dựng thông tin cơ bản cho lô sản xuất. */
    private AlertLotInfoItem buildLotInfoItem(ProductionLot lot) {
        return AlertLotInfoItem.builder()
            .lotId(lot.getId())
            .lotCode(generateLotCode(lot))
            .lotName(lot.getName())
            .status(lot.getStatus() != null ? lot.getStatus().name() : null)
            .expectedQuantity(lot.getExpectedQuantity())
            .actualQuantity(lot.getActualQuantity())
            .quantityUnit(lot.getExpectedQuantityUnit())
            .plantingDate(lot.getPlantingDate())
            .harvestDate(lot.getHarvestDate())
            .productCategoryName(lot.getProductCategory() != null ? lot.getProductCategory().getName() : null)
            .farmAreaName(lot.getFarmArea() != null ? lot.getFarmArea().getName() : null)
            .farmAreaAddress(resolveFarmAreaAddress(lot.getFarmArea()))
            .createdAt(lot.getCreatedAt())
            .build();
    }

    /** Xây dựng thông tin tổ chức cho lô sản xuất. */
    private AlertLotOrgItem buildOrgItem(Organization org) {
        if (org == null) {
            return null;
        }
        return AlertLotOrgItem.builder()
            .organizationId(org.getOrganizationId())
            .organizationName(org.getName())
            .taxCode(org.getCode())
            .address(org.getAddress())
            .contactPhone(org.getPhone())
            .communeName(org.getCommune() != null ? org.getCommune().getName() : null)
            .provinceName(org.getProvince() != null ? org.getProvince().getName() : null)
            .build();
    }

    /** Thu thập danh sách bằng chứng chi tiết các cảnh báo của lô. */
    private List<LotAlertEvidenceDetail> collectEvidenceDetailsForLot(ProductionLot lot) {
        UUID lotId = lot.getId();
        List<LotAlertEvidenceDetail> details = new ArrayList<>();

        boolean isRecalled = lot.getStatus() == ProductionLotStatus.RECALLED;
        List<Shipment> shipments = shipmentRepository.findByProductionLotId(lotId);
        List<Shipment> recalledShipments = shipments.stream()
            .filter(s -> s.getStatus() == ShipmentStatus.RECALLED || s.getStatus() == ShipmentStatus.RECALLING)
            .toList();
        boolean hasRecalledShipment = !recalledShipments.isEmpty();
        List<RecallRequest> recalls = recallRequestRepository.findByProductionLotIdInAndStatusIn(
            List.of(lotId), List.of(RecallRequestStatus.PENDING, RecallRequestStatus.APPROVED));

        if (isRecalled || hasRecalledShipment || !recalls.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("lotStatus", lot.getStatus() != null ? lot.getStatus().name() : null);
            data.put("hasRecalledShipment", hasRecalledShipment);
            data.put("affectedStage", "Khâu xuất kho & Lưu thông thương mại");
            data.put("traceRule", "Truy vết xuôi (Downstream): Cảnh báo phát sinh từ lô hàng xuất kho phái sinh hoặc quyết định thu hồi toàn lô");

            if (isRecalled) {
                data.put("recallScope", "Toàn bộ lô sản xuất (" + (lot.getName() != null ? lot.getName() : lot.getId()) + ")");
            } else if (hasRecalledShipment) {
                data.put("recallScope", recalledShipments.size() + " lô hàng xuất kho");
            }

            if (!recalledShipments.isEmpty()) {
                List<Map<String, Object>> recalledShipmentItems = recalledShipments.stream()
                    .map(s -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", s.getId());
                        item.put("name", s.getName());
                        item.put("quantity", s.getTotalQuantity());
                        item.put("status", s.getStatus() != null ? s.getStatus().name() : null);
                        item.put("packagingInfo", s.getPackagingInfo());
                        return item;
                    })
                    .toList();
                data.put("recalledShipmentList", recalledShipmentItems);
                data.put("recalledShipmentNames", recalledShipments.stream().map(Shipment::getName).toList());
            }

            if (!recalls.isEmpty()) {
                RecallRequest latest = recalls.get(0);
                data.put("recallRequestId", latest.getId());
                data.put("recallReason", latest.getReason());
                data.put("recallStatus", latest.getStatus().name());
                data.put("requestedAt", latest.getRequestedAt());
                data.put("approvedAt", latest.getApprovedAt());
                if (latest.getShipment() != null) {
                    data.put("targetShipmentName", latest.getShipment().getName());
                }
            }

            LocalDateTime trigAt = !recalls.isEmpty()
                ? (recalls.get(0).getApprovedAt() != null ? recalls.get(0).getApprovedAt() : recalls.get(0).getRequestedAt())
                : (lot.getUpdatedAt() != null ? lot.getUpdatedAt() : lot.getCreatedAt());

            details.add(LotAlertEvidenceDetail.builder()
                .alertType(LotAlertType.RECALLING)
                .severity("CRITICAL")
                .triggeredAt(trigAt)
                .title("Cảnh báo thu hồi lô hàng")
                .message("Lô sản xuất đang có quyết định hoặc yêu cầu thu hồi sản phẩm.")
                .evidenceData(data)
                .build());
        }

        List<TraceCode> lockedCodes = traceCodeRepository.findByProductionLotIdsAndStatus(
            List.of(lotId), TraceCodeStatus.LOCKED);
        if (!lockedCodes.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("lockedCount", lockedCodes.size());
            data.put("affectedStage", "Khâu kích hoạt & Lưu thông tem truy xuất");
            data.put("traceRule", "Truy vết xuôi (Downstream): Khóa tem nghi vấn giả mạo hoặc bất thường lượt quét");
            List<String> sampleCodes = lockedCodes.stream()
                .limit(10)
                .map(TraceCode::getCodeValue)
                .toList();
            data.put("sampleCodes", sampleCodes);

            LocalDateTime trigAt = lockedCodes.stream()
                .map(tc -> tc.getLockedAt() != null ? tc.getLockedAt() : tc.getCreatedAt())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

            details.add(LotAlertEvidenceDetail.builder()
                .alertType(LotAlertType.LOCKED_LABEL)
                .severity("HIGH")
                .triggeredAt(trigAt)
                .title("Cảnh báo tem truy xuất bị khóa")
                .message("Phát hiện " + lockedCodes.size() + " tem truy xuất thuộc lô đang bị khóa do nghi vấn vi phạm.")
                .evidenceData(data)
                .build());
        }

        List<InspectionRequest> failedInspections = inspectionRequestRepository.findByProductionLotIdInAndStatus(
            List.of(lotId), InspectionRequestStatus.FAILED);
        if (!failedInspections.isEmpty()) {
            InspectionRequest first = failedInspections.get(0);
            Map<String, Object> data = new HashMap<>();
            data.put("inspectionRequestId", first.getId());
            data.put("inspectionUnit", first.getInspectionUnit());
            data.put("sampleSentDate", first.getSampleSentDate());
            data.put("scopeWarningDetails", first.getScopeWarningDetails());
            data.put("affectedStage", "Khâu kiểm nghiệm chất lượng & An toàn thực phẩm");
            data.put("traceRule", "Truy vết tại khâu kiểm định chất lượng: Đối chiếu kết quả phân tích phòng Lab với ngưỡng tiêu chuẩn");

            List<InspectionCriterionResult> criterionResults = inspectionCriterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(first.getId());

            int totalCriteria = first.getCriteria() != null && !first.getCriteria().isEmpty()
                ? first.getCriteria().size()
                : criterionResults.size();

            List<InspectionCriterionResult> failedResults = criterionResults.stream()
                .filter(r -> Boolean.FALSE.equals(r.getPassed()))
                .toList();

            int failedCount = failedResults.size();
            int passedCount = (int) criterionResults.stream()
                .filter(r -> Boolean.TRUE.equals(r.getPassed()))
                .count();

            List<String> failedCriterionNames = failedResults.stream()
                .map(r -> r.getInspectionCriterion() != null ? r.getInspectionCriterion().getCriterionName() : "Chỉ tiêu không đạt")
                .distinct()
                .toList();

            if (failedCriterionNames.isEmpty() && first.getCriteria() != null && !first.getCriteria().isEmpty()) {
                failedCriterionNames = first.getCriteria().stream()
                    .map(InspectionCriterion::getCriterionName)
                    .distinct()
                    .toList();
                if (failedCount == 0) {
                    failedCount = failedCriterionNames.size();
                }
            }

            int finalTotal = totalCriteria > 0 ? totalCriteria : (failedCount > 0 ? failedCount : 1);
            int finalFailed = failedCount > 0 ? failedCount : 1;
            data.put("failedRatio", finalFailed + "/" + finalTotal + " chỉ tiêu không đạt");
            data.put("failedCriteriaCount", finalFailed);
            data.put("totalCriteriaCount", finalTotal);
            data.put("passedCriteriaCount", passedCount);
            data.put("failedCriteriaNames", failedCriterionNames);

            details.add(LotAlertEvidenceDetail.builder()
                .alertType(LotAlertType.INSPECTION_FAILED)
                .severity("HIGH")
                .triggeredAt(first.getUpdatedAt() != null ? first.getUpdatedAt() : first.getCreatedAt())
                .title("Cảnh báo kiểm nghiệm không đạt tiêu chuẩn")
                .message("Kết quả kiểm nghiệm chất lượng / an toàn thực phẩm không đạt yêu cầu.")
                .evidenceData(data)
                .build());
        }

        List<ChainEvent> harvestEvents = chainEventRepository.findHarvestEventsByLotIds(List.of(lotId));
        Optional<ChainEvent> earlyEventOpt = harvestEvents.stream()
            .filter(ce -> lotId.equals(extractLotIdFromEvent(ce)) && isEarlyHarvestEvent(ce))
            .findFirst();
        if (earlyEventOpt.isPresent()) {
            ChainEvent ev = earlyEventOpt.get();
            Map<String, Object> data = new HashMap<>();
            data.put("eventId", ev.getId());
            data.put("recordedAt", ev.getRecordedAt());
            data.put("rawEventData", ev.getEventData());
            data.put("affectedStage", "Khâu canh tác & Thu hoạch nông sản");
            data.put("traceRule", "Truy vết ngược (Upstream): Đối chiếu thời gian cách ly thuốc BVTV với nhật ký thu hoạch thực tế");

            details.add(LotAlertEvidenceDetail.builder()
                .alertType(LotAlertType.QUARANTINE_OVERWRITTEN)
                .severity("MEDIUM")
                .triggeredAt(ev.getRecordedAt() != null ? ev.getRecordedAt() : ev.getCreatedAt())
                .title("Cảnh báo ghi đè thời gian cách ly")
                .message("Sản phẩm được thu hoạch khi chưa hết thời gian cách ly thuốc bảo vệ thực vật theo quy chuẩn.")
                .evidenceData(data)
                .build());
        }

        List<ProductFeedback> seriousFeedbacks = productFeedbackRepository.findSeriousOpenFeedbacksByLotIds(
            List.of(lotId),
            List.of(ProductFeedbackSeverity.QUALITY_SUSPECTED, ProductFeedbackSeverity.COUNTERFEIT_SUSPECTED),
            ProductFeedbackStatus.CLOSED);
        if (!seriousFeedbacks.isEmpty()) {
            ProductFeedback fb = seriousFeedbacks.get(0);
            Map<String, Object> data = new HashMap<>();
            data.put("feedbackId", fb.getId());
            data.put("severity", fb.getSeverity().name());
            data.put("content", fb.getContent());
            data.put("feedbackStatus", fb.getStatus().name());
            data.put("openFeedbackCount", seriousFeedbacks.size());
            data.put("affectedStage", "Khâu lưu thông & Phản ánh thị trường");
            data.put("traceRule", "Truy vết hai chiều (Bi-directional): Khoanh vùng lô sản xuất từ phản ánh của người tiêu dùng");

            details.add(LotAlertEvidenceDetail.builder()
                .alertType(LotAlertType.SERIOUS_FEEDBACK_OPEN)
                .severity("HIGH")
                .triggeredAt(fb.getCreatedAt())
                .title("Cảnh báo phản ánh nghiêm trọng từ người tiêu dùng")
                .message("Có phản ánh nghiêm trọng về chất lượng hoặc nghi vấn hàng giả chưa được giải quyết dứt điểm.")
                .evidenceData(data)
                .build());
        }

        List<Alert> expiredAlerts = alertRepository.findByRelatedEntityIdInAndType(
            List.of(lotId), AlertType.INSPECTION_EXPIRED);
        if (!expiredAlerts.isEmpty()) {
            Alert al = expiredAlerts.get(0);
            Map<String, Object> data = new HashMap<>();
            data.put("alertId", al.getId());
            data.put("message", al.getMessage());
            data.put("createdAt", al.getCreatedAt());
            data.put("affectedStage", "Khâu quản lý hiệu lực chứng nhận");
            data.put("traceRule", "Truy vết thời hạn hiệu lực của phiếu kết quả kiểm nghiệm");

            details.add(LotAlertEvidenceDetail.builder()
                .alertType(LotAlertType.INSPECTION_EXPIRED)
                .severity("MEDIUM")
                .triggeredAt(al.getCreatedAt())
                .title("Cảnh báo kiểm nghiệm hết hiệu lực")
                .message(al.getMessage() != null ? al.getMessage() : "Hiệu lực kiểm nghiệm đã hết hạn nhưng lô vẫn lưu hành.")
                .evidenceData(data)
                .build());
        }

        return details;
    }

    /** Thu thập dòng lịch sử sự kiện dạng chỉ đọc của lô sản xuất. */
    private List<ReadonlyChainEventItem> collectReadonlyTimelineEvents(ProductionLot lot) {
        List<Shipment> shipments = shipmentRepository.findByProductionLotId(lot.getId());
        List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).toList();

        List<ChainEvent> shipmentEvents;
        if (!shipmentIds.isEmpty()) {
            shipmentEvents = chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);
        } else {
            shipmentEvents = Collections.emptyList();
        }

        List<ChainEvent> unassignedEvents = chainEventRepository.findByShipmentIsNullAndEventTypeIn(
            List.of(ChainEventType.HARVEST, ChainEventType.PREPROCESSING, ChainEventType.PACKAGING));
        List<ChainEvent> lotUnassignedEvents = unassignedEvents.stream()
            .filter(e -> lot.getId().equals(extractLotIdFromEvent(e)))
            .toList();

        Set<UUID> existingIds = new HashSet<>();
        List<ChainEvent> combined = new ArrayList<>();
        for (ChainEvent se : shipmentEvents) {
            if (existingIds.add(se.getId())) {
                combined.add(se);
            }
        }
        for (ChainEvent ue : lotUnassignedEvents) {
            if (existingIds.add(ue.getId())) {
                combined.add(ue);
            }
        }
        combined.sort(Comparator.comparing(ChainEvent::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return combined.stream()
            .map(e -> {
                boolean isEarly = isEarlyHarvestEvent(e);
                boolean isRecalledShipmentEvent = e.getShipment() != null &&
                    (e.getShipment().getStatus() == ShipmentStatus.RECALLED || e.getShipment().getStatus() == ShipmentStatus.RECALLING);
                boolean hasAlert = isEarly || isRecalledShipmentEvent;
                String alertWarning = null;
                if (isEarly) {
                    alertWarning = "Thu hoạch sớm trước thời gian cách ly thuốc BVTV";
                } else if (isRecalledShipmentEvent) {
                    alertWarning = "Thu thuộc lô hàng '" + e.getShipment().getName() + "' đang bị thu hồi";
                }

                return ReadonlyChainEventItem.builder()
                    .eventId(e.getId())
                    .eventType(e.getEventType() != null ? e.getEventType().name() : null)
                    .eventTypeName(getEventTypeName(e))
                    .recordedAt(e.getRecordedAt())
                    .recordedByName(e.getRecordedBy() != null ? e.getRecordedBy().getFullName() : null)
                    .location(formatEventLocation(e.getLocation()))
                    .earlyHarvest(isEarly)
                    .hasAlert(hasAlert)
                    .alertWarning(alertWarning)
                    .shipmentName(e.getShipment() != null ? e.getShipment().getName() : null)
                    .description(extractEventDescription(e))
                    .build();
            })
            .toList();
    }

    /** Định dạng tọa độ sự kiện thành chuỗi hiển thị. */
    private String formatEventLocation(Point point) {
        if (point == null) {
            return null;
        }
        return String.format(Locale.US, "Tọa độ: %s, %s",
            BigDecimal.valueOf(point.getX()).stripTrailingZeros().toPlainString(),
            BigDecimal.valueOf(point.getY()).stripTrailingZeros().toPlainString());
    }

    /** Lấy tên hiển thị tiếng Việt của loại sự kiện. */
    private String getEventTypeName(ChainEvent e) {
        if (e.getEventType() == null) {
            return "Sự kiện chuỗi cung ứng";
        }
        return switch (e.getEventType()) {
            case HARVEST -> "Thu hoạch";
            case PREPROCESSING -> "Sơ chế";
            case PACKAGING -> "Đóng gói";
            case WAREHOUSE_RECEIPT -> "Nhập kho";
            case TRANSPORT -> "Vận chuyển";
            case PROCUREMENT -> "Thu mua";
            default -> e.getEventType().name();
        };
    }

    /** Trích xuất mô tả ghi chú từ dữ liệu sự kiện. */
    private String extractEventDescription(ChainEvent e) {
        if (e.getEventData() == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(e.getEventData());
            if (node.has("notes")) {
                return node.get("notes").asText();
            }
            if (node.has("description")) {
                return node.get("description").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Tạo phản hồi phân trang rỗng. */
    private PageResponse<AlertLotSummaryResponse> buildEmptyPageResponse(Pageable pageable) {
        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
        int pageNumber = pageable.getPageNumber();
        return PageResponse.<AlertLotSummaryResponse>builder()
            .items(Collections.emptyList())
            .page(pageNumber)
            .size(pageSize)
            .totalElements(0)
            .totalPages(0)
            .first(true)
            .last(true)
            .build();
    }
}
