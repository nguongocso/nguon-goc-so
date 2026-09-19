import {
  AlertCircle,
  CloudOff,
  LoaderCircle,
  RefreshCw,
  Sprout,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

import { createFarmLog } from "@/api/farmLogApi";
import { getInputMaterials } from "@/api/inputMaterialApi";
import { HelpButton } from "@/components/help/HelpButton";
import { getProductionLots } from "@/api/productionLotApi";
import { CreateFarmLogForm } from "@/components/farm-log/CreateFarmLogForm";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useOfflineSync } from "@/hooks/useOfflineSync";
import {
  MAX_OFFLINE_EVENTS,
  getOfflineQueueCount,
} from "@/services/offlineQueue";
import {
  layDanhMucVatTu,
  layLoDuocPhanCong,
  luuDanhMucHoatDong,
  luuDanhMucVatTu,
  luuLoDuocPhanCong,
  kiemTraHanTatCaDanhMuc,
  type HanTatCaDanhMuc,
  type VatTuCache,
} from "@/lib/offline/farmLogDb";
import { HOAT_DONG_CANH_TAC_OPTIONS } from "@/utils/farmLogActivity";
import type { CreateFarmLogRequest, FarmActivityType } from "@/types/farmLog";
import type { ProductionLot } from "@/types/productionLot";

const ALLOWED_STATUSES: ProductionLot["status"][] = ["APPROVED", "HARVESTED"];

const CreateFarmLogPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const requestedProductionLotId = searchParams.get("productionLotId") ?? "";
  const requestedActivityType = (searchParams.get("activityType") as FarmActivityType) || undefined;
  const requestedMilestoneId = searchParams.get("milestoneId")
    ? Number(searchParams.get("milestoneId"))
    : undefined;

  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [danhMucVatTu, setDanhMucVatTu] = useState<VatTuCache[]>([]);
  const [hanDanhMuc, setHanDanhMuc] = useState<HanTatCaDanhMuc | null>(null);
  const [soBanGhiCho, setSoBanGhiCho] = useState(0);
  const { isOnline, farmLogPendingCount, isSyncing } = useOfflineSync();

  /**
   * Tải danh sách lô và danh mục vật tư/hoạt động để dùng ngoại tuyến
   * (NCL-10-CN-012 CV-01: tải sẵn khi còn mạng, TTL 7 ngày).
   */
  const lamMoiDanhMucNgoaiTuyen = useCallback(async (lots: ProductionLot[]) => {
    try {
      const hopLe = lots.filter((lot) => ALLOWED_STATUSES.includes(lot.status));
      await luuLoDuocPhanCong(
        hopLe.map((lot) => ({ id: lot.id, ten: lot.name, trangThai: lot.status })),
      );
      const vatTu = await getInputMaterials({ isActive: true, size: 1000 });
      const danhSachVatTu: VatTuCache[] = (vatTu.content ?? []).map((v) => ({
        id: v.id,
        ten: v.name,
        donVi: v.unit ?? "",
        nhomVatTu: v.materialGroup,
        soNgayCachLy: v.quarantineDays ?? 0,
      }));
      await luuDanhMucVatTu(danhSachVatTu);
      setDanhMucVatTu(danhSachVatTu);
      await luuDanhMucHoatDong(
        HOAT_DONG_CANH_TAC_OPTIONS.map((o) => ({ ma: o.value, nhan: o.label })),
      );
    } catch {
      // Danh mục tải sẵn là tiện ích bổ sung: lỗi thì dùng cache cũ.
      try {
        setDanhMucVatTu(await layDanhMucVatTu());
      } catch {
        // Bỏ qua
      }
    }
  }, []);

  const taiDanhSachCho = useCallback(async () => {
    try {
      setHanDanhMuc(await kiemTraHanTatCaDanhMuc());
    } catch {
      // IndexedDB lỗi: vẫn hiển thị form, form sẽ báo khi lưu tạm
    }
    try {
      // Hàng chờ chung (localStorage): đếm đồng bộ, không phụ thuộc IndexedDB.
      setSoBanGhiCho(getOfflineQueueCount());
    } catch {
      // localStorage lỗi: giữ số cũ
    }
  }, []);

  const loadProductionLots = useCallback(async () => {
    setIsLoading(true);
    setLoadError("");

    try {
      const data = await getProductionLots();
      setProductionLots(data);
      await lamMoiDanhMucNgoaiTuyen(data);
    } catch {
      // Mất mạng hoặc lỗi tải: dùng lô đã lưu trên thiết bị.
      try {
        const cache = await layLoDuocPhanCong();
        if (cache.length > 0) {
          setProductionLots(
            cache.map(
              (lo) =>
                ({
                  id: lo.id,
                  name: lo.ten,
                  status: lo.trangThai,
                }) as ProductionLot,
            ),
          );
          setLoadError("");
        } else {
          setLoadError("Không thể tải danh sách lô sản xuất. Vui lòng thử lại.");
        }
      } catch {
        setLoadError("Không thể tải danh sách lô sản xuất. Vui lòng thử lại.");
      }
      try {
        setDanhMucVatTu(await layDanhMucVatTu());
      } catch {
        // Bỏ qua
      }
    } finally {
      await taiDanhSachCho();
      setIsLoading(false);
    }
  }, [lamMoiDanhMucNgoaiTuyen, taiDanhSachCho]);

  useEffect(() => {
    void loadProductionLots();
  }, [loadProductionLots]);

  // Khi có mạng trở lại: làm mới danh mục để gia hạn TTL 7 ngày.
  // Bỏ qua lần render đầu vì đã tải ở effect trên.
  const daTaiLanDau = useRef(false);
  useEffect(() => {
    if (!daTaiLanDau.current) {
      daTaiLanDau.current = true;
      return;
    }
    if (isOnline) void loadProductionLots();
  }, [isOnline, loadProductionLots]);

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

  // Ngoại tuyến mà danh mục hết hạn hoặc không có lô nào: chặn ghi mới
  // (đúng Precondition spec: "đã đồng bộ danh mục khi còn mạng").
  const hetHanDanhMuc = !isOnline && hanDanhMuc !== null && !hanDanhMuc.conHan;
  const hangChoDay = soBanGhiCho >= MAX_OFFLINE_EVENTS;
  const chanForm =
    hetHanDanhMuc || (!isOnline && eligibleProductionLots.length === 0);

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
        </div>
      </header>

      {!isOnline && (
        <div className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          <CloudOff className="h-4 w-4 shrink-0" />
          <span>Đang ngoại tuyến — bản ghi sẽ được lưu tạm trên thiết bị và đồng bộ khi có mạng.</span>
        </div>
      )}

      {farmLogPendingCount > 0 && (
        <div className="flex items-center gap-2 rounded-lg border border-sky-300 bg-sky-50 p-3 text-sm text-sky-800">
          <RefreshCw className={`h-4 w-4 shrink-0 ${isSyncing ? 'animate-spin' : ''}`} />
          <span>
            {isSyncing
              ? 'Đang tự động đồng bộ nhật ký...'
              : `Có ${farmLogPendingCount} nhật ký chờ đồng bộ. Hệ thống sẽ tự động đồng bộ khi có mạng, bạn không cần thao tác gì thêm.`}
          </span>
        </div>
      )}

      {hetHanDanhMuc && (
        <div className="flex items-center gap-2 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>
            Danh mục tải sẵn đã hết hạn (quá 7 ngày
            {hanDanhMuc && hanDanhMuc.thieu.length > 0 ? `: ${hanDanhMuc.thieu.join(', ')}` : ''}).
            Vui lòng kết nối mạng để tải lại trước khi ghi.
          </span>
        </div>
      )}

      {hangChoDay && (
        <div className="flex items-center gap-2 rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>
            Hàng chờ đã đầy ({MAX_OFFLINE_EVENTS} bản ghi). Vui lòng kết nối mạng để
            đồng bộ trước khi lưu tạm thêm.
          </span>
        </div>
      )}

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
      ) : chanForm ? (
        <Card className="border-slate-200 bg-white shadow-sm">
          <CardContent className="grid min-h-80 place-items-center p-8 text-center">
            <div className="max-w-md">
              <CloudOff className="mx-auto size-10 text-slate-300" />
              <h2 className="mt-4 text-lg font-bold">
                Chưa thể ghi nhật ký lúc này
              </h2>
              <p className="mt-2 text-sm leading-6 text-slate-500">
                Vui lòng kết nối mạng để tải danh mục mới nhất rồi tiếp tục.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <CreateFarmLogForm
          productionLots={eligibleProductionLots}
          initialProductionLotId={initialProductionLotId}
          initialActivityType={requestedActivityType}
          initialMilestoneId={requestedMilestoneId}
          onCancel={() => navigate(-1)}
          onSubmit={handleSubmit}
          onSuccess={(log) => {
            navigate(`/production-lots/${log.productionLotId}?tab=farmlogs`);
          }}
          isOnline={isOnline}
          danhSachVatTuNgoaiTuyen={danhMucVatTu}
        />
      )}
    </div>
  );
};

export default CreateFarmLogPage;