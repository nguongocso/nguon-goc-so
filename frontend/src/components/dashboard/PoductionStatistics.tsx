import { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { DashboardResponse } from '@/api/productionLotApi';

interface ProductionStatisticsProps {
  data: DashboardResponse | null;
  isLoading?: boolean;
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#FF6B6B', '#845EC2'];

const statusLabels: Record<string, string> = {
  DRAFT: 'Bản nháp',
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  HARVESTED: 'Đã thu hoạch',
  PACKAGED: 'Đã đóng gói',
  CLOSED: 'Đã kết thúc',
};

export const ProductionStatistics = ({ data, isLoading = false }: ProductionStatisticsProps) => {
  // Chuyển byStatus object sang mảng cho PieChart
  const statusStats = useMemo(() => {
    if (!data?.byStatus) return [];
    return Object.entries(data.byStatus)
      .map(([key, value]) => ({
        name: statusLabels[key] || key,
        value: value,
      }))
      .filter((item) => item.value > 0);
  }, [data]);

  // timeSeries đã có sẵn từ API
  const timeSeries = data?.timeSeries || [];

  const summary = data?.summary || { totalLots: 0, totalExpectedYield: 0, totalActualYield: 0 };

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Đang tải thống kê...</CardTitle>
          </CardHeader>
        </Card>
      </div>
    );
  }

  // Nếu không có dữ liệu hoặc dữ liệu rỗng
  if (!data || summary.totalLots === 0) {
    return (
      <Card>
        <CardContent className="py-12 text-center text-muted-foreground">
          <p>Chưa có dữ liệu để hiển thị thống kê.</p>
        </CardContent>
      </Card>
    );
  }

  // Định dạng số
  const formatNumber = (num: number) => num.toFixed(1);

  return (
    <div className="space-y-6">
      {/* Thẻ thống kê nhanh */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground">Tổng số lô</p>
            <p className="text-3xl font-bold">{summary.totalLots}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground">Tổng sản lượng dự kiến</p>
            <p className="text-3xl font-bold">{formatNumber(summary.totalExpectedYield)} kg</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground">Sản lượng trung bình/lô</p>
            <p className="text-3xl font-bold">
              {summary.totalLots > 0 ? formatNumber(summary.totalExpectedYield / summary.totalLots) : 0} kg
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground">Tổng sản lượng thực tế</p>
            <p className="text-3xl font-bold">{formatNumber(summary.totalActualYield)} kg</p>
          </CardContent>
        </Card>
      </div>

      {/* Biểu đồ */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {/* Biểu đồ cột: số lô theo tháng */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Số lô theo tháng</CardTitle>
          </CardHeader>
          <CardContent>
            {timeSeries.length > 0 ? (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={timeSeries}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="period" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="lotCount" fill="#0088FE" name="Số lô" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-center text-muted-foreground py-8">Chưa có dữ liệu theo tháng</p>
            )}
          </CardContent>
        </Card>

        {/* Biểu đồ cột: sản lượng theo tháng */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Sản lượng (kg) theo tháng</CardTitle>
          </CardHeader>
          <CardContent>
            {timeSeries.length > 0 ? (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={timeSeries}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="period" />
                  <YAxis />
                  <Tooltip formatter={(value) => `${Number(value ?? 0).toFixed(1)} kg`} />
                  <Legend />
                  <Bar dataKey="expectedYield" fill="#00C49F" name="Dự kiến" />
                  <Bar dataKey="actualYield" fill="#FFBB28" name="Thực tế" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-center text-muted-foreground py-8">Chưa có dữ liệu theo tháng</p>
            )}
          </CardContent>
        </Card>

        {/* Biểu đồ tròn: phân bố trạng thái */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Phân bố trạng thái lô</CardTitle>
          </CardHeader>
          <CardContent className="flex justify-center">
            {statusStats.length > 0 ? (
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={statusStats}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`}
                    outerRadius={70}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {statusStats.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-muted-foreground">Chưa có dữ liệu trạng thái</p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};