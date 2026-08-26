import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { getCropAreaAnalysis } from "@/api/cropAreaAnalysisApi";
import type { CropAreaAnalysisResponse } from "@/types/cropAreaAnalysis";
import { CropAreaFilter } from "@/components/report/CropAreaFilter";
import { CropAreaSummaryCards } from "@/components/report/CropAreaSummaryCards";
import { AreaAnalysisTable } from "@/components/report/AreaAnalysisTable";
import { SeasonAnalysisTable } from "@/components/report/SeasonAnalysisTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { toast } from "sonner";
import { MapPinOff, RefreshCw, SearchX } from "lucide-react";
import { HelpButton } from "@/components/help/HelpButton";
import {
  NO_ASSIGNED_AREA_HINT,
  NO_ASSIGNED_AREA_MESSAGE,
} from "@/constants/reportMessages";

export default function CropAreaAnalysisPage() {
  const { user } = useAuth();
  const [data, setData] = useState<CropAreaAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [params, setParams] = useState({});

  const fetchData = async (filterParams: any = {}) => {
    try {
      setLoading(true);
      const result = await getCropAreaAnalysis(filterParams);
      setData(result);
    } catch (error: any) {
      const msg =
        error.response?.data?.message || "Không thể tải dữ liệu phân tích";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleFilter = (filters: any) => {
    setParams(filters);
    fetchData(filters);
  };

  const handleReset = () => {
    setParams({});
    fetchData({});
  };

  // Load initial data
  useEffect(() => {
    fetchData({});
  }, []);

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">
            Phân tích theo vùng trồng và mùa vụ
          </h1>
          <p className="text-sm text-muted-foreground">
            Tổng hợp sản lượng và số lô theo vùng trồng, mùa vụ
          </p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="report-crop-area-analysis" />
          <Button
            variant="outline"
            onClick={() => fetchData(params)}
            disabled={loading}
          >
            <RefreshCw
              className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
            />
            Làm mới
          </Button>
        </div>
      </div>

      <CropAreaFilter
        onFilter={handleFilter}
        onReset={handleReset}
        loading={loading}
        currentUserRole={user?.roleCode}
      />

      {loading ? (
        <div className="flex justify-center py-12">Đang tải...</div>
      ) : data && data.byArea.length === 0 && data.bySeason.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center py-12 text-center">
            {data.message === NO_ASSIGNED_AREA_MESSAGE ? (
              <>
                <MapPinOff className="mb-3 h-10 w-10 text-muted-foreground" />
                <p className="font-medium">{NO_ASSIGNED_AREA_MESSAGE}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {NO_ASSIGNED_AREA_HINT}
                </p>
              </>
            ) : (
              <>
                <SearchX className="mb-3 h-10 w-10 text-muted-foreground" />
                <p className="font-medium">
                  {data.message || "Chưa có dữ liệu phân tích cho bộ lọc hiện tại."}
                </p>
              </>
            )}
          </CardContent>
        </Card>
      ) : data ? (
        <>
          <CropAreaSummaryCards summary={data.summary} />
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <AreaAnalysisTable data={data.byArea} />
            <SeasonAnalysisTable data={data.bySeason} />
          </div>
        </>
      ) : (
        <div className="text-center py-12 text-muted-foreground">
          Không có dữ liệu phân tích
        </div>
      )}
    </div>
  );
}
