import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  CheckCircle2,
  AlertTriangle,
  LoaderCircle,
  Package,
  User,
  CalendarClock,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { HelpButton } from '@/components/help/HelpButton';
import { useWarehouseReceipt } from '@/hooks/useWarehouseReceipt';

const ALLOWED_THRESHOLD = 2.0;

export default function WarehouseReceiptDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const { detail, isLoadingDetail, error, fetchDetail } = useWarehouseReceipt();

  useEffect(() => {
    if (eventId) {
      fetchDetail(eventId);
    }
  }, [eventId, fetchDetail]);

  const formatDate = (iso: string) => {
    try {
      return new Date(iso).toLocaleDateString('vi-VN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
      });
    } catch {
      return iso;
    }
  };

  const formatDateTime = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' })
        + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return iso;
    }
  };

  // Loading state
  if (isLoadingDetail) {
    return (
      <div className="flex items-center justify-center py-24">
        <LoaderCircle className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // Error / not found state
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
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Nhập kho & đối chiếu
            </h1>
            <Badge
              variant={isExceeded ? 'destructive' : 'outline'}
              className={isExceeded ? 'rounded-full' : 'rounded-full text-emerald-700 border-emerald-300'}
            >
              {isExceeded ? 'Chênh lệch' : 'Khớp'}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {detail.shipmentName || 'Chi tiết sự kiện nhập kho'}
            {detail.recordedAt ? ` • ${formatDateTime(detail.recordedAt)}` : ''}
          </p>
        </div>
        <HelpButton screenKey="warehouse-receipt" />
      </div>

      {/* Section 1 — Thông tin lô hàng */}
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

      {/* Section 2 — Đối chiếu số lượng */}
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
            <div className={`rounded-lg p-4 ${isExceeded ? 'bg-red-50' : 'bg-gray-50'}`}>
              <p className={`text-xs ${isExceeded ? 'text-red-700' : 'text-gray-600'}`}>Chênh lệch</p>
              <p className={`mt-1 text-xl font-bold ${isExceeded ? 'text-red-900' : 'text-gray-900'}`}>
                {(detail.discrepancy ?? 0) >= 0 ? '+' : ''}
                {detail.discrepancy?.toLocaleString('vi-VN')} kg
              </p>
            </div>
            <div className={`rounded-lg p-4 ${isExceeded ? 'bg-red-50' : 'bg-gray-50'}`}>
              <p className={`text-xs ${isExceeded ? 'text-red-700' : 'text-gray-600'}`}>Tỷ lệ chênh lệch</p>
              <p className={`mt-1 text-xl font-bold ${isExceeded ? 'text-red-900' : 'text-gray-900'}`}>
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

      {/* Section 3 — Tình trạng hàng (chỉ khi có dữ liệu) */}
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

      {/* Section 4 — Lý do chênh lệch (chỉ khi có dữ liệu) */}
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

      {/* Section 5 — Thông tin ghi nhận */}
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
                {detail.recordedAt ? formatDateTime(detail.recordedAt) : '—'}
              </p>
            </div>
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Ngày nhập kho</p>
              <p className="text-sm font-medium">
                {detail.receiptDate ? formatDate(detail.receiptDate) : '—'}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}