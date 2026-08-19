import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea'; // thêm import Textarea
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Switch } from '@/components/ui/switch';
import { standardFormSchema, type StandardFormValues } from '@/utils/validators';
import type { Standard } from '@/types/standard';

interface StandardFormProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: StandardFormValues) => Promise<void>;
  initialData?: Standard | null;
  isLoading?: boolean;
}

export const StandardForm: React.FC<StandardFormProps> = ({
  open,
  onClose,
  onSubmit,
  initialData,
  isLoading = false,
}) => {
  const isEdit = !!initialData;

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<StandardFormValues>({
    resolver: zodResolver(standardFormSchema),
    defaultValues: {
      name: '',
      description: '',
      issuingBody: '',
      isActive: true,
    },
  });

  const isActive = watch('isActive');

  useEffect(() => {
    if (open) {
      if (initialData) {
        reset({
          name: initialData.name,
          description: initialData.description || '',
          issuingBody: initialData.issuingBody || '',
          isActive: initialData.isActive,
        });
      } else {
        reset({
          name: '',
          description: '',
          issuingBody: '',
          isActive: true,
        });
      }
    }
  }, [open, initialData, reset]);

  const handleFormSubmit = async (data: StandardFormValues) => {
    await onSubmit(data);
  };

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? 'Cập nhật tiêu chuẩn' : 'Thêm mới tiêu chuẩn'}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(handleFormSubmit)}>
          <div className="space-y-4 py-2">
            {/* Tên tiêu chuẩn */}
            <div className="space-y-2">
              <Label htmlFor="name">Tên tiêu chuẩn *</Label>
              <Input
                id="name"
                {...register('name')}
                placeholder="Nhập tên tiêu chuẩn"
                disabled={isLoading}
              />
              {errors.name && (
                <p className="text-sm text-red-500">{errors.name.message}</p>
              )}
            </div>

            {/* Cơ quan ban hành */}
            <div className="space-y-2">
              <Label htmlFor="issuingBody">Cơ quan ban hành</Label>
              <Input
                id="issuingBody"
                {...register('issuingBody')}
                placeholder="Nhập cơ quan ban hành"
                disabled={isLoading}
              />
              {errors.issuingBody && (
                <p className="text-sm text-red-500">
                  {errors.issuingBody.message}
                </p>
              )}
            </div>

            {/* Mô tả - sử dụng Textarea */}
            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Textarea
                id="description"
                {...register('description')}
                placeholder="Nhập mô tả (không bắt buộc)"
                disabled={isLoading}
                rows={4} // số dòng hiển thị
                className="resize-vertical" // cho phép kéo dãn theo chiều dọc
              />
              {errors.description && (
                <p className="text-sm text-red-500">
                  {errors.description.message}
                </p>
              )}
            </div>

            {/* Trạng thái hoạt động (chỉ hiển thị khi edit) */}
            {isEdit && (
              <div className="flex items-center justify-between">
                <Label htmlFor="isActive">Đang hoạt động</Label>
                <Switch
                  id="isActive"
                  checked={isActive}
                  onCheckedChange={(checked) =>
                    setValue('isActive', checked, { shouldValidate: true })
                  }
                  disabled={isLoading}
                />
              </div>
            )}
          </div>

          <DialogFooter className="mt-4">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isLoading}
            >
              Hủy
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? 'Đang xử lý...'
                : isEdit
                ? 'Cập nhật'
                : 'Thêm mới'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};