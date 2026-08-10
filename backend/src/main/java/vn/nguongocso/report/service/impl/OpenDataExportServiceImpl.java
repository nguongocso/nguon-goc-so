package vn.nguongocso.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.auth.service.CustomUserDetails;
import vn.nguongocso.exception.BusinessException;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.farm.repository.ProductionLotRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.organization.repository.OrganizationRepository;
import vn.nguongocso.export.dto.response.Qtn11ErrorDetailDto;
import vn.nguongocso.report.dto.response.OpenDataExportDto;
import vn.nguongocso.report.dto.response.OpenDataExportDto.*;
import vn.nguongocso.report.service.OpenDataExportService;
import vn.nguongocso.report.service.ReportAccessLogService;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.trace.entity.Shipment;
import vn.nguongocso.trace.repository.ShipmentRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ kết xuất dữ liệu mở cho Cán bộ quản lý ngành.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenDataExportServiceImpl implements OpenDataExportService {
    private final OrganizationRepository organizationRepository;
    private final ProductionLotRepository productionLotRepository;
    private final FarmLogRepository farmLogRepository;
    private final FarmLogAttachmentRepository farmLogAttachmentRepository;
    private final ShipmentRepository shipmentRepository;
    private final ChainEventRepository chainEventRepository;
    private final ReportAccessLogService reportAccessLogService;
    private final ObjectMapper objectMapper;

    private static final String REGULATOR_ROLE = "VT-05";
    private static final String REPORT_NAME = "OPEN_DATA_EXPORT";
    private static final String EMPTY_DATA_MESSAGE = "Không có dữ liệu mở đủ điều kiện để xuất trong phạm vi đã chọn.";

    /**
     * Kết xuất dữ liệu mở dựa trên các tiêu chí lọc.
     *
     * @param region      Địa bàn (tỉnh/thành phố)
     * @param fromDate    Ngày bắt đầu
     * @param toDate      Ngày kết thúc
     * @param format      Định dạng xuất (JSON, XML, CSV)
     * @param currentUser Thông tin người dùng hiện tại
     * @param ipAddress   Địa chỉ IP của người dùng
     * @return Mảng byte đại diện cho tệp dữ liệu mở đã xuất
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportOpenData(String region, LocalDate fromDate, LocalDate toDate, String format,
            CustomUserDetails currentUser, String ipAddress) {
        // 1. Phân quyền kiểm tra bảo mật (VT-05)
        validateRole(currentUser, ipAddress);

        // 2. Kiểm tra tính hợp lệ của tham số
        validateParams(region, fromDate, toDate, format);

        try {
            // 3. Tìm các tổ chức thuộc địa bàn
            List<Organization> organizations = organizationRepository.findByAddressContainingIgnoreCase(region);
            if (organizations.isEmpty()) {
                throw new BusinessException(EMPTY_DATA_MESSAGE);
            }

            List<UUID> orgIds = organizations.stream()
                    .map(Organization::getOrganizationId)
                    .toList();

            // 4. Lấy danh sách lô hàng trong khoảng thời gian có trạng thái CLOSED hoặc
            // PACKAGED
            List<ProductionLot> lots = productionLotRepository.findEligibleLotsForExport(
                    orgIds, fromDate, toDate, List.of(ProductionLotStatus.CLOSED, ProductionLotStatus.PACKAGED));
            if (lots.isEmpty()) {
                throw new BusinessException(EMPTY_DATA_MESSAGE);
            }

            // 5. Kiểm duyệt theo quy tắc QTN-11
            List<UUID> lotIds = lots.stream().map(ProductionLot::getId).toList();
            List<FarmLog> allLogs = farmLogRepository.findByProductionLotId_IdInOrderByExecutedDateAsc(lotIds);

            List<UUID> logIds = allLogs.stream().map(FarmLog::getId).toList();
            List<FarmLogAttachment> allAttachments = logIds.isEmpty() ? List.of()
                    : farmLogAttachmentRepository.findByFarmLogIdIn(logIds);

            // Phân nhóm logs và attachments để kiểm tra nhanh
            Map<UUID, List<FarmLog>> logsByLot = allLogs.stream()
                    .collect(Collectors.groupingBy(log -> log.getProductionLotId().getId()));
            Map<UUID, List<FarmLogAttachment>> attachmentsByLog = allAttachments.stream()
                    .collect(Collectors.groupingBy(att -> att.getFarmLog().getId()));

            List<ProductionLot> eligibleLots = new ArrayList<>();
            List<Qtn11ErrorDetailDto> qtn11ErrorDetails = new ArrayList<>();
            for (ProductionLot lot : lots) {
                Qtn11ErrorDetailDto detail = checkLotQTN11Detail(lot, logsByLot, attachmentsByLog);
                if (detail == null) {
                    eligibleLots.add(lot);
                } else {
                    qtn11ErrorDetails.add(detail);
                }
            }

            if (eligibleLots.isEmpty()) {
                throw new BusinessException(EMPTY_DATA_MESSAGE, qtn11ErrorDetails);
            }

            // 6. Truy vấn Shipments và ChainEvents liên quan
            List<UUID> eligibleLotIds = eligibleLots.stream().map(ProductionLot::getId).toList();
            List<Shipment> shipments = shipmentRepository.findByProductionLotIdIn(eligibleLotIds);

            Map<UUID, List<Shipment>> shipmentsByLot = shipments.stream()
                    .collect(Collectors.groupingBy(sh -> sh.getProductionLot().getId()));

            List<UUID> shipmentIds = shipments.stream().map(Shipment::getId).toList();
            List<ChainEvent> chainEvents = shipmentIds.isEmpty() ? List.of()
                    : chainEventRepository.findByShipmentIdInOrderByRecordedAtAsc(shipmentIds);

            Map<UUID, List<ChainEvent>> eventsByShipment = chainEvents.stream()
                    .collect(Collectors.groupingBy(e -> e.getShipment().getId()));

            // 7. Xây dựng DTOs kết xuất
            List<OpenDataExportDto> exportDtos = eligibleLots.stream().map(lot -> {
                Organization org = lot.getOrganization();
                var orgDto = OrganizationDto.builder()
                        .organizationId(org.getOrganizationId())
                        .organizationName(org.getName())
                        .organizationAddress(org.getAddress())
                        .build();

                FarmAreaDto areaDto = null;
                if (lot.getFarmArea() != null) {
                    var fa = lot.getFarmArea();
                    LocationDto locDto = null;
                    if (fa.getLocation() != null) {
                        locDto = LocationDto.builder()
                                .latitude(fa.getLocation().getY())
                                .longitude(fa.getLocation().getX())
                                .build();
                    }
                    areaDto = FarmAreaDto.builder()
                            .farmAreaId(fa.getId())
                            .farmAreaName(fa.getName())
                            .farmAreaSize(fa.getArea())
                            .farmAreaLocation(locDto)
                            .build();
                }

                List<FarmLog> lotLogs = logsByLot.getOrDefault(lot.getId(), List.of());
                List<FarmLogDto> logDtos = lotLogs.stream().map(logItem -> {
                    List<FarmLogAttachment> attList = attachmentsByLog.getOrDefault(logItem.getId(), List.of());
                    List<String> fileNames = attList.stream().map(FarmLogAttachment::getFileName).toList();

                    return FarmLogDto.builder()
                            .logId(logItem.getId())
                            .activityType(logItem.getActivityType().name())
                            .material(logItem.getMaterial())
                            .quantity(logItem.getQuantity())
                            .unit(logItem.getUnit())
                            .executedDate(logItem.getExecutedDate())
                            .notes(logItem.getNotes())
                            .attachments(fileNames)
                            .build();
                }).toList();

                List<Shipment> lotShipments = shipmentsByLot.getOrDefault(lot.getId(), List.of());
                List<ShipmentDto> shipmentDtos = lotShipments.stream().map(sh -> {
                    List<ChainEvent> shEvents = eventsByShipment.getOrDefault(sh.getId(), List.of());
                    List<JourneyEventDto> evDtos = shEvents.stream().map(ev -> {
                        LocationDto eventLoc = null;
                        if (ev.getLocation() != null) {
                            eventLoc = LocationDto.builder()
                                    .latitude(ev.getLocation().getY())
                                    .longitude(ev.getLocation().getX())
                                    .build();
                        }
                        return JourneyEventDto.builder()
                                .eventId(ev.getId())
                                .eventType(ev.getEventType().name())
                                .recordedAt(ev.getRecordedAt())
                                .actorName(ev.getRecordedBy() != null ? ev.getRecordedBy().getFullName() : "Hệ thống")
                                .eventLocation(eventLoc)
                                .build();
                    }).toList();

                    return ShipmentDto.builder()
                            .shipmentId(sh.getId())
                            .shipmentName(sh.getName())
                            .totalQuantity(sh.getTotalQuantity())
                            .shippedAt(sh.getCreatedAt())
                            .journeyEvents(evDtos)
                            .build();
                }).toList();

                return OpenDataExportDto.builder()
                        .lotId(lot.getId())
                        .lotCode(lot.getName())
                        .productCategory(lot.getProductCategory().getName())
                        .expectedQuantity(lot.getExpectedQuantity())
                        .expectedQuantityUnit(lot.getExpectedQuantityUnit())
                        .actualQuantity(lot.getActualQuantity())
                        .plantingDate(lot.getPlantingDate())
                        .harvestDate(lot.getHarvestDate())
                        .status(lot.getStatus().name())
                        .organization(orgDto)
                        .farmArea(areaDto)
                        .farmLogs(logDtos)
                        .shipments(shipmentDtos)
                        .build();
            }).toList();

            // 8. Định dạng xuất ra tệp dữ liệu tương ứng
            byte[] fileBytes;
            if ("XML".equalsIgnoreCase(format)) {
                fileBytes = generateXml(region, fromDate, toDate, exportDtos);
            } else if ("CSV".equalsIgnoreCase(format)) {
                fileBytes = generateCsv(exportDtos);
            } else {
                fileBytes = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(exportDtos)
                        .getBytes(StandardCharsets.UTF_8);
            }

            // 9. Ghi nhận log thành công
            reportAccessLogService.logAccess(
                    currentUser.getUserId(),
                    currentUser.getOrganizationId(),
                    currentUser.getOrganizationId(),
                    REPORT_NAME,
                    true,
                    ipAddress);

            return fileBytes;

        } catch (BusinessException e) {
            log.warn("Lỗi xuất dữ liệu mở: {}", e.getMessage());
            reportAccessLogService.logAccess(
                    currentUser.getUserId(),
                    currentUser.getOrganizationId(),
                    currentUser.getOrganizationId(),
                    REPORT_NAME,
                    false,
                    ipAddress);
            throw e;
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi xuất dữ liệu mở", e);
            reportAccessLogService.logAccess(
                    currentUser.getUserId(),
                    currentUser.getOrganizationId(),
                    currentUser.getOrganizationId(),
                    REPORT_NAME,
                    false,
                    ipAddress);
            throw new BusinessException("Đã xảy ra lỗi trong quá trình kết xuất dữ liệu mở.");
        }
    }

    private void validateRole(CustomUserDetails currentUser, String ipAddress) {
        if (currentUser == null || !REGULATOR_ROLE.equals(currentUser.getRoleCode())) {
            if (currentUser != null) {
                reportAccessLogService.logAccess(
                        currentUser.getUserId(),
                        currentUser.getOrganizationId(),
                        currentUser.getOrganizationId(),
                        REPORT_NAME,
                        false,
                        ipAddress);
            }
            throw new AccessDeniedException("Bạn không có quyền thực hiện chức năng này");
        }
    }

    private void validateParams(String region, LocalDate fromDate, LocalDate toDate, String format) {
        if (region == null || region.isBlank()) {
            throw new BusinessException("Tham số địa bàn không được để trống.");
        }
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException("Khoảng thời gian không hợp lệ.");
        }
        if (format == null || (!format.equalsIgnoreCase("JSON") && !format.equalsIgnoreCase("XML")
                && !format.equalsIgnoreCase("CSV"))) {
            throw new BusinessException("Định dạng xuất dữ liệu không hợp lệ.");
        }
    }

    private Qtn11ErrorDetailDto checkLotQTN11Detail(ProductionLot lot, Map<UUID, List<FarmLog>> logsByLot,
            Map<UUID, List<FarmLogAttachment>> attachmentsByLog) {
        List<FarmLog> lotLogs = logsByLot.getOrDefault(lot.getId(), List.of());

        boolean hasPlanting = false;
        boolean hasFertilizing = false;
        boolean hasPesticide = false;
        boolean hasHarvesting = false;

        for (FarmLog logItem : lotLogs) {
            List<FarmLogAttachment> atts = attachmentsByLog.getOrDefault(logItem.getId(), List.of());
            if (!atts.isEmpty()) {
                if (logItem.getActivityType() == FarmActivityType.PLANTING)
                    hasPlanting = true;
                else if (logItem.getActivityType() == FarmActivityType.FERTILIZING)
                    hasFertilizing = true;
                else if (logItem.getActivityType() == FarmActivityType.PESTICIDE)
                    hasPesticide = true;
                else if (logItem.getActivityType() == FarmActivityType.HARVESTING)
                    hasHarvesting = true;
            }
        }

        if (hasPlanting && hasFertilizing && hasPesticide && hasHarvesting) {
            return null; // Passes QTN-11
        }

        List<String> missingDocDetails = new ArrayList<>();
        if (!hasPlanting) missingDocDetails.add("Xuống giống (PLANTING) chưa có tệp/ảnh minh chứng");
        if (!hasFertilizing) missingDocDetails.add("Bón phân (FERTILIZING) chưa có tệp/ảnh minh chứng");
        if (!hasPesticide) missingDocDetails.add("Phun thuốc (PESTICIDE) chưa có tệp/ảnh minh chứng");
        if (!hasHarvesting) missingDocDetails.add("Thu hoạch (HARVESTING) chưa có tệp/ảnh minh chứng");

        return Qtn11ErrorDetailDto.builder()
                .id(lot.getId())
                .name(lot.getName())
                .lotCode(lot.getName())
                .missingEvents(List.of())
                .missingDocs(true)
                .missingDocDetails(missingDocDetails)
                .build();
    }

    private boolean verifyQTN11(UUID lotId, Map<UUID, List<FarmLog>> logsByLot,
            Map<UUID, List<FarmLogAttachment>> attachmentsByLog) {
        List<FarmLog> lotLogs = logsByLot.getOrDefault(lotId, List.of());

        boolean hasPlanting = false;
        boolean hasFertilizing = false;
        boolean hasPesticide = false;
        boolean hasHarvesting = false;

        for (FarmLog logItem : lotLogs) {
            List<FarmLogAttachment> atts = attachmentsByLog.getOrDefault(logItem.getId(), List.of());
            if (!atts.isEmpty()) {
                if (logItem.getActivityType() == FarmActivityType.PLANTING)
                    hasPlanting = true;
                else if (logItem.getActivityType() == FarmActivityType.FERTILIZING)
                    hasFertilizing = true;
                else if (logItem.getActivityType() == FarmActivityType.PESTICIDE)
                    hasPesticide = true;
                else if (logItem.getActivityType() == FarmActivityType.HARVESTING)
                    hasHarvesting = true;
            }
        }
        return hasPlanting && hasFertilizing && hasPesticide && hasHarvesting;
    }

    private byte[] generateXml(String region, LocalDate fromDate, LocalDate toDate, List<OpenDataExportDto> dtos) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<OpenDataExport region=\"").append(escapeXml(region))
                .append("\" fromDate=\"").append(fromDate)
                .append("\" toDate=\"").append(toDate).append("\">\n");
        sb.append("  <ProductionLots>\n");
        for (OpenDataExportDto dto : dtos) {
            sb.append("    <ProductionLot>\n");
            sb.append("      <LotId>").append(dto.getLotId()).append("</LotId>\n");
            sb.append("      <LotCode>").append(escapeXml(dto.getLotCode())).append("</LotCode>\n");
            sb.append("      <ProductCategory>").append(escapeXml(dto.getProductCategory()))
                    .append("</ProductCategory>\n");
            sb.append("      <ExpectedQuantity>").append(dto.getExpectedQuantity()).append("</ExpectedQuantity>\n");
            sb.append("      <ExpectedQuantityUnit>").append(escapeXml(dto.getExpectedQuantityUnit()))
                    .append("</ExpectedQuantityUnit>\n");
            sb.append("      <ActualQuantity>").append(dto.getActualQuantity() != null ? dto.getActualQuantity() : "")
                    .append("</ActualQuantity>\n");
            sb.append("      <PlantingDate>").append(dto.getPlantingDate() != null ? dto.getPlantingDate() : "")
                    .append("</PlantingDate>\n");
            sb.append("      <HarvestDate>").append(dto.getHarvestDate() != null ? dto.getHarvestDate() : "")
                    .append("</HarvestDate>\n");
            sb.append("      <Status>").append(dto.getStatus()).append("</Status>\n");

            if (dto.getOrganization() != null) {
                sb.append("      <Organization>\n");
                sb.append("        <OrganizationId>").append(dto.getOrganization().getOrganizationId())
                        .append("</OrganizationId>\n");
                sb.append("        <OrganizationName>").append(escapeXml(dto.getOrganization().getOrganizationName()))
                        .append("</OrganizationName>\n");
                sb.append("        <OrganizationAddress>")
                        .append(escapeXml(dto.getOrganization().getOrganizationAddress()))
                        .append("</OrganizationAddress>\n");
                sb.append("      </Organization>\n");
            }

            if (dto.getFarmArea() != null) {
                sb.append("      <FarmArea>\n");
                sb.append("        <FarmAreaId>").append(dto.getFarmArea().getFarmAreaId()).append("</FarmAreaId>\n");
                sb.append("        <FarmAreaName>").append(escapeXml(dto.getFarmArea().getFarmAreaName()))
                        .append("</FarmAreaName>\n");
                sb.append("        <FarmAreaSize>").append(dto.getFarmArea().getFarmAreaSize())
                        .append("</FarmAreaSize>\n");
                if (dto.getFarmArea().getFarmAreaLocation() != null) {
                    sb.append("        <FarmAreaLocation>\n");
                    sb.append("          <Latitude>").append(dto.getFarmArea().getFarmAreaLocation().getLatitude())
                            .append("</Latitude>\n");
                    sb.append("          <Longitude>").append(dto.getFarmArea().getFarmAreaLocation().getLongitude())
                            .append("</Longitude>\n");
                    sb.append("        </FarmAreaLocation>\n");
                }
                sb.append("      </FarmArea>\n");
            }

            sb.append("      <FarmLogs>\n");
            if (dto.getFarmLogs() != null) {
                for (FarmLogDto logItem : dto.getFarmLogs()) {
                    sb.append("        <FarmLog>\n");
                    sb.append("          <LogId>").append(logItem.getLogId()).append("</LogId>\n");
                    sb.append("          <ActivityType>").append(logItem.getActivityType()).append("</ActivityType>\n");
                    sb.append("          <Material>").append(escapeXml(logItem.getMaterial())).append("</Material>\n");
                    sb.append("          <Quantity>").append(logItem.getQuantity() != null ? logItem.getQuantity() : "")
                            .append("</Quantity>\n");
                    sb.append("          <Unit>").append(escapeXml(logItem.getUnit())).append("</Unit>\n");
                    sb.append("          <ExecutedDate>")
                            .append(logItem.getExecutedDate() != null ? logItem.getExecutedDate() : "")
                            .append("</ExecutedDate>\n");
                    sb.append("          <Notes>").append(escapeXml(logItem.getNotes())).append("</Notes>\n");
                    sb.append("          <Attachments>\n");
                    if (logItem.getAttachments() != null) {
                        for (String att : logItem.getAttachments()) {
                            sb.append("            <Attachment>").append(escapeXml(att)).append("</Attachment>\n");
                        }
                    }
                    sb.append("          </Attachments>\n");
                    sb.append("        </FarmLog>\n");
                }
            }
            sb.append("      </FarmLogs>\n");

            sb.append("      <Shipments>\n");
            if (dto.getShipments() != null) {
                for (ShipmentDto sh : dto.getShipments()) {
                    sb.append("        <Shipment>\n");
                    sb.append("          <ShipmentId>").append(sh.getShipmentId()).append("</ShipmentId>\n");
                    sb.append("          <ShipmentName>").append(escapeXml(sh.getShipmentName()))
                            .append("</ShipmentName>\n");
                    sb.append("          <TotalQuantity>").append(sh.getTotalQuantity()).append("</TotalQuantity>\n");
                    sb.append("          <ShippedAt>").append(sh.getShippedAt()).append("</ShippedAt>\n");
                    sb.append("          <JourneyEvents>\n");
                    if (sh.getJourneyEvents() != null) {
                        for (JourneyEventDto ev : sh.getJourneyEvents()) {
                            sb.append("            <JourneyEvent>\n");
                            sb.append("              <EventId>").append(ev.getEventId()).append("</EventId>\n");
                            sb.append("              <EventType>").append(ev.getEventType()).append("</EventType>\n");
                            sb.append("              <RecordedAt>").append(ev.getRecordedAt())
                                    .append("</RecordedAt>\n");
                            sb.append("              <ActorName>").append(escapeXml(ev.getActorName()))
                                    .append("</ActorName>\n");
                            if (ev.getEventLocation() != null) {
                                sb.append("              <EventLocation>\n");
                                sb.append("                <Latitude>").append(ev.getEventLocation().getLatitude())
                                        .append("</Latitude>\n");
                                sb.append("                <Longitude>").append(ev.getEventLocation().getLongitude())
                                        .append("</Longitude>\n");
                                sb.append("              </EventLocation>\n");
                            }
                            sb.append("            </JourneyEvent>\n");
                        }
                    }
                    sb.append("          </JourneyEvents>\n");
                    sb.append("        </Shipment>\n");
                }
            }
            sb.append("      </Shipments>\n");

            sb.append("    </ProductionLot>\n");
        }
        sb.append("  </ProductionLots>\n");
        sb.append("</OpenDataExport>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateCsv(List<OpenDataExportDto> dtos) {
        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 để hỗ trợ mở Excel Tiếng Việt không lỗi font
        sb.append('\ufeff');
        sb.append(
                "lotId,lotCode,productCategory,expectedQuantity,expectedQuantityUnit,actualQuantity,plantingDate,harvestDate,status,organizationName,organizationAddress,farmAreaName,totalFarmLogs,totalShipments\n");
        for (OpenDataExportDto dto : dtos) {
            sb.append(dto.getLotId()).append(",");
            sb.append(escapeCsv(dto.getLotCode())).append(",");
            sb.append(escapeCsv(dto.getProductCategory())).append(",");
            sb.append(dto.getExpectedQuantity()).append(",");
            sb.append(escapeCsv(dto.getExpectedQuantityUnit())).append(",");
            sb.append(dto.getActualQuantity() != null ? dto.getActualQuantity() : "").append(",");
            sb.append(dto.getPlantingDate() != null ? dto.getPlantingDate() : "").append(",");
            sb.append(dto.getHarvestDate() != null ? dto.getHarvestDate() : "").append(",");
            sb.append(dto.getStatus()).append(",");
            sb.append(dto.getOrganization() != null ? escapeCsv(dto.getOrganization().getOrganizationName()) : "")
                    .append(",");
            sb.append(dto.getOrganization() != null ? escapeCsv(dto.getOrganization().getOrganizationAddress()) : "")
                    .append(",");
            sb.append(dto.getFarmArea() != null ? escapeCsv(dto.getFarmArea().getFarmAreaName()) : "").append(",");
            sb.append(dto.getFarmLogs() != null ? dto.getFarmLogs().size() : 0).append(",");
            sb.append(dto.getShipments() != null ? dto.getShipments().size() : 0).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeXml(String value) {
        if (value == null)
            return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
