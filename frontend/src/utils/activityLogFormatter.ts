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
    RECORD_PREPROCESSING_EVENT: 'Ghi sự kiện sơ chế',
    CORRECT_PREPROCESSING_EVENT: 'Đính chính sơ chế',
    RECORD_PACKAGING_EVENT: 'Ghi sự kiện đóng gói',
    CORRECT_PACKAGING_EVENT: 'Đính chính đóng gói',
    RECORD_TRANSPORT_EVENT: 'Ghi sự kiện vận chuyển',
    RECORD_PROCUREMENT_EVENT: 'Ghi sự kiện thu mua',
    RECORD_WAREHOUSE_RECEIPT: 'Ghi nhận nhập kho',
    RECORD_STORAGE_CONDITION: 'Ghi điều kiện bảo quản',

    // Dossier & Export
    EXPORT: 'Xuất hồ sơ nguồn gốc',
    EXPORT_DOSSIER: 'Xuất hồ sơ nguồn gốc',
    GS1_DOSSIER_EXPORT: 'Xuất hồ sơ GS1',

    // Recall
    CREATE_RECALL_REQUEST: 'Tạo yêu cầu thu hồi',
    APPROVE_RECALL_REQUEST: 'Phê duyệt yêu cầu thu hồi',
    REJECT_RECALL_REQUEST: 'Từ chối yêu cầu thu hồi',

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

    // Input Material
    CREATE_INPUT_MATERIAL: 'Tạo vật tư nông nghiệp',
    UPDATE_INPUT_MATERIAL: 'Cập nhật vật tư nông nghiệp',
    DELETE_INPUT_MATERIAL: 'Xóa vật tư nông nghiệp',

    // Area Assignment
    ASSIGN_AREA: 'Gán địa bàn',
    UNASSIGN_AREA: 'Gỡ địa bàn',

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
    ADD_EXISTING_USER: 'Thêm thành viên',
    CREATE_MEMBER: 'Thêm thành viên',
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
    FARM_LOG_ATTACHMENT: 'Chứng từ nhật ký',
    FARMLOGATTACHMENT: 'Chứng từ nhật ký',
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
    ROLEPERMISSION: 'Phân quyền vai trò',
    PARTNER_API_KEY: 'Khóa API đối tác',
    PARTNERAPIKEY: 'Khóa API đối tác',
    API_KEY: 'Khóa API đối tác',
    APIKEY: 'Khóa API đối tác',
    INSPECTION_REQUEST: 'Yêu cầu kiểm nghiệm',
    INSPECTIONREQUEST: 'Yêu cầu kiểm nghiệm',
    INSPECTION_CRITERION_RESULT: 'Kết quả tiêu chí kiểm nghiệm',
    INSPECTIONCRITERIONRESULT: 'Kết quả tiêu chí kiểm nghiệm',
    INSPECTION_CRITERION: 'Tiêu chí kiểm nghiệm',
    INSPECTIONCRITERION: 'Tiêu chí kiểm nghiệm',
    RECALL_REQUEST: 'Yêu cầu thu hồi',
    RECALLREQUEST: 'Yêu cầu thu hồi',
    RECALL: 'Yêu cầu thu hồi',
    WAREHOUSE_RECEIPT: 'Phiếu nhập kho',
    WAREHOUSERECEIPT: 'Phiếu nhập kho',
    INPUT_MATERIAL: 'Vật tư nông nghiệp',
    INPUTMATERIAL: 'Vật tư nông nghiệp',
    STANDARD: 'Tiêu chuẩn chất lượng',
    CODE_RANGE: 'Dải mã truy xuất',
    CODERANGE: 'Dải mã truy xuất',
    TRACE_CODE: 'Mã tem QR',
    TRACECODE: 'Mã tem QR',
    BACKUP_SCHEDULE: 'Lịch sao lưu',
    BACKUPSCHEDULE: 'Lịch sao lưu',
    BACKUP_RESTORE: 'Sao lưu & phục hồi',
    BACKUPRESTORE: 'Sao lưu & phục hồi',
    ADMINISTRATIVE_UNIT: 'Đơn vị hành chính',
    ADMINISTRATIVEUNIT: 'Đơn vị hành chính',
    USER_AREA_ASSIGNMENT: 'Phân công địa bàn',
    USERAREAASSIGNMENT: 'Phân công địa bàn',
    PRODUCT_FEEDBACK: 'Phản ánh sản phẩm',
    PRODUCTFEEDBACK: 'Phản ánh sản phẩm',
    OFFLINE_SYNC_LOG: 'Đồng bộ ngoại tuyến',
    OFFLINESYNCLOG: 'Đồng bộ ngoại tuyến',
    PASSWORD_RESET_TOKEN: 'Đặt lại mật khẩu',
    PASSWORDRESETTOKEN: 'Đặt lại mật khẩu',
    ACCOUNT_LOCK: 'Khóa tài khoản',
    ACCOUNTLOCK: 'Khóa tài khoản',
    STORAGE_CONDITION: 'Điều kiện bảo quản',
    STORAGECONDITION: 'Điều kiện bảo quản',
    ALERT: 'Cảnh báo hệ thống',
    SYSTEM_MONITORING: 'Giám sát hệ thống',
    SYSTEMMONITORING: 'Giám sát hệ thống',
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
