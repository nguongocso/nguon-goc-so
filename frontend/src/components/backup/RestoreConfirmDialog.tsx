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
import { AlertTriangle, Loader2 } from 'lucide-react';
import type { BackupHistoryItem } from '@/types/backup';

interface RestoreConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>;
  backup: BackupHistoryItem | null;
  isRestoring?: boolean;
}

export const RestoreConfirmDialog = ({
  open,
  onClose,
  onConfirm,
  backup,
  isRestoring = false,
}: RestoreConfirmDialogProps) => {
  if (!backup) return null;

  const handleConfirm = async () => {
    await onConfirm();
  };

  return (
    <AlertDialog open={open} onOpenChange={(o) => !o && onClose()}>
      <AlertDialogPopup>
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2 text-destructive">
            <AlertTriangle className="h-5 w-5" />
            ⚠️ Xác nhận phục hồi dữ liệu
          </AlertDialogTitle>
          <AlertDialogDescription className="space-y-3">
            <p>
              Bạn sắp phục hồi dữ liệu từ bản sao lưu:
            </p>
            <div className="rounded-md bg-muted p-3 font-mono text-sm">
              {backup.fileName || `Bản sao lưu #${backup.id}`}
            </div>
            <div className="rounded-md border border-warning bg-warning-bg p-4 text-sm">
              <p className="font-semibold text-warning">🔴 Lưu ý quan trọng:</p>
              <ul className="mt-2 list-disc pl-5 space-y-1 text-muted-foreground">
                <li>
                  Hệ thống sẽ <strong>tạm dừng</strong> mọi hoạt động trong thời gian phục hồi.
                </li>
                <li>
                  Người dùng sẽ nhận được <strong>503 Service Unavailable</strong>.
                </li>
                <li>
                  Hệ thống sẽ tự động <strong>sao lưu dữ liệu hiện tại</strong> trước khi phục hồi.
                </li>
                <li>
                  Quá trình này <strong>không thể hủy bỏ</strong> và có thể mất vài phút.
                </li>
              </ul>
            </div>
            <p className="mt-2 font-medium">
              Bạn có chắc chắn muốn tiếp tục?
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isRestoring}>Hủy</AlertDialogCancel>
          <AlertDialogAction
            onClick={handleConfirm}
            disabled={isRestoring}
            className="bg-warning hover:bg-warning/90 text-white"
          >
            {isRestoring ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang phục hồi...
              </>
            ) : (
              'Xác nhận phục hồi'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};