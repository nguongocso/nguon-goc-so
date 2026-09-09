import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  CheckCircle2,
  ClipboardCheck,
  LoaderCircle,
  MessageSquareText,
  PackageSearch,
  RefreshCw,
  Save,
  TriangleAlert,
  UserRoundCheck,
} from "lucide-react";
import { toast } from "sonner";

import {
  assignProductFeedback,
  closeProductFeedback,
  createProductFeedbackRecall,
  getProductFeedbackById,
  updateProductFeedbackProcessing,
} from "@/api/productFeedbackApi";
import { getOrganizationMembers } from "@/api/memberApi";
import { getShipmentsByProductionLot } from "@/api/shipmentApi";
import { DetailField } from "@/components/common/detail/DetailField";
import { HelpButton } from "@/components/help/HelpButton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/hooks/useAuth";
import { maskId } from "@/lib/utils";
import type { OrganizationMember } from "@/types/member";
import type {
  ProductFeedback,
  ProductFeedbackSeverity,
} from "@/types/productFeedback";
import type { Shipment, TraceCode } from "@/types/shipment";
import {
  hasUnsavedClassification as checkUnsavedClassification,
  hasUnsavedProcessing as checkUnsavedProcessing,
} from "./productFeedbackDraft";
import {
  formatProductFeedbackDate,
  getProductFeedbackErrorMessage,
  PRODUCT_FEEDBACK_SEVERITY_LABELS,
  ProductFeedbackStatusPill,
} from "./productFeedbackPresentation";

const RECALL_STATUS_LABELS = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Đã từ chối",
} as const;

export default function ProductFeedbackDetailPage() {
  const { feedbackId } = useParams<{ feedbackId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const canProcess = user?.roleCode === "VT-02";

  const [feedback, setFeedback] = useState<ProductFeedback | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
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

  const loadFeedback = useCallback(async () => {
    if (!feedbackId) {
      setLoadError("Đường dẫn phản ánh không hợp lệ.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setLoadError(null);
      setFeedback(await getProductFeedbackById(feedbackId));
    } catch (error) {
      const message = getProductFeedbackErrorMessage(error, "Không thể tải chi tiết phản ánh");
      setFeedback(null);
      setLoadError(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [feedbackId]);

  useEffect(() => {
    void loadFeedback();
  }, [loadFeedback]);

  useEffect(() => {
    if (!feedback) return;

    setAssigneeId(feedback.assignedToUserId ?? "");
    setSeverity(feedback.severity);
    setTraceCodeId(feedback.traceCodeId ?? "");
    setTraceCodeValue(feedback.traceCodeValue ?? "");
    setProcessingContent(feedback.processingContent ?? "");
    setPublicResponse(feedback.publicResponse ?? "");
    setCloseReason(feedback.closeReason ?? "");
    setRecallReason("");
    setRecallEvidence("");
  }, [feedback]);

  useEffect(() => {
    if (!canProcess) return;

    getOrganizationMembers("ACTIVE")
      .then((items) => setMembers(items.filter((item) => item.roleCode === "VT-03")))
      .catch(() => setMembers([]));
  }, [canProcess]);

  useEffect(() => {
    if (!canProcess || !feedback || feedback.status === "CLOSED") return;

    let cancelled = false;
    setLoadingTraceCodes(true);
    setTraceCodeLoadError(null);

    getShipmentsByProductionLot(feedback.productionLotId)
      .then((items) => {
        if (cancelled) return;

        setShipments(items);
        const uniqueCodes = new Map<string, TraceCode>();
        items.forEach((shipment) => {
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
          ? items.find((shipment) =>
              shipment.traceCodes.some((code) => code.id === feedback.traceCodeId),
            )
          : undefined;
        const recallableShipments = items.filter((shipment) => shipment.status !== "RECALLED");
        setRecallShipmentId(
          linkedShipment?.id
            ?? (recallableShipments.length === 1 ? recallableShipments[0].id : ""),
        );
      })
      .catch(() => {
        if (cancelled) return;
        setTraceCodeOptions([]);
        setShipments([]);
        setRecallShipmentId("");
        setTraceCodeLoadError("Không thể tải danh sách mã tem của lô sản xuất.");
      })
      .finally(() => {
        if (!cancelled) setLoadingTraceCodes(false);
      });

    return () => {
      cancelled = true;
    };
  }, [canProcess, feedback]);

  const processingDraft = { severity, traceCodeId, processingContent, publicResponse };
  const hasUnsavedClassification = feedback != null
    && checkUnsavedClassification(feedback, processingDraft);
  const hasUnsavedProcessing = feedback != null
    && checkUnsavedProcessing(feedback, processingDraft);
  const selectedRecallShipment = shipments.find(
    (shipment) => shipment.id === recallShipmentId,
  );

  const handleTraceCodeValueChange = (value: string) => {
    setTraceCodeValue(value);
    const normalizedValue = value.trim().toLocaleLowerCase("vi");
    const selectedCode = traceCodeOptions.find(
      (traceCode) => traceCode.codeValue.toLocaleLowerCase("vi") === normalizedValue,
    );
    setTraceCodeId(selectedCode?.id ?? "");
    const selectedShipment = selectedCode
      ? shipments.find((shipment) =>
          shipment.traceCodes.some((code) => code.id === selectedCode.id),
        )
      : undefined;
    setRecallShipmentId(selectedShipment?.id ?? "");
  };

  const runAction = async (
    action: () => Promise<ProductFeedback>,
    successMessage: string,
  ) => {
    try {
      setSaving(true);
      setFeedback(await action());
      toast.success(successMessage);
    } catch (error) {
      toast.error(getProductFeedbackErrorMessage(error, "Không thể cập nhật phản ánh"));
    } finally {
      setSaving(false);
    }
  };

  const refreshDetail = async () => {
    if (!feedbackId) return;
    setFeedback(await getProductFeedbackById(feedbackId));
  };

  const createRecall = async () => {
    if (!feedback) return;
    try {
      setSaving(true);
      await createProductFeedbackRecall(feedback.id, {
        shipmentId: recallShipmentId,
        reason: recallReason.trim(),
        evidence: recallEvidence.trim() || undefined,
      });
      await refreshDetail();
      toast.success("Đã tạo yêu cầu thu hồi lô hàng");
    } catch (error) {
      toast.error(getProductFeedbackErrorMessage(error, "Không thể tạo yêu cầu thu hồi"));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[55vh] items-center justify-center text-muted-foreground">
        <LoaderCircle className="mr-2 h-5 w-5 animate-spin" />
        Đang tải chi tiết phản ánh...
      </div>
    );
  }

  if (loadError || !feedback) {
    return (
      <div className="mx-auto max-w-3xl space-y-4 py-8">
        <Alert variant="destructive">
          <TriangleAlert />
          <AlertDescription>{loadError ?? "Không tìm thấy phản ánh."}</AlertDescription>
        </Alert>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate("/product-feedbacks")}>
            Về danh sách phản ánh
          </Button>
          <Button onClick={() => void loadFeedback()}>Thử lại</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-emerald-100 p-2.5 text-emerald-700">
            <MessageSquareText className="h-6 w-6" />
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                Chi tiết và xử lý phản ánh
              </h1>
              <ProductFeedbackStatusPill status={feedback.status} />
            </div>
            <p className="mt-1 text-sm text-muted-foreground">
              Theo dõi, phân loại và xử lý phản ánh của người tiêu dùng.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="product-feedback" />
          <Button variant="outline" onClick={() => void loadFeedback()} disabled={saving}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader className="border-b">
            <CardTitle className="flex items-center gap-2 text-base">
              <MessageSquareText className="h-5 w-5 text-emerald-600" />
              Thông tin phản ánh
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-5 sm:grid-cols-2">
            <DetailField label="Mã phản ánh" mono value={maskId(feedback.id)} />
            <DetailField label="Thời gian gửi" value={formatProductFeedbackDate(feedback.createdAt)} />
            <DetailField
              className="sm:col-span-2"
              label="Nội dung"
              value={<span className="block whitespace-pre-wrap font-normal">{feedback.content}</span>}
            />
            <DetailField label="Mã tem" mono value={feedback.traceCodeValue} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b">
            <CardTitle className="flex items-center gap-2 text-base">
              <PackageSearch className="h-5 w-5 text-blue-600" />
              Sản phẩm và tổ chức
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-5 sm:grid-cols-2">
            <DetailField label="Lô sản xuất" value={feedback.productionLotName} />
            <DetailField label="Loại nông sản" value={feedback.productCategoryName} />
            <DetailField label="Tổ chức" value={feedback.organizationName} />
            <DetailField label="Người xử lý" value={feedback.assignedToName || "Chưa gán"} />
            <DetailField label="Mức độ" value={PRODUCT_FEEDBACK_SEVERITY_LABELS[feedback.severity]} />
            <DetailField
              label="Yêu cầu thu hồi gần nhất"
              value={feedback.latestRecallRequestStatus
                ? RECALL_STATUS_LABELS[feedback.latestRecallRequestStatus]
                : "Chưa có"}
            />
          </CardContent>
        </Card>
      </div>

      {canProcess && feedback.status !== "CLOSED" && (
        <>
          <Card>
            <CardHeader className="border-b">
              <CardTitle className="flex items-center gap-2 text-base">
                <UserRoundCheck className="h-5 w-5 text-blue-600" />
                Gán người xử lý
              </CardTitle>
              <CardDescription>
                Chọn một người ghi sự kiện đang hoạt động trong tổ chức để phụ trách phản ánh.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Select value={assigneeId || undefined} onValueChange={(value) => setAssigneeId(value ?? "")}>
                <SelectTrigger className="w-full md:max-w-xl">
                  <SelectValue placeholder="Chọn người ghi sự kiện đang hoạt động">
                    {members.find((member) => member.userId === assigneeId)?.fullName
                      ?? (feedback.assignedToUserId === assigneeId ? feedback.assignedToName : undefined)
                      ?? "Chọn người ghi sự kiện đang hoạt động"}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {members.map((member) => (
                    <SelectItem key={member.userId} value={member.userId}>
                      {member.fullName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                disabled={!assigneeId || assigneeId === feedback.assignedToUserId || saving}
                onClick={() => void runAction(
                  () => assignProductFeedback(feedback.id, { assignedToUserId: assigneeId }),
                  "Đã gán người xử lý",
                )}
              >
                <Save className="mr-2 h-4 w-4" />
                Lưu người xử lý
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="border-b">
              <CardTitle className="flex items-center gap-2 text-base">
                <ClipboardCheck className="h-5 w-5 text-emerald-600" />
                Phân loại và nội dung xử lý
              </CardTitle>
              <CardDescription>
                Nội dung nội bộ chỉ dành cho đơn vị xử lý; phản hồi công khai có thể được cung cấp cho người tiêu dùng.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="grid gap-5 lg:grid-cols-2">
                <div className="space-y-2">
                  <Label>Mức độ</Label>
                  <Select
                    value={severity}
                    onValueChange={(value) => setSeverity(value as ProductFeedbackSeverity)}
                  >
                    <SelectTrigger>
                      <SelectValue>{PRODUCT_FEEDBACK_SEVERITY_LABELS[severity]}</SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {Object.entries(PRODUCT_FEEDBACK_SEVERITY_LABELS).map(([value, label]) => (
                        <SelectItem key={value} value={value}>{label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

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
                          className="bg-muted font-mono"
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
                          placeholder={loadingTraceCodes
                            ? "Đang tải mã tem..."
                            : "Nhập hoặc chọn mã tem thuộc lô sản xuất"}
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
              </div>

              <div className="grid gap-5 lg:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="processing-content">Nội dung xử lý nội bộ</Label>
                  <Textarea
                    id="processing-content"
                    className="min-h-32"
                    maxLength={4000}
                    value={processingContent}
                    onChange={(event) => setProcessingContent(event.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="public-response">Phản hồi công khai</Label>
                  <Textarea
                    id="public-response"
                    className="min-h-32"
                    maxLength={2000}
                    value={publicResponse}
                    onChange={(event) => setPublicResponse(event.target.value)}
                  />
                </div>
              </div>

              <Button
                disabled={
                  !feedback.assignedToUserId
                  || !hasUnsavedProcessing
                  || saving
                  || (severity === "COUNTERFEIT_SUSPECTED" && !traceCodeId.trim())
                }
                onClick={() => void runAction(
                  () => updateProductFeedbackProcessing(feedback.id, {
                    severity,
                    traceCodeId: traceCodeId.trim() || null,
                    processingContent,
                    publicResponse,
                  }),
                  "Đã lưu nội dung xử lý",
                )}
              >
                <Save className="mr-2 h-4 w-4" />
                Lưu xử lý
              </Button>

              {hasUnsavedClassification ? (
                <Alert variant="warning">
                  <TriangleAlert />
                  <AlertDescription>
                    Mức độ hoặc mã tem chưa được lưu. Hãy lưu xử lý trước khi tạo yêu cầu thu hồi hoặc đóng phản ánh.
                  </AlertDescription>
                </Alert>
              ) : hasUnsavedProcessing ? (
                <Alert variant="warning">
                  <TriangleAlert />
                  <AlertDescription>
                    Nội dung đang có thay đổi chưa lưu. Hãy lưu xử lý trước khi tạo yêu cầu thu hồi.
                  </AlertDescription>
                </Alert>
              ) : null}
            </CardContent>
          </Card>

          <div className="space-y-6">
            {feedback.severity !== "INFORMATION" && feedback.status === "IN_PROGRESS" && (
              <Card className="border-amber-200">
                <CardHeader className="border-b border-amber-100">
                  <CardTitle className="flex items-center gap-2 text-base">
                    <TriangleAlert className="h-5 w-5 text-amber-600" />
                    Yêu cầu thu hồi lô hàng
                  </CardTitle>
                  <CardDescription>
                    Tạo yêu cầu để người quản lý khác xem xét và phê duyệt thu hồi.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="recall-shipment">Lô hàng cần thu hồi *</Label>
                    {feedback.traceCodeId ? (
                      <Input
                        id="recall-shipment"
                        value={selectedRecallShipment?.name ?? ""}
                        placeholder={loadingTraceCodes
                          ? "Đang xác định lô hàng..."
                          : "Không xác định được lô hàng"}
                        readOnly
                        className="bg-muted"
                      />
                    ) : (
                      <Select
                        value={recallShipmentId || undefined}
                        onValueChange={(value) => setRecallShipmentId(value ?? "")}
                        disabled={loadingTraceCodes}
                      >
                        <SelectTrigger id="recall-shipment">
                          <SelectValue placeholder="Chọn lô hàng">
                            {selectedRecallShipment
                              ? `${selectedRecallShipment.name} (${selectedRecallShipment.traceCodes.length} mã tem)`
                              : "Chọn lô hàng"}
                          </SelectValue>
                        </SelectTrigger>
                        <SelectContent>
                          {shipments
                            .filter((shipment) => shipment.status !== "RECALLED")
                            .map((shipment) => (
                              <SelectItem key={shipment.id} value={shipment.id}>
                                {shipment.name} ({shipment.traceCodes.length} mã tem)
                              </SelectItem>
                            ))}
                        </SelectContent>
                      </Select>
                    )}
                    <p className="text-xs text-muted-foreground">
                      {feedback.traceCodeId && recallShipmentId
                        ? "Lô hàng được xác định tự động từ mã tem của phản ánh và không thể thay đổi."
                        : "Chỉ lô hàng được chọn và toàn bộ mã tem thuộc lô hàng đó bị thu hồi; các lô hàng khác không bị ảnh hưởng."}
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="recall-reason">Lý do *</Label>
                    <Textarea
                      id="recall-reason"
                      className="min-h-24"
                      maxLength={1000}
                      value={recallReason}
                      onChange={(event) => setRecallReason(event.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="recall-evidence">Bằng chứng</Label>
                    <Textarea
                      id="recall-evidence"
                      className="min-h-24"
                      maxLength={2000}
                      value={recallEvidence}
                      onChange={(event) => setRecallEvidence(event.target.value)}
                    />
                  </div>
                  {feedback.hasPendingRecallRequest && (
                    <p className="text-sm text-amber-700">
                      Phản ánh đang có một yêu cầu thu hồi chờ duyệt.
                    </p>
                  )}
                  <Button
                    variant="destructive"
                    disabled={
                      !recallShipmentId
                      || !recallReason.trim()
                      || saving
                      || feedback.hasPendingRecallRequest
                      || hasUnsavedProcessing
                    }
                    onClick={() => void createRecall()}
                  >
                    Tạo yêu cầu thu hồi lô hàng
                  </Button>
                </CardContent>
              </Card>
            )}

            <Card className="border-emerald-200">
              <CardHeader className="border-b border-emerald-100">
                <CardTitle className="flex items-center gap-2 text-base">
                  <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                  Đóng phản ánh
                </CardTitle>
                <CardDescription>
                  Chỉ đóng khi đã có người xử lý, nội dung xử lý và không còn yêu cầu thu hồi chờ duyệt.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {feedback.hasPendingRecallRequest && (
                  <Alert variant="warning">
                    <TriangleAlert />
                    <AlertDescription>
                      Cần xử lý xong yêu cầu thu hồi đang chờ trước khi đóng.
                    </AlertDescription>
                  </Alert>
                )}
                {hasUnsavedClassification && (
                  <Alert variant="warning">
                    <TriangleAlert />
                    <AlertDescription>
                      Hãy lưu mức độ và mã tem trước khi đóng phản ánh.
                    </AlertDescription>
                  </Alert>
                )}
                <div className="space-y-2">
                  <Label htmlFor="close-reason">Lý do đóng *</Label>
                  <Textarea
                    id="close-reason"
                    className="min-h-28"
                    maxLength={1000}
                    value={closeReason}
                    onChange={(event) => setCloseReason(event.target.value)}
                  />
                </div>
                <Button
                  disabled={
                    !feedback.assignedToUserId
                    || !processingContent.trim()
                    || !closeReason.trim()
                    || feedback.hasPendingRecallRequest
                    || hasUnsavedClassification
                    || saving
                  }
                  onClick={() => void runAction(
                    () => closeProductFeedback(feedback.id, {
                      processingContent,
                      publicResponse,
                      closeReason: closeReason.trim(),
                    }),
                    "Đã đóng phản ánh",
                  )}
                >
                  <CheckCircle2 className="mr-2 h-4 w-4" />
                  Đóng phản ánh
                </Button>
              </CardContent>
            </Card>
          </div>
        </>
      )}

      {feedback.status === "CLOSED" && (
        <Card className="border-emerald-200">
          <CardHeader className="border-b border-emerald-100">
            <CardTitle className="flex items-center gap-2 text-base">
              <CheckCircle2 className="h-5 w-5 text-emerald-600" />
              Kết quả xử lý
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-5 sm:grid-cols-2">
            <DetailField label="Nội dung xử lý" value={feedback.processingContent} />
            <DetailField label="Phản hồi công khai" value={feedback.publicResponse} />
            <DetailField label="Lý do đóng" value={feedback.closeReason} />
            <DetailField label="Người đóng" value={feedback.closedByName} />
            <DetailField label="Thời điểm đóng" value={formatProductFeedbackDate(feedback.closedAt)} />
          </CardContent>
        </Card>
      )}

      {!canProcess && feedback.status !== "CLOSED" && (
        <Card>
          <CardHeader className="border-b">
            <CardTitle className="text-base">Thông tin xử lý hiện tại</CardTitle>
            <CardDescription>Tài khoản hiện tại có quyền xem nhưng không có quyền cập nhật phản ánh.</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-5 sm:grid-cols-2">
            <DetailField label="Nội dung xử lý" value={feedback.processingContent} />
            <DetailField label="Phản hồi công khai" value={feedback.publicResponse} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
