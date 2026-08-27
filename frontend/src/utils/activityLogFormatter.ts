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

    // Farm Area
    CREATE_FARM_AREA: 'Tạo vùng trồng',
    UPDATE_FARM_AREA: 'Cập nhật vùng trồng',
    DELETE_FARM_AREA: 'Xóa vùng trồng',

    // Category
    CREATE_PRODUCT_CATEGORY: 'Tạo loại nông sản',
    UPDATE_PRODUCT_CATEGORY: 'Cập nhật loại nông sản',
    DELETE_PRODUCT_CATEGORY: 'Xóa loại nông sản',

    // Organization & Member & Permissions
    CREATE_ORGANIZATION: 'Tạo tổ chức',
    UPDATE_ORGANIZATION: 'Cập nhật tổ chức',
    UPDATE_ORGANIZATION_PROFILE: 'Cập nhật hồ sơ tổ chức',
    CREATE_INVITATION: 'Tạo thư mời',
    JOIN_ORGANIZATION: 'Tham gia tổ chức',
    UPDATE_ROLE_PERMISSIONS: 'Cấu hình quyền vai trò',
    ACCESS_DENIED: 'Truy cập trái phép bị chặn',

    // API Key
    CREATE_API_KEY: 'Cấp API key đối tác',
    REVOKE_API_KEY: 'Thu hồi API key',

    // Inspection
    CREATE_INSPECTION_REQUEST: 'Tạo yêu cầu kiểm nghiệm',
    RECORD_INSPECTION_RESULT: 'Ghi kết quả kiểm nghiệm',
    UPDATE_INSPECTION_RESULT: 'Cập nhật kết quả kiểm nghiệm',
    RECORD_INSPECTION_RESULTS: 'Ghi bulk kết quả',
    DELETE_INSPECTION_RESULT: 'Xóa kết quả kiểm nghiệm',
    UPLOAD_INSPECTION_RESULT_FILE: 'Tải phiếu kết quả',

    // Shipment
    DELETE_SHIPMENT_DRAFT: 'Hủy bản nháp lô hàng',

    // Alert
    RESOLVE_ALERT: 'Xử lý cảnh báo',
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
    FARM_AREA: 'Vùng trồng',
    FARMAREA: 'Vùng trồng',
    SHIPMENT: 'Lô hàng',
    CHAIN_EVENT: 'Sự kiện chuỗi',
    CHAINEVENT: 'Sự kiện chuỗi',
    PRODUCT_CATEGORY: 'Loại nông sản',
    PRODUCTCATEGORY: 'Loại nông sản',
    CERTIFICATION: 'Chứng nhận chất lượng',
    USER: 'Tài khoản người dùng',
    ORGANIZATION_USER: 'Thành viên tổ chức',
    ORGANIZATIONUSER: 'Thành viên tổ chức',
    ORGANIZATION: 'Tổ chức',
    ORGANIZATION_ROLE_PERMISSION: 'Phân quyền vai trò',
    ORGANIZATIONROLEPERMISSION: 'Phân quyền vai trò',
    ROLE_PERMISSION: 'Phân quyền vai trò',
    PARTNER_API_KEY: 'Khóa API đối tác',
    PARTNERAPIKEY: 'Khóa API đối tác',
    API_KEY: 'Khóa API đối tác',
    INSPECTION_REQUEST: 'Yêu cầu kiểm nghiệm',
    INSPECTIONREQUEST: 'Yêu cầu kiểm nghiệm',
    INSPECTION_CRITERION_RESULT: 'Kết quả tiêu chí kiểm nghiệm',
    INSPECTIONCRITERIONRESULT: 'Kết quả tiêu chí kiểm nghiệm',
    INSPECTION_CRITERION: 'Tiêu chí kiểm nghiệm',
    INSPECTIONCRITERION: 'Tiêu chí kiểm nghiệm',
    ALERT: 'Cảnh báo hệ thống',
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
    act.includes('JOIN') ||
    act.includes('UPLOAD')
  ) {
    return 'bg-emerald-100 text-emerald-800 border-emerald-200';
  }
  if (
    act.includes('UPDATE') ||
    act.includes('EXPORT') ||
    act.includes('RECORD') ||
    act.includes('ATTACH') ||
    act.includes('ACTIVATE') ||
    act.includes('RESOLVE')
  ) {
    return 'bg-blue-100 text-blue-800 border-blue-200';
  }
  if (
    act.includes('DELETE') ||
    act.includes('REJECT') ||
    act.includes('DENIED') ||
    act.includes('REVOKE')
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
