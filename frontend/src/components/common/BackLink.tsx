import { ArrowLeft } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { resolveBackNav } from '@/config/backNavigation';
import { cn } from '@/lib/utils';

interface BackLinkProps {
  compact?: boolean;
  className?: string;
}

export const BackLink = ({ compact = false, className }: BackLinkProps) => {
  const navigate = useNavigate();
  const location = useLocation();

  const resolved = resolveBackNav(location.pathname, location.search);

  if (!resolved) return null;

  const handleBack = () => {
    const historyState = window.history.state as { idx?: number } | null;
    if (typeof historyState?.idx === 'number' && historyState.idx > 0) {
      navigate(-1);
    } else {
      navigate(resolved.fallbackTo, { replace: true });
    }
  };

  return (
    <button
      type="button"
      onClick={handleBack}
      title={resolved.label}
      aria-label={resolved.label}
      className={cn(
        'group inline-flex min-h-8 items-center gap-1.5 rounded-md text-sm font-medium text-emerald-700 underline-offset-4 transition-colors hover:text-emerald-900 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500',
        className,
      )}
    >
      <ArrowLeft className="size-4 shrink-0 transition-transform group-hover:-translate-x-0.5" />
      {!compact && <span className="max-w-64 truncate">{resolved.label}</span>}
    </button>
  );
};
