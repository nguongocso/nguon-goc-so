import { useEffect, useMemo, useRef, useState } from 'react';
import { toast } from 'sonner';
import { ClipboardCheck, ExternalLink, FileText, Loader2, Trash2, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  closeBulkRecallRequest,
  openEvidenceInNewTab,
  uploadRecallEvidence,
} from '@/api/recallApi';
import type {
  BulkRecallRequest,
  CloseBulkRecallLotResultPayload,
  CloseBulkRecallRequestPayload,
  LotResolution,
  RecallEvidenceFile,
} from '@/types/bulkRecall';

/** Nhãn tiếng Việt của từng kết quả xử lý lô theo QTN-27. */
export const RESOLUTION_LABEL: Record<LotResolution, string> = {
  DESTROYED: 'Đã tiêu hủy',
  RETURNED: 'Đã trả lại',
  REPROCESSED: 'Đã xử lý lại',
  UNRECOVERABLE: 'Không thu hồi được',
};

const RESOLUTION_OPTIONS = Object.entries(RESOLUTION_LABEL).map(
  ([value, label]) => ({ value: value as LotResolution, label }),
);

/** Giới hạn tối đa số lượng tệp biên bản đính kèm. */
const MAX_EVIDENCE_FILES = 5;

interface LotEntry {
  resolution?: LotResolution;
  recoveredQuantity: string;
  remediationMeasures: string;
}

interface Props {
  open: boolean;
  bulkRequest: BulkRecallRequest | null;
  onClose: () => void;
  onSuccess: () => void;
}

/**
 * Dialog kết thúc vụ việc thu hồi gắn liền với yêu cầu thu hồi hàng loạt (NCL-08-CN-012).
 *
 * Người dùng nhập kết quả xử lý cho từng lô trong phạm vi thu hồi (QTN-27) và
 * biện pháp khắc phục phòng ngừa cho từng lô. Hỗ trợ tải tệp biên bản đính kèm (.pdf, .docx).
 */
export const CloseBulkRecallDialog = ({
  open,
  bulkRequest,
  onClose,
  onSuccess,
}: Props) => {
  const [entries, setEntries] = useState<Record<string, LotEntry>>({});
  const [uploadedFiles, setUploadedFiles] = useState<RecallEvidenceFile[]>([]);
  const [isUploadingFile, setIsUploadingFile] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Chỉ xét các lô nằm trong phạm vi thu hồi (included = true)
  const includedShipments = useMemo(
    () => bulkRequest?.shipments.filter((s) => s.included) ?? [],
    [bulkRequest],
  );

  // Khởi tạo dữ liệu khi mở dialog
  useEffect(() => {
    if (bulkRequest && open) {
      const initial: Record<string, LotEntry> = {};
      includedShipments.forEach((lot) => {
        initial[lot.shipmentId] = {
          resolution: lot.resolution ?? undefined,
          recoveredQuantity:
            lot.recoveredQuantity != null ? String(lot.recoveredQuantity) : '',
          remediationMeasures:
            lot.notes ?? bulkRequest.remediationMeasures ?? '',
        };
      });
      setEntries(initial);
      setUploadedFiles(bulkRequest.evidenceFiles ?? []);
      setFieldErrors({});
    }
  }, [bulkRequest, includedShipments, open]);

  const pendingLotCount = useMemo(
    () =>
      includedShipments.filter(
        (lot) => !entries[lot.shipmentId]?.resolution,
      ).length,
    [includedShipments, entries],
  );

  const updateEntry = (shipmentId: string, patch: Partial<LotEntry>) => {
    setEntries((prev) => ({
      ...prev,
      [shipmentId]: { ...prev[shipmentId], ...patch },
    }));
    // Xóa lỗi của lô khi người dùng sửa lại
    setFieldErrors((prev) => {
      if (!prev[shipmentId]) return prev;
      const next = { ...prev };
      delete next[shipmentId];
      return next;
    });
  };

  /** Áp dụng nhanh biện pháp khắc phục của 1 lô cho tất cả các lô còn lại. */
  const applyRemediationToAll = (sourceShipmentId: string) => {
    const sourceText = entries[sourceShipmentId]?.remediationMeasures ?? '';
    if (!sourceText.trim()) {
      toast.warning('Vui lòng nhập biện pháp khắc phục trước khi áp dụng cho tất cả các lô.');
      return;
    }
    setEntries((prev) => {
      const next = { ...prev };
      includedShipments.forEach((lot) => {
        next[lot.shipmentId] = {
          ...next[lot.shipmentId],
          remediationMeasures: sourceText,
        };
      });
      return next;
    });
    // Xóa lỗi nếu có
    setFieldErrors((prev) => {
      const next = { ...prev };
      includedShipments.forEach((lot) => delete next[lot.shipmentId]);
      return next;
    });
    toast.success('Đã áp dụng biện pháp khắc phục cho tất cả các lô trong phạm vi.');
  };

  /** Xử lý tải lên tệp biên bản (hỗ trợ .pdf, .docx, .doc, tối đa 5 tệp). */
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    if (uploadedFiles.length >= MAX_EVIDENCE_FILES) {
      toast.warning(`Chỉ được phép tải lên tối đa ${MAX_EVIDENCE_FILES} tệp biên bản.`);
      e.target.value = '';
      return;
    }

    const file = files[0];
    const lowerName = file.name.toLowerCase();
    if (
      !lowerName.endsWith('.pdf') &&
      !lowerName.endsWith('.docx') &&
      !lowerName.endsWith('.doc')
    ) {
      toast.error('Chỉ chấp nhận tệp biên bản định dạng PDF (.pdf) hoặc Word (.docx, .doc).');
      e.target.value = '';
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      toast.error('Dung lượng tệp vượt quá giới hạn 10MB.');
      e.target.value = '';
      return;
    }

    try {
      setIsUploadingFile(true);
      const uploaded = await uploadRecallEvidence(file);
      setUploadedFiles((prev) => [...prev, uploaded]);
      toast.success(`Đã tải lên tệp biên bản: ${file.name}`);
    } catch (err: any) {
      toast.error(
        err.response?.data?.message || 'Không thể tải lên tệp biên bản lúc này.',
      );
    } finally {
      setIsUploadingFile(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  /** Xóa tệp biên bản khỏi danh sách đính kèm. */
  const handleRemoveFile = (fileId: string) => {
    setUploadedFiles((prev) => prev.filter((f) => f.id !== fileId));
  };

  const validate = (): boolean => {
    if (!bulkRequest) return false;
    const errors: Record<string, string> = {};

    includedShipments.forEach((lot) => {
      const entry = entries[lot.shipmentId] ?? {
        recoveredQuantity: '',
        remediationMeasures: '',
      };
      if (!entry.resolution) {
        errors[lot.shipmentId] = 'Vui lòng chọn kết quả xử lý cho lô này.';
        return;
      }
      const qty = Number(entry.recoveredQuantity);
      if (!entry.recoveredQuantity.trim() || Number.isNaN(qty) || qty < 0) {
        errors[lot.shipmentId] =
          'Số lượng thu hồi được là bắt buộc và không được âm.';
        return;
      }
      if (lot.totalQuantity != null && qty > lot.totalQuantity) {
        errors[lot.shipmentId] = `Số lượng thu hồi không được vượt quá tổng số lượng lô (${lot.totalQuantity}).`;
        return;
      }
      if (!entry.remediationMeasures.trim()) {
        errors[lot.shipmentId] =
          'Biện pháp khắc phục phòng ngừa cho lô này là bắt buộc.';
      }
    });

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!bulkRequest || submitting) return;
    if (!validate()) {
      toast.error(
        'Vui lòng nhập đủ kết quả xử lý và biện pháp khắc phục cho tất cả các lô.',
      );
      return;
    }

    const lotResults: CloseBulkRecallLotResultPayload[] = includedShipments.map(
      (lot) => {
        const entry = entries[lot.shipmentId];
        return {
          shipmentId: lot.shipmentId,
          resolution: entry.resolution as LotResolution,
          recoveredQuantity: Number(entry.recoveredQuantity),
          notes: entry.remediationMeasures.trim(),
        };
      },
    );

    // Ghép biện pháp khắc phục chung từ các lô
    const remediationMeasures =
      includedShipments.length === 1
        ? entries[includedShipments[0].shipmentId]?.remediationMeasures.trim() || ''
        : includedShipments
          .map(
            (lot) =>
              `${lot.shipmentName}: ${entries[lot.shipmentId]?.remediationMeasures.trim()}`,
          )
          .join('\n');

    const payload: CloseBulkRecallRequestPayload = {
      remediationMeasures,
      lotResults,
      evidenceFileIds: uploadedFiles.map((f) => f.id),
    };

    try {
      setSubmitting(true);
      await closeBulkRecallRequest(bulkRequest.id, payload);
      toast.success(
        'Đã kết thúc vụ việc thu hồi thành công. Trạng thái yêu cầu chuyển sang "Đã xử lý".',
      );
      onSuccess();
      onClose();
    } catch (err: any) {
      toast.error(
        err.response?.data?.message ||
        'Không thể kết thúc vụ việc thu hồi lúc này.',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && !submitting && onClose()}>
      <DialogContent className="sm:max-w-3xl" showCloseButton={!submitting}>
        {bulkRequest && (
          <>
            <DialogHeader>
              <div className="flex items-start gap-3 pr-8">
                <div className="rounded-full bg-emerald-100 p-2 text-emerald-700">
                  <ClipboardCheck className="h-5 w-5" />
                </div>
                <div className="space-y-1">
                  <DialogTitle>Kết thúc vụ việc thu hồi</DialogTitle>
                  <DialogDescription>
                    Nhập kết quả xử lý và biện pháp khắc phục phòng ngừa cho từng
                    lô trong phạm vi thu hồi. Vụ việc chỉ được kết thúc khi đủ cả
                    hai điều kiện.
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            <div className="max-h-[60vh] space-y-4 overflow-y-auto pr-1">
              <div className="rounded-lg border bg-muted/20 p-3 text-sm">
                <p>
                  <span className="text-muted-foreground">Lô sản xuất nguồn:</span>{' '}
                  <span className="font-semibold text-slate-800">
                    {bulkRequest.productionLotName}
                  </span>
                </p>
                <p className="mt-1">
                  <span className="text-muted-foreground">Số lô trong phạm vi:</span>{' '}
                  <span className="font-medium">
                    {includedShipments.length} lô
                  </span>
                </p>
                {pendingLotCount > 0 && (
                  <p className="mt-1 font-medium text-amber-700">
                    Còn {pendingLotCount} lô chưa chọn kết quả xử lý.
                  </p>
                )}
              </div>

              {/* Kết quả xử lý từng lô & Biện pháp khắc phục */}
              <div className="space-y-3">
                <Label className="text-sm font-semibold">
                  Kết quả xử lý & Biện pháp khắc phục từng lô hàng trong phạm vi
                </Label>
                {includedShipments.map((lot) => {
                  const entry = entries[lot.shipmentId] ?? {
                    recoveredQuantity: '',
                    remediationMeasures: '',
                  };
                  const error = fieldErrors[lot.shipmentId];
                  return (
                    <div
                      key={lot.shipmentId}
                      className={`space-y-3 rounded-lg border p-4 ${error ? 'border-red-300 bg-red-50/40' : 'border-slate-200 bg-white'
                        }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-slate-900">
                          {lot.shipmentName}
                        </span>
                        <div className="text-xs text-muted-foreground space-x-2">
                          {lot.totalQuantity != null && (
                            <span>Tổng lượng: <strong>{lot.totalQuantity}</strong></span>
                          )}
                          {lot.unit && <span>({lot.unit})</span>}
                        </div>
                      </div>

                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div className="space-y-1.5">
                          <Label className="text-xs">
                            Kết quả xử lý{' '}
                            <span className="text-red-500">*</span>
                          </Label>
                          <Select
                            value={entry.resolution ?? undefined}
                            onValueChange={(val) =>
                              updateEntry(lot.shipmentId, {
                                resolution: val as LotResolution,
                              })
                            }
                            disabled={submitting}
                          >
                            <SelectTrigger className="w-full">
                              <SelectValue placeholder="Chọn kết quả xử lý">
                                {entry.resolution ? RESOLUTION_LABEL[entry.resolution] : undefined}
                              </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                              {RESOLUTION_OPTIONS.map((opt) => (
                                <SelectItem key={opt.value} value={opt.value}>
                                  {opt.label}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>

                        <div className="space-y-1.5">
                          <Label className="text-xs">
                            Số lượng thu hồi được{' '}
                            <span className="text-red-500">*</span>
                          </Label>
                          <Input
                            type="number"
                            min={0}
                            max={lot.totalQuantity ?? undefined}
                            value={entry.recoveredQuantity}
                            onChange={(e) =>
                              updateEntry(lot.shipmentId, {
                                recoveredQuantity: e.target.value,
                              })
                            }
                            placeholder={`VD: 0 (tối đa ${lot.totalQuantity ?? '—'})`}
                            disabled={submitting}
                          />
                        </div>
                      </div>

                      {/* Mục Biện pháp khắc phục phòng ngừa thay thế cho Ghi chú/lý do */}
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                          <Label className="text-xs">
                            Biện pháp khắc phục phòng ngừa{' '}
                            <span className="text-red-500">*</span>
                          </Label>
                          {includedShipments.length > 1 && (
                            <button
                              type="button"
                              onClick={() => applyRemediationToAll(lot.shipmentId)}
                              className="text-[11px] text-emerald-600 hover:text-emerald-700 hover:underline cursor-pointer"
                              title="Sao chép biện pháp khắc phục này cho tất cả các lô khác"
                            >
                              Áp dụng cho tất cả các lô
                            </button>
                          )}
                        </div>
                        <Textarea
                          rows={2}
                          value={entry.remediationMeasures}
                          onChange={(e) =>
                            updateEntry(lot.shipmentId, {
                              remediationMeasures: e.target.value,
                            })
                          }
                          placeholder="Mô tả chi tiết biện pháp khắc phục và phòng ngừa đối với lô hàng này..."
                          disabled={submitting}
                        />
                      </div>

                      {error && (
                        <p className="text-xs font-medium text-red-600">
                          {error}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Khu vực đẩy tệp biên bản (.pdf, .docx) */}
              <div className="space-y-2 pt-3 border-t border-slate-200">
                <div className="flex items-center justify-between">
                  <Label className="text-xs font-semibold text-slate-800">
                    Tệp biên bản / bằng chứng thu hồi
                  </Label>
                  <span className="text-[11px] text-muted-foreground">
                    Hỗ trợ định dạng PDF (.pdf) hoặc Word (.docx, .doc), tối đa 10MB/tệp (tối đa {MAX_EVIDENCE_FILES} tệp) — {uploadedFiles.length}/{MAX_EVIDENCE_FILES} tệp
                  </span>
                </div>

                <input
                  type="file"
                  ref={fileInputRef}
                  accept=".pdf,.docx,.doc,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/msword"
                  className="hidden"
                  onChange={handleFileChange}
                  disabled={submitting || isUploadingFile || uploadedFiles.length >= MAX_EVIDENCE_FILES}
                />

                <div className="flex flex-wrap items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={submitting || isUploadingFile || uploadedFiles.length >= MAX_EVIDENCE_FILES}
                    className={`border-dashed ${
                      uploadedFiles.length >= MAX_EVIDENCE_FILES
                        ? 'border-slate-300 bg-slate-100 text-slate-400 cursor-not-allowed'
                        : 'border-emerald-300 bg-emerald-50/50 hover:bg-emerald-100/60 text-emerald-700'
                    }`}
                  >
                    {isUploadingFile ? (
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    ) : (
                      <Upload className="mr-2 h-4 w-4" />
                    )}
                    {isUploadingFile
                      ? 'Đang tải lên...'
                      : uploadedFiles.length >= MAX_EVIDENCE_FILES
                      ? `Đã đạt tối đa ${MAX_EVIDENCE_FILES} tệp`
                      : 'Tải lên tệp biên bản (.pdf, .docx)'}
                  </Button>
                </div>

                {/* Danh sách tệp biên bản đã tải lên */}
                {uploadedFiles.length > 0 && (
                  <div className="space-y-1.5 pt-1">
                    {uploadedFiles.map((file) => (
                      <div
                        key={file.id}
                        className="flex items-center justify-between p-2 rounded-md border border-slate-200 bg-slate-50 text-xs hover:bg-slate-100/70 transition-colors"
                      >
                        <div
                          onClick={() => openEvidenceInNewTab(file.id)}
                          className="flex items-center gap-2 min-w-0 flex-1 cursor-pointer group"
                          title="Bấm để mở xem tệp trong tab mới của trình duyệt"
                        >
                          <FileText className="h-4 w-4 text-emerald-600 flex-shrink-0 group-hover:text-emerald-700" />
                          <span
                            className="font-medium text-slate-800 truncate group-hover:text-emerald-700 group-hover:underline"
                            title={file.fileName}
                          >
                            {file.fileName}
                          </span>
                          <span className="text-slate-400 text-[11px] flex-shrink-0">
                            ({Math.round(file.fileSize / 1024)} KB)
                          </span>
                          <ExternalLink className="h-3 w-3 text-slate-400 group-hover:text-emerald-600 opacity-60 group-hover:opacity-100 flex-shrink-0" />
                        </div>
                        <div className="flex items-center gap-1 flex-shrink-0 ml-2">
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 text-slate-500 hover:text-emerald-700"
                            onClick={() => openEvidenceInNewTab(file.id)}
                            title="Mở xem tệp trong tab mới"
                          >
                            <ExternalLink className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 text-slate-400 hover:text-red-600"
                            onClick={() => handleRemoveFile(file.id)}
                            disabled={submitting}
                            title="Xóa tệp biên bản"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={onClose} disabled={submitting}>
                Hủy
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={submitting || isUploadingFile}
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
              >
                {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {submitting ? 'Đang xử lý...' : 'Xác nhận kết thúc vụ việc'}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default CloseBulkRecallDialog;
