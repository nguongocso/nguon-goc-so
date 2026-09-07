import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { ListCard } from "@/components/common/ListCard";
import { DataTableShell } from "@/components/common/DataTableShell";
import { TableCell, TableHead, TableRow } from "@/components/ui/table";
import {
  checkBatchDossierEligibility,
  exportBatchDossier,
  getBatchDossierExportHistory,
  type BatchDossierCheckResponse,
  type BatchDossierHistoryDto,
} from "@/api/dossierApi";

export default function BatchDossierExportPage() {
  const location = useLocation();
  const navigate = useNavigate();

  // Lấy danh sách shipmentIds từ location.state hoặc query string
  const stateShipmentIds: string[] = location.state?.shipmentIds || [];
  const queryParams = new URLSearchParams(location.search);
  const queryShipmentIds = queryParams.get("shipmentIds")
    ? queryParams.get("shipmentIds")!.split(",").filter(Boolean)
    : [];

  const shipmentIds = Array.from(new Set([...stateShipmentIds, ...queryShipmentIds]));

  const [isLoadingCheck, setIsLoadingCheck] = useState(true);
  const [isExporting, setIsExporting] = useState(false);
  const [checkResult, setCheckResult] = useState<BatchDossierCheckResponse | null>(null);

  const [title, setTitle] = useState("BỘ HỒ SƠ TRUY XUẤT NGUỒN GỐC NÔNG SẢN");
  const [note, setNote] = useState("");

  const [history, setHistory] = useState<BatchDossierHistoryDto[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);

  const loadHistory = useCallback(async () => {
    setIsLoadingHistory(true);
    try {
      const data = await getBatchDossierExportHistory();
      setHistory(data);
    } catch {
      toast.error("Không thể tải lịch sử xuất bộ hồ sơ.");
    } finally {
      setIsLoadingHistory(false);
    }
  }, []);

  const runEligibilityCheck = useCallback(async () => {
    if (shipmentIds.length === 0) {
      setIsLoadingCheck(false);
      return;
    }
    setIsLoadingCheck(true);
    try {
      const result = await checkBatchDossierEligibility(shipmentIds);
      setCheckResult(result);
    } catch (err: any) {
      toast.error(err.message || "Không thể kiểm tra điều kiện các lô hàng.");
    } finally {
      setIsLoadingCheck(false);
    }
  }, [shipmentIds]);

  useEffect(() => {
    void runEligibilityCheck();
    void loadHistory();
  }, [runEligibilityCheck, loadHistory]);

  const handleExport = async () => {
    if (!checkResult || checkResult.totalEligible === 0) {
      toast.error("Không có lô hàng nào đủ điều kiện để xuất bộ hồ sơ.");
      return;
    }

    const eligibleIds = checkResult.eligibleShipments.map((item) => item.shipmentId);
    setIsExporting(true);
    const toastId = toast.loading("Đang khởi tạo tệp bộ hồ sơ hợp nhất...");

    try {
      const blob = await exportBatchDossier({
        shipmentIds: eligibleIds,
        title: title.trim() || undefined,
        note: note.trim() || undefined,
      });

      toast.dismiss(toastId);

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `Bo_ho_so_truy_xuat_batch_${new Date().toISOString().slice(0, 10)}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast.success(`Xuất bộ hồ sơ cho ${eligibleIds.length} lô hàng thành công!`);
      void loadHistory();
    } catch (error: any) {
      toast.dismiss(toastId);
      toast.error(error.message || "Lỗi khi xuất bộ hồ sơ PDF.");
    } finally {
      setIsExporting(false);
    }
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return "—";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto p-4 md:p-6">
      {/* Tiêu đề trang - Left aligned, clean design */}
      <div className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          Xuất hồ sơ truy xuất cho nhiều lô
        </h1>
        <p className="text-sm text-muted-foreground">
          Xuất bộ hồ sơ truy xuất hợp nhất cho các lô hàng trong chuyến hàng (Bao gồm trang bìa tổng hợp và hồ sơ chi tiết từng lô).
        </p>
      </div>

      {shipmentIds.length === 0 ? (
        <ListCard className="p-6 text-center space-y-4">
          <p className="text-muted-foreground">
            Chưa có lô hàng nào được chọn. Vui lòng quay lại danh sách lô hàng và chọn các lô cần xuất hồ sơ.
          </p>
          <Button variant="outline" onClick={() => navigate(-1)}>
            Quay lại danh sách lô hàng
          </Button>
        </ListCard>
      ) : (
        <>
          {/* Card kết quả kiểm tra điều kiện QTN-11 */}
          <ListCard className="p-6 space-y-4">
            <h2 className="text-lg font-semibold text-foreground">
              1. Kết quả kiểm tra điều kiện chứng từ (QTN-11)
            </h2>

            {isLoadingCheck ? (
              <p className="text-sm text-muted-foreground">Đang kiểm tra điều kiện từng lô hàng...</p>
            ) : checkResult ? (
              <div className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="p-4 rounded-lg bg-muted/30 border text-center">
                    <span className="block text-2xl font-bold text-foreground">{checkResult.totalSelected}</span>
                    <span className="text-xs text-muted-foreground">Tổng số lô đã chọn</span>
                  </div>
                  <div className="p-4 rounded-lg bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800 text-center">
                    <span className="block text-2xl font-bold text-emerald-600 dark:text-emerald-400">{checkResult.totalEligible}</span>
                    <span className="text-xs text-emerald-700 dark:text-emerald-300">Lô đủ điều kiện xuất</span>
                  </div>
                  <div className="p-4 rounded-lg bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800 text-center">
                    <span className="block text-2xl font-bold text-amber-600 dark:text-amber-400">{checkResult.totalIneligible}</span>
                    <span className="text-xs text-amber-700 dark:text-amber-300">Lô chưa đủ chứng từ</span>
                  </div>
                </div>

                {/* Danh sách các lô chưa đủ chứng từ (nếu có) */}
                {checkResult.totalIneligible > 0 && (
                  <div className="p-4 rounded-lg bg-amber-500/10 border border-amber-500/20 space-y-3">
                    <p className="text-sm font-medium text-amber-900 dark:text-amber-200">
                      Phát hiện {checkResult.totalIneligible} lô hàng chưa hoàn tất chứng từ hoặc vi phạm điều kiện:
                    </p>
                    <div className="space-y-2 max-h-60 overflow-y-auto pr-2">
                      {checkResult.ineligibleShipments.map((item) => (
                        <div key={item.shipmentId} className="text-xs p-2.5 rounded bg-background border space-y-1">
                          <p className="font-semibold text-foreground">{item.shipmentName}</p>
                          <ul className="list-disc list-inside text-muted-foreground pl-1 space-y-0.5">
                            {item.missingDocuments.map((doc, idx) => (
                              <li key={idx}>{doc}</li>
                            ))}
                          </ul>
                        </div>
                      ))}
                    </div>
                    {checkResult.totalEligible > 0 && (
                      <p className="text-xs text-muted-foreground italic pt-1">
                        Hệ thống sẽ tự động lọc và tạo bộ hồ sơ cho {checkResult.totalEligible} lô đủ điều kiện còn lại.
                      </p>
                    )}
                  </div>
                )}

                {/* Danh sách các lô đủ điều kiện */}
                {checkResult.totalEligible > 0 && (
                  <div className="space-y-2">
                    <p className="text-xs font-medium text-muted-foreground">
                      Danh sách {checkResult.totalEligible} lô sẽ đưa vào bộ hồ sơ:
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {checkResult.eligibleShipments.map((item) => (
                        <span key={item.shipmentId} className="px-2.5 py-1 rounded bg-muted text-xs font-medium text-foreground">
                          {item.shipmentName}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : null}
          </ListCard>

          {/* Form cấu hình thông tin bộ hồ sơ */}
          <ListCard className="p-6 space-y-4">
            <h2 className="text-lg font-semibold text-foreground">
              2. Thông tin bộ hồ sơ
            </h2>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="batch-title">Tên tiêu đề bộ hồ sơ</Label>
                <Input
                  id="batch-title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Ví dụ: BỘ HỒ SƠ TRUY XUẤT CHUYẾN HÀNG SIÊU THỊ CO.OPMART"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="batch-note">Ghi chú bổ sung (Hiển thị trên trang bìa)</Label>
                <Textarea
                  id="batch-note"
                  rows={3}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Nhập ghi chú đơn hàng, quy cách đóng gói tổng hợp hoặc thông tin người giao..."
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t">
              <Button variant="outline" onClick={() => navigate(-1)} disabled={isExporting}>
                Quay lại
              </Button>
              <Button
                onClick={handleExport}
                disabled={isLoadingCheck || isExporting || !checkResult || checkResult.totalEligible === 0}
              >
                {isExporting ? "Đang tạo bộ hồ sơ PDF..." : `Xuất bộ hồ sơ PDF (${checkResult?.totalEligible || 0} lô)`}
              </Button>
            </div>
          </ListCard>

          {/* Bảng lịch sử xuất bộ hồ sơ inline */}
          <ListCard className="p-6 space-y-4">
            <h2 className="text-lg font-semibold text-foreground">
              3. Lịch sử xuất bộ hồ sơ
            </h2>

            <DataTableShell
              colSpan={6}
              header={
                <>
                  <TableHead className="w-12 text-center">STT</TableHead>
                  <TableHead>Tên bộ hồ sơ / File</TableHead>
                  <TableHead>Người xuất</TableHead>
                  <TableHead>Thời gian xuất</TableHead>
                  <TableHead>Kích thước</TableHead>
                  <TableHead className="text-center">Trạng thái</TableHead>
                </>
              }
              body={history.map((item, index) => (
                <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
                  <TableCell className="text-center font-medium text-muted-foreground">
                    {index + 1}
                  </TableCell>
                  <TableCell className="font-medium text-foreground">
                    {item.fileName || item.title}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {item.exporterName}
                  </TableCell>
                  <TableCell className="text-muted-foreground text-xs">
                    {new Date(item.exportedAt).toLocaleString("vi-VN")}
                  </TableCell>
                  <TableCell className="text-muted-foreground text-xs">
                    {formatFileSize(item.fileSize)}
                  </TableCell>
                  <TableCell className="text-center">
                    <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300">
                      {item.status}
                    </span>
                  </TableCell>
                </TableRow>
              ))}
              loading={isLoadingHistory}
              empty={!isLoadingHistory && history.length === 0}
              loadingMessage="Đang tải lịch sử xuất..."
              emptyMessage="Chưa có lịch sử xuất bộ hồ sơ nào."
            />
          </ListCard>
        </>
      )}
    </div>
  );
}
