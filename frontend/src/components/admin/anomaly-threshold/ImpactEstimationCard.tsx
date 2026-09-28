import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { AlertCircle, Activity, MapPin, Clock, Calendar, CheckCircle2, ShieldAlert } from 'lucide-react';
import type { ImpactEstimationResult } from '@/types/anomalyThreshold';

interface ImpactEstimationCardProps {
  result: ImpactEstimationResult | null;
  loading?: boolean;
  categoryName?: string | null;
}

export const ImpactEstimationCard: React.FC<ImpactEstimationCardProps> = ({
  result,
  loading = false,
  categoryName,
}) => {
  if (loading) {
    return (
      <Card className="border-amber-200 bg-amber-50/40">
        <CardContent className="py-6 flex flex-col items-center justify-center space-y-2">
          <Activity className="h-6 w-6 text-amber-600 animate-spin" />
          <p className="text-sm text-amber-700 font-medium">
            Đang phân tích dữ liệu quét 30 ngày gần nhất...
          </p>
        </CardContent>
      </Card>
    );
  }

  if (!result) return null;

  const hasAnomalies = result.estimatedAnomaliesCount > 0;

  return (
    <Card className="border-emerald-200 shadow-sm">
      <CardHeader className="pb-3 border-b">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-emerald-600" />
            <CardTitle className="text-base font-semibold">
              Ước lượng tác động (Mô phỏng 30 ngày)
            </CardTitle>
          </div>
          <div className="flex items-center gap-2">
            {categoryName && (
              <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-300">
                Loại: {categoryName}
              </Badge>
            )}
            <Badge variant="secondary">Chế độ Dry-run</Badge>
          </div>
        </div>
        <CardDescription className="text-xs text-muted-foreground mt-1">
          Dự kiến số lượng mã tem sẽ bị gắn cờ nếu áp dụng ngưỡng dự thảo. Không thay đổi dữ liệu thực tế.
        </CardDescription>
      </CardHeader>

      <CardContent className="pt-4 space-y-4">
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <div className="rounded-lg border p-3 bg-muted/30">
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Activity className="h-3.5 w-3.5 text-blue-500" />
              <span>Tổng lượt quét</span>
            </div>
            <p className="text-lg font-bold mt-1">{result.totalScansAnalyzed.toLocaleString('vi-VN')}</p>
          </div>

          <div className="rounded-lg border p-3 bg-muted/30">
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
              <span>Tổng mã tem</span>
            </div>
            <p className="text-lg font-bold mt-1">{result.totalTraceCodesAnalyzed.toLocaleString('vi-VN')}</p>
          </div>

          <div className={`rounded-lg border p-3 ${hasAnomalies ? 'bg-amber-50 border-amber-300' : 'bg-emerald-50 border-emerald-300'}`}>
            <div className="flex items-center gap-1 text-xs font-medium">
              <AlertCircle className={`h-3.5 w-3.5 ${hasAnomalies ? 'text-amber-600' : 'text-emerald-600'}`} />
              <span className={hasAnomalies ? 'text-amber-800' : 'text-emerald-800'}>Dự kiến bất thường</span>
            </div>
            <p className={`text-xl font-black mt-1 ${hasAnomalies ? 'text-amber-700' : 'text-emerald-700'}`}>
              {result.estimatedAnomaliesCount.toLocaleString('vi-VN')}
            </p>
          </div>

          <div className="rounded-lg border p-3 bg-muted/30">
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="h-3.5 w-3.5 text-orange-500" />
              <span>Tần suất cao</span>
            </div>
            <p className="text-lg font-bold mt-1">{result.highFrequencyCount.toLocaleString('vi-VN')}</p>
          </div>

          <div className="rounded-lg border p-3 bg-muted/30">
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <MapPin className="h-3.5 w-3.5 text-rose-500" />
              <span>Di chuyển phi lý</span>
            </div>
            <p className="text-lg font-bold mt-1">{result.impossibleTravelCount.toLocaleString('vi-VN')}</p>
          </div>

          <div className="rounded-lg border p-3 bg-muted/30">
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <Calendar className="h-3.5 w-3.5 text-purple-500" />
              <span>Quá hạn kích hoạt</span>
            </div>
            <p className="text-lg font-bold mt-1">{result.activationAgeCount.toLocaleString('vi-VN')}</p>
          </div>
        </div>

        <div className="flex items-start gap-2 p-3 rounded-md bg-muted/50 text-xs text-muted-foreground border">
          <AlertCircle className="h-4 w-4 text-emerald-600 flex-shrink-0 mt-0.5" />
          <p>{result.message}</p>
        </div>
      </CardContent>
    </Card>
  );
};
