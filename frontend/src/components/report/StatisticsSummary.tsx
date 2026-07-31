import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Eye, QrCode, AlertTriangle } from 'lucide-react';
import type { SummaryStats } from '@/types/lookupStatistics';

interface Props {
  stats: SummaryStats;
}

export const StatisticsSummary = ({ stats }: Props) => {
  const items = [
    {
      label: 'Tổng lượt quét',
      value: stats.totalScans.toLocaleString('vi-VN'),
      icon: Eye,
      color: 'text-blue-600',
      bg: 'bg-blue-50',
    },
    {
      label: 'Mã quét duy nhất',
      value: stats.totalUniqueCodes.toLocaleString('vi-VN'),
      icon: QrCode,
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
    {
      label: 'Quét bất thường',
      value: stats.abnormalScansCount.toLocaleString('vi-VN'),
      icon: AlertTriangle,
      color: 'text-amber-600',
      bg: 'bg-amber-50',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      {items.map((item) => (
        <Card key={item.label}>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {item.label}
            </CardTitle>
            <div className={`p-2 rounded-full ${item.bg}`}>
              <item.icon className={`h-4 w-4 ${item.color}`} />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{item.value}</div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};