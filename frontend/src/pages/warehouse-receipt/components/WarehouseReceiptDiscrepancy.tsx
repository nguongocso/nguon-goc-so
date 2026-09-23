import { AlertTriangle, CheckCircle2 } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';

interface DiscrepancyInfo {
  difference: number;
  percent: number;
  isExceeded: boolean;
}

interface WarehouseReceiptDiscrepancyProps {
  info: DiscrepancyInfo;
}

/** Hiển thị mức chênh lệch số lượng của phiếu nhập kho. */
export function WarehouseReceiptDiscrepancy({ info }: WarehouseReceiptDiscrepancyProps) {
  const color = info.isExceeded ? 'text-red-700' : 'text-emerald-700';

  return (
    <Card className={info.isExceeded ? 'border-red-200 bg-red-50' : 'border-emerald-200 bg-emerald-50'}>
      <CardContent className="p-3">
        <div className="flex items-start gap-2">
          {info.isExceeded ? (
            <AlertTriangle className="mt-0.5 size-4 text-red-600" />
          ) : (
            <CheckCircle2 className="mt-0.5 size-4 text-emerald-600" />
          )}
          <div className="space-y-1 text-sm">
            <p className={`font-semibold ${color}`}>
              {info.isExceeded ? 'Chênh lệch vượt ngưỡng!' : 'Chênh lệch trong ngưỡng cho phép'}
            </p>
            <div className={`grid grid-cols-2 gap-x-4 gap-y-0.5 ${color}`}>
              <span>Chênh lệch:</span>
              <span className="font-medium">
                {info.difference >= 0 ? '+' : ''}
                {Math.round(info.difference * 100) / 100} kg
              </span>
              <span>% Chênh lệch:</span>
              <span className="font-medium">
                {info.difference >= 0 ? '+' : ''}
                {Math.round(info.percent * 100) / 100}%
              </span>
              <span>Ngưỡng:</span>
              <span className="font-medium">2%</span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
