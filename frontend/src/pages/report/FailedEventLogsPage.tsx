import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ChevronLeft, ChevronRight, AlertTriangle, Maximize2, Minimize2 } from 'lucide-react';
import { TableCell, TableHead, TableRow } from '@/components/ui/table';
import { HelpButton } from '@/components/help/HelpButton';
import { toast } from 'sonner';
import { ListPageHeader } from '@/components/common/ListPageHeader';
import { ListCard } from '@/components/common/ListCard';
import { ListToolbar } from '@/components/common/ListToolbar';
import { SearchInput } from '@/components/common/SearchInput';
import { FilterSelect } from '@/components/common/FilterSelect';
import { RefreshButton } from '@/components/common/RefreshButton';
import { DataTableShell } from '@/components/common/DataTableShell';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { getFailedLogs } from '@/api/eventValidationApi';
import type { FailedEventLog } from '@/types/eventValidation';

const EVENT_TYPE_CONFIG: Record<string, { label: string; className: string }> = {
  HARVEST: { label: 'Thu hoạch', className: 'bg-lime-100 text-lime-700 border-lime-300' },
  PREPROCESSING: { label: 'Sơ chế, phân loại', className: 'bg-teal-100 text-teal-700 border-teal-300' },
  PACKAGING: { label: 'Đóng gói', className: 'bg-sky-100 text-sky-700 border-sky-300' },
  TRANSPORT: { label: 'Vận chuyển', className: 'bg-indigo-100 text-indigo-700 border-indigo-300' },
  PROCUREMENT: { label: 'Thu mua', className: 'bg-amber-100 text-amber-700 border-amber-300' },
  CORRECTION: { label: 'Sửa lỗi', className: 'bg-rose-100 text-rose-700 border-rose-300' },
  WAREHOUSE_RECEIPT: { label: 'Nhập kho', className: 'bg-violet-100 text-violet-700 border-violet-300' },
  STORAGE_CONDITION: { label: 'Theo dõi bảo quản', className: 'bg-emerald-100 text-emerald-700 border-emerald-300' },
};

const EVENT_TYPE_OPTIONS = [
  { value: 'ALL', label: 'Tất cả loại sự kiện' },
  ...Object.entries(EVENT_TYPE_CONFIG).map(([value, cfg]) => ({
    value,
    label: cfg.label,
  })),
];

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];
const DEFAULT_PAGE_SIZE = 10;

const formatDate = (iso: string) => {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return iso;
  }
};

export default function FailedEventLogsPage() {
  const [logs, setLogs] = useState<FailedEventLog[]>([]);
  const [loading, setLoading] = useState(true);

  const [search, setSearch] = useState('');
  const [eventType, setEventType] = useState('ALL');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE);
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null);

  useSetBreadcrumb([
    { label: 'Tổng quan', href: '/dashboard' },
    { label: 'Nhật ký sự kiện bị chặn' },
  ]);

  const fetchLogs = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getFailedLogs(0, 1000);
      setLogs(data.items);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải nhật ký lỗi');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return logs.filter((log) => {
      const matchKeyword =
        !q ||
        log.userFullName.toLowerCase().includes(q) ||
        log.lotCode.toLowerCase().includes(q) ||
        log.failureReason.toLowerCase().includes(q);
      const matchType = eventType === 'ALL' || log.eventType === eventType;
      return matchKeyword && matchType;
    });
  }, [logs, search, eventType]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / size));
  const safePage = Math.min(page, totalPages - 1);
  const paginated = useMemo(() => {
    const start = safePage * size;
    return filtered.slice(start, start + size);
  }, [filtered, safePage, size]);

  const toggleExpand = (logId: string) => {
    setExpandedLogId(expandedLogId === logId ? null : logId);
  };

  const getEventTypeBadge = (type: string) => {
    const config = EVENT_TYPE_CONFIG[type] || { label: type, className: 'bg-gray-100 text-gray-700 border-gray-300' };
    return (
      <Badge variant="outline" className={`${config.className} border text-xs font-semibold`}>
        {config.label}
      </Badge>
    );
  };

  const header = (
    <>
      <TableHead className="w-12 text-center">STT</TableHead>
      <TableHead>Thời gian</TableHead>
      <TableHead>Người thực hiện</TableHead>
      <TableHead>Loại sự kiện</TableHead>
      <TableHead>Mã lô</TableHead>
      <TableHead className="w-[40%]">Lý do</TableHead>
    </>
  );

  const body = paginated.map((log, index) => (
    <TableRow key={log.id} className="hover:bg-muted/40 transition-colors">
      <TableCell className="text-center font-medium text-muted-foreground">
        {safePage * size + index + 1}
      </TableCell>
      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
        {formatDate(log.attemptedAt)}
      </TableCell>
      <TableCell className="font-medium">{log.userFullName}</TableCell>
      <TableCell>{getEventTypeBadge(log.eventType)}</TableCell>
      <TableCell className="font-mono text-sm text-emerald-700">{log.lotCode}</TableCell>
      <TableCell>
        <div
          className={`relative group cursor-pointer rounded-md p-2 transition-colors ${
            expandedLogId === log.id
              ? 'bg-amber-50 border border-amber-200'
              : 'hover:bg-amber-50/50 border border-transparent'
          }`}
          onClick={() => toggleExpand(log.id)}
          title="Bấm để xem đầy đủ"
        >
          <div className={`${expandedLogId === log.id ? '' : 'line-clamp-2'} text-sm text-amber-800`}>
            {log.failureReason}
          </div>
          <div className="flex justify-end mt-1">
            {expandedLogId === log.id ? (
              <Minimize2 className="h-3 w-3 text-amber-500" />
            ) : (
              <Maximize2 className="h-3 w-3 text-amber-400 opacity-0 group-hover:opacity-100 transition-opacity" />
            )}
          </div>
        </div>
      </TableCell>
    </TableRow>
  ));

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={AlertTriangle}
        iconBoxClassName="bg-amber-100 text-amber-600 dark:text-amber-400"
        title="Nhật ký sự kiện bị chặn"
        description="Các lần ghi sự kiện bị từ chối do sai lô hoặc vi phạm quy tắc"
        actions={<HelpButton screenKey="report-failed-events" />}
      />

      <ListCard>
        <ListToolbar
          left={
            <>
              <SearchInput
                placeholder="Tìm theo người thực hiện, mã lô hoặc lý do..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
              <FilterSelect
                value={eventType}
                onValueChange={(val) => {
                  setEventType(val || 'ALL');
                  setPage(0);
                }}
                options={EVENT_TYPE_OPTIONS}
              />
            </>
          }
          right={<RefreshButton onClick={fetchLogs} loading={loading} />}
        />

        <DataTableShell
          header={header}
          body={body}
          loading={loading}
          empty={!loading && filtered.length === 0}
          colSpan={6}
          loadingMessage="Đang tải danh sách nhật ký lỗi..."
          emptyMessage={
            search || eventType !== 'ALL'
              ? 'Không tìm thấy nhật ký lỗi nào phù hợp với bộ lọc.'
              : 'Chưa có bản ghi lỗi nào. Hệ thống đang hoạt động ổn định.'
          }
        />

        {!loading && filtered.length > 0 && (
          <div className="flex flex-col gap-3 pt-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>Hiển thị</span>
              <Select
                value={String(size)}
                onValueChange={(val) => {
                  if (!val) return;
                  setSize(Number(val));
                  setPage(0);
                }}
              >
                <SelectTrigger className="h-8 w-20">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAGE_SIZE_OPTIONS.map((option) => (
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
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                disabled={safePage === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="min-w-28 text-center text-sm">
                Trang {safePage + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                disabled={safePage >= totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </ListCard>
    </div>
  );
}
