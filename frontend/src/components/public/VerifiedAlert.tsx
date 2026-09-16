import { CheckCircle2, ShieldCheck } from 'lucide-react';
import { useLanguage } from '@/context/LanguageContext';

interface VerifiedAlertProps {
  verificationNote?: string | null;
  unlockedAt?: string | null;
}

export const VerifiedAlert = ({ verificationNote, unlockedAt }: VerifiedAlertProps) => {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  const formatDateTime = (value: string | null | undefined) => {
    if (!value) return '';
    return new Date(value).toLocaleString(isEn ? 'en-US' : 'vi-VN', {
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
            {isEn ? "Information: Trace Code Verified Safe" : "Thông tin: Mã tem đã được xác minh an toàn"}
          </h2>
          <p className="text-sm text-emerald-700">
            {isEn
              ? "This trace code was inspected and verified safe by Administrator."
              : "Mã tem này đã được Quản trị viên hệ thống kiểm tra, xác minh tính hợp lệ và mở khóa an toàn."}
          </p>
          {verificationNote && (
            <div className="rounded-lg border border-emerald-300 bg-emerald-100/50 p-3">
              <div className="flex items-start gap-2">
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
                <div>
                  <p className="text-xs font-semibold text-emerald-800 flex items-center gap-1">
                    <span>{isEn ? "Verification Note:" : "Kết luận xác minh:"}</span>
                    {isEn && (
                      <span
                        className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-700 ring-1 ring-inset ring-amber-600/20"
                        title={t('original_badge_tooltip')}
                      >
                        [{t('original_badge')}]
                      </span>
                    )}
                  </p>
                  <p className="text-sm text-emerald-700">{verificationNote}</p>
                </div>
              </div>
            </div>
          )}
          {unlockedAt && (
            <p className="text-xs text-emerald-600">
              {isEn ? "Verified At:" : "Thời điểm xác minh:"} {formatDateTime(unlockedAt)}
            </p>
          )}
        </div>
      </div>
    </section>
  );
};
