import { Card, CardContent } from '@/components/ui/card';
import { Building2, Package, Weight } from 'lucide-react';

interface Props {
  totalOrganizations: number;
  totalShipments: number;
  totalQuantity: number;
}

export const IndustryReportSummary = ({ totalOrganizations, totalShipments, totalQuantity }: Props) => {
  const items = [
    { label: 'Tổng số tổ chức', value: totalOrganizations, icon: Building2, color: 'text-blue-600' },
    { label: 'Tổng số lô hàng', value: totalShipments, icon: Package, color: 'text-emerald-600' },
    { label: 'Tổng sản lượng (kg)', value: totalQuantity.toLocaleString(), icon: Weight, color: 'text-amber-600' },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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