import { useCallback, useEffect, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { getProductionLots } from '@/api/productionLotApi';
import { ProductionLotBoard } from '@/components/production-lot/ProductionLotBoard';
import { Card, CardContent } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PackageOpen, CheckCircle2, Sprout, PackageCheck } from 'lucide-react';
import type { ProductionLot } from '@/types/productionLot';
import { IndustryReportPanel } from '@/components/report/IndustryReportPanel';
import { HelpButton } from '@/components/help/HelpButton';

export function ManagementDashboard() {
  const [productionLots, setProductionLots] = useState<ProductionLot[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadProductionLots = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await getProductionLots();
      setProductionLots(data);
    } catch {
      toast.error('Không thể tải danh sách lô sản xuất');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProductionLots();
  }, [loadProductionLots]);

  const statistics = useMemo(() => {
    // NCL-02-CN-006: lô đã hủy không tính vào tổng số lô đang canh tác
    const total = productionLots.filter((lot) => lot.status !== 'CANCELLED').length;
    const approved = productionLots.filter((lot) => lot.status === 'APPROVED').length;
    const harvested = productionLots.filter((lot) => lot.status === 'HARVESTED').length;
    const packaged = productionLots.filter((lot) => lot.status === 'PACKAGED').length;
    return { total, approved, harvested, packaged };
  }, [productionLots]);

  const cards = [
    { title: 'Tổng số lô', value: statistics.total, icon: PackageOpen, iconClass: 'bg-info-bg text-info' },
    { title: 'Lô đã duyệt', value: statistics.approved, icon: CheckCircle2, iconClass: 'bg-success-bg text-success' },
    { title: 'Lô đã thu hoạch', value: statistics.harvested, icon: Sprout, iconClass: 'bg-success-bg text-success' },
    { title: 'Lô đã đóng gói', value: statistics.packaged, icon: PackageCheck, iconClass: 'bg-warning-bg text-warning' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">
            Quản lý ngành – Báo cáo tổng hợp
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Thống kê tình hình sản xuất và truy xuất nguồn gốc.
          </p>
        </div>
        <HelpButton screenKey="dashboard" />
      </div>

      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="flex w-full min-w-0 justify-start items-center gap-1.5 overflow-x-auto rounded-xl border border-emerald-100 bg-white/80 p-1.5 backdrop-blur-sm scrollbar-none">
          <TabsTrigger value="overview" className="shrink-0 rounded-lg px-3.5 py-2 text-xs sm:text-sm font-medium whitespace-nowrap data-[state=active]:bg-emerald-600 data-active:bg-emerald-600 data-[state=active]:text-white data-active:text-white transition-all cursor-pointer">Tổng quan</TabsTrigger>
          <TabsTrigger value="industry-report" className="shrink-0 rounded-lg px-3.5 py-2 text-xs sm:text-sm font-medium whitespace-nowrap data-[state=active]:bg-emerald-600 data-active:bg-emerald-600 data-[state=active]:text-white data-active:text-white transition-all cursor-pointer">Báo cáo theo địa bàn</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-4 space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {cards.map((card) => {
              const Icon = card.icon;
              return (
                <Card key={card.title}>
                  <CardContent className="flex items-center justify-between p-5">
                    <div>
                      <p className="text-sm text-muted-foreground">{card.title}</p>
                      <p className="mt-2 text-3xl font-bold text-foreground">
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

          <ProductionLotBoard
            lots={productionLots}
            isLoading={isLoading}
            canCreate={false}
            canEdit={false}
            canSubmitForApproval={false}
            canApprove={false}
            canRecordFarmLog={false}
            onRefresh={() => void loadProductionLots()}
            isRefreshing={isLoading}
          />
        </TabsContent>

        <TabsContent value="industry-report" className="mt-4">
          <IndustryReportPanel />
        </TabsContent>
      </Tabs>
    </div>
  );
}