import { useEffect, useState } from 'react';
import { AlertTriangle, LoaderCircle } from 'lucide-react';

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
import { Textarea } from '@/components/ui/textarea';
import type { Shipment } from '@/types/shipment';

interface RecallShipmentDialogProps {
  shipment: Shipment | null;
  isRecalling: boolean;
  onClose: () => void;
  onConfirm: (
    shipmentId: string,
    reason: string,
  ) => Promise<void>;
}

export const RecallShipmentDialog = ({
  shipment,
  isRecalling,
  onClose,
  onConfirm,
}: RecallShipmentDialogProps) => {
  const [reason, setReason] = useState('');

  const normalizedReason = reason.trim();
  const isReasonValid = normalizedReason.length > 0;

  useEffect(() => {
    if (shipment) {
      setReason('');
    }
  }, [shipment]);

  const handleConfirm = async () => {
    if (!shipment || !isReasonValid) return;

    try {
      await onConfirm(
        shipment.id,
        normalizedReason,
      );

      onClose();
    } catch {
      /*
       * useShipments đã hiển thị thông báo lỗi.
       * Giữ dialog mở để người dùng kiểm tra và thử lại.
       */
    }
  };

  return (
    <AlertDialog
      open={shipment !== null}
      onOpenChange={(open) => {
        if (!open && !isRecalling) {
          onClose();
        }
      }}
    >
      <AlertDialogPopup className="max-w-lg">
        <AlertDialogHeader>
          <div className="mb-2 flex size-11 items-center justify-center rounded-full bg-red-100 text-red-700">
            <AlertTriangle className="size-6" />
          </div>

          <AlertDialogTitle>
            Thu hồi lô hàng
          </AlertDialogTitle>

          <AlertDialogDescription>
            Lô hàng và toàn bộ mã truy xuất liên quan sẽ
            chuyển sang trạng thái “Đã thu hồi”. Cảnh báo
            thu hồi sẽ được hiển thị trên trang tra cứu
            công khai.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="mt-5 space-y-4">
          <div className="rounded-lg border bg-slate-50 px-4 py-3">
            <p className="text-sm text-muted-foreground">
              Lô hàng
            </p>

            <p className="mt-1 font-semibold">
              {shipment?.name ?? '—'}
            </p>
          </div>

          <div className="space-y-2">
            <label
              htmlFor="recall-reason"
              className="text-sm font-medium"
            >
              Lý do thu hồi{' '}
              <span className="text-red-600">*</span>
            </label>

            <Textarea
              id="recall-reason"
              value={reason}
              onChange={(event) =>
                setReason(event.target.value)
              }
              placeholder="Nhập lý do thu hồi lô hàng..."
              maxLength={1000}
              rows={5}
              disabled={isRecalling}
            />

            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>
                Lý do không được để trống.
              </span>

              <span>{reason.length}/1000</span>
            </div>
          </div>

          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            Không thể hoàn tác thao tác này. Hãy kiểm tra
            đúng lô hàng và lý do thu hồi trước khi xác nhận.
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isRecalling}>
            Hủy
          </AlertDialogCancel>

          <Button
            type="button"
            variant="destructive"
            disabled={isRecalling || !isReasonValid}
            onClick={() => {
              void handleConfirm();
            }}
          >
            {isRecalling && (
              <LoaderCircle className="size-4 animate-spin" />
            )}

            {isRecalling
              ? 'Đang thu hồi...'
              : 'Xác nhận thu hồi'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};