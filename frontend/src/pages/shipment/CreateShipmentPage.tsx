import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Boxes, Plus, AlertTriangle, Info, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { HelpButton } from "@/components/help/HelpButton";
import { useAuth } from "@/hooks/useAuth";
import { getRemainingCodes } from "@/api/codeRangeApi";
import { createShipment } from "@/api/shipmentApi";
import { getProductionLotById } from "@/api/productionLotApi";
import type { RemainingCodesResponse } from "@/types/codeRange";
import type { ProductionLot } from "@/types/productionLot";

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
  const [remainingCodes, setRemainingCodes] = useState<RemainingCodesResponse | null>(null);
  const [remainingLoading, setRemainingLoading] = useState(false);
  const [loadingLot, setLoadingLot] = useState(true);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      totalQuantity: undefined,
      packagingInfo: "",
    },
  });

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

    fetchLot();

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

    const remainingCount = remainingCodes?.remainingCount ?? 0;
    const hasCodeRange = remainingCodes?.hasCodeRange ?? false;

    if (remainingCodes !== null && (!hasCodeRange || remainingCount < data.totalQuantity)) {
      toast.error("Số lượng lô hàng vượt quá số mã truy xuất còn lại của tổ chức!");
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
      const msg = error.response?.data?.message || "Tạo lô hàng thất bại. Vui lòng thử lại.";
      toast.error(msg);
    }
  };

  const remainingCount = remainingCodes?.remainingCount ?? 0;
  const totalLimit = remainingCodes?.totalLimit ?? 0;
  const hasCodeRange = remainingCodes?.hasCodeRange ?? false;
  const isExhausted = remainingCodes !== null && (!hasCodeRange || remainingCount <= 0);

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
            Lô sản xuất: <span className="font-semibold text-slate-900">{lot?.name || lot?.code}</span>
            {lot?.code && <span className="ml-1 text-xs text-muted-foreground">({lot.code})</span>}
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
            Nhập số lượng sản phẩm đóng gói và quy cách để hệ thống tự động cấp phát dải mã QR truy xuất.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-5 pt-6">
            {/* Box trạng thái dải mã truy xuất */}
            <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 text-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-medium text-slate-700">Dải mã truy xuất còn lại của tổ chức:</span>
                {remainingLoading && !remainingCodes ? (
                  <span className="text-xs text-muted-foreground">Đang tải...</span>
                ) : !hasCodeRange ? (
                  <span className="flex items-center gap-1 font-semibold text-amber-600">
                    <AlertTriangle className="h-4 w-4" />
                    Chưa có dải mã
                  </span>
                ) : (
                  <span className="font-bold text-emerald-600 text-base">
                    {remainingCount.toLocaleString()} / {totalLimit.toLocaleString()} mã
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
                  Đã hết mã truy xuất. Không thể tạo thêm lô hàng. Vui lòng liên hệ quản trị viên để cấp thêm dải mã.
                </p>
              )}

              {!remainingLoading && remainingCodes !== null && !hasCodeRange && (
                <p className="flex items-center gap-1 text-xs text-red-500">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  Tổ chức chưa được cấp dải mã truy xuất. Vui lòng yêu cầu cấp dải mã trước.
                </p>
              )}
            </div>

            {/* Tên lô hàng */}
            <div className="space-y-1.5">
              <Label htmlFor="name" className="text-sm font-medium">
                Tên lô hàng <span className="text-red-500">*</span>
              </Label>
              <Input
                id="name"
                {...register("name")}
                placeholder="Ví dụ: Lô hàng chè Long Cốc T7/2026"
                disabled={isSubmitting || isExhausted}
              />
              {errors.name && (
                <p className="text-sm text-red-500">{errors.name.message}</p>
              )}
            </div>

            {/* Số lượng */}
            <div className="space-y-1.5">
              <Label htmlFor="totalQuantity" className="text-sm font-medium">
                Số lượng sản phẩm / đơn vị <span className="text-red-500">*</span>
              </Label>
              <Input
                id="totalQuantity"
                type="number"
                min={1}
                {...register("totalQuantity", { valueAsNumber: true })}
                placeholder="Nhập số lượng đơn vị"
                disabled={isSubmitting || isExhausted}
              />
              {errors.totalQuantity && (
                <p className="text-sm text-red-500">{errors.totalQuantity.message}</p>
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
                disabled={isSubmitting || isExhausted}
              />
            </div>

            {/* Thông tin sinh mã */}
            <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-blue-900 text-xs leading-relaxed">
              <Info className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
              <span>
                Số lượng mã truy xuất (QR Code) sẽ được hệ thống sinh tương ứng với số lượng bạn nhập. Hãy đảm bảo số lượng không vượt quá hạn mức dải mã của tổ chức.
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
                disabled={isSubmitting || isExhausted}
              >
                <Plus className="h-4 w-4 mr-1.5" />
                {isSubmitting ? "Đang tạo..." : "Tạo lô hàng"}
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  );
};

export default CreateShipmentPage;
