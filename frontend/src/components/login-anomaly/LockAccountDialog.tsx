import { useState } from 'react';
import { lockLoginAnomalyAccount } from '@/api/loginAnomalyApi';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import type { LoginAnomaly } from '@/types/loginAnomaly';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface Props {
  anomaly: LoginAnomaly | null;
  onClose: () => void;
  onLocked: () => void;
}

export function LockAccountDialog({ anomaly, onClose, onLocked }: Props) {
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!anomaly || submitting) return;

    try {
      setSubmitting(true);
      await lockLoginAnomalyAccount(anomaly.id);
      toast.success(`Đã khóa tạm tài khoản "${anomaly.username}"`);
      onLocked();
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể khóa tài khoản lúc này',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog
      open={Boolean(anomaly)}
      onOpenChange={(open) => !open && !submitting && onClose()}
    >
      <DialogContent className="sm:max-w-lg" showCloseButton={!submitting}>
        {anomaly && (
          <>
            <DialogHeader>
              <div className="flex items-start gap-3 pr-8">
                <div className="rounded-full bg-red-100 p-2 text-red-700">
                  <AlertTriangle className="h-5 w-5" />
                </div>
                <div className="space-y-1">
                  <DialogTitle>Xác nhận khóa tạm tài khoản</DialogTitle>
                  <DialogDescription>
                    Thao tác này sẽ khóa tạm tài khoản, người dùng sẽ không thể đăng nhập
                    cho đến khi được mở khóa. Hành động sẽ được ghi vào lịch sử hoạt động.
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            <div className="rounded-lg border bg-muted/20 p-3 text-sm">
              <p>
                <span className="text-muted-foreground">Người dùng:</span>{' '}
                <span className="font-medium">{anomaly.fullName}</span>{' '}
                <span className="text-muted-foreground">({anomaly.username})</span>
              </p>
              <p className="mt-1">
                <span className="text-muted-foreground">Tổ chức:</span> {anomaly.organizationName}
              </p>
              <p className="mt-1">
                <span className="text-muted-foreground">Nguyên nhân:</span> {anomaly.reason}
              </p>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={onClose} disabled={submitting}>
                Hủy
              </Button>
              <Button variant="delete" onClick={handleSubmit} disabled={submitting}>
                {submitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Đang khóa...
                  </>
                ) : (
                  'Khóa tài khoản'
                )}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
