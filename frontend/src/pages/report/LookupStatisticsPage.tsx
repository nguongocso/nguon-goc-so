import { useEffect, useState, useMemo } from 'react';
import {
  getLookupStatistics,
  getAbnormalScans,
} from '@/api/lookupStatisticsApi';
import type {
  LookupStatisticsResponse,
  AbnormalScanResponse,
} from '@/types/lookupStatistics';
import { StatisticsSummary } from '@/components/report/StatisticsSummary';
import { LocationChart } from '@/components/report/LocationChart';
import { LotStatsTable } from '@/components/report/LotStatsTable';
import { TimeSeriesChart } from '@/components/report/TimeSeriesChart';
import { AbnormalScansTable } from '@/components/report/AbnormalScansTable';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Card, CardContent } from '@/components/ui/card';
import { RefreshCw, Calendar } from 'lucide-react';

type GroupByType = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

const groupByLabels: Record<GroupByType, string> = {
  DAY: 'Ngày',
  WEEK: 'Tuần',
  MONTH: 'Tháng',
  YEAR: 'Năm',
};

// Helper: lấy ngày hôm nay (YYYY-MM-DD)
const getToday = () => new Date().toISOString().split('T')[0];

// Helper: lấy ngày đầu tháng hiện tại (YYYY-MM-DD)
const getFirstDayOfMonth = () => {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1)
    .toISOString()
    .split('T')[0];
};

// Helper: lấy ngày 7 ngày trước
const getSevenDaysAgo = () => {
  const now = new Date();
  now.setDate(now.getDate() - 7);
  return now.toISOString().split('T')[0];
};

export default function LookupStatisticsPage() {
  const [stats, setStats] = useState<LookupStatisticsResponse | null>(null);
  const [abnormalScans, setAbnormalScans] = useState<AbnormalScanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [abnormalLoading, setAbnormalLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // === Bộ lọc ngày mặc định: từ ngày = đầu tháng, đến ngày = hôm nay ===
  const defaultStartDate = useMemo(() => getFirstDayOfMonth(), []);
  const defaultEndDate = useMemo(() => getToday(), []);

  const [startDate, setStartDate] = useState(defaultStartDate);
  const [endDate, setEndDate] = useState(defaultEndDate);
  const [groupBy, setGroupBy] = useState<GroupByType>('DAY');

  // === Xác định preset nào đang active ===
  const activePreset = useMemo(() => {
    const today = getToday();
    const firstDay = getFirstDayOfMonth();
    const sevenDaysAgo = getSevenDaysAgo();

    if (startDate === today && endDate === today) return 'today';
    if (startDate === sevenDaysAgo && endDate === today) return 'week';
    if (startDate === firstDay && endDate === today) return 'month';
    return null;
  }, [startDate, endDate]);

  const fetchStats = async () => {
    try {
      setLoading(true);
      const data = await getLookupStatistics({
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        groupBy,
      });
      setStats(data);
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Không thể tải thống kê';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const fetchAbnormalScans = async () => {
    try {
      setAbnormalLoading(true);
      const data = await getAbnormalScans({
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: 10,
      });
      setAbnormalScans(data.content);
      setTotalPages(data.pageable.totalPages);
    } catch (error: any) {
      const msg = error.response?.data?.message || 'Không thể tải danh sách bất thường';
      toast.error(msg);
    } finally {
      setAbnormalLoading(false);
    }
  };

  // Tự động fetch khi thay đổi bộ lọc
  useEffect(() => {
    fetchStats();
  }, [startDate, endDate, groupBy]);

  useEffect(() => {
    fetchAbnormalScans();
  }, [page, startDate, endDate]);

  // === Các action nhanh cho bộ lọc ngày ===
  const setToday = () => {
    const today = getToday();
    setStartDate(today);
    setEndDate(today);
  };

  const setThisWeek = () => {
    const today = getToday();
    const sevenDaysAgo = getSevenDaysAgo();
    setStartDate(sevenDaysAgo);
    setEndDate(today);
  };

  const setThisMonth = () => {
    const today = getToday();
    const firstDay = getFirstDayOfMonth();
    setStartDate(firstDay);
    setEndDate(today);
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Thống kê lượt tra cứu</h1>
          <p className="text-sm text-muted-foreground">
            Tổng hợp số lượt quét mã theo lô và thời gian
          </p>
        </div>
        <Button variant="outline" onClick={fetchStats} disabled={loading}>
          <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      {/* Bộ lọc */}
      <Card className="border-emerald-100 bg-white/80 backdrop-blur-sm shadow-sm">
        <CardContent className="p-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Từ ngày */}
            <div className="space-y-1.5">
              <Label htmlFor="startDate" className="text-sm font-medium text-emerald-800">
                Từ ngày
              </Label>
              <div className="relative">
                <Input
                  id="startDate"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="pl-9 border-emerald-200 focus-visible:ring-emerald-100"
                />
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>

            {/* Đến ngày */}
            <div className="space-y-1.5">
              <Label htmlFor="endDate" className="text-sm font-medium text-emerald-800">
                Đến ngày
              </Label>
              <div className="relative">
                <Input
                  id="endDate"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="pl-9 border-emerald-200 focus-visible:ring-emerald-100"
                />
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>

            {/* Nhóm theo */}
            <div className="space-y-1.5">
              <Label htmlFor="groupBy" className="text-sm font-medium text-emerald-800">
                Nhóm theo
              </Label>
              <Select
                value={groupBy}
                onValueChange={(value: string | null) => {
                  if (value) setGroupBy(value as GroupByType);
                }}
              >
                <SelectTrigger className="border-emerald-200 focus:ring-emerald-100">
                  <SelectValue placeholder="Chọn nhóm">
                    {groupByLabels[groupBy]}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(groupByLabels).map(([key, label]) => (
                    <SelectItem key={key} value={key}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Nút chọn nhanh */}
          <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-emerald-100">
            <span className="text-sm font-medium text-muted-foreground mr-1">
              Chọn nhanh:
            </span>
            <Button
              type="button"
              variant={activePreset === 'today' ? 'default' : 'outline'}
              size="sm"
              onClick={setToday}
              className="text-xs h-8"
            >
              Hôm nay
            </Button>
            <Button
              type="button"
              variant={activePreset === 'week' ? 'default' : 'outline'}
              size="sm"
              onClick={setThisWeek}
              className="text-xs h-8"
            >
              7 ngày qua
            </Button>
            <Button
              type="button"
              variant={activePreset === 'month' ? 'default' : 'outline'}
              size="sm"
              onClick={setThisMonth}
              className="text-xs h-8"
            >
              Tháng này
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setStartDate('');
                setEndDate('');
                setGroupBy('DAY');
              }}
              className="text-xs h-8"
            >
              Xóa bộ lọc
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Nội dung dữ liệu */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        </div>
      ) : stats ? (
        <>
          <StatisticsSummary stats={stats.summary} />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <LocationChart data={stats.byLocation} />
            <TimeSeriesChart data={stats.timeSeries} />
          </div>

          <LotStatsTable data={stats.byProductionLot} />

          <AbnormalScansTable
            data={abnormalScans}
            totalPages={totalPages}
            currentPage={page}
            onPageChange={setPage}
            loading={abnormalLoading}
          />
        </>
      ) : (
        <div className="text-center py-12 text-muted-foreground">
          <p>Không có dữ liệu thống kê</p>
        </div>
      )}
    </div>
  );
}