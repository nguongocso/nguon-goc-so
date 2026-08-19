export const ACTION_LABELS: Record<string, string> = {
  // Basic CRUD
  CREATE: 'Tạo mới',
  UPDATE: 'Cập nhật',
  DELETE: 'Xóa',
  READ: 'Xem',

  // Approval
  APPROVE: 'Phê duyệt',
  REJECT: 'Từ chối',
  SUBMIT: 'Gửi duyệt',
  SUBMIT_PRODUCTION_LOT_FOR_APPROVAL: 'Gửi duyệt lô sản xuất',

  // Events
  RECORD_HARVEST_EVENT: 'Ghi sự kiện thu hoạch',
  RECORD_PACKAGING_EVENT: 'Ghi sự kiện đóng gói',
  RECORD_TRANSPORT_EVENT: 'Ghi sự kiện vận chuyển',
  CORRECT_PACKAGING_EVENT: 'Đính chính đóng gói',

  // Shipment
  ACTIVATE: 'Kích hoạt',
  RECALL: 'Thu hồi',
  RECALL_SHIPMENT: 'Thu hồi lô hàng',
  EXPORT: 'Xuất hồ sơ',

  // Authentication
  LOGIN: 'Đăng nhập',
  LOGOUT: 'Đăng xuất',

  // Production Lot
  APPROVE_PRODUCTION_LOT: 'Phê duyệt lô sản xuất',
  SUBMIT_PRODUCTION_LOT: 'Gửi duyệt lô sản xuất',

  // Organization, Members, Invitations
  CREATE_ORGANIZATION: 'Tạo tổ chức',
  UPDATE_ORGANIZATION: 'Cập nhật tổ chức',
  CREATE_INVITATION: 'Tạo thư mời',
  JOIN_ORGANIZATION: 'Tham gia tổ chức',
};

export const ACTION_COLORS: Record<string, string> = {
  CREATE: 'bg-success-bg text-success',
  UPDATE: 'bg-info-bg text-info',
  DELETE: 'bg-error-bg text-destructive',

  APPROVE: 'bg-success-bg text-success',
  REJECT: 'bg-error-bg text-destructive',

  ACTIVATE: 'bg-info-bg text-info',
  RECALL: 'bg-warning-bg text-status-pending',
  RECALL_SHIPMENT: 'bg-warning-bg text-status-pending',

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

export const getActionLabel = (action: string): string => {
  return ACTION_LABELS[action] || action;
};

export const getActionColor = (action: string): string => {
  return ACTION_COLORS[action] || 'bg-muted text-muted-foreground';
};