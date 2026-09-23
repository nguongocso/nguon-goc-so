import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { isAxiosError } from 'axios';
import { LoaderCircle, MessageCircleMore, Search } from 'lucide-react';
import { lookupPublicProductFeedback } from '@/api/productFeedbackApi';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useLanguage } from '@/context/LanguageContext';
import type { PublicProductFeedbackLookupResult } from '@/types/productFeedback';
import {
  ProductFeedbackInlineResult,
  type LookupErrorKind,
} from './ProductFeedbackInlineResult';

interface ProductFeedbackLookupDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialCode?: string;
}

/** Hộp thoại tra cứu trạng thái và phản hồi phản ánh sản phẩm. */
export function ProductFeedbackLookupDialog({
  open,
  onOpenChange,
  initialCode,
}: ProductFeedbackLookupDialogProps) {
  const { t } = useLanguage();
  const [code, setCode] = useState(initialCode ?? '');
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] =
    useState<PublicProductFeedbackLookupResult | null>(null);
  const [errorKind, setErrorKind] = useState<LookupErrorKind | null>(null);
  const [searchedCode, setSearchedCode] = useState('');

  const executeLookup = async (lookupCodeToQuery: string) => {
    const normalized = lookupCodeToQuery.trim().toUpperCase();
    if (!normalized) {
      setValidationMessage(t('feedback_lookup_empty_error'));
      return;
    }

    setValidationMessage(null);
    setErrorKind(null);
    setResult(null);
    setSearchedCode(normalized);
    setIsLoading(true);

    try {
      const data = await lookupPublicProductFeedback({ lookupCode: normalized });
      setResult(data);
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.status === 404) {
        setErrorKind('not-found');
      } else if (isAxiosError(error) && error.response?.status === 429) {
        setErrorKind('rate-limit');
      } else {
        setErrorKind('system');
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      const startCode = initialCode?.trim() ?? '';
      setCode(startCode);
      setValidationMessage(null);
      setResult(null);
      setErrorKind(null);
      setSearchedCode('');

      if (startCode) {
        void executeLookup(startCode);
      }
    }
  }, [open, initialCode]);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    void executeLookup(code);
  };

  const handleReset = () => {
    setResult(null);
    setErrorKind(null);
    setSearchedCode('');
    setCode('');
    setValidationMessage(null);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2 text-emerald-800">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-100">
              <MessageCircleMore className="h-4 w-4 text-emerald-700" />
            </div>
            <DialogTitle className="text-lg font-bold text-slate-900">
              {t('feedback_lookup_dialog_title')}
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-slate-500">
            {t('feedback_lookup_dialog_desc')}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-3 pt-2">
          <div className="space-y-1.5">
            <Label htmlFor="dialog-lookup-code" className="text-xs font-semibold text-slate-700">
              {t('feedback_lookup_input_label')}
            </Label>
            <div className="flex gap-2">
              <Input
                id="dialog-lookup-code"
                type="text"
                placeholder={t('feedback_lookup_input_placeholder')}
                value={code}
                onChange={(e) => {
                  setCode(e.target.value);
                  if (validationMessage) setValidationMessage(null);
                }}
                className="flex-1 font-mono uppercase tracking-wide border-emerald-200 focus-visible:ring-emerald-300"
                autoComplete="off"
                maxLength={64}
              />
              <Button
                type="submit"
                disabled={isLoading}
                className="gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {isLoading ? (
                  <LoaderCircle className="h-4 w-4 animate-spin" />
                ) : (
                  <Search className="h-4 w-4" />
                )}
                <span>{isLoading ? t('feedback_lookup_searching') : t('feedback_lookup_submit_btn')}</span>
              </Button>
            </div>
            {validationMessage && (
              <p className="text-xs text-destructive">{validationMessage}</p>
            )}
          </div>
        </form>

        <ProductFeedbackInlineResult
          isLoading={isLoading}
          lookupCode={searchedCode}
          result={result}
          errorKind={errorKind}
          onReset={handleReset}
          onRetry={() => void executeLookup(searchedCode)}
        />
      </DialogContent>
    </Dialog>
  );
}
