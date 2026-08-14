import { AlertCircle, CheckCircle2, LoaderCircle } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { cn } from '@/lib/utils';

interface LotValidationStatusProps {
  isValid: boolean | null;
  message: string;
  loading?: boolean;
  className?: string;
}

export const LotValidationStatus = ({ isValid, message, loading, className }: LotValidationStatusProps) => {
  if (loading) {
    return (
      <div className={cn('flex items-center gap-2 text-muted-foreground', className)}>
        <LoaderCircle className="h-4 w-4 animate-spin" />
        <span className="text-sm">Đang kiểm tra lô...</span>
      </div>
    );
  }

  if (isValid === null) return null;

  if (isValid) {
    return (
      <Alert className={cn('border-emerald-200 bg-emerald-50 text-emerald-800', className)}>
        <CheckCircle2 className="h-4 w-4" />
        <AlertDescription className="text-sm">{message}</AlertDescription>
      </Alert>
    );
  }

  return (
    <Alert className={cn('border-amber-200 bg-amber-50 text-amber-800', className)}>
      <AlertCircle className="h-4 w-4" />
      <AlertDescription className="text-sm">{message}</AlertDescription>
    </Alert>
  );
};