import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  getProductionLotDashboard,
  type DashboardResponse,
} from "@/api/productionLotApi";
import { getOrganizations } from "@/api/organizationApi";
import type { Organization } from "@/types/organization";
import { ProductionStatistics } from "@/components/dashboard/PoductionStatistics";
import { OrganizationListPage } from "@/pages/organization/OrganizationListPage";
import LookupStatisticsPage from "@/pages/report/LookupStatisticsPage";
import { HelpButton } from "@/components/help/HelpButton";

interface AdminDashboardProps {
  initialTab?: string | null;
}

export function AdminDashboard({ initialTab }: AdminDashboardProps) {
  const [dashboardData, setDashboardData] = useState<DashboardResponse | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<string>("");

  useEffect(() => {
    getOrganizations()
      .then((data) => {
        const mapped = (data as any[]).map((item) => ({
          id: item.id ?? item.organizationID,
          name: item.name ?? item.organizationName,
          code: item.code ?? item.organizationCode,
          type: item.type ?? item.organizationType,
          status: item.status,
          createdAt: item.createdAt,
          updatedAt: item.updatedAt,
        })) as Organization[];
        setOrganizations(mapped);
      })
      .catch(() => {
        toast.error("Không thể tải danh sách tổ chức để lọc.");
      });
  }, []);

  useEffect(() => {
    const loadData = async () => {
      try {
        setIsLoading(true);
        const data = await getProductionLotDashboard(
          selectedOrgId ? { organizationId: selectedOrgId } : undefined,
        );
        setDashboardData(data);
      } catch (error: any) {
        if (error.response?.status === 403) {
          toast.error("Bạn không có quyền xem dữ liệu tổ chức này");
        } else {
          toast.error("Không thể tải dữ liệu");
        }
        setDashboardData(null);
      } finally {
        setIsLoading(false);
      }
    };
    void loadData();
  }, [selectedOrgId]);

  const defaultTab =
    initialTab === "lookup-stats" ? "lookup-stats" : "overview";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Quản trị hệ thống</h1>
        <HelpButton screenKey="dashboard" />
      </div>

      <Tabs defaultValue={defaultTab} className="w-full">
        <TabsList className="bg-white/80 backdrop-blur-sm border border-emerald-100 p-1 rounded-xl gap-1 min-h-11 max-w-full overflow-x-auto overflow-y-hidden">
          <TabsTrigger value="overview" className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white">Tổng quan sản lượng</TabsTrigger>
          <TabsTrigger value="organizations" className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white">Tổ chức</TabsTrigger>
          <TabsTrigger value="lookup-stats" className="rounded-lg px-4 py-2 lg:px-5 min-h-9 data-[state=active]:bg-emerald-600 data-[state=active]:text-white">Thống kê tra cứu</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Bảng điều khiển sản lượng</CardTitle>
            </CardHeader>
            <CardContent>
              {organizations.length > 0 && (
                <div className="mb-4 flex items-center gap-3">
                  <label
                    className="text-sm font-medium"
                    htmlFor="admin-dashboard-org"
                  >
                    Tổ chức:
                  </label>
                  <Select
                    value={selectedOrgId}
                    onValueChange={(value) => setSelectedOrgId(value ?? "")}
                  >
                    <SelectTrigger className="w-full sm:w-[280px] md:w-[350px]">
                      <SelectValue placeholder="Chọn tổ chức">
                        {organizations.find((org) => org.id === selectedOrgId)
                          ?.name || "Chọn tổ chức"}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {organizations.map((org) => (
                        <SelectItem key={org.id} value={org.id}>
                          {org.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
              <ProductionStatistics
                data={dashboardData}
                isLoading={isLoading}
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="organizations" className="mt-4">
          <OrganizationListPage />
        </TabsContent>

        <TabsContent value="lookup-stats" className="mt-4">
          <LookupStatisticsPage />
        </TabsContent>
      </Tabs>
    </div>
  );
}
