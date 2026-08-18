export type OverallStatus = 'HEALTHY' | 'WARNING' | 'CRITICAL' | 'INSUFFICIENT_DATA';

export type MetricStatus = 'NORMAL' | 'WARNING' | 'CRITICAL' | 'INSUFFICIENT_DATA';

export interface MetricItem {
  metricCode: string;
  metricName: string;
  value: string;
  numericValue: number | null;
  threshold: string;
  status: MetricStatus;
  unit: string;
  message: string;
}

export interface SystemStatusResponse {
  overallStatus: OverallStatus;
  hasSufficientData: boolean;
  uptimeSeconds: number;
  lastUpdated: string;
  breachedMetricsCount: number;
  summaryMessage: string;
  metrics: {
    dbConnection: MetricItem;
    serverErrorCount: MetricItem;
    publicTraceAvgResponseTime: MetricItem;
    dataGatewayCallCount: MetricItem;
  };
}

export interface ThresholdItem {
  metricCode: string;
  metricName: string;
  thresholdValue: string;
  unit: string;
  description: string;
}