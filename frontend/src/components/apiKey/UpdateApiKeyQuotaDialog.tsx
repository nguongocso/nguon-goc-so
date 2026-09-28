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
import { TrendingUp, Loader2 } from 'lucide-react';
import { updateApiKeyQuota } from '@/api/apiKeyApi';
import { toApiError } from '@/api/apiError';
import type { PartnerApiKeyResponse, UpdateApiKeyQuotaRequest } from '@/types/apiKey';

interface UpdateApiKeyQuotaDialogProps {
  open: boolean;
  apiKeyData: PartnerApiKeyResponse | null;
  onClose: () => void;
  onSuccess: (updatedKey: PartnerApiKeyResponse) => void;
}

export const UpdateApiKeyQuotaDialog: React.FC<UpdateApiKeyQuotaDialogProps> = ({
  open,
  apiKeyData,
  onClose,
  onSuccess,
}) => {
  const [loading, setLoading] = useState(false);
  const [incrementBy, setIncrementBy] = useState<number | string>('');

  if (!apiKeyData) return null;

  const currentQuota = apiKeyData.rateLimitPerHour || 0;
  const incrementValue = Number(incrementBy);
  const hasValidIncrement = incrementBy !== '' && !isNaN(incrementValue) && incrementValue > 0;
  const previewQuota = hasValidIncrement ? currentQuota + incrementValue : currentQuota;

  const handleUpdate = async () => {
    try {
      if (!hasValidIncrement) {
        toast.error('Số lượt hạn mức bổ sung phải lớn hơn 0');
        return;
      }
      setLoading(true);
      const updatedKey = await updateApiKeyQuota(apiKeyData.id, { incrementBy: incrementValue } as UpdateApiKeyQuotaRequest);
      toast.success(`Đã nâng hạn mức cho khóa của "${apiKeyData.partnerName}" lên ${updatedKey.rateLimitPerHour} lượt/giờ!`);
      onSuccess(updatedKey);
      onClose();
    } catch (error: unknown) {
      toast.error(toApiError(error, 'Không thể nâng hạn mức khóa API').message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AlertDialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <AlertDialogPopup className="sm:max-w-md">
        <AlertDialogHeader>
          <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400 font-semibold mb-1">
            <TrendingUp className="w-5 h-5" />
            <span>Nâng hạn mức khóa truy cập</span>
          </div>
          <AlertDialogTitle className="text-xl">
            Nâng hạn mức cho khóa của "{apiKeyData.partnerName}"?
          </AlertDialogTitle>
          <AlertDialogDescription className="text-muted-foreground pt-1">
            Hạn mức hiện tại: <strong>{apiKeyData.rateLimitPerHour} lượt/giờ</strong>.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="mt-3">
          <label htmlFor="update-quota" className="block text-sm font-medium mb-1">
            Số lượt hạn mức bổ sung (lượt/giờ)
          </label>
          <input
            id="update-quota"
            type="number"
            min={1}
            className="w-full border rounded-md px-3 py-2 bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-emerald-500/40"
            value={incrementBy}
            onChange={(e) => setIncrementBy(e.target.value)}
            placeholder="Ví dụ: 50"
          />
          <p className="mt-2 text-sm text-muted-foreground">
            Hạn mức sau nâng: <strong className="text-foreground">{previewQuota} lượt/giờ</strong>
            {hasValidIncrement && (
              <span> ({currentQuota} + {incrementValue})</span>
            )}
          </p>
        </div>

        <AlertDialogFooter className="mt-4">
          <AlertDialogCancel onClick={onClose} disabled={loading}>
            Hủy bỏ
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleUpdate}
            disabled={loading}
            className="bg-emerald-600 hover:bg-emerald-700 text-white"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Đang cập nhật...
              </>
            ) : (
              'Nâng hạn mức'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};
