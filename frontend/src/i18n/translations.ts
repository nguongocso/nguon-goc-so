export type Language = 'vi' | 'en';

export const translations = {
  vi: {
    // Header & Meta
    header_subtitle: 'Tra cứu hành trình sản phẩm',
    code_label: 'Mã tra cứu',
    loading_info: 'Đang tra cứu thông tin...',
    invalid_code_title: 'Mã Không Hợp Lệ',
    cancelled_code_title: 'Cảnh Báo: Mã Tem Đã Hủy',
    back_to_home: 'Về trang chủ',
    footer_copyright: 'Nguồn gốc số. Thông tin chỉ mang tính tham khảo.',

    // Product Info
    product_info_title: 'Thông tin sản phẩm & nguồn gốc',
    product_name_label: 'Tên nông sản',
    lot_name_label: 'Tên lô sản xuất',
    shipment_code_label: 'Tên lô hàng',
    not_updated: 'Chưa cập nhật',
    original_badge: 'Nội dung gốc',
    original_badge_tooltip: 'Dữ liệu gốc do đơn vị sản xuất nhập bằng tiếng Việt',

    // Status Labels
    status_active: 'Đang hoạt động',
    status_recalled: 'Đã thu hồi',
    status_recalling: 'Đang thu hồi',
    status_draft: 'Nháp',
    status_code_printed: 'Đã in mã',

    // Recall Alert
    recall_alert_title: 'CẢNH BÁO THU HỒI',

    // Lock & Verification
    locked_title: 'Tem Bị Khóa',
    locked_desc: 'Mã tem này tạm thời bị khóa do hệ thống phát hiện bất thường.',
    verified_note_title: 'Ghi Chú Mở Khóa / Xác Minh',
    reason_label: 'Lý do khóa:',
    locked_at_label: 'Thời gian khóa:',

    // Certifications
    certifications_title: 'Chứng nhận công khai',
    no_certifications: 'Chưa có chứng nhận công khai cho lô hàng này.',
    issued_by: 'Đơn vị cấp:',
    issue_date: 'Ngày cấp:',
    expiry_date: 'Hạn dùng:',
    cert_status_valid: 'Hợp lệ',
    cert_status_expired: 'Hết hạn',
    cert_status_pending: 'Đang chờ xác thực',
    cert_status_verified: 'Đã đạt chuẩn',

    // Inspections
    inspections_title: 'Kết quả kiểm nghiệm công khai',
    no_inspections: 'Chưa có kết quả kiểm nghiệm công khai cho lô hàng này.',
    total_criteria: 'Tổng chỉ tiêu',
    passed_criteria: 'Đạt chuẩn',
    failed_criteria: 'Không đạt',
    failed_ratio: 'Tỷ lệ không đạt',
    inspection_round: 'Lần kiểm nghiệm',
    measured_passed: 'Đạt chuẩn (Trong ngưỡng an toàn)',
    measured_failed: 'Không đạt (Vượt ngưỡng quy định)',
    passed_badge: 'Đạt',
    failed_badge: 'Không đạt',
    inspector_label: 'Người kiểm nghiệm:',
    laboratory_label: 'Phòng kiểm nghiệm:',

    // Tabs & Map
    map_tab: 'Bản đồ hành trình',
    list_tab: 'Nhật ký sự kiện',
    no_location_data: '(không có dữ liệu GPS)',

    // Feedback
    feedback_title: 'Gửi phản ánh sản phẩm',
    feedback_not_available: 'Chức năng gửi phản ánh không khả dụng cho sản phẩm này.',

    // Event Types
    event_HARVEST: 'Thu hoạch',
    event_PACKAGING: 'Đóng gói',
    event_TRANSPORT: 'Vận chuyển',
    event_INBOUND: 'Nhập kho',
    event_OUTBOUND: 'Xuất kho',
    event_PROCESSING: 'Chế biến',
    event_TESTING: 'Kiểm nghiệm',
    event_CUSTOMS: 'Thông quan',
    event_RECALL: 'Thu hồi',
    event_OTHER: 'Sự kiện khác',
  },
  en: {
    // Header & Meta
    header_subtitle: 'Product Traceability Lookup',
    code_label: 'Lookup Code',
    loading_info: 'Looking up information...',
    invalid_code_title: 'Invalid Trace Code',
    cancelled_code_title: 'Warning: Trace Code Cancelled',
    back_to_home: 'Back to Home',
    footer_copyright: 'Nguon Goc So. Information provided for reference only.',

    // Product Info
    product_info_title: 'Product & Origin Information',
    product_name_label: 'Product Name',
    lot_name_label: 'Production Lot',
    shipment_code_label: 'Shipment Code',
    not_updated: 'Not updated',
    original_badge: 'Original',
    original_badge_tooltip: 'Original content entered by producer in Vietnamese',

    // Status Labels
    status_active: 'Active',
    status_recalled: 'Recalled',
    status_recalling: 'Being Recalled',
    status_draft: 'Draft',
    status_code_printed: 'Code Printed',

    // Recall Alert
    recall_alert_title: 'RECALL WARNING',

    // Lock & Verification
    locked_title: 'Trace Code Locked',
    locked_desc: 'This trace code is temporarily locked due to anomaly detection.',
    verified_note_title: 'Unlock / Verification Note',
    reason_label: 'Lock Reason:',
    locked_at_label: 'Locked At:',

    // Certifications
    certifications_title: 'Public Certifications',
    no_certifications: 'No public certifications available for this shipment.',
    issued_by: 'Issued by:',
    issue_date: 'Issue Date:',
    expiry_date: 'Expiry Date:',
    cert_status_valid: 'Valid',
    cert_status_expired: 'Expired',
    cert_status_pending: 'Pending Verification',
    cert_status_verified: 'Verified Standard',

    // Inspections
    inspections_title: 'Public Inspection Results',
    no_inspections: 'No public inspection results available for this shipment.',
    total_criteria: 'Total Criteria',
    passed_criteria: 'Passed',
    failed_criteria: 'Failed',
    failed_ratio: 'Failure Rate',
    inspection_round: 'Inspection Round',
    measured_passed: 'Passed (Within Safe Threshold)',
    measured_failed: 'Failed (Exceeds Threshold)',
    passed_badge: 'Passed',
    failed_badge: 'Failed',
    inspector_label: 'Inspector:',
    laboratory_label: 'Laboratory:',

    // Tabs & Map
    map_tab: 'Route Map',
    list_tab: 'Event Timeline',
    no_location_data: '(no GPS data)',

    // Feedback
    feedback_title: 'Submit Product Feedback',
    feedback_not_available: 'Feedback submittal is not available for this product.',

    // Event Types
    event_HARVEST: 'Harvest',
    event_PACKAGING: 'Packaging',
    event_TRANSPORT: 'Transport',
    event_INBOUND: 'Inbound Warehouse',
    event_OUTBOUND: 'Outbound Warehouse',
    event_PROCESSING: 'Processing',
    event_TESTING: 'Quality Testing',
    event_CUSTOMS: 'Customs Clearance',
    event_RECALL: 'Recall',
    event_OTHER: 'Other Event',
  },
};

export function getTranslation(lang: Language, key: keyof typeof translations.vi): string {
  return translations[lang]?.[key] || translations.vi[key] || String(key);
}
