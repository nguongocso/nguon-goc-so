import { Badge } from '@/components/ui/badge';

interface Props {
  isValid: boolean;
  expiryDate: string; // YYYY-MM-DD
}

const DAYS_MS = 1000 * 60 * 60 * 24;

export const CertificationStatusBadge = ({ isValid, expiryDate }: Props) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(expiryDate + 'T00:00:00');
  const daysRemaining = Math.ceil((expiry.getTime() - today.getTime()) / DAYS_MS);

  if (!isValid) {
    return <Badge variant="destructive">Hết hạn</Badge>;
  }

  if (daysRemaining <= 30) {
    return <Badge variant="warning">Sắp hết hạn ({daysRemaining} ngày)</Badge>;
  }

  return <Badge variant="success">Còn hiệu lực</Badge>;
};