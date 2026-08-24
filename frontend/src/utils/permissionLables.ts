// Mapping action sang tiếng Việt
export const actionLabels: Record<string, string> = {
  CREATE: 'Tạo / Thêm mới',
  READ: 'Xem danh sách & chi tiết',
  UPDATE: 'Cập nhật / Chỉnh sửa',
  DELETE: 'Xóa',
  APPROVE: 'Phê duyệt',
  VERIFY: 'Xác minh',
  EXPORT: 'Xuất dữ liệu / Hồ sơ',
  ACTIVATE: 'Kích hoạt',

  // Quyền sự kiện chuỗi cụ thể
  EVENT_FARM_LOG: 'Ghi nhật ký canh tác',
  EVENT_HARVEST: 'Ghi sự kiện thu hoạch',
  EVENT_PREPROCESSING: 'Ghi sự kiện sơ chế & phân loại',
  EVENT_PACKAGING: 'Ghi sự kiện đóng gói',
  EVENT_TRANSPORT: 'Ghi sự kiện vận chuyển',
};

// Mapping resource sang tiếng Việt (dùng nếu backend chưa trả về label tiếng Việt)
export const resourceLabels: Record<string, string> = {
  event_chain: 'Quyền ghi nhận sự kiện chuỗi',
  organization: 'Tổ chức',
  farm_area: 'Vùng trồng',
  production_lot: 'Lô sản xuất',
  farm_log: 'Nhật ký canh tác',
  shipment: 'Lô hàng',
  trace_code: 'Mã truy xuất',
  chain_event: 'Sự kiện chuỗi',
  certification: 'Chứng nhận',
  standard: 'Tiêu chuẩn',
  product_category: 'Loại nông sản',
  organization_user: 'Thành viên',
  role_permission: 'Phân quyền',
  notification: 'Thông báo',
  alert: 'Cảnh báo',
  report: 'Báo cáo',
  scan_statistics: 'Thống kê lượt quét',
  activity_log: 'Lịch sử hoạt động',
  user: 'Thành viên',
  role: 'Vai trò',
  export: 'Xuất dữ liệu',
  code_range: 'Dải mã truy xuất',
  traceability: 'Truy xuất nguồn gốc',
  recall: 'Thu hồi lô',
};

/**
 * Lấy tên hiển thị của resource, fallback về resource nếu không có mapping
 */
export const getResourceLabel = (resource: string): string => {
  return resourceLabels[resource] || resource;
};

/**
 * Lấy tên hiển thị của action, fallback về action nếu không có mapping
 */
export const getActionLabel = (action: string): string => {
  return actionLabels[action] || action;
};