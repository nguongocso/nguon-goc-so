import React, { useState } from 'react';
import {
  AlertTriangle,
  Check,
  ClipboardCopy,
  Link as LinkIcon,
  LoaderCircle,
  Mail,
  Send,
  ShieldAlert,
} from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { issueInspectionResultEntryLink } from '@/api/inspectionResultPortalApi';
import type { InspectionResultEntryLinkResponse } from '@/types/inspectionResultPortal';

export interface IssueInspectionResultLinkDialogProps {
  requestId: string;
  testingUnitName: string;
  defaultEmail?: string;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (link: InspectionResultEntryLinkResponse) => void;
}

export const IssueInspectionResultLinkDialog: React.FC<IssueInspectionResultLinkDialogProps> = ({
  requestId,
  testingUnitName,
  defaultEmail = '',
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [recipientEmail, setRecipientEmail] = useState(defaultEmail);
  const [expiryDays, setExpiryDays] = useState<number>(7);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [issuedLink, setIssuedLink] = useState<InspectionResultEntryLinkResponse | null>(null);
  const [copied, setCopied] = useState(false);

  // Reset form khi mở lại
  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setIssuedLink(null);
      setCopied(false);
      onClose();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recipientEmail.trim()) {
      toast.error('Vui lòng nhập địa chỉ email của đơn vị kiểm nghiệm.');
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await issueInspectionResultEntryLink(requestId, {
        recipientEmail: recipientEmail.trim(),
        expiryDays: Number(expiryDays) || 7,
      });

      setIssuedLink(res);
      toast.success('Cấp liên kết nhập kết quả thành công và đã gửi email.');
      if (onSuccess) {
        onSuccess(res);
      }
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            'Không thể cấp liên kết nhập kết quả.';
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopyLink = async () => {
    if (!issuedLink?.entryUrl) return;
    try {
      await navigator.clipboard.writeText(issuedLink.entryUrl);
      setCopied(true);
      toast.success('Đã sao chép liên kết vào bộ nhớ tạm.');
      setTimeout(() => setCopied(false), 3000);
    } catch {
      toast.error('Không thể sao chép liên kết, vui lòng bôi đen và sao chép thủ công.');
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        {!issuedLink ? (
          <form onSubmit={handleSubmit}>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2 text-lg">
                <LinkIcon className="h-5 w-5 text-emerald-600" />
                Cấp liên kết nhập kết quả cho đơn vị kiểm nghiệm
              </DialogTitle>
              <DialogDescription>
                Hệ thống sẽ tạo một liên kết tự động và bảo mật gửi tới đơn vị kiểm nghiệm{' '}
                <strong>{testingUnitName}</strong> để họ trực tiếp khai báo kết quả.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4 text-sm">
              <div className="space-y-1.5">
                <Label htmlFor="recipientEmail" className="font-medium text-foreground">
                  Email đại diện đơn vị kiểm nghiệm <span className="text-rose-500">*</span>
                </Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="recipientEmail"
                    type="email"
                    required
                    placeholder="kiemnghiem@donvi.vn"
                    value={recipientEmail}
                    onChange={(e) => setRecipientEmail(e.target.value)}
                    className="pl-9"
                  />
                </div>
                <p className="text-xs text-muted-foreground">
                  Hệ thống sẽ gửi email chứa đường link truy cập trực tiếp đến địa chỉ này.
                </p>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="expiryDays" className="font-medium text-foreground">
                  Thời hạn hiệu lực của liên kết (ngày) <span className="text-rose-500">*</span>
                </Label>
                <Input
                  id="expiryDays"
                  type="number"
                  min={1}
                  max={30}
                  required
                  value={expiryDays}
                  onChange={(e) => setExpiryDays(Number(e.target.value))}
                />
                <p className="text-xs text-muted-foreground">
                  Mặc định 7 ngày (tối đa 30 ngày). Sau thời hạn này liên kết sẽ tự động hết hiệu lực.
                </p>
              </div>

              <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/40 rounded-lg flex items-start gap-2.5 text-xs text-amber-800 dark:text-amber-300">
                <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
                <span>
                  Nếu trước đó đã có liên kết đang hoạt động, việc cấp liên kết mới sẽ{' '}
                  <strong>tự động thu hồi liên kết cũ</strong>. Mỗi yêu cầu chỉ có duy nhất 1 liên kết
                  có hiệu lực tại một thời điểm.
                </span>
              </div>
            </div>

            <DialogFooter className="gap-2 sm:gap-0">
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                onClick={() => handleOpenChange(false)}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {isSubmitting ? (
                  <>
                    <LoaderCircle className="h-4 w-4 animate-spin mr-2" />
                    Đang tạo liên kết...
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4 mr-2" />
                    Cấp và gửi liên kết
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        ) : (
          <div>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2 text-lg text-emerald-700 dark:text-emerald-400">
                <Check className="h-5 w-5" />
                Cấp liên kết thành công!
              </DialogTitle>
              <DialogDescription>
                Email đã được gửi đến <strong>{issuedLink.recipientEmail}</strong>. Bạn cũng có thể
                sao chép đường dẫn trực tiếp dưới đây để gửi cho đơn vị kiểm nghiệm.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 py-4 text-sm">
              <div className="space-y-1.5">
                <Label className="font-medium text-foreground">Đường dẫn cổng nhập kết quả</Label>
                <div className="flex items-center gap-2">
                  <Input
                    readOnly
                    value={issuedLink.entryUrl || ''}
                    className="font-mono text-xs bg-muted/50 select-all"
                  />
                  <Button
                    type="button"
                    variant={copied ? 'default' : 'outline'}
                    size="sm"
                    onClick={handleCopyLink}
                    className={copied ? 'bg-emerald-600 text-white shrink-0' : 'shrink-0'}
                  >
                    {copied ? (
                      <>
                        <Check className="h-4 w-4 mr-1.5" />
                        Đã chép
                      </>
                    ) : (
                      <>
                        <ClipboardCopy className="h-4 w-4 mr-1.5" />
                        Sao chép
                      </>
                    )}
                  </Button>
                </div>
              </div>

              <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/40 rounded-lg flex items-start gap-2.5 text-xs text-amber-800 dark:text-amber-300">
                <ShieldAlert className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
                <span>
                  <strong>Lưu ý bảo mật:</strong> Đường dẫn chứa mã bảo mật này chỉ hiển thị duy nhất
                  một lần tại màn hình này và sẽ không thể xem lại sau khi đóng cửa sổ.
                </span>
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                className="bg-emerald-600 hover:bg-emerald-700 text-white w-full sm:w-auto"
                onClick={() => handleOpenChange(false)}
              >
                Hoàn tất
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};
