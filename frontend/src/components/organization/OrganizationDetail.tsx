import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { toast } from 'sonner';

import {
  createOrganizationMember,
  getOrganizationDetail,
} from '@/api/organizationApi';
import { Badge } from '@/components/ui/badge';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { getRoleLabel } from '@/config/roleAccess';
import type { AddMemberRequest, OrganizationDetailResponse } from '@/types/organization';
import { AddExistingUserDialog } from './AddExistingUserDialog';
import {
  CreateOrganizationMemberForm,
  type CreateOrganizationMemberFormData,
} from './CreateOrganizationMemberFrom';

function getAvailableRolesForType(type: string): Array<{ id: number; code: string; name: string }> {
  if (type === 'COOPERATIVE') {
    return [
      { id: 2, code: 'VT-02', name: 'Quản lý hợp tác xã' },
      { id: 3, code: 'VT-03', name: 'Người ghi sự kiện' },
    ];
  }
  if (type === 'ENTERPRISE') {
    return [{ id: 4, code: 'VT-04', name: 'Doanh nghiệp thu mua' }];
  }
  if (type === 'GOVERNMENT') {
    return [{ id: 5, code: 'VT-05', name: 'Cán bộ ngành' }];
  }
  if (type === 'SYSTEM') {
    return [{ id: 6, code: 'VT-06', name: 'Người dùng hệ thống' }];
  }
  return [];
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const isActive = normalized === 'ACTIVE';

  const label = isActive ? 'Đang hoạt động' : 'Không hoạt động';
  const colorClasses = isActive
    ? 'bg-green-500 hover:bg-green-600 text-white'
    : 'bg-gray-300 hover:bg-gray-400 text-gray-700';

  return <Badge className={`${colorClasses} ml-2`}>{label}</Badge>;
}

/**
 * Hiển thị thông tin chi tiết tổ chức và danh sách thành viên thuộc tổ chức.
 */
export function OrganizationDetail() {
  const { id } = useParams<{ id: string }>();

  const [data, setData] = useState<OrganizationDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [openCreate, setOpenCreate] = useState(false);
  const [openAddExisting, setOpenAddExisting] = useState(false);

  const fetchOrganizationDetail = async () => {
    if (!id) {
      return;
    }
    try {
      setLoading(true);
      const detail = await getOrganizationDetail(id);
      setData(detail);
    } catch {
      toast.error('Không thể tải thông tin tổ chức');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchOrganizationDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleCreateMember = async (values: CreateOrganizationMemberFormData) => {
    if (!id) {
      return;
    }
    try {
      setSubmitting(true);
      const payload: AddMemberRequest = {
        username: values.username,
        password: values.password,
        fullName: values.fullName,
        email: values.email || undefined,
        phone: values.phone || undefined,
        roleId: values.roleId,
      };

      await createOrganizationMember(id, payload);
      toast.success('Tạo thành viên thành công');
      setOpenCreate(false);
      void fetchOrganizationDetail();
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      toast.error(axiosError.response?.data?.message || 'Tạo thành viên thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="p-6">Đang tải...</div>;
  }

  if (!data) {
    return <div className="p-6">Không tìm thấy tổ chức</div>;
  }

  const { profile, members } = data;
  const availableRoles = getAvailableRolesForType(profile.type);

  return (
    <div className="space-y-6">
      {/* THÔNG TIN TỔ CHỨC */}
      <Card>
        <CardHeader>
          <CardTitle>Thông tin tổ chức</CardTitle>
        </CardHeader>

        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <span className="font-semibold">Tên tổ chức:</span> {profile.name}
          </div>

          <div>
            <span className="font-semibold">Mã tổ chức:</span> {profile.code}
          </div>

          <div>
            <span className="font-semibold">Loại tổ chức:</span> {profile.type}
          </div>

          <div>
            <span className="font-semibold">Trạng thái:</span>
            <StatusBadge status={profile.status} />
          </div>

          <div>
            <span className="font-semibold">Email:</span> {profile.email || '-'}
          </div>

          <div>
            <span className="font-semibold">Số điện thoại:</span> {profile.phone || '-'}
          </div>

          <div className="md:col-span-2">
            <span className="font-semibold">Địa chỉ:</span> {profile.address || '-'}
          </div>
        </CardContent>
      </Card>

      {/* DANH SÁCH THÀNH VIÊN */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Danh sách thành viên</CardTitle>

          <DropdownMenu>
            <DropdownMenuTrigger className="h-9 px-4 py-2 bg-primary text-primary-foreground shadow hover:bg-primary/90 inline-flex items-center justify-center rounded-md text-sm font-medium">
              <Plus className="h-4 w-4 mr-1" />
              Thêm thành viên
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setOpenCreate(true)}>
                Tạo mới tài khoản
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setOpenAddExisting(true)}>
                Thêm tài khoản đã có
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </CardHeader>

        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tên đăng nhập</TableHead>
                <TableHead>Họ tên</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Số điện thoại</TableHead>
                <TableHead>Vai trò</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {members.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-6">
                    Chưa có thành viên nào
                  </TableCell>
                </TableRow>
              ) : (
                members.map((member) => (
                  <TableRow key={member.id}>
                    <TableCell>{member.username}</TableCell>
                    <TableCell>{member.fullName}</TableCell>
                    <TableCell>{member.email || '-'}</TableCell>
                    <TableCell>{member.phone || '-'}</TableCell>
                    <TableCell>
                      <Badge variant="outline">
                        {getRoleLabel(member.roleCode) || member.roleName}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <StatusBadge status={member.status} />
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* DIALOG: TẠO MỚI TÀI KHOẢN */}
      <Dialog open={openCreate} onOpenChange={setOpenCreate}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Tạo mới thành viên cho {profile.name}</DialogTitle>
          </DialogHeader>

          <CreateOrganizationMemberForm
            onSubmit={handleCreateMember}
            loading={submitting}
            organizationType={profile.type}
          />
        </DialogContent>
      </Dialog>

      {/* DIALOG: THÊM TÀI KHOẢN ĐÃ CÓ */}
      <AddExistingUserDialog
        open={openAddExisting}
        onOpenChange={setOpenAddExisting}
        organizationId={id || ''}
        onSuccess={() => void fetchOrganizationDetail()}
        availableRoles={availableRoles}
      />
    </div>
  );
}
