package vn.nguongocso.export.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.nguongocso.event.enums.ChainEventType;
import vn.nguongocso.farm.enums.AreaUnit;
import vn.nguongocso.farm.enums.FarmActivityType;
import vn.nguongocso.farm.enums.ProductionLotStatus;
import vn.nguongocso.organization.enums.OrganizationStatus;
import vn.nguongocso.organization.enums.OrganizationType;
import vn.nguongocso.trace.enums.ShipmentStatus;

/** Tiện ích Việt hóa các giá trị Enum và dữ liệu hồ sơ xuất (PDF, CSV, JSON). */
public final class ExportDisplayFormatter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<ProductionLotStatus, String> LOT_STATUS_MAP = createLotStatusMap();
    private static final Map<ChainEventType, String> CHAIN_EVENT_TYPE_MAP = createChainEventTypeMap();
    private static final Map<String, String> EVENT_KEY_MAP = createEventKeyMap();

    private ExportDisplayFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Việt hóa Loại tổ chức. */
    public static String formatOrganizationType(OrganizationType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case COOPERATIVE -> "Hợp tác xã";
            case ENTERPRISE -> "Doanh nghiệp";
            case GOVERNMENT -> "Cán bộ quản lý ngành";
            case SYSTEM -> "Tổ chức hệ thống";
        };
    }

    /** Việt hóa Trạng thái tổ chức. */
    public static String formatOrganizationStatus(OrganizationStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "Đang hoạt động";
            case INACTIVE -> "Ngừng hoạt động";
        };
    }

    /** Việt hóa Trạng thái lô sản xuất. */
    public static String formatProductionLotStatus(ProductionLotStatus status) {
        if (status == null) {
            return null;
        }
        return LOT_STATUS_MAP.get(status);
    }

    /** Việt hóa Trạng thái lô hàng vận chuyển. */
    public static String formatShipmentStatus(ShipmentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case CODE_PRINTED -> "Đã in mã";
            case ACTIVATED -> "Đã kích hoạt";
            case SPLIT -> "Đã tách lô";
            case RECALLING -> "Đang thu hồi";
            case RECALLED -> "Đã thu hồi";
        };
    }

    /** Việt hóa Hoạt động canh tác. */
    public static String formatFarmActivityType(FarmActivityType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PLANTING -> "Gieo giống / Xuống giống";
            case WATERING -> "Tưới nước";
            case FERTILIZING -> "Bón phân";
            case PESTICIDE -> "Phun thuốc BVTV";
            case WEEDING -> "Làm cỏ";
            case HARVESTING -> "Thu hoạch";
            case OTHER -> "Hoạt động khác";
        };
    }

    /** Việt hóa Loại sự kiện chuỗi cung ứng. */
    public static String formatChainEventType(ChainEventType type) {
        if (type == null) {
            return null;
        }
        return CHAIN_EVENT_TYPE_MAP.get(type);
    }

    /** Việt hóa Đơn vị diện tích. */
    public static String formatAreaUnit(AreaUnit unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit) {
            case HA -> "ha";
            case KM2 -> "km²";
        };
    }

    /** Định dạng chi tiết dữ liệu sự kiện dạng văn bản tiếng Việt dễ hiểu. */
    public static String formatEventData(String rawJson, String delimiter) {
        if (rawJson == null || rawJson.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    entries.add(formatSingleEntry(entry.getKey(), entry.getValue()));
                }
            }
            return String.join(delimiter != null ? delimiter : "; ", entries);
        } catch (Exception e) {
            String cleaned = rawJson.replaceAll("data:image/[^;\"]+;base64,[^\"]+", "[Tệp hình ảnh]");
            return cleaned.trim();
        }
    }

    private static String formatSingleEntry(String key, Object value) {
        String displayKey = formatEventKey(key);
        if (isImageKey(key)) {
            if (value instanceof List<?> list) {
                return "Hình ảnh: " + list.size() + " tệp";
            }
            if (value instanceof String strVal && strVal.startsWith("data:image/")) {
                return "Hình ảnh: 1 tệp";
            }
            return displayKey + ": " + value;
        }
        if (value instanceof String strVal && strVal.startsWith("data:image/")) {
            return displayKey + ": [Hình ảnh]";
        }
        return displayKey + ": " + value;
    }

    private static boolean isImageKey(String key) {
        return "images".equalsIgnoreCase(key) || "photos".equalsIgnoreCase(key) || "attachments".equalsIgnoreCase(key);
    }

    private static String formatEventKey(String key) {
        if (key == null) {
            return "";
        }
        return EVENT_KEY_MAP.getOrDefault(key.trim().toLowerCase(), key);
    }

    private static Map<ProductionLotStatus, String> createLotStatusMap() {
        Map<ProductionLotStatus, String> map = new EnumMap<>(ProductionLotStatus.class);
        map.put(ProductionLotStatus.DRAFT, "Bản nháp");
        map.put(ProductionLotStatus.PENDING, "Chờ duyệt");
        map.put(ProductionLotStatus.APPROVED, "Đã duyệt");
        map.put(ProductionLotStatus.REJECTED, "Bị từ chối");
        map.put(ProductionLotStatus.HARVESTED, "Đã thu hoạch");
        map.put(ProductionLotStatus.PREPROCESSED, "Đã sơ chế");
        map.put(ProductionLotStatus.PACKAGED, "Đã đóng gói");
        map.put(ProductionLotStatus.CLOSED, "Đã hoàn thành");
        map.put(ProductionLotStatus.RECALLED, "Đã thu hồi");
        map.put(ProductionLotStatus.CANCELLED, "Đã hủy");
        map.put(ProductionLotStatus.DISPOSED, "Đã loại bỏ");
        return map;
    }

    private static Map<ChainEventType, String> createChainEventTypeMap() {
        Map<ChainEventType, String> map = new EnumMap<>(ChainEventType.class);
        map.put(ChainEventType.HARVEST, "Thu hoạch");
        map.put(ChainEventType.PREPROCESSING, "Sơ chế và phân loại");
        map.put(ChainEventType.PACKAGING, "Đóng gói");
        map.put(ChainEventType.TRANSPORT, "Vận chuyển");
        map.put(ChainEventType.PROCUREMENT, "Thu mua");
        map.put(ChainEventType.CORRECTION, "Điều chỉnh dữ liệu");
        map.put(ChainEventType.WAREHOUSE_RECEIPT, "Nhập kho đối chiếu");
        map.put(ChainEventType.STORAGE_CONDITION, "Theo dõi bảo quản");
        map.put(ChainEventType.HANDOVER, "Bàn giao");
        map.put(ChainEventType.WAREHOUSE_ENTRY, "Nhập kho HTX");
        map.put(ChainEventType.WAREHOUSE_EXIT, "Xuất kho HTX");
        map.put(ChainEventType.SPLIT, "Tách lô");
        map.put(ChainEventType.FARM_LOG, "Nhật ký canh tác");
        return map;
    }

    private static Map<String, String> createEventKeyMap() {
        Map<String, String> map = new HashMap<>();
        map.put("notes", "Ghi chú");
        map.put("note", "Ghi chú");
        map.put("shipmentid", "Mã lô hàng");
        map.put("shipmentname", "Tên lô hàng");
        map.put("receivedquantity", "Số lượng");
        map.put("quantity", "Số lượng");
        map.put("tolocation", "Nơi đến");
        map.put("destination", "Nơi đến");
        map.put("fromlocation", "Nơi đi");
        map.put("origin", "Nơi đi");
        map.put("devicesource", "Nguồn thiết bị");
        map.put("licenseplate", "Biển số xe");
        map.put("vehiclenumber", "Biển số xe");
        map.put("drivername", "Tài xế");
        map.put("driver", "Tài xế");
        map.put("storagetemp", "Nhiệt độ");
        map.put("temperature", "Nhiệt độ");
        map.put("humidity", "Độ ẩm");
        map.put("weightkg", "Trọng lượng (kg)");
        map.put("packagingtype", "Quy cách đóng gói");
        map.put("santhuong", "Sản lượng");
        map.put("sanluong", "Sản lượng");
        map.put("sanluongkg", "Sản lượng");
        map.put("phuongthuc", "Phương thức");
        map.put("phuongthucthuhoach", "Phương thức");
        map.put("sothung", "Số thùng");
        map.put("quycach", "Quy cách");
        map.put("productionlotid", "Mã lô sản xuất");
        map.put("lotid", "Mã lô sản xuất");
        map.put("ngaythuhoach", "Ngày thu hoạch");
        return map;
    }
}
