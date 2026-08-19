import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AlertCircle } from 'lucide-react';

interface RecallAlertProps {
  message: string;
}

export const RecallAlert = ({ message }: RecallAlertProps) => {
  return (
    <Alert variant="destructive" className="p-4">
      <AlertCircle className="h-5 w-5 flex-shrink-0 mt-0.5" />
      <AlertTitle>Cảnh báo thu hồi</AlertTitle>
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  );
};