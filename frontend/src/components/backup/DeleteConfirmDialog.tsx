import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogPopup,
} from '@/components/ui/alert-dialog';
import { Trash2, Loader2 } from 'lucide-react';
import type { BackupHistoryItem } from '@/types/backup';

interface DeleteConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
  backup: BackupHistoryItem | null;
  isDeleting?: boolean;
}

export const DeleteConfirmDialog = ({
  open,
  onClose,
  onConfirm,
  backup,
  isDeleting = false,
}: DeleteConfirmDialogProps) => {
  if (!backup) return null;

  const handleConfirm = async () => {
    await onConfirm();
  };

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogPopup>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2 text-destructive">
            <Trash2 className="h-5 w-5" />
            Xác nhận xóa bản sao lưu
          </AlertDialogTitle>
          <AlertDialogDescription className="space-y-3">
            <p>
              Bạn sắp xóa bản sao lưu:
            </p>
            <div className="rounded-md bg-muted p-3 font-mono text-sm">
              {backup.fileName || `Bản sao lưu #${backup.id}`}
            </div>
            <div className="rounded-md border border-destructive/20 bg-error-bg p-4 text-sm">
              <p className="font-semibold text-destructive">⚠️ Thao tác này không thể hoàn tác.</p>
              <p className="mt-1 text-muted-foreground">
                File sao lưu sẽ bị xóa khỏi máy chủ và không thể khôi phục lại.
              </p>
            </div>
            <p className="mt-2 font-medium">
              Bạn có chắc chắn muốn xóa bản sao lưu này?
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isDeleting}>Hủy</AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirm}
            disabled={isDeleting}
            className="bg-destructive hover:bg-destructive/90 text-white"
          >
            {isDeleting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang xóa...
              </>
            ) : (
              'Xác nhận xóa'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};