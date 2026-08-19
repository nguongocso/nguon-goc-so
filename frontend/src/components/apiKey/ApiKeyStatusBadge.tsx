import React from 'react';
import { Badge } from '@/components/ui/badge';
import type { PartnerApiKeyStatus } from '@/types/apiKey';
import { CheckCircle2, XCircle, Clock } from 'lucide-react';

interface ApiKeyStatusBadgeProps {
  status: PartnerApiKeyStatus;
}

export const ApiKeyStatusBadge: React.FC<ApiKeyStatusBadgeProps> = ({ status }) => {
  switch (status) {
    case 'ACTIVE':
      return (
        <Badge className="bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-500/25 border-emerald-200 dark:border-emerald-800 flex items-center gap-1 w-fit">
          <CheckCircle2 className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
          <span>Đang hoạt động</span>
        </Badge>
      );
    case 'REVOKED':
      return (
        <Badge variant="destructive" className="bg-rose-500/15 text-rose-700 dark:text-rose-400 border-rose-200 dark:border-rose-800 flex items-center gap-1 w-fit">
          <XCircle className="w-3 h-3 text-rose-600 dark:text-rose-400" />
          <span>Đã thu hồi</span>
        </Badge>
      );
    case 'EXPIRED':
      return (
        <Badge variant="secondary" className="bg-amber-500/15 text-amber-700 dark:text-amber-400 border-amber-200 dark:border-amber-800 flex items-center gap-1 w-fit">
          <Clock className="w-3 h-3 text-amber-600 dark:text-amber-400" />
          <span>Hết hạn</span>
        </Badge>
      );
    default:
      return <Badge variant="outline">{status}</Badge>;
  }
};
