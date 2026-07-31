import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { LocationScanStats } from '@/types/lookupStatistics';

interface Props {
  data: LocationScanStats[];
}

export const LocationChart = ({ data }: Props) => {
  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm font-medium">Theo vị trí</CardTitle>
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
        <CardTitle className="text-sm font-medium">Theo vị trí</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {data.map((item) => (
            <div key={item.location}>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">{item.location}</span>
                <span className="font-medium">{item.scanCount}</span>
              </div>
              <div className="w-full bg-muted rounded-full h-2 mt-1">
                <div
                  className="bg-emerald-500 h-2 rounded-full transition-all"
                  style={{ width: `${(item.scanCount / max) * 100}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};