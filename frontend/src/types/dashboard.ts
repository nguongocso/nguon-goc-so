export interface DashboardSummary {
  totalLots: number;
  totalExpectedYield: number;
  totalActualYield: number;
}

export interface DashboardStatusCount {
  DRAFT: number;
  PENDING: number;
  APPROVED: number;
  REJECTED: number;
  HARVESTED: number;
  PACKAGED: number;
  CLOSED: number;
}

export interface DashboardTimeSeriesItem {
  period: string;
  lotCount: number;
  expectedYield: number;
  actualYield: number;
}

export interface DashboardData {
  summary: DashboardSummary;
  byStatus: DashboardStatusCount;
  timeSeries: DashboardTimeSeriesItem[];
}