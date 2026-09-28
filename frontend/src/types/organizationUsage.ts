/**
 * Kiểu dữ liệu bảng điều khiển mức độ sử dụng nền tảng theo tổ chức (NCL-07-CN-008).
 * Khớp contract backend GET /api/v1/reports/organization-usage.
 */

export interface MetricComparison {
  /** Giá trị kỳ hiện tại. */
  current: number;
  /** Giá trị kỳ trước. */
  previous: number;
  /** Chênh lệch tuyệt đối (current - previous). */
  change: number;
  /** Chênh lệch tương đối (%), null khi previous == 0. */
  changePercent: number | null;
}

export interface OrganizationUsageItem {
  organizationId: string;
  organizationCode: string;
  organizationName: string;
  organizationType: string | null;
  organizationStatus: string | null;
  createdAt: string;
  /** false = "Chưa có dữ liệu" cho kỳ được chọn. */
  hasData: boolean;
  /** Hoạt động gần nhất, null khi chưa từng có hoạt động. */
  lastActivityAt: string | null;
  /** true = "Cần liên hệ hỗ trợ" (không hoạt động 30 ngày). */
  needsSupport: boolean;
  /** Số lô sản xuất tạo mới. */
  productionLots: MetricComparison;
  /** Số mục nhật ký canh tác. */
  farmLogs: MetricComparison;
  /** Số sự kiện chuỗi. */
  chainEvents: MetricComparison;
  /** Số tem kích hoạt. */
  activatedLabels: MetricComparison;
  /** Số lượt tra cứu công khai. */
  publicLookups: MetricComparison;
  /** Số người dùng hoạt động. */
  activeUsers: MetricComparison;
}

export interface OrganizationUsageDashboard {
  startDate: string;
  endDate: string;
  previousStartDate: string;
  previousEndDate: string;
  totalOrganizations: number;
  items: OrganizationUsageItem[];
}

export interface OrganizationUsageQueryParams {
  startDate?: string;
  endDate?: string;
  organizationId?: string;
}

/** Khóa sắp xếp được trong bảng (gồm mức độ ưu tiên trạng thái hiển thị). */
export type OrganizationUsageSortKey =
  | 'organizationName'
  | 'productionLots'
  | 'farmLogs'
  | 'chainEvents'
  | 'activatedLabels'
  | 'publicLookups'
  | 'activeUsers'
  | 'lastActivityAt'
  /** Mức độ ưu tiên trạng thái: Cần liên hệ hỗ trợ → Đang hoạt động → Chưa có dữ liệu. */
  | 'statusPriority';

export const ORGANIZATION_USAGE_METRICS: Array<{
  key: Exclude<OrganizationUsageSortKey, 'organizationName' | 'lastActivityAt'>;
  label: string;
}> = [
  { key: 'productionLots', label: 'Lô sản xuất' },
  { key: 'farmLogs', label: 'Nhật ký' },
  { key: 'chainEvents', label: 'Sự kiện chuỗi' },
  { key: 'activatedLabels', label: 'Tem kích hoạt' },
  { key: 'publicLookups', label: 'Tra cứu công khai' },
  { key: 'activeUsers', label: 'Người dùng HT' },
];
