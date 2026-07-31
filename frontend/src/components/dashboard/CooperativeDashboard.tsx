import { useEffect, useMemo, useState } from 'react';
import {
  CheckCircle2,
  Clock3,
  FileText,
  PackageOpen,
} from 'lucide-react';
import { toast } from 'sonner';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent } from '@/components/ui/card';

import { getProductionLotDashboard, type DashboardResponse } from '@/api/productionLotApi';
import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';
<<<<<<< HEAD
import { ProductionStatistics } from '@/components/dashboard/PoductionStatistics'

export function CooperativeDashboard() {
  const [data, setData] = useState<DashboardResponse | null>(null);
=======
import type { ProductionLot } from '@/types/productionLot';
import LookupStatisticsPage from '@/pages/report/LookupStatisticsPage';

interface CooperativeDashboardProps {
  initialTab?: string | null;
}

export function CooperativeDashboard({ initialTab }: CooperativeDashboardProps) {
  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadDashboard = async () => {
      try {
        setIsLoading(true);
<<<<<<< HEAD
        const result = await getProductionLotDashboard();
        setData(result);
      } catch (error: any) {
        if (error.response?.status === 403) {
          toast.error('Bạn không có quyền truy cập dữ liệu này.');
        } else {
          toast.error('Không thể tải bảng điều khiển');
        }
        setData(null);
=======
        const data = await getProductionLots();
        setProductionLots(data);
      } catch {
        toast.error('Không thể tải danh sách lô sản xuất');
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
      } finally {
        setIsLoading(false);
      }
    };
<<<<<<< HEAD

    void loadDashboard();
=======
    void loadProductionLots();

    // Ghi nhận lần truy cập dashboard (TC-04)
    void logDashboardAccess('cooperative-dashboard');
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
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

<<<<<<< HEAD
      <ProductionStatistics data={data} isLoading={isLoading} />
      <ProductionLotBoard />
=======
      <Tabs defaultValue={defaultTab} className="w-full">
        <TabsList>
          <TabsTrigger value="overview">Tổng quan</TabsTrigger>
          <TabsTrigger value="lookup-stats">Thống kê tra cứu</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6 mt-4">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
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
                      <Icon className="size-6" />
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
          <ProductionLotBoard />
        </TabsContent>

        <TabsContent value="lookup-stats" className="mt-4">
          <LookupStatisticsPage />
        </TabsContent>
      </Tabs>
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
    </div>
  );
}