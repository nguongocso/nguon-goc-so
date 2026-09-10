package vn.nguongocso.trace.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.entity.OrganizationUser;
import vn.nguongocso.organization.repository.OrganizationUserRepository;
import vn.nguongocso.report.entity.TraceCodeScanLog;
import vn.nguongocso.report.repository.TraceCodeScanLogRepository;
import vn.nguongocso.trace.dto.response.*;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.entity.TraceCode;
import vn.nguongocso.trace.enums.ShipmentStatus;
import vn.nguongocso.trace.enums.TraceCodeStatus;
import vn.nguongocso.trace.repository.ShipmentRepository;
import vn.nguongocso.trace.repository.TraceCodeRepository;
import vn.nguongocso.trace.service.ImpactScopeTraceService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImpactScopeTraceServiceImpl implements ImpactScopeTraceService {

    private final ProductionLotRepository productionLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final TraceCodeRepository traceCodeRepository;
    private final ChainEventRepository chainEventRepository;
    private final TraceCodeScanLogRepository traceCodeScanLogRepository;
    private final OrganizationUserRepository organizationUserRepository;

    @Override
    @Transactional(readOnly = true)
    public ImpactScopeTraceResponse getImpactScopeTrace(String code, CustomUserDetails currentUser) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("Mã tìm kiếm không được để trống.");
        }

        String searchCode = code.trim();
        UUID uuidCode = tryParseUUID(searchCode);

        String rootNodeType = null;
        ProductionLot productionLot = null;

        // 1. Tìm theo TraceCode
        Optional<TraceCode> traceCodeOpt = traceCodeRepository.findByCodeValue(searchCode);
        if (!traceCodeOpt.isPresent() && uuidCode != null) {
            traceCodeOpt = traceCodeRepository.findById(uuidCode);
        }

        if (traceCodeOpt.isPresent()) {
            rootNodeType = "TRACE_CODE";
            Shipment shipment = traceCodeOpt.get().getShipment();
            if (shipment != null) {
                productionLot = shipment.getProductionLot();
            }
        }

        // 2. Nếu chưa thấy, tìm theo Shipment
        if (productionLot == null) {
            Optional<Shipment> shipmentOpt = Optional.empty();
            if (uuidCode != null) {
                shipmentOpt = shipmentRepository.findById(uuidCode);
            }
            if (!shipmentOpt.isPresent()) {
                List<Shipment> eligibleShipments = shipmentRepository.findEligibleShipments(null, null, null, null, null);
                shipmentOpt = eligibleShipments.stream()
                        .filter(s -> searchCode.equalsIgnoreCase(s.getName()))
                        .findFirst();
            }

            if (shipmentOpt.isPresent()) {
                rootNodeType = "SHIPMENT";
                productionLot = shipmentOpt.get().getProductionLot();
            }
        }

        // 3. Nếu chưa thấy, tìm theo ProductionLot
        if (productionLot == null) {
            Optional<ProductionLot> lotOpt = Optional.empty();
            if (uuidCode != null) {
                lotOpt = productionLotRepository.findById(uuidCode);
            }
            if (!lotOpt.isPresent()) {
                List<ProductionLot> allLots = productionLotRepository.findAll();
                lotOpt = allLots.stream()
                        .filter(l -> searchCode.equalsIgnoreCase(l.getName()))
                        .findFirst();
            }

            if (lotOpt.isPresent()) {
                rootNodeType = "PRODUCTION_LOT";
                productionLot = lotOpt.get();
            }
        }

        // Kịch bản TC-05: Mã không tồn tại
        if (productionLot == null) {
            throw new BusinessException("Mã truy vết không tồn tại trên hệ thống. Vui lòng kiểm tra lại mã lô sản xuất, lô hàng hoặc tem.");
        }

        // Kịch bản QTN-01: Cách ly dữ liệu tổ chức
        UUID lotOrgId = productionLot.getOrganization() != null ? productionLot.getOrganization().getOrganizationId() : null;
        boolean isAdmin = currentUser != null && "VT-01".equals(currentUser.getRoleCode());
        if (!isAdmin && currentUser != null && currentUser.getOrganizationId() != null) {
            if (!currentUser.getOrganizationId().equals(lotOrgId)) {
                throw new BusinessException("Bạn không có quyền xem thông tin truy vết của đối tượng thuộc tổ chức khác.");
            }
        }

        // Dựng Nút Upstream 1: Vùng trồng (FarmArea)
        FarmAreaTraceDto farmAreaDto = null;
        FarmArea farmArea = productionLot.getFarmArea();
        if (farmArea != null) {
            String locationStr = farmArea.getLocation() != null ? farmArea.getLocation().toString() : null;
            Double areaVal = farmArea.getArea() != null ? farmArea.getArea().doubleValue() : null;
            farmAreaDto = FarmAreaTraceDto.builder()
                    .id(farmArea.getId())
                    .code(farmArea.getId().toString().substring(0, 8))
                    .name(farmArea.getName())
                    .location(locationStr)
                    .areaSize(areaVal)
                    .build();
        }

        // Dựng Nút Upstream 2: Lô sản xuất (ProductionLot)
        ProductionLotTraceDto productionLotDto = ProductionLotTraceDto.builder()
                .id(productionLot.getId())
                .code(productionLot.getName())
                .name(productionLot.getName())
                .status(productionLot.getStatus())
                .expectedQuantity(productionLot.getExpectedQuantity())
                .expectedQuantityUnit(productionLot.getExpectedQuantityUnit())
                .actualQuantity(productionLot.getActualQuantity())
                .plantingDate(productionLot.getPlantingDate())
                .harvestDate(productionLot.getHarvestDate())
                .build();

        // Dựng Nhánh Xuôi (Downstream): Lô hàng (Shipments)
        List<Shipment> shipments = shipmentRepository.findByProductionLotId(productionLot.getId());
        List<ShipmentTraceDto> shipmentDtos = new ArrayList<>();

        long totalActivatedStampsAll = 0;
        Set<UUID> receivingOrgIds = new HashSet<>();
        long recalledShipmentsCount = 0;

        for (Shipment s : shipments) {
            if (s.getStatus() == ShipmentStatus.RECALLED) {
                recalledShipmentsCount++;
            }

            // Đếm tem đã kích hoạt
            List<TraceCode> traceCodes = traceCodeRepository.findByShipmentId(s.getId());
            long activatedCount = traceCodes.stream()
                    .filter(tc -> tc.getStatus() != TraceCodeStatus.INACTIVE)
                    .count();
            totalActivatedStampsAll += activatedCount;

            // Thống kê lượt quét
            long totalScans = traceCodeScanLogRepository.countScans(null, null, s.getId(), null, null);
            long suspectCount = traceCodeScanLogRepository.countAbnormalScans(null, null, s.getId(), null, null);

            LocalDateTime recentScanAt = null;
            List<TraceCodeScanLog> recentLogs = traceCodeScanLogRepository.findByTraceCodeIdAndScannedAtAfterOrderByScannedAtDesc(
                    null, LocalDateTime.now().minusYears(10));
            if (!recentLogs.isEmpty()) {
                recentScanAt = recentLogs.get(0).getScannedAt();
            }

            ScanStatsTraceDto scanStats = ScanStatsTraceDto.builder()
                    .totalScans(totalScans)
                    .recentScanAt(recentScanAt)
                    .suspectCount(suspectCount)
                    .build();

            // Lấy danh sách tất cả các sự kiện của lô hàng
            List<ChainEvent> events = chainEventRepository.findByShipmentIdOrderByRecordedAtAsc(s.getId());
            List<ChainEventTraceDto> eventDtos = new ArrayList<>();
            List<ReceivingOrganizationTraceDto> receivingOrgs = new ArrayList<>();

            for (ChainEvent ev : events) {
                // Map sự kiện sang DTO
                String locationStr = ev.getLocation() != null ? ev.getLocation().toString() : null;
                eventDtos.add(ChainEventTraceDto.builder()
                        .id(ev.getId())
                        .eventType(ev.getEventType())
                        .eventTypeName(getEventTypeName(ev.getEventType()))
                        .recordedAt(ev.getRecordedAt())
                        .location(locationStr)
                        .isCorrection(ev.isCorrection())
                        .build());

                // Xác định tổ chức nhận (PROCUREMENT, WAREHOUSE_RECEIPT)
                ChainEventType type = ev.getEventType();
                if (type == ChainEventType.PROCUREMENT || type == ChainEventType.WAREHOUSE_RECEIPT) {
                    if (ev.getRecordedBy() != null) {
                        Optional<OrganizationUser> ouOpt = organizationUserRepository.findFirstByUser(ev.getRecordedBy());
                        if (ouOpt.isPresent() && ouOpt.get().getOrganization() != null) {
                            Organization recOrg = ouOpt.get().getOrganization();
                            // Chỉ thêm vào tổ chức đã nhận đối tác (bên thứ ba) nếu khác tổ chức sở hữu lô sản xuất gốc
                            if (lotOrgId != null && !lotOrgId.equals(recOrg.getOrganizationId())) {
                                receivingOrgIds.add(recOrg.getOrganizationId());

                                Long recQty = null;
                                if (ev.getEventData() != null) {
                                    try {
                                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(ev.getEventData());
                                        if (node.has("receivedQuantity")) {
                                            recQty = node.get("receivedQuantity").asLong();
                                        } else if (node.has("quantity")) {
                                            recQty = node.get("quantity").asLong();
                                        }
                                    } catch (Exception ignored) {}
                                }
                                if (recQty == null) {
                                    recQty = s.getTotalQuantity();
                                }

                                receivingOrgs.add(ReceivingOrganizationTraceDto.builder()
                                        .organizationId(recOrg.getOrganizationId())
                                        .organizationName(recOrg.getName()) // Đáp ứng TC-04: Chỉ hiển thị tên & thời điểm
                                        .receivedAt(ev.getRecordedAt())
                                        .receivedQuantity(recQty)
                                        .eventType(type)
                                        .eventTypeName(getEventTypeName(type))
                                        .build());
                            }
                        }
                    }
                }
            }

            shipmentDtos.add(ShipmentTraceDto.builder()
                    .id(s.getId())
                    .code(s.getName())
                    .name(s.getName())
                    .status(s.getStatus())
                    .totalQuantity(s.getTotalQuantity())
                    .packagingInfo(s.getPackagingInfo())
                    .createdAt(s.getCreatedAt())
                    .activatedStampsCount(activatedCount)
                    .scanStats(scanStats)
                    .events(eventDtos)
                    .receivingOrganizations(receivingOrgs)
                    .build());
        }

        ImpactScopeSummaryDto summary = ImpactScopeSummaryDto.builder()
                .totalShipments(shipmentDtos.size())
                .totalActivatedStamps(totalActivatedStampsAll)
                .totalReceivingOrganizations(receivingOrgIds.size())
                .totalRecalledShipments(recalledShipmentsCount)
                .build();

        return ImpactScopeTraceResponse.builder()
                .rootNodeType(rootNodeType != null ? rootNodeType : "PRODUCTION_LOT")
                .searchedCode(searchCode)
                .farmArea(farmAreaDto)
                .productionLot(productionLotDto)
                .shipments(shipmentDtos)
                .summary(summary)
                .build();
    }

    private String getEventTypeName(ChainEventType type) {
        if (type == null) return "";
        return switch (type) {
            case TRANSPORT -> "Vận chuyển";
            case PROCUREMENT -> "Thu mua";
            case WAREHOUSE_RECEIPT -> "Nhập kho";
            case PACKAGING -> "Đóng gói";
            case PREPROCESSING -> "Sơ chế";
            case HARVEST -> "Thu hoạch";
            case STORAGE_CONDITION -> "Bảo quản";
            case CORRECTION -> "Đính chính";
            case WAREHOUSE_ENTRY -> "Nhập kho HTX";
            case WAREHOUSE_EXIT -> "Xuất kho HTX";
            case HANDOVER -> "Bàn giao";
            default -> type.name();
        };
    }

    private UUID tryParseUUID(String val) {
        try {
            return UUID.fromString(val);
        } catch (Exception e) {
            return null;
        }
    }
}
