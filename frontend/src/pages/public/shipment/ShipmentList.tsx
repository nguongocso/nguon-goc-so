import { useState, useMemo } from "react";
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
  Hash,
  History,
  Eye,
  QrCode,
  LogIn,
  LogOut,
} from "lucide-react";
import { useShipments } from "@/hooks/useShipments";
import { useRecallShipment } from "@/hooks/useRecallShipment";
import type { Shipment } from "@/types/shipment";
import { getLocalDateString } from "@/utils/dateTime";
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
import { ExportLabelsDialog } from "@/components/shipment/ExportLabelsDialog";


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

  // NCL-04-CN-005: Lô hàng đang được xuất tem QR
  const [labelExportShipment, setLabelExportShipment] =
    useState<Shipment | null>(null);

  type SelectionTarget = "BATCH_DOSSIER" | "WAREHOUSE_ENTRY" | "WAREHOUSE_EXIT" | null;
  const [selectionTarget, setSelectionTarget] = useState<SelectionTarget>(null);
  const isSelectionMode = selectionTarget !== null;

  const [selectedShipmentIds, setSelectedShipmentIds] = useState<string[]>([]);
  const canExportGs1 = usePermission(ROLE_ACCESS.gs1DossierExport);
  const canExportBatch = usePermission(ROLE_ACCESS.batchDossierExport);
  // NCL-04-CN-005: Chỉ VT-02 được xuất tem QR
  const canExportLabels = usePermission(ROLE_ACCESS.labelExport);
  // NCL-04-CN-007: Chỉ VT-02 được gửi yêu cầu cấp bổ sung mã
  const canRequestSupplement = usePermission(ROLE_ACCESS.supplementCreate);

  const [filterFromDate, setFilterFromDate] = useState("");
  const [filterToDate, setFilterToDate] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");

  const toggleSelectShipment = (id: string) => {
    setSelectedShipmentIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const {
    shipments,
    isLoading,
    activatingShipmentId,
    activateShipment,
    reload,
    page,
    totalPages,
    totalElements,
    setPage,
  } = useShipments(productionLotId);

  const filteredShipments = useMemo(() => {
    return shipments.filter((shipment: Shipment) => {
      if (filterFromDate) {
        const from = new Date(filterFromDate).getTime();
        const created = new Date(shipment.createdAt).getTime();
        if (created < from) return false;
      }
      if (filterToDate) {
        const to = new Date(filterToDate).getTime() + 86400000;
        const created = new Date(shipment.createdAt).getTime();
        if (created > to) return false;
      }
      if (filterStatus !== "ALL" && shipment.status !== filterStatus) {
        return false;
      }
      return true;
    });
  }, [shipments, filterFromDate, filterToDate, filterStatus]);

  const toggleSelectAllPage = () => {
    const pageIds = filteredShipments.map((s: Shipment) => s.id);
    const allSelected = pageIds.length > 0 && pageIds.every((id: string) => selectedShipmentIds.includes(id));
    if (allSelected) {
      setSelectedShipmentIds((prev: string[]) => prev.filter((id: string) => !pageIds.includes(id)));
    } else {
      setSelectedShipmentIds((prev: string[]) => Array.from(new Set([...prev, ...pageIds])));
    }
  };

  const handleCancelSelectionMode = () => {
    setSelectionTarget(null);
    setSelectedShipmentIds([]);
    setFilterFromDate("");
    setFilterToDate("");
    setFilterStatus("ALL");
  };

  const { recallingShipmentId, recallShipment } = useRecallShipment(reload);

  const [pendingDeleteDraft, setPendingDeleteDraft] = useState<Shipment | null>(
    null,
  );
  const { deletingDraft, deleteDraftShipment } = useDeleteDraftShipment(reload);

  // Chặn kích hoạt tem khi lô chưa đạt kiểm nghiệm / kết quả hết hiệu lực
  const handleActivateConfirm = async (shipmentId: string) => {
    let check;
    try {
      check = await checkCanActivateSeal(productionLotId);
    } catch {
      toast.error("Không thể kiểm tra điều kiện tạo lô hàng kích hoạt tem");
      throw new Error("cannot-check-activation");
    }
    if (!check.canActivate) {
      toast.error(check.reason || "Lô chưa đủ điều kiện tạo lô hàng và kích hoạt tem");
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

            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  if (selectedShipmentIds.length > 0) {
                    navigate(`/coop-warehouse-events/entry?productionLotId=${productionLotId}&shipmentIds=${selectedShipmentIds.join(",")}`);
                  } else {
                    setSelectionTarget("WAREHOUSE_ENTRY");
                  }
                }}
              >
                <LogIn className="mr-1.5 h-4 w-4" />
                Nhập kho HTX {selectedShipmentIds.length > 0 && selectionTarget === "WAREHOUSE_ENTRY" ? `(${selectedShipmentIds.length})` : ""}
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  if (selectedShipmentIds.length > 0) {
                    navigate(`/coop-warehouse-events/exit?productionLotId=${productionLotId}&shipmentIds=${selectedShipmentIds.join(",")}`);
                  } else {
                    setSelectionTarget("WAREHOUSE_EXIT");
                  }
                }}
              >
                <LogOut className="mr-1.5 h-4 w-4" />
                Xuất kho HTX {selectedShipmentIds.length > 0 && selectionTarget === "WAREHOUSE_EXIT" ? `(${selectedShipmentIds.length})` : ""}
              </Button>

              {/* NCL-04-CN-007: tùy chọn yêu cầu cấp bổ sung dải mã (chỉ VT-02) */}
              {canRequestSupplement && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate("/code-range-supplements/create")}
                >
                  <Hash className="mr-1 h-4 w-4" />
                  Cấp bổ sung mã
                </Button>
              )}

              {canExportBatch && shipments.length > 0 && (
                !isSelectionMode ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setSelectionTarget("BATCH_DOSSIER")}
                  >
                    <FileText className="mr-1.5 h-4 w-4" />
                    Xuất hồ sơ nhiều lô
                  </Button>
                ) : (
                  <>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={handleCancelSelectionMode}
                    >
                      Hủy chọn
                    </Button>

                    {selectionTarget === "WAREHOUSE_ENTRY" && (
                      <Button
                        variant="default"
                        size="sm"
                        disabled={selectedShipmentIds.length === 0}
                        className="bg-emerald-600 hover:bg-emerald-700 text-white"
                        onClick={() =>
                          navigate(`/coop-warehouse-events/entry?productionLotId=${productionLotId}&shipmentIds=${selectedShipmentIds.join(",")}`)
                        }
                      >
                        Xác nhận ghi Nhập kho ({selectedShipmentIds.length} lô)
                      </Button>
                    )}

                    {selectionTarget === "WAREHOUSE_EXIT" && (
                      <Button
                        variant="default"
                        size="sm"
                        disabled={selectedShipmentIds.length === 0}
                        className="bg-amber-600 hover:bg-amber-700 text-white"
                        onClick={() =>
                          navigate(`/coop-warehouse-events/exit?productionLotId=${productionLotId}&shipmentIds=${selectedShipmentIds.join(",")}`)
                        }
                      >
                        Xác nhận ghi Xuất kho ({selectedShipmentIds.length} lô)
                      </Button>
                    )}

                    {selectionTarget === "BATCH_DOSSIER" && (
                      <Button
                        variant="default"
                        size="sm"
                        disabled={selectedShipmentIds.length === 0}
                        onClick={() =>
                          navigate("/shipments/batch-dossier-export", {
                            state: { shipmentIds: selectedShipmentIds },
                          })
                        }
                      >
                        Xác nhận xuất bộ hồ sơ ({selectedShipmentIds.length} lô)
                      </Button>
                    )}
                  </>
                )
              )}

              {!isSelectionMode && canCreate && productionLotStatus === "PACKAGED" && (
                <Button variant="create" size="sm" onClick={() => navigate(`/production-lots/${productionLotId}/shipments/create`)}>
                  <Plus className="mr-1 h-4 w-4" />
                  Tạo lô hàng
                </Button>
              )}
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-4 space-y-4">
          {isSelectionMode && (
            <div className="rounded-md border border-slate-200 bg-slate-50/70 p-3.5">
              <div className="flex flex-wrap items-center gap-4 text-xs text-slate-700">
                <span className="font-semibold text-slate-900">Bộ lọc chọn lô:</span>

                <div className="flex items-center gap-1.5">
                  <label className="text-slate-600">Từ ngày:</label>
                  <input
                    type="date"
                    className="h-8 rounded-md border border-slate-300 bg-white px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-slate-400"
                    value={filterFromDate}
                    onChange={(e) => setFilterFromDate(e.target.value)}
                  />
                </div>

                <div className="flex items-center gap-1.5">
                  <label className="text-slate-600">Đến ngày:</label>
                  <input
                    type="date"
                    className="h-8 rounded-md border border-slate-300 bg-white px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-slate-400"
                    value={filterToDate}
                    onChange={(e) => setFilterToDate(e.target.value)}
                  />
                </div>

                <div className="flex items-center gap-1.5">
                  <label className="text-slate-600">Trạng thái:</label>
                  <select
                    className="h-8 rounded-md border border-slate-300 bg-white px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-slate-400"
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                  >
                    <option value="ALL">Tất cả trạng thái</option>
                    <option value="ACTIVATED">Đã kích hoạt</option>
                    <option value="CODE_PRINTED">Đã in mã</option>
                    <option value="RECALLED">Đã thu hồi</option>
                    <option value="DRAFT">Bản nháp</option>
                  </select>
                </div>

                {(filterFromDate || filterToDate || filterStatus !== "ALL") && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 px-2 text-xs text-red-600 hover:bg-red-50 hover:text-red-700"
                    onClick={() => {
                      setFilterFromDate("");
                      setFilterToDate("");
                      setFilterStatus("ALL");
                    }}
                  >
                    Đặt lại bộ lọc
                  </Button>
                )}

                <div className="ml-auto text-xs text-slate-500">
                  Hiển thị <span className="font-medium text-slate-900">{filteredShipments.length}</span> / {shipments.length} lô
                </div>
              </div>
            </div>
          )}

          <div className="rounded-md border overflow-x-auto bg-white">
            {isLoading ? (
              <div className="py-12 text-center text-muted-foreground">Đang tải...</div>
            ) : shipments.length === 0 ? (
              <div className="py-12 text-center text-muted-foreground">
                Chưa có lô hàng nào cho lô sản xuất này.
              </div>
            ) : filteredShipments.length === 0 ? (
              <div className="py-12 text-center text-muted-foreground">
                Không tìm thấy lô hàng phù hợp với bộ lọc hiện tại.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80">
                    {isSelectionMode && (
                      <TableHead className="w-10 text-center">
                        <input
                          type="checkbox"
                          className="rounded border-input"
                          checked={
                            filteredShipments.length > 0 &&
                            filteredShipments.every((s: Shipment) => selectedShipmentIds.includes(s.id))
                          }
                          onChange={toggleSelectAllPage}
                          title="Chọn tất cả các lô hiển thị"
                        />
                      </TableHead>
                    )}
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
                  {filteredShipments.map((shipment: Shipment) => (
                    <TableRow key={shipment.id} className="hover:bg-slate-50/60">
                      {isSelectionMode && (
                        <TableCell className="text-center">
                          <input
                            type="checkbox"
                            className="rounded border-input"
                            checked={selectedShipmentIds.includes(shipment.id)}
                            onChange={() => toggleSelectShipment(shipment.id)}
                          />
                        </TableCell>
                      )}
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
                                  navigate(
                                    `/coop-warehouse-events/entry?productionLotId=${productionLotId}&shipmentIds=${shipment.id}`
                                  )
                                }
                              >
                                <LogIn className="size-4" />
                                Nhập kho HTX
                              </DropdownMenuItem>

                              <DropdownMenuItem
                                onClick={() =>
                                  navigate(
                                    `/coop-warehouse-events/exit?productionLotId=${productionLotId}&shipmentIds=${shipment.id}`
                                  )
                                }
                              >
                                <LogOut className="size-4" />
                                Xuất kho HTX
                              </DropdownMenuItem>

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

                              {/* NCL-04-CN-005: Xuất tem QR — chỉ VT-02, lô đã sinh mã và chưa thu hồi */}
                              {canExportLabels &&
                                shipment.status !== "DRAFT" &&
                                shipment.status !== "RECALLED" &&
                                (shipment.traceCodes?.length || 0) > 0 && (
                                  <DropdownMenuItem
                                    onClick={() =>
                                      setLabelExportShipment(shipment)
                                    }
                                  >
                                    <QrCode className="size-4" />
                                    Xuất tem QR
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
            )}
          </div>

          {/* Pagination bar */}
          {!isLoading && totalElements > 0 && totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-slate-100 pt-4 mt-2">
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
      {/* NCL-04-CN-005: Dialog xuất tem QR */}
      <ExportLabelsDialog
        open={labelExportShipment !== null}
        shipment={labelExportShipment}
        onClose={() => setLabelExportShipment(null)}
      />

    </>
  );
};