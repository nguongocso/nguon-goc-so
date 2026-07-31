// src/components/dashboard/AdminDashboard.tsx – Bảng điều khiển sản lượng và lô (VT-01)
import { useEffect, useState } from 'react';
import { toast } from 'sonner';
<<<<<<< HEAD
import { getProductionLotDashboard, type DashboardResponse } from '@/api/productionLotApi';
=======
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import type { Organization } from '@/types/organization';
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
import { getOrganizations } from '@/api/organizationApi';
import { ProductionStatistics } from '@/components/dashboard/PoductionStatistics';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
<<<<<<< HEAD
import { OrganizationListPage } from '@/pages/organization/OrganizationListPage';
import type { Organization } from '@/types/organization';

export function AdminDashboard() {
  const [dashboardData, setDashboardData] = useState<DashboardResponse | null>(null);
=======
import { Building2, Users, Calendar } from 'lucide-react';
import LookupStatisticsPage from '@/pages/report/LookupStatisticsPage';

interface AdminDashboardProps {
  initialTab?: string | null;
}

export function AdminDashboard({ initialTab }: AdminDashboardProps) {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
  const [isLoading, setIsLoading] = useState(true);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<string>('');

  useEffect(() => {
    getOrganizations()
      .then((data) => {
        // Response thật từ backend dùng field organizationID/organizationName
        // (không khớp trực tiếp với type Organization khai báo phía FE) —
        // ánh xạ lại giống cách OrganizationListPage đang xử lý để tránh
        // dropdown hiển thị giá trị rỗng.
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
        // Không chặn Dashboard nếu tải danh sách tổ chức thất bại —
        // VT-01 vẫn xem được dữ liệu mặc định.
        toast.error('Không thể tải danh sách tổ chức để lọc.');
      });
  }, []);

  useEffect(() => {
    const loadData = async () => {
      try {
        setIsLoading(true);
<<<<<<< HEAD
        const data = await getProductionLotDashboard(
          selectedOrgId ? { organizationId: selectedOrgId } : undefined,
        );
        setDashboardData(data);
      } catch (error: any) {
        if (error.response?.status === 403) {
          toast.error('Bạn không có quyền xem dữ liệu tổ chức này');
        } else {
          toast.error('Không thể tải dữ liệu');
        }
        setDashboardData(null);
=======
        setError(null);
        const data = await getOrganizations();
        setOrganizations(data);
      } catch (err: any) {
        setError(err.message || 'Không thể tải danh sách tổ chức');
        toast.error('Không thể tải danh sách tổ chức');
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
      } finally {
        setIsLoading(false);
      }
    };
<<<<<<< HEAD
    void loadData();
  }, [selectedOrgId]);
=======
    void loadOrganizations();
  }, []);

  const defaultTab = initialTab === 'lookup-stats' ? 'lookup-stats' : 'organizations';

  const getValue = (obj: any, keys: string[]): string => {
    for (const key of keys) {
      if (obj[key] !== undefined && obj[key] !== null) return String(obj[key]);
    }
    return '—';
  };

  const getCreatedDate = (obj: any): string => {
    const dateStr = obj.createdAt || obj.createdDate || obj.created_at;
    if (!dateStr) return '—';
    try {
      return new Date(dateStr).toLocaleDateString('vi-VN');
    } catch {
      return '—';
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        <span className="ml-3 text-muted-foreground">Đang tải...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center text-red-700">
        <p className="font-semibold">Không thể tải dữ liệu</p>
        <p className="text-sm">{error}</p>
        <button className="mt-3 text-sm underline" onClick={() => window.location.reload()}>
          Thử lại
        </button>
      </div>
    );
  }
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Quản trị hệ thống</h1>
      </div>

<<<<<<< HEAD
      <Card>
        <CardHeader>
          <CardTitle>Bảng điều khiển sản lượng</CardTitle>
        </CardHeader>
        <CardContent>
          {organizations.length > 0 && (
            <div className="mb-4 flex items-center gap-3">
              <label className="text-sm font-medium" htmlFor="admin-dashboard-org">
                Tổ chức:
              </label>
              <select
                id="admin-dashboard-org"
                className="rounded-md border px-3 py-1.5 text-sm"
                value={selectedOrgId}
                onChange={(event) => setSelectedOrgId(event.target.value)}
              >
                <option value="">Mặc định (tổ chức của tôi)</option>
                {organizations.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          <ProductionStatistics data={dashboardData} isLoading={isLoading} />
        </CardContent>
      </Card>

      {/* Danh sách tổ chức — dùng lại trang có sẵn, không viết lại bảng */}
      <OrganizationListPage />
=======
      <Tabs defaultValue={defaultTab} className="w-full">
        <TabsList>
          <TabsTrigger value="organizations">Tổ chức</TabsTrigger>
          <TabsTrigger value="lookup-stats">Thống kê tra cứu</TabsTrigger>
        </TabsList>

        <TabsContent value="organizations" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Danh sách tổ chức</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[50px]">STT</TableHead>
                      <TableHead>ID</TableHead>
                      <TableHead>Tên tổ chức</TableHead>
                      <TableHead>Mã</TableHead>
                      <TableHead>Loại</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Ngày tạo</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {organizations.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-8">
                          <Users className="mx-auto h-8 w-8 text-muted-foreground/50" />
                          <p className="mt-2">Chưa có tổ chức nào</p>
                        </TableCell>
                      </TableRow>
                    ) : (
                      organizations.map((org, index) => (
                        <TableRow key={org.id}>
                          <TableCell className="text-center text-muted-foreground">{index + 1}</TableCell>
                          <TableCell className="font-mono text-xs">{org.id}</TableCell>
                          <TableCell className="font-medium">
                            {getValue(org, ['name', 'organizationName', 'organization_name'])}
                          </TableCell>
                          <TableCell>{getValue(org, ['code', 'organizationCode', 'organization_code'])}</TableCell>
                          <TableCell>{getValue(org, ['type', 'organizationType', 'organization_type'])}</TableCell>
                          <TableCell>
                            <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                              org.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                              org.status === 'INACTIVE' ? 'bg-gray-100 text-gray-800' : 'bg-yellow-100 text-yellow-800'
                            }`}>
                              {org.status || '—'}
                            </span>
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center gap-1">
                              <Calendar className="h-3 w-3 text-muted-foreground" />
                              {getCreatedDate(org)}
                            </div>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="lookup-stats" className="mt-4">
          <LookupStatisticsPage />
        </TabsContent>
      </Tabs>
>>>>>>> a977779d05bcbb9eb05043f861396ae6acac9789
    </div>
  );
}