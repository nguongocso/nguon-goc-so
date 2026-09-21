package vn.nguongocso.export.util;

import java.util.ArrayList;
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

/**
 * Tiện ích Việt hóa các giá trị Enum và dữ liệu hồ sơ xuất (PDF, CSV, JSON).
 */
public final class ExportDisplayFormatter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ExportDisplayFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Việt hóa Loại tổ chức.
     */
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

    /**
     * Việt hóa Trạng thái tổ chức.
     */
    public static String formatOrganizationStatus(OrganizationStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "Đang hoạt động";
            case INACTIVE -> "Ngừng hoạt động";
        };
    }

    /**
     * Việt hóa Trạng thái lô sản xuất.
     */
    public static String formatProductionLotStatus(ProductionLotStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
            case HARVESTED -> "Đã thu hoạch";
            case PREPROCESSED -> "Đã sơ chế";
            case PACKAGED -> "Đã đóng gói";
            case CLOSED -> "Đã hoàn thành";
            case RECALLED -> "Đã thu hồi";
            case CANCELLED -> "Đã hủy";
            case DISPOSED -> "Đã loại bỏ";
        };
    }

    /**
     * Việt hóa Trạng thái lô hàng vận chuyển.
     */
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

    /**
     * Việt hóa Hoạt động canh tác.
     */
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

    /**
     * Việt hóa Loại sự kiện chuỗi cung ứng.
     */
    public static String formatChainEventType(ChainEventType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case HARVEST -> "Thu hoạch";
            case PREPROCESSING -> "Sơ chế và phân loại";
            case PACKAGING -> "Đóng gói";
            case TRANSPORT -> "Vận chuyển";
            case PROCUREMENT -> "Thu mua";
            case CORRECTION -> "Điều chỉnh dữ liệu";
            case WAREHOUSE_RECEIPT -> "Nhập kho đối chiếu";
            case STORAGE_CONDITION -> "Theo dõi bảo quản";
            case HANDOVER -> "Bàn giao";
            case WAREHOUSE_ENTRY -> "Nhập kho HTX";
            case WAREHOUSE_EXIT -> "Xuất kho HTX";
            case SPLIT -> "Tách lô";
            case FARM_LOG -> "Nhật ký canh tác";
        };
    }

    /**
     * Việt hóa Đơn vị diện tích.
     */
    public static String formatAreaUnit(AreaUnit unit) {
        if (unit == null) {
            return null;
        }
        return switch (unit) {
            case HA -> "ha";
            case KM2 -> "km²";
        };
    }

    /**
     * Định dạng chi tiết dữ liệu sự kiện dạng văn bản tiếng Việt dễ hiểu.
     */
    public static String formatEventData(String rawJson, String delimiter) {
        if (rawJson == null || rawJson.isBlank()) {
            return "";
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;
                String displayKey = formatEventKey(key);

                if ("images".equalsIgnoreCase(key) || "photos".equalsIgnoreCase(key) || "attachments".equalsIgnoreCase(key)) {
                    if (value instanceof List) {
                        entries.add("Hình ảnh: " + ((List<?>) value).size() + " tệp");
                    } else if (value instanceof String strVal && strVal.startsWith("data:image/")) {
                        entries.add("Hình ảnh: 1 tệp");
                    } else {
                        entries.add(displayKey + ": " + value);
                    }
                } else if (value instanceof String strVal && strVal.startsWith("data:image/")) {
                    entries.add(displayKey + ": [Hình ảnh]");
                } else {
                    entries.add(displayKey + ": " + value);
                }
            }
            return String.join(delimiter != null ? delimiter : "; ", entries);
        } catch (Exception e) {
            String cleaned = rawJson.replaceAll("data:image/[^;\"]+;base64,[^\"]+", "[Tệp hình ảnh]");
            return cleaned.trim();
        }
    }

    private static String formatEventKey(String key) {
        if (key == null) return "";
        return switch (key.trim().toLowerCase()) {
            case "notes", "note" -> "Ghi chú";
            case "shipmentid" -> "Mã lô hàng";
            case "shipmentname" -> "Tên lô hàng";
            case "receivedquantity", "quantity" -> "Số lượng";
            case "tolocation", "destination" -> "Nơi đến";
            case "fromlocation", "origin" -> "Nơi đi";
            case "devicesource" -> "Nguồn thiết bị";
            case "licenseplate", "vehiclenumber" -> "Biển số xe";
            case "drivername", "driver" -> "Tài xế";
            case "storagetemp", "temperature" -> "Nhiệt độ";
            case "humidity" -> "Độ ẩm";
            case "weightkg" -> "Trọng lượng (kg)";
            case "packagingtype" -> "Quy cách đóng gói";
            case "santhuong", "sanluong", "sanluongkg" -> "Sản lượng";
            case "phuongthuc", "phuongthucthuhoach" -> "Phương thức";
            case "sothung" -> "Số thùng";
            case "quycach" -> "Quy cách";
            case "productionlotid", "lotid" -> "Mã lô sản xuất";
            case "ngaythuhoach" -> "Ngày thu hoạch";
            default -> key;
        };
    }
}
