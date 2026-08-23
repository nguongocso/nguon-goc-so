import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';

interface BackButtonProps {
  label?: string;
  fallbackTo?: string;
  className?: string;
}

export const BackButton = ({
  label = 'Quay lại',
  fallbackTo = '/dashboard',
  className,
}: BackButtonProps) => {
  const navigate = useNavigate();

  const handleBack = () => {
    const historyState = window.history.state as { idx?: number } | null;
    if (typeof historyState?.idx === 'number' && historyState.idx > 0) {
      navigate(-1);
    } else {
      navigate(fallbackTo, { replace: true });
    }
  };

  return (
    <Button variant="outline" onClick={handleBack} className={className}>
      <ArrowLeft className="size-4" />
      {label}
    </Button>
  );
};
