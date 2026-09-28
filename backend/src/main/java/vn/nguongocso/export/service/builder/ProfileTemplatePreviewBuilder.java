package vn.nguongocso.export.service.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.nguongocso.certification.entity.InspectionCriterion;
import vn.nguongocso.certification.entity.InspectionCriterionResult;
import vn.nguongocso.certification.entity.InspectionRequest;
import vn.nguongocso.certification.entity.ProductionLotCertification;
import vn.nguongocso.certification.repository.InspectionCriterionResultRepository;
import vn.nguongocso.certification.repository.InspectionRequestRepository;
import vn.nguongocso.certification.repository.ProductionLotCertificationRepository;
import vn.nguongocso.event.entity.ChainEvent;
import vn.nguongocso.event.repository.ChainEventRepository;
import vn.nguongocso.export.entity.ProfileTemplate;
import vn.nguongocso.export.util.ExportDisplayFormatter;
import vn.nguongocso.farm.entity.FarmArea;
import vn.nguongocso.farm.entity.FarmLog;
import vn.nguongocso.farm.entity.FarmLogAttachment;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.farm.repository.FarmLogAttachmentRepository;
import vn.nguongocso.farm.repository.FarmLogRepository;
import vn.nguongocso.organization.entity.Organization;
import vn.nguongocso.trace.entity.Shipment;

/** Thành phần chuyên trách truy vấn 6 domain repository và tổng hợp cây dữ liệu xem trước (preview snapshot). */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileTemplatePreviewBuilder {
    private final FarmLogRepository farmLogRepository;
    private final FarmLogAttachmentRepository farmLogAttachmentRepository;
    private final ProductionLotCertificationRepository productionLotCertificationRepository;
    private final InspectionRequestRepository inspectionRequestRepository;
    private final InspectionCriterionResultRepository inspectionCriterionResultRepository;
    private final ChainEventRepository chainEventRepository;

    /** Tổng hợp và định dạng toàn bộ cây dữ liệu xem trước hồ sơ truy xuất đã lọc theo mẫu. */
    @Transactional(readOnly = true)
    public Map<String, Object> buildPreviewSnapshot(
            Shipment shipment,
            ProfileTemplate template,
            Set<String> selectedFieldKeys) {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("shipmentId", shipment.getId());
        preview.put("appliedTemplate", buildAppliedTemplateInfo(template, selectedFieldKeys.size()));

        buildOrganizationSection(preview, shipment.getOrganization(), selectedFieldKeys);

        ProductionLot lot = shipment.getProductionLot();
        if (lot != null) {
            buildFarmAreaSection(preview, lot.getFarmArea(), selectedFieldKeys);
            buildProductionLotSection(preview, lot, selectedFieldKeys);
        }

        buildShipmentSection(preview, shipment, selectedFieldKeys);

        if (lot != null) {
            buildFarmLogsSection(preview, lot.getId(), selectedFieldKeys);
            buildCertificationsSection(preview, lot.getId(), selectedFieldKeys);
            buildInspectionsSection(preview, lot.getId(), selectedFieldKeys);
        }

        buildTimelineEventsSection(preview, shipment.getId(), selectedFieldKeys);
        return preview;
    }

    private Map<String, Object> buildAppliedTemplateInfo(ProfileTemplate template, int totalFields) {
        Map<String, Object> appliedTemplateInfo = new LinkedHashMap<>();
        if (template != null) {
            appliedTemplateInfo.put("templateId", template.getId());
            appliedTemplateInfo.put("templateName", template.getName());
            appliedTemplateInfo.put("isDefault", template.getIsDefault());
            appliedTemplateInfo.put("totalFields", totalFields);
        } else {
            appliedTemplateInfo.put("templateId", null);
            appliedTemplateInfo.put("templateName", "Mặc định hệ thống");
            appliedTemplateInfo.put("isDefault", true);
            appliedTemplateInfo.put("totalFields", totalFields);
        }
        return appliedTemplateInfo;
    }

    private void buildOrganizationSection(
            Map<String, Object> preview,
            Organization org,
            Set<String> selectedFieldKeys) {
        if (org == null) {
            return;
        }
        Map<String, Object> orgData = new LinkedHashMap<>();
        addFieldIfSelected(orgData, "name", "organization.name", selectedFieldKeys, org.getName());
        addFieldIfSelected(orgData, "code", "organization.code", selectedFieldKeys, org.getCode());
        addFieldIfSelected(orgData, "type", "organization.type", selectedFieldKeys,
                org.getType() != null ? ExportDisplayFormatter.formatOrganizationType(org.getType()) : null);
        addFieldIfSelected(orgData, "status", "organization.status", selectedFieldKeys,
                org.getStatus() != null ? ExportDisplayFormatter.formatOrganizationStatus(org.getStatus()) : null);
        addFieldIfSelected(orgData, "address", "organization.address", selectedFieldKeys, org.getAddress());
        addFieldIfSelected(orgData, "province", "organization.province", selectedFieldKeys,
                org.getProvince() != null ? org.getProvince().getName() : null);
        addFieldIfSelected(orgData, "phone", "organization.phone", selectedFieldKeys, org.getPhone());
        addFieldIfSelected(orgData, "email", "organization.email", selectedFieldKeys, org.getEmail());
        if (!orgData.isEmpty()) {
            preview.put("organization", orgData);
        }
    }

    private void buildFarmAreaSection(Map<String, Object> preview, FarmArea farmArea, Set<String> selectedFieldKeys) {
        if (farmArea == null) {
            return;
        }
        Map<String, Object> farmAreaData = new LinkedHashMap<>();
        addFieldIfSelected(farmAreaData, "name", "farmArea.name", selectedFieldKeys, farmArea.getName());
        addFieldIfSelected(farmAreaData, "location", "farmArea.location", selectedFieldKeys,
                farmArea.getLocation() != null
                        ? farmArea.getLocation().getY() + ", " + farmArea.getLocation().getX()
                        : null);
        addFieldIfSelected(farmAreaData, "area", "farmArea.area", selectedFieldKeys, farmArea.getArea());
        addFieldIfSelected(farmAreaData, "areaUnit", "farmArea.areaUnit", selectedFieldKeys,
                farmArea.getAreaUnit() != null ? ExportDisplayFormatter.formatAreaUnit(farmArea.getAreaUnit()) : null);
        addFieldIfSelected(farmAreaData, "cropType", "farmArea.cropType", selectedFieldKeys,
                farmArea.getCropType() != null ? farmArea.getCropType().getName() : null);
        addFieldIfSelected(farmAreaData, "isActive", "farmArea.isActive", selectedFieldKeys,
                Boolean.TRUE.equals(farmArea.getIsActive()) ? "Đang hoạt động" : "Tạm ngưng");
        if (!farmAreaData.isEmpty()) {
            preview.put("farmArea", farmAreaData);
        }
    }

    private void buildProductionLotSection(
            Map<String, Object> preview,
            ProductionLot lot,
            Set<String> selectedFieldKeys) {
        Map<String, Object> lotData = new LinkedHashMap<>();
        addFieldIfSelected(lotData, "name", "productionLot.name", selectedFieldKeys, lot.getName());
        addFieldIfSelected(lotData, "productCategory", "productionLot.productCategory", selectedFieldKeys,
                lot.getProductCategory() != null ? lot.getProductCategory().getName() : null);
        addFieldIfSelected(
                lotData, "plantingDate", "productionLot.plantingDate", selectedFieldKeys, lot.getPlantingDate());
        addFieldIfSelected(
                lotData, "harvestDate", "productionLot.harvestDate", selectedFieldKeys, lot.getHarvestDate());
        addFieldIfSelected(
                lotData,
                "expectedQuantity",
                "productionLot.expectedQuantity",
                selectedFieldKeys,
                lot.getExpectedQuantity());
        addFieldIfSelected(
                lotData,
                "expectedQuantityUnit",
                "productionLot.expectedQuantityUnit",
                selectedFieldKeys,
                lot.getExpectedQuantityUnit());
        addFieldIfSelected(
                lotData, "actualQuantity", "productionLot.actualQuantity", selectedFieldKeys, lot.getActualQuantity());
        addFieldIfSelected(lotData, "status", "productionLot.status", selectedFieldKeys,
                lot.getStatus() != null ? ExportDisplayFormatter.formatProductionLotStatus(lot.getStatus()) : null);
        if (!lotData.isEmpty()) {
            preview.put("productionLot", lotData);
        }
    }

    private void buildShipmentSection(Map<String, Object> preview, Shipment shipment, Set<String> selectedFieldKeys) {
        Map<String, Object> shipmentData = new LinkedHashMap<>();
        addFieldIfSelected(shipmentData, "name", "shipment.name", selectedFieldKeys, shipment.getName());
        addFieldIfSelected(
                shipmentData,
                "totalQuantity",
                "shipment.totalQuantity",
                selectedFieldKeys,
                shipment.getTotalQuantity());
        addFieldIfSelected(
                shipmentData,
                "packagingInfo",
                "shipment.packagingInfo",
                selectedFieldKeys,
                shipment.getPackagingInfo());
        addFieldIfSelected(shipmentData, "status", "shipment.status", selectedFieldKeys,
                shipment.getStatus() != null
                        ? ExportDisplayFormatter.formatShipmentStatus(shipment.getStatus())
                        : null);
        addFieldIfSelected(shipmentData, "createdAt", "shipment.createdAt", selectedFieldKeys, shipment.getCreatedAt());
        if (!shipmentData.isEmpty()) {
            preview.put("shipment", shipmentData);
        }
    }

    private void buildFarmLogsSection(Map<String, Object> preview, UUID lotId, Set<String> selectedFieldKeys) {
        if (!hasAnyPrefixSelected("farmLog.", selectedFieldKeys)) {
            return;
        }
        List<FarmLog> logs = farmLogRepository.findByProductionLotId_IdOrderByExecutedDateAsc(lotId);
        List<Map<String, Object>> logList = new ArrayList<>();
        for (FarmLog l : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            addFieldIfSelected(item, "activityType", "farmLog.activityType", selectedFieldKeys,
                    l.getActivityType() != null
                            ? ExportDisplayFormatter.formatFarmActivityType(l.getActivityType())
                            : null);
            addFieldIfSelected(item, "executedDate", "farmLog.executedDate", selectedFieldKeys, l.getExecutedDate());
            addFieldIfSelected(item, "material", "farmLog.material", selectedFieldKeys, l.getMaterial());
            addFieldIfSelected(item, "quantity", "farmLog.quantity", selectedFieldKeys, l.getQuantity());
            addFieldIfSelected(item, "unit", "farmLog.unit", selectedFieldKeys, l.getUnit());
            addFieldIfSelected(item, "notes", "farmLog.notes", selectedFieldKeys, l.getNotes());
            if (selectedFieldKeys.contains("farmLog.attachments") && l.getId() != null) {
                List<FarmLogAttachment> atts = farmLogAttachmentRepository.findByFarmLogId(l.getId());
                List<String> fileNames = atts.stream()
                        .map(FarmLogAttachment::getFileName)
                        .filter(Objects::nonNull)
                        .toList();
                item.put("attachments", new ArrayList<>(fileNames));
            }
            if (!item.isEmpty()) {
                logList.add(item);
            }
        }
        if (!logList.isEmpty()) {
            preview.put("farmLogs", logList);
        }
    }

    private void buildCertificationsSection(Map<String, Object> preview, UUID lotId, Set<String> selectedFieldKeys) {
        if (!hasAnyPrefixSelected("certification.", selectedFieldKeys)) {
            return;
        }
        List<ProductionLotCertification> certs =
                productionLotCertificationRepository.findByProductionLotIdIn(List.of(lotId));
        List<Map<String, Object>> certList = new ArrayList<>();
        for (ProductionLotCertification plc : certs) {
            if (plc.getCertification() != null) {
                var c = plc.getCertification();
                Map<String, Object> item = new LinkedHashMap<>();
                addFieldIfSelected(item, "name", "certification.name", selectedFieldKeys, c.getName());
                addFieldIfSelected(item, "standardName", "certification.standardName", selectedFieldKeys,
                        c.getStandard() != null ? c.getStandard().getName() : null);
                addFieldIfSelected(
                        item, "certificationCode", "certification.certificationCode", selectedFieldKeys, c.getCode());
                addFieldIfSelected(item, "issueDate", "certification.issueDate", selectedFieldKeys, c.getIssueDate());
                addFieldIfSelected(
                        item, "expiryDate", "certification.expiryDate", selectedFieldKeys, c.getExpiryDate());
                addFieldIfSelected(item, "certifier", "certification.certifier", selectedFieldKeys, c.getIssuedBy());
                if (!item.isEmpty()) {
                    certList.add(item);
                }
            }
        }
        if (!certList.isEmpty()) {
            preview.put("certifications", certList);
        }
    }

    private void buildInspectionsSection(Map<String, Object> preview, UUID lotId, Set<String> selectedFieldKeys) {
        if (!hasAnyPrefixSelected("inspection.", selectedFieldKeys)) {
            return;
        }
        List<InspectionRequest> inspections =
                inspectionRequestRepository.findByProductionLot_IdOrderByCreatedAtDesc(lotId);
        List<Map<String, Object>> inspList = new ArrayList<>();
        for (InspectionRequest ir : inspections) {
            appendInspectionRequestItems(ir, selectedFieldKeys, inspList);
        }
        if (!inspList.isEmpty()) {
            preview.put("inspections", inspList);
        }
    }

    private void appendInspectionRequestItems(
            InspectionRequest ir, Set<String> selectedFieldKeys, List<Map<String, Object>> inspList) {
        List<InspectionCriterionResult> results = inspectionCriterionResultRepository
                .findByInspectionCriterion_InspectionRequest_Id(ir.getId());
        Map<UUID, InspectionCriterionResult> resultMap = results.stream()
                .filter(r -> r.getInspectionCriterion() != null && r.getInspectionCriterion().getId() != null)
                .collect(Collectors.toMap(r -> r.getInspectionCriterion().getId(), r -> r, (r1, r2) -> r1));

        if (ir.getCriteria() != null && !ir.getCriteria().isEmpty()) {
            for (InspectionCriterion c : ir.getCriteria()) {
                Map<String, Object> item = buildCriterionItem(ir, c, resultMap.get(c.getId()), selectedFieldKeys);
                if (!item.isEmpty()) {
                    inspList.add(item);
                }
            }
        } else {
            Map<String, Object> fallback = buildFallbackInspectionItem(ir, selectedFieldKeys);
            if (!fallback.isEmpty()) {
                inspList.add(fallback);
            }
        }
    }

    private Map<String, Object> buildCriterionItem(
            InspectionRequest ir, InspectionCriterion c,
            InspectionCriterionResult res, Set<String> selectedFieldKeys) {
        Map<String, Object> item = new LinkedHashMap<>();
        addFieldIfSelected(
                item, "sampleSentDate", "inspection.sampleSentDate", selectedFieldKeys, ir.getSampleSentDate());
        addFieldIfSelected(
                item, "inspectionUnit", "inspection.inspectionUnit", selectedFieldKeys, ir.getInspectionUnit());

        String critLabel = c.getCriterionName() != null ? c.getCriterionName() : c.getCriterionCode();
        if (c.getStandard() != null && c.getStandard().getName() != null) {
            critLabel = critLabel + " (" + c.getStandard().getName() + ")";
        }
        addFieldIfSelected(item, "criterionName", "inspection.criterionName", selectedFieldKeys, critLabel);

        String outcome = res == null ? "Chưa có kết quả" : (Boolean.TRUE.equals(res.getPassed()) ? "Đạt" : "Không đạt");
        addFieldIfSelected(item, "passed", "inspection.passed", selectedFieldKeys, outcome);
        addFieldIfSelected(item, "status", "inspection.passed", selectedFieldKeys, outcome);
        addFieldIfSelected(
                item,
                "resultDate",
                "inspection.resultDate",
                selectedFieldKeys,
                res != null ? res.getResultDate() : null);
        addFieldIfSelected(
                item,
                "expiryDate",
                "inspection.expiryDate",
                selectedFieldKeys,
                res != null ? res.getExpiryDate() : null);
        return item;
    }

    private Map<String, Object> buildFallbackInspectionItem(InspectionRequest ir, Set<String> selectedFieldKeys) {
        Map<String, Object> item = new LinkedHashMap<>();
        addFieldIfSelected(
                item, "sampleSentDate", "inspection.sampleSentDate", selectedFieldKeys, ir.getSampleSentDate());
        addFieldIfSelected(
                item, "inspectionUnit", "inspection.inspectionUnit", selectedFieldKeys, ir.getInspectionUnit());
        String outcome = ir.getStatus() != null ? ir.getStatus().name() : null;
        addFieldIfSelected(item, "passed", "inspection.passed", selectedFieldKeys, outcome);
        addFieldIfSelected(item, "status", "inspection.passed", selectedFieldKeys, outcome);
        return item;
    }

    private void buildTimelineEventsSection(
            Map<String, Object> preview,
            UUID shipmentId,
            Set<String> selectedFieldKeys) {
        if (!hasAnyPrefixSelected("chainEvent.", selectedFieldKeys)) {
            return;
        }
        List<ChainEvent> events = chainEventRepository.findByShipment_IdOrderByRecordedAtAsc(shipmentId);
        List<Map<String, Object>> eventList = new ArrayList<>();
        for (ChainEvent ce : events) {
            Map<String, Object> item = new LinkedHashMap<>();
            addFieldIfSelected(item, "eventType", "chainEvent.eventType", selectedFieldKeys,
                    ce.getEventType() != null ? ExportDisplayFormatter.formatChainEventType(ce.getEventType()) : null);
            addFieldIfSelected(item, "recordedAt", "chainEvent.recordedAt", selectedFieldKeys, ce.getRecordedAt());
            addFieldIfSelected(item, "recordedBy", "chainEvent.recordedBy", selectedFieldKeys,
                    ce.getRecordedBy() != null ? ce.getRecordedBy().getFullName() : null);
            String locStr = ce.getLocation() != null
                    ? ce.getLocation().getY() + ", " + ce.getLocation().getX()
                    : null;
            addFieldIfSelected(item, "location", "chainEvent.location", selectedFieldKeys, locStr);
            addFieldIfSelected(item, "eventData", "chainEvent.eventData", selectedFieldKeys,
                    ExportDisplayFormatter.formatEventData(ce.getEventData(), "; "));
            if (!item.isEmpty()) {
                eventList.add(item);
            }
        }
        if (!eventList.isEmpty()) {
            preview.put("timelineEvents", eventList);
        }
    }

    private void addFieldIfSelected(
            Map<String, Object> target,
            String outputKey,
            String fieldKey,
            Set<String> selectedFieldKeys,
            Object value) {
        if (selectedFieldKeys.contains(fieldKey)) {
            target.put(outputKey, value);
        }
    }

    private boolean hasAnyPrefixSelected(String prefix, Set<String> selectedFieldKeys) {
        return selectedFieldKeys.stream().anyMatch(key -> key.startsWith(prefix));
    }
}
