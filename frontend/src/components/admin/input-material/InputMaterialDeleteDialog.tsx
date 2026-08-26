import { useState } from 'react';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { AlertCircle, ShieldAlert } from 'lucide-react';
import { toast } from 'sonner';
import { deleteInputMaterial, toggleInputMaterialStatus } from '@/api/inputMaterialApi';
import type { InputMaterial } from '@/types/inputMaterial';

interface Props {
  material: InputMaterial | null;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const InputMaterialDeleteDialog = ({ material, open, onClose, onSuccess }: Props) => {
  const [submitting, setSubmitting] = useState(false);
  const [blockedError, setBlockedError] = useState<string | null>(null);

  if (!material) return null;

  const handleDelete = async () => {
    try {
      setSubmitting(true);
      setBlockedError(null);
      await deleteInputMaterial(material.id);
      toast.success(`Đã xóa vật tư "${material.name}" thành công`);
      onSuccess();
      onClose();
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        'Vật tư đã được dùng trong nhật ký canh tác. Hệ thống chặn xóa và chỉ cho phép ngừng sử dụng.';
      setBlockedError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSwitchToDeactivate = async () => {
    try {
      setSubmitting(true);
      await toggleInputMaterialStatus(material.id, false);
      toast.success(`Đã chuyển vật tư "${material.name}" sang trạng thái Ngừng sử dụng`);
      onSuccess();
      onClose();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái');
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    setBlockedError(null);
    onClose();
  };

  return (
    <AlertDialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <AlertDialogContent className="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2 text-red-600 dark:text-red-400">
            <AlertCircle className="h-5 w-5" />
            Xác nhận xóa vật tư đầu vào
          </AlertDialogTitle>
          <AlertDialogDescription className="text-sm pt-2">
            Bạn có chắc chắn muốn xóa bản ghi vật tư{' '}
            <strong className="text-gray-900 dark:text-gray-100">"{material.name}"</strong> khỏi danh mục dùng chung?
          </AlertDialogDescription>
        </AlertDialogHeader>

        {blockedError && (
          <div className="p-3 my-2 rounded-md bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-800 flex items-start gap-2.5">
            <ShieldAlert className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
            <div className="flex flex-col gap-1.5 text-xs text-amber-800 dark:text-amber-300">
              <span className="font-semibold">Hệ thống chặn xóa bản ghi (TC-04):</span>
              <span>{blockedError}</span>
            </div>
          </div>
        )}

        <AlertDialogFooter className="pt-2">
          <AlertDialogCancel onClick={handleClose} disabled={submitting}>
            Hủy bỏ
          </AlertDialogCancel>

          {blockedError ? (
            <Button
              type="button"
              variant="default"
              onClick={handleSwitchToDeactivate}
              disabled={submitting}
              className="bg-amber-600 hover:bg-amber-700 text-white"
            >
              Ngừng sử dụng vật tư
            </Button>
          ) : (
            <Button
              type="button"
              variant="destructive"
              onClick={handleDelete}
              disabled={submitting}
            >
              {submitting ? 'Đang xóa...' : 'Xóa vật tư'}
            </Button>
          )}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};
