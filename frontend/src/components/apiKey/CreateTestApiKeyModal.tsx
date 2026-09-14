import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { FlaskConical, AlertCircle, Sparkles } from 'lucide-react';
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
import { createTestApiKey } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse } from '@/types/apiKey';

const testApiKeySchema = z.object({
  partnerName: z
    .string()
    .trim()
    .min(1, 'Vui lòng nhập tên đối tác hoặc đơn vị thử nghiệm')
    .max(255, 'Tên đối tác không được vượt quá 255 ký tự'),
  expireDays: z
    .number({ invalid_type_error: 'Vui lòng nhập số ngày hợp lệ' })
    .int('Số ngày phải là số nguyên')
    .min(1, 'Thời hạn tối thiểu là 1 ngày')
    .max(30, 'Thời hạn thử nghiệm tối đa là 30 ngày theo quy định'),
  rateLimitPerHour: z
    .number({ invalid_type_error: 'Vui lòng nhập hạn mức gọi' })
    .int('Hạn mức phải là số nguyên')
    .min(1, 'Hạn mức tối thiểu là 1 lượt/giờ')
    .max(100, 'Hạn mức thử nghiệm tối đa là 100 lượt/giờ'),
});

type TestApiKeyFormValues = z.infer<typeof testApiKeySchema>;

interface CreateTestApiKeyModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (keyData: PartnerApiKeyResponse) => void;
}

/**
 * Modal cấp khóa thử nghiệm (Sandbox API Key) cho bên thứ ba (NCL-12-CN-004)
 * Áp dụng 100% design system: Dialog, Button, Input, Sonner toast, Lucide icons.
 */
export const CreateTestApiKeyModal: React.FC<CreateTestApiKeyModalProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<TestApiKeyFormValues>({
    resolver: zodResolver(testApiKeySchema),
    defaultValues: {
      partnerName: '',
      expireDays: 14,
      rateLimitPerHour: 100,
    },
  });

  const currentExpireDays = watch('expireDays');

  useEffect(() => {
    if (open) {
      reset({
        partnerName: '',
        expireDays: 14,
        rateLimitPerHour: 100,
      });
    }
  }, [open, reset]);

  const onSubmit = async (values: TestApiKeyFormValues) => {
    try {
      setSubmitting(true);
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + values.expireDays);

      const createdKey = await createTestApiKey({
        partnerName: values.partnerName,
        rateLimitPerHour: values.rateLimitPerHour,
        expireDays: values.expireDays,
        expiresAt: futureDate.toISOString(),
      });

      toast.success('Cấp khóa thử nghiệm thành công!');
      onSuccess(createdKey);
      onClose();
    } catch (error: any) {
      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        'Không thể cấp khóa thử nghiệm. Vui lòng kiểm tra lại quyền hạn.';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary font-semibold text-sm">
            <FlaskConical className="w-4 h-4" />
            <span>Môi trường Thử nghiệm (Sandbox)</span>
          </div>
          <DialogTitle className="text-xl">Cấp khóa API thử nghiệm</DialogTitle>
          <DialogDescription>
            Khóa thử nghiệm (Sandbox Key) cho phép đối tác bên thứ ba kết nối và kiểm thử API
            truy xuất nguồn gốc với dữ liệu mẫu chuẩn hóa, an toàn tuyệt đối.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4 py-2">
          {/* Hộp thông tin chế độ thử nghiệm */}
          <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/60 border border-border text-xs text-muted-foreground">
            <Sparkles className="w-4 h-4 text-primary shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="font-semibold text-foreground block">
                Đặc quyền Sandbox (is_test=true):
              </span>
              <p>
                Khóa này có tiền tố <code className="font-mono text-primary font-bold">nks_test_</code>. Mọi truy vấn lấy lô sản xuất sẽ trả về dữ liệu mẫu mô phỏng chuẩn GS1, không làm thay đổi hay để lộ dữ liệu thật.
              </p>
            </div>
          </div>

          {/* Tên đối tác */}
          <div className="space-y-1.5">
            <Label htmlFor="test-partner-name" className="text-xs font-medium">
              Tên đối tác / Kỹ thuật viên kết nối <span className="text-destructive">*</span>
            </Label>
            <Input
              id="test-partner-name"
              placeholder="VD: Đội kỹ thuật Big C / Phần mềm ERP đối tác"
              {...register('partnerName')}
              disabled={submitting}
              autoFocus
            />
            {errors.partnerName && (
              <p className="text-xs text-destructive flex items-center gap-1 mt-1">
                <AlertCircle className="w-3 h-3" />
                {errors.partnerName.message}
              </p>
            )}
          </div>

          {/* Thời hạn (ngày) */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <Label htmlFor="test-expire-days" className="text-xs font-medium">
                Thời hạn hiệu lực (ngày) <span className="text-destructive">*</span>
              </Label>
              <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <span>Chọn nhanh:</span>
                {[7, 14, 30].map((days) => (
                  <button
                    key={days}
                    type="button"
                    onClick={() => setValue('expireDays', days, { shouldValidate: true })}
                    className={`px-1.5 py-0.5 rounded border text-[11px] font-medium transition-colors ${
                      currentExpireDays === days
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'bg-muted hover:bg-muted/80 border-border text-foreground'
                    }`}
                  >
                    {days} ngày
                  </button>
                ))}
              </div>
            </div>
            <Input
              id="test-expire-days"
              type="number"
              min={1}
              max={30}
              {...register('expireDays', { valueAsNumber: true })}
              disabled={submitting}
            />
            {errors.expireDays ? (
              <p className="text-xs text-destructive flex items-center gap-1 mt-1">
                <AlertCircle className="w-3 h-3" />
                {errors.expireDays.message}
              </p>
            ) : (
              <p className="text-[11px] text-muted-foreground">
                Tối đa 30 ngày theo quy định bảo mật thử nghiệm.
              </p>
            )}
          </div>

          {/* Hạn mức số lượt gọi */}
          <div className="space-y-1.5">
            <Label htmlFor="test-rate-limit" className="text-xs font-medium">
              Hạn mức gọi API (lượt/giờ) <span className="text-destructive">*</span>
            </Label>
            <Input
              id="test-rate-limit"
              type="number"
              min={1}
              max={100}
              {...register('rateLimitPerHour', { valueAsNumber: true })}
              disabled={submitting}
            />
            {errors.rateLimitPerHour ? (
              <p className="text-xs text-destructive flex items-center gap-1 mt-1">
                <AlertCircle className="w-3 h-3" />
                {errors.rateLimitPerHour.message}
              </p>
            ) : (
              <p className="text-[11px] text-muted-foreground">
                Khóa thử nghiệm giới hạn tối đa 100 lượt/giờ.
              </p>
            )}
          </div>

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={submitting}
            >
              Hủy bỏ
            </Button>
            <Button
              type="submit"
              variant="create"
              disabled={submitting}
              className="gap-2"
            >
              <FlaskConical className="w-4 h-4" />
              <span>{submitting ? 'Đang cấp khóa...' : 'Cấp khóa thử nghiệm'}</span>
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default CreateTestApiKeyModal;
