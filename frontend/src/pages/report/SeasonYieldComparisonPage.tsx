import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import axios from "axios";
import {
  AlertCircle,
  BarChart3,
  CalendarRange,
  Info,
  PackageCheck,
  RefreshCw,
  Scale,
} from "lucide-react";
import { getSeasonYieldComparison } from "@/api/seasonYieldComparisonApi";
import { SeasonYieldComparisonChart } from "@/components/report/SeasonYieldComparisonChart";
import { SeasonYieldComparisonFilter } from "@/components/report/SeasonYieldComparisonFilter";
import { SeasonYieldComparisonTable } from "@/components/report/SeasonYieldComparisonTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { useAuth } from "@/hooks/useAuth";
import type {
  SeasonYieldComparisonParams,
  SeasonYieldComparisonResponse,
} from "@/types/seasonYieldComparison";

const CURRENT_YEAR = new Date().getFullYear();
const DEFAULT_YEARS = [CURRENT_YEAR - 1, CURRENT_YEAR];
const DEFAULT_PARAMS: SeasonYieldComparisonParams = { years: DEFAULT_YEARS };

const numberFormatter = new Intl.NumberFormat("vi-VN", {
  maximumFractionDigits: 1,
});

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || "Không thể tải dữ liệu so sánh mùa vụ.";
  }
  return "Không thể tải dữ liệu so sánh mùa vụ.";
}

export default function SeasonYieldComparisonPage() {
  const { user } = useAuth();
  const [data, setData] = useState<SeasonYieldComparisonResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [lastParams, setLastParams] = useState<SeasonYieldComparisonParams>(DEFAULT_PARAMS);

  const loadComparison = useCallback(async (params: SeasonYieldComparisonParams) => {
    setLoading(true);
    setErrorMessage("");
    setLastParams(params);
    try {
      setData(await getSeasonYieldComparison(params));
    } catch (error) {
      setData(null);
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadComparison(DEFAULT_PARAMS);
  }, [loadComparison]);

  const summary = useMemo(() => {
    const seasons = data?.seasons ?? [];
    const baseline = seasons.find(
      (item) =>
        item.year === data?.baselineYear &&
        item.seasonCode === data?.baselineSeasonCode,
    );
    const highest = seasons.reduce<(typeof seasons)[number] | null>(
      (current, item) => (!current || item.totalQuantity > current.totalQuantity ? item : current),
      null,
    );

    return {
      count: seasons.length,
      lots: seasons.reduce((total, item) => total + item.lotCount, 0),
      baseline,
      highest,
    };
  }, [data]);

  const hasComparisonData = Boolean(data?.hasData && data.seasons.length > 0);

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-4 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-medium text-primary">
            <BarChart3 className="h-4 w-4" /> Báo cáo sản lượng
          </div>
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
            So sánh sản lượng giữa các mùa vụ
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            Đối chiếu tổng sản lượng, số lô và mức tăng giảm giữa các mùa vụ theo dữ liệu hiện có.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="report-season-yield" />
          <Button variant="outline" onClick={() => void loadComparison(lastParams)} disabled={loading}>
            <RefreshCw className={loading ? "animate-spin" : ""} />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="flex items-start gap-3 rounded-xl bg-blue-50 p-4 text-sm text-blue-900 ring-1 ring-blue-200">
        <Info className="mt-0.5 h-5 w-5 shrink-0 text-blue-600" />
        <p>
          Phiên bản hiện tại so sánh theo danh sách năm. Mùa vụ gốc và các nhóm Đông Xuân, Hè Thu,
          Thu Đông được backend tự xác định từ dữ liệu lô sản xuất.
        </p>
      </div>

      <SeasonYieldComparisonFilter
        defaultYears={DEFAULT_YEARS}
        currentUserRole={user?.roleCode}
        loading={loading}
        onCompare={(params) => void loadComparison(params)}
      />

      {loading && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4" aria-label="Đang tải dữ liệu">
          {Array.from({ length: 4 }, (_, index) => (
            <div key={index} className="h-28 animate-pulse rounded-xl bg-muted" />
          ))}
        </div>
      )}

      {!loading && errorMessage && (
        <Card className="border-destructive/30 bg-destructive/5">
          <CardContent className="flex flex-col items-center py-12 text-center">
            <AlertCircle className="mb-3 h-10 w-10 text-destructive" />
            <h2 className="text-lg font-semibold">Không tải được dữ liệu</h2>
            <p className="mt-1 max-w-xl text-muted-foreground">{errorMessage}</p>
            <Button className="mt-4" onClick={() => void loadComparison(lastParams)}>
              Thử lại
            </Button>
          </CardContent>
        </Card>
      )}

      {!loading && !errorMessage && !hasComparisonData && (
        <Card>
          <CardContent className="flex flex-col items-center py-14 text-center">
            <CalendarRange className="mb-3 h-11 w-11 text-muted-foreground" />
            <h2 className="text-lg font-semibold">Chưa có dữ liệu để so sánh</h2>
            <p className="mt-1 max-w-xl text-muted-foreground">
              {data?.message || "Hãy thử chọn thêm năm hoặc thay đổi bộ lọc."}
            </p>
          </CardContent>
        </Card>
      )}

      {!loading && !errorMessage && hasComparisonData && data && (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <SummaryCard
              icon={<CalendarRange className="h-5 w-5" />}
              label="Số mùa vụ"
              value={`${summary.count}`}
              detail="Tổ hợp năm và mùa vụ"
            />
            <SummaryCard
              icon={<Scale className="h-5 w-5" />}
              label="Sản lượng mùa vụ gốc"
              value={`${numberFormatter.format(summary.baseline?.totalQuantity ?? 0)} kg`}
              detail={summary.baseline ? `${summary.baseline.seasonName} ${summary.baseline.year}` : "Chưa xác định"}
            />
            <SummaryCard
              icon={<BarChart3 className="h-5 w-5" />}
              label="Sản lượng cao nhất"
              value={`${numberFormatter.format(summary.highest?.totalQuantity ?? 0)} kg`}
              detail={summary.highest ? `${summary.highest.seasonName} ${summary.highest.year}` : "Chưa xác định"}
            />
            <SummaryCard
              icon={<PackageCheck className="h-5 w-5" />}
              label="Tổng lượt lô"
              value={numberFormatter.format(summary.lots)}
              detail="Cộng theo các nhóm mùa vụ"
            />
          </div>

          <SeasonYieldComparisonChart
            data={data.seasons}
            baselineYear={data.baselineYear}
            baselineSeasonCode={data.baselineSeasonCode}
          />
          <SeasonYieldComparisonTable
            data={data.seasons}
            baselineYear={data.baselineYear}
            baselineSeasonCode={data.baselineSeasonCode}
          />
        </>
      )}
    </div>
  );
}

function SummaryCard({
  icon,
  label,
  value,
  detail,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <Card>
      <CardContent>
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="mt-2 text-2xl font-bold tracking-tight">{value}</p>
            <p className="mt-1 text-xs text-muted-foreground">{detail}</p>
          </div>
          <span className="rounded-lg bg-primary/10 p-2 text-primary">{icon}</span>
        </div>
      </CardContent>
    </Card>
  );
}
