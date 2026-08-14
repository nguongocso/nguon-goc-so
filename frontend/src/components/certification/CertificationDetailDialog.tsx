import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import type { CertificationResponse } from '@/types/certification';
import { CertificationStatusBadge } from '@/components/certification/CertificationStatusBadge';
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
          <DialogTitle className="text-xl font-bold text-[#111827]">
            {certification.name}
          </DialogTitle>
          <DialogDescription className="text-[#6B7280]">
            Thông tin chi tiết chứng nhận
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Status */}
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-[#4B5563]">Trạng thái:</span>
            <CertificationStatusBadge
              isValid={certification.isValid}
              expiryDate={certification.expiryDate}
            />
          </div>

          {/* Info grid */}
          <div className="grid gap-4">
            <DetailRow
              icon={<Hash className="h-4 w-4 text-[#4B5563]" />}
              label="Mã chứng nhận"
              value={certification.code}
            />
            <DetailRow
              icon={<FileBadge className="h-4 w-4 text-[#4B5563]" />}
              label="Tên chứng nhận"
              value={certification.name}
            />
            <DetailRow
              icon={<Building2 className="h-4 w-4 text-[#4B5563]" />}
              label="Cơ quan cấp"
              value={certification.issuedBy || '—'}
            />
            <DetailRow
              icon={<CalendarDays className="h-4 w-4 text-[#4B5563]" />}
              label="Ngày cấp"
              value={formatDate(certification.issueDate)}
            />
            <DetailRow
              icon={<CalendarDays className="h-4 w-4 text-[#D32F2F]" />}
              label="Ngày hết hạn"
              value={formatDate(certification.expiryDate)}
            />
          </div>

          {/* Expiration warning */}
          {daysRemaining !== null && daysRemaining <= 30 && (
            <div className="rounded-lg border border-[#F9A825]/30 bg-[#FFF8E1] p-4">
              <p className="text-sm font-medium text-[#F9A825]">
                ⚠️ Chứng nhận sẽ hết hạn trong {daysRemaining} ngày
              </p>
              <p className="text-xs text-[#6B7280] mt-1">
                Vui lòng gia hạn chứng nhận trước khi hết hạn để duy trì hiệu lực.
              </p>
            </div>
          )}

          {!certification.isValid && (
            <div className="rounded-lg border border-[#D32F2F]/30 bg-[#FFEBEE] p-4">
              <p className="text-sm font-medium text-[#D32F2F]">
                ❌ Chứng nhận đã hết hạn
              </p>
              <p className="text-xs text-[#6B7280] mt-1">
                Chứng nhận này không còn hiệu lực và không thể gắn cho lô sản xuất.
              </p>
            </div>
          )}

          {/* Metadata */}
          <div className="border-t border-[#E5E7EB] pt-4">
            <p className="text-xs text-[#9CA3AF]">
              ID: {certification.id}
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

const DetailRow = ({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) => (
  <div className="flex items-start gap-3">
    <div className="mt-0.5">{icon}</div>
    <div className="flex-1 min-w-0">
      <p className="text-xs font-medium text-[#9CA3AF] uppercase tracking-wider">{label}</p>
      <p className="text-sm text-[#1F2937] font-medium break-words">{value}</p>
    </div>
  </div>
);