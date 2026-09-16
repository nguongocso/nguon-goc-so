import React from 'react';
import { ShieldAlert, AlertTriangle, Clock, Layers } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import type { AggregateAlertSummaryCounts } from '@/types/aggregateAlert';

interface AggregateAlertSummaryCardsProps {
  summaryCounts?: AggregateAlertSummaryCounts | null;
  selectedSeverity?: string;
  onSelectSeverity?: (severity: string) => void;
}

const TYPE_NAME_MAP: Record<string, string> = {
  SCAN_ANOMALY: 'Tem quét bất thường',
  CERT_EXPIRING: 'Chứng nhận sắp hết hạn',
  CERT_EXPIRED: 'Chứng nhận đã hết hạn',
  INSPECTION_EXPIRING: 'Kiểm nghiệm sắp hết hạn',
  INSPECTION_EXPIRED: 'Kiểm nghiệm đã hết hạn',
  UNPROCESSED_FEEDBACK: 'Phản ánh chưa xử lý',
  CODE_RANGE_QUOTA: 'Hạn mức dải mã',
  OVERDUE_MILESTONE: 'Mốc canh tác quá hạn',
  OPEN_RECALL_CASE: 'Vụ việc thu hồi',
};

export const AggregateAlertSummaryCards: React.FC<AggregateAlertSummaryCardsProps> = ({
  summaryCounts,
  selectedSeverity = '',
  onSelectSeverity,
}) => {
  const totalOpen = summaryCounts?.totalOpen ?? 0;
  const highCount = summaryCounts?.highSeverityCount ?? 0;
  const mediumCount = summaryCounts?.mediumSeverityCount ?? 0;

  // Tìm nguồn cảnh báo có số lượng nhiều nhất
  let topTypeName = 'Không có';
  let topTypeCount = 0;
  if (summaryCounts?.byTypeCounts) {
    for (const [key, count] of Object.entries(summaryCounts.byTypeCounts)) {
      if (count > topTypeCount) {
        topTypeCount = count;
        topTypeName = TYPE_NAME_MAP[key] || key;
      }
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {/* Card 1: Tổng cảnh báo đang mở */}
      <Card
        onClick={() => onSelectSeverity && onSelectSeverity('')}
        className={`cursor-pointer transition-all duration-200 hover:shadow-md ${
          selectedSeverity === '' ? 'ring-2 ring-emerald-500 bg-emerald-50/20' : ''
        }`}
      >
        <CardContent className="p-5">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Tổng cảnh báo mở
              </p>
              <h3 className="mt-1 text-2xl font-bold text-gray-900 tracking-tight">
                {totalOpen}
              </h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Gom từ 7 nguồn hệ thống
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
              <ShieldAlert className="h-6 w-6" />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Card 2: Cảnh báo mức cao / khẩn cấp */}
      <Card
        onClick={() => onSelectSeverity && onSelectSeverity(selectedSeverity === 'HIGH' ? '' : 'HIGH')}
        className={`cursor-pointer transition-all duration-200 hover:shadow-md ${
          selectedSeverity === 'HIGH' ? 'ring-2 ring-red-500 bg-red-50/30' : ''
        }`}
      >
        <CardContent className="p-5">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <p className="text-xs font-medium text-red-700 uppercase tracking-wider">
                  Mức cao / Khẩn cấp
                </p>
                {highCount > 0 && (
                  <span className="inline-flex h-2 w-2 rounded-full bg-red-600 animate-pulse" />
                )}
              </div>
              <h3 className="mt-1 text-2xl font-bold text-red-600 tracking-tight">
                {highCount}
              </h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Cần ưu tiên xử lý ngay
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-red-50 text-red-600">
              <AlertTriangle className="h-6 w-6" />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Card 3: Cảnh báo mức trung bình */}
      <Card
        onClick={() => onSelectSeverity && onSelectSeverity(selectedSeverity === 'MEDIUM' ? '' : 'MEDIUM')}
        className={`cursor-pointer transition-all duration-200 hover:shadow-md ${
          selectedSeverity === 'MEDIUM' ? 'ring-2 ring-amber-500 bg-amber-50/30' : ''
        }`}
      >
        <CardContent className="p-5">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <p className="text-xs font-medium text-amber-700 uppercase tracking-wider">
                Mức trung bình
              </p>
              <h3 className="mt-1 text-2xl font-bold text-amber-600 tracking-tight">
                {mediumCount}
              </h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Cần theo dõi kế hoạch
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
              <Clock className="h-6 w-6" />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Card 4: Nguồn chiếm nhiều nhất */}
      <Card className="transition-all duration-200">
        <CardContent className="p-5">
          <div className="flex items-center justify-between">
            <div className="min-w-0">
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Nhiều việc nhất
              </p>
              <h3 className="mt-1 text-base font-bold text-purple-700 truncate tracking-tight" title={topTypeName}>
                {topTypeName}
              </h3>
              <p className="mt-1 text-xs text-muted-foreground">
                {topTypeCount > 0 ? `${topTypeCount} cảnh báo đang mở` : 'Không có việc tồn'}
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-50 text-purple-600">
              <Layers className="h-6 w-6" />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
