import React from "react";
import {
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  FlaskConical,
  History,
  LoaderCircle,
  ShieldCheck,
  ShieldQuestion,
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type {
  PublicInspectionResponse,
  PublicInspectionResult,
  PublicInspectionRound,
} from "@/types/publicInspection";
import { getLocalDateString } from "@/utils/dateTime";

export interface PublicInspectionSectionProps {
  inspections?: PublicInspectionResult[];
  data?: PublicInspectionResponse | null;
  isLoading?: boolean;
  error?: string | null;
}

const formatDate = (dateValue: string | null | undefined) => {
  if (!dateValue) return "Chưa cập nhật";

  const [year, month, day] = dateValue.split("-");
  if (!year || !month || !day) return dateValue;

  return `${day}/${month}/${year}`;
};

const isExpired = (expiryDate?: string): boolean => {
  if (!expiryDate) return false;
  const today = getLocalDateString();
  return expiryDate < today;
};

/** Nhãn + màu badge cho trạng thái từng lần kiểm nghiệm trong lịch sử. */
const roundStatusMeta: Record<
  string,
  { label: string; className: string }
> = {
  PENDING_RESULT: {
    label: "Chờ kết quả",
    className: "bg-amber-100 text-amber-800 border border-amber-200",
  },
  PASSED: {
    label: "Đạt",
    className: "bg-emerald-100 text-emerald-800 border border-emerald-200",
  },
  FAILED: {
    label: "Không đạt",
    className: "bg-red-100 text-red-800 border border-red-200",
  },
  CANCELLED: {
    label: "Đã hủy",
    className: "bg-gray-100 text-gray-600 border border-gray-200",
  },
};

const getRoundStatusMeta = (status?: string | null) =>
  (status && roundStatusMeta[status]) || {
    label: status || "Không xác định",
    className: "bg-gray-100 text-gray-600 border border-gray-200",
  };

/** Dòng thời gian lịch sử kiểm nghiệm — chỉ hiển thị khi có từ 2 lần kiểm trở lên. */
const InspectionHistoryTimeline: React.FC<{ history: PublicInspectionRound[] }> = ({
  history,
}) => (
  <div className="rounded-lg border border-gray-200 overflow-hidden">
    <div className="flex items-center gap-2 px-4 py-3 border-b border-gray-100 bg-gray-50">
      <History className="h-4 w-4 text-gray-500" />
      <p className="text-sm font-semibold text-gray-800">
        Lịch sử kiểm nghiệm ({history.length} lần gửi mẫu)
      </p>
    </div>
    <ol className="divide-y divide-gray-100">
      {history.map((roundItem) => {
        const meta = getRoundStatusMeta(roundItem.status);
        return (
          <li key={roundItem.round} className="px-4 py-3">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
              <span className="text-xs font-bold uppercase tracking-wide text-gray-500">
                Lần {roundItem.round}
              </span>
              <span
                className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-2xs font-semibold ${meta.className}`}
              >
                {meta.label}
              </span>
              <span className="text-xs text-gray-600">
                {roundItem.laboratoryName || "Phòng kiểm nghiệm"}
              </span>
              <span className="flex items-center gap-1 text-xs text-gray-500">
                <CalendarDays className="h-3 w-3 text-gray-400" />
                {formatDate(roundItem.sampleSentDate)}
              </span>
              <span className="text-xs text-gray-600 ml-auto">
                <strong
                  className={
                    roundItem.failedCriteriaCount > 0
                      ? "text-red-700"
                      : "text-emerald-700"
                  }
                >
                  {roundItem.passedCriteria}/{roundItem.totalCriteria}
                </strong>{" "}
                chỉ tiêu đạt
              </span>
            </div>
            {roundItem.results.length > 0 && (
              <ul className="mt-2 space-y-1 border-l-2 border-gray-100 pl-3">
                {roundItem.results.map((result) => (
                  <li
                    key={result.id}
                    className="flex items-center justify-between gap-2 text-xs"
                  >
                    <span className="text-gray-700">
                      {result.criterionName}
                    </span>
                    <span
                      className={`inline-flex items-center gap-1 font-medium ${
                        result.passed ? "text-emerald-700" : "text-red-700"
                      }`}
                    >
                      {result.passed ? (
                        <CheckCircle2 className="h-3 w-3" />
                      ) : (
                        <CircleAlert className="h-3 w-3" />
                      )}
                      {result.passed ? "Đạt" : "Không đạt"}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </li>
        );
      })}
    </ol>
    <p className="px-4 py-2.5 text-2xs text-gray-500 italic bg-gray-50 border-t border-gray-100">
      Kết quả của các lần kiểm nghiệm trước được lưu giữ nguyên — lần kiểm
      nghiệm mới nhất là căn cứ đánh giá hiện tại của lô.
    </p>
  </div>
);

export const PublicInspectionSection: React.FC<PublicInspectionSectionProps> = ({
  inspections,
  data,
  isLoading = false,
  error,
}) => {
  const items = inspections ?? data?.inspections ?? [];
  const history = data?.history ?? [];
  const hasInspection = items.length > 0;

  /*
   * Thống kê tổng hợp kết quả kiểm nghiệm:
   * ưu tiên dùng số liệu tổng hợp từ backend; nếu không có
   * (ví dụ component được truyền inspections trực tiếp),
   * tự tính từ danh sách hiển thị.
   */
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
            Kết quả kiểm nghiệm chất lượng
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            Các chỉ tiêu kiểm nghiệm, kết quả đo lường và đánh giá chất lượng của lô sản xuất.
          </p>
        </CardHeader>

        <CardContent className="pt-4">
          {isLoading ? (
            <div className="flex min-h-28 flex-col items-center justify-center gap-3 text-sm text-gray-500">
              <LoaderCircle className="h-6 w-6 animate-spin text-emerald-600" />
              Đang tải kết quả kiểm nghiệm...
            </div>
          ) : error ? (
            <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
              <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-amber-700" />
              <p>{error}</p>
            </div>
          ) : hasInspection ? (
            <div className="space-y-4">
              {/* Tổng hợp kết quả kiểm nghiệm */}
              <div
                className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm"
                aria-label={`Tổng hợp kết quả: đạt ${passed} trên ${total} chỉ tiêu, không đạt ${failed} trên ${total} chỉ tiêu, tỷ lệ không đạt ${failedRatio} phần trăm`}
              >
                <span className="inline-flex items-center gap-1.5 font-medium text-emerald-800">
                  <CheckCircle2 className="h-4 w-4" />
                  Đạt {passed}/{total} chỉ tiêu
                </span>
                <span className="inline-flex items-center gap-1.5 font-medium text-red-800">
                  <CircleAlert className="h-4 w-4" />
                  Không đạt {failed}/{total} ({failedRatio}%)
                </span>
              </div>

              {/* Dòng thời gian lịch sử kiểm nghiệm — hiện khi có ≥ 2 lần gửi mẫu */}
              {history.length >= 2 && <InspectionHistoryTimeline history={history} />}

              <div className="overflow-x-auto rounded-lg border border-gray-200">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-gray-50/80">
                    <tr>
                      <th className="px-4 py-3 text-left font-semibold text-gray-700">
                        Chỉ tiêu kiểm nghiệm
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-gray-700">
                        Ngưỡng chuẩn
                      </th>
                      <th className="px-4 py-3 text-left font-semibold text-gray-700">
                        Kết quả đo
                      </th>
                      <th className="px-4 py-3 text-center font-semibold text-gray-700">
                        Đánh giá
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 bg-white">
                    {items.map((item) => {
                      const expired = isExpired(item.expiryDate);
                      return (
                        <tr
                          key={item.id}
                          className="hover:bg-slate-50/70 transition-colors"
                        >
                          <td className="px-4 py-3">
                            <div className="font-medium text-gray-900">
                              {item.criterionName}
                            </div>
                            {item.laboratoryName && (
                              <div className="text-xs text-gray-500 mt-0.5">
                                Đơn vị: {item.laboratoryName}
                              </div>
                            )}
                          </td>
                          <td className="px-4 py-3 text-gray-600">
                            {item.standardValue || "Theo quy chuẩn"}
                          </td>
                          <td className="px-4 py-3 text-gray-800 font-mono text-xs sm:text-sm">
                            {item.measuredValue || (item.passed ? "Đạt chuẩn" : "Không đạt")}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <span
                              className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold shadow-xs ${
                                item.passed
                                  ? "bg-emerald-100 text-emerald-800 border border-emerald-200"
                                  : "bg-red-100 text-red-800 border border-red-200"
                              }`}
                            >
                              {item.passed ? (
                                <CheckCircle2 className="h-3.5 w-3.5" />
                              ) : (
                                <CircleAlert className="h-3.5 w-3.5" />
                              )}
                              {item.passed ? "Đạt chuẩn" : "Không đạt"}
                            </span>
                            {expired && (
                              <div className="text-2xs font-medium text-red-600 mt-1">
                                (Hết hiệu lực)
                              </div>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              {/* Thông tin ngày cấp và thời hạn hiệu lực chung */}
              {items[0] && (items[0].inspectionDate || items[0].expiryDate) && (
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-gray-50 px-4 py-3 text-xs text-gray-600 border border-gray-100">
                  {items[0].inspectionDate && (
                    <div className="flex items-center gap-1.5">
                      <CalendarDays className="h-3.5 w-3.5 text-gray-400" />
                      <span>Ngày kiểm nghiệm: <strong className="text-gray-800">{formatDate(items[0].inspectionDate)}</strong></span>
                    </div>
                  )}
                  {items[0].expiryDate && (
                    <div className="flex items-center gap-1.5">
                      <ShieldCheck className="h-3.5 w-3.5 text-emerald-600" />
                      <span>Hạn hiệu lực kết quả: <strong className="text-gray-800">{formatDate(items[0].expiryDate)}</strong></span>
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="flex min-h-28 flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center">
              <ShieldQuestion className="h-7 w-7 text-gray-400" />
              <p className="font-medium text-gray-700">
                Chưa có dữ liệu kiểm nghiệm
              </p>
              <p className="text-sm text-gray-500 italic max-w-sm">
                Lô sản xuất chưa có dữ liệu kiểm nghiệm chất lượng công khai.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  );
};

export default PublicInspectionSection;