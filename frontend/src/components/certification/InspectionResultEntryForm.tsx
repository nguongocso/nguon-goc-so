import React, { useMemo, useState } from 'react';
import {
  AlertTriangle,
  Calendar,
  Check,
  CheckCircle2,
  FileCheck2,
  FileUp,
  Info,
  LoaderCircle,
  Search,
  Sparkles,
  X,
  XCircle,
} from 'lucide-react';
import { toast } from 'sonner';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type {
  PublicInspectionResultEntryCriterion,
  InspectionCriterionResultItemInput,
} from '@/types/inspectionResultPortal';

const toISODate = (date: Date): string => {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
};

const addYearsToISO = (dateStr: string, years: number): string => {
  try {
    const d = new Date(dateStr + 'T00:00:00');
    d.setFullYear(d.getFullYear() + years);
    return toISODate(d);
  } catch {
    return '';
  }
};

const addMonthsToISO = (dateStr: string, months: number): string => {
  try {
    const d = new Date(dateStr + 'T00:00:00');
    d.setMonth(d.getMonth() + months);
    return toISODate(d);
  } catch {
    return '';
  }
};

export interface CriterionRowState {
  criterionId: string;
  code: string;
  name: string;
  standardName?: string | null;
  passed: boolean | null;
  resultDate: string;
  expiryDate: string;
  filePath: string;
  selectedFileName: string;
  uploading: boolean;
}

type FilterTab = 'ALL' | 'UNSET' | 'PASSED' | 'FAILED';

export interface InspectionResultEntryFormProps {
  criteria: PublicInspectionResultEntryCriterion[];
  onSubmit: (results: InspectionCriterionResultItemInput[]) => Promise<void>;
  onUploadFile: (criterionId: string, file: File) => Promise<string>;
  isSubmitting?: boolean;
  submitButtonText?: string;
  readOnly?: boolean;
}

export const InspectionResultEntryForm: React.FC<InspectionResultEntryFormProps> = ({
  criteria,
  onSubmit,
  onUploadFile,
  isSubmitting = false,
  submitButtonText = 'Xác nhận và gửi kết quả kiểm nghiệm',
  readOnly = false,
}) => {
  const [rows, setRows] = useState<CriterionRowState[]>(() =>
    criteria.map((c) => ({
      criterionId: c.criterionId,
      code: c.code,
      name: c.name,
      standardName: c.standardName ?? null,
      passed: null,
      resultDate: toISODate(new Date()),
      expiryDate: addYearsToISO(toISODate(new Date()), 1),
      filePath: '',
      selectedFileName: '',
      uploading: false,
    }))
  );

  const [filterTab, setFilterTab] = useState<FilterTab>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  // Thống kê tiến độ
  const stats = useMemo(() => {
    const total = rows.length;
    const passedCount = rows.filter((r) => r.passed === true).length;
    const failedCount = rows.filter((r) => r.passed === false).length;
    const unsetCount = rows.filter((r) => r.passed === null).length;
    const isAllSet = total > 0 && unsetCount === 0;
    return { total, passedCount, failedCount, unsetCount, isAllSet };
  }, [rows]);

  // Bộ lọc danh sách hiển thị
  const filteredRows = useMemo(() => {
    return rows.filter((r) => {
      if (filterTab === 'UNSET' && r.passed !== null) return false;
      if (filterTab === 'PASSED' && r.passed !== true) return false;
      if (filterTab === 'FAILED' && r.passed !== false) return false;

      if (searchTerm.trim()) {
        const q = searchTerm.trim().toLowerCase();
        const matchName = r.name.toLowerCase().includes(q);
        const matchCode = r.code.toLowerCase().includes(q);
        const matchStd = r.standardName?.toLowerCase().includes(q) ?? false;
        if (!matchName && !matchCode && !matchStd) return false;
      }

      return true;
    });
  }, [rows, filterTab, searchTerm]);

  // Cập nhật trạng thái từng hàng
  const updateRow = (criterionId: string, patch: Partial<CriterionRowState>) => {
    setRows((prev) =>
      prev.map((r) => (r.criterionId === criterionId ? { ...r, ...patch } : r))
    );
  };

  // Hành động hàng loạt: Đánh dấu tất cả ĐẠT
  const handleMarkAllPassed = () => {
    setRows((prev) =>
      prev.map((r) => ({
        ...r,
        passed: true,
        resultDate: r.resultDate || toISODate(new Date()),
        expiryDate: r.expiryDate || addYearsToISO(toISODate(new Date()), 1),
      }))
    );
    toast.success('Đã đánh dấu toàn bộ chỉ tiêu là "Đạt".');
  };

  // Hành động hàng loạt: Áp dụng ngày hết hạn (6 tháng hoặc 1 năm)
  const handleApplyExpiryToAll = (months: number) => {
    const today = toISODate(new Date());
    setRows((prev) =>
      prev.map((r) => ({
        ...r,
        resultDate: today,
        expiryDate: addMonthsToISO(today, months),
      }))
    );
    toast.success(`Đã áp dụng thời hạn ${months} tháng cho tất cả chỉ tiêu.`);
  };

  // Xử lý upload file cho chỉ tiêu
  const handleFileChange = async (
    criterionId: string,
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Giới hạn 5MB và định dạng PDF/JPG/PNG theo đúng hợp đồng API
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Dung lượng tệp không được vượt quá 5MB.');
      return;
    }

    const validTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg'];
    if (!validTypes.includes(file.type)) {
      toast.error('Chỉ chấp nhận tệp định dạng PDF, JPG hoặc PNG.');
      return;
    }

    updateRow(criterionId, { uploading: true, selectedFileName: file.name });
    try {
      const filePath = await onUploadFile(criterionId, file);
      updateRow(criterionId, { filePath, uploading: false });
      toast.success(`Tải lên tệp cho "${file.name}" thành công.`);
    } catch (err: unknown) {
      updateRow(criterionId, { uploading: false, selectedFileName: '' });
      const msg = err instanceof Error ? err.message : 'Tải lên tệp thất bại.';
      toast.error(msg);
    }
  };

  // Kiểm tra tính hợp lệ trước khi mở modal xác nhận
  const handleValidateAndOpenConfirm = () => {
    const unselected = rows.filter((r) => r.passed === null);
    if (unselected.length > 0) {
      toast.error(
        `Còn ${unselected.length} chỉ tiêu chưa được đánh giá Đạt/Không đạt. Vui lòng hoàn tất toàn bộ.`
      );
      setFilterTab('UNSET');
      return;
    }

    // Kiểm tra tính hợp lệ ngày của các chỉ tiêu Đạt
    for (const r of rows) {
      if (r.passed === true) {
        if (!r.resultDate) {
          toast.error(`Chỉ tiêu "${r.name}" chưa có Ngày cấp kết quả.`);
          return;
        }
        if (!r.expiryDate) {
          toast.error(`Chỉ tiêu "${r.name}" chưa có Ngày hết hiệu lực.`);
          return;
        }
        if (r.resultDate > r.expiryDate) {
          toast.error(
            `Chỉ tiêu "${r.name}": Ngày cấp (${r.resultDate}) không thể sau Ngày hết hạn (${r.expiryDate}).`
          );
          return;
        }
      }
    }

    setShowConfirmModal(true);
  };

  // Nộp kết quả
  const handleConfirmSubmit = async () => {
    const payload: InspectionCriterionResultItemInput[] = rows.map((r) => ({
      criterionId: r.criterionId,
      passed: r.passed === true,
      resultDate: r.passed === true ? r.resultDate : undefined,
      expiryDate: r.passed === true ? r.expiryDate : undefined,
      filePath: r.filePath ? r.filePath : undefined,
    }));

    try {
      await onSubmit(payload);
      setShowConfirmModal(false);
    } catch {
      // lỗi đã được xử lý ở parent
    }
  };

  return (
    <div className="space-y-6">
      {/* Thanh tiến độ và công cụ hàng loạt */}
      <Card className="border-border/80 shadow-sm">
        <CardHeader className="pb-3">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <Sparkles className="h-5 w-5 text-emerald-600" />
                Danh sách chỉ tiêu kiểm nghiệm ({stats.total})
              </CardTitle>
              <CardDescription>
                Đánh giá kết quả cho từng chỉ tiêu và tải lên phiếu kiểm nghiệm tương ứng nếu có.
              </CardDescription>
            </div>

            {/* Thống kê nhanh */}
            <div className="flex items-center gap-2 flex-wrap">
              <Badge variant="outline" className="px-3 py-1 bg-muted/40 font-medium">
                {`Tổng số: ${stats.total}`}
              </Badge>
              <Badge
                variant="outline"
                className="px-3 py-1 bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 font-medium"
              >
                {`Đạt: ${stats.passedCount}`}
              </Badge>
              <Badge
                variant="outline"
                className="px-3 py-1 bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/40 dark:text-rose-400 font-medium"
              >
                {`Không đạt: ${stats.failedCount}`}
              </Badge>
              {stats.unsetCount > 0 && (
                <Badge
                  variant="outline"
                  className="px-3 py-1 bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-400 font-medium"
                >
                  {`Chưa nhập: ${stats.unsetCount}`}
                </Badge>
              )}
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-4">
          {/* Thanh công cụ tìm kiếm và lọc */}
          <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between pt-1">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Tìm kiếm chỉ tiêu theo tên, mã..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9"
              />
            </div>

            <div className="flex items-center gap-2 flex-wrap">
              <div className="inline-flex rounded-lg border p-1 bg-muted/30">
                <Button
                  type="button"
                  variant={filterTab === 'ALL' ? 'secondary' : 'ghost'}
                  size="sm"
                  onClick={() => setFilterTab('ALL')}
                  className="h-8 text-xs font-medium"
                >
                  Tất cả ({stats.total})
                </Button>
                <Button
                  type="button"
                  variant={filterTab === 'UNSET' ? 'secondary' : 'ghost'}
                  size="sm"
                  onClick={() => setFilterTab('UNSET')}
                  className="h-8 text-xs font-medium text-amber-700 dark:text-amber-400"
                >
                  Chưa nhập ({stats.unsetCount})
                </Button>
                <Button
                  type="button"
                  variant={filterTab === 'PASSED' ? 'secondary' : 'ghost'}
                  size="sm"
                  onClick={() => setFilterTab('PASSED')}
                  className="h-8 text-xs font-medium text-emerald-700 dark:text-emerald-400"
                >
                  Đạt ({stats.passedCount})
                </Button>
                <Button
                  type="button"
                  variant={filterTab === 'FAILED' ? 'secondary' : 'ghost'}
                  size="sm"
                  onClick={() => setFilterTab('FAILED')}
                  className="h-8 text-xs font-medium text-rose-700 dark:text-rose-400"
                >
                  Không đạt ({stats.failedCount})
                </Button>
              </div>

              {!readOnly && (
                <div className="flex items-center gap-1.5 ml-auto">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleMarkAllPassed}
                    className="h-8 text-xs text-emerald-700 hover:text-emerald-800 border-emerald-200"
                  >
                    <Check className="h-3.5 w-3.5 mr-1" />
                    Tất cả Đạt
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleApplyExpiryToAll(12)}
                    className="h-8 text-xs text-muted-foreground hover:text-foreground"
                    title="Gán ngày cấp hôm nay và hiệu lực 1 năm cho tất cả"
                  >
                    <Calendar className="h-3.5 w-3.5 mr-1" />
                    1 năm
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleApplyExpiryToAll(6)}
                    className="h-8 text-xs text-muted-foreground hover:text-foreground"
                    title="Gán ngày cấp hôm nay và hiệu lực 6 tháng cho tất cả"
                  >
                    <Calendar className="h-3.5 w-3.5 mr-1" />
                    6 tháng
                  </Button>
                </div>
              )}
            </div>
          </div>

          {/* Danh sách các chỉ tiêu */}
          <div className="space-y-3 pt-2">
            {filteredRows.length === 0 ? (
              <div className="py-10 text-center text-muted-foreground border border-dashed rounded-lg">
                Không tìm thấy chỉ tiêu nào phù hợp với bộ lọc hiện tại.
              </div>
            ) : (
              filteredRows.map((row, index) => {
                const isUnset = row.passed === null;
                const isPassed = row.passed === true;
                const isFailed = row.passed === false;

                return (
                  <div
                    key={row.criterionId}
                    className={`p-4 rounded-lg border transition-all ${
                      isUnset
                        ? 'border-amber-200/80 bg-amber-50/20 dark:bg-amber-950/10'
                        : isPassed
                        ? 'border-emerald-200/80 bg-emerald-50/20 dark:bg-emerald-950/10'
                        : 'border-rose-200/80 bg-rose-50/20 dark:bg-rose-950/10'
                    }`}
                  >
                    <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                      {/* Cột trái: Tên chỉ tiêu, tiêu chuẩn, mã */}
                      <div className="flex-1 space-y-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-xs font-semibold text-muted-foreground">
                            #{index + 1}
                          </span>
                          <span className="font-semibold text-foreground text-sm md:text-base">
                            {row.name}
                          </span>
                          <Badge variant="outline" className="text-xs font-mono">
                            {row.code}
                          </Badge>
                          {row.standardName && (
                            <Badge variant="secondary" className="text-xs">
                              {row.standardName}
                            </Badge>
                          )}
                        </div>

                        {/* Thông báo trạng thái dòng */}
                        <div className="text-xs text-muted-foreground flex items-center gap-1.5 pt-0.5">
                          {isUnset && (
                            <span className="text-amber-600 dark:text-amber-400 flex items-center gap-1 font-medium">
                              <AlertTriangle className="h-3.5 w-3.5" />
                              Vui lòng chọn kết quả đánh giá (Đạt / Không đạt)
                            </span>
                          )}
                          {isPassed && (
                            <span className="text-emerald-600 dark:text-emerald-400 flex items-center gap-1 font-medium">
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              Đạt yêu cầu kiểm nghiệm
                            </span>
                          )}
                          {isFailed && (
                            <span className="text-rose-600 dark:text-rose-400 flex items-center gap-1 font-medium">
                              <XCircle className="h-3.5 w-3.5" />
                              Không đạt (không yêu cầu thời hạn hiệu lực)
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Cột giữa: Nút chuyển Đạt / Không đạt */}
                      <div className="flex items-center gap-2">
                        <Button
                          type="button"
                          variant={isPassed ? 'default' : 'outline'}
                          size="sm"
                          disabled={readOnly}
                          onClick={() =>
                            updateRow(row.criterionId, {
                              passed: true,
                              resultDate: row.resultDate || toISODate(new Date()),
                              expiryDate: row.expiryDate || addYearsToISO(toISODate(new Date()), 1),
                            })
                          }
                          className={
                            isPassed
                              ? 'bg-emerald-600 hover:bg-emerald-700 text-white font-medium h-9'
                              : 'h-9 border-emerald-200 text-emerald-700 hover:bg-emerald-50'
                          }
                        >
                          <Check className="h-4 w-4 mr-1.5" />
                          Đạt
                        </Button>

                        <Button
                          type="button"
                          variant={isFailed ? 'destructive' : 'outline'}
                          size="sm"
                          disabled={readOnly}
                          onClick={() =>
                            updateRow(row.criterionId, {
                              passed: false,
                            })
                          }
                          className={
                            isFailed
                              ? 'bg-rose-600 hover:bg-rose-700 text-white font-medium h-9'
                              : 'h-9 border-rose-200 text-rose-700 hover:bg-rose-50'
                          }
                        >
                          <X className="h-4 w-4 mr-1.5" />
                          Không đạt
                        </Button>
                      </div>
                    </div>

                    {/* Hàng mở rộng khi Đạt: Chọn Ngày cấp, Ngày hết hạn & Upload phiếu */}
                    {isPassed && (
                      <div className="mt-4 pt-3 border-t border-border/60 grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                        <div>
                          <Label className="text-xs font-medium text-foreground mb-1 block">
                            Ngày cấp kết quả <span className="text-rose-500">*</span>
                          </Label>
                          <Input
                            type="date"
                            disabled={readOnly}
                            value={row.resultDate}
                            onChange={(e) =>
                              updateRow(row.criterionId, { resultDate: e.target.value })
                            }
                            className="h-8 text-xs"
                          />
                        </div>

                        <div>
                          <Label className="text-xs font-medium text-foreground mb-1 block">
                            Ngày hết hiệu lực <span className="text-rose-500">*</span>
                          </Label>
                          <Input
                            type="date"
                            disabled={readOnly}
                            value={row.expiryDate}
                            onChange={(e) =>
                              updateRow(row.criterionId, { expiryDate: e.target.value })
                            }
                            className="h-8 text-xs"
                          />
                        </div>

                        <div>
                          <Label className="text-xs font-medium text-foreground mb-1 block">
                            Phiếu kết quả (PDF/Ảnh)
                          </Label>
                          <div className="flex items-center gap-2">
                            <label className="cursor-pointer flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md border border-input bg-background hover:bg-accent text-xs font-medium transition-colors h-8 flex-1 truncate">
                              {row.uploading ? (
                                <>
                                  <LoaderCircle className="h-3.5 w-3.5 animate-spin text-emerald-600" />
                                  <span>Đang tải...</span>
                                </>
                              ) : row.filePath ? (
                                <>
                                  <FileCheck2 className="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                                  <span className="truncate">
                                    {row.selectedFileName || 'Đã có phiếu kết quả'}
                                  </span>
                                </>
                              ) : (
                                <>
                                  <FileUp className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                                  <span className="text-muted-foreground truncate">Chọn tệp...</span>
                                </>
                              )}
                              <input
                                type="file"
                                accept=".pdf,.jpg,.jpeg,.png"
                                disabled={readOnly || row.uploading}
                                onChange={(e) => handleFileChange(row.criterionId, e)}
                                className="hidden"
                              />
                            </label>

                            {row.filePath && (
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() =>
                                  updateRow(row.criterionId, { filePath: '', selectedFileName: '' })
                                }
                                className="h-8 w-8 p-0 text-muted-foreground hover:text-rose-600"
                                title="Gỡ tệp"
                              >
                                <X className="h-3.5 w-3.5" />
                              </Button>
                            )}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>

          {/* Cảnh báo toàn bộ chỉ tiêu */}
          {!stats.isAllSet && (
            <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/40 rounded-lg flex items-start gap-2 text-xs text-amber-800 dark:text-amber-300">
              <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
              <span>
                Bạn cần hoàn tất đánh giá cho <strong>toàn bộ {stats.total} chỉ tiêu</strong> trước khi có
                thể gửi kết quả kiểm nghiệm. Hiện còn <strong>{stats.unsetCount} chỉ tiêu</strong> chưa
                chọn.
              </span>
            </div>
          )}

          {/* Nút nộp kết quả chính */}
          {!readOnly && (
            <div className="pt-4 flex items-center justify-end">
              <Button
                type="button"
                size="lg"
                disabled={!stats.isAllSet || isSubmitting}
                onClick={handleValidateAndOpenConfirm}
                className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium px-6 shadow-sm"
              >
                {isSubmitting ? (
                  <>
                    <LoaderCircle className="h-4 w-4 animate-spin mr-2" />
                    Đang lưu kết quả...
                  </>
                ) : (
                  <>
                    <FileCheck2 className="h-4 w-4 mr-2" />
                    {submitButtonText}
                  </>
                )}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Modal xác nhận trước khi gửi */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-150">
          <Card className="w-full max-w-lg border shadow-xl bg-card">
            <CardHeader>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <Info className="h-5 w-5 text-emerald-600" />
                Xác nhận nộp kết quả kiểm nghiệm
              </CardTitle>
              <CardDescription>
                Hành động này sẽ ghi nhận chính thức kết quả kiểm nghiệm và liên kết này sẽ hết hiệu
                lực ngay sau khi nộp (chống gửi lặp lại).
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-lg border p-3 bg-muted/30 space-y-2 text-xs">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tổng số chỉ tiêu:</span>
                  <span className="font-semibold">{stats.total}</span>
                </div>
                <div className="flex justify-between text-emerald-700 dark:text-emerald-400">
                  <span>Chỉ tiêu ĐẠT:</span>
                  <span className="font-semibold">{stats.passedCount}</span>
                </div>
                <div className="flex justify-between text-rose-700 dark:text-rose-400">
                  <span>Chỉ tiêu KHÔNG ĐẠT:</span>
                  <span className="font-semibold">{stats.failedCount}</span>
                </div>
              </div>

              {stats.failedCount > 0 && (
                <div className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800/40 rounded-lg flex items-start gap-2 text-xs text-rose-800 dark:text-rose-300">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-rose-600 mt-0.5" />
                  <span>
                    Lưu ý: Yêu cầu kiểm nghiệm có <strong>{stats.failedCount} chỉ tiêu không đạt</strong>.
                    Toàn bộ yêu cầu kiểm nghiệm sẽ được kết luận là <strong>KHÔNG ĐẠT</strong> theo quy
                    trình QTN-21.
                  </span>
                </div>
              )}

              <div className="flex items-center justify-end gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={isSubmitting}
                  onClick={() => setShowConfirmModal(false)}
                >
                  Kiểm tra lại
                </Button>
                <Button
                  type="button"
                  disabled={isSubmitting}
                  onClick={handleConfirmSubmit}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  {isSubmitting ? (
                    <>
                      <LoaderCircle className="h-4 w-4 animate-spin mr-2" />
                      Đang gửi...
                    </>
                  ) : (
                    <>
                      <Check className="h-4 w-4 mr-2" />
                      Đồng ý nộp kết quả
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};
