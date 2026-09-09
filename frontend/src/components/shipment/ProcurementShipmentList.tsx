import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Eye, FileJson, FileText, Handshake, ShoppingCart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import { ListCard } from "@/components/common/ListCard";
import { ListToolbar } from "@/components/common/ListToolbar";
import { SearchInput } from "@/components/common/SearchInput";
import { FilterSelect } from "@/components/common/FilterSelect";
import { RefreshButton } from "@/components/common/RefreshButton";
import { DataTableShell } from "@/components/common/DataTableShell";
import { Pagination } from "@/components/common/Pagination";
import { ShipmentStatusBadge } from "@/components/shipment/ShipmentStatusBadge";
import { ROLE_ACCESS } from "@/config/roleAccess";
import { usePermission } from "@/hooks/usePermission";
import type { ProcurementShipment } from "@/types/shipment";
import type { HandoverDetailResponse } from "@/types/shipmentHandover";
import { getEligibleShipments, getShipmentById } from "@/api/shipmentApi";
import { getReceivedHandovers } from "@/api/handoverApi";
import { checkDossierEligibility, exportDossier, exportGs1Dossier } from "@/api/dossierApi";
import { getLocalDateString } from "@/utils/dateTime";
import { DossierIneligibleDialog } from "@/components/shipment/DossierIneligibleDialog";

const PAGE_SIZE = 10;

interface ProcurementShipmentListProps {
  /** Callback khi người dùng bấm "Ghi nhận thu mua" trên một lô hàng */
  onRecordProcurement: (shipmentId: string) => void;
}

export function ProcurementShipmentList({
  onRecordProcurement,
}: ProcurementShipmentListProps) {
  const [shipments, setShipments] = useState<ProcurementShipment[]>([]);
  const [handoverByShipment, setHandoverByShipment] = useState<
    Map<string, HandoverDetailResponse>
  >(new Map());
  const [acceptedShipmentIds, setAcceptedShipmentIds] = useState<Set<string>>(
    new Set(),
  );
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [orgFilter, setOrgFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const navigate = useNavigate();

  const [isSelectionMode, setIsSelectionMode] = useState(false);
  const [selectedShipmentIds, setSelectedShipmentIds] = useState<string[]>([]);
  const canExportGs1 = usePermission(ROLE_ACCESS.gs1DossierExport);
  const canExportBatch = usePermission(ROLE_ACCESS.batchDossierExport);

  const handleViewDetail = async (shipmentId: string) => {
    try {
      const data = await getShipmentById(shipmentId);

      if (!data.productionLotId) {
        toast.error("Không thể xác định lô sản xuất của lô hàng này.");
        return;
      }

      navigate(
        `/production-lots/${data.productionLotId}/shipments/${shipmentId}`,
      );
    } catch {
      toast.error("Không thể tải chi tiết lô hàng.");
    }
  };

  const loadShipments = useCallback(async () => {
    setIsLoading(true);
    try {
      const [data, received] = await Promise.all([
        getEligibleShipments(),
        getReceivedHandovers().catch(() => [] as HandoverDetailResponse[]),
      ]);
      setShipments(data);

      // Ánh xạ "lô hàng → phiếu bàn giao mới nhất" để hiển thị nút xem phiếu
      // (NCL-05-CN-008: tổ chức nhận xem được phiếu bàn giao ngay từ Dashboard).
      const map = new Map<string, HandoverDetailResponse>();
      const accepted = new Set<string>();
      for (const handover of received) {
        if (handover.status === "ACCEPTED") {
          accepted.add(handover.shipmentId);
        }
        const latest = map.get(handover.shipmentId);
        if (
          !latest ||
          new Date(handover.createdAt) > new Date(latest.createdAt)
        ) {
          map.set(handover.shipmentId, handover);
        }
      }
      setHandoverByShipment(map);
      setAcceptedShipmentIds(accepted);
    } catch {
      toast.error("Không thể tải danh sách lô hàng thu mua.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadShipments();
  }, [loadShipments]);

  const categoryOptions = useMemo(() => {
    const names = Array.from(
      new Set(
        shipments
          .map((shipment) => shipment.productCategoryName)
          .filter((name): name is string => Boolean(name)),
      ),
    );
    return [
      { value: "ALL", label: "Tất cả nông sản" },
      ...names.map((name) => ({ value: name, label: name })),
    ];
  }, [shipments]);

  const organizationOptions = useMemo(() => {
    const names = Array.from(
      new Set(
        shipments
          .map((shipment) => shipment.organizationName)
          .filter((name): name is string => Boolean(name)),
      ),
    );
    return [
      { value: "ALL", label: "Tất cả tổ chức" },
      ...names.map((name) => ({ value: name, label: name })),
    ];
  }, [shipments]);

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return shipments.filter(
      (shipment) =>
        (categoryFilter === "ALL" ||
          shipment.productCategoryName === categoryFilter) &&
        (orgFilter === "ALL" ||
          shipment.organizationName === orgFilter) &&
        (!keyword ||
          shipment.name.toLowerCase().includes(keyword) ||
          (shipment.productionLotName ?? "").toLowerCase().includes(keyword) ||
          (shipment.productCategoryName ?? "").toLowerCase().includes(keyword) ||
          (shipment.organizationName ?? "").toLowerCase().includes(keyword)),
    );
  }, [shipments, search, categoryFilter, orgFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginatedShipments = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const toggleSelectShipment = (id: string) => {
    setSelectedShipmentIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const toggleSelectAllPage = () => {
    const pageIds = paginatedShipments.map((s) => s.id);
    const allSelected = pageIds.every((id) => selectedShipmentIds.includes(id));
    if (allSelected) {
      setSelectedShipmentIds((prev) => prev.filter((id) => !pageIds.includes(id)));
    } else {
      setSelectedShipmentIds((prev) => Array.from(new Set([...prev, ...pageIds])));
    }
  };

  const handleGoToBatchExport = () => {
    if (selectedShipmentIds.length === 0) {
      toast.error("Vui lòng chọn ít nhất 1 lô hàng để xuất bộ hồ sơ.");
      return;
    }
    navigate("/shipments/batch-dossier-export", {
      state: { shipmentIds: selectedShipmentIds },
    });
  };

  const [filterFromDate, setFilterFromDate] = useState("");
  const [filterToDate, setFilterToDate] = useState("");

  const handleCancelSelectionMode = () => {
    setIsSelectionMode(false);
    setSelectedShipmentIds([]);
    setFilterFromDate("");
    setFilterToDate("");
  };

  const handleExportGs1 = async (shipmentId: string) => {
    const toastId = toast.loading("Đang tạo hồ sơ GS1...");
    try {
      const { blob, fileName } = await exportGs1Dossier(
        shipmentId,
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

  const [ineligibleDialog, setIneligibleDialog] = useState<{
    open: boolean;
    missingDocs: string[];
    shipmentName: string;
  }>({
    open: false,
    missingDocs: [],
    shipmentName: "",
  });

  const handleExportDossier = async (shipment: ProcurementShipment) => {
    let toastId: string | number | undefined;
    try {
      const checkResult = await checkDossierEligibility(shipment.id);

      if (!checkResult.eligible) {
        setIneligibleDialog({
          open: true,
          missingDocs: checkResult.missingDocuments,
          shipmentName: shipment.name,
        });
        return;
      }

      toastId = toast.loading("Đang tạo hồ sơ...");
      const blob = await exportDossier(shipment.id);
      toast.dismiss(toastId);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;

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

  return (
    <>
      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm tên lô hàng, lô sản xuất, nông sản hoặc tổ chức..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                aria-label="Tìm kiếm lô hàng thu mua"
              />
              <FilterSelect
                value={categoryFilter}
                onValueChange={(value) => {
                  setCategoryFilter(value ?? "ALL");
                  setPage(0);
                }}
                options={categoryOptions}
              />
              <FilterSelect
                value={orgFilter}
                onValueChange={(value) => {
                  setOrgFilter(value ?? "ALL");
                  setPage(0);
                }}
                options={organizationOptions}
              />
            </>
          }
          right={
            <div className="flex items-center gap-2">
              {canExportBatch && filtered.length > 0 && (
                !isSelectionMode ? (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setIsSelectionMode(true)}
                  >
                    <FileText className="mr-1.5 h-4 w-4" />
                    Xuất hồ sơ nhiều lô
                  </Button>
                ) : (
                  <>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={handleCancelSelectionMode}
                    >
                      Hủy chọn
                    </Button>
                    <Button
                      type="button"
                      variant="default"
                      size="sm"
                      disabled={selectedShipmentIds.length === 0}
                      onClick={handleGoToBatchExport}
                    >
                      Xác nhận xuất bộ hồ sơ ({selectedShipmentIds.length} lô)
                    </Button>
                  </>
                )
              )}
              <RefreshButton onClick={loadShipments} loading={isLoading} />
            </div>
          }
        />

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

              {(filterFromDate || filterToDate) && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2 text-xs text-red-600 hover:bg-red-50 hover:text-red-700"
                  onClick={() => {
                    setFilterFromDate("");
                    setFilterToDate("");
                  }}
                >
                  Đặt lại bộ lọc
                </Button>
              )}

              <div className="ml-auto text-xs text-slate-500">
                Hiển thị <span className="font-medium text-slate-900">{filtered.length}</span> / {shipments.length} lô
              </div>
            </div>
          </div>
        )}

        <DataTableShell
          colSpan={isSelectionMode ? 9 : 8}
          header={
            <>
              {isSelectionMode && (
                <TableHead className="w-10 text-center">
                  <input
                    type="checkbox"
                    className="rounded border-input"
                    checked={
                      paginatedShipments.length > 0 &&
                      paginatedShipments.every((s) => selectedShipmentIds.includes(s.id))
                    }
                    onChange={toggleSelectAllPage}
                    title="Chọn tất cả trên trang này"
                  />
                </TableHead>
              )}
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Tên lô hàng</TableHead>
              <TableHead>Lô sản xuất</TableHead>
              <TableHead>Nông sản</TableHead>
              <TableHead>Tổ chức</TableHead>
              <TableHead>Sản lượng</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-center">Thao tác</TableHead>
            </>
          }
          body={paginatedShipments.map((shipment, index) => (
            <TableRow
              key={shipment.id}
              className="hover:bg-muted/40 transition-colors"
            >
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
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="font-semibold text-foreground">
                {shipment.name}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {shipment.productionLotName ?? "—"}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {shipment.productCategoryName ?? "—"}
              </TableCell>
              <TableCell className="text-muted-foreground font-medium">
                {shipment.organizationName ?? "—"}
              </TableCell>
              <TableCell className="text-muted-foreground">
                {shipment.totalQuantity != null
                  ? shipment.totalQuantity.toLocaleString("vi-VN")
                  : "—"}
              </TableCell>
              <TableCell>
                <ShipmentStatusBadge status={shipment.status} />
              </TableCell>
              <TableCell className="text-center">
                <div className="flex items-center justify-center gap-1">
                  {/* Chỉ ghi nhận thu mua cho lô đã được bàn giao và bên nhận đã xác
                  nhận (có phiếu ACCEPTED, không phụ thuộc phiếu mới nhất) —
                  consistent với luồng nhận hàng. */}
                  {acceptedShipmentIds.has(shipment.id) && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-sm"
                      title="Ghi nhận thu mua"
                      className="hover:bg-muted"
                      onClick={() => onRecordProcurement(shipment.id)}
                    >
                      <ShoppingCart className="size-4" />
                    </Button>
                  )}

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    title="Xuất hồ sơ"
                    className="hover:bg-muted"
                    onClick={() => handleExportDossier(shipment)}
                  >
                    <FileText className="size-4" />
                  </Button>

                  {canExportGs1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-sm"
                      title="Xuất hồ sơ GS1"
                      className="hover:bg-muted"
                      onClick={() => handleExportGs1(shipment.id)}
                    >
                      <FileJson className="size-4" />
                    </Button>
                  )}

                  {handoverByShipment.get(shipment.id) && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-sm"
                      title="Xem phiếu bàn giao"
                      className="hover:bg-muted"
                      onClick={() =>
                        navigate(
                          `/shipment-handovers/${handoverByShipment.get(shipment.id)!.id}`,
                        )
                      }
                    >
                      <Handshake className="size-4" />
                    </Button>
                  )}

                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    title="Xem chi tiết"
                    className="hover:bg-muted"
                    onClick={() => handleViewDetail(shipment.id)}
                  >
                    <Eye className="size-4" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
          loading={isLoading}
          empty={!isLoading && filtered.length === 0}
          loadingMessage="Đang tải danh sách lô hàng..."
          emptyMessage={
            search.trim() || categoryFilter !== "ALL"
              ? "Không tìm thấy lô hàng phù hợp. Hãy thử thay đổi từ khóa tìm kiếm."
              : "Chưa có lô hàng nào được thu mua, bàn giao hoặc nhập kho. Các lô hàng đã được bàn giao và xác nhận nhận sẽ xuất hiện tại đây."
          }
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={isLoading}
          itemLabel="lô hàng"
          onPageChange={setPage}
        />
      </ListCard>

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
    </>
  );
}
