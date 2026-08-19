import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Clock3, FileText, PackageOpen } from 'lucide-react';
import { toast } from 'sonner';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent } from '@/components/ui/card';
import { getProductionLots, getProductionLotDashboard, type DashboardResponse } from '@/api/productionLotApi';
import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';
import { ProductionStatistics } from '@/components/dashboard/PoductionStatistics';
import type { ProductionLot } from '@/types/productionLot';
import LookupStatisticsPage from '@/pages/report/LookupStatisticsPage';

interface CooperativeDashboardProps {
  initialTab?: string | null;
}

export function CooperativeDashboard({ initialTab }: CooperativeDashboardProps) {
  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [dashboardData, setDashboardData] = useState<DashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        setIsLoading(true);
        // Gọi song song: danh sách lô cho thẻ thống kê và dữ liệu cho biểu đồ
        const [lots, dashData] = await Promise.all([
          getProductionLots(),
          getProductionLotDashboard()
        ]);
        setProductionLots(lots);
        setDashboardData(dashData);
      } catch {
        toast.error('Không thể tải dữ liệu bảng điều khiển');
      } finally {
        setIsLoading(false);
      }
    };
    void loadData();
  }, []);

  const statistics = useMemo(() => ({
    total: productionLots.length,
    draft: productionLots.filter((lot) => lot.status === 'DRAFT').length,
    pending: productionLots.filter((lot) => lot.status === 'PENDING').length,
    approved: productionLots.filter((lot) => lot.status === 'APPROVED').length,
  }), [productionLots]);

  const defaultTab = initialTab === 'lookup-stats' ? 'lookup-stats' : 'overview';

  const cards = [
    { title: 'Tổng số lô sản xuất', value: statistics.total, icon: PackageOpen, iconClass: 'bg-emerald-100 text-emerald-700' },
    { title: 'Lô đang ở bản nháp', value: statistics.draft, icon: FileText, iconClass: 'bg-slate-100 text-slate-700' },
    { title: 'Lô đang chờ duyệt', value: statistics.pending, icon: Clock3, iconClass: 'bg-amber-100 text-amber-700' },
    { title: 'Lô đã được duyệt', value: statistics.approved, icon: CheckCircle2, iconClass: 'bg-blue-100 text-blue-700' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Tổng quan hợp tác xã</h1>
        <p className="mt-1 text-sm text-slate-500">
          Theo dõi tình hình các lô sản xuất và thống kê tra cứu của hợp tác xã.
        </p>
      </div>

      <Tabs defaultValue={defaultTab} className="w-full">
        <TabsList className="bg-white/80 backdrop-blur-sm border border-emerald-100 p-1 rounded-xl gap-1 min-h-11 max-w-full overflow-x-auto overflow-y-hidden">
          <TabsTrigger value="overview" className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white">Tổng quan</TabsTrigger>
          <TabsTrigger value="lookup-stats" className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white">Thống kê tra cứu</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6 mt-4">
          {/* Thẻ thống kê */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {cards.map((card) => {
              const Icon = card.icon;
              return (
                <Card key={card.title}>
                  <CardContent className="flex items-center justify-between p-5">
                    <div>
                      <p className="text-sm text-slate-500">{card.title}</p>
                      <p className="mt-2 text-3xl font-bold text-slate-900">
                        {isLoading ? '...' : card.value}
                      </p>
                    </div>
                    <div className={`rounded-xl p-3 ${card.iconClass}`}>
                      {Icon && <Icon className="size-6" />}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>

          {/* Biểu đồ sản lượng */}
          <ProductionStatistics data={dashboardData} isLoading={isLoading} />

          {/* Bảng danh sách lô */}
          <ProductionLotBoard />
        </TabsContent>

        <TabsContent value="lookup-stats" className="mt-4">
          <LookupStatisticsPage />
        </TabsContent>
      </Tabs>
    </div>
  );
}