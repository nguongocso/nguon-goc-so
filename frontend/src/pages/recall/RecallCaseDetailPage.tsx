import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, Info, PackageCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { HelpButton } from '@/components/help/HelpButton';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { getRecallCase } from '@/api/recallCaseApi';
import {
  CloseRecallCaseDialog,
  RESOLUTION_LABEL,
} from '@/pages/recall/CloseRecallCaseDialog';
import type { LotResolution, RecallCase } from '@/types/recallCase';

const CASE_STATUS_MAP: Record<
  string,
  { label: string; tone: 'warning' | 'success' }
> = {
  OPEN: { label: 'Đang xử lý', tone: 'warning' },
  CLOSED: { label: 'Đã xử lý xong', tone: 'success' },
};

const RESOLUTION_TONE: Record<LotResolution, 'danger' | 'info' | 'warning'> = {
  DESTROYED: 'danger',
  RETURNED: 'info',
  REPROCESSED: 'warning',
  UNRECOVERABLE: 'danger',
};

/**
 * Trang chi tiết vụ việc thu hồi (NCL-08-CN-012).
 *
 * Hiển thị thông tin vụ việc, danh sách lô trong phạm vi kèm kết quả xử lý
 * và cho phép Quản lý hợp tác xã (VT-02) kết thúc vụ việc khi đủ điều kiện.
 */
export const RecallCaseDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [recallCase, setRecallCase] = useState<RecallCase | null>(null);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Vụ việc thu hồi', href: '/recall-cases' },
    { label: 'Chi tiết vụ việc' },
  ]);

  const load = useCallback(async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await getRecallCase(id);
      setRecallCase(data);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải vụ việc thu hồi');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  const statusInfo = recallCase
    ? CASE_STATUS_MAP[recallCase.status] ?? {
        label: recallCase.status,
        tone: 'neutral' as const,
      }
    : null;

  const pendingLotCount =
    recallCase?.lotResults.filter((lot) => !lot.resolution).length ?? 0;

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-center py-16 text-muted-foreground">
          Đang tải chi tiết vụ việc thu hồi...
        </div>
      </div>
    );
  }

  if (!recallCase) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-center py-16 text-muted-foreground">
          Không tìm thấy vụ việc thu hồi.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Nút quay lại + tiêu đề */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate('/recall-cases')}
            aria-label="Quay lại danh sách vụ việc thu hồi"
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-xl font-semibold flex items-center gap-3">
              Chi tiết vụ việc thu hồi
              {statusInfo && (
                <StatusBadge
                  label={statusInfo.label}
                  tone={statusInfo.tone}
                />
              )}
            </h1>
            <p className="text-sm text-muted-foreground font-mono">
              {recallCase.caseCode}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="recall-case-detail" />
          {recallCase.status === 'OPEN' && (
            <Button onClick={() => setDialogOpen(true)}>
              <PackageCheck className="mr-2 h-4 w-4" />
              Kết thúc vụ việc
            </Button>
          )}
        </div>
      </div>

      {/* Cảnh báo khi còn lô chưa có kết quả xử lý */}
      {recallCase.status === 'OPEN' && pendingLotCount > 0 && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          <span>
            Còn <strong>{pendingLotCount}</strong> lô chưa có kết quả xử lý.
            Vụ việc chỉ được đóng khi tất cả các lô đã có kết quả xử lý và đã
            nhập biện pháp khắc phục phòng ngừa (QTN-27).
          </span>
        </div>
      )}

      {/* Thông báo khi vụ việc đã đóng */}
      {recallCase.status === 'CLOSED' && (
        <div className="flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          <span>
            Vụ việc đã đóng ngày {formatDate(recallCase.closedAt)}. Cảnh báo
            công khai của các lô đã chuyển sang nội dung{' '}
            <strong>"đã xử lý xong"</strong> và không thể bị ẩn (QTN-09, QTN-27).
            Kết quả xử lý đã được khóa, không thể chỉnh sửa.
          </span>
        </div>
      )}

      {/* Thông tin vụ việc */}
      <Card>
        <CardContent className="grid grid-cols-1 gap-4 pt-6 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <p className="text-xs text-muted-foreground">Lô sản xuất</p>
            <p className="font-medium">{recallCase.productionLotName}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Số lô hàng trong phạm vi</p>
            <p className="font-medium">{recallCase.shipmentCount}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Ngày mở</p>
            <p className="font-medium">{formatDate(recallCase.createdAt)}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Ngày đóng</p>
            <p className="font-medium">{formatDate(recallCase.closedAt)}</p>
          </div>
          {recallCase.remediationMeasures && (
            <div className="sm:col-span-2 lg:col-span-4">
              <p className="text-xs text-muted-foreground">
                Biện pháp khắc phục phòng ngừa
              </p>
              <p className="whitespace-pre-line font-medium">
                {recallCase.remediationMeasures}
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Kết quả xử lý từng lô */}
      <Card>
        <CardContent className="pt-6">
          <h2 className="mb-4 font-semibold">Kết quả xử lý từng lô</h2>
          <div className="overflow-hidden rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Lô hàng</TableHead>
                  <TableHead>Đơn vị</TableHead>
                  <TableHead>Kết quả xử lý</TableHead>
                  <TableHead className="text-right">
                    Số lượng thu hồi được
                  </TableHead>
                  <TableHead>Ghi chú</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {recallCase.lotResults.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={5}
                      className="py-8 text-center text-muted-foreground"
                    >
                      Chưa có lô hàng nào trong phạm vi vụ việc.
                    </TableCell>
                  </TableRow>
                ) : (
                  recallCase.lotResults.map((lot) => (
                    <TableRow key={lot.shipmentId}>
                      <TableCell className="font-medium">
                        {lot.shipmentName}
                      </TableCell>
                      <TableCell>{lot.unit ?? '—'}</TableCell>
                      <TableCell>
                        {lot.resolution ? (
                          <StatusBadge
                            label={RESOLUTION_LABEL[lot.resolution]}
                            tone={RESOLUTION_TONE[lot.resolution]}
                          />
                        ) : (
                          <StatusBadge label="Chưa nhập" tone="danger" />
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        {lot.recoveredQuantity ?? '—'}
                      </TableCell>
                      <TableCell className="max-w-[280px] truncate" title={lot.notes ?? ''}>
                        {lot.notes || '—'}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Dialog kết thúc vụ việc */}
      <CloseRecallCaseDialog
        open={dialogOpen}
        recallCase={recallCase}
        onClose={() => setDialogOpen(false)}
        onClosed={() => {
          setDialogOpen(false);
          load();
        }}
      />
    </div>
  );
};

export default RecallCaseDetailPage;
