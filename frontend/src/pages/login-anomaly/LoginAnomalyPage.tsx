import { useEffect, useMemo, useState } from 'react';
import { getLoginAnomalies } from '@/api/loginAnomalyApi';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { PageResponse } from '@/types/common';
import type {
  LoginAnomaly,
  LoginAnomalyAccountStatusFilter,
  LoginAnomalySeverityFilter,
} from '@/types/loginAnomaly';
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  LockKeyhole,
  MapPin,
  RefreshCw,
  Search,
  ShieldAlert,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { LockAccountDialog } from '@/components/login-anomaly/LockAccountDialog';

interface AnomalyFilters {
  severity: LoginAnomalySeverityFilter;
  accountStatus: LoginAnomalyAccountStatusFilter;
  keyword: string;
  fromDate: string;
  toDate: string;
}

const INITIAL_FILTERS: AnomalyFilters = {
  severity: 'ALL',
  accountStatus: 'ALL',
  keyword: '',
  fromDate: '',
  toDate: '',
};

const EMPTY_PAGE: PageResponse<LoginAnomaly> = {
  items: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 20,
  first: true,
  last: true,
};

const formatDateTime = (value: string) =>
  new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

const getSeverityBadge = (severity: LoginAnomaly['severity']) => {
  if (severity === 'HIGH') {
    return {
      variant: 'destructive' as const,
      label: 'Cao',
    };
  }
  if (severity === 'MEDIUM') {
    return {
      variant: 'warning' as const,
      label: 'Trung bình',
    };
  }
  return {
    variant: 'secondary' as const,
    label: 'Thấp',
  };
};

export default function LoginAnomalyPage() {
  const [draftFilters, setDraftFilters] = useState<AnomalyFilters>(INITIAL_FILTERS);
  const [filters, setFilters] = useState<AnomalyFilters>(INITIAL_FILTERS);
  const [result, setResult] = useState<PageResponse<LoginAnomaly>>(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lockTarget, setLockTarget] = useState<LoginAnomaly | null>(null);

  const highOnPage = useMemo(
    () => result.items.filter((item) => item.severity === 'HIGH').length,
    [result.items],
  );
  const lockedOnPage = useMemo(
    () => result.items.filter((item) => item.accountStatus === 'LOCKED').length,
    [result.items],
  );

  const fetchAnomalies = async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await getLoginAnomalies({
        severity: filters.severity,
        accountStatus: filters.accountStatus,
        keyword: filters.keyword || undefined,
        fromDate: filters.fromDate || undefined,
        toDate: filters.toDate || undefined,
        page,
        size,
      });

      setResult(data);
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Không thể tải danh sách đăng nhập bất thường';
      toast.error(message);
      setError(message);
      setResult(EMPTY_PAGE);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnomalies();
  }, [filters, page, size]);

  const applyFilters = () => {
    if (
      draftFilters.fromDate &&
      draftFilters.toDate &&
      draftFilters.fromDate > draftFilters.toDate
    ) {
      toast.error('Ngày bắt đầu không được sau ngày kết thúc');
      return;
    }
    setPage(0);
    setFilters(draftFilters);
  };

  const resetFilters = () => {
    setDraftFilters(INITIAL_FILTERS);
    setPage(0);
    setFilters(INITIAL_FILTERS);
  };

  return (
    <div className="container mx-auto space-y-6 py-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-red-100 p-2.5 text-red-700">
            <ShieldAlert className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Đăng nhập bất thường</h1>
            <p className="text-sm text-muted-foreground">
              Theo dõi các phiên đăng nhập có dấu hiệu bất thường và khóa tạm tài khoản nghi vấn
            </p>
          </div>
        </div>
        <Button variant="outline" onClick={fetchAnomalies} disabled={loading}>
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Theo bộ lọc</p>
              <p className="text-2xl font-bold">{result.totalElements}</p>
              <p className="text-xs text-muted-foreground">Tổng bất thường</p>
            </div>
            <AlertTriangle className="h-8 w-8 text-amber-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trang hiện tại</p>
              <p className="text-2xl font-bold">{highOnPage}</p>
              <p className="text-xs text-muted-foreground">Mức độ cao</p>
            </div>
            <ShieldAlert className="h-8 w-8 text-red-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trang hiện tại</p>
              <p className="text-2xl font-bold">{lockedOnPage}</p>
              <p className="text-xs text-muted-foreground">Tài khoản bị khóa</p>
            </div>
            <LockKeyhole className="h-8 w-8 text-gray-500" />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Bộ lọc</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-5">
            <div className="space-y-2">
              <Label htmlFor="anomalySeverity">Mức độ</Label>
              <Select
                value={draftFilters.severity}
                onValueChange={(value) =>
                  value &&
                  setDraftFilters((current) => ({
                    ...current,
                    severity: value as LoginAnomalySeverityFilter,
                  }))
                }
              >
                <SelectTrigger id="anomalySeverity">
                  <span>
                    {draftFilters.severity === 'ALL'
                      ? 'Tất cả'
                      : draftFilters.severity === 'HIGH'
                        ? 'Cao'
                        : draftFilters.severity === 'MEDIUM'
                          ? 'Trung bình'
                          : 'Thấp'}
                  </span>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả</SelectItem>
                  <SelectItem value="HIGH">Cao</SelectItem>
                  <SelectItem value="MEDIUM">Trung bình</SelectItem>
                  <SelectItem value="LOW">Thấp</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="anomalyAccountStatus">Trạng thái tài khoản</Label>
              <Select
                value={draftFilters.accountStatus}
                onValueChange={(value) =>
                  value &&
                  setDraftFilters((current) => ({
                    ...current,
                    accountStatus: value as LoginAnomalyAccountStatusFilter,
                  }))
                }
              >
                <SelectTrigger id="anomalyAccountStatus">
                  <span>
                    {draftFilters.accountStatus === 'ALL'
                      ? 'Tất cả'
                      : draftFilters.accountStatus === 'LOCKED'
                        ? 'Bị khóa'
                        : 'Hoạt động'}
                  </span>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả</SelectItem>
                  <SelectItem value="ACTIVE">Hoạt động</SelectItem>
                  <SelectItem value="LOCKED">Bị khóa</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="anomalyKeyword">Từ khóa</Label>
              <Input
                id="anomalyKeyword"
                placeholder="Tên đăng nhập, họ tên hoặc IP"
                value={draftFilters.keyword}
                onChange={(event) =>
                  setDraftFilters((current) => ({
                    ...current,
                    keyword: event.target.value,
                  }))
                }
                onKeyDown={(event) => {
                  if (event.key === 'Enter') applyFilters();
                }}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="anomalyFromDate">Từ ngày</Label>
              <Input
                id="anomalyFromDate"
                type="date"
                value={draftFilters.fromDate}
                onChange={(event) =>
                  setDraftFilters((current) => ({
                    ...current,
                    fromDate: event.target.value,
                  }))
                }
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="anomalyToDate">Đến ngày</Label>
              <Input
                id="anomalyToDate"
                type="date"
                value={draftFilters.toDate}
                onChange={(event) =>
                  setDraftFilters((current) => ({
                    ...current,
                    toDate: event.target.value,
                  }))
                }
              />
            </div>
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="delete" onClick={resetFilters} disabled={loading}>
              <X className="h-4 w-4" />
              Xóa bộ lọc
            </Button>
            <Button variant="search" onClick={applyFilters} disabled={loading}>
              <Search className="h-4 w-4" />
              Tìm kiếm
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Danh sách đăng nhập bất thường</CardTitle>
          <span className="text-sm text-muted-foreground">
            Tổng số: {result.totalElements} bản ghi
          </span>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex justify-center py-16">
              <RefreshCw className="h-7 w-7 animate-spin text-primary" />
            </div>
          ) : error ? (
            <div className="flex flex-col items-center py-16 text-center">
              <AlertTriangle className="mb-3 h-10 w-10 text-red-500" />
              <p className="font-medium text-foreground">Không thể tải danh sách</p>
              <p className="mb-4 text-sm text-muted-foreground">{error}</p>
              <Button onClick={fetchAnomalies}>
                <RefreshCw className="h-4 w-4" />
                Thử lại
              </Button>
            </div>
          ) : result.items.length === 0 ? (
            <div className="flex flex-col items-center py-16 text-center text-muted-foreground">
              <CheckCircle2 className="mb-3 h-10 w-10 text-emerald-500" />
              <p className="font-medium text-foreground">Không có bản ghi phù hợp</p>
              <p className="text-sm">Hãy thử thay đổi bộ lọc hoặc khoảng thời gian tìm kiếm.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Thời gian đăng nhập</TableHead>
                    <TableHead>Người dùng</TableHead>
                    <TableHead>Tổ chức</TableHead>
                    <TableHead>IP</TableHead>
                    <TableHead>Vị trí</TableHead>
                    <TableHead>Nguyên nhân</TableHead>
                    <TableHead>Mức độ</TableHead>
                    <TableHead>Trạng thái tài khoản</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {result.items.map((anomaly) => {
                    const severity = getSeverityBadge(anomaly.severity);
                    return (
                      <TableRow key={anomaly.id}>
                        <TableCell className="whitespace-nowrap text-sm">
                          {formatDateTime(anomaly.loginAt)}
                        </TableCell>
                        <TableCell>
                          <p className="text-sm font-medium">{anomaly.fullName}</p>
                          <p className="text-xs text-muted-foreground">{anomaly.username}</p>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm">
                          {anomaly.organizationName}
                        </TableCell>
                        <TableCell>
                          <span className="block max-w-36 truncate font-mono text-xs" title={anomaly.ipAddress}>
                            {anomaly.ipAddress}
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="flex items-center gap-1 text-sm text-muted-foreground">
                            <MapPin className="h-3.5 w-3.5" />
                            {anomaly.location}
                          </span>
                        </TableCell>
                        <TableCell className="max-w-64 text-sm">{anomaly.reason}</TableCell>
                        <TableCell>
                          <Badge variant={severity.variant}>{severity.label}</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant={anomaly.accountStatus === 'LOCKED' ? 'destructive' : 'success'}>
                            {anomaly.accountStatus === 'LOCKED' ? 'Bị khóa' : 'Hoạt động'}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex justify-end">
                            <Button
                              variant="delete"
                              size="sm"
                              disabled={anomaly.accountStatus === 'LOCKED'}
                              onClick={() => setLockTarget(anomaly)}
                            >
                              <LockKeyhole className="h-4 w-4" />
                              {anomaly.accountStatus === 'LOCKED' ? 'Đã khóa' : 'Khóa tạm'}
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}

          {!loading && !error && result.totalPages > 0 && (
            <div className="flex flex-col gap-3 border-t px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
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
                <span>bản ghi</span>
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
        </CardContent>
      </Card>

      <LockAccountDialog
        anomaly={lockTarget}
        onClose={() => setLockTarget(null)}
        onLocked={() => {
          setLockTarget(null);
          fetchAnomalies();
        }}
      />
    </div>
  );
}
