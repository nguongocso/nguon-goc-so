import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select';
import { getValidCertifications, attachCertification } from '@/api/certificationApi';
import type { Certification, AttachCertificationRequest } from '@/types/certification';

const formSchema = z.object({
  certificationId: z.string().uuid('Vui lòng chọn chứng nhận'),
  note: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface Props {
  open: boolean;
  onClose: () => void;
  lotId: string;
  onSuccess: () => void;
}

export const AttachCertificationDialog = ({ open, onClose, lotId, onSuccess }: Props) => {
  const [certifications, setCertifications] = useState<Certification[]>([]);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      certificationId: '',
      note: '',
    },
  });

  const selectedId = watch('certificationId');
  const selectedCert = certifications.find(c => c.id === selectedId);

  useEffect(() => {
    if (open) {
      const fetchCertifications = async () => {
        try {
          setLoading(true);
          const data = await getValidCertifications();
          setCertifications(data);
          // Nếu có certification mặc định, set nó ở đây (nếu muốn)
        } catch (error: any) {
          toast.error('Không thể tải danh sách chứng nhận');
        } finally {
          setLoading(false);
        }
      };
      fetchCertifications();
    }
  }, [open]);

  const onSubmit = async (values: FormValues) => {
    try {
      const payload: AttachCertificationRequest = {
        certificationId: values.certificationId,
        note: values.note || undefined,
      };
      await attachCertification(lotId, payload);
      toast.success('Gắn chứng nhận thành công');
      onSuccess();
      onClose();
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Có lỗi xảy ra khi gắn chứng nhận';
      toast.error(msg);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Gắn chứng nhận cho lô</DialogTitle>
          <DialogDescription>
            Chọn một chứng nhận còn hiệu lực để gắn vào lô sản xuất.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="certificationId">Chứng nhận *</Label>
              {loading ? (
                <div className="text-sm text-muted-foreground">Đang tải...</div>
              ) : certifications.length === 0 ? (
                <div className="text-sm text-amber-600">Chưa có chứng nhận còn hiệu lực.</div>
              ) : (
                <Select
                  value={selectedId}
                  onValueChange={(value) => setValue('certificationId', value || '')}
                >
                  <SelectTrigger>
                    {/* 👇 Custom hiển thị tên thay vì ID */}
                    <span className="truncate">
                      {selectedCert
                        ? `${selectedCert.name} (${selectedCert.code})`
                        : "Chọn chứng nhận"}
                    </span>
                  </SelectTrigger>
                  <SelectContent>
                    {certifications.map((cert) => (
                      <SelectItem key={cert.id} value={cert.id}>
                        {cert.name} ({cert.code}) - Hết hạn: {new Date(cert.expiryDate).toLocaleDateString('vi-VN')}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
              {errors.certificationId && <p className="text-sm text-red-500">{errors.certificationId.message}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="note">Ghi chú (tùy chọn)</Label>
              <Input id="note" {...register('note')} placeholder="Nhập ghi chú..." />
              {errors.note && <p className="text-sm text-red-500">{errors.note.message}</p>}
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="submit" variant="create" disabled={isSubmitting || !selectedId || loading}>
              {isSubmitting ? 'Đang gắn...' : 'Gắn chứng nhận'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};