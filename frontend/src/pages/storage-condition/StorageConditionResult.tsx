import { AlertTriangle, CheckCircle2 } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

import { cn } from '@/lib/utils';
import type { StorageConditionResponse } from '@/types/storageCondition';

interface StorageConditionResultProps {
  result: StorageConditionResponse;
}

const alertLabels: Record<string, string> = {
  CRITICAL: 'Nghiêm trọng',
  WARNING: 'Cảnh báo',
  OK: 'Bình thường',
};

const alertClasses: Record<string, string> = {
  CRITICAL: 'border-red-300 bg-red-50 text-red-700 gap-1',
  WARNING: 'border-yellow-300 bg-yellow-50 text-yellow-700 gap-1',
  OK: 'border-green-300 bg-green-50 text-green-700 gap-1',
};

/** Hiển thị kết quả và cảnh báo sau khi ghi nhận. */
export function StorageConditionResult({ result }: StorageConditionResultProps) {
  const temperatureExceeded = result.isTemperatureExceeded;
  const humidityExceeded = result.isHumidityExceeded;

  return (
    <Card className={result.alertLevel !== 'OK' ? 'border-red-200' : 'border-emerald-200'}>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          {result.alertLevel !== 'OK'
            ? <AlertTriangle className="size-5 text-red-600" />
            : <CheckCircle2 className="size-5 text-emerald-600" />}
          Kết quả ghi nhận
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Metric label="Nhiệt độ" value={`${result.temperature}°C`} />
          <Metric label="Độ ẩm" value={`${result.humidity}%`} />
          <StatusMetric label="Nhiệt độ" exceeded={temperatureExceeded} />
          <StatusMetric label="Độ ẩm" exceeded={humidityExceeded} />
        </div>

        <div className="flex items-center gap-2">
          <Badge
            variant="outline"
            className={alertClasses[result.alertLevel] ?? 'gap-1'}
          >
            {result.alertLevel === 'OK'
              ? <CheckCircle2 className="size-4" />
              : <AlertTriangle className="size-4" />}
            {alertLabels[result.alertLevel] ?? result.alertLevel}
          </Badge>
        </div>

        {result.thresholds && (
          <div className="rounded-lg bg-gray-50 p-3">
            <p className="mb-2 text-xs font-medium text-gray-600">Ngưỡng bảo quản</p>
            <div className="grid grid-cols-2 gap-2 text-xs text-gray-600">
              <span>
                Nhiệt độ: {result.thresholds.tempMin}°C – {result.thresholds.tempMax}°C
              </span>
              <span>
                Độ ẩm: {result.thresholds.humidityMin}% – {result.thresholds.humidityMax}%
              </span>
            </div>
          </div>
        )}

        <div className="text-xs text-muted-foreground">
          <p>Người ghi: {result.recordedBy}</p>
          <p>Thời gian: {new Date(result.recordedAt).toLocaleString('vi-VN')}</p>
        </div>
      </CardContent>
    </Card>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-blue-50 p-3">
      <p className="text-xs text-blue-700">{label}</p>
      <p className="mt-1 text-lg font-bold text-blue-900">{value}</p>
    </div>
  );
}

function StatusMetric({ label, exceeded }: { label: string; exceeded: boolean }) {
  return (
    <div className={cn('rounded-lg p-3', exceeded ? 'bg-red-50' : 'bg-emerald-50')}>
      <p className={cn('text-xs', exceeded ? 'text-red-700' : 'text-emerald-700')}>
        {label}
      </p>
      <p className={cn('mt-1 text-sm font-bold', exceeded ? 'text-red-900' : 'text-emerald-900')}>
        {exceeded ? 'Vượt ngưỡng' : 'Đạt'}
      </p>
    </div>
  );
}
