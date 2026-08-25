import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { ChevronLeft, ChevronRight, Eye, RefreshCcw } from 'lucide-react';
import { HelpButton } from '@/components/help/HelpButton';
import { getRecallRequests } from '@/api/recallApi';
import type {
  PageResponse,
  RecallRequest,
  RecallRequestStatus,
} from '@/types/recallRequest';

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Chờ duyệt', className: 'bg-yellow-100 text-yellow-800' },
  APPROVED: { label: 'Đã duyệt', className: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Đã từ chối', className: 'bg-red-100 text-red-700' },
};

export const RecallRequestListPage = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<PageResponse<RecallRequest> | null>(null);
  const [status, setStatus] = useState<RecallRequestStatus>('PENDING');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const result = await getRecallRequests({
        status,
        page,
        size: 10,
      });
      setData(result);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Không thể tải danh sách yêu cầu');
    } finally {
      setLoading(false);
    }
  }, [status, page]);

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

  return (
    <div className="space-y-6">
      <Card className="border-slate-200 bg-white shadow-sm rounded-xl">
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-xl font-bold text-slate-900">
            Yêu cầu thu hồi lô sản xuất
          </CardTitle>
          <div className="flex items-center gap-2">
            <HelpButton screenKey="recall-request-list" />
            <Button variant="outline" onClick={() => load()} disabled={loading}>
              <RefreshCcw className="size-4 mr-1" /> Làm mới
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="mb-4 flex items-center gap-2">
            {(['PENDING', 'APPROVED', 'REJECTED'] as RecallRequestStatus[]).map(
              (s) => (
                <Button
                  key={s}
                  variant={status === s ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setStatus(s);
                    setPage(0);
                  }}
                >
                  {STATUS_MAP[s].label}
                </Button>
              ),
            )}
          </div>

          {loading ? (
            <div className="py-8 text-center text-muted-foreground">Đang tải...</div>
          ) : !data || data.items.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">
              Không có yêu cầu thu hồi nào.
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50">
                      <TableHead className="font-semibold text-slate-700">Lô sản xuất</TableHead>
                      <TableHead className="font-semibold text-slate-700">Người yêu cầu</TableHead>
                      <TableHead className="font-semibold text-slate-700">Thời điểm</TableHead>
                      <TableHead className="font-semibold text-slate-700">Lý do</TableHead>
                      <TableHead className="font-semibold text-slate-700">Trạng thái</TableHead>
                      <TableHead className="font-semibold text-slate-700 text-center">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.items.map((item) => (
                      <TableRow key={item.id} className="hover:bg-slate-50/80">
                        <TableCell className="font-medium">{item.lotName}</TableCell>
                        <TableCell>{item.requestedBy?.fullName || '—'}</TableCell>
                        <TableCell>{formatDate(item.requestedAt)}</TableCell>
                        <TableCell className="max-w-[240px] truncate">
                          {item.reason}
                        </TableCell>
                        <TableCell>
                          <span
                            className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                              STATUS_MAP[item.status]?.className ||
                              'bg-gray-100 text-gray-700'
                            }`}
                          >
                            {STATUS_MAP[item.status]?.label || item.status}
                          </span>
                        </TableCell>
                        <TableCell className="text-center">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => navigate(`/recall-requests/${item.id}`)}
                          >
                            <Eye className="size-4 mr-1" /> Chi tiết
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {data.totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    <ChevronLeft className="size-4" /> Trước
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    Trang {data.page + 1} / {data.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Sau <ChevronRight className="size-4" />
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
};