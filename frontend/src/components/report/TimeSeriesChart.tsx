import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { TimeSeriesData } from '@/types/lookupStatistics';

interface Props {
  data: TimeSeriesData[];
}

export const TimeSeriesChart = ({ data }: Props) => {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Theo thời gian</CardTitle>
        </CardHeader>
        <CardContent className="text-center text-muted-foreground py-8">
          Chưa có dữ liệu quét
        </CardContent>
      </Card>
    );
  }

  const max = Math.max(...data.map((d) => d.scanCount));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium">Theo thời gian</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex items-end gap-2 h-48">
          {data.map((item) => {
            const height = max > 0 ? (item.scanCount / max) * 100 : 0;
            return (
              <div key={item.period} className="flex-1 flex flex-col items-center">
                <div
                  className="w-full bg-emerald-500 rounded-t hover:bg-emerald-600 transition-all"
                  style={{ height: `${Math.max(height, 4)}%`, minHeight: '4px' }}
                />
                <span className="text-xs text-muted-foreground mt-1 truncate w-full text-center">
                  {item.period}
                </span>
                <span className="text-xs font-medium">{item.scanCount}</span>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
};