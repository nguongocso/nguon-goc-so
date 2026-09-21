import {
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  FlaskConical,
  History,
  LoaderCircle,
  ShieldCheck,
  ShieldQuestion,
} from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useLanguage } from '@/context/LanguageContext';
import type {
  PublicInspectionResponse,
  PublicInspectionResult,
  PublicInspectionRound,
} from '@/types/publicInspection';
import { getLocalDateString } from '@/utils/dateTime';

export interface PublicInspectionSectionProps {
  inspections?: PublicInspectionResult[];
  data?: PublicInspectionResponse | null;
  isLoading?: boolean;
  error?: string | null;
}

const formatDate = (dateValue: string | null | undefined, fallbackText = 'Chưa cập nhật') => {
  if (!dateValue) return fallbackText;

  const [year, month, day] = dateValue.split('-');
  if (!year || !month || !day) return dateValue;

  return `${day}/${month}/${year}`;
};

const isExpired = (expiryDate?: string): boolean => {
  if (!expiryDate) return false;
  const today = getLocalDateString();
  return expiryDate < today;
};

/** Dòng thời gian lịch sử kiểm nghiệm — chỉ hiển thị khi có từ 2 lần kiểm trở lên. */
function InspectionHistoryTimeline({ history }: { history: PublicInspectionRound[] }) {
  const { t } = useLanguage();
  const isEn = false; // or based on context

  const roundStatusMeta: Record<string, { label: string; className: string }> = {
    PENDING_RESULT: {
      label: isEn ? 'Pending' : 'Chờ kết quả',
      className: 'bg-amber-100 text-amber-800 border border-amber-200',
    },
    PASSED: {
      label: t('passed_badge'),
      className: 'bg-emerald-100 text-emerald-800 border border-emerald-200',
    },
    FAILED: {
      label: t('failed_badge'),
      className: 'bg-red-100 text-red-800 border border-red-200',
    },
    CANCELLED: {
      label: isEn ? 'Cancelled' : 'Đã hủy',
      className: 'bg-gray-100 text-gray-600 border border-gray-200',
    },
  };

  const getMeta = (status?: string | null) =>
    (status && roundStatusMeta[status]) || {
      label: status || (isEn ? 'Unknown' : 'Không xác định'),
      className: 'bg-gray-100 text-gray-600 border border-gray-200',
    };

  return (
    <div className="rounded-lg border border-gray-200 overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-gray-100 bg-gray-50">
        <History className="h-4 w-4 text-gray-500" />
        <p className="text-sm font-semibold text-gray-800">
          {isEn
            ? `Inspection History (${history.length} rounds)`
            : `Lịch sử kiểm nghiệm (${history.length} lần gửi mẫu)`}
        </p>
      </div>
      <ol className="divide-y divide-gray-100">
        {history.map((roundItem) => {
          const meta = getMeta(roundItem.status);
          return (
            <li key={roundItem.round} className="px-4 py-3">
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
                <span className="text-xs font-bold uppercase tracking-wide text-gray-500">
                  {isEn ? `Round ${roundItem.round}` : `Lần ${roundItem.round}`}
                </span>
                <span
                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-2xs font-semibold ${meta.className}`}
                >
                  {meta.label}
                </span>
                <span className="text-xs text-gray-600">
                  {roundItem.laboratoryName || (isEn ? 'Testing Laboratory' : 'Phòng kiểm nghiệm')}
                </span>
                <span className="flex items-center gap-1 text-xs text-gray-500">
                  <CalendarDays className="h-3 w-3 text-gray-400" />
                  {formatDate(roundItem.sampleSentDate, t('not_updated'))}
                </span>
                <span className="text-xs text-gray-600 ml-auto">
                  <strong
                    className={
                      roundItem.failedCriteriaCount > 0
                        ? 'text-red-700'
                        : 'text-emerald-700'
                    }
                  >
                    {roundItem.passedCriteria}/{roundItem.totalCriteria}
                  </strong>{' '}
                  {isEn ? 'passed criteria' : 'chỉ tiêu đạt'}
                </span>
              </div>
              {roundItem.results.length > 0 && (
                <ul className="mt-2 space-y-1 border-l-2 border-gray-100 pl-3">
                  {roundItem.results.map((result) => {
                    const criterionDisplayName = isEn
                      ? result.criterionNameEn || result.criterionName
                      : result.criterionName;
                    return (
                      <li
                        key={result.id}
                        className="flex items-center justify-between gap-2 text-xs"
                      >
                        <span className="text-gray-700">
                          {criterionDisplayName}
                        </span>
                        <span
                          className={`inline-flex items-center gap-1 font-medium ${
                            result.passed ? 'text-emerald-700' : 'text-red-700'
                          }`}
                        >
                          {result.passed ? (
                            <CheckCircle2 className="h-3 w-3" />
                          ) : (
                            <CircleAlert className="h-3 w-3" />
                          )}
                          {result.passed ? t('passed_badge') : t('failed_badge')}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
            </li>
          );
        })}
      </ol>
      <p className="px-4 py-2.5 text-2xs text-gray-500 italic bg-gray-50 border-t border-gray-100">
        {isEn
          ? 'Previous inspection results are preserved — the latest inspection serves as current evaluation basis.'
          : 'Kết quả của các lần kiểm nghiệm trước được lưu giữ nguyên — lần kiểm nghiệm mới nhất là căn cứ đánh giá hiện tại của lô.'}
      </p>
    </div>
  );
}

/**
 * Hiển thị kết quả và chỉ tiêu kiểm nghiệm nông sản trên trang tra cứu công khai.
 */
export function PublicInspectionSection({
  inspections,
  data,
  isLoading = false,
  error,
}: PublicInspectionSectionProps) {
  const { lang, t } = useLanguage();
  const isEn = lang === 'en';

  const items = inspections ?? data?.inspections ?? [];
  const history = data?.history ?? [];
  const hasInspection = items.length > 0;

  const total = data?.totalCriteria ?? items.length;
  const passed = data?.passedCriteria ?? items.filter((i) => i.passed).length;
  const failed = data?.failedCriteriaCount ?? total - passed;
  const failedRatio =
    data?.failedRatio ??
    (total > 0 ? Math.round((failed / total) * 1000) / 10 : 0);

  return (
    <section aria-labelledby="public-inspection-title">
      <Card className="shadow-sm border-gray-100">
        <CardHeader className="border-b border-gray-100">
          <CardTitle
            id="public-inspection-title"
            className="flex items-center gap-2 text-gray-900"
          >
            <FlaskConical className="h-5 w-5 text-emerald-600" />
            {t('inspections_title')}
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
          ) : hasInspection ? (
            <div className="space-y-4">
              {/* Thống kê tổng hợp */}
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm">
                <span className="inline-flex items-center gap-1.5 font-medium text-emerald-800">
                  <CheckCircle2 className="h-4 w-4" />
                  {isEn ? `${t('passed_criteria')}: ${passed}/${total}` : `Đạt ${passed}/${total} chỉ tiêu`}
                </span>
                <span className="inline-flex items-center gap-1.5 font-medium text-red-800">
                  <CircleAlert className="h-4 w-4" />
                  {isEn
                    ? `${t('failed_criteria')}: ${failed}/${total} (${failedRatio}%)`
                    : `Không đạt ${failed}/${total} (${failedRatio}%)`}
                </span>
              </div>

              {/* Lịch sử kiểm nghiệm */}
              {history.length >= 2 && <InspectionHistoryTimeline history={history} />}

              <div className="overflow-x-auto rounded-lg border border-gray-200">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-gray-50/80">
                    <tr>
                      <th className="px-3 py-3 text-center font-semibold text-gray-700 w-12">
                        {isEn ? 'No.' : 'STT'}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-gray-700">
                        {isEn ? 'Inspection Criterion' : 'Chỉ tiêu kiểm nghiệm'}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-gray-700">
                        {isEn ? 'Standard Threshold' : 'Ngưỡng chuẩn'}
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-gray-700">
                        {isEn ? 'Measured Result' : 'Kết quả đo'}
                      </th>
                      <th className="px-4 py-3 text-center font-semibold text-gray-700">
                        {isEn ? 'Evaluation' : 'Đánh giá'}
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 bg-white">
                    {items.map((item, index) => {
                      const expired = isExpired(item.expiryDate);
                      const criterionNameDisplay = isEn
                        ? item.criterionNameEn || item.criterionName
                        : item.criterionName;
                      const standardValueDisplay = isEn
                        ? item.standardValueEn || item.standardValue || 'Standard Specification'
                        : item.standardValue || 'Theo quy chuẩn';

                      let measuredValueDisplay = item.measuredValue;
                      if (item.measuredValue === 'Đạt chuẩn (Trong ngưỡng an toàn)') {
                        measuredValueDisplay = t('measured_passed');
                      } else if (item.measuredValue === 'Không đạt (Vượt ngưỡng quy định)') {
                        measuredValueDisplay = t('measured_failed');
                      } else if (!item.measuredValue) {
                        measuredValueDisplay = item.passed ? t('measured_passed') : t('measured_failed');
                      }

                      return (
                        <tr
                          key={item.id}
                          className="hover:bg-slate-50/70 transition-colors"
                        >
                          <td className="px-3 py-3 text-center text-xs font-medium text-gray-500">
                            {index + 1}
                          </td>
                          <td className="px-4 py-3">
                            <div className="font-medium text-gray-900">
                              {criterionNameDisplay}
                            </div>
                            <div className="flex items-center gap-2 flex-wrap mt-0.5">
                              {item.laboratoryName && (
                                <span className="text-xs text-gray-500">
                                  {t('laboratory_label')} {item.laboratoryName}
                                </span>
                              )}
                              {item.entrySource === 'TESTING_UNIT_PORTAL' && (
                                <span
                                  className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-teal-50 text-teal-700 border border-teal-200"
                                  title="Kết quả do chính đơn vị kiểm nghiệm nhập trực tiếp qua cổng liên kết số"
                                >
                                  <ShieldCheck className="h-3 w-3 text-teal-600" />
                                  Đơn vị kiểm nghiệm khai
                                </span>
                              )}
                              {item.entrySource === 'COOPERATIVE_MANUAL' && (
                                <span
                                  className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 border border-slate-200"
                                  title="Kết quả do Hợp tác xã nhập tay theo phiếu giấy"
                                >
                                  HTX nhập
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-gray-600">
                            {standardValueDisplay}
                          </td>
                          <td className="px-4 py-3 text-gray-800 font-mono text-xs sm:text-sm">
                            {measuredValueDisplay}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <span
                              className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold shadow-xs ${
                                item.passed
                                  ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                                  : 'bg-red-100 text-red-800 border border-red-200'
                              }`}
                            >
                              {item.passed ? (
                                <CheckCircle2 className="h-3.5 w-3.5" />
                              ) : (
                                <CircleAlert className="h-3.5 w-3.5" />
                              )}
                              {item.passed ? t('passed_badge') : t('failed_badge')}
                            </span>
                            {expired && (
                              <div className="text-2xs font-medium text-red-600 mt-1">
                                {isEn ? '(Expired)' : '(Hết hiệu lực)'}
                              </div>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Thông tin ngày cấp và hạn hiệu lực */}
              {items[0] && (items[0].inspectionDate || items[0].expiryDate) && (
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-gray-50 px-4 py-3 text-xs text-gray-600 border border-gray-100">
                  {items[0].inspectionDate && (
                    <div className="flex items-center gap-1.5">
                      <CalendarDays className="h-3.5 w-3.5 text-gray-400" />
                      <span>
                        {isEn ? 'Inspection Date:' : 'Ngày kiểm nghiệm:'}{' '}
                        <strong className="text-gray-800">
                          {formatDate(items[0].inspectionDate, t('not_updated'))}
                        </strong>
                      </span>
                    </div>
                  )}
                  {items[0].expiryDate && (
                    <div className="flex items-center gap-1.5">
                      <ShieldCheck className="h-3.5 w-3.5 text-emerald-600" />
                      <span>
                        {isEn ? 'Validity Expiry:' : 'Hạn hiệu lực kết quả:'}{' '}
                        <strong className="text-gray-800">
                          {formatDate(items[0].expiryDate, t('not_updated'))}
                        </strong>
                      </span>
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="flex min-h-28 flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center">
              <ShieldQuestion className="h-7 w-7 text-gray-400" />
              <p className="font-medium text-gray-700">
                {t('no_inspections')}
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  );
}

export default PublicInspectionSection;
