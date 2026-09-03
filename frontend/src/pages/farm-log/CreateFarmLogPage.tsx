import {
  AlertCircle,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
  Sprout,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

import { createFarmLog } from "@/api/farmLogApi";
import { HelpButton } from "@/components/help/HelpButton";
import { getProductionLots } from "@/api/productionLotApi";
import { CreateFarmLogForm } from "@/components/farm-log/CreateFarmLogForm";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { CreateFarmLogRequest } from "@/types/farmLog";
import type { ProductionLot } from "@/types/productionLot";

const ALLOWED_STATUSES: ProductionLot["status"][] = ["APPROVED", "HARVESTED"];

const CreateFarmLogPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const requestedProductionLotId = searchParams.get("productionLotId") ?? "";

  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const loadProductionLots = useCallback(async () => {
    setIsLoading(true);
    setLoadError("");

    try {
      setProductionLots(await getProductionLots());
    } catch {
      setLoadError("Không thể tải danh sách lô sản xuất. Vui lòng thử lại.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProductionLots();
  }, [loadProductionLots]);

  const eligibleProductionLots = useMemo(
    () => productionLots.filter((lot) => ALLOWED_STATUSES.includes(lot.status)),
    [productionLots],
  );

  const initialProductionLotId = useMemo(() => {
    if (!requestedProductionLotId) return undefined;

    return eligibleProductionLots.some(
      (lot) => lot.id === requestedProductionLotId,
    )
      ? requestedProductionLotId
      : undefined;
  }, [eligibleProductionLots, requestedProductionLotId]);

  const requestedLotIsInvalid =
    Boolean(requestedProductionLotId) && !isLoading && !initialProductionLotId;

  const handleSubmit = async (payload: CreateFarmLogRequest) => {
    const result = await createFarmLog(payload);
    toast.success("Lưu nhật ký canh tác thành công");

    return result;
  };

  // Trường hợp lô đã chọn không hợp lệ
  if (!isLoading && !loadError && requestedLotIsInvalid) {
    return (
      <div className="space-y-6">
        <Card className="border-amber-200 bg-amber-50 shadow-sm">
          <CardContent className="grid min-h-80 place-items-center p-8 text-center">
            <div className="max-w-md">
              <AlertCircle className="mx-auto size-10 text-amber-600" />
              <h2 className="mt-4 text-lg font-bold text-amber-800">
                Lô không hợp lệ
              </h2>
              <p className="mt-2 text-sm leading-6 text-amber-700">
                Lô sản xuất được chọn không tồn tại hoặc chưa đủ điều kiện ghi
                nhật ký. Vui lòng quay lại và chọn lô hợp lệ.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Ghi nhật ký canh tác
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Ghi nhận hoạt động thực tế, vật tư sử dụng và ngày thực hiện cho lô sản xuất.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <HelpButton screenKey="farm-log-create" />
          <div className="flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-800">
            <ShieldCheck className="size-4 text-blue-600" />
            <span>Người ghi sự kiện · VT-03</span>
          </div>
        </div>
      </header>

      {isLoading ? (
        <Card className="border-slate-200 bg-white shadow-sm">
          <CardContent className="grid min-h-80 place-items-center p-8 text-center">
            <div>
              <LoaderCircle className="mx-auto size-8 animate-spin text-emerald-700" />
              <p className="mt-4 font-semibold">
                Đang tải danh sách lô sản xuất...
              </p>
            </div>
          </CardContent>
        </Card>
      ) : loadError ? (
        <Card className="border-red-200 bg-white shadow-sm">
          <CardContent className="grid min-h-80 place-items-center p-8 text-center">
            <div>
              <p className="font-semibold text-red-700">{loadError}</p>
              <Button
                type="button"
                variant="outline"
                className="mt-4"
                onClick={() => void loadProductionLots()}
              >
                <RefreshCw className="size-4" />
                Thử lại
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : eligibleProductionLots.length === 0 ? (
        <Card className="border-slate-200 bg-white shadow-sm">
          <CardContent className="grid min-h-80 place-items-center p-8 text-center">
            <div className="max-w-md">
              <Sprout className="mx-auto size-10 text-slate-300" />
              <h2 className="mt-4 text-lg font-bold">
                Chưa có lô đủ điều kiện
              </h2>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Chỉ có thể ghi nhật ký cho lô đã duyệt hoặc đã thu hoạch trong
                tổ chức của bạn.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <CreateFarmLogForm
          productionLots={eligibleProductionLots}
          initialProductionLotId={initialProductionLotId}
          onCancel={() => navigate(-1)}
          onSubmit={handleSubmit}
          onSuccess={(log) => {
            navigate(`/production-lots/${log.productionLotId}?tab=farmlogs`);
          }}
        />
      )}
    </div>
  );
};

export default CreateFarmLogPage;