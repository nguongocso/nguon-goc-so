import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { PlusCircle, RefreshCw } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { getOrganizations } from '@/api/organizationApi';
import { type Organization } from '@/types/organization';
import { ORGANIZATION_TYPES } from '@/utils/constants';
import { usePermission } from '@/hooks/usePermission';
import { ROLE_ACCESS } from '@/config/roleAccess';

export function OrganizationListPage() {
  const navigate = useNavigate();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);

  const canCreate = usePermission(ROLE_ACCESS.organizationCreate);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const data = await getOrganizations();

      // ✅ Ánh xạ dữ liệu từ API sang kiểu Organization
      const mappedData: Organization[] = data.map((item: any) => ({
        id: item.organizationID,
        name: item.organizationName,
        code: item.organizationCode,
        type: item.organizationType,
        status: item.status,
        createdAt: item.createdAt,
        updatedAt: item.updatedAt,
      }));

      setOrganizations(mappedData);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách tổ chức');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganizations();
  }, []);

  const getStatusBadge = (status: string) => {
    const variants: Record<string, 'default' | 'destructive' | 'secondary'> = {
      ACTIVE: 'default',
      INACTIVE: 'destructive',
    };
    const labels: Record<string, string> = {
      ACTIVE: 'Đang hoạt động',
      INACTIVE: 'Ngừng hoạt động',
    };
    return <Badge variant={variants[status] || 'secondary'}>{labels[status] || status}</Badge>;
  };

  const getTypeLabel = (type: string) => {
    return ORGANIZATION_TYPES[type as keyof typeof ORGANIZATION_TYPES] || type;
  };

  return (
    <div className="py-8">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Danh sách tổ chức</CardTitle>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchOrganizations}
              disabled={loading}
            >
              <RefreshCw className={`h-4 w-4 mr-1 ${loading ? 'animate-spin' : ''}`} />
              Làm mới
            </Button>
            {canCreate && (
              <Button variant="create" onClick={() => navigate('/organizations/create')}>
                <PlusCircle className="h-4 w-4 mr-1" />
                Tạo tổ chức
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-8">Đang tải...</div>
          ) : organizations.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              Chưa có tổ chức nào. Nhấn "Tạo tổ chức" để thêm mới.
            </div>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Mã</TableHead>
                    <TableHead>Tên tổ chức</TableHead>
                    <TableHead>Loại</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Ngày tạo</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {organizations.map((org) => (
                    <TableRow key={org.id}>
                      <TableCell className="font-medium">{org.code}</TableCell>
                      <TableCell>{org.name}</TableCell>
                      <TableCell>{getTypeLabel(org.type)}</TableCell>
                      <TableCell>{getStatusBadge(org.status)}</TableCell>
                      <TableCell>{new Date(org.createdAt).toLocaleDateString('vi-VN')}</TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="view"
                          size="sm"
                          onClick={() => navigate(`/organizations/${org.id}`)}
                        >
                          Xem
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}