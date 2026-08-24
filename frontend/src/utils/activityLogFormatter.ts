/**
 * Tiện ích định dạng và Việt hóa nhật ký hoạt động hệ thống.
 */

// Hàm chuẩn hóa chuỗi (loại bỏ gạch dưới và viết hoa) để so khớp linh hoạt camelCase và snake_case
const normalizeKey = (key: string): string => {
  return key.replace(/_/g, '').toUpperCase();
};

export const formatActionType = (action: string): string => {
  if (!action) return '—';

  const exactMap: Record<string, string> = {
    CREATE: 'Tạo mới',
    UPDATE: 'Cập nhật',
    DELETE: 'Xóa',
    READ: 'Xem',
    APPROVE: 'Phê duyệt',
    REJECT: 'Từ chối / Trả lại',
    SUBMIT: 'Gửi duyệt',
    LOCK: 'Khóa tài khoản / Khóa tem',
    UNLOCK: 'Mở khóa',
    LOGIN: 'Đăng nhập hệ thống',
    LOGOUT: 'Đăng xuất',
    ACTIVATE: 'Kích hoạt',
    RECALL: 'Thu hồi lô',
    RECALL_SHIPMENT: 'Thu hồi lô hàng',

    // Events
    RECORD_EVENT: 'Ghi sự kiện chuỗi',
    RECORD_HARVEST_EVENT: 'Ghi sự kiện thu hoạch',
    RECORD_PACKAGING_EVENT: 'Ghi sự kiện đóng gói',
    RECORD_TRANSPORT_EVENT: 'Ghi sự kiện vận chuyển',
    RECORD_PROCUREMENT_EVENT: 'Ghi sự kiện thu mua',
    CORRECT_PACKAGING_EVENT: 'Đính chính đóng gói',

    // Dossier & Export
    EXPORT: 'Xuất hồ sơ nguồn gốc',
    EXPORT_DOSSIER: 'Xuất hồ sơ nguồn gốc',
    GS1_DOSSIER_EXPORT: 'Xuất hồ sơ GS1',

    // Certification
    CREATE_CERTIFICATION: 'Tạo chứng nhận',
    UPDATE_CERTIFICATION: 'Cập nhật chứng nhận',
    DELETE_CERTIFICATION: 'Xóa chứng nhận',
    ATTACH_CERTIFICATION: 'Gắn chứng nhận',

    // Production Lot
    CREATE_PRODUCTION_LOT: 'Tạo lô sản xuất',
    UPDATE_PRODUCTION_LOT: 'Cập nhật lô sản xuất',
    SUBMIT_PRODUCTION_LOT: 'Gửi duyệt lô sản xuất',
    SUBMIT_PRODUCTION_LOT_FOR_APPROVAL: 'Gửi duyệt lô sản xuất',
    APPROVE_PRODUCTION_LOT: 'Phê duyệt lô sản xuất',

    // Farm Log
    CREATE_FARM_LOG: 'Ghi nhật ký canh tác',
    UPDATE_FARM_LOG: 'Cập nhật nhật ký canh tác',
    DELETE_FARM_LOG: 'Xóa nhật ký canh tác',

    // Category
    CREATE_PRODUCT_CATEGORY: 'Tạo loại nông sản',
    UPDATE_PRODUCT_CATEGORY: 'Cập nhật loại nông sản',
    DELETE_PRODUCT_CATEGORY: 'Xóa loại nông sản',

    // Organization & Member
    CREATE_ORGANIZATION: 'Tạo tổ chức',
    UPDATE_ORGANIZATION: 'Cập nhật tổ chức',
    CREATE_INVITATION: 'Tạo thư mời',
    JOIN_ORGANIZATION: 'Tham gia tổ chức',
    ACCESS_DENIED: 'Truy cập trái phép bị chặn',
  };

  const upper = action.toUpperCase();
  if (exactMap[upper]) return exactMap[upper];

  // Tìm theo key chuẩn hóa
  const norm = normalizeKey(action);
  for (const [k, v] of Object.entries(exactMap)) {
    if (normalizeKey(k) === norm) return v;
  }

  return action;
};

export const formatTargetType = (target: string): string => {
  if (!target) return '—';

  const exactMap: Record<string, string> = {
    PRODUCTION_LOT: 'Lô sản xuất',
    PRODUCTIONLOT: 'Lô sản xuất',
    FARM_LOG: 'Nhật ký canh tác',
    FARMLOG: 'Nhật ký canh tác',
    SHIPMENT: 'Lô hàng xuất',
    CHAIN_EVENT: 'Sự kiện chuỗi',
    CHAINEVENT: 'Sự kiện chuỗi',
    PRODUCT_CATEGORY: 'Loại nông sản',
    PRODUCTCATEGORY: 'Loại nông sản',
    CERTIFICATION: 'Chứng nhận chất lượng',
    USER: 'Tài khoản người dùng',
    ORGANIZATION_USER: 'Thành viên tổ chức',
    ORGANIZATIONUSER: 'Thành viên tổ chức',
    ORGANIZATION: 'Tổ chức',
    SYSTEM_MONITORING: 'Giám sát hệ thống',
    ATTACHMENT: 'Chứng từ đính kèm',
    INVITATION: 'Thư mời thành viên',
  };

  const upper = target.toUpperCase();
  if (exactMap[upper]) return exactMap[upper];

  const norm = normalizeKey(target);
  for (const [k, v] of Object.entries(exactMap)) {
    if (normalizeKey(k) === norm) return v;
  }

  return target;
};

export const getActionColor = (action: string): string => {
  if (!action) return 'bg-slate-100 text-slate-700 border-slate-200';
  const act = action.toUpperCase();

  if (
    act.includes('CREATE') ||
    act.includes('APPROVE') ||
    act.includes('JOIN')
  ) {
    return 'bg-emerald-100 text-emerald-800 border-emerald-200';
  }
  if (
    act.includes('UPDATE') ||
    act.includes('EXPORT') ||
    act.includes('RECORD') ||
    act.includes('ATTACH') ||
    act.includes('ACTIVATE')
  ) {
    return 'bg-blue-100 text-blue-800 border-blue-200';
  }
  if (
    act.includes('DELETE') ||
    act.includes('REJECT') ||
    act.includes('DENIED')
  ) {
    return 'bg-rose-100 text-rose-800 border-rose-200';
  }
  if (
    act.includes('RECALL') ||
    act.includes('LOCK') ||
    act.includes('SUBMIT')
  ) {
    return 'bg-amber-100 text-amber-800 border-amber-200';
  }

  return 'bg-slate-100 text-slate-700 border-slate-200';
};
