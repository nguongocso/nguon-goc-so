import { useEffect, useMemo, useState } from 'react';
import { getScanAnomalyAlerts } from '@/api/scanAnomalyAlertApi';
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
import type {
  ScanAnomalyAlert,
  ScanAnomalyAlertFilterStatus,
  ScanAnomalyAlertListResponse,
} from '@/types/scanAnomalyAlert';
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eye,
  MapPin,
  RefreshCw,
  Search,
  ShieldAlert,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { ResolveScanAnomalyAlertDialog } from './components/ResolveScanAnomalyAlertDialog';
import { ScanAnomalyAlertDetailsDialog } from './components/ScanAnomalyAlertDetailsDialog';
import { HelpButton } from '@/components/help/HelpButton';

interface AlertFilters {
  status: ScanAnomalyAlertFilterStatus;
  fromDate: string;
  toDate: string;
}

const INITIAL_FILTERS: AlertFilters = {
  status: 'ALL',
  fromDate: '',
  toDate: '',
};

const EMPTY_PAGE: ScanAnomalyAlertListResponse = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 20,
};

const formatDateTime = (value: string) =>
  new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

export default function ScanAnomalyAlertPage() {
  const [draftFilters, setDraftFilters] = useState<AlertFilters>(INITIAL_FILTERS);
  const [filters, setFilters] = useState<AlertFilters>(INITIAL_FILTERS);
  const [result, setResult] = useState<ScanAnomalyAlertListResponse>(EMPTY_PAGE);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [detailsAlert, setDetailsAlert] = useState<ScanAnomalyAlert | null>(null);
  const [resolveAlert, setResolveAlert] = useState<ScanAnomalyAlert | null>(null);

  const pendingOnPage = useMemo(
    () => result.content.filter((alert) => alert.status === 'PENDING').length,
    [result.content],
  );
  const highOnPage = useMemo(
    () => result.content.filter((alert) => alert.severity === 'HIGH').length,
    [result.content],
  );

  const fetchAlerts = async () => {
    try {
      setLoading(true);

      const data = await getScanAnomalyAlerts({
        status: filters.status === 'ALL' ? undefined : filters.status,
        fromDate: filters.fromDate || undefined,
        toDate: filters.toDate || undefined,
        page,
        size,
      });

      setResult(data);
    } catch (error: any) {
      toast.error(
        error.response?.data?.message ||
        'Không thể tải danh sách cảnh báo',
      );

      setResult((current) => ({
        ...current,
        content: [],
      }));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
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

  const openResolveDialog = (alert: ScanAnomalyAlert) => {
    setDetailsAlert(null);
    setResolveAlert(alert);
  };

  return (
    <div className="container mx-auto space-y-6 py-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-amber-100 p-2.5 text-amber-700">
            <ShieldAlert className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Cảnh báo tem quét bất thường</h1>
            <p className="text-sm text-muted-foreground">
              Phát hiện mã có dấu hiệu được quét tại nhiều vị trí khác nhau
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="alert-scan-anomaly" />
          <Button variant="outline" onClick={fetchAlerts} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Theo bộ lọc</p>
              <p className="text-2xl font-bold">{result.totalElements}</p>
              <p className="text-xs text-muted-foreground">Tổng cảnh báo</p>
            </div>
            <AlertTriangle className="h-8 w-8 text-amber-500" />
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm text-muted-foreground">Trang hiện tại</p>
              <p className="text-2xl font-bold">{pendingOnPage}</p>
              <p className="text-xs text-muted-foreground">Chờ xử lý</p>
            </div>
            <RefreshCw className="h-8 w-8 text-blue-500" />
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
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Bộ lọc cảnh báo</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-3">
            <div className="space-y-2">
              <Label htmlFor="alertStatus">Trạng thái</Label>
              <Select
                value={draftFilters.status}
                onValueChange={(value) =>
                  value &&
                  setDraftFilters((current) => ({
                    ...current,
                    status: value as ScanAnomalyAlertFilterStatus,
                  }))
                }
              >
                <SelectTrigger id="alertStatus">
                  <span>
                    {draftFilters.status === 'ALL'
                      ? 'Tất cả'
                      : draftFilters.status === 'PENDING'
                        ? 'Chờ xử lý'
                        : 'Đã xử lý'}
                  </span>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả</SelectItem>
                  <SelectItem value="PENDING">Chờ xử lý</SelectItem>
                  <SelectItem value="RESOLVED">Đã xử lý</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="alertFromDate">Từ ngày</Label>
              <Input
                id="alertFromDate"
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
              <Label htmlFor="alertToDate">Đến ngày</Label>
              <Input
                id="alertToDate"
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
          <CardTitle className="text-base">Danh sách cảnh báo</CardTitle>
          <span className="text-sm text-muted-foreground">
            Tổng số: {result.totalElements} cảnh báo
          </span>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex justify-center py-16">
              <RefreshCw className="h-7 w-7 animate-spin text-primary" />
            </div>
          ) : result.content.length === 0 ? (
            <div className="flex flex-col items-center py-16 text-center text-muted-foreground">
              <CheckCircle2 className="mb-3 h-10 w-10 text-emerald-500" />
              <p className="font-medium text-foreground">Không có cảnh báo phù hợp</p>
              <p className="text-sm">Hãy thử thay đổi trạng thái hoặc khoảng thời gian lọc.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Thời gian tạo</TableHead>
                    <TableHead>Mức độ</TableHead>
                    <TableHead>Mã TraceCode</TableHead>
                    <TableHead>Dữ liệu quét</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {result.content.map((alert) => (
                    <TableRow key={alert.id}>
                      <TableCell className="whitespace-nowrap text-sm">
                        {formatDateTime(alert.createdAt)}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={alert.severity === 'HIGH' ? 'destructive' : 'outline'}
                          className={alert.severity === 'MEDIUM' ? 'border-amber-300 text-amber-700' : undefined}
                        >
                          {alert.severity === 'HIGH' ? 'Cao' : 'Trung bình'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <span className="block max-w-44 truncate font-mono text-xs" title={alert.relatedEntityId}>
                          {alert.relatedEntityId}
                        </span>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          <p>{alert.details.scanCount} lượt quét</p>
                          <p className="flex items-center gap-1 text-xs text-muted-foreground">
                            <MapPin className="h-3 w-3" />
                            {alert.details.locations.length} vị trí · ngưỡng {alert.details.thresholdConfigured}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={alert.status === 'PENDING' ? 'secondary' : 'outline'}>
                          {alert.status === 'PENDING' ? 'Chờ xử lý' : 'Đã xử lý'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDetailsAlert(alert)}
                          >
                            <Eye className="h-4 w-4" />
                            Chi tiết
                          </Button>
                          {alert.status === 'PENDING' && (
                            <Button
                              size="sm"
                              onClick={() => openResolveDialog(alert)}
                              className="bg-emerald-600 hover:bg-emerald-700"
                            >
                              <CheckCircle2 className="h-4 w-4" />
                              Xử lý
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}

          {!loading && result.totalPages > 0 && (
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
                <span>cảnh báo</span>
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

      <ScanAnomalyAlertDetailsDialog
        alert={detailsAlert}
        onClose={() => setDetailsAlert(null)}
        onResolve={openResolveDialog}
      />
      <ResolveScanAnomalyAlertDialog
        alert={resolveAlert}
        onClose={() => setResolveAlert(null)}
        onResolved={() => {
          setResolveAlert(null);
          fetchAlerts();
        }}
      />
    </div>
  );
}
