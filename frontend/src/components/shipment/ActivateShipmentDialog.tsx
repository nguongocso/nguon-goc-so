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
import type { Shipment } from '@/types/shipment';
import { LoaderCircle, ShieldCheck } from 'lucide-react';

interface ActivateShipmentDialogProps {
  shipment: Shipment | null;
  isActivating: boolean;
  onClose: () => void;
  onConfirm: (shipmentId: string) => Promise<void>;
}

export const ActivateShipmentDialog = ({
  shipment,
  isActivating,
  onClose,
  onConfirm,
}: ActivateShipmentDialogProps) => {
  const handleConfirm = async () => {
    if (!shipment) return;

    try {
      await onConfirm(shipment.id);
      onClose();
    } catch {
      // Hook đã hiển thị thông báo lỗi; giữ dialog mở để người dùng thử lại.
    }
  };

  return (
    <AlertDialog
      open={shipment !== null}
      onOpenChange={(open) => {
        if (!open && !isActivating) onClose();
      }}
    >
      <AlertDialogPopup className="max-w-lg">
        <AlertDialogHeader>
          <div className="mb-2 flex size-11 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
            <ShieldCheck className="size-6" />
          </div>
          <AlertDialogTitle>Kích hoạt tem truy xuất</AlertDialogTitle>
          <AlertDialogDescription>
            Xác nhận kích hoạt toàn bộ tem đã cấp cho lô hàng này.
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
            <dt className="text-sm text-muted-foreground">Số tem kích hoạt</dt>
            <dd className="text-right text-sm font-semibold">
              {(shipment?.traceCodes?.length ??
                shipment?.totalQuantity ??
                0).toLocaleString('vi-VN')}{' '}
              tem
            </dd>
          </div>
        </dl>

        <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Hãy kiểm tra đúng lô hàng trước khi xác nhận. Sau khi thành công,
          trạng thái lô hàng sẽ chuyển sang “Đã kích hoạt”.
        </p>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={isActivating}>Hủy</AlertDialogCancel>
          {/* CHANGED: thêm variant="edit" */}
          <Button
            type="button"
            disabled={isActivating}
            variant="edit"
            onClick={() => {
              void handleConfirm();
            }}
          >
            {isActivating && <LoaderCircle className="size-4 animate-spin" />}
            {isActivating ? 'Đang kích hoạt...' : 'Xác nhận kích hoạt'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};