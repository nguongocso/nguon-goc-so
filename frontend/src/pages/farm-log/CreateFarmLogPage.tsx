import {
  AlertCircle,
  ArrowLeft,
  ClipboardPenLine,
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
      <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-950 md:px-8">
        <div className="mx-auto max-w-7xl">
          <button
            type="button"
            className="mb-7 inline-flex items-center gap-2 text-sm font-semibold text-emerald-800 hover:text-emerald-950"
            onClick={() => void navigate("/production-lots")}
          >
            <ArrowLeft className="size-4" />
            Quay lại danh sách lô
          </button>

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
                <Button
                  type="button"
                  variant="outline"
                  className="mt-5 border-amber-300 text-amber-800 hover:bg-amber-100"
                  onClick={() => navigate("/production-lots")}
                >
                  Quay lại danh sách lô
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-950 md:px-8">
      <div className="mx-auto max-w-7xl">
        <button
          type="button"
          className="mb-7 inline-flex items-center gap-2 text-sm font-semibold text-emerald-800 hover:text-emerald-950"
          onClick={() => void navigate("/production-lots")}
        >
          <ArrowLeft className="size-4" />
          Quay lại danh sách lô
        </button>

        <header className="mb-8 flex flex-col justify-between gap-5 md:flex-row md:items-end">
          <div>
            <p className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">
              <ClipboardPenLine className="size-4" />
              Nhật ký canh tác
            </p>
            <h1 className="text-3xl font-bold tracking-tight">
              Ghi nhật ký canh tác
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
              Ghi nhận hoạt động thực tế, vật tư sử dụng và ngày thực hiện cho
              lô sản xuất.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <HelpButton screenKey="farm-log-create" />
            <div className="flex items-center gap-3 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3">
              <ShieldCheck className="size-5 text-blue-700" />
              <div>
                <p className="text-xs font-bold text-blue-950">
                  Quyền ghi nhật ký
                </p>
                <p className="mt-0.5 text-xs text-blue-700">
                  Người ghi sự kiện · VT-03
                </p>
              </div>
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
                <Button
                  type="button"
                  variant="outline"
                  className="mt-5"
                  onClick={() => void navigate("/production-lots")}
                >
                  Quay lại danh sách lô
                </Button>
              </div>
            </CardContent>
          </Card>
        ) : (
          <CreateFarmLogForm
            productionLots={eligibleProductionLots}
            initialProductionLotId={initialProductionLotId}
            onCancel={() => navigate(-1)}
            onSubmit={handleSubmit}
          />
        )}
      </div>
    </main>
  );
};

export default CreateFarmLogPage;