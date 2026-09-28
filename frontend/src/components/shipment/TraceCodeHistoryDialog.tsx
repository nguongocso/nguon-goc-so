import { useEffect, useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  AlertTriangle,
  BadgeCheck,
  Ban,
  Calendar,
  Eye,
  Lock,
  Package,
  Printer,
  RefreshCw,
  Unlock,
  User,
} from 'lucide-react';
import { getTraceCodeHistory } from '@/api/traceCodeApi';
import type { HistoryEvent, TraceCodeHistory } from '@/types/traceCode';
import { TraceCodeStatusBadge } from './TraceCodeStatusBadge';

interface TraceCodeHistoryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  codeValue: string | null;
}

const formatDateTime = (value: string) => {
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('vi-VN');
};

const getEventIcon = (type: string) => {
  switch (type) {
    case 'CREATED':
      return <Package className="h-4 w-4 text-blue-600" />;
    case 'PRINTED':
      return <Printer className="h-4 w-4 text-indigo-600" />;
    case 'ACTIVATED':
      return <BadgeCheck className="h-4 w-4 text-emerald-600" />;
    case 'LOCKED':
      return <Lock className="h-4 w-4 text-rose-600" />;
    case 'UNLOCKED':
      return <Unlock className="h-4 w-4 text-emerald-600" />;
    case 'CANCELLED':
      return <Ban className="h-4 w-4 text-slate-500" />;
    case 'RECALLED':
      return <AlertTriangle className="h-4 w-4 text-amber-600" />;
    case 'SCANNED':
      return <Eye className="h-4 w-4 text-cyan-600" />;
    default:
      return <Calendar className="h-4 w-4 text-slate-500" />;
  }
};

const getEventTypeBadgeLabel = (type: string) => {
  switch (type) {
    case 'CREATED':
      return 'Khởi tạo';
    case 'PRINTED':
      return 'In tem';
    case 'ACTIVATED':
      return 'Kích hoạt';
    case 'LOCKED':
      return 'Khóa tem';
    case 'UNLOCKED':
      return 'Mở khóa';
    case 'CANCELLED':
      return 'Hủy tem';
    case 'RECALLED':
      return 'Thu hồi';
    case 'SCANNED':
      return 'Quét tra cứu';
    default:
      return type;
  }
};

export function TraceCodeHistoryDialog({
  open,
  onOpenChange,
  codeValue,
}: TraceCodeHistoryDialogProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<TraceCodeHistory | null>(null);

  useEffect(() => {
    if (!open || !codeValue) {
      setData(null);
      setError(null);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError(null);

    getTraceCodeHistory(codeValue)
      .then((res) => {
        if (isMounted) {
          setData(res);
        }
      })
      .catch((err: any) => {
        if (isMounted) {
          setError(
            err?.response?.data?.message ||
              'Không thể tải lịch sử mã tem. Vui lòng thử lại.',
          );
        }
      })
      .finally(() => {
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [open, codeValue]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col p-6">
        <DialogHeader>
          <div className="flex items-center justify-between gap-3 pr-6">
            <div>
              <DialogTitle className="text-xl font-bold flex items-center gap-2">
                Lịch sử mã tem:
                <span className="font-mono text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                  {codeValue}
                </span>
              </DialogTitle>
              {data && (
                <p className="text-xs text-muted-foreground mt-1">
                  Lô hàng: <span className="font-medium text-foreground">{data.shipmentName}</span>
                  {' • '}
                  Tổng lượt quét: <span className="font-semibold text-emerald-700">{data.scanCount}</span>
                </p>
              )}
            </div>
            {data && <TraceCodeStatusBadge status={data.status} />}
          </div>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto pr-2 mt-4 space-y-4">
          {loading && (
            <div className="flex flex-col items-center justify-center h-48 text-muted-foreground gap-2">
              <RefreshCw className="h-6 w-6 animate-spin text-emerald-600" />
              <p className="text-sm">Đang tải lịch sử vòng đời mã tem...</p>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-sm">
              <p className="font-medium">Lỗi tải dữ liệu</p>
              <p className="mt-1">{error}</p>
              <Button
                variant="outline"
                size="sm"
                className="mt-3 border-rose-300 hover:bg-rose-100"
                onClick={() => {
                  if (codeValue) {
                    setLoading(true);
                    setError(null);
                    getTraceCodeHistory(codeValue)
                      .then(setData)
                      .catch((e: any) => setError(e?.response?.data?.message || 'Lỗi tải dữ liệu'))
                      .finally(() => setLoading(false));
                  }
                }}
              >
                Thử lại
              </Button>
            </div>
          )}

          {!loading && !error && data && data.events.length === 0 && (
            <div className="text-center py-12 text-muted-foreground text-sm">
              Chưa có sự kiện nào được ghi nhận cho mã tem này.
            </div>
          )}

          {!loading && !error && data && data.events.length > 0 && (
            <div className="relative pl-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200 space-y-6">
              {data.events.map((event: HistoryEvent, idx: number) => (
                <div key={idx} className="relative group">
                  {/* Timeline Dot */}
                  <div className="absolute -left-6 top-1 flex items-center justify-center w-5 h-5 rounded-full bg-white border-2 border-slate-300 group-hover:border-emerald-500 shadow-sm">
                    {getEventIcon(event.type)}
                  </div>

                  {/* Event Content */}
                  <div className="bg-slate-50 border border-slate-200 rounded-lg p-3.5 hover:border-slate-300 transition-colors">
                    <div className="flex flex-wrap items-center justify-between gap-2 mb-1.5">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-slate-200 text-slate-800">
                          {getEventTypeBadgeLabel(event.type)}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {formatDateTime(event.timestamp)}
                        </span>
                      </div>
                      {event.actorName && (
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <User className="h-3 w-3" />
                          {event.actorName}
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-foreground font-medium whitespace-pre-wrap">
                      {event.details}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="pt-4 border-t flex justify-end">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Đóng
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
