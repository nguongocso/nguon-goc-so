import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Boxes, Plus, AlertTriangle, Hash, Info, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { CodeRangeSupplementDialog } from "@/components/shipment/CodeRangeSupplementDialog";
import { useAuth } from "@/hooks/useAuth";
import { getRemainingCodes } from "@/api/codeRangeApi";
import { createShipment } from "@/api/shipmentApi";
import { getProductionLotById } from "@/api/productionLotApi";
import { checkCanActivateSeal } from "@/api/certificationApi";
import type { RemainingCodesResponse } from "@/types/codeRange";
import type { ProductionLot } from "@/types/productionLot";
import type { CanActivateSealCheck } from "@/types/certification";

const formSchema = z.object({
  name: z.string().min(1, "Vui lòng nhập tên lô hàng"),
  totalQuantity: z
    .number({ invalid_type_error: "Vui lòng nhập số lượng" })
    .int()
    .min(1, "Số lượng phải lớn hơn 0"),
  packagingInfo: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

export const CreateShipmentPage: React.FC = () => {
  const { productionLotId } = useParams<{ productionLotId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [remainingCodes, setRemainingCodes] =
    useState<RemainingCodesResponse | null>(null);
  const [remainingLoading, setRemainingLoading] = useState(false);
  // NCL-04-CN-007: dialog yêu cầu cấp bổ sung dải mã
  const [supplementDialogOpen, setSupplementDialogOpen] = useState(false);
  const [loadingLot, setLoadingLot] = useState(true);
  // NCL-11-CN-005: trạng thái kiểm nghiệm hiệu lực — dùng chung với pre-check
  // GET /production-lots/{lotId}/can-activate-seal (cùng logic backend gate).
  const [inspectionCheck, setInspectionCheck] =
    useState<CanActivateSealCheck | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      totalQuantity: undefined,
      packagingInfo: "",
    },
  });

  /**
   * Trạng thái kiểm nghiệm hiệu lực — được suy diễn từ pre-check
   * `can-activate-seal`, i.e. cùng một nguồn sự thật với backend gate.
   *
   * Đảm bảo frontend phản ánh đúng trạng thái backend: lịch sử FAIL được
   * giữ nguyên nhưng chỉ khiến BLOCK khi chưa có kết quả PASS mới nhất
   * cho tất cả chỉ tiêu (N/N).
   */
  const isInspectionBlocked = !!inspectionCheck && !inspectionCheck.canActivate;
  const isLotDisposed = lot?.status === "DISPOSED";
  const isLotCancelled = lot?.status === "CANCELLED";

  useEffect(() => {
    if (!productionLotId) return;

    const fetchLot = async () => {
      try {
        setLoadingLot(true);
        const data = await getProductionLotById(productionLotId);
        setLot(data);
      } catch (error) {
        toast.error("Không thể tải thông tin lô sản xuất");
      } finally {
        setLoadingLot(false);
      }
    };

    // NCL-11-CN-005: Lấy trạng thái kiểm nghiệm hiệu lực (cùng logic với backend gate)
    const fetchInspectionCheck = async () => {
      try {
        const data = await checkCanActivateSeal(productionLotId);
        setInspectionCheck(data);
      } catch {
        // Không tải được → không block ở frontend; backend vẫn enforce nguyên tắc.
        setInspectionCheck(null);
      }
    };

    void fetchLot();
    void fetchInspectionCheck();

    if (user?.organizationId) {
      setRemainingLoading(true);
      getRemainingCodes(user.organizationId)
        .then(setRemainingCodes)
        .catch(() => setRemainingCodes(null))
        .finally(() => setRemainingLoading(false));
    }
  }, [productionLotId, user?.organizationId]);

  const onSubmit = async (data: FormValues) => {
    if (!productionLotId) return;

    // NCL-11-CN-005: Chặn tạo lô hàng khi lô không đạt kiểm nghiệm
    if (isInspectionBlocked) {
      toast.error(
        inspectionCheck?.reason ||
          "Không thể tạo lô hàng vì lô sản xuất chưa đạt kiểm nghiệm.",
      );
      return;
    }

    if (isLotDisposed) {
      toast.error("Không thể tạo lô hàng vì lô sản xuất đã bị loại bỏ.");
      return;
    }

    if (isLotCancelled) {
      toast.error("Không thể tạo lô hàng vì lô sản xuất đã bị hủy.");
      return;
    }

    const remainingCount = remainingCodes?.remainingCount ?? 0;
    const hasCodeRange = remainingCodes?.hasCodeRange ?? false;

    if (
      remainingCodes !== null &&
      (!hasCodeRange || remainingCount < data.totalQuantity)
    ) {
      toast.error(
        "Số lượng lô hàng vượt quá số mã truy xuất còn lại của tổ chức!",
      );
      return;
    }

    try {
      await createShipment({
        productionLotId,
        name: data.name,
        totalQuantity: data.totalQuantity,
        packagingInfo: data.packagingInfo || undefined,
      });

      toast.success("Tạo lô hàng và sinh mã truy xuất thành công!");
      navigate(`/production-lots/${productionLotId}`);
    } catch (error: any) {
      // NCL-11-CN-005: Xử lý lỗi nghiệp vụ từ backend
      const msg =
        error.response?.data?.message ||
        "Tạo lô hàng thất bại. Vui lòng thử lại.";
      toast.error(msg);
    }
  };

  // NCL-11-CN-005: Kiểm tra xem form có bị khóa không
  const isBlocked = isInspectionBlocked || isLotDisposed || isLotCancelled;

  const remainingCount = remainingCodes?.remainingCount ?? 0;
  const totalLimit = remainingCodes?.totalLimit ?? 0;
  const hasCodeRange = remainingCodes?.hasCodeRange ?? false;
  const isExhausted =
    remainingCodes !== null && (!hasCodeRange || remainingCount <= 0);
  // NCL-04-CN-007: lối tắt yêu cầu cấp bổ sung khi hạn mức dưới 20% hoặc đã hết
  const isNearlyExhausted =
    hasCodeRange && totalLimit > 0 && remainingCount / totalLimit < 0.2;
  const showSupplementLink =
    user?.roleCode === 'VT-02' &&
    !remainingLoading &&
    remainingCodes !== null &&
    (isExhausted || isNearlyExhausted);

  // NCL-04-CN-007: cảnh báo động khi số lượng nhập vượt hạn mức còn lại
  const watchedQuantity = watch("totalQuantity");
  const exceedsQuota =
    remainingCodes !== null &&
    hasCodeRange &&
    typeof watchedQuantity === "number" &&
    !Number.isNaN(watchedQuantity) &&
    watchedQuantity > remainingCount;

  if (loadingLot) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2 text-emerald-600" />
        Đang tải thông tin lô sản xuất...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header trang */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
            <Boxes className="size-6 text-emerald-600" />
            Tạo lô hàng mới
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Lô sản xuất:{" "}
            <span className="font-semibold text-slate-900">
              {lot?.name || lot?.code}
            </span>
            {lot?.code && (
              <span className="ml-1 text-xs text-muted-foreground">
                ({lot.code})
              </span>
            )}
          </p>
        </div>
        <HelpButton screenKey="shipments" />
      </div>

      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-lg font-semibold text-slate-900">
            Thông tin lô hàng xuất xưởng
          </CardTitle>
          <CardDescription>
            Nhập số lượng sản phẩm đóng gói và quy cách để hệ thống tự động cấp
            phát dải mã QR truy xuất.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-5 pt-6">
            {/* Box trạng thái dải mã truy xuất */}
            <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 text-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-700">
                  Dải mã truy xuất còn lại của tổ chức:
                </span>
                {remainingLoading && !remainingCodes ? (
                  <span className="text-xs text-muted-foreground">
                    Đang tải...
                  </span>
                ) : !hasCodeRange ? (
                  <span className="flex items-center gap-1 font-semibold text-amber-600">
                    <AlertTriangle className="h-4 w-4" />
                    Chưa có dải mã
                  </span>
                ) : (
                  <span className="font-bold text-emerald-600 text-base">
                    {remainingCount.toLocaleString()} /{" "}
                    {totalLimit.toLocaleString()} mã
                  </span>
                )}
              </div>

              {!remainingLoading && !remainingCodes && user?.organizationId && (
                <p className="text-xs text-red-500">
                  Không thể tải số lượng mã còn lại.
                </p>
              )}

              {!user?.organizationId && (
                <p className="flex items-center gap-1 text-xs text-red-500">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  Không xác định được tổ chức.
                </p>
              )}

              {!remainingLoading && hasCodeRange && remainingCount <= 0 && (
                <p className="flex items-center gap-1 text-xs text-red-500">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  Đã hết mã truy xuất. Không thể tạo thêm lô hàng. Vui lòng liên
                  hệ quản trị viên để cấp thêm dải mã.
                </p>
              )}

              {!remainingLoading &&
                remainingCodes !== null &&
                !hasCodeRange && (
                  <p className="flex items-center gap-1 text-xs text-red-500">
                    <AlertTriangle className="h-3.5 w-3.5" />
                    Tổ chức chưa được cấp dải mã truy xuất. Vui lòng yêu cầu cấp
                    dải mã trước.
                  </p>
                )}

              {/* NCL-04-CN-007: cảnh báo + lối tắt yêu cầu cấp bổ sung mã */}
              {showSupplementLink && (
                <div className="flex flex-col gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 sm:flex-row sm:items-center sm:justify-between">
                  <p className="flex items-center gap-1 text-xs text-amber-700">
                    <AlertTriangle className="h-3.5 w-3.5" />
                    {isExhausted
                      ? 'Hạn mức đã hết, vui lòng yêu cầu cấp bổ sung mã truy xuất.'
                      : `Hạn mức còn lại dưới 20% (${remainingCount.toLocaleString()}/${totalLimit.toLocaleString()} mã). Nên gửi yêu cầu cấp bổ sung trước khi hết mã.`}
                  </p>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => setSupplementDialogOpen(true)}
                  >
                    <Hash className="h-3.5 w-3.5 mr-1" />
                    Yêu cầu cấp bổ sung mã
                  </Button>
                </div>
              )}
            </div>

            {/* NCL-11-CN-005: Cảnh báo lô không đạt kiểm nghiệm */}
            {isBlocked && (
              <div className="rounded-xl border border-red-200 bg-red-50 p-4">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-medium text-red-800">
                      Không thể tạo lô hàng
                    </p>
                    <p className="mt-1 text-sm text-red-700">
                      {isInspectionBlocked &&
                        (inspectionCheck?.reason ||
                          "Lô sản xuất chưa đạt kiểm nghiệm. Vui lòng xử lý lô trước khi tạo lô hàng.")}
                      {isLotDisposed &&
                        "Lô sản xuất đã bị loại bỏ. Không thể tạo lô hàng."}
                      {isLotCancelled &&
                        "Lô sản xuất đã bị hủy. Không thể tạo lô hàng."}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Tên lô hàng */}
            <div className="space-y-1.5">
              <Label htmlFor="name" className="text-sm font-medium">
                Tên lô hàng <span className="text-red-500">*</span>
              </Label>
              <Input
                id="name"
                {...register("name")}
                placeholder="Ví dụ: Lô hàng chè Long Cốc T7/2026"
                disabled={isSubmitting || isExhausted || isBlocked}
              />
              {errors.name && (
                <p className="text-sm text-red-500">{errors.name.message}</p>
              )}
            </div>

            {/* Số lượng */}
            <div className="space-y-1.5">
              <Label htmlFor="totalQuantity" className="text-sm font-medium">
                Số lượng sản phẩm / đơn vị{" "}
                <span className="text-red-500">*</span>
              </Label>
              <Input
                id="totalQuantity"
                type="number"
                min={1}
                {...register("totalQuantity", { valueAsNumber: true })}
                placeholder="Nhập số lượng đơn vị"
                disabled={isSubmitting || isExhausted || isBlocked}
              />
              {errors.totalQuantity && (
                <p className="text-sm text-red-500">
                  {errors.totalQuantity.message}
                </p>
              )}

              {/* NCL-04-CN-007: cảnh báo động khi số lượng vượt hạn mức còn lại */}
              {exceedsQuota && (
                <div className="flex flex-col gap-2 rounded-lg border border-red-200 bg-red-50 p-3 sm:flex-row sm:items-center sm:justify-between">
                  <p className="flex items-center gap-1.5 text-xs font-medium text-red-700">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    Hạn mức đã hết / không đủ mã. Chỉ còn{' '}
                    <span className="font-bold">
                      {remainingCount.toLocaleString()}
                    </span>{' '}
                    mã truy xuất, vui lòng giảm số lượng hoặc yêu cầu cấp bổ sung
                    mã.
                  </p>
                  {user?.roleCode === 'VT-02' && (
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="border-red-300 text-red-700 hover:bg-red-100"
                      onClick={() => setSupplementDialogOpen(true)}
                    >
                      <Hash className="h-3.5 w-3.5 mr-1" />
                      Yêu cầu cấp bổ sung mã
                    </Button>
                  )}
                </div>
              )}
            </div>

            {/* Thông tin đóng gói */}
            <div className="space-y-1.5">
              <Label htmlFor="packagingInfo" className="text-sm font-medium">
                Thông tin đóng gói / Quy cách (không bắt buộc)
              </Label>
              <Input
                id="packagingInfo"
                {...register("packagingInfo")}
                placeholder="Ví dụ: Túi 500g, đóng thùng 20 túi/thùng"
                disabled={isSubmitting || isExhausted || isBlocked}
              />
            </div>

            {/* Thông tin sinh mã */}
            <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-blue-900 text-xs leading-relaxed">
              <Info className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
              <span>
                Số lượng mã truy xuất (QR Code) sẽ được hệ thống sinh tương ứng
                với số lượng bạn nhập. Hãy đảm bảo số lượng không vượt quá hạn
                mức dải mã của tổ chức.
              </span>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate(`/production-lots/${productionLotId}`)}
                disabled={isSubmitting}
              >
                Hủy
              </Button>
              <Button
                type="submit"
                variant="create"
                disabled={
                  isSubmitting || isExhausted || isBlocked || exceedsQuota
                }
              >
                <Plus className="h-4 w-4 mr-1.5" />
                {isSubmitting ? "Đang tạo..." : "Tạo lô hàng"}
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>

      {/* NCL-04-CN-007: Dialog yêu cầu cấp bổ sung dải mã */}
      <CodeRangeSupplementDialog
        open={supplementDialogOpen}
        onClose={() => setSupplementDialogOpen(false)}
      />
    </div>
  );
};

export default CreateShipmentPage;
