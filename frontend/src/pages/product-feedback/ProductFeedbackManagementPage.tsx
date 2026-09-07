import { useCallback, useEffect, useState } from "react";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { ChevronLeft, ChevronRight, RefreshCw, Search } from "lucide-react";
import { isAxiosError } from "axios";
import { toast } from "sonner";

import {
  assignProductFeedback,
  closeProductFeedback,
  createProductFeedbackRecall,
  getProductFeedbackById,
  getProductFeedbacks,
  updateProductFeedbackProcessing,
} from "@/api/productFeedbackApi";
import { getOrganizationMembers } from "@/api/memberApi";
import { getShipmentsByProductionLot } from "@/api/shipmentApi";
import { DetailField } from "@/components/common/detail/DetailField";
import { DetailSection } from "@/components/common/detail/DetailSection";
import { HelpButton } from "@/components/help/HelpButton";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/hooks/useAuth";
import { maskId } from "@/lib/utils";
import type { PageResponse } from "@/types/common";
import type { OrganizationMember } from "@/types/member";
import type {
  ProductFeedback,
  ProductFeedbackSeverity,
  ProductFeedbackStatus,
} from "@/types/productFeedback";
import type { Shipment, TraceCode } from "@/types/shipment";
import {
  hasUnsavedClassification as checkUnsavedClassification,
  hasUnsavedProcessing as checkUnsavedProcessing,
} from "./productFeedbackDraft";

const STATUS_LABELS: Record<ProductFeedbackStatus, string> = {
  NEW: "Mới",
  IN_PROGRESS: "Đang xử lý",
  ESCALATED_TO_RECALL: "Đã chuyển thu hồi",
  CLOSED: "Đã đóng",
};

const SEVERITY_LABELS: Record<ProductFeedbackSeverity, string> = {
  INFORMATION: "Thông tin",
  QUALITY_SUSPECTED: "Nghi ngờ chất lượng",
  COUNTERFEIT_SUSPECTED: "Nghi ngờ tem giả",
};

function formatDate(value?: string): string {
  if (!value) return "—";
  try {
    return format(new Date(value), "dd/MM/yyyy HH:mm", { locale: vi });
  } catch {
    return value;
  }
}

function errorMessage(error: unknown, fallback: string): string {
  return isAxiosError<{ message?: string }>(error)
    ? error.response?.data?.message ?? fallback
    : fallback;
}

function StatusPill({ status }: { status: ProductFeedbackStatus }) {
  const color = {
    NEW: "bg-blue-50 text-blue-700",
    IN_PROGRESS: "bg-amber-50 text-amber-700",
    ESCALATED_TO_RECALL: "bg-red-50 text-red-700",
    CLOSED: "bg-emerald-50 text-emerald-700",
  }[status];
  return <span className={`rounded-full px-2 py-1 text-xs font-medium ${color}`}>{STATUS_LABELS[status]}</span>;
}

export default function ProductFeedbackManagementPage() {
  const { user } = useAuth();
  const canProcess = user?.roleCode === "VT-02";
  const [feedbacks, setFeedbacks] = useState<ProductFeedback[]>([]);
  const [pageInfo, setPageInfo] = useState<Omit<PageResponse<ProductFeedback>, "items">>({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [status, setStatus] = useState<"ALL" | ProductFeedbackStatus>("ALL");
  const [severity, setSeverity] = useState<"ALL" | ProductFeedbackSeverity>("ALL");
  const [selectedFeedback, setSelectedFeedback] = useState<ProductFeedback | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  const fetchFeedbacks = useCallback(async () => {
    try {
      setLoading(true);
      setLoadError(null);
      const data = await getProductFeedbacks({
        page,
        size,
        sort: "createdAt,desc",
        keyword: appliedKeyword || undefined,
        status: status === "ALL" ? undefined : status,
        severity: severity === "ALL" ? undefined : severity,
      });
      setFeedbacks(data.items);
      setPageInfo({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        first: data.first,
        last: data.last,
      });
    } catch (error: unknown) {
      const message = errorMessage(error, "Không thể tải danh sách phản ánh");
      setLoadError(message);
      setFeedbacks([]);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [appliedKeyword, page, severity, size, status]);

  useEffect(() => {
    void fetchFeedbacks();
  }, [fetchFeedbacks]);

  const openDetail = async (feedback: ProductFeedback) => {
    setSelectedFeedback(feedback);
    setDetailOpen(true);
    try {
      setSelectedFeedback(await getProductFeedbackById(feedback.id));
    } catch (error) {
      toast.error(errorMessage(error, "Không thể tải chi tiết phản ánh"));
    }
  };

  const applyFilters = () => {
    setPage(0);
    setAppliedKeyword(keyword.trim());
  };

  const clearFilters = () => {
    setKeyword("");
    setAppliedKeyword("");
    setStatus("ALL");
    setSeverity("ALL");
    setPage(0);
  };

  const updateFeedback = (updated: ProductFeedback) => {
    setSelectedFeedback(updated);
    setFeedbacks((current) => current.map((item) => (item.id === updated.id ? updated : item)));
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Nhận phản ánh</h1>
          <p className="text-sm text-muted-foreground">Xem và xử lý các phản ánh từ người tiêu dùng về sản phẩm.</p>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="product-feedback" />
          <Button variant="outline" size="sm" onClick={() => void fetchFeedbacks()} disabled={loading}>
            <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Summary title="Tổng phản ánh" value={loading ? "..." : pageInfo.totalElements} />
        <Summary title="Trang hiện tại" value={`${pageInfo.totalPages ? pageInfo.page + 1 : 0} / ${pageInfo.totalPages}`} />
        <Summary title="Kích thước trang" value={pageInfo.size} />
      </div>

      <div className="rounded-xl border bg-white p-4 shadow-sm">
        <div className="grid gap-3 lg:grid-cols-[1fr_220px_220px_auto_auto]">
          <Input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && applyFilters()}
            placeholder="Tìm theo nội dung, lô sản xuất hoặc mã tem..."
          />
          <Select value={status} onValueChange={(value) => { setStatus(value as typeof status); setPage(0); }}>
            <SelectTrigger>
              <SelectValue>
                {status === "ALL" ? "Tất cả trạng thái" : STATUS_LABELS[status]}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
              {Object.entries(STATUS_LABELS).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={severity} onValueChange={(value) => { setSeverity(value as typeof severity); setPage(0); }}>
            <SelectTrigger>
              <SelectValue>
                {severity === "ALL" ? "Tất cả mức độ" : SEVERITY_LABELS[severity]}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tất cả mức độ</SelectItem>
              {Object.entries(SEVERITY_LABELS).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
          <Button onClick={applyFilters}><Search className="mr-2 h-4 w-4" />Tìm kiếm</Button>
          <Button variant="outline" onClick={clearFilters}>Xóa lọc</Button>
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-left text-xs font-semibold uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3">Lô sản xuất</th>
                <th className="px-4 py-3">Nội dung phản ánh</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3">Mức độ</th>
                <th className="px-4 py-3">Người xử lý</th>
                <th className="px-4 py-3">Thời gian gửi</th>
                <th className="px-4 py-3 text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <MessageRow colSpan={7} message="Đang tải dữ liệu..." />
              ) : loadError ? (
                <MessageRow colSpan={7} message={loadError} error />
              ) : feedbacks.length === 0 ? (
                <MessageRow colSpan={7} message="Không tìm thấy phản ánh phù hợp." />
              ) : feedbacks.map((feedback) => (
                <tr key={feedback.id} className="transition-colors hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium">{feedback.productionLotName}</td>
                  <td className="max-w-xs px-4 py-3"><p className="truncate" title={feedback.content}>{feedback.content}</p></td>
                  <td className="px-4 py-3"><StatusPill status={feedback.status} /></td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{SEVERITY_LABELS[feedback.severity]}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{feedback.assignedToName || "Chưa gán"}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted-foreground">{formatDate(feedback.createdAt)}</td>
                  <td className="px-4 py-3 text-center"><Button variant="outline" size="sm" onClick={() => void openDetail(feedback)}>Xem chi tiết</Button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {!loading && !loadError && pageInfo.totalPages > 0 && (
          <div className="flex flex-col-reverse items-center justify-between gap-3 border-t px-4 py-3 sm:flex-row">
            <p className="text-xs text-muted-foreground">Hiển thị {pageInfo.page * pageInfo.size + 1}–{Math.min((pageInfo.page + 1) * pageInfo.size, pageInfo.totalElements)} trên {pageInfo.totalElements} phản ánh</p>
            <div className="flex items-center gap-2">
              <Select value={String(size)} onValueChange={(value) => { setSize(Number(value)); setPage(0); }}>
                <SelectTrigger className="h-8 w-[90px] text-xs">
                  <SelectValue>{size} dòng</SelectValue>
                </SelectTrigger>
                <SelectContent>{[5, 10, 20, 50].map((value) => <SelectItem key={value} value={String(value)}>{value} dòng</SelectItem>)}</SelectContent>
              </Select>
              <Button variant="outline" size="icon" className="h-8 w-8" disabled={pageInfo.first} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft className="h-4 w-4" /></Button>
              <span className="min-w-[60px] text-center text-xs text-muted-foreground">{pageInfo.page + 1} / {pageInfo.totalPages}</span>
              <Button variant="outline" size="icon" className="h-8 w-8" disabled={pageInfo.last} onClick={() => setPage((value) => value + 1)}><ChevronRight className="h-4 w-4" /></Button>
            </div>
          </div>
        )}
      </div>

      <FeedbackDetailSheet
        open={detailOpen}
        feedback={selectedFeedback}
        canProcess={canProcess}
        onUpdated={updateFeedback}
        onClose={() => { setDetailOpen(false); setSelectedFeedback(null); }}
      />
    </div>
  );
}

function Summary({ title, value }: { title: string; value: string | number }) {
  return <div className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm"><p className="text-sm font-medium text-muted-foreground">{title}</p><p className="mt-1 text-2xl font-bold text-emerald-700">{value}</p></div>;
}

function MessageRow({ colSpan, message, error = false }: { colSpan: number; message: string; error?: boolean }) {
  return <tr><td colSpan={colSpan} className={`px-4 py-12 text-center ${error ? "text-destructive" : "text-muted-foreground"}`}>{message}</td></tr>;
}

function FeedbackDetailSheet({
  feedback,
  open,
  canProcess,
  onUpdated,
  onClose,
}: {
  feedback: ProductFeedback | null;
  open: boolean;
  canProcess: boolean;
  onUpdated: (feedback: ProductFeedback) => void;
  onClose: () => void;
}) {
  const [members, setMembers] = useState<OrganizationMember[]>([]);
  const [assigneeId, setAssigneeId] = useState("");
  const [severity, setSeverity] = useState<ProductFeedbackSeverity>("INFORMATION");
  const [traceCodeId, setTraceCodeId] = useState("");
  const [traceCodeValue, setTraceCodeValue] = useState("");
  const [traceCodeOptions, setTraceCodeOptions] = useState<TraceCode[]>([]);
  const [shipments, setShipments] = useState<Shipment[]>([]);
  const [recallShipmentId, setRecallShipmentId] = useState("");
  const [loadingTraceCodes, setLoadingTraceCodes] = useState(false);
  const [traceCodeLoadError, setTraceCodeLoadError] = useState<string | null>(null);
  const [processingContent, setProcessingContent] = useState("");
  const [publicResponse, setPublicResponse] = useState("");
  const [closeReason, setCloseReason] = useState("");
  const [recallReason, setRecallReason] = useState("");
  const [recallEvidence, setRecallEvidence] = useState("");
  const [saving, setSaving] = useState(false);

  const processingDraft = { severity, traceCodeId, processingContent, publicResponse };
  const hasUnsavedClassification = feedback != null
    && checkUnsavedClassification(feedback, processingDraft);
  const hasUnsavedProcessing = feedback != null
    && checkUnsavedProcessing(feedback, processingDraft);

  useEffect(() => {
    if (!feedback) return;
    setAssigneeId(feedback.assignedToUserId ?? "");
    setSeverity(feedback.severity);
    setTraceCodeId(feedback.traceCodeId ?? "");
    setTraceCodeValue(feedback.traceCodeValue ?? "");
    setTraceCodeOptions([]);
    setShipments([]);
    setRecallShipmentId("");
    setTraceCodeLoadError(null);
    setProcessingContent(feedback.processingContent ?? "");
    setPublicResponse(feedback.publicResponse ?? "");
    setCloseReason(feedback.closeReason ?? "");
    setRecallReason("");
    setRecallEvidence("");
  }, [feedback]);

  useEffect(() => {
    if (!open || !canProcess) return;
    getOrganizationMembers("ACTIVE")
      .then((items) => setMembers(items.filter((item) => item.roleCode === "VT-03")))
      .catch(() => setMembers([]));
  }, [canProcess, open]);

  useEffect(() => {
    if (
      !open
      || !canProcess
      || !feedback
      || feedback.status === "CLOSED"
    ) return;

    let cancelled = false;
    setLoadingTraceCodes(true);
    setTraceCodeLoadError(null);
    getShipmentsByProductionLot(feedback.productionLotId)
      .then((shipments) => {
        if (cancelled) return;
        setShipments(shipments);
        const uniqueCodes = new Map<string, TraceCode>();
        shipments.forEach((shipment) => {
          shipment.traceCodes.forEach((traceCode) => {
            uniqueCodes.set(traceCode.id, traceCode);
          });
        });
        setTraceCodeOptions(
          Array.from(uniqueCodes.values()).sort((a, b) =>
            a.codeValue.localeCompare(b.codeValue, "vi"),
          ),
        );
        const linkedShipment = feedback.traceCodeId
          ? shipments.find((shipment) => shipment.traceCodes.some((code) => code.id === feedback.traceCodeId))
          : undefined;
        const recallableShipments = shipments.filter((shipment) => shipment.status !== "RECALLED");
        setRecallShipmentId(linkedShipment?.id ?? (recallableShipments.length === 1 ? recallableShipments[0].id : ""));
      })
      .catch(() => {
        if (!cancelled) {
          setTraceCodeOptions([]);
          setShipments([]);
          setRecallShipmentId("");
          setTraceCodeLoadError("Không thể tải danh sách mã tem của lô sản xuất.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingTraceCodes(false);
      });

    return () => {
      cancelled = true;
    };
  }, [canProcess, feedback, open, severity]);

  const handleTraceCodeValueChange = (value: string) => {
    setTraceCodeValue(value);
    const normalizedValue = value.trim().toLocaleLowerCase("vi");
    const selectedCode = traceCodeOptions.find(
      (traceCode) => traceCode.codeValue.toLocaleLowerCase("vi") === normalizedValue,
    );
    setTraceCodeId(selectedCode?.id ?? "");
    const selectedShipment = selectedCode
      ? shipments.find((shipment) => shipment.traceCodes.some((code) => code.id === selectedCode.id))
      : undefined;
    setRecallShipmentId(selectedShipment?.id ?? "");
  };

  const runAction = async (action: () => Promise<ProductFeedback>, success: string) => {
    try {
      setSaving(true);
      const updated = await action();
      onUpdated(updated);
      toast.success(success);
    } catch (error) {
      toast.error(errorMessage(error, "Không thể cập nhật phản ánh"));
    } finally {
      setSaving(false);
    }
  };

  const refreshDetail = async () => {
    if (!feedback) return;
    onUpdated(await getProductFeedbackById(feedback.id));
  };

  return (
    <Sheet open={open} onOpenChange={(nextOpen) => !nextOpen && onClose()}>
      <SheetContent side="right" className="flex w-full flex-col gap-0 p-0 sm:max-w-xl">
        {feedback && <>
          <SheetHeader className="border-b px-6 py-4">
            <SheetTitle>Chi tiết và xử lý phản ánh</SheetTitle>
            <SheetDescription><StatusPill status={feedback.status} /></SheetDescription>
          </SheetHeader>
          <div className="min-h-0 flex-1 space-y-6 overflow-y-auto px-6 py-4">
            <DetailSection title="Thông tin phản ánh" contentClassName="space-y-3">
              <DetailField label="Mã phản ánh" mono value={maskId(feedback.id)} />
              <DetailField label="Nội dung" value={<span className="block whitespace-pre-wrap font-normal">{feedback.content}</span>} />
              <DetailField label="Thời gian gửi" value={formatDate(feedback.createdAt)} />
              <DetailField label="Mã tem" value={feedback.traceCodeValue || "—"} />
            </DetailSection>
            <DetailSection title="Sản phẩm và tổ chức" contentClassName="space-y-3">
              <DetailField label="Lô sản xuất" value={feedback.productionLotName} />
              <DetailField label="Loại nông sản" value={feedback.productCategoryName || "—"} />
              <DetailField label="Tổ chức" value={feedback.organizationName || "—"} />
            </DetailSection>

            {canProcess && feedback.status !== "CLOSED" && <>
              <section className="space-y-3 rounded-lg border p-4">
                <h3 className="font-semibold">Gán người xử lý</h3>
                <Select value={assigneeId || undefined} onValueChange={(value) => setAssigneeId(value ?? "")}>
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn người ghi sự kiện đang hoạt động">
                      {members.find((member) => member.userId === assigneeId)?.fullName
                        ?? (feedback.assignedToUserId === assigneeId ? feedback.assignedToName : undefined)
                        ?? "Chọn người ghi sự kiện đang hoạt động"}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>{members.map((member) => <SelectItem key={member.userId} value={member.userId}>{member.fullName}</SelectItem>)}</SelectContent>
                </Select>
                <Button disabled={!assigneeId || saving} onClick={() => void runAction(() => assignProductFeedback(feedback.id, { assignedToUserId: assigneeId }), "Đã gán người xử lý")}>Lưu người xử lý</Button>
              </section>

              <section className="space-y-3 rounded-lg border p-4">
                <h3 className="font-semibold">Phân loại và nội dung xử lý</h3>
                <div className="space-y-2"><Label>Mức độ</Label><Select value={severity} onValueChange={(value) => setSeverity(value as ProductFeedbackSeverity)}><SelectTrigger><SelectValue>{SEVERITY_LABELS[severity]}</SelectValue></SelectTrigger><SelectContent>{Object.entries(SEVERITY_LABELS).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent></Select></div>
                {severity === "COUNTERFEIT_SUSPECTED" && (
                  <div className="space-y-2">
                    <Label htmlFor="trace-code-value">Mã tem *</Label>
                    {feedback.traceCodeId ? (
                      <>
                        <Input
                          id="trace-code-value"
                          value={feedback.traceCodeValue ?? ""}
                          placeholder="Mã tem đã liên kết"
                          readOnly
                          className="bg-muted"
                        />
                        <p className="text-xs text-muted-foreground">
                          Mã tem được ghi nhận từ phản ánh và không thể thay đổi tại đây.
                        </p>
                      </>
                    ) : (
                      <>
                        <Input
                          id="trace-code-value"
                          list={`trace-code-options-${feedback.id}`}
                          value={traceCodeValue}
                          onChange={(event) => handleTraceCodeValueChange(event.target.value)}
                          placeholder={loadingTraceCodes ? "Đang tải mã tem..." : "Nhập hoặc chọn mã tem thuộc lô sản xuất"}
                          disabled={loadingTraceCodes || Boolean(traceCodeLoadError)}
                          autoComplete="off"
                        />
                        <datalist id={`trace-code-options-${feedback.id}`}>
                          {traceCodeOptions.map((traceCode) => (
                            <option key={traceCode.id} value={traceCode.codeValue} />
                          ))}
                        </datalist>
                        {traceCodeLoadError ? (
                          <p className="text-xs text-destructive">{traceCodeLoadError}</p>
                        ) : traceCodeOptions.length === 0 && !loadingTraceCodes ? (
                          <p className="text-xs text-muted-foreground">
                            Lô sản xuất này chưa có mã tem để liên kết.
                          </p>
                        ) : traceCodeValue.trim() && !traceCodeId ? (
                          <p className="text-xs text-destructive">
                            Vui lòng chọn một mã tem hợp lệ trong danh sách.
                          </p>
                        ) : (
                          <p className="text-xs text-muted-foreground">
                            Chỉ hiển thị các mã tem thuộc lô sản xuất của phản ánh.
                          </p>
                        )}
                      </>
                    )}
                  </div>
                )}
                <div className="space-y-2"><Label htmlFor="processing-content">Nội dung xử lý nội bộ</Label><Textarea id="processing-content" maxLength={4000} value={processingContent} onChange={(event) => setProcessingContent(event.target.value)} /></div>
                <div className="space-y-2"><Label htmlFor="public-response">Phản hồi công khai</Label><Textarea id="public-response" maxLength={2000} value={publicResponse} onChange={(event) => setPublicResponse(event.target.value)} /></div>
                <Button disabled={!feedback.assignedToUserId || saving || (severity === "COUNTERFEIT_SUSPECTED" && !traceCodeId.trim())} onClick={() => void runAction(() => updateProductFeedbackProcessing(feedback.id, { severity, traceCodeId: traceCodeId.trim() || null, processingContent, publicResponse }), "Đã lưu nội dung xử lý")}>Lưu xử lý</Button>
                {hasUnsavedClassification ? (
                  <p className="text-sm text-amber-700">
                    Mức độ hoặc mã tem chưa được lưu. Hãy lưu xử lý trước khi tạo đề nghị thu hồi hoặc đóng phản ánh.
                  </p>
                ) : hasUnsavedProcessing ? (
                  <p className="text-sm text-amber-700">
                    Nội dung đang có thay đổi chưa lưu. Hãy lưu xử lý trước khi tạo đề nghị thu hồi.
                  </p>
                ) : null}
              </section>

              {feedback.severity !== "INFORMATION" && feedback.status === "IN_PROGRESS" && (
                <section className="space-y-3 rounded-lg border border-amber-200 p-4">
                  <h3 className="font-semibold">Đề nghị thu hồi lô hàng</h3>
                  <div className="space-y-2">
                    <Label htmlFor="recall-shipment">Lô hàng cần thu hồi *</Label>
                    <Select
                      value={recallShipmentId || undefined}
                      onValueChange={(value) => setRecallShipmentId(value ?? "")}
                      disabled={Boolean(feedback.traceCodeId) || loadingTraceCodes}
                    >
                      <SelectTrigger id="recall-shipment"><SelectValue placeholder="Chọn lô hàng" /></SelectTrigger>
                      <SelectContent>
                        {shipments.filter((shipment) => shipment.status !== "RECALLED").map((shipment) => (
                          <SelectItem key={shipment.id} value={shipment.id}>{shipment.name} ({shipment.traceCodes.length} mã tem)</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {feedback.traceCodeId && recallShipmentId ? (
                      <p className="text-xs text-muted-foreground">Lô hàng được xác định tự động từ mã tem của phản ánh và không thể thay đổi.</p>
                    ) : (
                      <p className="text-xs text-muted-foreground">Chỉ lô hàng được chọn và toàn bộ mã tem thuộc lô hàng đó bị thu hồi; các lô hàng khác không bị ảnh hưởng.</p>
                    )}
                  </div>
                  <div className="space-y-2"><Label htmlFor="recall-reason">Lý do *</Label><Textarea id="recall-reason" maxLength={1000} value={recallReason} onChange={(event) => setRecallReason(event.target.value)} /></div>
                  <div className="space-y-2"><Label htmlFor="recall-evidence">Bằng chứng</Label><Textarea id="recall-evidence" maxLength={2000} value={recallEvidence} onChange={(event) => setRecallEvidence(event.target.value)} /></div>
                  <Button variant="destructive" disabled={!recallShipmentId || !recallReason.trim() || saving || feedback.hasPendingRecallRequest || hasUnsavedProcessing} onClick={async () => {
                    try {
                      setSaving(true);
                      await createProductFeedbackRecall(feedback.id, { shipmentId: recallShipmentId, reason: recallReason.trim(), evidence: recallEvidence.trim() || undefined });
                      await refreshDetail();
                      toast.success("Đã tạo đề nghị thu hồi lô hàng");
                    } catch (error) {
                      toast.error(errorMessage(error, "Không thể tạo đề nghị thu hồi"));
                    } finally {
                      setSaving(false);
                    }
                  }}>Tạo đề nghị thu hồi lô hàng</Button>
                </section>
              )}

              <section className="space-y-3 rounded-lg border border-emerald-200 p-4">
                <h3 className="font-semibold">Đóng phản ánh</h3>
                {feedback.hasPendingRecallRequest && <p className="text-sm text-amber-700">Cần xử lý xong đề nghị thu hồi đang chờ trước khi đóng.</p>}
                {hasUnsavedClassification && <p className="text-sm text-amber-700">Hãy lưu mức độ và mã tem trước khi đóng phản ánh.</p>}
                <div className="space-y-2"><Label htmlFor="close-reason">Lý do đóng *</Label><Textarea id="close-reason" maxLength={1000} value={closeReason} onChange={(event) => setCloseReason(event.target.value)} /></div>
                <Button disabled={!feedback.assignedToUserId || !processingContent.trim() || !closeReason.trim() || feedback.hasPendingRecallRequest || hasUnsavedClassification || saving} onClick={() => void runAction(() => closeProductFeedback(feedback.id, { processingContent, publicResponse, closeReason: closeReason.trim() }), "Đã đóng phản ánh")}>Đóng phản ánh</Button>
              </section>
            </>}

            {feedback.status === "CLOSED" && <DetailSection title="Kết quả xử lý" contentClassName="space-y-3"><DetailField label="Nội dung xử lý" value={feedback.processingContent || "—"} /><DetailField label="Phản hồi công khai" value={feedback.publicResponse || "—"} /><DetailField label="Lý do đóng" value={feedback.closeReason || "—"} /><DetailField label="Người đóng" value={feedback.closedByName || "—"} /><DetailField label="Thời điểm đóng" value={formatDate(feedback.closedAt)} /></DetailSection>}
          </div>
          <SheetFooter className="border-t px-6 py-4"><Button variant="outline" className="w-full" onClick={onClose}>Đóng cửa sổ</Button></SheetFooter>
        </>}
      </SheetContent>
    </Sheet>
  );
}
