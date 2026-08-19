import { Lock, AlertTriangle } from 'lucide-react';

interface LockAlertProps {
  lockReason: string | null;
  lockedAt: string | null;
}

export const LockAlert = ({ lockReason, lockedAt }: LockAlertProps) => {
  const formatDateTime = (value: string | null) => {
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
    <section className="rounded-xl border-2 border-red-400 bg-red-50 p-5 shadow-sm">
      <div className="flex gap-3">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-red-100">
          <Lock className="h-6 w-6 text-red-600" />
        </div>
        <div className="space-y-2">
          <h2 className="text-lg font-bold text-red-800">
            Cảnh báo: Mã tem bị nghi ngờ giả mạo
          </h2>
          <p className="text-sm text-red-700">
            Mã tem này đã bị khóa bởi Quản trị viên nền tảng do có dấu hiệu bất thường.
            Vui lòng liên hệ với cơ quan chức năng hoặc nhà sản xuất để được hỗ trợ.
          </p>
          {lockReason && (
            <div className="rounded-lg border border-red-300 bg-red-100/50 p-3">
              <div className="flex items-start gap-2">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
                <div>
                  <p className="text-xs font-semibold text-red-800">Lý do khóa:</p>
                  <p className="text-sm text-red-700">{lockReason}</p>
                </div>
              </div>
            </div>
          )}
          {lockedAt && (
            <p className="text-xs text-red-500">
              Thời điểm khóa: {formatDateTime(lockedAt)}
            </p>
          )}
        </div>
      </div>
    </section>
  );
};