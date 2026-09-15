import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  History,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Ban,
  Key,
  Send,
} from 'lucide-react';
import { toast } from 'sonner';

import type {
  PartnerWebhookNotificationResponse,
  WebhookDeliveryStatus,
} from '@/types/apiKey';
import { getPartnerWebhookNotifications } from '@/api/apiKeyApi';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { Pagination } from '@/components/common/Pagination';
import { DataTableShell } from '@/components/common/DataTableShell';
import { TableHead, TableRow, TableCell } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { HelpButton } from '@/components/help/HelpButton';

const STATUS_FILTER_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'SUCCESS', label: 'Thành công' },
  { value: 'PENDING_RETRY', label: 'Chờ thử lại (Retrying)' },
  { value: 'FAILED', label: 'Thất bại (Failed)' },
  { value: 'CANCELLED', label: 'Đã hủy (Khóa thu hồi)' },
];

export const PartnerWebhookNotificationHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [notifications, setNotifications] = useState<PartnerWebhookNotificationResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Phân trang
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Khóa API đối tác', href: '/integration/api-keys' },
    { label: 'Lịch sử thông báo thu hồi' },
  ]);

  const fetchHistory = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const filter = statusFilter === 'ALL' ? undefined : (statusFilter as WebhookDeliveryStatus);
      const data = await getPartnerWebhookNotifications(id, filter, page, pageSize);
      setNotifications(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch {
      toast.error('Không thể tải lịch sử gửi thông báo.');
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, [id, page, statusFilter]);

  // Bộ lọc tìm kiếm trên máy khách
  const filteredNotifications = useMemo(() => {
    if (!search.trim()) return notifications;
    const q = search.toLowerCase().trim();
    return notifications.filter(
      (n) =>
        n.lotCode.toLowerCase().includes(q) ||
        (n.publicReason && n.publicReason.toLowerCase().includes(q)) ||
        (n.targetUrl && n.targetUrl.toLowerCase().includes(q))
    );
  }, [notifications, search]);

  const toggleExpand = (notifId: string) => {
    setExpandedId((prev) => (prev === notifId ? null : notifId));
  };

  const renderStatusBadge = (status: WebhookDeliveryStatus) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <Badge className="bg-emerald-50 text-emerald-700 dark:bg-emerald-950/70 dark:text-emerald-300 border-emerald-300 dark:border-emerald-800 gap-1 font-medium">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
            <span>Thành công</span>
          </Badge>
        );
      case 'PENDING_RETRY':
        return (
          <Badge className="bg-amber-50 text-amber-700 dark:bg-amber-950/70 dark:text-amber-300 border-amber-300 dark:border-amber-800 gap-1 font-medium">
            <Clock className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
            <span>Đang thử lại</span>
          </Badge>
        );
      case 'FAILED':
        return (
          <Badge variant="destructive" className="gap-1 font-medium">
            <AlertTriangle className="w-3.5 h-3.5" /> Thất bại
          </Badge>
        );
      case 'CANCELLED':
        return (
          <Badge variant="secondary" className="gap-1 text-muted-foreground font-medium">
            <Ban className="w-3.5 h-3.5" /> Đã hủy (Khóa thu hồi)
          </Badge>
        );
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  // Thống kê nhanh trong trang
  const successCount = notifications.filter((n) => n.deliveryStatus === 'SUCCESS').length;
  const retryingCount = notifications.filter((n) => n.deliveryStatus === 'PENDING_RETRY').length;
  const failedCount = notifications.filter((n) => n.deliveryStatus === 'FAILED' || n.deliveryStatus === 'CANCELLED').length;

  return (
    <div className="space-y-6">
      {/* Header trang (chỉ giữ biểu tượng và tiêu đề, điều hướng bằng breadcrumbs) */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-950/60 dark:text-emerald-400">
            <History className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Lịch sử gửi thông báo thu hồi
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <HelpButton screenKey="admin-api-keys" />
          <Button
            variant="outline"
            onClick={() => navigate('/integration/api-keys')}
            className="gap-2"
          >
            <Key className="w-4 h-4" />
            <span>Quản lý Khóa API</span>
          </Button>
        </div>
      </div>

      {/* Thống kê phát thông báo */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Tổng gói tin gửi đi</p>
              <h3 className="text-2xl font-bold mt-1 text-foreground">{totalElements}</h3>
              <p className="text-[11px] text-muted-foreground mt-0.5">Tất cả thông báo</p>
            </div>
            <div className="p-3 bg-blue-500/10 text-blue-600 dark:text-blue-400 rounded-full">
              <Send className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Gửi thành công</p>
              <h3 className="text-2xl font-bold mt-1 text-emerald-600 dark:text-emerald-400">
                {successCount}
              </h3>
              <p className="text-[11px] text-muted-foreground mt-0.5">Phản hồi 2xx từ đối tác</p>
            </div>
            <div className="p-3 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 rounded-full">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card">
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-muted-foreground">Chờ thử lại / Lỗi</p>
              <h3 className="text-2xl font-bold mt-1 text-amber-600 dark:text-amber-400">
                {retryingCount + failedCount}
              </h3>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                {retryingCount} đang chờ retry
              </p>
            </div>
            <div className="p-3 bg-amber-500/10 text-amber-600 dark:text-amber-400 rounded-full">
              <Clock className="w-5 h-5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Bảng dữ liệu danh sách lịch sử thông báo */}
      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo mã lô, lý do thu hồi, URL..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <FilterSelect
                value={statusFilter}
                onValueChange={(val) => {
                  setStatusFilter(val || 'ALL');
                  setPage(0);
                }}
                options={STATUS_FILTER_OPTIONS}
                placeholder="Lọc trạng thái"
              />
            </>
          }
          right={<RefreshButton onClick={fetchHistory} loading={loading} />}
        />

        <DataTableShell
          header={
            <>
              <TableHead className="w-12 text-center">STT</TableHead>
              <TableHead>Mã lô hàng thu hồi</TableHead>
              <TableHead>Trạng thái thu hồi</TableHead>
              <TableHead>Kết quả phát tin</TableHead>
              <TableHead className="text-center">Số lần gửi</TableHead>
              <TableHead>Phản hồi HTTP gần nhất</TableHead>
              <TableHead>Thời gian phát sinh</TableHead>
              <TableHead className="text-center">Chi tiết</TableHead>
            </>
          }
          body={filteredNotifications.map((item, index) => {
            const isExpanded = expandedId === item.id;
            return (
              <React.Fragment key={item.id}>
                <TableRow className="hover:bg-muted/40 transition-colors">
                  <TableCell className="text-center font-medium text-muted-foreground">
                    {page * pageSize + index + 1}
                  </TableCell>

                  <TableCell>
                    <div className="font-semibold text-foreground text-sm">{item.lotCode}</div>
                    {item.publicReason && (
                      <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1 max-w-xs" title={item.publicReason}>
                        Lý do: {item.publicReason}
                      </p>
                    )}
                  </TableCell>

                  <TableCell>
                    <Badge variant="outline" className="font-mono text-xs">
                      {item.newStatus === 'RECALLING' ? 'Đang thu hồi' : 'Đã thu hồi'}
                    </Badge>
                  </TableCell>

                  <TableCell>
                    <div className="space-y-1">
                      {renderStatusBadge(item.deliveryStatus)}
                      {item.nextRetryAt && item.deliveryStatus === 'PENDING_RETRY' && (
                        <div className="text-[11px] text-amber-600 dark:text-amber-400 flex items-center gap-1">
                          <Clock className="w-3 h-3 shrink-0" />
                          <span>Thử lại lúc: {new Date(item.nextRetryAt).toLocaleTimeString('vi-VN')}</span>
                        </div>
                      )}
                    </div>
                  </TableCell>

                  <TableCell className="text-center">
                    <span className="inline-block px-2.5 py-0.5 rounded-full text-xs font-mono font-semibold bg-muted text-foreground">
                      {item.attemptCount} / {item.maxAttempts}
                    </span>
                  </TableCell>

                  <TableCell>
                    {item.lastHttpStatus ? (
                      <span
                        className={`inline-block font-mono text-xs font-bold px-2 py-0.5 rounded ${
                          item.lastHttpStatus >= 200 && item.lastHttpStatus < 300
                            ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300'
                            : 'bg-rose-50 text-rose-700 dark:bg-rose-950 dark:text-rose-300'
                        }`}
                      >
                        HTTP {item.lastHttpStatus}
                      </span>
                    ) : (
                      <span className="text-xs text-muted-foreground italic">Chưa có</span>
                    )}
                    {item.lastErrorMessage && item.deliveryStatus !== 'SUCCESS' && (
                      <p className="text-[11px] text-rose-500 font-mono mt-0.5 truncate max-w-[200px]" title={item.lastErrorMessage}>
                        {item.lastErrorMessage}
                      </p>
                    )}
                  </TableCell>

                  <TableCell>
                    <div className="text-xs space-y-0.5">
                      <div className="font-medium text-foreground">
                        {new Date(item.createdAt).toLocaleDateString('vi-VN')}
                      </div>
                      <div className="text-muted-foreground font-mono text-[11px]">
                        {new Date(item.createdAt).toLocaleTimeString('vi-VN')}
                      </div>
                    </div>
                  </TableCell>

                  <TableCell className="text-center">
                    {item.attempts && item.attempts.length > 0 ? (
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-8 text-xs"
                        onClick={() => toggleExpand(item.id)}
                      >
                        {isExpanded ? 'Đóng' : 'Chi tiết'}
                      </Button>
                    ) : (
                      <span className="text-xs text-muted-foreground">-</span>
                    )}
                  </TableCell>
                </TableRow>

                {/* Dòng mở rộng hiển thị chi tiết các lần thử gửi (Attempts log) */}
                {isExpanded && item.attempts && item.attempts.length > 0 && (
                  <TableRow className="bg-muted/20">
                    <TableCell colSpan={8} className="p-4">
                      <div className="space-y-2.5 rounded-lg border bg-card p-3.5 shadow-xs">
                        <div className="flex items-center justify-between text-xs font-semibold text-foreground pb-2 border-b">
                          <span className="flex items-center gap-1.5">
                            <History className="w-4 h-4 text-primary" />
                            Nhật ký chi tiết các lần gửi thông báo tới đối tác
                          </span>
                          <span className="text-muted-foreground font-mono text-[11px]">
                            URL: {item.targetUrl}
                          </span>
                        </div>

                        <div className="space-y-2">
                          {item.attempts.map((att) => (
                            <div
                              key={att.attemptNumber}
                              className="p-2.5 rounded-md bg-muted/40 text-xs font-mono space-y-1 border border-border/50"
                            >
                              <div className="flex items-center justify-between text-muted-foreground">
                                <div className="flex items-center gap-2">
                                  <span className="font-bold text-foreground">
                                    Lần #{att.attemptNumber}
                                  </span>
                                  <span>•</span>
                                  <span>
                                    {new Date(att.attemptedAt).toLocaleString('vi-VN')}
                                  </span>
                                </div>
                                <div className="flex items-center gap-3">
                                  {att.httpStatus && (
                                    <span
                                      className={`font-bold ${
                                        att.httpStatus >= 200 && att.httpStatus < 300
                                          ? 'text-emerald-600 dark:text-emerald-400'
                                          : 'text-rose-600 dark:text-rose-400'
                                      }`}
                                    >
                                      HTTP {att.httpStatus}
                                    </span>
                                  )}
                                  <span className="text-muted-foreground">{att.durationMs} ms</span>
                                </div>
                              </div>

                              {att.errorMessage && (
                                <div className="text-[11px] text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 p-1.5 rounded border border-rose-200 dark:border-rose-900 break-all">
                                  Lỗi: {att.errorMessage}
                                </div>
                              )}

                              {att.responseBody && (
                                <div className="text-[11px] text-muted-foreground bg-background p-1.5 rounded border truncate max-w-full">
                                  Phản hồi máy chủ: {att.responseBody}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    </TableCell>
                  </TableRow>
                )}
              </React.Fragment>
            );
          })}
          loading={loading}
          empty={!loading && filteredNotifications.length === 0}
          colSpan={8}
          loadingMessage="Đang tải lịch sử gửi thông báo..."
          emptyMessage="Chưa có thông báo thu hồi nào được gửi tới đối tác này."
        />

        {/* Phân trang chuẩn */}
        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          pageSize={pageSize}
          loading={loading}
          itemLabel="thông báo"
          onPageChange={setPage}
        />
      </ListCard>
    </div>
  );
};

export default PartnerWebhookNotificationHistoryPage;
