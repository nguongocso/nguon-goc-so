package vn.nguongocso.alert.service.impl;

import java.util.Locale;
import java.util.Map;

/** Việt hóa các mã nghiệp vụ khi trình bày trong tệp xuất nhật ký hoạt động. */
final class ActivityLogExportLabelFormatter {
    private static final Map<String, String> ACTION_LABELS = Map.ofEntries(
            Map.entry("CREATE", "Tạo mới"),
            Map.entry("UPDATE", "Cập nhật"),
            Map.entry("DELETE", "Xóa"),
            Map.entry("DISPOSE", "Loại bỏ"),
            Map.entry("READ", "Xem"),
            Map.entry("APPROVE", "Phê duyệt"),
            Map.entry("REJECT", "Từ chối / Trả lại"),
            Map.entry("SUBMIT", "Gửi duyệt"),
            Map.entry("LOCK", "Khóa tài khoản / Khóa tem"),
            Map.entry("UNLOCK", "Mở khóa"),
            Map.entry("LOGIN", "Đăng nhập hệ thống"),
            Map.entry("LOGOUT", "Đăng xuất"),
            Map.entry("ACTIVATE", "Kích hoạt"),
            Map.entry("RECALL", "Thu hồi lô"),
            Map.entry("RECALL_SHIPMENT", "Thu hồi lô hàng"),
            Map.entry("RECORD_EVENT", "Ghi sự kiện chuỗi"),
            Map.entry("RECORD_HARVEST_EVENT", "Ghi sự kiện thu hoạch"),
            Map.entry("RECORD_PREPROCESSING_EVENT", "Ghi sự kiện sơ chế"),
            Map.entry("CORRECT_PREPROCESSING_EVENT", "Đính chính sơ chế"),
            Map.entry("RECORD_PACKAGING_EVENT", "Ghi sự kiện đóng gói"),
            Map.entry("CORRECT_PACKAGING_EVENT", "Đính chính đóng gói"),
            Map.entry("RECORD_TRANSPORT_EVENT", "Ghi sự kiện vận chuyển"),
            Map.entry("RECORD_PROCUREMENT_EVENT", "Ghi sự kiện thu mua"),
            Map.entry("RECORD_WAREHOUSE_RECEIPT", "Ghi nhận nhập kho"),
            Map.entry("RECORD_STORAGE_CONDITION", "Ghi điều kiện bảo quản"),
            Map.entry("RECORD_HANDOVER_EVENT", "Ghi sự kiện bàn giao"),
            Map.entry("HANDOVER", "Bàn giao"),
            Map.entry("CREATE_HANDOVER", "Tạo phiếu bàn giao"),
            Map.entry("ACCEPT_HANDOVER", "Xác nhận bàn giao"),
            Map.entry("REJECT_HANDOVER", "Từ chối bàn giao"),
            Map.entry("CANCEL_HANDOVER", "Hủy phiếu bàn giao"),
            Map.entry("EXPORT", "Xuất hồ sơ nguồn gốc"),
            Map.entry("EXPORT_DOSSIER", "Xuất hồ sơ nguồn gốc"),
            Map.entry("GS1_DOSSIER_EXPORT", "Xuất hồ sơ GS1"),
            Map.entry("EXPORT_ACTIVITY_LOG", "Xuất nhật ký hoạt động"),
            Map.entry("CREATE_RECALL_REQUEST", "Tạo yêu cầu thu hồi"),
            Map.entry("APPROVE_RECALL_REQUEST", "Phê duyệt yêu cầu thu hồi"),
            Map.entry("REJECT_RECALL_REQUEST", "Từ chối yêu cầu thu hồi"),
            Map.entry("CREATE_BULK_RECALL_REQUEST", "Tạo yêu cầu thu hồi hàng loạt"),
            Map.entry("APPROVE_BULK_RECALL_REQUEST", "Phê duyệt yêu cầu thu hồi hàng loạt"),
            Map.entry("REJECT_BULK_RECALL_REQUEST", "Từ chối yêu cầu thu hồi hàng loạt"),
            Map.entry("CREATE_CERTIFICATION", "Tạo chứng nhận"),
            Map.entry("UPDATE_CERTIFICATION", "Cập nhật chứng nhận"),
            Map.entry("DELETE_CERTIFICATION", "Xóa chứng nhận"),
            Map.entry("ATTACH_CERTIFICATION", "Gắn chứng nhận"),
            Map.entry("VERIFY_CERTIFICATION", "Xác thực chứng nhận"),
            Map.entry("REJECT_CERTIFICATION", "Từ chối chứng nhận"),
            Map.entry("CREATE_PRODUCTION_LOT", "Tạo lô sản xuất"),
            Map.entry("UPDATE_PRODUCTION_LOT", "Cập nhật lô sản xuất"),
            Map.entry("SUBMIT_PRODUCTION_LOT", "Gửi duyệt lô sản xuất"),
            Map.entry("SUBMIT_PRODUCTION_LOT_FOR_APPROVAL", "Gửi duyệt lô sản xuất"),
            Map.entry("APPROVE_PRODUCTION_LOT", "Phê duyệt lô sản xuất"),
            Map.entry("CREATE_FARM_LOG", "Ghi nhật ký canh tác"),
            Map.entry("UPDATE_FARM_LOG", "Cập nhật nhật ký canh tác"),
            Map.entry("DELETE_FARM_LOG", "Xóa nhật ký canh tác"),
            Map.entry("CREATE_FARM_AREA", "Tạo vùng trồng"),
            Map.entry("UPDATE_FARM_AREA", "Cập nhật vùng trồng"),
            Map.entry("DELETE_FARM_AREA", "Xóa vùng trồng"),
            Map.entry("CREATE_INPUT_MATERIAL", "Tạo vật tư nông nghiệp"),
            Map.entry("UPDATE_INPUT_MATERIAL", "Cập nhật vật tư nông nghiệp"),
            Map.entry("DELETE_INPUT_MATERIAL", "Xóa vật tư nông nghiệp"),
            Map.entry("ASSIGN_AREA", "Gán địa bàn"),
            Map.entry("UNASSIGN_AREA", "Gỡ địa bàn"),
            Map.entry("CREATE_PRODUCT_CATEGORY", "Tạo loại nông sản"),
            Map.entry("UPDATE_PRODUCT_CATEGORY", "Cập nhật loại nông sản"),
            Map.entry("DELETE_PRODUCT_CATEGORY", "Xóa loại nông sản"),
            Map.entry("CREATE_ORGANIZATION", "Tạo tổ chức"),
            Map.entry("UPDATE_ORGANIZATION", "Cập nhật tổ chức"),
            Map.entry("UPDATE_ORGANIZATION_PROFILE", "Cập nhật hồ sơ tổ chức"),
            Map.entry("CREATE_INVITATION", "Tạo thư mời"),
            Map.entry("JOIN_ORGANIZATION", "Tham gia tổ chức"),
            Map.entry("ADD_EXISTING_USER", "Thêm thành viên"),
            Map.entry("CREATE_MEMBER", "Thêm thành viên"),
            Map.entry("UPDATE_ROLE_PERMISSIONS", "Cấu hình quyền vai trò"),
            Map.entry("ACCESS_DENIED", "Truy cập trái phép bị chặn"),
            Map.entry("CREATE_API_KEY", "Cấp API key đối tác"),
            Map.entry("REVOKE_API_KEY", "Thu hồi API key"),
            Map.entry("CREATE_INSPECTION_REQUEST", "Tạo yêu cầu kiểm nghiệm"),
            Map.entry("RECORD_INSPECTION_RESULT", "Ghi kết quả kiểm nghiệm"),
            Map.entry("UPDATE_INSPECTION_RESULT", "Cập nhật kết quả kiểm nghiệm"),
            Map.entry("RECORD_INSPECTION_RESULTS", "Ghi hàng loạt kết quả kiểm nghiệm"),
            Map.entry("DELETE_INSPECTION_RESULT", "Xóa kết quả kiểm nghiệm"),
            Map.entry("UPLOAD_INSPECTION_RESULT_FILE", "Tải phiếu kết quả"),
            Map.entry("DELETE_SHIPMENT_DRAFT", "Hủy bản nháp lô hàng"),
            Map.entry("RESOLVE_ALERT", "Xử lý cảnh báo"),
            Map.entry("DEACTIVATE", "Vô hiệu hóa thành viên"),
            Map.entry("REACTIVATE", "Kích hoạt lại thành viên"),
            Map.entry("DEACTIVATE_BLOCKED", "Từ chối vô hiệu hóa thành viên"));

    /** 
     * Danh mục các loại đối tượng được ghi nhật ký hoạt động.
     */
    private static final Map<String, String> OBJECT_LABELS = Map.ofEntries(
            Map.entry("PRODUCTIONLOT", "Lô sản xuất"),
            Map.entry("FARMLOG", "Nhật ký canh tác"),
            Map.entry("FARMLOGATTACHMENT", "Chứng từ nhật ký"),
            Map.entry("FARMAREA", "Vùng trồng"),
            Map.entry("SHIPMENT", "Lô hàng"),
            Map.entry("SHIPMENTHANDOVER", "Phiếu bàn giao"),
            Map.entry("HANDOVER", "Phiếu bàn giao"),
            Map.entry("CHAINEVENT", "Sự kiện chuỗi"),
            Map.entry("PRODUCTCATEGORY", "Loại nông sản"),
            Map.entry("CERTIFICATION", "Chứng nhận chất lượng"),
            Map.entry("USER", "Tài khoản người dùng"),
            Map.entry("ORGANIZATIONUSER", "Thành viên tổ chức"),
            Map.entry("ORGANIZATION", "Tổ chức"),
            Map.entry("ORGANIZATIONROLEPERMISSION", "Phân quyền vai trò"),
            Map.entry("ROLEPERMISSION", "Phân quyền vai trò"),
            Map.entry("PARTNERAPIKEY", "Khóa API đối tác"),
            Map.entry("APIKEY", "Khóa API đối tác"),
            Map.entry("INSPECTIONREQUEST", "Yêu cầu kiểm nghiệm"),
            Map.entry("INSPECTIONCRITERIONRESULT", "Kết quả tiêu chí kiểm nghiệm"),
            Map.entry("INSPECTIONCRITERION", "Tiêu chí kiểm nghiệm"),
            Map.entry("RECALLREQUEST", "Yêu cầu thu hồi"),
            Map.entry("RECALL", "Yêu cầu thu hồi"),
            Map.entry("BULKRECALLREQUEST", "Yêu cầu thu hồi hàng loạt"),
            Map.entry("WAREHOUSERECEIPT", "Phiếu nhập kho"),
            Map.entry("INPUTMATERIAL", "Vật tư nông nghiệp"),
            Map.entry("STANDARD", "Tiêu chuẩn chất lượng"),
            Map.entry("CODERANGE", "Dải mã truy xuất"),
            Map.entry("TRACECODE", "Mã tem QR"),
            Map.entry("BACKUPSCHEDULE", "Lịch sao lưu"),
            Map.entry("BACKUPRESTORE", "Sao lưu và phục hồi"),
            Map.entry("ADMINISTRATIVEUNIT", "Đơn vị hành chính"),
            Map.entry("USERAREAASSIGNMENT", "Phân công địa bàn"),
            Map.entry("PRODUCTFEEDBACK", "Phản ánh sản phẩm"),
            Map.entry("OFFLINESYNCLOG", "Đồng bộ ngoại tuyến"),
            Map.entry("PASSWORDRESETTOKEN", "Đặt lại mật khẩu"),
            Map.entry("ACCOUNTLOCK", "Khóa tài khoản"),
            Map.entry("STORAGECONDITION", "Điều kiện bảo quản"),
            Map.entry("ALERT", "Cảnh báo hệ thống"),
            Map.entry("SYSTEMMONITORING", "Giám sát hệ thống"),
            Map.entry("ATTACHMENT", "Chứng từ đính kèm"),
            Map.entry("INVITATION", "Thư mời thành viên"),
            Map.entry("ACTIVITYLOGEXPORT", "Yêu cầu xuất nhật ký hoạt động"));

    /** 
     * Danh mục các loại vai trò.
     */
    private static final Map<String, String> ROLE_LABELS = Map.of(
            "VT-01", "Quản trị viên nền tảng (VT-01)",
            "VT-02", "Quản lý hợp tác xã (VT-02)",
            "VT-03", "Người ghi sự kiện (VT-03)",
            "VT-04", "Doanh nghiệp thu mua (VT-04)",
            "VT-05", "Cán bộ quản lý ngành (VT-05)");

    /**
     * Constructor.
     */
    private ActivityLogExportLabelFormatter() {
    }

    /**
     * Định dạng hành động.
     */
    static String formatAction(String value) {
        return format(ACTION_LABELS, value, false);
    }

    /**
     * Định dạng loại đối tượng.
     */
    static String formatObjectType(String value) {
        return format(OBJECT_LABELS, value, true);
    }

    /**
     * Định dạng vai trò.
     */
    static String formatRole(String value) {
        if (value == null || value.isBlank())
            return value;
        return ROLE_LABELS.getOrDefault(value.toUpperCase(Locale.ROOT), value);
    }

    /**
     * Định dạng chuỗi.
     */
    private static String format(Map<String, String> labels, String value, boolean removeUnderscores) {
        if (value == null || value.isBlank())
            return value;
        String key = value.toUpperCase(Locale.ROOT);
        if (removeUnderscores)
            key = key.replace("_", "");
        return labels.getOrDefault(key, value);
    }
}
