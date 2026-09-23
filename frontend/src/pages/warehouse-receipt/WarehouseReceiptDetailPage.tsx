import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { AlertTriangle, LoaderCircle, Package, User, CalendarClock } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { HelpButton } from '@/components/help/HelpButton';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';
import { WarehouseReceiptQuantityComparison } from './components/WarehouseReceiptQuantityComparison';
import {
  formatWarehouseReceiptDate,
  formatWarehouseReceiptDateTime,
} from './warehouseReceiptFormatters';

/**
 * Trang chi tiết sự kiện nhập kho và đối chiếu số lượng khai báo, thực nhận.
 */
export default function WarehouseReceiptDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const { detail, isLoadingDetail, error, fetchDetail } = useWarehouseReceipt();

  useEffect(() => {
    if (eventId) {
      fetchDetail(eventId);
    }
  }, [eventId, fetchDetail]);

  if (isLoadingDetail) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoaderCircle className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="mx-auto max-w-3xl space-y-6 p-4">
        <Alert variant="destructive">
          <AlertDescription>
            {error || 'Không tìm thấy sự kiện nhập kho.'}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const isExceeded = !!detail.isDiscrepancyExceeded;
  const hasCondition = !!detail.conditionNote && detail.conditionNote.trim() !== '';
  const hasReason = !!detail.reason && detail.reason.trim() !== '';

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Nhập kho
            </h1>
            <Badge
              variant={isExceeded ? 'destructive' : 'outline'}
              className={
                isExceeded
                  ? 'rounded-full'
                  : 'rounded-full border-emerald-300 text-emerald-700'
              }
            >
              {isExceeded ? 'Chênh lệch' : 'Khớp'}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {detail.shipmentName || 'Chi tiết sự kiện nhập kho'}
            {detail.recordedAt ? ` • ${formatWarehouseReceiptDateTime(detail.recordedAt)}` : ''}
          </p>
        </div>
        <HelpButton screenKey="warehouse-receipt" />
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <Package className="size-5 text-blue-700" />
            Thông tin lô hàng
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Mã truy xuất</p>
              <p className="font-mono text-sm font-medium">
                {detail.traceCode || '—'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Tên lô hàng</p>
              <p className="text-sm font-medium">{detail.shipmentName || '—'}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <WarehouseReceiptQuantityComparison detail={detail} isExceeded={isExceeded} />

      {hasCondition && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Tình trạng hàng</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-foreground">{detail.conditionNote}</p>
          </CardContent>
        </Card>
      )}

      {hasReason && (
        <Card className={isExceeded ? 'border-red-200' : ''}>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <AlertTriangle className="size-5 text-red-600" />
              Lý do chênh lệch
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-foreground">{detail.reason}</p>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Thông tin ghi nhận</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-1">
              <p className="flex items-center gap-1 text-xs text-muted-foreground">
                <User className="size-3" />
                Người ghi nhận
              </p>
              <p className="text-sm font-medium">{detail.recordedBy || '—'}</p>
            </div>
            <div className="space-y-1">
              <p className="flex items-center gap-1 text-xs text-muted-foreground">
                <CalendarClock className="size-3" />
                Thời gian ghi nhận
              </p>
              <p className="text-sm font-medium">
                {detail.recordedAt ? formatWarehouseReceiptDateTime(detail.recordedAt) : '—'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Ngày nhập kho</p>
              <p className="text-sm font-medium">
                {detail.receiptDate ? formatWarehouseReceiptDate(detail.receiptDate) : '—'}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
