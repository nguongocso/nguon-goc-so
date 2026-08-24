package vn.nguongocso.permission.constant;

import java.util.Map;

/**
 * Tên hiển thị của các nhóm chức năng và mã quyền sự kiện chuỗi.
 */
public final class ResourceLabelConstants {

    // Mã quyền chuẩn cho sự kiện chuỗi
    public static final String EVENT_FARM_LOG = "EVENT_FARM_LOG";           // Ghi nhật ký canh tác
    public static final String EVENT_HARVEST = "EVENT_HARVEST";             // Ghi sự kiện thu hoạch
    public static final String EVENT_PREPROCESSING = "EVENT_PREPROCESSING"; // Ghi sự kiện sơ chế & phân loại
    public static final String EVENT_PACKAGING = "EVENT_PACKAGING";         // Ghi sự kiện đóng gói
    public static final String EVENT_TRANSPORT = "EVENT_TRANSPORT";         // Ghi sự kiện vận chuyển

    private ResourceLabelConstants() {
    }

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("event_chain", "Quyền ghi nhận sự kiện chuỗi"),
            Map.entry("organization", "Tổ chức"),
            Map.entry("farm_area", "Vùng trồng"),
            Map.entry("production_lot", "Lô sản xuất"),
            Map.entry("farm_log", "Nhật ký canh tác"),
            Map.entry("shipment", "Lô hàng"),
            Map.entry("trace_code", "Mã truy xuất"),
            Map.entry("chain_event", "Sự kiện chuỗi"),
            Map.entry("certification", "Chứng nhận"),
            Map.entry("standard", "Tiêu chuẩn"),
            Map.entry("product_category", "Loại nông sản"),
            Map.entry("organization_user", "Thành viên"),
            Map.entry("role_permission", "Phân quyền"),
            Map.entry("notification", "Thông báo"),
            Map.entry("alert", "Cảnh báo"),
            Map.entry("report", "Báo cáo"),
            Map.entry("scan_statistics", "Thống kê lượt quét"),
            Map.entry("activity_log", "Lịch sử hoạt động"),
            Map.entry("user", "Thành viên"),
            Map.entry("role", "Vai trò"),
            Map.entry("export", "Xuất dữ liệu"),
            Map.entry("code_range", "Dải mã truy xuất"),
            Map.entry("traceability", "Truy xuất nguồn gốc"),
            Map.entry("recall", "Thu hồi lô"),
            Map.entry("product_feedback", "Phản ánh sản phẩm"));

    /**
     * Lấy tên hiển thị của nhóm chức năng dựa trên tên tài nguyên.
     *
     * @param resource tên tài nguyên
     * @return tên hiển thị của nhóm chức năng, hoặc chính tên tài nguyên nếu không
     *         tìm thấy
     */
    public static String getLabel(String resource) {
        return LABELS.getOrDefault(resource, resource);
    }
}