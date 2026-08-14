import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ChevronLeft, ChevronRight, RefreshCw, AlertTriangle, Maximize2, Minimize2 } from 'lucide-react';
import { toast } from 'sonner';
import { getFailedLogs } from '@/api/eventValidationApi';
import type { FailedEventLog } from '@/types/eventValidation';
import type { PageResponse } from '@/types/common';

// Ánh xạ loại sự kiện sang tiếng Việt và màu badge
const EVENT_TYPE_CONFIG: Record<string, { label: string; className: string }> = {
  HARVEST: { label: 'Thu hoạch', className: 'bg-lime-100 text-lime-700 border-lime-300' },
  PACKAGING: { label: 'Đóng gói', className: 'bg-sky-100 text-sky-700 border-sky-300' },
  TRANSPORT: { label: 'Vận chuyển', className: 'bg-indigo-100 text-indigo-700 border-indigo-300' },
  PROCUREMENT: { label: 'Thu mua', className: 'bg-amber-100 text-amber-700 border-amber-300' },
  MOBILE: { label: 'Ngoài đồng', className: 'bg-emerald-100 text-emerald-700 border-emerald-300' },
};

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
  const [pageInfo, setPageInfo] = useState<Omit<PageResponse<FailedEventLog>, 'items'>>({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [expandedLogId, setExpandedLogId] = useState<string | null>(null);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const data = await getFailedLogs(page, size);
      setLogs(data.items);
      setPageInfo({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        first: data.first,
        last: data.last,
      });
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải nhật ký lỗi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [page, size]);

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

  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50/50 via-white to-green-50/30 px-4 py-8 md:px-8">
      <div className="mx-auto max-w-7xl space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-amber-100">
              <AlertTriangle className="h-5 w-5 text-amber-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-emerald-800">
                Nhật ký sự kiện bị chặn
              </h1>
              <p className="text-sm text-muted-foreground">
                Các lần ghi sự kiện bị từ chối do sai lô hoặc vi phạm quy tắc
              </p>
            </div>
          </div>
          <Button variant="outline" onClick={fetchLogs} disabled={loading} className="border-emerald-200 hover:bg-emerald-50">
            <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        {/* Card chứa bảng */}
        <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
          <CardHeader className="border-b border-emerald-100 flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-lg font-bold text-emerald-800">
                Danh sách lỗi
              </CardTitle>
              <p className="text-sm text-muted-foreground">
                Tổng số: {pageInfo.totalElements} bản ghi
              </p>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            {loading ? (
              <div className="flex justify-center items-center py-16 text-muted-foreground">
                <RefreshCw className="h-5 w-5 animate-spin mr-2 text-emerald-500" />
                Đang tải dữ liệu...
              </div>
            ) : logs.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
                <AlertTriangle className="h-10 w-10 text-emerald-300 mb-3" />
                <p className="font-semibold text-emerald-800">Chưa có bản ghi lỗi nào</p>
                <p className="text-sm">Hệ thống đang hoạt động ổn định.</p>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow className="bg-emerald-50/50">
                        <TableHead className="text-emerald-800 font-semibold">Thời gian</TableHead>
                        <TableHead className="text-emerald-800 font-semibold">Người thực hiện</TableHead>
                        <TableHead className="text-emerald-800 font-semibold">Loại sự kiện</TableHead>
                        <TableHead className="text-emerald-800 font-semibold">Mã lô</TableHead>
                        <TableHead className="text-emerald-800 font-semibold w-[40%]">Lý do</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {logs.map((log) => (
                        <TableRow key={log.id} className="hover:bg-emerald-50/20 transition-colors">
                          <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                            {formatDate(log.attemptedAt)}
                          </TableCell>
                          <TableCell className="font-medium">{log.userFullName}</TableCell>
                          <TableCell>{getEventTypeBadge(log.eventType)}</TableCell>
                          <TableCell className="font-mono text-sm text-emerald-700">
                            {log.lotCode}
                          </TableCell>
                          <TableCell className="py-3">
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
                      ))}
                    </TableBody>
                  </Table>
                </div>

                {/* Phân trang */}
                <div className="flex items-center justify-between border-t border-emerald-100 px-4 py-3">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <span>Hiển thị</span>
                    <Select
                      value={String(size)}
                      onValueChange={(val) => {
                        setSize(Number(val));
                        setPage(0);
                      }}
                    >
                      <SelectTrigger className="w-[70px] h-8 border-emerald-200">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {[5, 10, 20, 50].map((s) => (
                          <SelectItem key={s} value={String(s)}>
                            {s}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <span>bản ghi</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage(page - 1)}
                      disabled={pageInfo.first}
                      className="border-emerald-200 hover:bg-emerald-50"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <span className="text-sm text-emerald-800 font-medium">
                      Trang {pageInfo.page + 1} / {pageInfo.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage(page + 1)}
                      disabled={pageInfo.last}
                      className="border-emerald-200 hover:bg-emerald-50"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}