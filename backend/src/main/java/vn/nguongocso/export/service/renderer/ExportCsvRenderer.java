package vn.nguongocso.export.service.renderer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/** Thành phần định dạng dữ liệu xem trước hồ sơ truy xuất sang tệp CSV. */
@Component
public class ExportCsvRenderer {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Chuyển đổi dữ liệu xem trước hồ sơ sang chuỗi CSV hoàn chỉnh. */
    public String renderPreviewToCsv(Map<String, Object> preview) {
        LocalDateTime exportTime = preview.get("exportedAt") instanceof LocalDateTime dt
                ? dt
                : LocalDateTime.now();
        return renderPreviewToCsv(preview, exportTime);
    }

    /** Chuyển đổi dữ liệu xem trước hồ sơ sang chuỗi CSV hoàn chỉnh với thời điểm xuất cố định. */
    public String renderPreviewToCsv(Map<String, Object> preview, LocalDateTime exportTime) {
        StringBuilder sb = new StringBuilder("\uFEFF");

        appendHeaderSection(sb, preview, exportTime);
        appendMetadataTables(sb, preview);
        appendCertificationsSection(sb, preview);
        appendFarmLogsSection(sb, preview);
        appendInspectionsSection(sb, preview);
        appendTimelineSection(sb, preview);

        return sb.toString();
    }

    private void appendHeaderSection(StringBuilder sb, Map<String, Object> preview, LocalDateTime exportTime) {
        sb.append("# HỒ SƠ TRUY XUẤT NGUỒN GỐC SẢN PHẨM\n");
        if (preview.get("appliedTemplate") instanceof Map<?, ?> tpl) {
            sb.append("# Mẫu hồ sơ: ").append(escapeCsv(String.valueOf(tpl.get("templateName")))).append("\n");
        }
        sb.append("# Thời gian xuất: ").append(exportTime.format(DATE_TIME_FORMATTER)).append("\n\n");
        sb.append("Nhóm thông tin,Trường dữ liệu,Giá trị\n");
    }

    private void appendMetadataTables(StringBuilder sb, Map<String, Object> preview) {
        appendOrganizationRows(sb, preview);
        appendFarmAreaRows(sb, preview);
        appendProductionLotRows(sb, preview);
        appendShipmentRows(sb, preview);
    }

    private void appendOrganizationRows(StringBuilder sb, Map<String, Object> preview) {
        if (preview.get("organization") instanceof Map<?, ?> org) {
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Tên tổ chức", org.get("name"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Mã định danh", org.get("code"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Loại hình tổ chức", org.get("type"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Trạng thái tổ chức", org.get("status"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Địa chỉ", org.get("address"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Tỉnh / Thành phố", org.get("province"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Số điện thoại", org.get("phone"));
            appendCsvRowIfPresent(sb, "Đơn vị sản xuất (HTX)", "Email", org.get("email"));
        }
    }

    private void appendFarmAreaRows(StringBuilder sb, Map<String, Object> preview) {
        if (preview.get("farmArea") instanceof Map<?, ?> farmArea) {
            appendCsvRowIfPresent(sb, "Vùng trồng", "Tên vùng trồng", farmArea.get("name"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Tọa độ địa lý", farmArea.get("location"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Diện tích canh tác", farmArea.get("area"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Đơn vị diện tích", farmArea.get("areaUnit"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Loại cây trồng", farmArea.get("cropType"));
            appendCsvRowIfPresent(sb, "Vùng trồng", "Trạng thái vùng trồng", farmArea.get("isActive"));
        }
    }

    private void appendProductionLotRows(StringBuilder sb, Map<String, Object> preview) {
        if (preview.get("productionLot") instanceof Map<?, ?> lot) {
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Tên lô sản xuất", lot.get("name"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Danh mục sản phẩm", lot.get("productCategory"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Ngày xuống giống", lot.get("plantingDate"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Ngày thu hoạch", lot.get("harvestDate"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Sản lượng dự kiến", lot.get("expectedQuantity"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Đơn vị tính sản lượng", lot.get("expectedQuantityUnit"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Sản lượng thực tế", lot.get("actualQuantity"));
            appendCsvRowIfPresent(sb, "Lô sản xuất", "Trạng thái", lot.get("status"));
        }
    }

    private void appendShipmentRows(StringBuilder sb, Map<String, Object> preview) {
        if (preview.get("shipment") instanceof Map<?, ?> shipment) {
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Tên lô hàng", shipment.get("name"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Số lượng", shipment.get("totalQuantity"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Quy cách đóng gói", shipment.get("packagingInfo"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Trạng thái", shipment.get("status"));
            appendCsvRowIfPresent(sb, "Lô hàng vận chuyển", "Thời điểm tạo lô hàng", shipment.get("createdAt"));
        }
    }

    private void appendCertificationsSection(StringBuilder sb, Map<String, Object> preview) {
        if (preview.get("certifications") instanceof List<?> certs && !certs.isEmpty()) {
            sb.append("\n# CHỨNG NHẬN TIÊU CHUẨN\n");
            sb.append("STT,Tên chứng nhận,Tiêu chuẩn,Số hiệu,Ngày cấp,Hạn hiệu lực,Tổ chức chứng nhận\n");
            int idx = 1;
            for (Object item : certs) {
                if (item instanceof Map<?, ?> cItem) {
                    sb.append(idx++).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "name"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "standardName"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "certificationCode"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "issueDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "expiryDate"))).append(",");
                    sb.append(escapeCsv(getMapValue(cItem, "certifier"))).append("\n");
                }
            }
        }
    }

    private void appendFarmLogsSection(StringBuilder sb, Map<String, Object> preview) {
        if (!(preview.get("farmLogs") instanceof List<?> logs) || logs.isEmpty()) {
            return;
        }
        sb.append("\n# LỊCH TRÌNH CANH TÁC & CHỨNG TỪ\n");
        sb.append("STT,Ngày thực hiện,Hoạt động,Vật tư / Số lượng,Ghi chú,Chứng từ đính kèm\n");
        int idx = 1;
        for (Object item : logs) {
            if (item instanceof Map<?, ?> logItem) {
                appendFarmLogRow(sb, idx++, logItem);
            }
        }
    }

    private void appendFarmLogRow(StringBuilder sb, int idx, Map<?, ?> logItem) {
        sb.append(idx).append(",");
        sb.append(escapeCsv(getMapValue(logItem, "executedDate"))).append(",");
        sb.append(escapeCsv(getMapValue(logItem, "activityType"))).append(",");
        String mat = getMapValue(logItem, "material");
        Object qty = logItem.get("quantity");
        String unit = getMapValue(logItem, "unit");
        String matInfo = mat + (qty != null ? " (" + qty + (!unit.isBlank() ? " " + unit : "") + ")" : "");
        sb.append(escapeCsv(matInfo.trim())).append(",");
        sb.append(escapeCsv(getMapValue(logItem, "notes"))).append(",");

        String attStr = "";
        if (logItem.get("attachments") instanceof List<?> attList) {
            attStr = attList.stream().map(Object::toString).collect(Collectors.joining("; "));
        }
        sb.append(escapeCsv(attStr)).append("\n");
    }

    private void appendInspectionsSection(StringBuilder sb, Map<String, Object> preview) {
        if (!(preview.get("inspections") instanceof List<?> insps) || insps.isEmpty()) {
            return;
        }
        sb.append("\n# LỊCH SỬ KIỂM NGHIỆM\n");
        sb.append("STT,Ngày gửi mẫu,Đơn vị kiểm nghiệm,Chỉ tiêu / Tiêu chuẩn,Kết quả,Ngày cấp kết quả,Hạn hiệu lực\n");
        int idx = 1;
        for (Object item : insps) {
            if (item instanceof Map<?, ?> inspItem) {
                appendInspectionRow(sb, idx++, inspItem);
            }
        }
    }

    private void appendInspectionRow(StringBuilder sb, int idx, Map<?, ?> inspItem) {
        sb.append(idx).append(",");
        sb.append(escapeCsv(getMapValue(inspItem, "sampleSentDate"))).append(",");
        sb.append(escapeCsv(getMapValue(inspItem, "inspectionUnit"))).append(",");
        sb.append(escapeCsv(getMapValue(inspItem, "criterionName"))).append(",");
        String res = getMapValue(inspItem, "passed");
        if (res.isBlank()) {
            res = getMapValue(inspItem, "status");
        }
        sb.append(escapeCsv(res)).append(",");
        sb.append(escapeCsv(getMapValue(inspItem, "resultDate"))).append(",");
        sb.append(escapeCsv(getMapValue(inspItem, "expiryDate"))).append("\n");
    }

    private void appendTimelineSection(StringBuilder sb, Map<String, Object> preview) {
        if (preview.get("timelineEvents") instanceof List<?> events && !events.isEmpty()) {
            sb.append("\n# DÒNG SỰ KIỆN CHUỖI CUNG ỨNG\n");
            sb.append("STT,Thời điểm ghi nhận,Loại sự kiện,Tọa độ địa điểm,Chi tiết sự kiện,Người ghi nhận\n");
            int idx = 1;
            for (Object item : events) {
                if (item instanceof Map<?, ?> ev) {
                    sb.append(idx++).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "recordedAt"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "eventType"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "location"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "eventData"))).append(",");
                    sb.append(escapeCsv(getMapValue(ev, "recordedBy"))).append("\n");
                }
            }
        }
    }

    private void appendCsvRowIfPresent(StringBuilder sb, String group, String label, Object val) {
        if (val != null) {
            sb.append(escapeCsv(group)).append(",")
                    .append(escapeCsv(label)).append(",")
                    .append(escapeCsv(val.toString())).append("\n");
        }
    }

    private String getMapValue(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    /** Escape giá trị trường CSV theo chuẩn RFC 4180. */
    public String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
