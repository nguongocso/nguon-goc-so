package vn.nguongocso.event.service.processor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import vn.nguongocso.event.dto.request.RecordWarehouseEntryRequest;
import vn.nguongocso.event.dto.request.RecordWarehouseExitRequest;
import vn.nguongocso.farm.entity.ProductionLot;
import vn.nguongocso.trace.entity.Shipment;

/** Tạo dữ liệu sự kiện nhập, xuất kho và tính thời gian lưu kho. */
final class CoopWarehouseEventDataBuilder {
    private CoopWarehouseEventDataBuilder() {}

    static Map<String, Object> buildEntryData(
            Shipment shipment,
            ProductionLot lot,
            RecordWarehouseEntryRequest request) {
        Map<String, Object> data = buildShipmentData(shipment, lot);
        data.put("warehouseName", request.getWarehouseName());
        data.put("entryTime", request.getEntryTime().toString());
        putIfNotNull(data, "storageCondition", request.getStorageCondition());
        putIfNotNull(data, "notes", request.getNotes());
        putImages(data, request.getImages());
        data.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");
        return data;
    }

    static Map<String, Object> buildExitData(
            Shipment shipment,
            ProductionLot lot,
            String warehouseName,
            LocalDateTime entryTime,
            RecordWarehouseExitRequest request,
            DurationResult duration) {
        Map<String, Object> data = buildShipmentData(shipment, lot);
        data.put("warehouseName", warehouseName);
        data.put("entryTime", entryTime.toString());
        data.put("exitTime", request.getExitTime().toString());
        data.put("storageDurationDays", duration.storageDays());
        data.put("storageDurationHours", duration.storageHours());
        putIfNotNull(data, "maxAllowedStorageDays", duration.maxDays());
        data.put("isStorageExceeded", duration.isExceeded());
        putIfNotNull(data, "warningMessage", duration.warning());
        putIfNotNull(data, "destination", request.getDestination());
        putIfNotNull(data, "notes", request.getNotes());
        putImages(data, request.getImages());
        data.put("deviceSource", request.getDeviceSource() != null ? request.getDeviceSource() : "WEB");
        return data;
    }

    static DurationResult calculateStorageDuration(
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            ProductionLot lot) {
        long storageHours = Duration.between(entryTime, exitTime).toHours();
        long storageDays = Duration.between(entryTime, exitTime).toDays();
        Integer maxDays = lot != null && lot.getProductCategory() != null
                ? lot.getProductCategory().getMaxStorageDays()
                : null;

        boolean exceeded = maxDays != null && storageDays > maxDays;
        String warning = exceeded ? buildStorageWarning(lot, storageDays, maxDays) : null;
        return new DurationResult(storageHours, storageDays, maxDays, exceeded, warning);
    }

    private static Map<String, Object> buildShipmentData(Shipment shipment, ProductionLot lot) {
        Map<String, Object> data = new HashMap<>();
        data.put("shipmentId", shipment.getId().toString());
        data.put("shipmentName", shipment.getName());
        if (lot != null) {
            data.put("productionLotId", lot.getId().toString());
            data.put("productionLotName", lot.getName());
        }
        return data;
    }

    private static String buildStorageWarning(ProductionLot lot, long storageDays, int maxDays) {
        String categoryName = lot.getProductCategory() != null ? lot.getProductCategory().getName() : "";
        return "CẢNH BÁO: Thời gian lưu kho (" + storageDays
                + " ngày) vượt quá ngưỡng bảo quản cho phép (" + maxDays
                + " ngày) cho loại nông sản [" + categoryName + "]";
    }

    private static void putIfNotNull(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private static void putImages(Map<String, Object> data, java.util.List<String> images) {
        if (images != null && !images.isEmpty()) {
            data.put("images", images);
        }
    }

    record DurationResult(
            long storageHours,
            long storageDays,
            Integer maxDays,
            boolean isExceeded,
            String warning) {}
}
