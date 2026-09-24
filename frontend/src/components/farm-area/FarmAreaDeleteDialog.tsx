import React, { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertTriangle, Trash2, PowerOff } from 'lucide-react';
import { toast } from 'sonner';
import { deleteFarmArea, toggleFarmAreaStatus } from '@/api/farmAreaApi';
import { toApiError } from '@/api/apiError';
import type { FarmArea } from '@/types/farmArea';

interface FarmAreaDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  farmArea: FarmArea | null;
}

export const FarmAreaDeleteDialog: React.FC<FarmAreaDeleteDialogProps> = ({
  open,
  onClose,
  onSuccess,
  farmArea,
}) => {
  // State: trạng thái tải
  const [loading, setLoading] = useState(false);

  // Handlers: xoá và ngừng sử dụng vùng trồng
  const handleDelete = async () => {
    if (!farmArea || (farmArea.associatedLotsCount || 0) > 0) return;
    try {
      setLoading(true);
      await deleteFarmArea(farmArea.id);
      toast.success('Đã xóa vùng trồng thành công');
      onSuccess();
      onClose();
    } catch (error: unknown) {
      // Chuẩn hoá lỗi API để tránh treo trạng thái tải
      toast.error(toApiError(error, 'Không thể xóa vùng trồng').message);
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async () => {
    if (!farmArea) return;
    try {
      setLoading(true);
      await toggleFarmAreaStatus(farmArea.id, false);
      toast.success(`Đã chuyển vùng trồng '${farmArea.name}' sang trạng thái Ngừng sử dụng`);
      onSuccess();
      onClose();
    } catch (error: unknown) {
      // Chuẩn hoá lỗi API để tránh treo trạng thái tải
      toast.error(toApiError(error, 'Không thể đổi trạng thái vùng trồng').message);
    } finally {
      setLoading(false);
    }
  };

  // Giá trị dẫn xuất từ vùng trồng hiện tại
  const associatedLots = farmArea?.associatedLotsCount || 0;
  const isBlocked = associatedLots > 0;

  // JSX: trả về null khi chưa chọn vùng trồng
  if (!farmArea) return null;

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-md bg-white rounded-xl shadow-xl">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Trash2 className="h-5 w-5 text-red-600" />
            Xác nhận xóa vùng trồng
          </DialogTitle>
          <DialogDescription className="text-sm text-slate-600 pt-1">
            Bạn có chắc chắn muốn xóa vùng trồng <span className="font-bold text-slate-900">"{farmArea.name}"</span>?
          </DialogDescription>
        </DialogHeader>

        {isBlocked ? (
          <Alert className="bg-red-50 border-red-200 text-red-900 my-2">
            <AlertTriangle className="h-5 w-5 text-red-600 shrink-0" />
            <div>
              <AlertTitle className="font-semibold text-sm">Chặn thao tác xóa (TC-03)!</AlertTitle>
              <AlertDescription className="text-xs leading-relaxed mt-1">
                Vùng trồng này đã có <span className="font-bold">{associatedLots} lô sản xuất</span> liên quan. Hệ thống không cho phép xóa dữ liệu gốc đã được liên kết với lô sản xuất. Bạn có thể chuyển vùng trồng sang trạng thái <span className="font-bold underline">Ngừng sử dụng</span>.
              </AlertDescription>
            </div>
          </Alert>
        ) : (
          <p className="text-xs text-slate-500 py-1">
            Hành động này sẽ xóa hoàn toàn vùng trồng chưa có lô sản xuất nào. Thao tác này không thể hoàn tác.
          </p>
        )}

        <DialogFooter className="pt-3 gap-2">
          <Button
            variant="outline"
            onClick={onClose}
            disabled={loading}
          >
            Hủy
          </Button>

          {isBlocked ? (
            <Button
              onClick={handleDeactivate}
              disabled={loading}
              className="bg-amber-600 hover:bg-amber-700 text-white flex items-center gap-1.5"
            >
              <PowerOff className="h-4 w-4" />
              Chuyển sang Ngừng sử dụng
            </Button>
          ) : (
            <Button
              onClick={handleDelete}
              disabled={loading}
              className="bg-red-600 hover:bg-red-700 text-white"
            >
              {loading ? 'Đang xóa...' : 'Xóa vùng trồng'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
