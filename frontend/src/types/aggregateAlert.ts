/**
 * Định nghĩa các kiểu dữ liệu cho cảnh báo tổng hợp (NCL-08-CN-016).
 */

export type AggregateAlertType =
  | 'SCAN_ANOMALY'
  | 'CERT_EXPIRING'
  | 'CERT_EXPIRED'
  | 'INSPECTION_EXPIRING'
  | 'INSPECTION_EXPIRED'
  | 'UNPROCESSED_FEEDBACK'
  | 'CODE_RANGE_QUOTA'
  | 'OVERDUE_MILESTONE'
  | 'OPEN_RECALL_CASE';

export type AggregateAlertSeverity = 'HIGH' | 'MEDIUM';

export type AggregateAlertStatus = 'OPEN' | 'RESOLVED';

export interface AggregateAlertItem {
  id: string;
  type: AggregateAlertType;
  typeName: string;
  severity: AggregateAlertSeverity;
  title: string;
  message: string;
  relatedEntityType: string;
  relatedEntityId: string;
  relatedEntityName: string | null;
  createdAt: string;
  actionUrl: string;
  organizationId: string | null;
  organizationName: string;
  status: AggregateAlertStatus;
}

export interface AggregateAlertSummaryCounts {
  totalOpen: number;
  highSeverityCount: number;
  mediumSeverityCount: number;
  byTypeCounts: Record<string, number>;
}

export interface AggregateAlertPageResponse {
  items: AggregateAlertItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  summaryCounts: AggregateAlertSummaryCounts;
}

export interface AggregateAlertCountResponse {
  totalOpen: number;
  highSeverityCount: number;
  mediumSeverityCount: number;
  byTypeCounts: Record<string, number>;
}

export interface UnviewedAlertCountResponse {
  unviewedCount: number;
  hasHighSeverity: boolean;
}

export interface AggregateAlertFilterParams {
  type?: string;
  severity?: string;
  status?: string;
  organizationId?: string;
  keyword?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}
