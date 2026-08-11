import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import type { WarehouseReceiptResponse } from '@/types/warehouseReceipt';

interface Props {
  receipt: WarehouseReceiptResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function WarehouseReceiptDetailSheet({ receipt, open, onOpenChange }: Props) {
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

  const rows: [string, string | number | undefined | null][] = [
    ['Mã sự kiện', receipt.id],
    ['Loại sự kiện', 'Nhập kho (WAREHOUSE_RECEIPT)'],
    ['Mã truy xuất', receipt.traceCode],
    ['Lô hàng', receipt.shipmentName],
    ['Mã lô hàng', receipt.shipmentId],
    ['Số lượng khai báo', `${receipt.declaredQuantity?.toLocaleString('vi-VN')} kg`],
    ['Số lượng thực nhận', `${receipt.receivedQuantity?.toLocaleString('vi-VN')} kg`],
    ['Chênh lệch', `${(receipt.discrepancy ?? 0) >= 0 ? '+' : ''}${receipt.discrepancy?.toLocaleString('vi-VN')} kg`],
    ['% Chênh lệch', `${(receipt.discrepancyPercent ?? 0) >= 0 ? '+' : ''}${receipt.discrepancyPercent}%`],
    ['Vượt ngưỡng', receipt.isDiscrepancyExceeded ? 'Có' : 'Không'],
    ['Lý do chênh lệch', receipt.reason],
    ['Tình trạng hàng', receipt.conditionNote],
    ['Ngày nhập kho', receipt.receiptDate ? formatDate(receipt.receiptDate) : '—'],
    ['Thời gian ghi nhận', formatDateTime(receipt.recordedAt)],
    ['Người ghi nhận', receipt.recordedBy],
    ['Thông báo đã gửi', receipt.notificationSent ? 'Đã gửi' : 'Không'],
  ];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-lg">
            Chi tiết nhập kho
          </DialogTitle>
          <DialogDescription>
            Sự kiện nhập kho — chỉ đọc
          </DialogDescription>
        </DialogHeader>
        <div className="mt-4 space-y-4">
          {rows.map(([label, value]) => {
            if (value === undefined || value === null) return null;
            return (
              <div key={label} className="flex flex-col gap-0.5 border-b border-gray-100 pb-3">
                <span className="text-xs text-muted-foreground">{label}</span>
                <span className="text-sm font-medium text-foreground">
                  {String(value)}
                </span>
              </div>
            );
          })}
        </div>
      </DialogContent>
    </Dialog>
  );
}