import { Card, CardContent } from '@/components/ui/card';
import { PackageOpen, Sprout, Wheat, MapPin } from 'lucide-react';
import type { SummaryStats } from '@/types/cropAreaAnalysis';

interface Props {
  summary: SummaryStats;
}

export const CropAreaSummaryCards = ({ summary }: Props) => {
  const items = [
    { label: 'Tổng số lô', value: summary.totalLots, icon: PackageOpen, color: 'text-blue-600' },
    { label: 'Sản lượng dự kiến', value: summary.totalExpectedYield.toLocaleString() + ' kg', icon: Sprout, color: 'text-emerald-600' },
    { label: 'Sản lượng thực tế', value: summary.totalActualYield.toLocaleString() + ' kg', icon: Wheat, color: 'text-amber-600' },
    { label: 'Tổng diện tích', value: summary.totalArea.toLocaleString() + ' ha', icon: MapPin, color: 'text-purple-600' },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {items.map((item) => (
        <Card key={item.label}>
          <CardContent className="flex items-center justify-between p-5">
            <div>
              <p className="text-sm text-muted-foreground">{item.label}</p>
              <p className="text-2xl font-bold">{item.value}</p>
            </div>
            <div className={`p-3 rounded-full bg-muted ${item.color}`}>
              <item.icon className="h-6 w-6" />
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};