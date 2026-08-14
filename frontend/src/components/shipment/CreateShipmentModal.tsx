import { useEffect } from 'react';
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
import { Info } from 'lucide-react';
import type { CreateShipmentPayload } from '@/types/shipment';

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
    }
  }, [open, reset]);

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
            {/* CHANGED: thêm variant="create" */}
            <Button type="submit" disabled={loading} variant="create">
              {loading ? 'Đang tạo...' : 'Tạo lô hàng'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};