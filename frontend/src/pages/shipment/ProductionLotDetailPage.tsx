import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Package, Plus, Sprout } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { getProductionLotById } from "@/api/productionLotApi";
import { ShipmentList } from "@/pages/shipment/ShipmentList";
import { FarmLogList } from "@/components/farm-log/FarmLogList";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { HarvestForm } from "@/components/trace-event/HarvestForm";
import type { ProductionLot } from "@/types/productionLot";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import type { ProductionLotCertification } from "@/types/certification";
import {
  detachCertification,
  getLotCertifications,
} from "@/api/certificationApi";
import { CertificationList } from "@/components/certification/CertificationList";
import { AttachCertificationDialog } from "@/components/certification/AttachCertificationDialog";

// Ánh xạ trạng thái sang tiếng Việt và màu sắc
const STATUS_MAP: Record<string, { label: string; className: string }> = {
  DRAFT: {
    label: "Bản nháp",
    className: "bg-gray-200 text-gray-700",
  },
  PENDING: {
    label: "Chờ duyệt",
    className: "bg-yellow-100 text-yellow-800 border-yellow-300",
  },
  APPROVED: {
    label: "Đã duyệt",
    className: "bg-emerald-100 text-emerald-800 border-emerald-300",
  },
  HARVESTED: {
    label: "Đã thu hoạch",
    className: "bg-lime-100 text-lime-800 border-lime-300",
  },
  PACKAGED: {
    label: "Đã đóng gói",
    className: "bg-sky-100 text-sky-800 border-sky-300",
  },
  SHIPPED: {
    label: "Đang vận chuyển",
    className: "bg-indigo-100 text-indigo-800 border-indigo-300",
  },
  RECALLED: {
    label: "Đã thu hồi",
    className: "bg-red-100 text-red-800 border-red-300",
  },
  COMPLETED: {
    label: "Hoàn thành",
    className: "bg-purple-100 text-purple-800 border-purple-300",
  },
};

const getStatusBadge = (status: string) => {
  const config = STATUS_MAP[status] || {
    label: status,
    className: "bg-gray-100 text-gray-700",
  };
  return (
    <Badge
      variant="outline"
      className={`${config.className} border text-xs font-semibold px-2.5 py-0.5`}
    >
      {config.label}
    </Badge>
  );
};

export const ProductionLotDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const canCreateFarmLog = usePermission(ROLE_ACCESS.farmLogCreate);
  const [lot, setLot] = useState<ProductionLot | null>(null);
  const [loading, setLoading] = useState(true);
  const [showHarvestForm, setShowHarvestForm] = useState(false);
  const [certifications, setCertifications] = useState<
    ProductionLotCertification[]
  >([]);
  const [loadingCerts, setLoadingCerts] = useState(false);
  const [attachDialogOpen, setAttachDialogOpen] = useState(false);

  const loadLot = async () => {
    if (!id) return;
    try {
      const data = await getProductionLotById(id);
      setLot(data);
    } catch (error) {
      toast.error("Không thể tải thông tin lô sản xuất");
    } finally {
      setLoading(false);
    }
  };

  const loadCertifications = async () => {
    if (!id) return;
    try {
      setLoadingCerts(true);
      const data = await getLotCertifications(id);
      setCertifications(data);
    } catch (error: any) {
      toast.error("Không thể tải danh sách chứng nhận");
    } finally {
      setLoadingCerts(false);
    }
  };

  useEffect(() => {
    loadLot();
  }, [id]);

  useEffect(() => {
    if (id) {
      loadCertifications();
    }
  }, [id]);

  const canRecordHarvest =
    user?.roleCode === "VT-02" || user?.roleCode === "VT-03";

  if (loading) {
    return (
      <div className="flex justify-center items-center py-20 text-muted-foreground">
        <Sprout className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
        Đang tải...
      </div>
    );
  }

  if (!lot) {
    return (
      <div className="text-center py-20 text-muted-foreground">
        Không tìm thấy lô sản xuất
      </div>
    );
  }

  const canCreateShipment =
    user?.roleCode === "VT-02" && lot.status === "PACKAGED";
  const canActivateShipment = user?.roleCode === "VT-02";
  const canRecallShipment = user?.roleCode === "VT-02";
  const canRecordPackaging =
    (user?.roleCode === "VT-02" || user?.roleCode === "VT-03") &&
    lot.status === "HARVESTED";
  const canManageCert = user?.roleCode === "VT-02";

  const handleDetach = async (certificationId: string) => {
    if (!id) {
      toast.error("Không tìm thấy ID lô sản xuất");
      return;
    }
    if (!confirm("Bạn có chắc chắn muốn gỡ chứng nhận này?")) return;
    try {
      await detachCertification(id, certificationId);
      toast.success("Gỡ chứng nhận thành công");
      await loadCertifications();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Không thể gỡ chứng nhận");
    }
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-1" />
          Quay lại
        </Button>
        <div className="flex gap-2">
          {canRecordHarvest &&
            lot.status === "APPROVED" &&
            !showHarvestForm && (
              <Button
                onClick={() => setShowHarvestForm(true)}
                variant="create"
              >
                <Sprout className="h-4 w-4 mr-1" />
                Ghi nhận thu hoạch
              </Button>
            )}
          {canRecordPackaging && (
            <Button
              onClick={() =>
                navigate(`/packaging-events/create?productionLotId=${lot.id}`)
              }
              variant="create"
            >
              <Package className="h-4 w-4 mr-1" />
              Ghi đóng gói
            </Button>
          )}
        </div>
      </div>

      {/* Thông tin chính */}
      <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
        <CardHeader className="flex flex-row items-start justify-between pb-4">
          <div>
            <CardTitle className="text-xl font-bold text-emerald-800">
              {lot.name}
            </CardTitle>
            <p className="text-sm text-muted-foreground mt-1">
              Mã lô: {lot.id}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {getStatusBadge(lot.status)}
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Ô thông tin */}
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Sản lượng dự kiến
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.expectedQuantity} {lot.expectedQuantityUnit}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Sản lượng thực tế
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.actualQuantity ? `${lot.actualQuantity} kg` : "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Vùng trồng
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.farmAreaName || "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
                Nông sản
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {lot.productCategoryName || "—"}
              </p>
            </div>
          </div>

          {/* Ngày quan trọng */}
          <div className="mt-6 grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="flex items-center gap-3 rounded-lg bg-emerald-50 p-3">
              <Sprout className="h-5 w-5 text-emerald-600" />
              <div>
                <p className="text-xs text-muted-foreground">Ngày trồng</p>
                <p className="font-medium">
                  {lot.plantingDate
                    ? new Date(lot.plantingDate).toLocaleDateString("vi-VN")
                    : "—"}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-lg bg-amber-50 p-3">
              <Package className="h-5 w-5 text-amber-600" />
              <div>
                <p className="text-xs text-muted-foreground">Ngày thu hoạch</p>
                <p className="font-medium">
                  {lot.harvestDate
                    ? new Date(lot.harvestDate).toLocaleDateString("vi-VN")
                    : "—"}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-lg bg-blue-50 p-3">
              <Package className="h-5 w-5 text-blue-600" />
              <div>
                <p className="text-xs text-muted-foreground">Người tạo</p>
                <p className="font-medium">{lot.createdByName || "—"}</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Harvest Form (nếu bật) */}
      {showHarvestForm && (
        <HarvestForm
          productionLotId={lot.id}
          productionLotName={lot.name}
          onSuccess={() => {
            setShowHarvestForm(false);
            loadLot();
          }}
          onCancel={() => setShowHarvestForm(false)}
        />
      )}

      {/* Tabs chi tiết */}
      <Tabs defaultValue="info" className="w-full">
        <TabsList className="bg-white/80 backdrop-blur-sm border border-emerald-100 p-1 rounded-xl gap-1 min-h-11 max-w-full overflow-x-auto overflow-y-hidden">
          <TabsTrigger
            value="info"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Thông tin chung
          </TabsTrigger>
          <TabsTrigger
            value="farmlogs"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Nhật ký canh tác
          </TabsTrigger>
          <TabsTrigger
            value="shipments"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Lô hàng & Mã QR
          </TabsTrigger>
          <TabsTrigger
            value="certifications"
            className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            Chứng nhận
          </TabsTrigger>
        </TabsList>

        <TabsContent value="info" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
            <CardContent className="pt-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="rounded-lg bg-muted/50 p-4">
                  <span className="text-sm text-muted-foreground">ID</span>
                  <p className="font-mono text-sm">{lot.id}</p>
                </div>
                <div className="rounded-lg bg-muted/50 p-4">
                  <span className="text-sm text-muted-foreground">Ngày tạo</span>
                  <p className="font-medium">
                    {new Date(lot.createdAt).toLocaleString("vi-VN")}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="farmlogs" className="mt-4">
          <FarmLogList
            productionLotId={lot.id}
            productionLotName={lot.name}
            canCreate={canCreateFarmLog}
          />
        </TabsContent>

        <TabsContent value="shipments" className="mt-4">
          <ShipmentList
            productionLotId={lot.id}
            productionLotStatus={lot.status}
            canCreate={canCreateShipment}
            canActivate={canActivateShipment}
            canRecall={canRecallShipment}
          />
        </TabsContent>

        <TabsContent value="certifications" className="mt-4">
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-lg font-semibold text-emerald-800">
                Chứng nhận của lô
              </h2>
              {canManageCert && (
                <Button
                  onClick={() => setAttachDialogOpen(true)}
                  variant="create"
                >
                  <Plus className="h-4 w-4 mr-1" /> Gắn chứng nhận
                </Button>
              )}
            </div>
            <CertificationList
              certifications={certifications}
              onDetach={handleDetach}
              canManage={canManageCert}
              loading={loadingCerts}
            />
          </div>
        </TabsContent>
      </Tabs>

      <AttachCertificationDialog
        open={attachDialogOpen}
        onClose={() => setAttachDialogOpen(false)}
        lotId={id!}
        onSuccess={loadCertifications}
      />
    </div>
  );
};