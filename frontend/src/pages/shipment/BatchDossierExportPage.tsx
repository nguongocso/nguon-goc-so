import { useCallback, useEffect, useMemo, useState } from "react";
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
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LoaderCircle, Sparkles, CheckCircle2 } from "lucide-react";
import { TemplateOptionContent } from "@/components/export/TemplateOptionContent";
import { useAuth } from "@/hooks/useAuth";
import { ProfileTemplateSelector } from "@/components/export/ProfileTemplateSelector";
import {
  checkBatchDossierEligibility,
  exportBatchDossier,
  getBatchDossierExportHistory,
  type BatchDossierCheckResponse,
  type BatchDossierHistoryDto,
} from "@/api/dossierApi";
import {
  getBatchProfileTemplates,
  type ProfileTemplate,
} from "@/api/profileTemplateApi";
import { useSetBreadcrumb } from "@/components/common/AppBreadcrumb";

export default function BatchDossierExportPage() {
  const location = useLocation();
  const navigate = useNavigate();

  useSetBreadcrumb([
    { label: "Tổng quan", href: "/dashboard" },
    { label: "Lô sản xuất", href: "/production-lots" },
    { label: "Xuất hồ sơ truy xuất cho nhiều lô" },
  ]);

  // Ghi nhớ danh sách shipmentIds ổn định để tránh re-render lặp vô tận
  const shipmentIdsKey = useMemo(() => {
    const stateShipmentIds: string[] = location.state?.shipmentIds || [];
    const queryParams = new URLSearchParams(location.search);
    const queryShipmentIds = queryParams.get("shipmentIds")
      ? queryParams.get("shipmentIds")!.split(",").filter(Boolean)
      : [];

    return Array.from(new Set([...stateShipmentIds, ...queryShipmentIds])).sort().join(",");
  }, [location.state, location.search]);

  const shipmentIds = useMemo(() => {
    return shipmentIdsKey ? shipmentIdsKey.split(",") : [];
  }, [shipmentIdsKey]);

  const [isLoadingCheck, setIsLoadingCheck] = useState(true);
  const [isExporting, setIsExporting] = useState(false);
  const [checkResult, setCheckResult] = useState<BatchDossierCheckResponse | null>(null);

  const [title, setTitle] = useState("BỘ HỒ SƠ TRUY XUẤT NGUỒN GỐC NÔNG SẢN");
  const [note, setNote] = useState("");

  // NCL-07-CN-007: chọn mẫu hồ sơ truy xuất theo yêu cầu đối tác khi xuất nhiều lô
  const { user } = useAuth();
  const userOrganizationId = user?.organizationId || "";

  // Thông tin các tổ chức có trong danh sách lô hàng (dành cho VT-04 tổng hợp mẫu của các HTX)
  const [involvedOrgIds, setInvolvedOrgIds] = useState<string[]>([]);
  const [involvedOrgNames, setInvolvedOrgNames] = useState<Record<string, string>>({});
  const [templates, setTemplates] = useState<ProfileTemplate[]>([]);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [showInfoText] = useState(true);
  const isBuyerRole = user?.roleCode === 'VT-04';

  /**
   * ID mẫu hồ sơ sẽ truyền vào API khi xuất:
   * - `undefined` → dùng mẫu mặc định hệ thống
   * - UUID string → dùng mẫu tùy chỉnh của tổ chức
   * State này được cập nhật qua callback onTemplateChange của ProfileTemplateSelector.
   */
  const [activeTemplateId, setActiveTemplateId] = useState<string | undefined>(undefined);


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

  useEffect(() => {
    if (!shipmentIdsKey) {
      setIsLoadingCheck(false);
      return;
    }
    let isMounted = true;
    setIsLoadingCheck(true);

    // Gọi API kiểm tra điều kiện
    checkBatchDossierEligibility(shipmentIdsKey.split(","))
      .then((result) => {
        if (isMounted) {
          setCheckResult(result);

          // NCL-07-CN-007: Nếu là VT-04, tổng hợp organizationId từ KẾT QUẢ batch-check
          // (backend đã kèm organizationId/organizationName cho từng lô) rồi fetch
          // mẫu hồ sơ của các HTX sở hữu lô hàng.
          if (user?.roleCode === "VT-04") {
            const orgMap = new Map<string, string>();
            const collectOrg = (item: unknown) => {
              const rawOrgId = (item as { organizationId?: unknown })?.organizationId;
              const orgId = typeof rawOrgId === "string" ? rawOrgId : rawOrgId != null ? String(rawOrgId) : "";
              const orgName = (item as { organizationName?: string })?.organizationName;
              if (orgId && !orgMap.has(orgId)) {
                orgMap.set(orgId, orgName || orgId);
              }
            };
            [...(result.eligibleShipments || []), ...(result.ineligibleShipments || [])].forEach(collectOrg);
            // Dự phòng: batch-check từ backend cũ chưa có organizationId -> suy ra từ
            // cooperativeOrganizationId đã truyền qua navigation state (ProcurementShipmentList).
            if (orgMap.size === 0 && location.state?.shipmentOrgMap) {
              const passed = location.state.shipmentOrgMap as Record<string, { orgId?: string; orgName?: string }>;
              shipmentIdsKey.split(",").forEach((sid) => {
                const entry = passed[sid];
                if (entry?.orgId && !orgMap.has(entry.orgId)) {
                  orgMap.set(entry.orgId, entry.orgName || entry.orgId);
                }
              });
            }
            if (orgMap.size > 0) {
              const orgIdList = Array.from(orgMap.keys());
              setInvolvedOrgIds(orgIdList);
              setInvolvedOrgNames(Object.fromEntries(orgMap));

              // Gọi API lấy templates từ nhiều tổ chức
              setTemplatesLoading(true);
              getBatchProfileTemplates(orgIdList)
                .then((templates) => {
                  if (isMounted) {
                    setTemplates(templates);
                  }
                })
                .catch(() => {
                  if (isMounted) {
                    toast.error("Không thể tải danh sách mẫu hồ sơ.");
                  }
                })
                .finally(() => {
                  if (isMounted) {
                    setTemplatesLoading(false);
                  }
                });
            }
          }
        }
      })
      .catch((err: any) => {
        if (isMounted) {
          toast.error(err.message || "Không thể kiểm tra điều kiện các lô hàng.");
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoadingCheck(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [shipmentIdsKey]);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

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
        templateId: activeTemplateId,
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
    <div className="space-y-6">
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
              1. Kết quả kiểm tra điều kiện chứng từ
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

              {/* NCL-07-CN-007: chọn mẫu hồ sơ áp dụng */}
              {isBuyerRole && involvedOrgIds.length > 1 ? (
                /* VT-04 xuất batch với nhiều tổ chức: tổng hợp mẫu của các HTX sở hữu lô hàng */
                <div className="space-y-2">
                  <Label htmlFor="batch-template" className="text-sm font-semibold flex items-center gap-1.5">
                    <Sparkles className="size-4 text-emerald-600" />
                    Mẫu hồ sơ áp dụng
                    {involvedOrgIds.length > 1 && (
                      <span className="text-xs text-muted-foreground font-normal">
                        (tổng hợp mẫu từ {involvedOrgIds.length} tổ chức: {involvedOrgIds.map((id) => involvedOrgNames[id] || id).join(", ")})
                      </span>
                    )}
                  </Label>
                  {templatesLoading ? (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <LoaderCircle className="animate-spin size-4" />
                      Đang tải mẫu...
                    </div>
                  ) : (
                    <Select
                      value={activeTemplateId || "default"}
                      onValueChange={(value) => setActiveTemplateId(value === "default" || value == null ? undefined : value)}
                      disabled={isExporting}
                    >
                      <SelectTrigger id="batch-template" className="w-full md:w-1/2">
                        <SelectValue placeholder="Chọn mẫu hồ sơ">
                          {activeTemplateId ? (
                            templates.find((t) => t.id === activeTemplateId)?.name || "Mẫu đã chọn"
                          ) : (
                            <span className="text-muted-foreground">Chọn mẫu mặc định</span>
                          )}
                        </SelectValue>
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="default" label="Mẫu mặc định hệ thống">
                          <span className="font-medium">Mẫu mặc định hệ thống</span>
                        </SelectItem>
                        {involvedOrgIds.map((orgId) => {
                          const orgTemplates = templates.filter((t) => t.organizationId === orgId);
                          if (orgTemplates.length === 0) return null;
                          const orgLabel = involvedOrgNames[orgId] || orgId;
                          return (
                            <SelectGroup key={orgId}>
                              <SelectLabel>{orgLabel}</SelectLabel>
                              {orgTemplates.map((tpl) => (
                                <SelectItem
                                  key={tpl.id}
                                  value={tpl.id}
                                  label={`${tpl.name}${tpl.partnerName ? ` (${tpl.partnerName})` : ""}${tpl.isDefault ? " — Mặc định" : ""}`}
                                >
                                  <TemplateOptionContent
                                    name={tpl.name}
                                    partnerName={tpl.partnerName}
                                    isDefault={tpl.isDefault}
                                  />
                                </SelectItem>
                              ))}
                            </SelectGroup>
                          );
                        })}
                      </SelectContent>
                    </Select>
                  )}
                  {showInfoText && (
                    <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-slate-900/50 border text-xs text-muted-foreground flex items-start gap-2">
                      <CheckCircle2 className="size-4 text-emerald-500 shrink-0 mt-0.5" />
                      <div>
                        {activeTemplateId === "default" || !activeTemplateId ? (
                          <span>
                            Áp dụng biểu mẫu mặc định của hệ thống gồm đầy đủ các trường bắt buộc và
                            toàn bộ thông tin sản xuất, canh tác, kiểm nghiệm.
                          </span>
                        ) : (
                          <span>
                            Áp dụng mẫu <strong className="text-foreground">
                              {templates.find((t) => t.id === activeTemplateId)?.name}
                            </strong>
                            . Hồ sơ xuất ra sẽ được lọc chính xác theo cấu hình
                            {templates.find((t) => t.id === activeTemplateId)?.fields?.length || 0} trường đã chọn.
                          </span>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                /* VT-02 hoặc VT-04 xuất batch với 1 tổ chức: dùng ProfileTemplateSelector mặc định */
                <ProfileTemplateSelector
                  organizationId={isBuyerRole && involvedOrgIds.length > 0 ? involvedOrgIds[0] : userOrganizationId}
                  onTemplateChange={(id) => {
                    setActiveTemplateId(id === "default" ? undefined : id);
                  }}
                  disabled={isExporting}
                  showInfoText
                  triggerClassName="w-full md:w-1/2"
                  triggerId="batch-template"
                />
              )}

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
