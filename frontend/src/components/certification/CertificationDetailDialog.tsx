import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import type { CertificationResponse } from '@/types/certification';
import { CertificationStatusBadge } from '@/components/certification/CertificationStatusBadge';
import { DetailField } from '@/components/common/detail/DetailField';
import { CalendarDays, FileBadge, Building2, Hash } from 'lucide-react';

interface Props {
  certification: CertificationResponse | null;
  open: boolean;
  onClose: () => void;
}

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr + 'T00:00:00').toLocaleDateString('vi-VN', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return dateStr;
  }
};

const getDaysRemaining = (expiryDate: string, isValid: boolean) => {
  if (!isValid) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(expiryDate + 'T00:00:00');
  return Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
};

export const CertificationDetailDialog = ({ certification, open, onClose }: Props) => {
  if (!certification) return null;

  const daysRemaining = getDaysRemaining(certification.expiryDate, certification.isValid);

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold text-foreground">
            {certification.name}
          </DialogTitle>
          <DialogDescription>
            Thông tin chi tiết chứng nhận
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Status */}
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-muted-foreground">Trạng thái:</span>
            <CertificationStatusBadge
              isValid={certification.isValid}
              expiryDate={certification.expiryDate}
            />
          </div>

          {/* Info grid */}
          <div className="grid gap-4">
            <DetailField
              icon={<Hash className="h-4 w-4 text-muted-foreground" />}
              label="Mã chứng nhận"
              value={certification.code}
            />
            <DetailField
              icon={<FileBadge className="h-4 w-4 text-muted-foreground" />}
              label="Tên chứng nhận"
              value={certification.name}
            />
            <DetailField
              icon={<Building2 className="h-4 w-4 text-muted-foreground" />}
              label="Cơ quan cấp"
              value={certification.issuedBy || undefined}
            />
            <DetailField
              icon={<CalendarDays className="h-4 w-4 text-muted-foreground" />}
              label="Ngày cấp"
              value={formatDate(certification.issueDate)}
            />
            <DetailField
              icon={<CalendarDays className="h-4 w-4 text-destructive" />}
              label="Ngày hết hạn"
              value={formatDate(certification.expiryDate)}
            />
          </div>

          {/* Expiration warning */}
          {daysRemaining !== null && daysRemaining <= 30 && (
            <div className="rounded-lg border border-warning/30 bg-warning-bg p-4">
              <p className="text-sm font-medium text-warning">
                ⚠️ Chứng nhận sẽ hết hạn trong {daysRemaining} ngày
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Vui lòng gia hạn chứng nhận trước khi hết hạn để duy trì hiệu lực.
              </p>
            </div>
          )}

          {!certification.isValid && (
            <div className="rounded-lg border border-destructive/30 bg-error-bg p-4">
              <p className="text-sm font-medium text-destructive">
                ❌ Chứng nhận đã hết hạn
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                Chứng nhận này không còn hiệu lực và không thể gắn cho lô sản xuất.
              </p>
            </div>
          )}

          {/* Metadata */}
          <div className="border-t border-border pt-4">
            <p className="text-xs text-muted-foreground">
              ID: {certification.id}
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};
