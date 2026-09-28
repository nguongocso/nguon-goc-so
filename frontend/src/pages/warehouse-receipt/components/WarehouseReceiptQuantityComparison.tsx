import { AlertTriangle, CheckCircle2 } from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

import type { WarehouseReceiptResponse } from '@/types/warehouseReceipt';

const ALLOWED_THRESHOLD = 2;

interface WarehouseReceiptQuantityComparisonProps {
  detail: WarehouseReceiptResponse;
  isExceeded: boolean;
}

/** Hiển thị kết quả đối chiếu số lượng khai báo và thực nhận. */
export function WarehouseReceiptQuantityComparison({
  detail,
  isExceeded,
}: WarehouseReceiptQuantityComparisonProps) {
  const valueBackground = isExceeded ? 'bg-red-50' : 'bg-gray-50';
  const valueLabelColor = isExceeded ? 'text-red-700' : 'text-gray-600';
  const valueColor = isExceeded ? 'text-red-900' : 'text-gray-900';

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          {isExceeded ? (
            <AlertTriangle className="size-5 text-red-600" />
          ) : (
            <CheckCircle2 className="size-5 text-emerald-600" />
          )}
          Đối chiếu số lượng
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <div className="rounded-lg bg-blue-50 p-4">
            <p className="text-xs text-blue-700">Số lượng khai báo</p>
            <p className="mt-1 text-xl font-bold text-blue-900">
              {detail.declaredQuantity?.toLocaleString('vi-VN')} kg
            </p>
          </div>
          <div className="rounded-lg bg-emerald-50 p-4">
            <p className="text-xs text-emerald-700">Số lượng thực nhận</p>
            <p className="mt-1 text-xl font-bold text-emerald-900">
              {detail.receivedQuantity?.toLocaleString('vi-VN')} kg
            </p>
          </div>
          <div className={`rounded-lg p-4 ${valueBackground}`}>
            <p className={`text-xs ${valueLabelColor}`}>Chênh lệch</p>
            <p className={`mt-1 text-xl font-bold ${valueColor}`}>
              {(detail.discrepancy ?? 0) >= 0 ? '+' : ''}
              {detail.discrepancy?.toLocaleString('vi-VN')} kg
            </p>
          </div>
          <div className={`rounded-lg p-4 ${valueBackground}`}>
            <p className={`text-xs ${valueLabelColor}`}>Tỷ lệ chênh lệch</p>
            <p className={`mt-1 text-xl font-bold ${valueColor}`}>
              {(detail.discrepancyPercent ?? 0) >= 0 ? '+' : ''}
              {detail.discrepancyPercent}%
            </p>
          </div>
        </div>

        <div className="mt-4 flex items-center gap-2 text-sm">
          <span className="text-muted-foreground">Ngưỡng cho phép:</span>
          <Badge variant="outline">{ALLOWED_THRESHOLD}%</Badge>
          <span className="ml-2">
            {isExceeded ? 'Đã vượt ngưỡng' : 'Trong ngưỡng cho phép'}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
