import React from 'react';
import { RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface RefreshButtonProps {
  onClick?: () => void;
  loading?: boolean;
  label?: string;
}

export const RefreshButton: React.FC<RefreshButtonProps> = ({
  onClick,
  loading = false,
  label = 'Làm mới',
}) => {
  return (
    <Button variant="outline" onClick={onClick} disabled={loading}>
      <RefreshCw className={`size-4 ${loading ? 'animate-spin' : ''}`} />
      {label}
    </Button>
  );
};
