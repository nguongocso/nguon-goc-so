import React from "react";
import {
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  FlaskConical,
  LoaderCircle,
  ShieldCheck,
  ShieldQuestion,
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type {
  PublicInspectionResponse,
  PublicInspectionResult,
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

export const PublicInspectionSection: React.FC<PublicInspectionSectionProps> = ({
  inspections,
  data,
  isLoading = false,
  error,
}) => {
  const items = inspections ?? data?.inspections ?? [];
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