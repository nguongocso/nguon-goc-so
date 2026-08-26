import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  BadgeCheck,
  ChevronLeft,
  ChevronRight,
  FileText,
  FileJson,
  Plus,
  Ban,
  MoreHorizontal,
  History,
  Eye,
} from "lucide-react";
import { useShipments } from "@/hooks/useShipments";
import { useRecallShipment } from "@/hooks/useRecallShipment";
import type { Shipment, CreateShipmentPayload } from "@/types/shipment";
import { getLocalDateString } from "@/utils/dateTime";
import { CreateShipmentModal } from "@/components/shipment/CreateShipmentModal";
import { ShipmentTimelineDialog } from "@/components/shipment/ShipmentTimelineDialog";
import { ActivateShipmentDialog } from "@/components/shipment/ActivateShipmentDialog";
import { RecallShipmentDialog } from "@/components/shipment/RecallShipmentDialog";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import {
  checkDossierEligibility,
  exportDossier,
  exportGs1Dossier,
} from "@/api/dossierApi";
import { DossierIneligibleDialog } from "@/components/shipment/DossierIneligibleDialog";
import { ShipmentStatusBadge } from "@/components/shipment/ShipmentStatusBadge";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { usePermission } from "@/hooks/usePermission";
import { useDeleteDraftShipment } from "@/hooks/useDeleteDraftShipment";
import { checkCanActivateSeal } from "@/api/certificationApi";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";

interface ShipmentListProps {
  productionLotId: string;
  productionLotStatus: string;
  canCreate: boolean;
  canActivate: boolean;
  canRecall: boolean;
}

export const ShipmentList = ({
  productionLotId,
  productionLotStatus,
  canCreate,
  canActivate,
  canRecall,
}: ShipmentListProps) => {
  const navigate = useNavigate();
  const [modalOpen, setModalOpen] = useState(false);

  const [activatingShipment, setActivatingShipment] = useState<Shipment | null>(
    null,
  );

  const [recallingShipment, setRecallingShipment] = useState<Shipment | null>(
    null,
  );

  const [timelineDialog, setTimelineDialog] = useState<{
    open: boolean;
    shipmentId: string;
    name: string;
  }>({
    open: false,
    shipmentId: "",
    name: "",
  });

  const [ineligibleDialog, setIneligibleDialog] = useState<{
    open: boolean;
    missingDocs: string[];
    shipmentName: string;
  }>({
    open: false,
    missingDocs: [],
    shipmentName: "",
  });

  const canExportGs1 = usePermission(ROLE_ACCESS.gs1DossierExport);

  const {
    shipments,
    isLoading,
    createShipment,
    isCreating,
    activatingShipmentId,
    activateShipment,
    reload,
    page,
    totalPages,
    totalElements,
    setPage,
  } = useShipments(productionLotId);

  const { recallingShipmentId, recallShipment } = useRecallShipment(reload);

  const [pendingDeleteDraft, setPendingDeleteDraft] = useState<Shipment | null>(
    null,
  );
  const { deletingDraft, deleteDraftShipment } = useDeleteDraftShipment(reload);

  const handleCreate = async (payload: CreateShipmentPayload) => {
    await createShipment(payload);
  };

  // Chặn kích hoạt tem khi lô chưa đạt kiểm nghiệm / kết quả hết hiệu lực
  const handleActivateConfirm = async (shipmentId: string) => {
    let check;
    try {
      check = await checkCanActivateSeal(productionLotId);
    } catch {
      toast.error("Không thể kiểm tra điều kiện kích hoạt tem");
      throw new Error("cannot-check-activation");
    }
    if (!check.canActivate) {
      toast.error(check.reason || "Lô chưa đủ điều kiện kích hoạt tem");
      throw new Error("cannot-activate");
    }
    await activateShipment(shipmentId);
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleString("vi-VN");
    } catch {
      return dateStr;
    }
  };

  const handleExportDossier = async (shipment: Shipment) => {
    let toastId: string | number | undefined;
    try {
      // 1. Kiểm tra điều kiện
      const checkResult = await checkDossierEligibility(shipment.id);

      if (!checkResult.eligible) {
        // 2. Hiển thị dialog thiếu chứng từ
        setIneligibleDialog({
          open: true,
          missingDocs: checkResult.missingDocuments,
          shipmentName: shipment.name,
        });
        return;
      }

      // 3. Đủ điều kiện → tải file PDF
      toastId = toast.loading("Đang tạo hồ sơ...");
      const blob = await exportDossier(shipment.id);
      toast.dismiss(toastId);

      // Tạo link tải file
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;

      // Lấy tên file từ Content-Disposition hoặc tự tạo
      const contentDisposition = (blob as any).headers?.get?.(
        "content-disposition",
      );

      let fileName = `Ho_so_truy_xuat_${shipment.name}_${getLocalDateString()}.pdf`;

      if (contentDisposition) {
        const match = contentDisposition.match(
          /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/,
        );

        if (match && match[1]) {
          fileName = match[1].replace(/['"]/g, "");
        }
      }

      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast.success("Tải hồ sơ thành công");
    } catch (error: any) {
      if (toastId != null) {
        toast.dismiss(toastId);
      }

      const msg =
        error.message ||
        error.response?.data?.message ||
        "Có lỗi xảy ra khi xuất hồ sơ.";

      toast.error(msg);
    }
  };

  const handleExportGs1Dossier = async (shipment: Shipment) => {
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
        error.message ||
        error.response?.data?.message ||
        "Có lỗi xảy ra khi xuất hồ sơ GS1.";

      toast.error(msg);
    }
  };

  return (
    <>
      <Card className="rounded-xl border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex items-center justify-between">
            <CardTitle className="text-xl font-bold text-slate-900">Danh sách lô hàng</CardTitle>

            {canCreate && productionLotStatus === "PACKAGED" && (
              <Button variant="create" size="sm" onClick={() => setModalOpen(true)}>
                <Plus className="mr-1 h-4 w-4" />
                Tạo lô hàng
              </Button>
            )}
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {isLoading ? (
            <div className="py-12 text-center text-muted-foreground">Đang tải...</div>
          ) : shipments.length === 0 ? (
            <div className="py-12 text-center text-muted-foreground">
              Chưa có lô hàng nào cho lô sản xuất này.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80">
                    <TableHead className="font-semibold text-slate-700">Tên lô hàng</TableHead>
                    <TableHead className="text-center font-semibold text-slate-700">Số lượng</TableHead>
                    <TableHead className="font-semibold text-slate-700">Quy cách</TableHead>
                    <TableHead className="font-semibold text-slate-700">Trạng thái</TableHead>
                    <TableHead className="font-semibold text-slate-700">Ngày tạo</TableHead>
                    <TableHead className="text-center font-semibold text-slate-700">Số mã</TableHead>
                    <TableHead className="text-center font-semibold text-slate-700">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {shipments.map((shipment) => (
                    <TableRow key={shipment.id} className="hover:bg-slate-50/60">
                      <TableCell className="font-medium">
                        {shipment.name}
                      </TableCell>

                      <TableCell className="text-center">
                        {shipment.totalQuantity}
                      </TableCell>

                      <TableCell>
                        {shipment.packagingInfo || "—"}
                      </TableCell>

                      <TableCell>
                        <ShipmentStatusBadge status={shipment.status} />
                      </TableCell>

                      <TableCell>
                        {formatDate(shipment.createdAt)}
                      </TableCell>

                      <TableCell className="text-center">
                        {shipment.traceCodes?.length || 0}
                      </TableCell>

                      <TableCell className="text-center">
                        <div className="flex items-center justify-center gap-1.5">
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-auto px-2.5 py-1 text-xs"
                            onClick={() =>
                              navigate(
                                `/production-lots/${productionLotId}/shipments/${shipment.id}`,
                              )
                            }
                          >
                            <Eye className="mr-1 h-3 w-3" />
                            Chi tiết
                          </Button>

                          <DropdownMenu>
                            <DropdownMenuTrigger
                              className="size-7"
                              aria-label="Thao tác khác"
                            >
                              <MoreHorizontal className="size-4" />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent>
                              {canActivate &&
                                shipment.status === "CODE_PRINTED" && (
                                  <DropdownMenuItem
                                    onClick={() =>
                                      setActivatingShipment(shipment)
                                    }
                                  >
                                    <BadgeCheck className="size-4" />
                                    Kích hoạt
                                  </DropdownMenuItem>
                                )}

                              <DropdownMenuItem
                                onClick={() =>
                                  setTimelineDialog({
                                    open: true,
                                    shipmentId: shipment.id,
                                    name: shipment.name,
                                  })
                                }
                              >
                                <History className="size-4" />
                                Sự kiện
                              </DropdownMenuItem>

                              <DropdownMenuItem
                                onClick={() => handleExportDossier(shipment)}
                              >
                                <FileText className="size-4" />
                                Xuất hồ sơ
                              </DropdownMenuItem>

                              {canExportGs1 && (
                                <DropdownMenuItem
                                  onClick={() =>
                                    handleExportGs1Dossier(shipment)
                                  }
                                >
                                  <FileJson className="size-4" />
                                  Xuất hồ sơ GS1
                                </DropdownMenuItem>
                              )}

                              {((canRecall &&
                                shipment.status !== "RECALLED") ||
                                shipment.status === "DRAFT" ||
                                shipment.status === "CODE_PRINTED") && (
                                  <DropdownMenuSeparator />
                                )}

                              {canRecall &&
                                shipment.status !== "RECALLED" && (
                                  <DropdownMenuItem
                                    variant="destructive"
                                    onClick={() =>
                                      setRecallingShipment(shipment)
                                    }
                                  >
                                    <Ban className="size-4" />
                                    Thu hồi
                                  </DropdownMenuItem>
                                )}

                              {(shipment.status === "DRAFT" ||
                                shipment.status === "CODE_PRINTED") && (
                                  <DropdownMenuItem
                                    variant="destructive"
                                    onClick={() =>
                                      setPendingDeleteDraft(shipment)
                                    }
                                  >
                                    Hủy nháp
                                  </DropdownMenuItem>
                                )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}

          {/* Pagination bar */}
          {!isLoading && totalElements > 0 && totalPages > 1 && (
            <div className="flex items-center justify-between border-t pt-4 mt-2">
              <p className="text-sm text-muted-foreground">
                Trang{" "}
                <span className="font-medium text-foreground">{page + 1}</span>
                {" "}/{" "}
                <span className="font-medium text-foreground">{totalPages}</span>
                {" "}&#183;{" "}
                Tổng{" "}
                <span className="font-medium text-foreground">
                  {totalElements}
                </span>{" "}
                lô hàng
              </p>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  className="h-8 w-8 p-0"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="h-8 w-8 p-0"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(page + 1)}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <CreateShipmentModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSubmit={handleCreate}
        productionLotId={productionLotId}
        loading={isCreating}
      />

      <ShipmentTimelineDialog
        open={timelineDialog.open}
        onClose={() =>
          setTimelineDialog({
            open: false,
            shipmentId: "",
            name: "",
          })
        }
        shipmentId={timelineDialog.shipmentId}
        shipmentName={timelineDialog.name}
      />

      <ActivateShipmentDialog
        shipment={activatingShipment}
        isActivating={
          activatingShipmentId === activatingShipment?.id
        }
        onClose={() => setActivatingShipment(null)}
        onConfirm={handleActivateConfirm}
      />

      <RecallShipmentDialog
        shipment={recallingShipment}
        isRecalling={recallingShipmentId === recallingShipment?.id}
        onClose={() => setRecallingShipment(null)}
        onConfirm={async (shipmentId, reason) => {
          await recallShipment(shipmentId, reason);
        }}
      />

      <DossierIneligibleDialog
        open={ineligibleDialog.open}
        onClose={() =>
          setIneligibleDialog({
            open: false,
            missingDocs: [],
            shipmentName: "",
          })
        }
        missingDocs={ineligibleDialog.missingDocs}
        shipmentName={ineligibleDialog.shipmentName}
      />

      <ConfirmDialog
        open={pendingDeleteDraft !== null}
        onOpenChange={(open) => !open && setPendingDeleteDraft(null)}
        title="Hủy bản nháp lô hàng"
        description={`Bạn có chắc chắn muốn hủy bản nháp "${pendingDeleteDraft?.name}"?`}
        confirmLabel="Hủy nháp"
        variant="destructive"
        loading={deletingDraft}
        onConfirm={async () => {
          if (pendingDeleteDraft) {
            await deleteDraftShipment(pendingDeleteDraft);
          }
          setPendingDeleteDraft(null);
        }}
      />
    </>
  );
};