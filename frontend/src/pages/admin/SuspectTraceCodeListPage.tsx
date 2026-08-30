import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSuspectTraceCodes } from '@/api/suspectTraceCodeApi';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import type { SuspectTraceCodeResponse, PageResponse } from '@/types/suspectTraceCode';
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Lock,
  MapPin,
  Search,
  ShieldAlert,
  Unlock,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { LockTraceCodeDialog } from './components/LockTraceCodeDialog';
import { UnlockTraceCodeDialog } from './components/UnlockTraceCodeDialog';
import { HelpButton } from '@/components/help/HelpButton';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { DataTableShell } from '@/components/common/DataTableShell';
import { RefreshButton } from '@/components/common/RefreshButton';
import { StatusBadge } from '@/components/common/StatusBadge';
import { useAuth } from '@/hooks/useAuth';

const EMPTY_PAGE: PageResponse<SuspectTraceCodeResponse> = {
  items: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 20,
  first: true,
  last: true,
};

const formatDateTime = (value: string | null) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export default function SuspectTraceCodeListPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [draftMinScore, setDraftMinScore] = useState<string>('');
  const [draftStatus, setDraftStatus] = useState<string>('ALL');
  const [minScore, setMinScore] = useState<number | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [result, setResult] = useState<PageResponse<SuspectTraceCodeResponse>>(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [lockTarget, setLockTarget] = useState<SuspectTraceCodeResponse | null>(null);
  const [unlockTarget, setUnlockTarget] = useState<SuspectTraceCodeResponse | null>(null);

  const suspectCount = useMemo(
    () => result.items.filter((item) => item.status === 'SUSPECT').length,
    [result.items],
  );
  const lockedCount = useMemo(
    () => result.items.filter((item) => item.status === 'LOCKED').length,
    [result.items],
  );
  const activeCount = useMemo(
    () => result.items.filter((item) => item.status === 'ACTIVE').length,
    [result.items],
  );

  const fetchData = async () => {
    try {
      setLoading(true);
      const data = await getSuspectTraceCodes({
        minScore,
        status: statusFilter,
        page,
        size,
      });
      setResult(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message || 'Không thể tải danh sách mã tem nghi vấn',
      );
      setResult(EMPTY_PAGE);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [minScore, statusFilter, page, size]);

  const applyFilters = () => {
    let parsedScore: number | undefined = undefined;
    if (draftMinScore.trim() !== '') {
      parsedScore = parseInt(draftMinScore, 10);
      if (isNaN(parsedScore) || parsedScore < 0 || parsedScore > 100) {
        toast.error('Điểm nghi vấn phải từ 0 đến 100');
        return;
      }
    }
    setPage(0);
    setMinScore(parsedScore);
    setStatusFilter(draftStatus === 'ALL' ? undefined : draftStatus);
  };

  const resetFilters = () => {
    setDraftMinScore('');
    setDraftStatus('ALL');
    setPage(0);
    setMinScore(undefined);
    setStatusFilter(undefined);
  };

  const getStatusBadge = (status: string) => {
    if (status === 'SUSPECT') {
      return <StatusBadge label="Nghi vấn" tone="warning" icon={AlertTriangle} />;
    }
    if (status === 'LOCKED') {
      return <StatusBadge label="Đã khóa" tone="danger" icon={Lock} />;
    }
    if (status === 'ACTIVE') {
      return <StatusBadge label="Đã xác minh" tone="success" icon={CheckCircle2} />;
    }
    return <StatusBadge label={status} tone="neutral" />;
  };

  const getScoreColor = (score: number) => {
    if (score >= 70) return 'text-red-600 font-bold';
    if (score >= 50) return 'text-amber-600 font-semibold';
    return 'text-muted-foreground';
  };

  const header = (
    <>
      <TableHead>Mã tem</TableHead>
      <TableHead>Lô hàng</TableHead>
      <TableHead>Điểm NV</TableHead>
      <TableHead>Lý do nghi vấn</TableHead>
      <TableHead>Lượt quét</TableHead>
      <TableHead>Địa điểm</TableHead>
      <TableHead>Trạng thái</TableHead>
      <TableHead className="text-center">Thao tác</TableHead>
      <TableHead className="text-center">Chi tiết</TableHead>
    </>
  );

  const body = result.items.map((item) => (
    <TableRow key={item.id} className="hover:bg-muted/40 transition-colors">
      <TableCell>
        <span className="block max-w-36 truncate font-mono text-xs" title={item.codeValue}>
          {item.codeValue}
        </span>
      </TableCell>
      <TableCell>
        <span className="block max-w-44 truncate text-sm" title={item.shipmentName}>
          {item.shipmentName}
        </span>
      </TableCell>
      <TableCell>
        <span className={getScoreColor(item.suspicionScore)}>
          {item.suspicionScore}
        </span>
      </TableCell>
      <TableCell>
        <span className="block max-w-56 truncate text-xs text-muted-foreground"
          title={item.suspicionReason || undefined}>
          {item.suspicionReason || '—'}
        </span>
      </TableCell>
      <TableCell>
        <div className="text-sm">
          <p>{item.scanCount} lượt</p>
          <p className="text-xs text-muted-foreground">
            {formatDateTime(item.lastScannedAt)}
          </p>
        </div>
      </TableCell>
      <TableCell>
        <p className="flex items-center gap-1 text-sm">
          <MapPin className="h-3 w-3" />
          {item.uniqueLocations}
        </p>
      </TableCell>
      <TableCell>{getStatusBadge(item.status)}</TableCell>
      <TableCell>
        <div className="flex justify-center gap-1">
          {item.status === 'SUSPECT' && (
            <Button
              size="icon-sm"
              variant="ghost"
              title="Khóa tem"
              onClick={() => setLockTarget(item)}
            >
              <Lock className="size-4 text-red-600" />
            </Button>
          )}
          {item.status === 'LOCKED' && (
            <Button
              size="icon-sm"
              variant="ghost"
              title="Mở khóa tem"
              onClick={() => setUnlockTarget(item)}
            >
              <Unlock className="size-4 text-emerald-600" />
            </Button>
          )}
          {item.status !== 'SUSPECT' && item.status !== 'LOCKED' && (
            <span className="text-sm text-muted-foreground">—</span>
          )}
        </div>
      </TableCell>
      <TableCell>
        <div className="flex justify-center">
          <Button
            size="sm"
            variant="outline"
            onClick={() => navigate(`/admin/suspect-trace-codes/${item.id}`)}
          >
            Chi tiết
          </Button>
        </div>
      </TableCell>
    </TableRow>
  ));

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={ShieldAlert}
        iconBoxClassName="bg-red-100 text-red-700 dark:text-red-400"
        title="Quản lý mã tem nghi vấn"
        description="Xem xét và khóa các mã tem có dấu hiệu quét bất thường"
        actions={
          <>
            <HelpButton screenKey="admin-suspect-trace-codes" />
            <RefreshButton onClick={fetchData} loading={loading} />
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-4">
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Tổng số</p>
              <p className="text-2xl font-bold">{result.totalElements}</p>
              <p className="text-xs text-muted-foreground">Mã tem nghi vấn</p>
            </div>
            <AlertTriangle className="h-8 w-8 text-amber-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trang hiện tại</p>
              <p className="text-2xl font-bold">{suspectCount}</p>
              <p className="text-xs text-muted-foreground">Chưa khóa</p>
            </div>
            <ShieldAlert className="h-8 w-8 text-amber-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trang hiện tại</p>
              <p className="text-2xl font-bold">{lockedCount}</p>
              <p className="text-xs text-muted-foreground">Đã khóa</p>
            </div>
            <Lock className="h-8 w-8 text-red-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trang hiện tại</p>
              <p className="text-2xl font-bold">{activeCount}</p>
              <p className="text-xs text-muted-foreground">Đã xác minh</p>
            </div>
            <CheckCircle2 className="h-8 w-8 text-emerald-500" />
          </CardContent>
        </Card>
      </div>

      <ListCard>
        <ListToolbar
          left={
            <>
              <div className="space-y-1">
                <Label htmlFor="minScore" className="text-xs text-muted-foreground">Điểm nghi vấn tối thiểu</Label>
                <Input
                  id="minScore"
                  type="number"
                  min={0}
                  max={100}
                  value={draftMinScore}
                  onChange={(e) => setDraftMinScore(e.target.value)}
                  placeholder="30"
                  className="w-32"
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="statusFilter" className="text-xs text-muted-foreground">Trạng thái</Label>
                <Select
                  value={draftStatus}
                  onValueChange={(value) => value && setDraftStatus(value)}
                  items={[
                    { value: 'ALL', label: 'Tất cả' },
                    { value: 'SUSPECT', label: 'Nghi vấn' },
                    { value: 'LOCKED', label: 'Đã khóa' },
                    { value: 'ACTIVE', label: 'Đã xác minh' },
                  ]}
                >
                  <SelectTrigger id="statusFilter" className="w-40">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">Tất cả</SelectItem>
                    <SelectItem value="SUSPECT">Nghi vấn</SelectItem>
                    <SelectItem value="LOCKED">Đã khóa</SelectItem>
                    <SelectItem value="ACTIVE">Đã xác minh</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </>
          }
          right={
            <>
              <Button variant="delete" onClick={resetFilters} disabled={loading}>
                <X className="h-4 w-4" />
                Xóa bộ lọc
              </Button>
              <Button variant="search" onClick={applyFilters} disabled={loading}>
                <Search className="h-4 w-4" />
                Tìm kiếm
              </Button>
            </>
          }
        />

        <DataTableShell
          header={header}
          body={body}
          loading={loading}
          empty={!loading && result.items.length === 0}
          colSpan={9}
          loadingMessage="Đang tải danh sách mã tem nghi vấn..."
          emptyMessage="Không có mã tem nghi vấn"
        />

        {!loading && result.totalPages > 0 && (
          <div className="flex flex-col gap-3 pt-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>Hiển thị</span>
              <Select
                value={String(size)}
                onValueChange={(value) => {
                  if (!value) return;
                  setSize(Number(value));
                  setPage(0);
                }}
              >
                <SelectTrigger className="h-8 w-20">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[10, 20, 50].map((option) => (
                    <SelectItem key={option} value={String(option)}>
                      {option}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <span>mã tem</span>
            </div>
            <div className="flex items-center justify-end gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((current) => current - 1)}
                disabled={page === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="min-w-28 text-center text-sm">
                Trang {result.page + 1} / {result.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((current) => current + 1)}
                disabled={page >= result.totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </ListCard>

      <LockTraceCodeDialog
        traceCode={lockTarget}
        onClose={() => setLockTarget(null)}
        onSuccess={() => {
          setLockTarget(null);
          fetchData();
        }}
      />

      <UnlockTraceCodeDialog
        traceCode={unlockTarget}
        currentUserId={user?.userId}
        onClose={() => setUnlockTarget(null)}
        onSuccess={() => {
          setUnlockTarget(null);
          fetchData();
        }}
      />
    </div>
  );
}
