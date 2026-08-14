import { useState } from 'react';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogPopup,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type { Shipment } from '@/types/shipment';
import { AlertTriangle, LoaderCircle } from 'lucide-react';

interface RecallShipmentDialogProps {
  shipment: Shipment | null;
  isRecalling: boolean;
  onClose: () => void;
  onConfirm: (shipmentId: string, reason: string) => Promise<void>;
}

export const RecallShipmentDialog = ({
  shipment,
  isRecalling,
  onClose,
  onConfirm,
}: RecallShipmentDialogProps) => {
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleClose = () => {
    setReason('');
    setError(null);
    onClose();
  };

  const handleConfirm = async () => {
    if (!shipment) return;

    if (!reason.trim()) {
      setError('Lý do thu hồi không được để trống.');
      return;
    }

    try {
      await onConfirm(shipment.id, reason.trim());
      handleClose();
    } catch {
      // Hook đã hiển thị thông báo lỗi; giữ dialog mở để người dùng thử lại.
    }
  };

  return (
    <AlertDialog
      open={shipment !== null}
      onOpenChange={(open) => {
        if (!open && !isRecalling) handleClose();
      }}
    >
      <AlertDialogPopup className="max-w-lg">
        <AlertDialogHeader>
          <div className="mb-2 flex size-11 items-center justify-center rounded-full bg-red-100 text-red-700">
            <AlertTriangle className="size-6" />
          </div>
          <AlertDialogTitle>Thu hồi lô hàng</AlertDialogTitle>
          <AlertDialogDescription>
            Lô hàng và toàn bộ mã truy xuất liên quan sẽ chuyển sang trạng thái
            "Đã thu hồi". Cảnh báo sẽ hiển thị công khai trên trang tra cứu và
            không thể ẩn sau đó.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <dl className="mt-5 divide-y rounded-lg border bg-slate-50 px-4">
          <div className="flex items-start justify-between gap-4 py-3">
            <dt className="text-sm text-muted-foreground">Lô hàng</dt>
            <dd className="text-right text-sm font-semibold">
              {shipment?.name ?? '—'}
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4 py-3">
            <dt className="text-sm text-muted-foreground">Lô sản xuất</dt>
            <dd className="text-right text-sm font-semibold">
              {shipment?.productionLotName ?? '—'}
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4 py-3">
            <dt className="text-sm text-muted-foreground">Số mã truy xuất</dt>
            <dd className="text-right text-sm font-semibold">
              {(shipment?.traceCodes?.length ?? 0).toLocaleString('vi-VN')} mã
            </dd>
          </div>
        </dl>

        <div className="mt-4 space-y-1.5">
          <Label htmlFor="recall-reason">
            Lý do thu hồi <span className="text-red-600">*</span>
          </Label>
          <Textarea
            id="recall-reason"
            placeholder="VD: Phát hiện dư lượng thuốc bảo vệ thực vật vượt ngưỡng cho phép"
            value={reason}
            disabled={isRecalling}
            onChange={(e) => {
              setReason(e.target.value);
              if (error) setError(null);
            }}
            rows={3}
          />
          {error && <p className="text-sm text-red-600">{error}</p>}
        </div>

        <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Hãy kiểm tra đúng lô hàng trước khi xác nhận. Thao tác này không thể
          hoàn tác.
        </p>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isRecalling} onClick={handleClose}>
            Hủy
          </AlertDialogCancel>
          {/* CHANGED: variant="destructive" → variant="delete" */}
          <Button
            type="button"
            variant="delete"
            disabled={isRecalling}
            onClick={() => {
              void handleConfirm();
            }}
          >
            {isRecalling && <LoaderCircle className="size-4 animate-spin" />}
            {isRecalling ? 'Đang thu hồi...' : 'Xác nhận thu hồi'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};