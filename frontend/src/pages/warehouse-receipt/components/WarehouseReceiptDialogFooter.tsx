import { LoaderCircle, Send } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { DialogFooter } from '@/components/ui/dialog';

interface WarehouseReceiptDialogFooterProps {
  isSubmitting: boolean;
  canSubmit: boolean;
  onCancel: () => void;
}

/** Hiển thị thao tác xác nhận hoặc hủy phiếu nhập kho. */
export function WarehouseReceiptDialogFooter({
  isSubmitting,
  canSubmit,
  onCancel,
}: WarehouseReceiptDialogFooterProps) {
  return (
    <DialogFooter>
      <Button
        type="button"
        variant="outline"
        onClick={onCancel}
        disabled={isSubmitting}
      >
        Hủy
      </Button>
      <Button
        type="submit"
        variant="view"
        disabled={isSubmitting || !canSubmit}
      >
        {isSubmitting ? (
          <>
            <LoaderCircle className="size-4 animate-spin" />
            Đang ghi nhận...
          </>
        ) : (
          <>
            <Send className="size-4" />
            Xác nhận nhập kho
          </>
        )}
      </Button>
    </DialogFooter>
  );
}
