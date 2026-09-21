import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { isAxiosError } from 'axios';
import { CheckCircle2, Copy, MessageSquareWarning, Search, Send } from 'lucide-react';
import { toast } from 'sonner';

import { createProductFeedback } from '@/api/productFeedbackApi';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { useLanguage } from '@/context/LanguageContext';
import type { PublicProductFeedbackCreated } from '@/types/productFeedback';
import { ProductFeedbackLookupDialog } from './ProductFeedbackLookupDialog';

interface ProductFeedbackFormProps {
  productionLotId: string;
  productName: string;
  traceCodeValue?: string;
}

interface ProductFeedbackFormValues {
  content: string;
}

/**
 * Form gửi phản ánh chất lượng sản phẩm từ trang tra cứu công khai.
 */
export function ProductFeedbackForm({
  productionLotId,
  productName,
  traceCodeValue,
}: ProductFeedbackFormProps) {
  const { t } = useLanguage();
  const [submittedFeedback, setSubmittedFeedback] =
    useState<PublicProductFeedbackCreated | null>(null);
  const [isLookupDialogOpen, setIsLookupDialogOpen] = useState(false);
  const [lookupInitialCode, setLookupInitialCode] = useState<string | undefined>(
    undefined,
  );
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ProductFeedbackFormValues>({
    defaultValues: { content: '' },
  });

  const contentLength = watch('content')?.length ?? 0;

  const onSubmit = async ({ content }: ProductFeedbackFormValues) => {
    try {
      const createdFeedback = await createProductFeedback(productionLotId, {
        content: content.trim(),
        traceCodeValue,
      });

      reset();
      setSubmittedFeedback(createdFeedback);
      toast.success(t('feedback_toast_success'));
    } catch (error: unknown) {
      const message = isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message ??
          (error.response
            ? t('feedback_error_submit')
            : t('feedback_error_network'))
        : t('feedback_error_generic');

      toast.error(message);
    }
  };

  const copyLookupCode = async () => {
    if (!submittedFeedback?.lookupCode || !navigator.clipboard) {
      toast.error(t('feedback_toast_copy_error'));
      return;
    }

    try {
      await navigator.clipboard.writeText(submittedFeedback.lookupCode);
      toast.success(t('feedback_toast_copy_success'));
    } catch {
      toast.error(t('feedback_toast_copy_error'));
    }
  };

  if (submittedFeedback) {
    return (
      <section className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-5 shadow-sm">
        <div className="flex gap-3">
          <CheckCircle2 className="mt-0.5 h-6 w-6 shrink-0 text-emerald-700" />
          <div>
            <h2 className="font-semibold text-gray-900">{t('feedback_success_title')}</h2>
            <p className="mt-1 text-sm leading-5 text-gray-600">
              {t('feedback_success_desc')}
            </p>
          </div>
        </div>

        <div className="mt-5 rounded-xl border border-emerald-200 bg-white p-4 text-center">
          <p className="text-sm font-medium text-gray-600">{t('feedback_lookup_code_label')}</p>
          <div className="mt-2 flex items-center justify-center">
            <div className="invisible inline-flex shrink-0 items-center gap-1" aria-hidden="true">
              <div className="h-8 w-8" />
              <div className="h-8 w-8" />
            </div>

            <span className="mx-2 select-all break-all font-mono text-xl font-bold tracking-wide text-emerald-800">
              {submittedFeedback.lookupCode}
            </span>

            <div className="inline-flex shrink-0 items-center gap-1">
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={() => void copyLookupCode()}
                className="h-8 w-8 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                title={t('feedback_copy_code_btn')}
                aria-label={t('feedback_copy_code_btn')}
              >
                <Copy className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                onClick={() => {
                  setLookupInitialCode(submittedFeedback.lookupCode);
                  setIsLookupDialogOpen(true);
                }}
                className="h-8 w-8 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                title={t('feedback_lookup_status_btn')}
                aria-label={t('feedback_lookup_status_btn')}
              >
                <Search className="h-4 w-4" />
              </Button>
            </div>
          </div>
          <p className="mt-2 text-xs leading-5 text-amber-700">
            {t('feedback_save_code_warning')}
          </p>
        </div>

        <button
          type="button"
          onClick={() => setSubmittedFeedback(null)}
          className="mt-4 text-sm font-medium text-emerald-700 underline-offset-4 hover:underline"
        >
          {t('feedback_submit_another')}
        </button>

        <ProductFeedbackLookupDialog
          open={isLookupDialogOpen}
          onOpenChange={setIsLookupDialogOpen}
          initialCode={lookupInitialCode}
        />
      </section>
    );
  }

  const feedbackDesc = t('feedback_desc').replace('{productName}', productName);

  return (
    <section className="rounded-xl border border-amber-200 bg-amber-50/60 p-5 shadow-sm">
      <div className="flex gap-3">
        <MessageSquareWarning className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />

        <div>
          <h2 className="font-semibold text-gray-900">
            {t('feedback_title')}
          </h2>

          <p className="mt-1 text-sm leading-5 text-gray-600">
            {feedbackDesc}
          </p>
        </div>
      </div>

      <form className="mt-4 space-y-3" onSubmit={handleSubmit(onSubmit)}>
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-4">
            <Label htmlFor="feedback-content">{t('feedback_content_label')}</Label>

            <span className="text-xs text-gray-500">
              {contentLength}/1000
            </span>
          </div>

          <Textarea
            id="feedback-content"
            className="min-h-28 resize-y bg-white"
            placeholder={t('feedback_placeholder')}
            maxLength={1000}
            aria-invalid={Boolean(errors.content)}
            {...register('content', {
              validate: (value) =>
                value.trim().length > 0 ||
                t('feedback_validation_required'),
              maxLength: {
                value: 1000,
                message: t('feedback_validation_max'),
              },
            })}
          />

          {errors.content && (
            <p className="text-sm text-destructive">
              {errors.content.message}
            </p>
          )}
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setLookupInitialCode(undefined);
              setIsLookupDialogOpen(true);
            }}
            className="gap-2 border-emerald-300 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
          >
            <Search className="h-4 w-4" />
            {t('feedback_lookup_btn')}
          </Button>

          <Button type="submit" disabled={isSubmitting} variant="create">
            <Send className="h-4 w-4" />
            {isSubmitting ? t('feedback_submitting') : t('feedback_submit_btn')}
          </Button>
        </div>
      </form>

      <ProductFeedbackLookupDialog
        open={isLookupDialogOpen}
        onOpenChange={setIsLookupDialogOpen}
        initialCode={lookupInitialCode}
      />
    </section>
  );
}

export default ProductFeedbackForm;
