import { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { toast } from "sonner";
import {
  AlertTriangle,
  FileUp,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getInspectionRequestDetail, recordInspectionRequestResults, uploadInspectionResultFile } from "@/api/certificationApi";
import type { InspectionRequestDetailResponse } from "@/types/certification";

const toISODate = (date: Date) => {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};

interface CriterionInputState {
  criterionId: string;
  passed: boolean | null;
  resultDate: string;
  expiryDate: string;
  filePath: string;
  selectedFileName: string;
  uploading: boolean;
}

interface RecordInspectionResultDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  requestId: string;
  onRecorded: () => void;
}

export function RecordInspectionResultDialog({
  open,
  onOpenChange,
  requestId,
  onRecorded,
}: RecordInspectionResultDialogProps) {
  const [detail, setDetail] = useState<InspectionRequestDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [criteriaInputs, setCriteriaInputs] = useState<CriterionInputState[]>([]);
  const [touched, setTouched] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const today = toISODate(new Date());

  useEffect(() => {
    if (!open || !requestId) return;
    setDetail(null);
    setDetailError(null);
    setCriteriaInputs([]);
    setTouched(false);
    setSubmitting(false);

    const loadDetail = async () => {
      try {
        setDetailLoading(true);
        const data = await getInspectionRequestDetail(requestId);
        setDetail(data);
        setCriteriaInputs(
          data.criteria.map((criterion) => ({
            criterionId: criterion.criterionId,
            passed: criterion.result?.passed ?? null,
            resultDate: criterion.result?.resultDate ?? today,
            expiryDate: criterion.result?.expiryDate ?? "",
            filePath: criterion.result?.filePath ?? "",
            selectedFileName: "",
            uploading: false,
          }))
        );
      } catch (error) {
        setDetailError(
          axios.isAxiosError(error)
            ? error.response?.data?.message || "Không thể tải chi tiết yêu cầu kiểm nghiệm"
            : "Không thể tải chi tiết yêu cầu kiểm nghiệm"
        );
      } finally {
        setDetailLoading(false);
      }
    };
    void loadDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, requestId]);

  const dateErrorByCriterion = useMemo(() => {
    const errors: Record<string, string> = {};
    for (const input of criteriaInputs) {
      if (input.resultDate === "" || input.expiryDate === "") {
        errors[input.criterionId] = "Vui lòng nhập ngày cấp và ngày hết hiệu lực.";
        continue;
      }
      if (input.expiryDate < input.resultDate) {
        errors[input.criterionId] = "Ngày hết hiệu lực phải sau ngày cấp kết quả.";
        continue;
      }
      if (input.expiryDate < today) {
        errors[input.criterionId] = "Ngày hết hiệu lực phải >= ngày hiện tại.";
      }
    }
    return errors;
  }, [criteriaInputs, today]);

  const allCriteriaAnswered =
    criteriaInputs.length > 0 &&
    criteriaInputs.every((input) => input.passed !== null);

  const allDatesValid = Object.keys(dateErrorByCriterion).length === 0;

  const canSubmit =
    !submitting &&
    detail !== null &&
    allCriteriaAnswered &&
    allDatesValid &&
    criteriaInputs.every((input) => !input.uploading);

  const setCriterionPassed = (criterionId: string, passed: boolean) => {
    setTouched(true);
    setCriteriaInputs((prev) =>
      prev.map((input) =>
        input.criterionId === criterionId ? { ...input, passed } : input
      )
    );
  };

  const setCriterionField = (
    criterionId: string,
    field: "resultDate" | "expiryDate",
    value: string
  ) => {
    setCriteriaInputs((prev) =>
      prev.map((input) =>
        input.criterionId === criterionId ? { ...input, [field]: value } : input
      )
    );
  };

  const handleFileChange = async (criterionId: string, file: File | null) => {
    if (!file) return;
    setCriteriaInputs((prev) =>
      prev.map((input) =>
        input.criterionId === criterionId
          ? { ...input, uploading: true, filePath: "", selectedFileName: file.name }
          : input
      )
    );
    try {
      const url = await uploadInspectionResultFile(criterionId, file);
      setCriteriaInputs((prev) =>
        prev.map((input) =>
          input.criterionId === criterionId
            ? { ...input, filePath: url.filePath, uploading: false }
            : input
        )
      );
    } catch {
      setCriteriaInputs((prev) =>
        prev.map((input) =>
          input.criterionId === criterionId
            ? { ...input, uploading: false, selectedFileName: "" }
            : input
        )
      );
      toast.error("Không thể tải lên phiếu kết quả kiểm nghiệm");
    }
  };

  const handleSubmit = async () => {
    if (!canSubmit || !detail) return;
    setSubmitting(true);
    try {
      await recordInspectionRequestResults(
        detail.testRequestId,
        {
          results: criteriaInputs.map((input) => ({
            criterionId: input.criterionId,
            resultDate: input.resultDate,
            expiryDate: input.expiryDate,
            passed: input.passed as boolean,
            filePath: input.filePath === "" ? null : input.filePath,
          })),
        }
      );
      toast.success("Ghi nhận kết quả kiểm nghiệm thành công");
      onRecorded();
      onOpenChange(false);
    } catch (error) {
      const message = axios.isAxiosError(error)
        ? error.response?.data?.message || "Lỗi không xác định"
        : "Lỗi không xác định";
      toast.error(
        `Không thể ghi nhận kết quả kiểm nghiệm: ${message}`
      );
    } finally {
      setSubmitting(false);
    }
  };

  const recordedCount = useMemo(
    () => detail?.criteria.filter((criterion) => criterion.result !== null).length ?? 0,
    [detail]
  );

  /*
   * Thống kê tổng hợp cập nhật trực tiếp theo trạng thái form,
   * giúp người nhập thấy ngay tỷ lệ Đạt/Không đạt trước khi lưu.
   */
  const liveStats = useMemo(() => {
    const total = detail?.criteria.length ?? 0;
    let passed = 0;
    let failed = 0;
    for (const input of criteriaInputs) {
      if (input.passed === true) passed++;
      else if (input.passed === false) failed++;
    }
    const ratio =
      total > 0 ? Math.round((failed / total) * 1000) / 10 : 0;
    return { total, passed, failed, ratio };
  }, [detail, criteriaInputs]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-emerald-600" />
            Nhập kết quả kiểm nghiệm
          </DialogTitle>
          <DialogDescription>
            {detail
              ? `${detail.testingUnit} — Ngày gửi mẫu: ${detail.sampleSentDate}`
              : "Đang tải thông tin yêu cầu kiểm nghiệm..."}
          </DialogDescription>
        </DialogHeader>

        {detailLoading ? (
          <div className="flex items-center justify-center gap-2 py-10 text-muted-foreground">
            <LoaderCircle className="h-5 w-5 animate-spin text-emerald-500" />
            Đang tải chi tiết yêu cầu...
          </div>
        ) : detailError ? (
          <div className="flex flex-col items-center gap-3 py-10 text-muted-foreground">
            <AlertTriangle className="h-6 w-6 text-amber-500" />
            <p>{detailError}</p>
          </div>
        ) : detail ? (
          <div className="space-y-5">
            <p className="text-sm text-muted-foreground">
              Chọn kết luận Đạt/Không đạt và ngày hiệu lực cho từng chỉ tiêu. Yêu cầu
              chuyển
              <span className="font-medium text-emerald-700"> Đạt </span>
              khi tất cả chỉ tiêu đều đạt; nếu có chỉ tiêu không đạt, yêu cầu chuyển
              <span className="font-medium text-red-700"> Không đạt</span>.
              {recordedCount > 0 &&
                ` Đã có ${recordedCount}/${detail.criteria.length} chỉ tiêu có kết quả, có thể sửa lại.`}
            </p>

            {/* Tổng hợp kết quả kiểm nghiệm */}
            <div
              className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm"
              aria-live="polite"
              aria-label={`Kết quả kiểm nghiệm: đạt ${liveStats.passed} trên ${liveStats.total} chỉ tiêu, không đạt ${liveStats.failed} trên ${liveStats.total} chỉ tiêu, tỷ lệ không đạt ${liveStats.ratio} phần trăm`}
            >
              <span className="font-medium text-gray-700">
                Kết quả kiểm nghiệm:
              </span>
              <span className="inline-flex items-center gap-1 font-medium text-emerald-700">
                <ShieldCheck className="h-4 w-4" />
                Đạt {liveStats.passed}/{liveStats.total} tiêu chí
              </span>
              <span className="inline-flex items-center gap-1 font-medium text-red-700">
                <AlertTriangle className="h-4 w-4" />
                Không đạt {liveStats.failed}/{liveStats.total} (
                {liveStats.ratio}%)
              </span>
            </div>

            {/* Chỉ tiêu */}
            <div className="space-y-3">
              {detail.criteria.map((criterion) => {
                const input = criteriaInputs.find(
                  (item) => item.criterionId === criterion.criterionId
                );
                const resultError =
                  touched && input && input.passed === null;
                const dateError = input
                  ? dateErrorByCriterion[input.criterionId]
                  : undefined;
                return (
                  <div
                    key={criterion.criterionId}
                    className={`rounded-lg border p-3 ${
                      resultError || dateError
                        ? "border-red-200 bg-red-50/40"
                        : "border-gray-200"
                    }`}
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="min-w-0">
                        <p className="text-sm font-medium">
                          {criterion.name}
                          <span className="ml-2 font-mono text-xs text-muted-foreground">
                            {criterion.code}
                          </span>
                        </p>
                        {criterion.standardName && (
                          <p className="text-xs text-muted-foreground">
                            {criterion.standardName}
                          </p>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        {criterion.result && (
                          <Badge
                            variant="outline"
                            className="border-sky-200 bg-sky-50 text-sky-800"
                          >
                            Đã có kết quả
                          </Badge>
                        )}
                        <div className="flex gap-1.5">
                          <Button
                            type="button"
                            size="sm"
                            variant={
                              input?.passed === true ? "create" : "outline"
                            }
                            onClick={() =>
                              setCriterionPassed(criterion.criterionId, true)
                            }
                          >
                            Đạt
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant={
                              input?.passed === false ? "destructive" : "outline"
                            }
                            onClick={() =>
                              setCriterionPassed(criterion.criterionId, false)
                            }
                          >
                            Không đạt
                          </Button>
                        </div>
                      </div>
                    </div>

                    {/* Ngày cấp / hết hiệu lực */}
                    <div className="mt-2 grid grid-cols-1 gap-3 sm:grid-cols-2">
                      <div className="space-y-1">
                        <Label className="text-xs">Ngày cấp kết quả</Label>
                        <Input
                          type="date"
                          className="h-9 text-sm"
                          value={input?.resultDate ?? ""}
                          max={input?.expiryDate || undefined}
                          onChange={(event) =>
                            setCriterionField(
                              criterion.criterionId,
                              "resultDate",
                              event.target.value
                            )
                          }
                        />
                      </div>
                      <div className="space-y-1">
                        <Label className="text-xs">Ngày hết hiệu lực</Label>
                        <Input
                          type="date"
                          className="h-9 text-sm"
                          value={input?.expiryDate ?? ""}
                          min={input?.resultDate || undefined}
                          onChange={(event) =>
                            setCriterionField(
                              criterion.criterionId,
                              "expiryDate",
                              event.target.value
                            )
                          }
                        />
                      </div>
                    </div>

                    {/* Phiếu kết quả */}
                    <div className="mt-2 space-y-1">
                      <Label className="text-xs">
                        Phiếu kết quả kiểm nghiệm (ảnh / PDF, không bắt buộc)
                      </Label>
                      <Input
                        type="file"
                        className="h-9 text-sm"
                        accept="image/*,.pdf"
                        onChange={(event) =>
                          void handleFileChange(
                            criterion.criterionId,
                            event.target.files?.[0] ?? null
                          )
                        }
                      />
                      {input?.uploading && (
                        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <LoaderCircle className="h-3.5 w-3.5 animate-spin text-emerald-500" />
                          Đang tải lên phiếu kết quả...
                        </p>
                      )}
                      {!input?.uploading && input?.filePath && (
                        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <FileUp className="h-3.5 w-3.5 text-emerald-600" />
                          Đã đính kèm: {input.selectedFileName || input.filePath}
                        </p>
                      )}
                    </div>

                    {resultError && (
                      <p className="mt-1 text-xs text-red-600">
                        Vui lòng chọn kết quả cho chỉ tiêu này.
                      </p>
                    )}
                    {dateError && (
                      <p className="mt-1 flex items-center gap-1 text-xs text-red-600">
                        <AlertTriangle className="h-3.5 w-3.5" />
                        {dateError}
                      </p>
                    )}
                  </div>
                );
              })}
            </div>

            {touched && !allCriteriaAnswered && (
              <p className="flex items-center gap-1.5 text-sm text-red-600">
                <AlertTriangle className="h-4 w-4" />
                Vui lòng nhập kết quả cho tất cả các chỉ tiêu của yêu cầu.
              </p>
            )}
          </div>
        ) : null}

        <DialogFooter className="gap-2">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={submitting}
          >
            Hủy
          </Button>
          <Button
            variant="create"
            onClick={() => void handleSubmit()}
            disabled={!canSubmit}
          >
            {submitting && (
              <LoaderCircle className="mr-1 h-4 w-4 animate-spin" />
            )}
            Lưu kết quả
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}