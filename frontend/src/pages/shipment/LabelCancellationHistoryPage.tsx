import { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { getLabelCancellationHistory } from '@/api/labelCancellationApi';
import { getShipmentById } from '@/api/shipmentApi';
import type { Shipment, LabelCancellationHistoryItem } from '@/types/shipment';
import {
  Ban,
  Calendar,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  History,
  Loader2,
  RefreshCw,
  Search,
  UserCheck,
} from 'lucide-react';
import { toast } from 'sonner';
import { useSetBreadcrumb } from '@/components/common/AppBreadcrumb';
import { HelpButton } from '@/components/help/HelpButton';

export default function LabelCancellationHistoryPage() {
  const { id: shipmentId, lotId } = useParams<{ id: string; lotId?: string }>();
  const navigate = useNavigate();

  const [shipment, setShipment] = useState<Shipment | null>(null);
  const [historyList, setHistoryList] = useState<LabelCancellationHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterReason, setFilterReason] = useState<string>('ALL');
  const [filterType, setFilterType] = useState<string>('ALL');

  // Pagination state (Max 10 items per page)
  const [currentPage, setCurrentPage] = useState(1);
  const PAGE_SIZE = 10;

  const backUrl = lotId
    ? `/production-lots/${lotId}/shipments/${shipmentId}`
    : `/shipments/${shipmentId}`;

  useSetBreadcrumb(
    shipment
      ? [
        { label: 'Dashboard', href: '/dashboard' },
        { label: 'Lô sản xuất', href: '/production-lots' },
        ...(shipment.productionLotId
          ? [
            {
              label: shipment.productionLotName || 'Chi tiết lô sản xuất',
              href: `/production-lots/${shipment.productionLotId}`,
            },
          ]
          : []),
        { label: shipment.name || 'Chi tiết lô hàng', href: backUrl },
        { label: 'Lịch sử hủy tem in hỏng' },
      ]
      : null,
  );

  const fetchData = async (showRefreshToast = false) => {
    if (!shipmentId) return;
    if (showRefreshToast) setRefreshing(true);
    else setLoading(true);

    try {
      const [shipmentData, historyData] = await Promise.all([
        getShipmentById(shipmentId),
        getLabelCancellationHistory(shipmentId),
      ]);
      setShipment(shipmentData);
      setHistoryList(historyData);
      if (showRefreshToast) {
        toast.success('Đã cập nhật lịch sử mới nhất');
      }
    } catch (err: any) {
      toast.error(err.message || 'Lỗi khi tải nhật ký lịch sử hủy tem');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [shipmentId]);

  // Statistics calculation
  const totalCancelledCount = useMemo(() => {
    return historyList.reduce((acc, curr) => acc + curr.quantity, 0);
  }, [historyList]);

  const totalBatches = historyList.length;

  const filteredHistory = useMemo(() => {
    return historyList.filter((item) => {
      // Reason filter
      if (filterReason !== 'ALL' && item.reasonType !== filterReason) {
        return false;
      }
      // Type filter
      if (filterType !== 'ALL' && item.cancellationType !== filterType) {
        return false;
      }
      // Search term
      if (searchTerm.trim() !== '') {
        const query = searchTerm.toLowerCase();
        const matchesName = item.cancelledByName?.toLowerCase().includes(query);
        const matchesNote = item.reasonNote?.toLowerCase().includes(query);
        const matchesFrom = item.rangeFromCode?.toLowerCase().includes(query);
        const matchesTo = item.rangeToCode?.toLowerCase().includes(query);
        return matchesName || matchesNote || matchesFrom || matchesTo;
      }
      return true;
    });
  }, [historyList, filterReason, filterType, searchTerm]);

  // Reset to page 1 whenever filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, filterReason, filterType]);

  const totalPages = Math.ceil(filteredHistory.length / PAGE_SIZE) || 1;

  const paginatedHistory = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredHistory.slice(start, start + PAGE_SIZE);
  }, [filteredHistory, currentPage]);

  const getReasonLabel = (reasonType: string) => {
    switch (reasonType) {
      case 'PRINT_ERROR':
        return { label: 'In hỏng / mờ / nhòe QR', variant: 'bg-red-50 text-red-700 border-red-200' };
      case 'PRINT_MISALIGNED':
        return { label: 'In lệch / lề cắt viền', variant: 'bg-orange-50 text-orange-700 border-orange-200' };
      case 'PEELED_OFF_DAMAGED':
        return { label: 'Bong tróc / Rách tem', variant: 'bg-amber-50 text-amber-700 border-amber-200' };
      default:
        return { label: 'Lý do khác', variant: 'bg-slate-100 text-slate-700 border-slate-200' };
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  const cancelUrl = lotId
    ? `/production-lots/${lotId}/shipments/${shipmentId}/cancel-labels`
    : `/shipments/${shipmentId}/cancel-labels`;

  if (loading) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-3">
        <Loader2 className="size-8 animate-spin text-emerald-600" />
        <p className="text-sm font-medium text-slate-500">Đang tải nhật ký lịch sử hủy tem...</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-4 md:p-6">

      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900">
              Lịch sử hủy tem in hỏng & hoàn hạn mức
            </h1>
            <Badge variant="outline" className="border-red-200 bg-red-50 text-red-700">
              Lô hàng: {shipment?.name}
            </Badge>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Nhật ký ghi nhận đầy đủ lịch sử các đợt đánh dấu hủy tem hỏng và hoàn trả hạn mức dải mã nguyên tử.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <HelpButton screenKey="label-cancellation-history" />
          <Button
            variant="outline"
            size="sm"
            onClick={() => fetchData(true)}
            disabled={refreshing}
            className="gap-2 text-xs"
          >
            <RefreshCw className={`size-3.5 ${refreshing ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
          <Button
            size="sm"
            onClick={() => navigate(cancelUrl)}
            className="gap-2 bg-red-600 hover:bg-red-700 text-white font-medium text-xs shadow-sm"
          >
            <Ban className="size-3.5" />
            Hủy tem in hỏng
          </Button>
        </div>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card className="border-slate-200 shadow-sm bg-gradient-to-br from-red-50/50 to-white">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500">Tổng tem đã hủy</p>
                <p className="mt-1 text-2xl font-bold text-red-600">
                  -{totalCancelledCount.toLocaleString('vi-VN')} <span className="text-sm font-normal text-red-500">tem</span>
                </p>
              </div>
              <div className="flex size-11 items-center justify-center rounded-xl bg-red-100 text-red-600">
                <Ban className="size-5" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-sm bg-gradient-to-br from-blue-50/50 to-white">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500">Số đợt thao tác hủy</p>
                <p className="mt-1 text-2xl font-bold text-blue-700">
                  {totalBatches} <span className="text-sm font-normal text-blue-500">đợt</span>
                </p>
              </div>
              <div className="flex size-11 items-center justify-center rounded-xl bg-blue-100 text-blue-600">
                <History className="size-5" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-sm bg-gradient-to-br from-emerald-50/50 to-white">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500">Hạn mức dải mã đã hoàn</p>
                <p className="mt-1 text-2xl font-bold text-emerald-600">
                  +{totalCancelledCount.toLocaleString('vi-VN')} <span className="text-sm font-normal text-emerald-500">mã</span>
                </p>
              </div>
              <div className="flex size-11 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
                <CheckCircle2 className="size-5" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Table Card */}
      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="border-b border-slate-100 bg-slate-50/50 pb-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <CardTitle className="text-sm font-bold text-slate-800 flex items-center gap-2">
              <History className="size-4 text-slate-500" />
              Danh sách chi tiết lịch sử đợt hủy tem ({filteredHistory.length})
            </CardTitle>

            {/* Filter Tools */}
            <div className="flex flex-wrap items-center gap-2">
              <div className="relative min-w-[220px]">
                <Search className="absolute left-2.5 top-2.5 size-3.5 text-slate-400" />
                <Input
                  type="text"
                  placeholder="Tìm theo mã, ghi chú, người hủy..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="h-8 pl-8 text-xs bg-white"
                />
              </div>

              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="h-8 rounded-md border border-slate-200 bg-white px-2.5 text-xs text-slate-700 focus:border-emerald-500 focus:outline-none"
              >
                <option value="ALL">Tất cả phương thức</option>
                <option value="RANGE">Theo khoảng mã (Range)</option>
                <option value="SINGLE">Nhập / Quét mã lẻ (Single)</option>
              </select>

              <select
                value={filterReason}
                onChange={(e) => setFilterReason(e.target.value)}
                className="h-8 rounded-md border border-slate-200 bg-white px-2.5 text-xs text-slate-700 focus:border-emerald-500 focus:outline-none"
              >
                <option value="ALL">Tất cả lý do</option>
                <option value="PRINT_ERROR">Tem in hỏng/nhòe QR</option>
                <option value="PRINT_MISALIGNED">Tem in lệch viền</option>
                <option value="PEELED_OFF_DAMAGED">Tem rách/bong tróc</option>
                <option value="OTHER">Lý do khác</option>
              </select>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {filteredHistory.length === 0 ? (
            <div className="flex min-h-[300px] flex-col items-center justify-center p-8 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
                <Ban className="size-6" />
              </div>
              <h3 className="mt-3 text-sm font-semibold text-slate-800">
                Chưa có dữ liệu lịch sử hủy tem
              </h3>
              <p className="mt-1 text-xs text-slate-500 max-w-sm">
                {searchTerm || filterReason !== 'ALL' || filterType !== 'ALL'
                  ? 'Không tìm thấy đợt hủy tem nào phù hợp với bộ lọc tìm kiếm.'
                  : 'Lô hàng này hiện chưa thực hiện bất kỳ thao tác hủy tem in hỏng nào.'}
              </p>
              {(searchTerm || filterReason !== 'ALL' || filterType !== 'ALL') && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setSearchTerm('');
                    setFilterReason('ALL');
                    setFilterType('ALL');
                  }}
                  className="mt-4 text-xs"
                >
                  Xóa bộ lọc
                </Button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
                    <TableHead className="w-12 text-center text-xs font-semibold text-slate-600">STT</TableHead>
                    <TableHead className="text-xs font-semibold text-slate-600">Thời gian thực hiện</TableHead>
                    <TableHead className="text-xs font-semibold text-slate-600">Người thực hiện</TableHead>
                    <TableHead className="text-center text-xs font-semibold text-slate-600">Số lượng tem</TableHead>
                    <TableHead className="text-xs font-semibold text-slate-600">Phương thức / Khoảng mã tem</TableHead>
                    <TableHead className="text-xs font-semibold text-slate-600">Lý do hủy & Ghi chú</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedHistory.map((item, index) => {
                    const reasonInfo = getReasonLabel(item.reasonType);
                    const stt = (currentPage - 1) * PAGE_SIZE + index + 1;
                    return (
                      <TableRow key={item.id} className="hover:bg-slate-50/50">
                        <TableCell className="text-center text-xs font-medium text-slate-500">
                          {stt}
                        </TableCell>

                        <TableCell className="text-xs font-medium text-slate-800 whitespace-nowrap">
                          <div className="flex items-center gap-1.5">
                            <Calendar className="size-3.5 text-slate-400" />
                            {formatDate(item.cancelledAt)}
                          </div>
                        </TableCell>

                        <TableCell className="text-xs text-slate-700">
                          <div className="flex items-center gap-1.5">
                            <UserCheck className="size-3.5 text-emerald-600" />
                            <span className="font-semibold">{item.cancelledByName || 'Tài khoản hệ thống'}</span>
                          </div>
                        </TableCell>

                        <TableCell className="text-center">
                          <span className="inline-flex items-center rounded-md bg-red-50 px-2.5 py-1 text-xs font-bold text-red-700 ring-1 ring-inset ring-red-600/10">
                            -{item.quantity} tem
                          </span>
                        </TableCell>

                        <TableCell className="text-xs">
                          {item.cancellationType === 'RANGE' ? (
                            <div className="flex items-center gap-1 font-mono text-slate-800">
                              <Badge variant="outline" className="bg-slate-50 text-[10px] border-slate-200">
                                Khoảng
                              </Badge>
                              <span className="rounded bg-slate-100 px-1.5 py-0.5 font-semibold text-slate-900">
                                {item.rangeFromCode}
                              </span>
                              <span className="text-slate-400">→</span>
                              <span className="rounded bg-slate-100 px-1.5 py-0.5 font-semibold text-slate-900">
                                {item.rangeToCode}
                              </span>
                            </div>
                          ) : (
                            <div className="flex items-center gap-1">
                              <Badge variant="outline" className="bg-slate-50 text-[10px] border-slate-200">
                                Mã lẻ
                              </Badge>
                              <span className="text-slate-600">Quét / Nhập danh sách mã đơn</span>
                            </div>
                          )}
                        </TableCell>

                        <TableCell className="text-xs">
                          <div className="space-y-1">
                            <span
                              className={`inline-block rounded border px-2 py-0.5 text-[11px] font-semibold ${reasonInfo.variant}`}
                            >
                              {reasonInfo.label}
                            </span>
                            {item.reasonNote && (
                              <p className="text-slate-600 italic text-[11px]">
                                "{item.reasonNote}"
                              </p>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}

          {/* Pagination Footer */}
          {filteredHistory.length > 0 && (
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-t border-slate-100 px-5 py-3.5 text-xs text-slate-600">
              <div>
                Hiển thị <span className="font-semibold text-slate-900">{(currentPage - 1) * PAGE_SIZE + 1}</span> -{' '}
                <span className="font-semibold text-slate-900">
                  {Math.min(currentPage * PAGE_SIZE, filteredHistory.length)}
                </span>{' '}
                trong tổng số <span className="font-semibold text-slate-900">{filteredHistory.length}</span> đợt hủy tem
              </div>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}
                  className="h-8 gap-1 text-xs"
                >
                  <ChevronLeft className="size-3.5" />
                  Trang trước
                </Button>

                <span className="px-2 font-medium text-slate-700">
                  Trang <strong className="text-slate-900">{currentPage}</strong> / {totalPages}
                </span>

                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                  className="h-8 gap-1 text-xs"
                >
                  Trang sau
                  <ChevronRight className="size-3.5" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
