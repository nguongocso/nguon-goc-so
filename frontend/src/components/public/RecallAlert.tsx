import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertCircle } from 'lucide-react';
import { useLanguage } from '@/context/LanguageContext';

interface RecallAlertProps {
  message: string;
  messageEn?: string | null;
}

export const RecallAlert = ({ message, messageEn }: RecallAlertProps) => {
  const { lang, t } = useLanguage();

  const displayMessage = (lang === 'en' && messageEn) ? messageEn : message;

  return (
    <Alert variant="destructive" className="p-4 border-red-300 bg-red-50 text-red-900">
      <AlertCircle className="h-5 w-5 flex-shrink-0 mt-0.5 text-red-600" />
      <AlertTitle className="font-bold text-red-800 tracking-wide">
        {t('recall_alert_title')}
      </AlertTitle>
      <AlertDescription className="mt-1 text-sm font-medium leading-relaxed">
        {displayMessage}
      </AlertDescription>
    </Alert>
  );
};