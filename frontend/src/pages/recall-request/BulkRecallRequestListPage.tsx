import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Check, ClipboardCheck, PackageX, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { HelpButton } from '@/components/help/HelpButton';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { Pagination } from '@/components/common/Pagination';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import {
  approveBulkRecallRequest,
  getBulkRecallRequests,
  rejectBulkRecallRequest,
} from '@/api/recallApi';
import { useAuth } from '@/hooks/useAuth';
import { CloseBulkRecallDialog } from './CloseBulkRecallDialog';
import type {
  BulkRecallRequest,
  BulkRecallRequestStatus,
} from '@/types/bulkRecall';

const PAGE_SIZE = 10;

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'COMPLETED', label: 'Đã xử lý' },
  { value: 'REJECTED', label: 'Đã từ chối' },
];

const STATUS_TONE: Record<BulkRecallRequestStatus, 'warning' | 'success' | 'danger' | 'info'> = {
  PENDING: 'warning',
  APPROVED: 'info',
  COMPLETED: 'success',
  REJECTED: 'danger',
};

const STATUS_LABEL: Record<BulkRecallRequestStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  COMPLETED: 'Đã xử lý',
  REJECTED: 'Đã từ chối',
};

/**
 * Danh sách yêu cầu thu hồi theo phạm vi ảnh hưởng (NCL-08-CN-011, NCL-08-CN-012).
 * Cho phép quản lý (VT-02) tra cứu, mở chi tiết để duyệt và kết thúc vụ việc thu hồi.
 */
export const BulkRecallRequestListPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [data, setData] = useState<BulkRecallRequest[]>([]);
  const [loading, setLoading] = useState(false);

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);

  // Dialog kết thúc vụ việc (NCL-08-CN-012)
  const [selectedRequestForClose, setSelectedRequestForClose] = useState<BulkRecallRequest | null>(null);
  const [closeDialogOpen, setCloseDialogOpen] = useState(false);

  // Dialog phê duyệt yêu cầu (NCL-08-CN-011)
  const [selectedRequestForApprove, setSelectedRequestForApprove] = useState<BulkRecallRequest | null>(null);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [approveRemarks, setApproveRemarks] = useState('');

  // Dialog từ chối yêu cầu (NCL-08-CN-011)
  const [selectedRequestForReject, setSelectedRequestForReject] = useState<BulkRecallRequest | null>(null);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  const [actionLoading, setActionLoading] = useState(false);

  const isManager = user?.roleCode === 'VT-02' || user?.roleCode === 'VT-01';

  const handleApprove = async () => {
    if (!selectedRequestForApprove) return;
    try {
      setActionLoading(true);
      const result = await approveBulkRecallRequest(selectedRequestForApprove.id, {
        remarks: approveRemarks.trim() || undefined,
      });
      const includedCount = result.shipments?.filter((s) => s.included).length || 0;
      toast.success(
        `Đã phê duyệt yêu cầu thu hồi. Tổng số lô chuyển sang trạng thái "Đang thu hồi": ${includedCount}`,
      );
      setApproveDialogOpen(false);
      setSelectedRequestForApprove(null);
      load();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể phê duyệt yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequestForReject) return;
    if (!rejectionReason.trim()) {
      toast.error('Lý do từ chối không được để trống');
      return;
    }
    try {
      setActionLoading(true);
      await rejectBulkRecallRequest(selectedRequestForReject.id, {
        reason: rejectionReason.trim(),
      });
      toast.success('Đã từ chối yêu cầu thu hồi');
      setRejectDialogOpen(false);
      setSelectedRequestForReject(null);
      load();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể từ chối yêu cầu');
    } finally {
      setActionLoading(false);
    }
  };

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Yêu cầu thu hồi theo phạm vi' },
  ]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getBulkRecallRequests({
        page: 0,
        size: 1000,
      });
      setData(result.items);
    } catch (err: any) {
      toast.error(
        err.response?.data?.message || 'Không thể tải danh sách yêu cầu thu hồi',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return data.filter((item) => {
      const matchKeyword =
        !q ||
        item.productionLotName.toLowerCase().includes(q) ||
        (item.requestedBy?.fullName ?? '').toLowerCase().includes(q) ||
        item.reason.toLowerCase().includes(q);
      const matchStatus =
        status === 'ALL' || item.status === status;
      return matchKeyword && matchStatus;
    });
  }, [data, search, status]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = filtered.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleString('vi-VN');
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="space-y-4">
      <ListPageHeader
        icon={PackageX}
        title="Yêu cầu thu hồi theo phạm vi ảnh hưởng"
        description="Danh sách yêu cầu thu hồi hàng loạt theo kết quả truy vết phạm vi ảnh hưởng."
        actions={<HelpButton screenKey="bulk-recall-request-list" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo lô sản xuất, người tạo hoặc lý do..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
              <FilterSelect
                value={status}
                onValueChange={(val) => {
                  setStatus(val || 'ALL');
                  setPage(0);
                }}
                options={STATUS_FILTER_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={load} loading={loading} />}
        />

        <DataTableShell
          colSpan={8}
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Lô sản xuất nguồn</TableHead>
              <TableHead>Người tạo</TableHead>
              <TableHead>Thời điểm</TableHead>
              <TableHead>Lý do</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-center w-32">Thao tác</TableHead>
              <TableHead className="text-center w-24">Chi tiết</TableHead>
            </>
          }
          body={paginated.map((item, index) => (
            <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
              <TableCell className="text-center font-medium text-muted-foreground">
                {safePage * PAGE_SIZE + index + 1}
              </TableCell>
              <TableCell className="font-medium">{item.productionLotName}</TableCell>
              <TableCell>{item.requestedBy?.fullName || '—'}</TableCell>
              <TableCell>{formatDate(item.requestedAt)}</TableCell>
              <TableCell className="max-w-[240px] truncate" title={item.reason}>
                {item.reason}
              </TableCell>
              <TableCell>
                <StatusBadge
                  label={STATUS_LABEL[item.status]}
                  tone={STATUS_TONE[item.status]}
                />
              </TableCell>
              <TableCell className="text-center">
                {item.status === 'APPROVED' && isManager ? (
                  <Button
                    size="icon-sm"
                    variant="outline"
                    className="border-emerald-300 bg-emerald-50 text-emerald-600 hover:bg-emerald-100 hover:text-emerald-700 cursor-pointer"
                    title="Kết thúc vụ việc"
                    aria-label="Kết thúc vụ việc"
                    onClick={() => {
                      setSelectedRequestForClose(item);
                      setCloseDialogOpen(true);
                    }}
                  >
                    <ClipboardCheck className="size-4" aria-hidden="true" />
                  </Button>
                ) : item.status === 'PENDING' && isManager && user?.userId !== item.requestedBy?.userId ? (
                  <div className="flex items-center justify-center gap-1.5">
                    <Button
                      size="icon-sm"
                      variant="outline"
                      className="border-emerald-300 bg-emerald-50 text-emerald-600 hover:bg-emerald-100 hover:text-emerald-700 cursor-pointer"
                      title="Phê duyệt yêu cầu"
                      aria-label="Phê duyệt yêu cầu"
                      onClick={() => {
                        setSelectedRequestForApprove(item);
                        setApproveRemarks('');
                        setApproveDialogOpen(true);
                      }}
                    >
                      <Check className="size-4" aria-hidden="true" />
                    </Button>
                    <Button
                      size="icon-sm"
                      variant="outline"
                      className="border-rose-300 bg-rose-50 text-rose-600 hover:bg-rose-100 hover:text-rose-700 cursor-pointer"
                      title="Từ chối yêu cầu"
                      aria-label="Từ chối yêu cầu"
                      onClick={() => {
                        setSelectedRequestForReject(item);
                        setRejectionReason('');
                        setRejectDialogOpen(true);
                      }}
                    >
                      <X className="size-4" aria-hidden="true" />
                    </Button>
                  </div>
                ) : (
                  <span className="text-muted-foreground/40">—</span>
                )}
              </TableCell>
              <TableCell className="text-center">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate(`/recall-requests/bulk/${item.id}`)}
                >
                  Chi tiết
                </Button>
              </TableCell>
            </TableRow>
          ))}
          loading={loading}
          empty={!loading && filtered.length === 0}
          loadingMessage="Đang tải danh sách yêu cầu thu hồi..."
          emptyMessage={
            search || status !== 'ALL'
              ? 'Không tìm thấy yêu cầu nào phù hợp với bộ lọc.'
              : 'Chưa có yêu cầu thu hồi nào.'
          }
        />

        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          totalElements={filtered.length}
          pageSize={PAGE_SIZE}
          loading={loading}
          itemLabel="yêu cầu thu hồi"
          onPageChange={setPage}
        />
      </ListCard>

      {/* Dialog kết thúc vụ việc thu hồi (NCL-08-CN-012) */}
      <CloseBulkRecallDialog
        open={closeDialogOpen}
        bulkRequest={selectedRequestForClose}
        onClose={() => {
          setCloseDialogOpen(false);
          setSelectedRequestForClose(null);
        }}
        onSuccess={load}
      />

      {/* Dialog phê duyệt yêu cầu thu hồi hàng loạt */}
      <AlertDialog open={approveDialogOpen} onOpenChange={setApproveDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Phê duyệt yêu cầu thu hồi hàng loạt</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-4">
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-sm text-amber-800">
              <p className="font-medium mb-1">Cảnh báo quan trọng</p>
              <p>
                Sau khi phê duyệt, {selectedRequestForApprove?.shipments?.filter((s) => s.included).length || 0} lô hàng trong phạm vi sẽ chuyển sang trạng thái{' '}
                <strong>"Đang thu hồi"</strong> và hệ thống sẽ:
              </p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>Bật cảnh báo thu hồi công khai khi tra cứu tem cho các lô hàng này</li>
                <li>Gửi thông báo đến các doanh nghiệp thu mua liên quan</li>
              </ul>
            </div>
            <div className="space-y-2">
              <Label htmlFor="list-approve-remarks">Ghi chú phê duyệt (tùy chọn)</Label>
              <Textarea
                id="list-approve-remarks"
                placeholder="Nhập ghi chú khi phê duyệt..."
                value={approveRemarks}
                onChange={(e) => setApproveRemarks(e.target.value)}
                className="min-h-[80px]"
                disabled={actionLoading}
              />
            </div>
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={handleApprove}
              disabled={actionLoading}
            >
              {actionLoading ? 'Đang xử lý...' : 'Phê duyệt'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Dialog từ chối yêu cầu thu hồi */}
      <AlertDialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Từ chối yêu cầu thu hồi</AlertDialogTitle>
          </AlertDialogHeader>
          <div className="space-y-2">
            <Label htmlFor="list-rejection-reason">
              Lý do từ chối <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="list-rejection-reason"
              placeholder="Nhập lý do từ chối yêu cầu này..."
              value={rejectionReason}
              onChange={(e) => setRejectionReason(e.target.value)}
              className="min-h-[80px]"
              disabled={actionLoading}
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={actionLoading}>Hủy</AlertDialogCancel>
            <Button
              variant="destructive"
              onClick={handleReject}
              disabled={actionLoading}
            >
              {actionLoading ? 'Đang xử lý...' : 'Từ chối'}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};

export default BulkRecallRequestListPage;