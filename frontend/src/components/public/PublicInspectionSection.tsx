import {
  BadgeCheck,
  CalendarDays,
  CircleAlert,
  FlaskConical,
  LoaderCircle,
  ShieldQuestion,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type {
  PublicInspectionResponse,
  PublicInspectionResult,
} from "@/types/publicInspection";
import { getLocalDateString } from "@/utils/dateTime";

interface PublicInspectionSectionProps {
  data?: PublicInspectionResponse | null;
  isLoading?: boolean;
  error?: string | null;
}

const formatDate = (dateValue: string | null) => {
  if (!dateValue) return "Chưa cập nhật";

  const [year, month, day] = dateValue.split("-");
  if (!year || !month || !day) return dateValue;

  return `${day}/${month}/${year}`;
};

const isExpired = (expiryDate: string): boolean => {
  const today = getLocalDateString();
  return expiryDate < today;
};

function InspectionCard({
  inspection,
}: {
  inspection: PublicInspectionResult;
}) {
  const passed = inspection.overallResult === "PASSED";
  const expired = isExpired(inspection.expiryDate);

  return (
    <article
      className={
        passed && !expired
          ? "rounded-lg border border-emerald-100 bg-emerald-50/40 p-4"
          : "rounded-lg border border-slate-200 bg-slate-50 p-4"
      }
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="font-semibold text-gray-900">Kết quả kiểm nghiệm</h3>
          <p className="mt-1 break-all font-mono text-xs text-gray-500">
            Yêu cầu: #{inspection.requestId.slice(0, 8)}
          </p>
        </div>

        <Badge
          className={
            passed
              ? "shrink-0 border-emerald-200 bg-emerald-100 text-emerald-800 hover:bg-emerald-100"
              : "shrink-0 border-red-200 bg-red-100 text-red-800 hover:bg-red-100"
          }
          variant="outline"
        >
          {passed ? <BadgeCheck /> : <CircleAlert />}
          {inspection.overallResultLabel}
        </Badge>
      </div>

      <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
        <div className="flex items-start gap-2 text-gray-600">
          <CalendarDays className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
          <div>
            <dt className="text-xs text-gray-500">Ngày cấp kết quả</dt>
            <dd className="mt-0.5 text-gray-800">
              {formatDate(inspection.issueDate)}
            </dd>
          </div>
        </div>

        <div className="flex items-start gap-2 text-gray-600">
          <CalendarDays className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" />
          <div>
            <dt className="text-xs text-gray-500">Hết hiệu lực</dt>
            <dd className="mt-0.5 text-gray-800">
              {formatDate(inspection.expiryDate)}
              {expired && (
                <span className="ml-2 text-xs font-medium text-red-600">
                  (đã hết hiệu lực)
                </span>
              )}
            </dd>
          </div>
        </div>
      </dl>
    </article>
  );
}

export function PublicInspectionSection({
  data,
  isLoading = false,
  error,
}: PublicInspectionSectionProps) {
  const inspections = data?.inspections ?? [];
  const hasInspection = Boolean(
    data?.hasInspection && inspections.length > 0
  );

  return (
    <section aria-labelledby="public-inspection-title">
      <Card className="shadow-sm">
        <CardHeader className="border-b border-gray-100">
          <CardTitle
            id="public-inspection-title"
            className="flex items-center gap-2 text-gray-900"
          >
            <FlaskConical className="h-5 w-5 text-emerald-600" />
            Kết quả kiểm nghiệm
          </CardTitle>
          <p className="text-sm text-muted-foreground">
            Kết luận kiểm nghiệm chất lượng và thời hạn hiệu lực của lô sản
            xuất này.
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
            <div className="space-y-3">
              {inspections.map((inspection) => (
                <InspectionCard
                  key={inspection.requestId}
                  inspection={inspection}
                />
              ))}
            </div>
          ) : (
            <div className="flex min-h-28 flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center">
              <ShieldQuestion className="h-7 w-7 text-gray-400" />
              <p className="font-medium text-gray-700">
                Chưa có kết quả kiểm nghiệm
              </p>
              <p className="max-w-sm text-sm text-gray-500">
                Lô sản xuất này chưa được ghi nhận kết quả kiểm nghiệm.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </section>
  );
}