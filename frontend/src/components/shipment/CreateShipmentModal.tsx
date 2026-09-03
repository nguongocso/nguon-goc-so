import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Info, AlertTriangle } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { getRemainingCodes } from '@/api/codeRangeApi';
import type { CreateShipmentPayload } from '@/types/shipment';
import type { RemainingCodesResponse } from '@/types/codeRange';

const formSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên lô hàng'),
  totalQuantity: z
    .number({ invalid_type_error: 'Vui lòng nhập số lượng' })
    .int()
    .min(1, 'Số lượng phải lớn hơn 0'),
  packagingInfo: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface CreateShipmentModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateShipmentPayload) => Promise<void>;
  productionLotId: string;
  loading?: boolean;
}

export const CreateShipmentModal = ({
  open,
  onClose,
  onSubmit,
  productionLotId,
  loading = false,
}: CreateShipmentModalProps) => {
  const { user } = useAuth();
  const [remainingCodes, setRemainingCodes] = useState<RemainingCodesResponse | null>(null);
  const [remainingLoading, setRemainingLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: '',
      totalQuantity: undefined,
      packagingInfo: '',
    },
  });

  useEffect(() => {
    if (open) {
      reset();
      setRemainingCodes(null);
      if (user?.organizationId) {
        setRemainingLoading(true);
        getRemainingCodes(user.organizationId)
          .then(setRemainingCodes)
          .catch(() => setRemainingCodes(null))
          .finally(() => setRemainingLoading(false));
      }
    }
  }, [open, reset, user?.organizationId]);

  const onFormSubmit = async (data: FormValues) => {
    await onSubmit({
      productionLotId,
      name: data.name,
      totalQuantity: data.totalQuantity,
      packagingInfo: data.packagingInfo || undefined,
    });
    reset();
    onClose();
  };

  const remainingCount = remainingCodes?.remainingCount ?? 0;
  const totalLimit = remainingCodes?.totalLimit ?? 0;
  const hasCodeRange = remainingCodes?.hasCodeRange ?? false;
  const isExhausted =
    remainingCodes !== null &&
    (!hasCodeRange || remainingCount <= 0);

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Tạo lô hàng mới</DialogTitle>
          <DialogDescription>
            Nhập thông tin lô hàng để sinh mã truy xuất.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-4">
          <div className="rounded-lg border bg-card p-3 text-sm">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground">Mã còn lại:</span>
              {remainingLoading && !remainingCodes ? (
                <span className="text-sm text-muted-foreground">Đang tải...</span>
              ) : !hasCodeRange ? (
                <span className="flex items-center gap-1 font-semibold text-amber-600">
                  <AlertTriangle className="h-3 w-3" />
                  Chưa có dải mã
                </span>
              ) : (
                <span className="font-semibold text-emerald-600">
                  {remainingCount.toLocaleString()} / {totalLimit.toLocaleString()}
                </span>
              )}
            </div>
            {!remainingLoading && !remainingCodes && user?.organizationId && (
              <p className="mt-1 text-xs text-red-500">
                Không thể tải số lượng mã còn lại.
              </p>
            )}
            {!user?.organizationId && (
              <p className="mt-1 flex items-center gap-1 text-xs text-red-500">
                <AlertTriangle className="h-3 w-3" />
                Không xác định được tổ chức.
              </p>
            )}
            {!remainingLoading && hasCodeRange && remainingCount <= 0 && (
              <p className="mt-1 flex items-center gap-1 text-xs text-red-500">
                <AlertTriangle className="h-3 w-3" />
                Đã hết mã truy xuất. Không thể tạo thêm lô hàng.
              </p>
            )}
            {!remainingLoading && remainingCodes !== null && !hasCodeRange && (
              <p className="mt-1 flex items-center gap-1 text-xs text-red-500">
                <AlertTriangle className="h-3 w-3" />
                Tổ chức chưa được cấp dải mã truy xuất.
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="name">Tên lô hàng *</Label>
            <Input
              id="name"
              {...register('name')}
              placeholder="Ví dụ: Lô hàng chè Long Cốc T7/2026"
            />
            {errors.name && <p className="text-sm text-red-500">{errors.name.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="totalQuantity">Số lượng *</Label>
            <Input
              id="totalQuantity"
              type="number"
              {...register('totalQuantity', { valueAsNumber: true })}
              placeholder="Nhập số lượng đơn vị"
              min="1"
              step="1"
            />
            {errors.totalQuantity && (
              <p className="text-sm text-red-500">{errors.totalQuantity.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="packagingInfo">Thông tin đóng gói (không bắt buộc)</Label>
            <Input
              id="packagingInfo"
              {...register('packagingInfo')}
              placeholder="Ví dụ: Túi 500g, đóng thùng 20 túi/thùng"
            />
          </div>

          <Alert className="bg-blue-50 border-blue-200">
            <Info className="h-4 w-4 text-blue-500" />
            <AlertDescription className="text-sm text-blue-700">
              Số lượng mã truy xuất sẽ được sinh tương ứng với số lượng bạn nhập.
              Đảm bảo không vượt quá hạn mức dải mã của tổ chức.
            </AlertDescription>
          </Alert>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose} disabled={loading}>
              Hủy
            </Button>
            <Button type="submit" disabled={loading || isExhausted} variant="create">
              {loading ? 'Đang tạo...' : 'Tạo lô hàng'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};