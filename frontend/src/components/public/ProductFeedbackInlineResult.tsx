import {
  LoaderCircle,
  RefreshCw,
  TriangleAlert,
  X,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { useLanguage } from '@/context/LanguageContext';
import type { translations } from '@/i18n/translations';
import type {
  ProductFeedbackStatus,
  PublicProductFeedbackLookupResult,
} from '@/types/productFeedback';

export type LookupErrorKind = 'not-found' | 'rate-limit' | 'system';

type TranslationKey = keyof typeof translations.vi;

const STATUS_META: Record<
  ProductFeedbackStatus,
  {
    labelKey: TranslationKey;
    descKey: TranslationKey;
    className: string;
  }
> = {
  NEW: {
    labelKey: 'feedback_status_NEW_label',
    descKey: 'feedback_status_NEW_desc',
    className: 'border-sky-200 bg-sky-50 text-sky-700',
  },
  IN_PROGRESS: {
    labelKey: 'feedback_status_IN_PROGRESS_label',
    descKey: 'feedback_status_IN_PROGRESS_desc',
    className: 'border-amber-200 bg-amber-50 text-amber-700',
  },
  ESCALATED_TO_RECALL: {
    labelKey: 'feedback_status_ESCALATED_TO_RECALL_label',
    descKey: 'feedback_status_ESCALATED_TO_RECALL_desc',
    className: 'border-orange-200 bg-orange-50 text-orange-700',
  },
  CLOSED: {
    labelKey: 'feedback_status_CLOSED_label',
    descKey: 'feedback_status_CLOSED_desc',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  },
};

const ERROR_META: Record<
  LookupErrorKind,
  {
    titleKey: TranslationKey;
    msgKey: TranslationKey;
  }
> = {
  'not-found': {
    titleKey: 'feedback_error_not_found_title',
    msgKey: 'feedback_error_not_found_desc',
  },
  'rate-limit': {
    titleKey: 'feedback_error_rate_limit_title',
    msgKey: 'feedback_error_rate_limit_desc',
  },
  system: {
    titleKey: 'feedback_error_system_title',
    msgKey: 'feedback_error_system_desc',
  },
};

interface ProductFeedbackInlineResultProps {
  isLoading: boolean;
  lookupCode: string;
  result: PublicProductFeedbackLookupResult | null;
  errorKind: LookupErrorKind | null;
  onReset: () => void;
  onRetry?: () => void;
}

/**
 * Hiển thị trực tiếp kết quả hoặc lỗi tra cứu phản ánh ngay bên dưới ô tìm kiếm trên trang chủ hoặc trong modal.
 * Tự động chuyển đổi ngôn ngữ Việt / Anh theo LanguageContext.
 */
export function ProductFeedbackInlineResult({
  isLoading,
  lookupCode,
  result,
  errorKind,
  onReset,
  onRetry,
}: ProductFeedbackInlineResultProps) {
  const { t } = useLanguage();

  if (isLoading) {
    return (
      <div className="rounded-2xl border border-emerald-100 bg-emerald-50/50 p-5 text-center" aria-live="polite">
        <div className="flex items-center justify-center gap-2 text-emerald-700 font-medium text-sm">
          <LoaderCircle className="h-5 w-5 animate-spin" />
          <span>
            {t('feedback_result_searching').replace(
              '{code}',
              lookupCode ? `(${lookupCode})` : '',
            )}
          </span>
        </div>
      </div>
    );
  }

  if (result) {
    const statusMeta = STATUS_META[result.status];

    return (
      <div className="rounded-2xl border border-emerald-100 bg-white/90 p-5 shadow-md shadow-emerald-100/50 text-left animate-in fade-in-50 duration-200" aria-live="polite">
        <div className="flex items-center justify-between gap-2 border-b border-slate-100 pb-3">
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              {t('feedback_result_title')}
            </p>
            <p className="font-mono text-sm font-bold text-emerald-800 break-all">{lookupCode}</p>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onReset}
            className="h-8 w-8 p-0 text-slate-400 hover:text-slate-600 rounded-full"
            title={t('feedback_result_close')}
          >
            <X className="h-4 w-4" />
            <span className="sr-only">{t('feedback_result_close')}</span>
          </Button>
        </div>

        <div className="mt-4">
          <p className="text-xs font-medium text-slate-500">{t('feedback_result_status_label')}</p>
          <div className={`mt-1.5 inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${statusMeta.className}`}>
            {t(statusMeta.labelKey)}
          </div>
          <p className="mt-2 text-xs leading-5 text-slate-600">{t(statusMeta.descKey)}</p>
        </div>

        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50/80 p-3.5">
          <p className="text-xs font-semibold text-slate-800">{t('feedback_result_public_response')}</p>
          <p className="mt-1.5 whitespace-pre-wrap text-xs leading-5 text-slate-600">
            {result.publicResponse?.trim() || t('feedback_result_no_response')}
          </p>
        </div>

        <div className="mt-4 flex gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onReset}
            className="w-full gap-1.5 border-emerald-200 text-emerald-700 hover:bg-emerald-50 text-xs h-9"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            {t('feedback_result_search_another')}
          </Button>
        </div>
      </div>
    );
  }

  if (errorKind) {
    const errorMeta = ERROR_META[errorKind];

    return (
      <div className="rounded-2xl border border-red-200 bg-red-50/70 p-4 text-left animate-in fade-in-50 duration-200" role="alert">
        <div className="flex gap-3">
          <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-sm text-slate-900">{t(errorMeta.titleKey)}</p>
            <p className="mt-1 text-xs leading-5 text-slate-600">{t(errorMeta.msgKey)}</p>
          </div>
        </div>

        <div className="mt-3 flex gap-2">
          {errorKind === 'system' && onRetry ? (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onRetry}
              className="flex-1 gap-1.5 border-red-300 text-red-700 hover:bg-red-100 text-xs h-8"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              {t('feedback_result_retry')}
            </Button>
          ) : null}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onReset}
            className="flex-1 gap-1.5 border-slate-200 text-slate-700 hover:bg-white text-xs h-8"
          >
            {t('feedback_result_close_btn')}
          </Button>
        </div>
      </div>
    );
  }

  return null;
}

export default ProductFeedbackInlineResult;
