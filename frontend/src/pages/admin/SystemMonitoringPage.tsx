import { useCallback, useEffect, useMemo, useState } from 'react';
import { getSystemStatus } from '@/api/monitoringApi';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type {
  MetricItem,
  MetricStatus,
  OverallStatus,
  SystemStatusResponse,
} from '@/types/monitoring';
import {
  Activity,
  AlertOctagon,
  CheckCircle2,
  Clock,
  Gauge,
  HeartPulse,
  RefreshCw,
  Server,
  ShieldCheck,
  Timer,
  TriangleAlert,
} from 'lucide-react';
import { toast } from 'sonner';
import { HelpButton } from '@/components/help/HelpButton';

const POLL_INTERVAL_MS = 20_000;

const METRIC_LABELS: Record<MetricStatus, string> = {
  NORMAL: 'Bình thường',
  WARNING: 'Cảnh báo',
  CRITICAL: 'Nguy hiểm',
  INSUFFICIENT_DATA: 'Chưa đủ dữ liệu',
};

const METRIC_BADGE_VARIANTS: Record<MetricStatus, 'success' | 'warning' | 'destructive' | 'secondary'> = {
  NORMAL: 'success',
  WARNING: 'warning',
  CRITICAL: 'destructive',
  INSUFFICIENT_DATA: 'secondary',
};

const OVERALL_CONFIG: Record<
  OverallStatus,
  { label: string; bannerClass: string; icon: React.ReactNode }
> = {
  HEALTHY: {
    label: 'HỆ THỐNG KHỎE MẠNH',
    bannerClass: 'border-emerald-600/30 bg-gradient-to-r from-emerald-600 to-emerald-500 text-white',
    icon: <ShieldCheck className="h-8 w-8" />,
  },
  WARNING: {
    label: 'CẢNH BÁO',
    bannerClass: 'border-amber-600/30 bg-gradient-to-r from-amber-600 to-amber-500 text-white',
    icon: <TriangleAlert className="h-8 w-8" />,
  },
  CRITICAL: {
    label: 'NGUY HIỂM',
    bannerClass: 'border-red-700/30 bg-gradient-to-r from-red-700 to-red-600 text-white',
    icon: <AlertOctagon className="h-8 w-8" />,
  },
  INSUFFICIENT_DATA: {
    label: 'CHƯA ĐỦ DỮ LIỆU',
    bannerClass: 'border-slate-500/30 bg-gradient-to-r from-slate-600 to-slate-500 text-white',
    icon: <Clock className="h-8 w-8" />,
  },
};

const formatUptime = (seconds: number) => {
  if (seconds < 60) return `${seconds} giây`;
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours === 0) return `${minutes} phút`;
  return `${hours} giờ ${minutes} phút`;
};

const formatUpdatedAt = (value: string) => {
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};

const metricIcons: Record<string, React.ReactNode> = {
  DB_CONNECTION: <Server className="h-5 w-5" />,
  SERVER_ERRORS: <AlertOctagon className="h-5 w-5" />,
  PUBLIC_TRACE_LATENCY: <Timer className="h-5 w-5" />,
  DATA_GATEWAY_CALLS: <Gauge className="h-5 w-5" />,
};

function MetricCard({ metric }: { metric: MetricItem }) {
  const formatMetricValue = (val: string | number) => {
    if (val === 'UP') return 'Hoạt động (UP)';
    if (val === 'DOWN') return 'Ngừng hoạt động (DOWN)';
    return val;
  };

  const formatThreshold = (threshold: string | number, unit: string) => {
    if (threshold === 'UP' && unit === 'STATUS') return 'Hoạt động (UP)';
    if (threshold === 'DOWN' && unit === 'STATUS') return 'Ngừng hoạt động (DOWN)';
    if (unit === 'STATUS') return String(threshold);
    return `${threshold} ${unit}`;
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="flex items-center gap-2 text-sm font-medium">
          <span className="text-muted-foreground">{metricIcons[metric.metricCode]}</span>
          {metric.metricName}
        </CardTitle>
        <Badge variant={METRIC_BADGE_VARIANTS[metric.status]}>
          {METRIC_LABELS[metric.status]}
        </Badge>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-bold">
          {formatMetricValue(metric.value)}
          {metric.unit !== 'STATUS' && metric.unit ? (
            <span className="ml-1 text-sm font-normal text-muted-foreground">
              {metric.unit}
            </span>
          ) : null}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          Ngưỡng: {formatThreshold(metric.threshold, metric.unit)}
        </p>
        <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
          {metric.message}
        </p>
      </CardContent>
    </Card>
  );
}

export function SystemMonitoringPage() {
  const [status, setStatus] = useState<SystemStatusResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchStatus = useCallback(async () => {
    try {
      const data = await getSystemStatus();
      setStatus(data);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải thông tin giám sát hệ thống');
    }
  }, []);

  const handleRefresh = useCallback(async () => {
    setLoading(true);
    await fetchStatus();
    setLoading(false);
  }, [fetchStatus]);

  useEffect(() => {
    void fetchStatus();
    const intervalId = window.setInterval(() => {
      void fetchStatus();
    }, POLL_INTERVAL_MS);
    return () => window.clearInterval(intervalId);
  }, [fetchStatus]);

  const overall = useMemo(() => {
    if (!status) return null;
    return OVERALL_CONFIG[status.overallStatus];
  }, [status]);

  const metricList = useMemo(() => {
    if (!status) return [];
    return Object.values(status.metrics);
  }, [status]);

  if (!status) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-emerald-100 p-2.5 text-emerald-700">
            <HeartPulse className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Giám sát hệ thống</h1>
            <p className="text-sm text-muted-foreground">
              Tổng quan sức khỏe hệ thống trước buổi trình diễn
            </p>
          </div>
        </div>
        <Card>
          <CardContent className="flex items-center justify-center gap-3 p-10 text-muted-foreground">
            <RefreshCw className="h-5 w-5 animate-spin" />
            Đang tải thông tin giám sát...
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-emerald-100 p-2.5 text-emerald-700">
            <HeartPulse className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Giám sát hệ thống</h1>
            <p className="text-sm text-muted-foreground">
              Tổng quan sức khỏe hệ thống trước buổi trình diễn
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="admin-system-monitoring" />
          <Button variant="outline" size="sm" onClick={handleRefresh} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <Card className={overall ? overall.bannerClass : ''}>
        <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="rounded-2xl bg-white/15 p-3">{overall?.icon}</div>
            <div>
              <p className="text-sm font-medium opacity-90">Trạng thái tổng thể</p>
              <p className="text-2xl font-bold">{overall?.label}</p>
              <p className="mt-1 text-sm opacity-90">{status.summaryMessage}</p>
            </div>
          </div>
          <div className="flex flex-col gap-1 text-sm sm:items-end">
            <p className="flex items-center gap-2">
              <Clock className="h-4 w-4" />
              Uptime: {formatUptime(status.uptimeSeconds)}
            </p>
            <p>
              Chỉ số vượt ngưỡng: {status.breachedMetricsCount}
            </p>
            <p className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4" />
              Cập nhật: {formatUpdatedAt(status.lastUpdated)}
            </p>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {metricList.map((metric) => (
          <MetricCard key={metric.metricCode} metric={metric} />
        ))}
      </div>

      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Activity className="h-4 w-4" />
        Tự động làm mới mỗi {POLL_INTERVAL_MS / 1000} giây khi mở trang.
      </div>
    </div>
  );
}