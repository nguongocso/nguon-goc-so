import React, { useState } from 'react';
import {
  AlertDialog,
  AlertDialogPopup,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogAction,
  AlertDialogCancel,
} from '@/components/ui/alert-dialog';
import { toast } from 'sonner';
import { CalendarPlus, Loader2 } from 'lucide-react';
import { renewApiKey } from '@/api/apiKeyApi';
import { toApiError } from '@/api/apiError';
import type { PartnerApiKeyResponse, RenewApiKeyRequest } from '@/types/apiKey';

interface RenewApiKeyDialogProps {
  open: boolean;
  apiKeyData: PartnerApiKeyResponse | null;
  onClose: () => void;
  onSuccess: (renewedKey: PartnerApiKeyResponse) => void;
}

export const RenewApiKeyDialog: React.FC<RenewApiKeyDialogProps> = ({
  open,
  apiKeyData,
  onClose,
  onSuccess,
}) => {
  const [loading, setLoading] = useState(false);
  const [newExpiry, setNewExpiry] = useState('');

  if (!apiKeyData) return null;

  const handleRenew = async () => {
    try {
      if (!newExpiry) {
        toast.error('Vui lòng chọn ngày hết hạn mới');
        return;
      }
      const newDate = new Date(newExpiry);
      if (isNaN(newDate.getTime()) || newDate <= new Date()) {
        toast.error('Ngày hết hạn mới phải ở thời điểm tương lai');
        return;
      }
      setLoading(true);
      const updatedKey = await renewApiKey(apiKeyData.id, { expiresAt: newExpiry } as RenewApiKeyRequest);
      toast.success(`Đã gia hạn khóa API của "${apiKeyData.partnerName}" thành công!`);
      onSuccess(updatedKey);
      onClose();
    } catch (error: unknown) {
      toast.error(toApiError(error, 'Không thể gia hạn khóa API').message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AlertDialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <AlertDialogPopup className="sm:max-w-md">
        <AlertDialogHeader>
          <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400 font-semibold mb-1">
            <CalendarPlus className="w-5 h-5" />
            <span>Gia hạn khóa truy cập</span>
          </div>
          <AlertDialogTitle className="text-xl">
            Gia hạn khóa của "{apiKeyData.partnerName}"?
          </AlertDialogTitle>
          <AlertDialogDescription className="text-muted-foreground pt-1">
            Khóa <code>{apiKeyData.keyPrefix}</code> hiện hết hạn vào{' '}
            <strong>{apiKeyData.expiresAt ? new Date(apiKeyData.expiresAt).toLocaleString('vi-VN') : '—'}</strong>.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="mt-3">
          <label htmlFor="renew-expiry" className="block text-sm font-medium mb-1">
            Ngày hết hạn mới
          </label>
          <input
            id="renew-expiry"
            type="datetime-local"
            className="w-full border rounded-md px-3 py-2 bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-amber-500/40"
            value={newExpiry}
            onChange={(e) => setNewExpiry(e.target.value)}
          />
        </div>

        <AlertDialogFooter className="mt-4">
          <AlertDialogCancel onClick={onClose} disabled={loading}>
            Hủy bỏ
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleRenew}
            disabled={loading}
            className="bg-amber-600 hover:bg-amber-700 text-white"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Đang gia hạn...
              </>
            ) : (
              'Gia hạn khóa'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};
