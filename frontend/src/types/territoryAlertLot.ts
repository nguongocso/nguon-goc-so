/**
 * Danh mục 6 loại cảnh báo chính thức cho lô sản xuất (NCL-07-CN-006).
 */
export type LotAlertType =
  | 'RECALLING'
  | 'LOCKED_LABEL'
  | 'INSPECTION_FAILED'
  | 'QUARANTINE_OVERWRITTEN'
  | 'SERIOUS_FEEDBACK_OPEN'
  | 'INSPECTION_EXPIRED';

/**
 * Cấu hình hiển thị nhãn tiếng Việt và màu sắc cho từng loại cảnh báo.
 */
export interface AlertTypeConfig {
  label: string;
  badgeVariant: 'destructive' | 'warning' | 'default' | 'secondary' | 'outline';
  badgeClass: string;
  description: string;
}

export const ALERT_TYPE_CONFIG_MAP: Record<LotAlertType, AlertTypeConfig> = {
  RECALLING: {
    label: 'Đang thu hồi',
    badgeVariant: 'destructive',
    badgeClass: 'bg-red-100 text-red-800 border-red-200 dark:bg-red-950/40 dark:text-red-300 dark:border-red-900',
    description: 'Lô đang hoặc đã trong diện thu hồi sản phẩm.',
  },
  LOCKED_LABEL: {
    label: 'Tem bị khóa',
    badgeVariant: 'destructive',
    badgeClass: 'bg-rose-100 text-rose-800 border-rose-200 dark:bg-rose-950/40 dark:text-rose-300 dark:border-rose-900',
    description: 'Có ít nhất một mã tem bị khóa do nghi vấn giả mạo hoặc quét bất thường.',
  },
  INSPECTION_FAILED: {
    label: 'Kết quả kiểm nghiệm không đạt',
    badgeVariant: 'destructive',
    badgeClass: 'bg-amber-100 text-amber-800 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-900',
    description: 'Lô có kết quả kiểm nghiệm không đạt tiêu chuẩn an toàn / chất lượng.',
  },
  QUARANTINE_OVERWRITTEN: {
    label: 'Ghi đè thời gian cách ly',
    badgeVariant: 'warning',
    badgeClass: 'bg-orange-100 text-orange-800 border-orange-200 dark:bg-orange-950/40 dark:text-orange-300 dark:border-orange-900',
    description: 'Thu hoạch ghi đè trước khi hết thời gian cách ly thuốc BVTV / phân bón.',
  },
  SERIOUS_FEEDBACK_OPEN: {
    label: 'Phản ánh mức độ nghiêm trọng',
    badgeVariant: 'warning',
    badgeClass: 'bg-purple-100 text-purple-800 border-purple-200 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-900',
    description: 'Có phản ánh người tiêu dùng ở mức nghiêm trọng chưa được xử lý đóng.',
  },
  INSPECTION_EXPIRED: {
    label: 'Kiểm nghiệm hết hiệu lực',
    badgeVariant: 'warning',
    badgeClass: 'bg-yellow-100 text-yellow-800 border-yellow-200 dark:bg-yellow-950/40 dark:text-yellow-300 dark:border-yellow-900',
    description: 'Kết quả kiểm nghiệm của lô sản xuất đã quá hạn hiệu lực.',
  },
};

/**
 * Lấy nhãn tiếng Việt của loại cảnh báo.
 */
export function getLotAlertLabel(type: LotAlertType | string): string {
  if (type in ALERT_TYPE_CONFIG_MAP) {
    return ALERT_TYPE_CONFIG_MAP[type as LotAlertType].label;
  }
  return type || 'Cảnh báo không xác định';
}

/**
 * Tóm tắt ngắn gọn từng cảnh báo hiển thị dạng huy hiệu (badge).
 */
export interface AlertBadgeSummary {
  alertType: LotAlertType;
  alertName: string;
  severity: string;
  triggeredAt: string;
  briefNote?: string;
}

/**
 * DTO một dòng lô trong danh sách lô có cảnh báo.
 */
export interface AlertLotSummaryResponse {
  lotId: string;
  lotCode: string;
  lotName: string;
  organizationId: string;
  organizationName: string;
  productCategoryId?: string;
  productCategoryName?: string;
  farmAreaName?: string;
  communeName?: string;
  provinceName?: string;
  lotStatus: string;
  alertTypes: LotAlertType[];
  primaryAlertType: LotAlertType;
  alertCount: number;
  latestAlertTriggeredAt: string;
  alertSummaries: AlertBadgeSummary[];
  createdAt: string;
}

/**
 * Thông tin chi tiết lô sản xuất (Read-only).
 */
export interface AlertLotInfoItem {
  lotId: string;
  lotCode: string;
  lotName: string;
  status: string;
  expectedQuantity?: number;
  actualQuantity?: number;
  quantityUnit?: string;
  plantingDate?: string;
  harvestDate?: string;
  productCategoryName?: string;
  farmAreaName?: string;
  farmAreaAddress?: string;
  createdAt: string;
}

/**
 * Thông tin tổ chức sở hữu lô.
 */
export interface AlertLotOrgItem {
  organizationId: string;
  organizationName: string;
  taxCode?: string;
  address?: string;
  communeName?: string;
  provinceName?: string;
  representativeName?: string;
  contactPhone?: string;
}

/**
 * Chi tiết bằng chứng cảnh báo của một lô sản xuất.
 */
export interface LotAlertEvidenceDetail {
  alertType: LotAlertType;
  severity: string;
  triggeredAt: string;
  title: string;
  message: string;
  evidenceData?: Record<string, unknown>;
}

/**
 * Mục dòng sự kiện trong chi tiết lô cảnh báo (Read-only).
 */
export interface ReadonlyChainEventItem {
  eventId: string;
  eventType: string;
  eventTypeName?: string;
  recordedAt: string;
  recordedByName?: string;
  location?: string;
  earlyHarvest?: boolean;
  hasAlert?: boolean;
  alertWarning?: string;
  shipmentName?: string;
  description?: string;
}

/**
 * DTO chi tiết một lô có cảnh báo cho Cán bộ quản lý ngành (Read-only).
 */
export interface AlertLotDetailResponse {
  lotInfo: AlertLotInfoItem;
  organization: AlertLotOrgItem;
  activeAlerts: LotAlertEvidenceDetail[];
  timelineEvents: ReadonlyChainEventItem[];
}

/**
 * Tham số truy vấn danh sách lô có cảnh báo.
 */
export interface AlertLotQueryParams {
  alertType?: LotAlertType | '';
  organizationId?: string;
  fromDate?: string;
  toDate?: string;
  unitIds?: string[];
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}
