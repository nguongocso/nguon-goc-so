import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import {
  CheckCircle2,
  ClipboardList,
  Clock,
  FlaskConical,
  LoaderCircle,
  Package,
  AlertTriangle,
  Trash2,
  Info,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert";
import { getProductionLotById } from "@/api/productionLotApi";
import { getInspectionRequests } from "@/api/certificationApi";
import type { ProductionLot } from "@/types/productionLot";
import type { InspectionRequestListItem, InspectionValidityStatus } from "@/types/certification";
import { PRODUCTION_LOT_STATUS_LABELS } from "@/components/production-lot/ProductionLotStatusBadge";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { ProcessFailedLotDialog } from "@/components/production-lot/ProcessFailedLotDialog";
import { DisposeLotDialog } from "@/components/production-lot/DisposeLotDialog";
import { ReInspectionDialog } from "@/components/production-lot/ReInspectionDialog";
import { disposeProductionLot } from "@/api/productionLotApi";
import type { DisposeProductionLotRequest } from "@/types/productionLot";

/**
 * Mục chỉ tiêu hiển thị trong khối Hiệu lực kết quả kiểm nghiệm (NCL-11-CN-004).
 */
interface ValidityCriterionItem {
  id?: string | number;
  name: string;
  resultText: string;
  status: InspectionValidityStatus;
  expiryDate: string | null;
  daysRemaining: number | null;
  daysOverdue: number | null;
}

/**
 * Trạng thái kiểm nghiệm suy diễn từ dữ liệu kiểm nghiệm.
 * Không phải enum của ProductionLotStatus.
 */
type DerivedInspectionStatus =
  | "NOT_INSPECTED"
  | "PASSED"
  | "FAILED"
  | "RE_INSPECTION_PENDING";

const formatDate = (dateStr: string | null | undefined) => {
  if (!dateStr) return "—";
  try {
    return new Date(dateStr + "T00:00:00").toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  } catch {
    return dateStr;
  }
};

/**
 * Suy diễn trạng thái kiểm nghiệm từ danh sách yêu cầu kiểm nghiệm.
 * Quy tắc:
 * - Không có yêu cầu nào → NOT_INSPECTED
 * - Yêu cầu mới nhất PASSED → PASSED
 * - Yêu cầu mới nhất FAILED → FAILED
 * - Yêu cầu mới nhất PENDING → RE_INSPECTION_PENDING
 */
const deriveInspectionStatus = (
  requests: InspectionRequestListItem[],
): DerivedInspectionStatus => {
  if (requests.length === 0) return "NOT_INSPECTED";

  // Sắp xếp theo thỉ số failedRatio giảm dần (yêu cầu gần nhất có failedRatio cao nhất)
  // Hoặc dùng criteriaCount như một tiêu chí để sắp xếp
  const sorted = [...requests].sort(
    (a, b) => b.failedRatio - a.failedRatio,
  );

  const latest = sorted[0];

  switch (latest.status) {
    case "PASSED":
      return "PASSED";
    case "FAILED":
      return "FAILED";
    case "PENDING":
      return "RE_INSPECTION_PENDING";
    default:
      return "NOT_INSPECTED";
  }
};

export const ProductionLotInspectionPage: React.FC = () => {
  const { id: rawId, lotId } = useParams<{ id?: string; lotId?: string }>();
  const id = rawId || lotId;
  const navigate = useNavigate();
  const canInspect = usePermission(ROLE_ACCESS.inspectionRequest);

  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [inspectionRequests, setInspectionRequests] = useState<InspectionRequestListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [inspectionLoading, setInspectionLoading] = useState(true);

  // Dialog states
  const [processDialogOpen, setProcessDialogOpen] = useState(false);
  const [disposeDialogOpen, setDisposeDialogOpen] = useState(false);
  const [reInspectionDialogOpen, setReInspectionDialogOpen] = useState(false);

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Lô sản xuất", href: "/production-lots" },
    { label: lot?.name || "Chi tiết" },
  ]);

  const fetchLot = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getProductionLotById(id);
      setLot(data);
    } catch {
      toast.error("Không thể tải thông tin lô sản xuất");
    } finally {
      setLoading(false);
    }
  }, [id]);

  const fetchInspectionRequests = useCallback(async () => {
    if (!id) return;
    try {
      setInspectionLoading(true);
      const data = await getInspectionRequests({ lotId: id });
      setInspectionRequests(data.items);
    } catch {
      // Không hiển thị lỗi — inspection history là phần bổ sung
      setInspectionRequests([]);
    } finally {
      setInspectionLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void fetchLot();
    void fetchInspectionRequests();
  }, [fetchLot, fetchInspectionRequests]);

  const inspectionStatus = deriveInspectionStatus(inspectionRequests);
  const isFailed = inspectionStatus === "FAILED";
  const isDisposed = lot?.status === "DISPOSED";

  // Xử lý loại bỏ lô
  const handleDispose = async (lotId: string, payload: DisposeProductionLotRequest) => {
    try {
      await disposeProductionLot(lotId, payload);
      toast.success("Đã loại bỏ lô sản xuất.");
      // Refresh dữ liệu
      await fetchLot();
    } catch (error: any) {
      const message =
        error.response?.data?.message || "Không thể loại bỏ lô sản xuất.";
      toast.error(message);
      throw error;
    }
  };

  // Chuyển hướng đến trang tạo yêu cầu kiểm nghiệm lại
  const handleNavigateToCreateInspection = (lotId: string) => {
    navigate(`/production-lots/${lotId}/inspection-requests/create`);
  };

  // Danh sách chi tiết các chỉ tiêu cảnh báo (sắp hết hạn hoặc đã hết hạn)
  const warningValidityItems = React.useMemo<ValidityCriterionItem[]>(() => {
    if (!lot?.inspectionValidity) return [];

    // 1. Ưu tiên từ criteria nếu backend có trả
    if (lot.inspectionValidity.criteria && lot.inspectionValidity.criteria.length > 0) {
      const warningCriteria = lot.inspectionValidity.criteria
        .filter((c) => c.status === "EXPIRING" || c.status === "EXPIRED")
        .map((c) => ({
          id: c.criterionId || c.criterionCode || c.criterionName,
          name: c.criterionName,
          resultText: c.passed ? "Đạt" : "Không đạt",
          status: c.status,
          expiryDate: c.expiryDate ?? null,
          daysRemaining: c.daysRemaining ?? null,
          daysOverdue: c.daysOverdue ?? null,
        }));
      if (warningCriteria.length > 0) return warningCriteria;
    }

    // 2. Fallback từ expiringCriteria / expiredCriteria mảng string
    const fallbackItems: ValidityCriterionItem[] = [];
    if (lot.inspectionValidity.status === "EXPIRING" && lot.inspectionValidity.expiringCriteria) {
      for (const critName of lot.inspectionValidity.expiringCriteria) {
        fallbackItems.push({
          id: critName,
          name: critName,
          resultText: "Đạt",
          status: "EXPIRING",
          expiryDate: lot.inspectionValidity.earliestExpiryDate ?? null,
          daysRemaining: lot.inspectionValidity.daysRemaining ?? null,
          daysOverdue: null,
        });
      }
    } else if (lot.inspectionValidity.status === "EXPIRED" && lot.inspectionValidity.expiredCriteria) {
      for (const critName of lot.inspectionValidity.expiredCriteria) {
        fallbackItems.push({
          id: critName,
          name: critName,
          resultText: "Đạt",
          status: "EXPIRED",
          expiryDate: lot.inspectionValidity.earliestExpiryDate ?? null,
          daysRemaining: null,
          daysOverdue: lot.inspectionValidity.daysOverdue ?? null,
        });
      }
    }
    return fallbackItems;
  }, [lot?.inspectionValidity]);

  const hasExpiredCriterion = useMemo(() => {
    if (lot?.inspectionValidity?.status === "EXPIRED") return true;
    return warningValidityItems.some((item) => item.status === "EXPIRED");
  }, [lot?.inspectionValidity?.status, warningValidityItems]);

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center">
        <LoaderCircle className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!lot) {
    return (
      <div className="flex min-h-[400px] flex-col items-center justify-center gap-4">
        <AlertTriangle className="h-12 w-12 text-muted-foreground" />
        <p className="text-muted-foreground">Không tìm thấy lô sản xuất.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h1 className="text-2xl font-bold">{lot.name}</h1>
            <p className="text-sm text-muted-foreground">
              {lot.productCategoryName} • {lot.organizationName}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {/* Nút tạo Lô hàng — chỉ hiển thị khi lô đạt kiểm nghiệm */}
          {!isFailed && !isDisposed && lot.status !== "CANCELLED" && (
            <Button
              variant="create"
              onClick={() => navigate(`/production-lots/${lot.id}/shipments/create`)}
            >
              <Package className="h-4 w-4 mr-1.5" />
              Tạo lô hàng
            </Button>
          )}
          {/* Nút xử lý lô không đạt */}
          {isFailed && canInspect && !isDisposed && (
            <Button
              variant="default"
              onClick={() => setProcessDialogOpen(true)}
            >
              <AlertTriangle className="h-4 w-4 mr-1.5" />
              Xử lý lô
            </Button>
          )}
        </div>
      </div>

      {/* Cảnh báo lô không đạt kiểm nghiệm */}
      {isFailed && (
        <Alert variant="destructive" className="border-red-200 bg-red-50">
          <AlertTriangle className="h-4 w-4 text-red-600" />
          <AlertTitle className="text-red-800">Không đạt kiểm nghiệm</AlertTitle>
          <AlertDescription className="text-red-700">
            Lô sản xuất này có kết quả kiểm nghiệm Không đạt.
            Không thể tạo Lô hàng. Vui lòng xử lý theo một trong hai hướng:
            Loại bỏ lô hoặc Kiểm nghiệm lại.
          </AlertDescription>
        </Alert>
      )}

      {/* Cảnh báo lô đã loại bỏ */}
      {isDisposed && (
        <Alert className="border-gray-300 bg-gray-50">
          <Trash2 className="h-4 w-4 text-gray-600" />
          <AlertTitle className="text-gray-800">Đã loại bỏ</AlertTitle>
          <AlertDescription className="text-gray-700">
            Lô sản xuất đã bị loại bỏ. Lý do: {lot.disposalReason}.
            Biện pháp xử lý: {lot.handlingMeasure}.
          </AlertDescription>
        </Alert>
      )}

      {/* Thông tin lô */}
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Info className="h-4 w-4" />
              Thông tin lô sản xuất
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-muted-foreground">Trạng thái</p>
                <Badge
                  variant="outline"
                  className={
                    isDisposed
                      ? "bg-gray-100 text-gray-700 border-gray-300"
                      : "bg-emerald-100 text-emerald-800 border-emerald-300"
                  }
                >
                  {PRODUCTION_LOT_STATUS_LABELS[lot.status] || lot.status}
                </Badge>
              </div>
              <div>
                <p className="text-muted-foreground">Nông sản</p>
                <p className="font-medium">{lot.productCategoryName || "—"}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Vùng trồng</p>
                <p className="font-medium">{lot.farmAreaName || "—"}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Số lượng dự kiến</p>
                <p className="font-medium">
                  {lot.expectedQuantity} {lot.expectedQuantityUnit}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground">Ngày gieo trồng</p>
                <p className="font-medium">{formatDate(lot.plantingDate)}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Ngày thu hoạch</p>
                <p className="font-medium">{formatDate(lot.harvestDate)}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* NCL-11-CN-004: Khối cảnh báo hiệu lực kết quả kiểm nghiệm sắp hết / đã hết */}
        {warningValidityItems.length > 0 &&
          lot.status !== "RECALLED" &&
          lot.status !== "CANCELLED" &&
          lot.status !== "DISPOSED" && (
          <Card
            className={cn(
              "shadow-sm",
              hasExpiredCriterion
                ? "border-rose-300 bg-rose-50/70 text-rose-950"
                : "border-orange-300 bg-orange-50/80 text-orange-950",
            )}
          >
            <CardHeader className="pb-2">
              <CardTitle
                className={cn(
                  "flex items-center justify-center text-center gap-2 text-base font-bold",
                  hasExpiredCriterion ? "text-rose-900" : "text-orange-900",
                )}
              >
                <AlertTriangle
                  className={cn(
                    "h-5 w-5 shrink-0",
                    hasExpiredCriterion ? "text-rose-600" : "text-orange-600",
                  )}
                />
                <span>
                  {hasExpiredCriterion
                    ? "Cảnh báo hiệu lực kết quả kiểm nghiệm đã hết hạn"
                    : "Cảnh báo hiệu lực kết quả kiểm nghiệm sắp hết"}
                </span>
              </CardTitle>
            </CardHeader>
            <CardContent>
              {/* Giao diện Mobile: Card danh sách các chỉ tiêu cảnh báo */}
              <div className="space-y-3 sm:hidden">
                {warningValidityItems.map((item, idx) => (
                  <div
                    key={item.id ?? idx}
                    className={cn(
                      "p-3 rounded-lg bg-white/80 space-y-2 text-sm shadow-xs border",
                      item.status === "EXPIRED"
                        ? "border-rose-200/90"
                        : "border-orange-200/80",
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span
                        className={cn(
                          "font-medium break-words",
                          item.status === "EXPIRING"
                            ? "text-orange-900 font-semibold"
                            : item.status === "EXPIRED"
                            ? "text-rose-800 font-semibold"
                            : "text-foreground",
                        )}
                      >
                        {item.name}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>
                        Kết quả:{" "}
                        <span className="font-medium text-emerald-700">
                          {item.resultText}
                        </span>
                      </span>
                      <span>
                        Hết hiệu lực:{" "}
                        <span className="font-medium text-foreground">
                          {formatDate(item.expiryDate)}
                        </span>
                      </span>
                    </div>
                    <div className="text-xs">
                      {item.status === "EXPIRING" && (
                        <span className="font-semibold text-orange-600">
                          Thời gian còn lại:{" "}
                          {item.daysRemaining === 0
                            ? "Hết hạn hôm nay"
                            : `Còn ${item.daysRemaining} ngày`}
                        </span>
                      )}
                      {item.status === "EXPIRED" && (
                        <span className="font-semibold text-rose-600">
                          Thời gian quá hạn:{" "}
                          {item.daysOverdue != null && item.daysOverdue > 0
                            ? `Quá hạn ${item.daysOverdue} ngày`
                            : "Đã hết hạn"}
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              {/* Giao diện Desktop / Tablet: Bảng thông tin chi tiết từng chỉ tiêu cảnh báo */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr
                      className={cn(
                        "border-b text-xs font-semibold text-left",
                        hasExpiredCriterion
                          ? "border-rose-200/80 text-rose-900/80"
                          : "border-orange-200/80 text-orange-900/80",
                      )}
                    >
                      <th className="pb-2">
                        {hasExpiredCriterion
                          ? "Tiêu chí cảnh báo / đã hết hạn"
                          : "Tiêu chí sắp hết hạn"}
                      </th>
                      <th className="pb-2">Kết quả kiểm nghiệm</th>
                      <th className="pb-2">Ngày hết hiệu lực</th>
                      <th className="pb-2">
                        {hasExpiredCriterion
                          ? "Thời gian còn lại / quá hạn"
                          : "Thời gian còn lại"}
                      </th>
                    </tr>
                  </thead>
                  <tbody
                    className={cn(
                      "divide-y",
                      hasExpiredCriterion
                        ? "divide-rose-100/80"
                        : "divide-orange-100/80",
                    )}
                  >
                    {warningValidityItems.map((item, idx) => (
                      <tr key={item.id ?? idx} className="h-10">
                        <td className="py-2.5 font-medium break-words pr-4">
                          <span
                            className={cn(
                              item.status === "EXPIRING"
                                ? "text-orange-900 font-semibold"
                                : item.status === "EXPIRED"
                                ? "text-rose-800 font-semibold"
                                : "text-foreground",
                            )}
                          >
                            {item.name}
                          </span>
                        </td>
                        <td className="py-2.5 font-medium text-emerald-700 pr-4">
                          {item.resultText}
                        </td>
                        <td className="py-2.5 font-medium pr-4">
                          {formatDate(item.expiryDate)}
                        </td>
                        <td className="py-2.5 pr-4">
                          {item.status === "EXPIRING" && (
                            <span className="font-semibold text-orange-600">
                              {item.daysRemaining === 0
                                ? "Hết hạn hôm nay"
                                : `Còn ${item.daysRemaining} ngày`}
                            </span>
                          )}
                          {item.status === "EXPIRED" && (
                            <span className="font-semibold text-rose-600">
                              {item.daysOverdue != null && item.daysOverdue > 0
                                ? `Quá hạn ${item.daysOverdue} ngày`
                                : "Đã hết hạn"}
                            </span>
                          )}
                          {item.status !== "EXPIRING" && item.status !== "EXPIRED" && (
                            <span className="font-medium text-emerald-600">
                              {item.daysRemaining != null
                                ? `Còn ${item.daysRemaining} ngày`
                                : "—"}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* CTA khi có chỉ tiêu hết hiệu lực: hiện nút yêu cầu kiểm nghiệm lại ngay lập tức */}
              {hasExpiredCriterion && (lot.inspectionValidity?.canCreateNewRequest ?? true) && (
                <div
                  className={cn(
                    "mt-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-t pt-3",
                    hasExpiredCriterion
                      ? "border-rose-200/70"
                      : "border-orange-200/60",
                  )}
                >
                  <p className="text-sm font-medium text-rose-600">
                    Kết quả kiểm nghiệm có chỉ tiêu đã hết hiệu lực. Vui lòng tạo yêu cầu kiểm nghiệm lại để tiếp tục xuất lô hàng và kích hoạt tem.
                  </p>
                  <Button
                    size="sm"
                    variant="outline"
                    className="shrink-0 border-emerald-600 text-emerald-700 hover:bg-emerald-50 font-semibold"
                    onClick={() => navigate(`/production-lots/${lot.id}/inspection-requests/create`)}
                  >
                    <ClipboardList className="h-4 w-4 mr-1.5" />
                    Yêu cầu kiểm nghiệm lại
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Trạng thái kiểm nghiệm */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <FlaskConical className="h-4 w-4" />
              Trạng thái kiểm nghiệm
            </CardTitle>
          </CardHeader>
          <CardContent>
            {inspectionLoading ? (
              <div className="flex items-center justify-center py-4">
                <LoaderCircle className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  {inspectionStatus === "PASSED" && (
                    <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300">
                      <CheckCircle2 className="h-3 w-3 mr-1" />
                      Đạt kiểm nghiệm
                    </Badge>
                  )}
                  {inspectionStatus === "FAILED" && (
                    <Badge className="bg-red-100 text-red-800 border-red-300">
                      <AlertTriangle className="h-3 w-3 mr-1" />
                      Không đạt kiểm nghiệm
                    </Badge>
                  )}
                  {inspectionStatus === "RE_INSPECTION_PENDING" && (
                    <Badge className="bg-amber-100 text-amber-800 border-amber-300">
                      <Clock className="h-3 w-3 mr-1" />
                      Đang kiểm nghiệm lại
                    </Badge>
                  )}
                  {inspectionStatus === "NOT_INSPECTED" && (
                    <Badge variant="outline">
                      <Clock className="h-3 w-3 mr-1" />
                      Chưa kiểm nghiệm
                    </Badge>
                  )}
                </div>
                <p className="text-sm text-muted-foreground">
                  {inspectionStatus === "PASSED" &&
                    "Lô đủ điều kiện tạo Lô hàng."}
                  {inspectionStatus === "FAILED" &&
                    "Lô chưa đủ điều kiện tạo Lô hàng. Cần xử lý."}
                  {inspectionStatus === "RE_INSPECTION_PENDING" &&
                    "Lô đang chờ kết quả kiểm nghiệm lại. Không thể tạo Lô hàng."}
                  {inspectionStatus === "NOT_INSPECTED" &&
                    "Lô chưa có yêu cầu kiểm nghiệm."}
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Lịch sử kiểm nghiệm */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <ClipboardList className="h-4 w-4" />
            Lịch sử kiểm nghiệm
          </CardTitle>
        </CardHeader>
        <CardContent>
          {inspectionLoading ? (
            <div className="flex items-center justify-center py-8">
              <LoaderCircle className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : inspectionRequests.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
              <FlaskConical className="h-8 w-8 mb-2" />
              <p>Chưa có yêu cầu kiểm nghiệm nào.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {inspectionRequests.map((request, index) => (
                <div
                  key={request.testRequestId}
                  className="rounded-lg border p-4"
                >
                  <div className="flex items-center justify-between mb-2">
                    <p className="font-medium">Lần {index + 1}</p>
                    <Badge
                      variant="outline"
                      className={
                        request.status === "PASSED"
                          ? "bg-emerald-100 text-emerald-800 border-emerald-300"
                          : request.status === "FAILED"
                          ? "bg-red-100 text-red-800 border-red-300"
                          : "bg-amber-100 text-amber-800 border-amber-300"
                      }
                    >
                      {request.status === "PASSED"
                        ? "Đạt"
                        : request.status === "FAILED"
                        ? "Không đạt"
                        : "Chờ kết quả"}
                    </Badge>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <FlaskConical className="h-3 w-3" />
                      {request.testingUnit}
                    </div>
                    {request.status !== "PENDING" && (
                      <div className="flex items-center gap-1">
                        <CheckCircle2 className="h-3 w-3" />
                        {request.criteriaCount - request.failedCriteriaCount}/{request.criteriaCount} chỉ tiêu đạt
                      </div>
                    )}
                  </div>
                  {request.failedCriteriaCount > 0 && (
                    <div className="mt-2 text-sm">
                      <p className="text-red-600">
                        {request.failedCriteriaCount} chỉ tiêu không đạt (tỷ lệ: {request.failedRatio}%)
                      </p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Dialogs */}
      <ProcessFailedLotDialog
        open={processDialogOpen}
        lot={lot}
        onClose={() => setProcessDialogOpen(false)}
        onSelectDispose={() => {
          setProcessDialogOpen(false);
          setDisposeDialogOpen(true);
        }}
        onSelectReInspection={() => {
          setProcessDialogOpen(false);
          setReInspectionDialogOpen(true);
        }}
      />

      <DisposeLotDialog
        open={disposeDialogOpen}
        lot={lot}
        onClose={() => setDisposeDialogOpen(false)}
        onDispose={handleDispose}
      />

      <ReInspectionDialog
        open={reInspectionDialogOpen}
        lot={lot}
        onClose={() => setReInspectionDialogOpen(false)}
        onNavigateToCreateInspection={handleNavigateToCreateInspection}
      />
    </div>
  );
};

export default ProductionLotInspectionPage;
