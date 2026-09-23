/**
 * Bảng ánh xạ nhãn hiển thị và màu sắc trạng thái cho các hành động trong hệ thống.
 * Cung cấp hàm tiện ích lấy nhãn và lớp CSS tương ứng theo mã hành động.
 */

/**
 * Nhãn tiếng Việt tương ứng cho từng mã hành động nghiệp vụ.
 */
export const ACTION_LABELS: Record<string, string> = {
  // Thao tác dữ liệu cơ bản
  CREATE: 'Tạo mới',
  UPDATE: 'Cập nhật',
  DELETE: 'Xóa',
  READ: 'Xem',

  // Phê duyệt
  APPROVE: 'Phê duyệt',
  REJECT: 'Từ chối',
  SUBMIT: 'Gửi duyệt',
  SUBMIT_PRODUCTION_LOT_FOR_APPROVAL: 'Gửi duyệt lô sản xuất',

  // Sự kiện chuỗi
  RECORD_HARVEST_EVENT: 'Ghi sự kiện thu hoạch',
  RECORD_PACKAGING_EVENT: 'Ghi sự kiện đóng gói',
  RECORD_TRANSPORT_EVENT: 'Ghi sự kiện vận chuyển',
  CORRECT_PACKAGING_EVENT: 'Đính chính đóng gói',

  // Lô hàng & Mã tem
  ACTIVATE: 'Kích hoạt',
  RECALL: 'Thu hồi',
  RECALL_SHIPMENT: 'Thu hồi lô hàng',
  EXPORT: 'Xuất hồ sơ',
  LOCK_TRACE_CODE: 'Khóa mã tem',
  UNLOCK_TRACE_CODE: 'Mở khóa mã tem',

  // Xác thực
  LOGIN: 'Đăng nhập',
  LOGOUT: 'Đăng xuất',

  // Lô sản xuất
  APPROVE_PRODUCTION_LOT: 'Phê duyệt lô sản xuất',
  SUBMIT_PRODUCTION_LOT: 'Gửi duyệt lô sản xuất',

  // Tổ chức & Thành viên
  CREATE_ORGANIZATION: 'Tạo tổ chức',
  UPDATE_ORGANIZATION: 'Cập nhật tổ chức',
  CREATE_INVITATION: 'Tạo thư mời',
  JOIN_ORGANIZATION: 'Tham gia tổ chức',
};

/**
 * Lớp CSS Tailwind màu sắc tương ứng cho từng mã hành động.
 */
export const ACTION_COLORS: Record<string, string> = {
  CREATE: 'bg-success-bg text-success',
  UPDATE: 'bg-info-bg text-info',
  DELETE: 'bg-error-bg text-destructive',

  APPROVE: 'bg-success-bg text-success',
  REJECT: 'bg-error-bg text-destructive',

  ACTIVATE: 'bg-info-bg text-info',
  RECALL: 'bg-warning-bg text-status-pending',
  RECALL_SHIPMENT: 'bg-warning-bg text-status-pending',
  LOCK_TRACE_CODE: 'bg-error-bg text-destructive',
  UNLOCK_TRACE_CODE: 'bg-success-bg text-success',

  EXPORT: 'bg-info-bg text-info',

  LOGIN: 'bg-muted text-muted-foreground',
  LOGOUT: 'bg-muted text-muted-foreground',

  SUBMIT: 'bg-warning-bg text-status-pending',
  SUBMIT_PRODUCTION_LOT_FOR_APPROVAL: 'bg-warning-bg text-status-pending',

  RECORD_HARVEST_EVENT: 'bg-success-bg text-success',
  RECORD_PACKAGING_EVENT: 'bg-success-bg text-success',
  RECORD_TRANSPORT_EVENT: 'bg-success-bg text-success',
  CORRECT_PACKAGING_EVENT: 'bg-info-bg text-info',

  APPROVE_PRODUCTION_LOT: 'bg-success-bg text-success',
  SUBMIT_PRODUCTION_LOT: 'bg-warning-bg text-status-pending',

  CREATE_ORGANIZATION: 'bg-success-bg text-success',
  UPDATE_ORGANIZATION: 'bg-info-bg text-info',
  CREATE_INVITATION: 'bg-info-bg text-info',
  JOIN_ORGANIZATION: 'bg-success-bg text-success',
};

/**
 * Lấy nhãn hiển thị tiếng Việt của hành động.
 *
 * @param action Mã hành động cần tra cứu.
 * @returns Nhãn tiếng Việt hoặc chính mã hành động nếu không tìm thấy.
 */
export const getActionLabel = (action: string): string => {
  return ACTION_LABELS[action] || action;
};

/**
 * Lấy lớp CSS màu sắc badge của hành động.
 *
 * @param action Mã hành động cần tra cứu.
 * @returns Chuỗi lớp CSS tương ứng hoặc lớp mặc định muted.
 */
export const getActionColor = (action: string): string => {
  return ACTION_COLORS[action] || 'bg-muted text-muted-foreground';
};
