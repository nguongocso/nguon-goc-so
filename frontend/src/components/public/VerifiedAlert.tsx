import { CheckCircle2, ShieldCheck } from 'lucide-react';

interface VerifiedAlertProps {
  verificationNote?: string | null;
  unlockedAt?: string | null;
}

export const VerifiedAlert = ({ verificationNote, unlockedAt }: VerifiedAlertProps) => {
  const formatDateTime = (value: string | null | undefined) => {
    if (!value) return '';
    return new Date(value).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <section className="rounded-xl border-2 border-emerald-400 bg-emerald-50 p-5 shadow-sm">
      <div className="flex gap-3">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-emerald-100">
          <ShieldCheck className="h-6 w-6 text-emerald-600" />
        </div>
        <div className="space-y-2">
          <h2 className="text-lg font-bold text-emerald-800">
            Thông tin: Mã tem đã được xác minh an toàn
          </h2>
          <p className="text-sm text-emerald-700">
            Mã tem này đã được Quản trị viên hệ thống kiểm tra, xác minh tính hợp lệ và mở khóa an toàn.
          </p>
          {verificationNote && (
            <div className="rounded-lg border border-emerald-300 bg-emerald-100/50 p-3">
              <div className="flex items-start gap-2">
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
                <div>
                  <p className="text-xs font-semibold text-emerald-800">Kết luận xác minh:</p>
                  <p className="text-sm text-emerald-700">{verificationNote}</p>
                </div>
              </div>
            </div>
          )}
          {unlockedAt && (
            <p className="text-xs text-emerald-600">
              Thời điểm xác minh: {formatDateTime(unlockedAt)}
            </p>
          )}
        </div>
      </div>
    </section>
  );
};
