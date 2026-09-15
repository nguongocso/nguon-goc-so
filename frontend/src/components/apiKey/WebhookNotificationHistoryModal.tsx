import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  History,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Ban,
  ChevronDown,
  ChevronRight,
  RefreshCw,
  Loader2,
} from 'lucide-react';
import type {
  PartnerApiKeyResponse,
  PartnerWebhookNotificationResponse,
  WebhookDeliveryStatus,
} from '@/types/apiKey';
import { getPartnerWebhookNotifications } from '@/api/apiKeyApi';

interface WebhookNotificationHistoryModalProps {
  open: boolean;
  apiKey: PartnerApiKeyResponse | null;
  onClose: () => void;
}

export const WebhookNotificationHistoryModal: React.FC<WebhookNotificationHistoryModalProps> = ({
  open,
  apiKey,
  onClose,
}) => {
  const [notifications, setNotifications] = useState<PartnerWebhookNotificationResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchHistory = async () => {
    if (!apiKey) return;
    setLoading(true);
    try {
      const filter = statusFilter === 'ALL' ? undefined : (statusFilter as WebhookDeliveryStatus);
      const data = await getPartnerWebhookNotifications(apiKey.id, filter, page, 10);
      setNotifications(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch {
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open && apiKey) {
      setPage(0);
      fetchHistory();
    }
  }, [open, apiKey, statusFilter]);

  useEffect(() => {
    if (open && apiKey) {
      fetchHistory();
    }
  }, [page]);

  if (!apiKey) {
    return null;
  }

  const renderStatusBadge = (status: WebhookDeliveryStatus) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <Badge className="bg-emerald-100 dark:bg-emerald-950 text-emerald-700 dark:text-emerald-300 border-emerald-300 gap-1">
            <CheckCircle2 className="w-3 h-3" /> Thành công
          </Badge>
        );
      case 'PENDING_RETRY':
        return (
          <Badge className="bg-amber-100 dark:bg-amber-950 text-amber-700 dark:text-amber-300 border-amber-300 gap-1 animate-pulse">
            <Clock className="w-3 h-3" /> Chờ thử lại
          </Badge>
        );
      case 'FAILED':
        return (
          <Badge variant="destructive" className="gap-1">
            <AlertTriangle className="w-3 h-3" /> Thất bại
          </Badge>
        );
      case 'CANCELLED':
        return (
          <Badge variant="secondary" className="gap-1 text-muted-foreground">
            <Ban className="w-3 h-3" /> Đã hủy (Khóa thu hồi)
          </Badge>
        );
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-3xl max-h-[85vh] flex flex-col">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary font-semibold mb-1">
            <History className="w-5 h-5 text-primary" />
            <span>Lịch sử gửi thông báo Webhook thu hồi</span>
          </div>
          <DialogTitle className="text-xl">
            Đối tác: {apiKey.partnerName} ({apiKey.keyPrefix}...)
          </DialogTitle>
          <DialogDescription className="text-muted-foreground pt-0.5">
            Xem nhật ký các gói tin thông báo thu hồi, lịch giãn dần và kết quả phản hồi HTTP từ máy chủ đối tác.
          </DialogDescription>
        </DialogHeader>

        {/* Thanh công cụ lọc */}
        <div className="flex items-center justify-between gap-3 py-2 border-b">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-muted-foreground">Lọc trạng thái:</span>
            <Select value={statusFilter} onValueChange={(val) => setStatusFilter(val || 'ALL')}>
              <SelectTrigger className="h-8 text-xs w-44">
                <SelectValue placeholder="Tất cả trạng thái" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tất cả ({totalElements})</SelectItem>
                <SelectItem value="SUCCESS">Thành công</SelectItem>
                <SelectItem value="PENDING_RETRY">Chờ thử lại (Retrying)</SelectItem>
                <SelectItem value="FAILED">Thất bại (Failed)</SelectItem>
                <SelectItem value="CANCELLED">Đã hủy (Cancelled)</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Button
            variant="ghost"
            size="sm"
            onClick={fetchHistory}
            disabled={loading}
            className="h-8 text-xs gap-1"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </Button>
        </div>

        {/* Danh sách bản ghi thông báo */}
        <div className="flex-1 overflow-y-auto space-y-3 py-2 min-h-[300px]">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-16 text-muted-foreground gap-2">
              <Loader2 className="w-6 h-6 animate-spin text-primary" />
              <span className="text-sm">Đang tải lịch sử thông báo...</span>
            </div>
          ) : notifications.length === 0 ? (
            <div className="text-center py-16 text-muted-foreground">
              <History className="w-8 h-8 mx-auto mb-2 opacity-40" />
              <p className="text-sm">Chưa có thông báo thu hồi nào được gửi tới đối tác này.</p>
            </div>
          ) : (
            notifications.map((item) => {
              const isExpanded = expandedId === item.id;
              return (
                <div
                  key={item.id}
                  className="p-3.5 rounded-lg border bg-card hover:bg-muted/20 transition-colors space-y-2 text-xs"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-sm text-foreground">{item.lotCode}</span>
                        <Badge variant="outline" className="text-[11px] font-mono">
                          {item.newStatus === 'RECALLING' ? 'Đang thu hồi' : 'Đã thu hồi'}
                        </Badge>
                        {renderStatusBadge(item.deliveryStatus)}
                      </div>
                      <p className="text-muted-foreground mt-1 line-clamp-1">
                        Lý do: {item.publicReason || 'Theo quyết định thu hồi của đơn vị sản xuất'}
                      </p>
                    </div>

                    <div className="text-right shrink-0 space-y-0.5">
                      <div className="text-muted-foreground">
                        {new Date(item.createdAt).toLocaleString('vi-VN')}
                      </div>
                      <span className="inline-block text-[11px] font-mono px-2 py-0.5 rounded bg-muted">
                        Lần thử: {item.attemptCount}/{item.maxAttempts}
                      </span>
                    </div>
                  </div>

                  {item.nextRetryAt && item.deliveryStatus === 'PENDING_RETRY' && (
                    <div className="flex items-center gap-1 text-[11px] text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 p-1.5 rounded border border-amber-200 dark:border-amber-800">
                      <Clock className="w-3.5 h-3.5 shrink-0" />
                      <span>Thử lại tiếp theo lúc: {new Date(item.nextRetryAt).toLocaleString('vi-VN')}</span>
                    </div>
                  )}

                  {item.lastErrorMessage && item.deliveryStatus !== 'SUCCESS' && (
                    <div className="text-[11px] text-destructive bg-destructive/10 p-2 rounded border border-destructive/20 font-mono">
                      {item.lastErrorMessage}
                    </div>
                  )}

                  {/* Nút xem chi tiết các lần thử gửi (attempts log) */}
                  {item.attempts && item.attempts.length > 0 && (
                    <div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="h-6 px-1.5 text-[11px] text-primary gap-1"
                        onClick={() => setExpandedId(isExpanded ? null : item.id)}
                      >
                        {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
                        <span>{isExpanded ? 'Ẩn chi tiết các lần gửi' : `Xem chi tiết ${item.attempts.length} lần gửi`}</span>
                      </Button>

                      {isExpanded && (
                        <div className="mt-2 space-y-1.5 border-t pt-2 pl-2">
                          {item.attempts.map((att) => (
                            <div
                              key={att.attemptNumber}
                              className="p-2 rounded bg-muted/40 text-[11px] space-y-1 font-mono"
                            >
                              <div className="flex items-center justify-between text-muted-foreground">
                                <span>Lần #{att.attemptNumber} • {new Date(att.attemptedAt).toLocaleTimeString('vi-VN')}</span>
                                <div className="flex items-center gap-2">
                                  {att.httpStatus && (
                                    <span className={att.httpStatus >= 200 && att.httpStatus < 300 ? 'text-emerald-600 font-bold' : 'text-rose-600 font-bold'}>
                                      HTTP {att.httpStatus}
                                    </span>
                                  )}
                                  <span>{att.durationMs}ms</span>
                                </div>
                              </div>
                              {att.errorMessage && (
                                <p className="text-rose-500 break-all">{att.errorMessage}</p>
                              )}
                              {att.responseBody && (
                                <p className="text-muted-foreground truncate max-w-full">
                                  Phản hồi: {att.responseBody}
                                </p>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Phân trang */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-2 border-t text-xs">
            <span className="text-muted-foreground">
              Trang {page + 1} / {totalPages}
            </span>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                className="h-7 px-2 text-xs"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Trước
              </Button>
              <Button
                variant="outline"
                size="sm"
                className="h-7 px-2 text-xs"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                Sau
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default WebhookNotificationHistoryModal;
