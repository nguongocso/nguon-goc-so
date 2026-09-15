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
    status_suspected: 'Nghi vấn',
    status_locked: 'Đã khóa',
    status_cancelled: 'Đã hủy',

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
    feedback_desc: 'Nếu bạn nghi ngờ tem giả hoặc thấy thông tin của {productName} chưa chính xác, hãy gửi phản ánh để hợp tác xã kiểm tra.',
    feedback_content_label: 'Nội dung phản ánh *',
    feedback_placeholder: 'Ví dụ: Thông tin ngày thu hoạch trên hệ thống không khớp với bao bì sản phẩm.',
    feedback_validation_required: 'Vui lòng nhập nội dung phản ánh.',
    feedback_validation_max: 'Nội dung phản ánh không được vượt quá 1000 ký tự.',
    feedback_submitting: 'Đang gửi...',
    feedback_submit_btn: 'Gửi phản ánh',
    feedback_success_title: 'Đã gửi phản ánh',
    feedback_success_desc: 'Hợp tác xã sẽ tiếp nhận và cập nhật tiến độ xử lý trên hệ thống.',
    feedback_lookup_code_label: 'Mã tra cứu phản ánh của bạn',
    feedback_save_code_warning: 'Hãy lưu mã này ngay. Vì lý do bảo mật, hệ thống không thể hiển thị lại mã sau khi bạn rời trang.',
    feedback_copy_code_btn: 'Sao chép mã',
    feedback_lookup_status_btn: 'Tra cứu trạng thái',
    feedback_submit_another: 'Gửi phản ánh khác',
    feedback_toast_success: 'Đã gửi phản ánh. Vui lòng lưu mã tra cứu.',
    feedback_toast_copy_success: 'Đã sao chép mã tra cứu.',
    feedback_toast_copy_error: 'Không thể sao chép tự động. Vui lòng chọn và sao chép mã.',
    feedback_not_available: 'Chức năng gửi phản ánh không khả dụng cho sản phẩm này.',

    // Event Types
    event_HARVEST: 'Thu hoạch',
    event_PREPROCESSING: 'Sơ chế & Phân loại',
    event_PACKAGING: 'Đóng gói',
    event_TRANSPORT: 'Vận chuyển',
    event_PROCUREMENT: 'Thu mua',
    event_CORRECTION: 'Điều chỉnh',
    event_WAREHOUSE_RECEIPT: 'Nhập kho',
    event_STORAGE_CONDITION: 'Điều kiện bảo quản',
    event_WAREHOUSE_ENTRY: 'Nhập kho HTX',
    event_WAREHOUSE_EXIT: 'Xuất kho HTX',
    event_SPLIT: 'Tách lô hàng',
    event_HANDOVER: 'Bàn giao',
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
    status_suspected: 'Suspected',
    status_locked: 'Locked',
    status_cancelled: 'Cancelled',

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
    feedback_desc: 'If you suspect a counterfeit label or find inaccurate information for {productName}, please submit feedback for verification.',
    feedback_content_label: 'Feedback Content *',
    feedback_placeholder: 'Example: Harvest date in the system does not match the product packaging.',
    feedback_validation_required: 'Please enter feedback content.',
    feedback_validation_max: 'Feedback content must not exceed 1000 characters.',
    feedback_submitting: 'Submitting...',
    feedback_submit_btn: 'Submit Feedback',
    feedback_success_title: 'Feedback Submitted',
    feedback_success_desc: 'The cooperative will receive your feedback and update processing progress on the system.',
    feedback_lookup_code_label: 'Your feedback lookup code',
    feedback_save_code_warning: 'Please save this code now. For security reasons, the system cannot display it again after you leave this page.',
    feedback_copy_code_btn: 'Copy Code',
    feedback_lookup_status_btn: 'Check Status',
    feedback_submit_another: 'Submit Another Feedback',
    feedback_toast_success: 'Feedback submitted successfully. Please save your lookup code.',
    feedback_toast_copy_success: 'Lookup code copied to clipboard.',
    feedback_toast_copy_error: 'Unable to copy automatically. Please select and copy the code manually.',
    feedback_not_available: 'Feedback submittal is not available for this product.',

    // Event Types
    event_HARVEST: 'Harvest',
    event_PREPROCESSING: 'Preprocessing & Grading',
    event_PACKAGING: 'Packaging',
    event_TRANSPORT: 'Transport',
    event_PROCUREMENT: 'Procurement',
    event_CORRECTION: 'Correction',
    event_WAREHOUSE_RECEIPT: 'Warehouse Receipt',
    event_STORAGE_CONDITION: 'Storage Condition',
    event_WAREHOUSE_ENTRY: 'HTX Warehouse Inbound',
    event_WAREHOUSE_EXIT: 'HTX Warehouse Outbound',
    event_SPLIT: 'Shipment Split',
    event_HANDOVER: 'Handover',
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
