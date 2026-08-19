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
import { toast } from 'sonner';
import { Copy, Check, AlertTriangle, Key } from 'lucide-react';
import type { PartnerApiKeyResponse } from '@/types/apiKey';

interface RawApiKeyModalProps {
  open: boolean;
  apiKeyData: PartnerApiKeyResponse | null;
  onClose: () => void;
}

export const RawApiKeyModal: React.FC<RawApiKeyModalProps> = ({
  open,
  apiKeyData,
  onClose,
}) => {
  const [copied, setCopied] = useState(false);

  if (!apiKeyData || !apiKeyData.rawApiKey) {
    return null;
  }

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(apiKeyData.rawApiKey || '');
      setCopied(true);
      toast.success('Đã sao chép khóa API vào khay nhớ tạm!');
      setTimeout(() => setCopied(false), 3000);
    } catch (err) {
      toast.error('Không thể sao chép tự động. Vui lòng chọn và sao chép thủ công.');
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-md border-amber-200 dark:border-amber-900">
        <DialogHeader>
          <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-semibold mb-1">
            <Key className="w-5 h-5" />
            <span>Khóa truy cập đã được tạo thành công!</span>
          </div>
          <DialogTitle className="text-xl">Khóa API của {apiKeyData.partnerName}</DialogTitle>
          <DialogDescription className="text-muted-foreground pt-1">
            Dưới đây là Khóa bản rõ dùng cho bên thứ ba tích hợp hệ thống.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Cảnh báo chỉ hiển thị 1 lần */}
          <div className="flex items-start gap-3 p-3.5 rounded-lg bg-amber-50 dark:bg-amber-950/50 border border-amber-200 dark:border-amber-800 text-amber-800 dark:text-amber-200 text-xs sm:text-sm">
            <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold block mb-0.5">QUAN TRỌNG: Hãy lưu lại khóa này ngay!</span>
              Khóa bản rõ này <strong>chỉ hiển thị duy nhất 1 lần lúc tạo</strong>. Sau khi đóng cửa sổ này, bạn sẽ không thể xem lại khóa nữa vì lý do bảo mật.
            </div>
          </div>

          {/* Ô hiển thị Key với nút Copy */}
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-muted-foreground">Khóa API (Header X-API-KEY):</label>
            <div className="flex items-center gap-2 p-2.5 rounded-lg bg-slate-900 text-slate-100 font-mono text-sm break-all select-all border border-slate-800">
              <span className="flex-1 font-mono text-emerald-400 tracking-tight">{apiKeyData.rawApiKey}</span>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                className="shrink-0 gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white border-none"
                onClick={handleCopy}
              >
                {copied ? (
                  <>
                    <Check className="w-4 h-4" />
                    <span>Đã chép</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4" />
                    <span>Sao chép</span>
                  </>
                )}
              </Button>
            </div>
          </div>

          {/* Chi tiết hạn mức & thời hạn */}
          <div className="grid grid-cols-2 gap-2 p-3 rounded-lg bg-muted/40 text-xs">
            <div>
              <span className="text-muted-foreground block">Hạn mức gọi:</span>
              <span className="font-semibold text-foreground">{apiKeyData.rateLimitPerHour} lượt / giờ</span>
            </div>
            <div>
              <span className="text-muted-foreground block">Hết hạn vào:</span>
              <span className="font-semibold text-foreground">
                {new Date(apiKeyData.expiresAt).toLocaleDateString('vi-VN')} {new Date(apiKeyData.expiresAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button type="button" variant="default" className="w-full sm:w-auto" onClick={onClose}>
            Tôi đã lưu khóa, Đóng cửa sổ
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
