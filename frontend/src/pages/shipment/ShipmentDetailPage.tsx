import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  AlertCircle,
  ArrowLeft,
  BadgeCheck,
  Ban,
  FileJson,
  FileText,
  LoaderCircle,
  Package,
  QrCode,
  ScrollText,
} from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/hooks/useAuth";
import { usePermission } from "@/hooks/usePermission";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { getShipmentById } from "@/api/shipmentApi";
import { getShipmentTimeline } from "@/api/chainEventApi";
import {
  checkDossierEligibility,
  exportDossier,
  exportGs1Dossier,
} from "@/api/dossierApi";
import { deleteDraft } from "@/api/eventValidationApi";
import { useRecallShipment } from "@/hooks/useRecallShipment";
import { activateShipmentStamps } from "@/api/shipmentApi";
import type { Shipment } from "@/types/shipment";
import type { ChainEventResponse } from "@/types/packaging";
import { maskId } from "@/lib/utils";
import { QrCodeGrid } from "@/components/shipment/QrCodeGrid";
import { ShipmentTimelineItem } from "@/components/shipment/ShipmentTimelineItem";
import { ActivateShipmentDialog } from "@/components/shipment/ActivateShipmentDialog";
import { RecallShipmentDialog } from "@/components/shipment/RecallShipmentDialog";
import { DossierIneligibleDialog } from "@/components/shipment/DossierIneligibleDialog";

// ─── Constants ────────────────────────────────────────────────────────────────

const STATUS_LABEL: Record<Shipment["status"], string> = {
  DRAFT: "Nháp",
  CODE_PRINTED: "Đã in mã",
  ACTIVATED: "Đã kích hoạt",
  RECALLED: "Đã thu hồi",
};

const STATUS_CLASS: Record<Shipment["status"], string> = {
  DRAFT: "bg-status-draft/10 text-status-draft border-status-draft/20",
  CODE_PRINTED:
    "bg-status-packaged/10 text-status-packaged border-status-packaged/20",
  ACTIVATED:
    "bg-status-approved/10 text-status-approved border-status-approved/20",
  RECALLED:
    "bg-status-rejected/10 text-status-rejected border-status-rejected/20",
};

const formatDateTime = (value: string) => {
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString("vi-VN");
};

// ─── Component ────────────────────────────────────────────────────────────────

export const ShipmentDetailPage = () => {
  const { lotId, shipmentId } = useParams<{
    lotId: string;
    shipmentId: string;
  }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  // ── Shipment data ──────────────────────────────────────────────────────────
  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [loadingShipment, setLoadingShipment] = useState(true);
  const [shipmentError, setShipmentError] = useState<string | null>(null);

  // ── Timeline data ──────────────────────────────────────────────────────────
  const [timeline, setTimeline] = useState<ChainEventResponse[]>([]);
  const [loadingTimeline, setLoadingTimeline] = useState(false);
  const [timelineError, setTimelineError] = useState<string | null>(null);
  const [timelineLoaded, setTimelineLoaded] = useState(false);

  // ── Action dialogs ─────────────────────────────────────────────────────────
  const [activating, setActivating] = useState(false);
  const [showActivateDialog, setShowActivateDialog] = useState(false);
  const [showRecallDialog, setShowRecallDialog] = useState(false);
  const [ineligibleDialog, setIneligibleDialog] = useState<{
    open: boolean;
    missingDocs: string[];
  }>({ open: false, missingDocs: [] });

  const { recallingShipmentId, recallShipment } = useRecallShipment(() => {
    // Reload shipment after recall
    void loadShipment();
  });

  // ── Permissions ────────────────────────────────────────────────────────────
  const canActivate = user?.roleCode === "VT-02";
  const canRecall = user?.roleCode === "VT-02";
  const canExportGs1 = usePermission(ROLE_ACCESS.gs1DossierExport);

  // ── Loaders ────────────────────────────────────────────────────────────────

  async function loadShipment() {
    if (!shipmentId) return;
    setLoadingShipment(true);
    setShipmentError(null);
    try {
      const data = await getShipmentById(shipmentId);
      setShipment(data);
    } catch (err: any) {
      setShipmentError(
        err.response?.data?.message ??
        "Không thể tải thông tin lô hàng.",
      );
    } finally {
      setLoadingShipment(false);
    }
  };

  const loadTimeline = async () => {
    if (!shipmentId || timelineLoaded) return;
    setLoadingTimeline(true);
    setTimelineError(null);
    try {
      const data = await getShipmentTimeline(shipmentId);
      setTimeline(data);
      setTimelineLoaded(true);
    } catch (err: any) {
      setTimelineError(
        err.response?.data?.message ?? "Không thể tải dòng thời gian.",
      );
    } finally {
      setLoadingTimeline(false);
    }
  };

  useEffect(() => {
    void loadShipment();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shipmentId]);

  // ── Action handlers ────────────────────────────────────────────────────────

  const handleActivate = async (id: string) => {
    setActivating(true);
    try {
      const updated = await activateShipmentStamps(id);
      setShipment(updated);
      toast.success("Kích hoạt tem thành công!");
      setShowActivateDialog(false);
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? "Không thể kích hoạt tem.");
    } finally {
      setActivating(false);
    }
  };

  const handleExportDossier = async () => {
    if (!shipment) return;
    try {
      const checkResult = await checkDossierEligibility(shipment.id);
      if (!checkResult.eligible) {
        setIneligibleDialog({
          open: true,
          missingDocs: checkResult.missingDocuments,
        });
        return;
      }

      toast.loading("Đang tạo hồ sơ...");
      const blob = await exportDossier(shipment.id);
      toast.dismiss();

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `Ho_so_truy_xuat_${shipment.name}_${new Date()
        .toISOString()
        .slice(0, 10)}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      toast.success("Tải hồ sơ thành công");
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? "Có lỗi xảy ra khi xuất hồ sơ.");
    }
  };

  const handleExportGs1Dossier = async () => {
    if (!shipment) return;
    const toastId = toast.loading("Đang tạo hồ sơ GS1...");
    try {
      const { blob, fileName } = await exportGs1Dossier(
        shipment.id,
        "json",
        true,
      );
      toast.dismiss(toastId);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast.success("Tải hồ sơ GS1 thành công");
    } catch (error: any) {
      toast.dismiss(toastId);
      const msg =
        error.response?.data?.message ||
        "Có lỗi xảy ra khi xuất hồ sơ GS1.";

      toast.error(msg);
    }
  };

  const handleDeleteDraft = async () => {
    if (!shipment) return;
    if (!confirm(`Bạn có chắc chắn muốn hủy bản nháp "${shipment.name}"?`))
      return;
    try {
      await deleteDraft(shipment.id);
      toast.success("Hủy bản nháp thành công");
      navigate(-1);
    } catch (err: any) {
      toast.error(err.response?.data?.message ?? "Không thể hủy bản nháp");
    }
  };

  // ── Derived flags ──────────────────────────────────────────────────────────
  const canActivateThis =
    canActivate && shipment?.status === "CODE_PRINTED";
  const canRecallThis =
    canRecall && shipment?.status !== "RECALLED";
  const canDeleteDraft =
    shipment?.status === "DRAFT" || shipment?.status === "CODE_PRINTED";

  // ── Render guards ──────────────────────────────────────────────────────────

  if (loadingShipment) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-muted-foreground">
        <LoaderCircle className="size-9 animate-spin text-emerald-500" />
        <p>Đang tải thông tin lô hàng...</p>
      </div>
    );
  }

  if (shipmentError || !shipment) {
    return (
      <div className="container mx-auto py-10">
        <Button variant="outline" onClick={() => navigate(-1)} className="mb-6">
          <ArrowLeft className="mr-1 h-4 w-4" />
          Quay lại
        </Button>
        <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-5 text-red-700">
          <AlertCircle className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-semibold">Không thể tải dữ liệu</p>
            <p className="mt-1 text-sm">
              {shipmentError ?? "Không tìm thấy lô hàng."}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // ── Main render ────────────────────────────────────────────────────────────

  return (
    <div className="container mx-auto space-y-6 py-6">
      {/* ── Top navigation bar ── */}
      <Button
        variant="outline"
        onClick={() =>
          navigate(`/production-lots/${lotId}`, {
            state: { activeTab: "shipments" },
          })
        }
      >
        <ArrowLeft className="mr-1 h-4 w-4" />
        Quay lại lô sản xuất
      </Button>

      {/* ── Header card ── */}
      <Card className="border-emerald-100 bg-white/80 shadow-sm backdrop-blur-sm">
        <CardContent className="pt-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            {/* Title + meta */}
            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-2xl font-bold text-emerald-800">
                  {shipment.name}
                </h1>
                <Badge
                  variant="outline"
                  className={`border text-xs font-semibold px-2.5 py-0.5 ${STATUS_CLASS[shipment.status]}`}
                >
                  {STATUS_LABEL[shipment.status]}
                </Badge>
              </div>
              <p className="font-mono text-xs text-muted-foreground">
                {maskId(shipment.id)}
              </p>
            </div>

            {/* Action buttons */}
            <div className="flex flex-wrap gap-2">
              {canActivateThis && (
                <Button
                  variant="create"
                  onClick={() => setShowActivateDialog(true)}
                >
                  <BadgeCheck className="mr-1 h-4 w-4" />
                  Kích hoạt
                </Button>
              )}

              <Button variant="outline" onClick={handleExportDossier}>
                <FileText className="mr-1 h-4 w-4" />
                Xuất hồ sơ
              </Button>

              {canExportGs1 && (
                <Button variant="outline" onClick={handleExportGs1Dossier}>
                  <FileJson className="mr-1 h-4 w-4" />
                  Xuất hồ sơ GS1
                </Button>
              )}

              {canRecallThis && (
                <Button
                  variant="outline"
                  className="border-red-300 text-red-600 hover:bg-red-50"
                  onClick={() => setShowRecallDialog(true)}
                >
                  <Ban className="mr-1 h-4 w-4" />
                  Thu hồi
                </Button>
              )}

              {canDeleteDraft && (
                <Button
                  variant="outline"
                  className="border-red-300 text-red-600 hover:bg-red-50"
                  onClick={handleDeleteDraft}
                >
                  Hủy nháp
                </Button>
              )}
            </div>
          </div>

          {/* ── Info grid ── */}
          <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Số lượng
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {shipment.totalQuantity.toLocaleString("vi-VN")}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Số mã QR
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {shipment.traceCodes.length.toLocaleString("vi-VN")}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Quy cách
              </span>
              <p className="mt-1 text-lg font-semibold text-emerald-800">
                {shipment.packagingInfo || "—"}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
              <span className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Ngày tạo
              </span>
              <p className="mt-1 text-sm font-semibold text-emerald-800">
                {formatDateTime(shipment.createdAt)}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* ── Tab content ── */}
      <Tabs defaultValue="info" className="w-full">
        <TabsList className="max-w-full overflow-x-auto overflow-y-hidden rounded-xl border border-emerald-100 bg-white/80 backdrop-blur-sm p-1 gap-1 min-h-11">
          <TabsTrigger
            value="info"
            className="rounded-lg px-4 py-2 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            <ScrollText className="mr-1.5 h-4 w-4" />
            Thông tin
          </TabsTrigger>
          <TabsTrigger
            value="qr"
            className="rounded-lg px-4 py-2 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
          >
            <QrCode className="mr-1.5 h-4 w-4" />
            Mã QR
            {shipment.traceCodes.length > 0 && (
              <span className="ml-1.5 rounded-full bg-emerald-100 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-700 data-[state=active]:bg-white/20 data-[state=active]:text-white">
                {shipment.traceCodes.length}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger
            value="events"
            className="rounded-lg px-4 py-2 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white"
            onClick={() => void loadTimeline()}
          >
            <Package className="mr-1.5 h-4 w-4" />
            Sự kiện
          </TabsTrigger>
        </TabsList>

        {/* ── Tab: Thông tin ── */}
        <TabsContent value="info" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 shadow-sm">
            <CardContent className="pt-6">
              <dl className="divide-y rounded-lg border">
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">Lô sản xuất</dt>
                  <dd className="text-sm font-medium">
                    {shipment.productionLotName || "—"}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">ID lô sản xuất</dt>
                  <dd className="break-all font-mono text-sm font-medium">
                    {maskId(shipment.productionLotId)}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">ID lô hàng</dt>
                  <dd className="break-all font-mono text-sm font-medium">
                    {maskId(shipment.id)}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">Tên lô hàng</dt>
                  <dd className="text-sm font-medium">{shipment.name}</dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">Số lượng</dt>
                  <dd className="text-sm font-medium">
                    {shipment.totalQuantity.toLocaleString("vi-VN")}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">
                    Quy cách đóng gói
                  </dt>
                  <dd className="whitespace-pre-wrap text-sm font-medium">
                    {shipment.packagingInfo || "—"}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">
                    Số mã truy xuất
                  </dt>
                  <dd className="text-sm font-medium">
                    {shipment.traceCodes.length.toLocaleString("vi-VN")}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">Người tạo</dt>
                  <dd className="text-sm font-medium">
                    {shipment.createdByName || "—"}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">Ngày tạo</dt>
                  <dd className="text-sm font-medium">
                    {formatDateTime(shipment.createdAt)}
                  </dd>
                </div>
                <div className="grid gap-1 px-4 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
                  <dt className="text-sm text-muted-foreground">Trạng thái</dt>
                  <dd>
                    <span
                      className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${STATUS_CLASS[shipment.status]}`}
                    >
                      {STATUS_LABEL[shipment.status]}
                    </span>
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Tab: Mã QR ── */}
        <TabsContent value="qr" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 shadow-sm">
            <CardContent className="pt-6">
              <div className="mb-4 flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Tổng số mã:{" "}
                  <span className="font-semibold text-foreground">
                    {shipment.traceCodes.length}
                  </span>
                </p>
              </div>
              <QrCodeGrid traceCodes={shipment.traceCodes} />
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Tab: Sự kiện ── */}
        <TabsContent value="events" className="mt-4">
          <Card className="border-emerald-100 bg-white/80 shadow-sm">
            <CardContent className="pt-6">
              {loadingTimeline && (
                <div className="flex justify-center py-12">
                  <LoaderCircle className="h-8 w-8 animate-spin text-emerald-600" />
                </div>
              )}

              {timelineError && (
                <div className="flex items-start gap-2 rounded-lg bg-red-50 p-4 text-red-600">
                  <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
                  <span>{timelineError}</span>
                </div>
              )}

              {!loadingTimeline && !timelineError && timeline.length === 0 && (
                <div className="flex flex-col items-center py-12 text-muted-foreground">
                  <Package className="mb-3 h-10 w-10 text-gray-300" />
                  <p className="text-lg font-semibold">Chưa có sự kiện</p>
                  <p className="text-sm">
                    Lô hàng này chưa có sự kiện nào được ghi nhận.
                  </p>
                </div>
              )}

              {!loadingTimeline && !timelineError && timeline.length > 0 && (
                <div className="relative py-2">
                  <p className="mb-4 text-sm text-muted-foreground">
                    {timeline.length === 1
                      ? "1 sự kiện"
                      : `${timeline.length} sự kiện`}
                  </p>
                  {timeline.map((event, idx) => (
                    <ShipmentTimelineItem
                      key={event.id}
                      event={event}
                      index={idx}
                      total={timeline.length}
                    />
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ── Dialogs ── */}
      <ActivateShipmentDialog
        shipment={showActivateDialog ? shipment : null}
        isActivating={activating}
        onClose={() => setShowActivateDialog(false)}
        onConfirm={handleActivate}
      />

      <RecallShipmentDialog
        shipment={showRecallDialog ? shipment : null}
        isRecalling={recallingShipmentId === shipment.id}
        onClose={() => setShowRecallDialog(false)}
        onConfirm={async (id, reason) => {
          await recallShipment(id, reason);
          setShowRecallDialog(false);
        }}
      />

      <DossierIneligibleDialog
        open={ineligibleDialog.open}
        onClose={() => setIneligibleDialog({ open: false, missingDocs: [] })}
        missingDocs={ineligibleDialog.missingDocs}
        shipmentName={shipment.name}
      />
    </div>
  );
};
