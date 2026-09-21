import { AlertTriangle, Lock } from 'lucide-react';

import { useLanguage } from '@/context/LanguageContext';

interface LockAlertProps {
  lockReason: string | null;
  lockedAt: string | null;
}

/**
 * Cảnh báo mã tem bị khóa do nghi ngờ bất thường/giả mạo.
 */
export function LockAlert({ lockReason, lockedAt }: LockAlertProps) {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  const formatDateTime = (value: string | null) => {
    if (!value) {
      return '';
    }
    return new Date(value).toLocaleString(isEn ? 'en-US' : 'vi-VN', {
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
            {isEn ? 'Warning: Suspected Counterfeit Code Locked' : 'Cảnh báo: Mã tem bị nghi ngờ giả mạo'}
          </h2>
          <p className="text-sm text-red-700">
            {isEn
              ? 'This trace code was locked by Platform Administrator due to anomaly detection.'
              : 'Mã tem này đã bị khóa bởi Quản trị viên nền tảng do có dấu hiệu bất thường.'}
          </p>
          {lockReason && (
            <div className="rounded-lg border border-red-300 bg-red-100/50 p-3">
              <div className="flex items-start gap-2">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
                <div>
                  <p className="text-xs font-semibold text-red-800 flex items-center gap-1">
                    <span>{t('reason_label')}</span>
                    {isEn && (
                      <span
                        className="inline-flex items-center rounded-md bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-700 ring-1 ring-inset ring-amber-600/20"
                        title={t('original_badge_tooltip')}
                      >
                        [{t('original_badge')}]
                      </span>
                    )}
                  </p>
                  <p className="text-sm text-red-700">{lockReason}</p>
                </div>
              </div>
            </div>
          )}
          {lockedAt && (
            <p className="text-xs text-red-500">
              {t('locked_at_label')} {formatDateTime(lockedAt)}
            </p>
          )}
        </div>
      </div>
    </section>
  );
}

export default LockAlert;
