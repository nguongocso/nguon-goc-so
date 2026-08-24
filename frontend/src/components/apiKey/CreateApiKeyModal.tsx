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
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { getLocalDateTimeString } from '@/utils/dateTime';
import { toast } from 'sonner';
import { KeyRound, Loader2, ShieldAlert } from 'lucide-react';
import { createApiKey } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse } from '@/types/apiKey';

interface CreateApiKeyModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (createdKey: PartnerApiKeyResponse) => void;
}

export const CreateApiKeyModal: React.FC<CreateApiKeyModalProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const [partnerName, setPartnerName] = useState('');
  const [rateLimitPerHour, setRateLimitPerHour] = useState<number>(100);

  // Mặc định hết hạn sau 30 ngày (theo giờ local cho input datetime-local)
  const getDefaultExpiry = () => {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return getLocalDateTimeString(d); // YYYY-MM-DDTHH:mm
  };

  const getMinExpiry = () => {
    // Tối thiểu từ thời điểm hiện tại (cộng thêm 5 phút buffer)
    const now = new Date();
    now.setMinutes(now.getMinutes() + 5);
    return getLocalDateTimeString(now);
  };

  const [expiresAt, setExpiresAt] = useState(getDefaultExpiry());
  const [loading, setLoading] = useState(false);

  const resetForm = () => {
    setPartnerName('');
    setRateLimitPerHour(100);
    setExpiresAt(getDefaultExpiry());
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!partnerName.trim()) {
      toast.error('Vui lòng nhập tên đối tác / doanh nghiệp thu mua');
      return;
    }
    if (rateLimitPerHour < 1) {
      toast.error('Hạn mức gọi API phải lớn hơn hoặc bằng 1 lượt/giờ');
      return;
    }

    const selectedExpiryDate = new Date(expiresAt);
    const now = new Date();
    if (isNaN(selectedExpiryDate.getTime()) || selectedExpiryDate <= now) {
      toast.error('Thời gian hết hạn của khóa phải ở thời điểm tương lai');
      return;
    }

    try {
      setLoading(true);
      // Gửi ISO String dạng YYYY-MM-DDTHH:mm:ss theo múi giờ địa phương
      const isoExpiresAt = `${expiresAt}:00`;
      const newKey = await createApiKey({
        partnerName: partnerName.trim(),
        rateLimitPerHour: Number(rateLimitPerHour),
        expiresAt: isoExpiresAt,
      });

      toast.success(`Khóa API cho "${newKey.partnerName}" đã được tạo thành công!`);
      resetForm();
      onSuccess(newKey);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tạo khóa API đối tác');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400 font-semibold mb-1">
              <KeyRound className="w-5 h-5" />
              <span>Cấp khóa truy cập đối tác (Partner API Key)</span>
            </div>
            <DialogTitle className="text-xl">Cấp khóa API cho bên thứ ba</DialogTitle>
            <DialogDescription className="text-muted-foreground pt-1">
              Tạo khóa truy cập cho phép phần mềm doanh nghiệp thu mua tự động lấy hồ sơ truy xuất lô sản xuất.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            {/* Tên đối tác */}
            <div className="space-y-1.5">
              <Label htmlFor="partnerName" className="font-medium">
                Tên đối tác / Doanh nghiệp thu mua <span className="text-rose-500">*</span>
              </Label>
              <Input
                id="partnerName"
                placeholder="VD: Công ty TNHH Thu Mua Nông Sản ABC"
                value={partnerName}
                onChange={(e) => setPartnerName(e.target.value)}
                disabled={loading}
                required
              />
            </div>

            {/* Hạn mức số lượt gọi / giờ */}
            <div className="space-y-1.5">
              <Label htmlFor="rateLimitPerHour" className="font-medium">
                Hạn mức gọi API (Số lượt / giờ) <span className="text-rose-500">*</span>
              </Label>
              <Input
                id="rateLimitPerHour"
                type="number"
                min={1}
                max={100000}
                placeholder="VD: 100"
                value={rateLimitPerHour}
                onChange={(e) => setRateLimitPerHour(Number(e.target.value))}
                disabled={loading}
                required
              />
              <p className="text-xs text-muted-foreground">
                Hệ thống sẽ chặn tự động nếu bên thứ ba gọi vượt quá số lượt này trong 1 giờ.
              </p>
            </div>

            {/* Thời gian hết hạn */}
            <div className="space-y-1.5">
              <Label htmlFor="expiresAt" className="font-medium">
                Thời gian hết hạn khóa <span className="text-rose-500">*</span>
              </Label>
              <div className="relative">
                <Input
                  id="expiresAt"
                  type="datetime-local"
                  min={getMinExpiry()}
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  disabled={loading}
                  required
                />
              </div>
              <p className="text-xs text-muted-foreground">
                Thời gian hết hạn bắt buộc phải ở thời điểm tương lai.
              </p>
            </div>

            {/* Ghi chú bảo mật */}
            <div className="flex items-start gap-2 p-3 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-800 dark:text-blue-200 text-xs border border-blue-200 dark:border-blue-900">
              <ShieldAlert className="w-4 h-4 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" />
              <span>
                Sau khi bấm cấp khóa, hệ thống sẽ hiển thị chuỗi API Key bản rõ 1 lần duy nhất để bạn sao chép gửi cho đối tác.
              </span>
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button type="button" variant="outline" onClick={onClose} disabled={loading}>
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={loading}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Đang cấp khóa...
                </>
              ) : (
                'Cấp khóa mới'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};
