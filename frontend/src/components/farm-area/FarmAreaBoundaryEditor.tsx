import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  History,
  Info,
  RefreshCw,
  RotateCcw,
  Save,
  Trash2,
} from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { getFarmAreaBoundary, updateFarmAreaBoundary } from '@/api/farmAreaApi';
import type {
  AreaDeviationErrorData,
  FarmArea,
  FarmAreaBoundaryResponse,
  LatLng,
} from '@/types/farmArea';
import {
  calculateAreaDeviation,
  calculateGeodesicAreaHa,
} from '@/utils/geoAreaCalculator';
import { BoundaryMapEditor } from './BoundaryMapEditor';
import { BoundaryPastePanel } from './BoundaryPastePanel';
import { AreaDeviationConfirmDialog } from './AreaDeviationConfirmDialog';

interface Props {
  farmArea: FarmArea;
  onSaveSuccess?: (updatedBoundary: FarmAreaBoundaryResponse) => void;
  onDirtyChange?: (isDirty: boolean) => void;
}

export const FarmAreaBoundaryEditor: React.FC<Props> = ({
  farmArea,
  onSaveSuccess,
  onDirtyChange,
}) => {
  const [savedBoundary, setSavedBoundary] = useState<FarmAreaBoundaryResponse | null>(null);
  const [draftPoints, setDraftPoints] = useState<LatLng[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [selectedVertexIndex, setSelectedVertexIndex] = useState<number | null>(null);

  // Dialog xác nhận chênh lệch 409
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [deviationError, setDeviationError] = useState<AreaDeviationErrorData | null>(null);

  // Tải ranh giới hiện tại từ backend
  useEffect(() => {
    let isMounted = true;

    const fetchBoundary = async () => {
      try {
        setIsLoading(true);
        setLoadError(null);
        const data = await getFarmAreaBoundary(farmArea.id);
        if (!isMounted) return;

        setSavedBoundary(data);
        const initialPoints = data?.points ? [...data.points] : [];
        setDraftPoints(initialPoints);
      } catch (error: any) {
        if (!isMounted) return;
        const message = error.response?.data?.message || 'Không thể tải ranh giới vùng trồng';
        setLoadError(message);
        toast.error(message);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    void fetchBoundary();

    return () => {
      isMounted = false;
    };
  }, [farmArea.id, reloadKey]);

  // So sánh xem draft có thay đổi so với dữ liệu đã lưu không
  const isDirty = (() => {
    const saved = savedBoundary?.points ?? [];
    if (saved.length !== draftPoints.length) return true;
    for (let i = 0; i < saved.length; i++) {
      if (
        Math.abs(saved[i].latitude - draftPoints[i].latitude) > 1e-6 ||
        Math.abs(saved[i].longitude - draftPoints[i].longitude) > 1e-6
      ) {
        return true;
      }
    }
    return false;
  })();

  useEffect(() => {
    onDirtyChange?.(isDirty);
    return () => onDirtyChange?.(false);
  }, [isDirty, onDirtyChange]);

  // Tính diện tích xem trước (tạm tính)
  const declaredAreaHa = farmArea.area || 0;
  const calculatedAreaHa = calculateGeodesicAreaHa(draftPoints);
  const deviationPercent = calculateAreaDeviation(declaredAreaHa, calculatedAreaHa);
  const thresholdPercentage = savedBoundary?.thresholdPercentage;
  const isDeviationHigh =
    thresholdPercentage != null && deviationPercent > thresholdPercentage;

  // Thêm một đỉnh mới
  const handleAddPoint = (point: LatLng) => {
    // Tránh trùng điểm liền kề
    if (draftPoints.length > 0) {
      const last = draftPoints[draftPoints.length - 1];
      if (
        Math.abs(last.latitude - point.latitude) < 1e-6 &&
        Math.abs(last.longitude - point.longitude) < 1e-6
      ) {
        return;
      }
    }
    setDraftPoints((prev) => [...prev, point]);
    setSelectedVertexIndex(null);
  };

  // Cập nhật vị trí đỉnh khi kéo marker
  const handleUpdatePoint = (index: number, point: LatLng) => {
    setDraftPoints((prev) => {
      const next = [...prev];
      next[index] = point;
      return next;
    });
  };

  // Xóa 1 đỉnh cụ thể
  const handleDeletePoint = (index: number) => {
    setDraftPoints((prev) => prev.filter((_, i) => i !== index));
    if (selectedVertexIndex === index) {
      setSelectedVertexIndex(null);
    }
  };

  // Hoàn tác đỉnh cuối cùng
  const handleUndoLastPoint = () => {
    setDraftPoints((prev) => prev.slice(0, -1));
    setSelectedVertexIndex(null);
  };

  // Xóa toàn bộ đỉnh
  const handleClearAll = () => {
    setDraftPoints([]);
    setSelectedVertexIndex(null);
  };

  // Khôi phục ranh giới ban đầu
  const handleResetToSaved = () => {
    const original = savedBoundary?.points ? [...savedBoundary.points] : [];
    setDraftPoints(original);
    setSelectedVertexIndex(null);
  };

  // Áp dụng danh sách đỉnh từ clipboard
  const handleApplyPaste = (newPoints: LatLng[]) => {
    setDraftPoints(newPoints);
    setSelectedVertexIndex(null);
    toast.success(`Đã áp dụng ${newPoints.length} đỉnh ranh giới từ danh sách`);
  };

  // Thực hiện lưu ranh giới
  const performSave = async (confirmed: boolean) => {
    if (draftPoints.length < 3) {
      toast.error('Ranh giới phải có tối thiểu 3 đỉnh phân biệt (TC-02).');
      return;
    }

    try {
      setIsSaving(true);
      const res = await updateFarmAreaBoundary(farmArea.id, {
        points: draftPoints,
        confirmed,
      });

      setSavedBoundary(res);
      setDraftPoints(res.points ? [...res.points] : []);
      setConfirmDialogOpen(false);
      setDeviationError(null);
      toast.success('Cập nhật ranh giới vùng trồng thành công!');
      onSaveSuccess?.(res);
    } catch (error: any) {
      const status = error.response?.status;
      const errorData = error.response?.data?.errors as AreaDeviationErrorData | undefined;

      // Dialog chỉ dùng số liệu chính thức từ lỗi 409 của backend.
      if (status === 409 && errorData?.code === 'AREA_DEVIATION_CONFIRMATION_REQUIRED') {
        setDeviationError(errorData);
        setConfirmDialogOpen(true);
        return;
      }

      toast.error(error.response?.data?.message || 'Không thể lưu ranh giới vùng trồng');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-72 items-center justify-center text-sm text-muted-foreground">
        <RefreshCw className="mr-2 size-5 animate-spin text-emerald-600" />
        Đang tải dữ liệu ranh giới vùng trồng...
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center gap-4 rounded-xl border border-red-200 bg-red-50 p-6 text-center dark:border-red-900 dark:bg-red-950/30">
        <AlertTriangle className="size-8 text-red-600" />
        <div>
          <h2 className="font-semibold text-red-800 dark:text-red-300">
            Không thể tải ranh giới vùng trồng
          </h2>
          <p className="mt-1 text-sm text-red-700 dark:text-red-400">{loadError}</p>
        </div>
        <Button type="button" variant="outline" onClick={() => setReloadKey((value) => value + 1)}>
          <RefreshCw className="mr-2 size-4" />
          Thử lại
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Thanh tiêu đề con & huy hiệu trạng thái */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-border pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold tracking-tight text-slate-900 dark:text-foreground">
              Khoanh ranh giới đa giác trên bản đồ
            </h2>
            {isDirty ? (
              <Badge variant="outline" className="border-amber-400 bg-amber-50 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300 text-xs">
                ● Chưa lưu
              </Badge>
            ) : savedBoundary?.points && savedBoundary.points.length >= 3 ? (
              <Badge variant="outline" className="border-emerald-300 bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300 text-xs">
                <CheckCircle2 className="mr-1 size-3 text-emerald-600" />
                Đã lưu ranh giới
              </Badge>
            ) : (
              <Badge variant="outline" className="text-muted-foreground text-xs">
                Chưa thiết lập ranh giới
              </Badge>
            )}
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Chấm các đỉnh trên bản đồ, kéo đỉnh để căn chỉnh hoặc dán danh sách tọa độ từ GPS/Google Earth.
          </p>
        </div>

        {savedBoundary?.updatedAt && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Clock className="size-3.5" />
            <span>
              Cập nhật lần cuối:{' '}
              {new Date(savedBoundary.updatedAt).toLocaleString('vi-VN')}
            </span>
          </div>
        )}
      </div>

      {/* Bố cục 2 cột: 2/3 Bản đồ Leaflet, 1/3 Bảng điều khiển */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Cột trái: Bản đồ (8/12) */}
        <div className="lg:col-span-8 space-y-3">
          <BoundaryMapEditor
            points={draftPoints}
            initialCenter={{
              latitude: farmArea.latitude,
              longitude: farmArea.longitude,
            }}
            onAddPoint={handleAddPoint}
            onUpdatePoint={handleUpdatePoint}
            onSelectPoint={setSelectedVertexIndex}
            selectedIndex={selectedVertexIndex}
            disabled={isSaving}
          />
        </div>

        {/* Cột phải: Bảng điều khiển & Thống kê (4/12) */}
        <div className="lg:col-span-4 space-y-4">
          {/* Card So sánh diện tích */}
          <div className="rounded-xl border border-border bg-card p-4 shadow-sm space-y-3">
            <h3 className="text-sm font-semibold text-foreground flex items-center justify-between">
              <span>Đối chiếu diện tích</span>
              <span className="text-[11px] font-normal text-muted-foreground">Chuẩn WGS84</span>
            </h3>

            <div className="space-y-2.5 text-xs">
              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Diện tích khai báo:</span>
                <span className="font-semibold text-slate-900 dark:text-foreground">
                  {Number(declaredAreaHa).toFixed(4)} ha
                </span>
              </div>

              <div className="flex items-center justify-between py-1 border-b border-border/60">
                <span className="text-muted-foreground">Diện tích ranh giới:</span>
                <div className="text-right">
                  <span className="font-bold text-emerald-600 dark:text-emerald-400">
                    {Number(calculatedAreaHa).toFixed(4)} ha
                  </span>
                  <span className="block text-[10px] text-muted-foreground">(Tạm tính)</span>
                </div>
              </div>

              <div className="flex items-center justify-between py-1">
                <span className="text-muted-foreground">Chênh lệch:</span>
                <span
                  className={`font-bold ${
                    isDeviationHigh
                      ? 'text-amber-600 dark:text-amber-400'
                      : 'text-slate-900 dark:text-foreground'
                  }`}
                >
                  {deviationPercent.toFixed(2)}%
                </span>
              </div>
            </div>

            {isDeviationHigh && (
              <div className="rounded-lg bg-amber-50 p-2.5 text-xs text-amber-800 dark:bg-amber-950/40 dark:text-amber-300 flex items-start gap-2">
                <AlertTriangle className="size-4 shrink-0 mt-0.5 text-amber-600" />
                <p className="leading-tight">
                  Chênh lệch &gt; {thresholdPercentage}% so với diện tích khai báo. Khi lưu sẽ yêu cầu xác nhận.
                </p>
              </div>
            )}
          </div>

          {/* Panel Dán tọa độ */}
          <BoundaryPastePanel onApplyPoints={handleApplyPaste} disabled={isSaving} />

          {/* Card Danh sách đỉnh */}
          <div className="rounded-xl border border-border bg-card p-4 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-semibold text-foreground">
                Danh sách đỉnh ({draftPoints.length})
              </h4>
              <div className="flex gap-1.5">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleUndoLastPoint}
                  disabled={draftPoints.length === 0 || isSaving}
                  className="h-7 px-2 text-xs gap-1"
                  title="Xóa đỉnh cuối vừa tạo"
                >
                  <RotateCcw className="size-3" />
                  Hoàn tác
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleClearAll}
                  disabled={draftPoints.length === 0 || isSaving}
                  className="h-7 px-2 text-xs text-rose-600 hover:text-rose-700 dark:text-rose-400 gap-1"
                  title="Xóa tất cả các đỉnh"
                >
                  <Trash2 className="size-3" />
                  Xóa hết
                </Button>
              </div>
            </div>

            {draftPoints.length === 0 ? (
              <div className="rounded-lg border border-dashed border-border p-4 text-center text-xs text-muted-foreground">
                Chưa có điểm nào. Hãy nhấp trên bản đồ để thêm đỉnh ranh giới.
              </div>
            ) : (
              <div className="max-h-48 overflow-y-auto space-y-1.5 pr-1 text-xs">
                {draftPoints.map((p, idx) => (
                  <div
                    key={idx}
                    onClick={() => setSelectedVertexIndex(idx)}
                    className={`flex items-center justify-between rounded-lg border px-2.5 py-1.5 transition-colors cursor-pointer ${
                      selectedVertexIndex === idx
                        ? 'border-amber-400 bg-amber-50/70 dark:bg-amber-950/30'
                        : 'border-border/60 hover:bg-slate-50 dark:hover:bg-muted/40'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span className="flex size-5 items-center justify-center rounded-full bg-emerald-600 text-[10px] font-bold text-white">
                        {idx + 1}
                      </span>
                      <span className="font-mono text-[11px] text-slate-700 dark:text-slate-300">
                        {p.latitude.toFixed(6)}, {p.longitude.toFixed(6)}
                      </span>
                    </div>

                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeletePoint(idx);
                      }}
                      disabled={isSaving}
                      className="size-6 p-0 text-muted-foreground hover:text-rose-600"
                      title={`Xóa đỉnh ${idx + 1}`}
                      aria-label={`Xóa đỉnh ${idx + 1}`}
                    >
                      <Trash2 className="size-3" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Thanh nút thao tác chính phía dưới */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 rounded-xl border border-border bg-slate-50/80 p-4 dark:bg-card">
        <div className="text-xs text-muted-foreground flex items-center gap-1.5">
          <Info className="size-4 text-emerald-600 shrink-0" />
          <span>
            {draftPoints.length < 3
              ? 'Cần tối thiểu 3 đỉnh để có thể lưu ranh giới.'
              : isDirty
              ? 'Bạn có thay đổi ranh giới chưa được lưu vào hệ thống.'
              : 'Ranh giới đã được lưu đồng bộ với máy chủ.'}
          </span>
        </div>

        <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={handleResetToSaved}
            disabled={!isDirty || isSaving}
            className="flex items-center gap-1.5"
          >
            <History className="size-4" />
            Khôi phục
          </Button>

          <Button
            type="button"
            variant="default"
            onClick={() => performSave(false)}
            disabled={!isDirty || draftPoints.length < 3 || isSaving}
            className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white min-w-32"
          >
            {isSaving ? (
              <>
                <RefreshCw className="size-4 animate-spin" />
                Đang lưu...
              </>
            ) : (
              <>
                <Save className="size-4" />
                Lưu ranh giới
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Dialog xác nhận chênh lệch diện tích khi lỗi 409 */}
      <AreaDeviationConfirmDialog
        open={confirmDialogOpen}
        data={deviationError}
        onConfirm={() => performSave(true)}
        onCancel={() => setConfirmDialogOpen(false)}
        isSubmitting={isSaving}
      />
    </div>
  );
};
