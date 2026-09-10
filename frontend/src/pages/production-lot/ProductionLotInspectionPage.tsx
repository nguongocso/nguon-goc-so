import React, { useCallback, useEffect, useState } from "react";
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
  CalendarClock,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert";
import { getProductionLotById } from "@/api/productionLotApi";
import { getInspectionRequests } from "@/api/certificationApi";
import type { ProductionLot } from "@/types/productionLot";
import type { InspectionRequestListItem } from "@/types/certification";
import { PRODUCTION_LOT_STATUS_LABELS } from "@/components/production-lot/ProductionLotStatusBadge";
import { InspectionValidityBadge } from "@/components/production-lot/ProductionLotStatusBadge";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { ProcessFailedLotDialog } from "@/components/production-lot/ProcessFailedLotDialog";
import { DisposeLotDialog } from "@/components/production-lot/DisposeLotDialog";
import { ReInspectionDialog } from "@/components/production-lot/ReInspectionDialog";
import { disposeProductionLot } from "@/api/productionLotApi";
import type { DisposeProductionLotRequest } from "@/types/productionLot";

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

        {/* NCL-11-CN-004: Khu vực hiệu lực kết quả kiểm nghiệm */}
        {lot.inspectionValidity &&
          lot.status !== "RECALLED" &&
          lot.status !== "CANCELLED" &&
          lot.status !== "DISPOSED" && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <CalendarClock className="h-4 w-4" />
                Hiệu lực kết quả kiểm nghiệm
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-muted-foreground">Trạng thái</p>
                  <div className="mt-1">
                    <InspectionValidityBadge status={lot.inspectionValidity.status} />
                  </div>
                </div>
                <div>
                  <p className="text-muted-foreground">Ngày hết hiệu lực</p>
                  <p className="font-medium">
                    {lot.inspectionValidity.earliestExpiryDate
                      ? formatDate(lot.inspectionValidity.earliestExpiryDate)
                      : "—"}
                  </p>
                </div>
                {lot.inspectionValidity.status === "EXPIRING" && lot.inspectionValidity.daysRemaining != null && (
                  <div>
                    <p className="text-muted-foreground">Còn lại</p>
                    <p className="font-medium text-orange-600">
                      {lot.inspectionValidity.daysRemaining} ngày
                    </p>
                  </div>
                )}
                {lot.inspectionValidity.status === "EXPIRED" && lot.inspectionValidity.daysOverdue != null && (
                  <div>
                    <p className="text-muted-foreground">Quá hạn</p>
                    <p className="font-medium text-rose-600">
                      {lot.inspectionValidity.daysOverdue} ngày
                    </p>
                  </div>
                )}
                {lot.inspectionValidity.status === "EXPIRING" &&
                  lot.inspectionValidity.expiringCriteria &&
                  lot.inspectionValidity.expiringCriteria.length > 0 && (
                    <div className="col-span-2 mt-1 rounded-lg border border-orange-200 bg-orange-50 p-2.5 text-xs text-orange-900">
                      <span className="font-semibold">
                        Tiêu chí sắp hết hiệu lực ({lot.inspectionValidity.expiringCriteria.length}):
                      </span>{" "}
                      {lot.inspectionValidity.expiringCriteria.join(", ")}
                    </div>
                  )}
                {lot.inspectionValidity.status === "EXPIRED" &&
                  lot.inspectionValidity.expiredCriteria &&
                  lot.inspectionValidity.expiredCriteria.length > 0 && (
                    <div className="col-span-2 mt-1 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-xs text-rose-900">
                      <span className="font-semibold">
                        Tiêu chí đã hết hiệu lực ({lot.inspectionValidity.expiredCriteria.length}):
                      </span>{" "}
                      {lot.inspectionValidity.expiredCriteria.join(", ")}
                    </div>
                  )}
                {lot.inspectionValidity.status === "EXPIRED" && lot.inspectionValidity.canCreateNewRequest && (
                  <div className="col-span-2 mt-2">
                    <Button
                      variant="outline"
                      onClick={() => navigate(`/production-lots/${lot.id}/inspection-requests/create`)}
                    >
                      <ClipboardList className="h-4 w-4 mr-1.5" />
                      Tạo yêu cầu kiểm nghiệm mới
                    </Button>
                  </div>
                )}
              </div>
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
