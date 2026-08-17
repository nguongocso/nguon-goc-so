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
import { ShieldX, Loader2 } from 'lucide-react';
import { revokeApiKey } from '@/api/apiKeyApi';
import type { PartnerApiKeyResponse } from '@/types/apiKey';

interface RevokeApiKeyDialogProps {
  open: boolean;
  apiKeyData: PartnerApiKeyResponse | null;
  onClose: () => void;
  onSuccess: (revokedKey: PartnerApiKeyResponse) => void;
}

export const RevokeApiKeyDialog: React.FC<RevokeApiKeyDialogProps> = ({
  open,
  apiKeyData,
  onClose,
  onSuccess,
}) => {
  const [loading, setLoading] = useState(false);

  if (!apiKeyData) return null;

  const handleRevoke = async () => {
    try {
      setLoading(true);
      const updatedKey = await revokeApiKey(apiKeyData.id);
      toast.success(`Đã thu hồi khóa API của "${apiKeyData.partnerName}" thành công!`);
      onSuccess(updatedKey);
      onClose();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể thu hồi khóa API');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AlertDialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <AlertDialogPopup className="sm:max-w-md">
        <AlertDialogHeader>
          <div className="flex items-center gap-2 text-rose-600 dark:text-rose-400 font-semibold mb-1">
            <ShieldX className="w-5 h-5" />
            <span>Thu hồi khóa truy cập</span>
          </div>
          <AlertDialogTitle className="text-xl">
            Xác nhận thu hồi khóa của "{apiKeyData.partnerName}"?
          </AlertDialogTitle>
          <AlertDialogDescription className="text-muted-foreground pt-1">
            Hành động này sẽ ngắt quyền truy cập ngay lập tức đối với hệ thống của đối tác <strong>{apiKeyData.partnerName}</strong>. 
            Mọi yêu cầu lấy hồ sơ truy xuất sử dụng khóa mã <code>{apiKeyData.keyPrefix}</code> từ bây giờ sẽ bị ngắt kết nối tự động.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <AlertDialogFooter className="mt-4">
          <AlertDialogCancel onClick={onClose} disabled={loading}>
            Hủy bỏ
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleRevoke}
            disabled={loading}
            className="bg-rose-600 hover:bg-rose-700 text-white border-none"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Đang thu hồi...
              </>
            ) : (
              'Thu hồi khóa ngay'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogPopup>
    </AlertDialog>
  );
};
