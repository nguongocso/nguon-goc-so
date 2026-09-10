import { useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { ClipboardCheck, Loader2 } from 'lucide-react';
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
import { closeRecallCase } from '@/api/recallCaseApi';
import type {
  CloseRecallCasePayload,
  LotResolution,
  RecallCase,
  RecallLotResultPayload,
} from '@/types/recallCase';

/** Nhãn tiếng Việt của từng kết quả xử lý lô. */
export const RESOLUTION_LABEL: Record<LotResolution, string> = {
  DESTROYED: 'Đã tiêu hủy',
  RETURNED: 'Đã trả lại',
  REPROCESSED: 'Đã xử lý lại',
  UNRECOVERABLE: 'Không thu hồi được',
};

const RESOLUTION_OPTIONS = Object.entries(RESOLUTION_LABEL).map(
  ([value, label]) => ({ value, label }),
);

interface LotEntry {
  resolution?: LotResolution;
  recoveredQuantity: string;
  notes: string;
}

interface Props {
  open: boolean;
  recallCase: RecallCase | null;
  onClose: () => void;
  onClosed: () => void;
}

/**
 * Dialog kết thúc vụ việc thu hồi (NCL-08-CN-012).
 *
 * Nhập kết quả xử lý cho từng lô (QTN-27) và biện pháp khắc phục phòng ngừa
 * chung. Hệ thống chặn đóng khi còn lô thiếu kết quả hoặc thiếu biện pháp.
 */
export const CloseRecallCaseDialog = ({
  open,
  recallCase,
  onClose,
  onClosed,
}: Props) => {
  const [entries, setEntries] = useState<Record<string, LotEntry>>({});
  const [remediationMeasures, setRemediationMeasures] = useState('');
  const [evidenceRaw, setEvidenceRaw] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // Khởi tạo dữ liệu nhập từ kết quả đã có (nếu lô đã được nhập trước đó).
  useEffect(() => {
    if (recallCase && open) {
      const initial: Record<string, LotEntry> = {};
      recallCase.lotResults.forEach((lot) => {
        initial[lot.shipmentId] = {
          resolution: lot.resolution ?? undefined,
          recoveredQuantity:
            lot.recoveredQuantity != null ? String(lot.recoveredQuantity) : '',
          notes: lot.notes ?? '',
        };
      });
      setEntries(initial);
      setRemediationMeasures(recallCase.remediationMeasures ?? '');
      setEvidenceRaw((recallCase.evidenceFileIds ?? []).join(', '));
      setFieldErrors({});
    }
  }, [recallCase, open]);

  const pendingLotCount = useMemo(
    () =>
      recallCase?.lotResults.filter(
        (lot) => !entries[lot.shipmentId]?.resolution,
      ).length ?? 0,
    [recallCase, entries],
  );

  const updateEntry = (shipmentId: string, patch: Partial<LotEntry>) => {
    setEntries((prev) => ({
      ...prev,
      [shipmentId]: { ...prev[shipmentId], ...patch },
    }));
    // Xóa lỗi của lô khi người dùng bắt đầu sửa lại.
    setFieldErrors((prev) => {
      if (!prev[shipmentId]) return prev;
      const next = { ...prev };
      delete next[shipmentId];
      return next;
    });
  };

  const validate = (): boolean => {
    if (!recallCase) return false;
    const errors: Record<string, string> = {};

    recallCase.lotResults.forEach((lot) => {
      const entry = entries[lot.shipmentId] ?? {
        recoveredQuantity: '',
        notes: '',
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
      if (entry.resolution === 'UNRECOVERABLE' && !entry.notes.trim()) {
        errors[lot.shipmentId] =
          'Không thu hồi được: bắt buộc nhập lý do và biện pháp xử lý rủi ro.';
      }
    });

    if (!remediationMeasures.trim()) {
      errors.__remediation__ =
        'Biện pháp khắc phục phòng ngừa là bắt buộc trước khi đóng vụ việc.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!recallCase || submitting) return;
    if (!validate()) {
      toast.error(
        'Vui lòng nhập đủ kết quả xử lý cho tất cả các lô và biện pháp khắc phục.',
      );
      return;
    }

    const lotResults: RecallLotResultPayload[] = recallCase.lotResults.map(
      (lot) => {
        const entry = entries[lot.shipmentId];
        return {
          shipmentId: lot.shipmentId,
          resolution: entry.resolution as LotResolution,
          recoveredQuantity: Number(entry.recoveredQuantity),
          notes: entry.notes.trim() || undefined,
        };
      },
    );

    const payload: CloseRecallCasePayload = {
      remediationMeasures: remediationMeasures.trim(),
      lotResults,
      evidenceFileIds: evidenceRaw
        .split(/[,\n]/)
        .map((s) => s.trim())
        .filter(Boolean),
    };

    try {
      setSubmitting(true);
      await closeRecallCase(recallCase.id, payload);
      toast.success(
        'Đã kết thúc vụ việc thu hồi. Cảnh báo công khai đã chuyển sang "đã xử lý xong".',
      );
      onClosed();
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
    <Dialog
      open={open}
      onOpenChange={(o) => !o && !submitting && onClose()}
    >
      <DialogContent className="sm:max-w-3xl" showCloseButton={!submitting}>
        {recallCase && (
          <>
            <DialogHeader>
              <div className="flex items-start gap-3 pr-8">
                <div className="rounded-full bg-amber-100 p-2 text-amber-700">
                  <ClipboardCheck className="h-5 w-5" />
                </div>
                <div className="space-y-1">
                  <DialogTitle>Kết thúc vụ việc thu hồi</DialogTitle>
                  <DialogDescription>
                    Nhập kết quả xử lý cho tất cả các lô và biện pháp khắc phục
                    phòng ngừa. Vụ việc chỉ được đóng khi đủ cả hai điều kiện
                    (QTN-27).
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            <div className="max-h-[55vh] space-y-4 overflow-y-auto pr-1">
              <div className="rounded-lg border bg-muted/20 p-3 text-sm">
                <p>
                  <span className="text-muted-foreground">Mã vụ việc:</span>{' '}
                  <span className="font-mono font-medium">
                    {recallCase.caseCode}
                  </span>
                </p>
                <p className="mt-1">
                  <span className="text-muted-foreground">Lô sản xuất:</span>{' '}
                  <span className="font-medium">
                    {recallCase.productionLotName}
                  </span>
                </p>
                {pendingLotCount > 0 && (
                  <p className="mt-1 font-medium text-amber-700">
                    Còn {pendingLotCount} lô chưa có kết quả xử lý.
                  </p>
                )}
              </div>

              {/* Kết quả xử lý từng lô */}
              <div className="space-y-3">
                <Label>Kết quả xử lý từng lô</Label>
                {recallCase.lotResults.map((lot) => {
                  const entry = entries[lot.shipmentId] ?? {
                    recoveredQuantity: '',
                    notes: '',
                  };
                  const error = fieldErrors[lot.shipmentId];
                  return (
                    <div
                      key={lot.shipmentId}
                      className={`space-y-3 rounded-lg border p-4 ${
                        error ? 'border-red-300 bg-red-50/40' : 'border-slate-200'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium">
                          {lot.shipmentName}
                        </span>
                        {lot.unit && (
                          <span className="text-xs text-muted-foreground">
                            Đơn vị: {lot.unit}
                          </span>
                        )}
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
                              <SelectValue placeholder="Chọn kết quả xử lý" />
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
                            value={entry.recoveredQuantity}
                            onChange={(e) =>
                              updateEntry(lot.shipmentId, {
                                recoveredQuantity: e.target.value,
                              })
                            }
                            placeholder="VD: 0"
                            disabled={submitting}
                          />
                        </div>
                      </div>

                      <div className="space-y-1.5">
                        <Label className="text-xs">
                          Ghi chú / lý do
                          {entry.resolution === 'UNRECOVERABLE' && (
                            <span className="text-red-500"> (bắt buộc)</span>
                          )}
                        </Label>
                        <Textarea
                          rows={2}
                          value={entry.notes}
                          onChange={(e) =>
                            updateEntry(lot.shipmentId, { notes: e.target.value })
                          }
                          placeholder={
                            entry.resolution === 'UNRECOVERABLE'
                              ? 'Bắt buộc: lý do không thu hồi được và biện pháp xử lý rủi ro...'
                              : 'Ghi chú thêm về kết quả xử lý (tùy chọn)...'
                          }
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
            </div>

            {/* Biện pháp khắc phục + tệp biên bản */}
            <div className="space-y-1.5">
              <Label>
                Biện pháp khắc phục phòng ngừa{' '}
                <span className="text-red-500">*</span>
              </Label>
              <Textarea
                rows={3}
                value={remediationMeasures}
                onChange={(e) => {
                  setRemediationMeasures(e.target.value);
                  setFieldErrors((prev) => {
                    const next = { ...prev };
                    delete next.__remediation__;
                    return next;
                  });
                }}
                placeholder="Mô tả biện pháp khắc phục và phòng ngừa chung cho vụ việc..."
                disabled={submitting}
              />
              {fieldErrors.__remediation__ && (
                <p className="text-xs font-medium text-red-600">
                  {fieldErrors.__remediation__}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs">
                ID tệp biên bản (tùy chọn, phân tách bằng dấu phẩy)
              </Label>
              <Input
                value={evidenceRaw}
                onChange={(e) => setEvidenceRaw(e.target.value)}
                placeholder="UUID tệp biên bản đã tải lên (nếu có)"
                disabled={submitting}
              />
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={onClose} disabled={submitting}>
                Hủy
              </Button>
              <Button onClick={handleSubmit} disabled={submitting}>
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

export default CloseRecallCaseDialog;
