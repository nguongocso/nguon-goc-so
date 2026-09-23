import { AlertCircle, CheckCircle2, Package } from 'lucide-react';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';
import type { ShipmentStatusItem } from './coopWarehouseUtils';

interface CoopWarehouseShipmentSectionProps {
  mode: 'entry' | 'exit';
  items: ShipmentStatusItem[];
  invalidItems: ShipmentStatusItem[];
  loading: boolean;
}

/** Hiển thị danh sách lô hàng và cảnh báo trạng thái kho. */
export function CoopWarehouseShipmentSection({
  mode,
  items,
  invalidItems,
  loading,
}: CoopWarehouseShipmentSectionProps) {
  const isEntry = mode === 'entry';
  const action = isEntry ? 'nhập' : 'xuất';

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between border-b border-slate-100 pb-1">
        <Label className="flex items-center gap-2 text-sm font-semibold text-slate-900">
          <Package className={cn('h-4 w-4', isEntry ? 'text-emerald-600' : 'text-amber-600')} />
          1. Danh sách lô hàng thực hiện {action} kho ({items.length} lô)
        </Label>
      </div>

      {loading ? (
        <div className="animate-pulse rounded-lg border bg-slate-50 p-4 text-sm text-slate-600">
          Đang tải thông tin các lô hàng...
        </div>
      ) : items.length === 0 ? (
        <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          Chưa có lô hàng nào được chọn. Vui lòng quay lại danh sách để chọn lô hàng.
        </div>
      ) : (
        <div className="max-h-64 space-y-2 overflow-y-auto pr-1">
          {items.map(({ shipment, warehouseStatus }) => {
            const invalid = warehouseStatus === (isEntry ? 'IN_WAREHOUSE' : 'NOT_IN_WAREHOUSE');
            return (
              <div
                key={shipment.id}
                className={cn(
                  'flex items-center justify-between rounded-lg border p-3.5 text-sm',
                  invalid
                    ? 'border-red-200 bg-red-50/70 text-red-950'
                    : isEntry
                      ? 'border-emerald-200 bg-emerald-50/50 text-emerald-950'
                      : 'border-amber-200 bg-amber-50/50 text-amber-950',
                )}
              >
                <div className="space-y-0.5">
                  <p className="font-semibold text-slate-900">{shipment.name}</p>
                  <p className="text-xs text-slate-600">
                    Số lượng: <span className="font-medium">{shipment.totalQuantity}</span> | Quy cách:{' '}
                    <span className="font-medium">{shipment.packagingInfo || '—'}</span>
                  </p>
                </div>
                <span
                  className={cn(
                    'inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold',
                    invalid
                      ? 'border-red-300 bg-red-100 text-red-800'
                      : 'border-emerald-300 bg-emerald-100 text-emerald-800',
                  )}
                >
                  {invalid ? <AlertCircle className="h-3.5 w-3.5" /> : <CheckCircle2 className="h-3.5 w-3.5" />}
                  {isEntry
                    ? invalid ? 'Đang ở trong kho' : 'Sẵn sàng nhập kho'
                    : invalid ? 'Chưa có sự kiện nhập kho' : 'Đang trong kho (Đã nhập)'}
                </span>
              </div>
            );
          })}
        </div>
      )}

      {invalidItems.length > 0 && (
        <div className="space-y-1.5 rounded-lg border-2 border-red-400 bg-red-100/90 p-4 text-sm text-red-950">
          <p className="flex items-center gap-2 text-base font-bold text-red-900">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
            Cảnh báo vi phạm ràng buộc nghiệp vụ:
          </p>
          {invalidItems.map(({ shipment }) => (
            <p key={shipment.id} className="pl-7 font-medium text-red-900">
              • Lô hàng <span className="font-bold text-red-950">&quot;{shipment.name}&quot;</span>{' '}
              {isEntry
                ? 'hiện đang ở trong kho HTX, vui lòng ghi xuất kho trước khi nhập mới.'
                : 'chưa được ghi nhận nhập kho HTX. Hãy ghi nhập kho trước khi xuất kho.'}
            </p>
          ))}
        </div>
      )}
    </div>
  );
}
