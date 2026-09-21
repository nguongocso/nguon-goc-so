import {
  Award,
  BadgeCheck,
  CalendarDays,
  CircleAlert,
  FileCheck2,
  Landmark,
  LoaderCircle,
} from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useLanguage } from '@/context/LanguageContext';
import type {
  PublicCertification,
  PublicLotCertificationsResponse,
} from '@/types/publicCertification';

interface PublicCertificationsSectionProps {
  data?: PublicLotCertificationsResponse | null;
  isLoading?: boolean;
  error?: string | null;
}

const formatDate = (dateValue: string | null, notUpdatedText: string) => {
  if (!dateValue) return notUpdatedText;

  const [year, month, day] = dateValue.split('-');
  if (!year || !month || !day) return dateValue;

  return `${day}/${month}/${year}`;
};

function CertificationCard({ certification }: { certification: PublicCertification }) {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  const isValid = certification.status === 'VALID';

  // TC-04 Fallback: nếu certificationNameEn null thì fallback về certificationName
  const displayName = isEn
    ? certification.certificationNameEn || certification.certificationName
    : certification.certificationName;

  const statusLabel = isEn
    ? isValid
      ? t('cert_status_valid')
      : t('cert_status_expired')
    : certification.statusLabel;

  return (
    <article
      className={
        isValid
          ? 'rounded-lg border border-emerald-100 bg-emerald-50/40 p-4'
          : 'rounded-lg border border-slate-200 bg-slate-50 p-4'
      }
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="font-semibold text-gray-900">
            {displayName}
          </h3>
          <p className="mt-1 break-all font-mono text-xs text-gray-500">
            {isEn ? 'Code' : 'Mã'}: {certification.certificationCode}
          </p>
        </div>

        <Badge
          className={
            isValid
              ? 'shrink-0 border-emerald-200 bg-emerald-100 text-emerald-800 hover:bg-emerald-100'
              : 'shrink-0 border-slate-200 bg-slate-200 text-slate-700 hover:bg-slate-200'
          }
          variant="outline"
        >
          {isValid ? <BadgeCheck /> : <CircleAlert />}
          {statusLabel}
        </Badge>
      </div>

      <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div className="flex items-start gap-2 text-gray-600">
          <Landmark className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
          <div>
            <dt className="text-xs text-gray-500">{t('issued_by')}</dt>
            <dd className="mt-0.5 text-gray-800">
              {certification.issuedBy || t('not_updated')}
            </dd>
          </div>
        </div>

        <div className="flex items-start gap-2 text-gray-600">
          <CalendarDays className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
          <div>
            <dt className="text-xs text-gray-500">{t('expiry_date')}</dt>
            <dd className="mt-0.5 text-gray-800">
              {formatDate(certification.issueDate, t('not_updated'))} -{' '}
              {formatDate(certification.expiryDate, t('not_updated'))}
            </dd>
          </div>
        </div>
      </dl>
    </article>
  );
}

/**
 * Hiển thị danh sách chứng nhận tiêu chuẩn của lô sản xuất trên trang tra cứu công khai.
 */
export function PublicCertificationsSection({
  data,
  isLoading = false,
  error,
}: PublicCertificationsSectionProps) {
  const { t } = useLanguage();
  const certifications = data?.certifications ?? [];
  const hasCertification = Boolean(
    data?.hasCertification && certifications.length > 0,
  );

  return (
    <section aria-labelledby="public-certifications-title">
      <Card className="shadow-sm">
        <CardHeader className="border-b border-gray-100">
          <CardTitle
            id="public-certifications-title"
            className="flex items-center gap-2 text-gray-900"
          >
            <Award className="h-5 w-5 text-emerald-600" />
            {t('certifications_title')}
          </CardTitle>
        </CardHeader>

        <CardContent className="pt-4">
          {isLoading ? (
            <div className="flex min-h-28 flex-col items-center justify-center gap-3 text-sm text-gray-500">
              <LoaderCircle className="h-6 w-6 animate-spin text-emerald-600" />
              {t('loading_info')}
            </div>
          ) : error ? (
            <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />
              <p>{error}</p>
            </div>
          ) : hasCertification ? (
            <div className="space-y-3">
              {certifications.map((certification) => (
                <CertificationCard
                  key={certification.certificationId}
                  certification={certification}
                />
              ))}
            </div>
          ) : (
            <div className="flex min-h-28 flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center">
              <FileCheck2 className="h-7 w-7 text-gray-400" />
              <p className="font-medium text-gray-700">{t('no_certifications')}</p>
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  );
}

export default PublicCertificationsSection;
