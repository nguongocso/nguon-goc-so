import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  getOrganizationDetail,
  createOrganizationMember,
} from "@/api/organizationApi";
import type { OrganizationDetailResponse } from "@/types/organization";
import { Plus } from "lucide-react";
import type { AddMemberRequest } from "@/types/organization";
import {
  CreateOrganizationMemberForm,
  type CreateOrganizationMemberFormData,
} from "./CreateOrganizationMemberFrom";
import { toast } from "sonner";
import { getRoleLabel } from "@/config/roleAccess";
import { AddExistingUserDialog } from "./AddExistingUserDialog";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

// Hàm lấy danh sách role theo loại tổ chức
const getAvailableRolesForType = (type: string) => {
  if (type === "COOPERATIVE") {
    return [
      { id: 2, code: "VT-02", name: "Quản lý hợp tác xã" },
      { id: 3, code: "VT-03", name: "Người ghi sự kiện" },
    ];
  } else if (type === "ENTERPRISE") {
    return [{ id: 4, code: "VT-04", name: "Doanh nghiệp thu mua" }];
  } else if (type === "GOVERNMENT") {
    return [{ id: 5, code: "VT-05", name: "Cán bộ ngành" }];
  } else if (type === "SYSTEM") {
    return [{ id: 6, code: "VT-06", name: "Người dùng hệ thống" }];
  }
  return [];
};

// Helper để render badge trạng thái với màu sắc và nhãn tiếng Việt
const StatusBadge = ({ status }: { status: string }) => {
  const normalized = status.toUpperCase();
  const isActive = normalized === "ACTIVE";

  const label = isActive ? "Đang hoạt động" : "Không hoạt động";
  const colorClasses = isActive
    ? "bg-green-500 hover:bg-green-600 text-white"
    : "bg-gray-300 hover:bg-gray-400 text-gray-700";

  return <Badge className={`${colorClasses} ml-2`}>{label}</Badge>;
};

export function OrganizationDetail() {
  const { id } = useParams();

  const [data, setData] = useState<OrganizationDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [openCreate, setOpenCreate] = useState(false);
  const [openAddExisting, setOpenAddExisting] = useState(false);

  // Hàm fetch dữ liệu
  const fetchOrganizationDetail = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const detail = await getOrganizationDetail(id);
      setData(detail);
    } catch (error) {
      toast.error("Không thể tải thông tin tổ chức");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrganizationDetail();
  }, [id]);

  const handleCreateMember = async (
    values: CreateOrganizationMemberFormData,
  ) => {
    if (!id) return;

    const payload: AddMemberRequest = {
      username: values.username,
      password: values.password,
      fullName: values.fullName,
      phone: values.phone?.trim() ? values.phone.trim() : undefined,
      email: values.email?.trim() ? values.email.trim() : undefined,
      roleId: values.roleId,
    };

    try {
      setSubmitting(true);
      await createOrganizationMember(id, payload);
      await fetchOrganizationDetail();
      setOpenCreate(false);
      toast.success("Thêm tài khoản thành công");
    } catch (error: any) {
      const message =
        error?.response?.data?.message || "Không thể thêm tài khoản";
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const ORGANIZATION_TYPE_LABELS: Record<string, string> = {
    COOPERATIVE: "Hợp tác xã",
    ENTERPRISE: "Doanh nghiệp",
    GOVERNMENT: "Cán bộ ngành",
    SYSTEM: "Tổ chức hệ thống",
  };

  if (loading) return <div>Đang tải...</div>;
  if (!data) return <div>Không tìm thấy tổ chức</div>;

  const isSystem = data.profile.type === "SYSTEM";
  const availableRoles = getAvailableRolesForType(data.profile.type);

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Thông tin tổ chức</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <div>
            <b>Mã:</b> {data.profile.code}
          </div>
          <div>
            <b>Tên:</b> {data.profile.name}
          </div>
          <div>
            <b>Loại:</b>{" "}
            {ORGANIZATION_TYPE_LABELS[data.profile.type] ?? data.profile.type}
          </div>
          <div>
            <b>Email:</b> {data.profile.email}
          </div>
          <div>
            <b>SĐT:</b> {data.profile.phone}
          </div>
          <div>
            <b>Địa chỉ:</b> {data.profile.address}
          </div>
          <div>
            <b>Trạng thái:</b>
            <StatusBadge status={data.profile.status} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between flex-wrap gap-2">
          <CardTitle>Danh sách tài khoản</CardTitle>
          <DropdownMenu>
            <DropdownMenuTrigger className="inline-flex items-center justify-center rounded-md bg-primary text-primary-foreground px-4 py-2 text-sm font-medium hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2">
              <Plus className="w-4 h-4 mr-1" />
              Thêm tài khoản
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setOpenCreate(true)}>
                Thêm mới
              </DropdownMenuItem>
              {!isSystem && (
                <DropdownMenuItem onClick={() => setOpenAddExisting(true)}>
                  Thêm tài khoản đã có
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tài khoản</TableHead>
                <TableHead>Họ tên</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Vai trò</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.members.map((m) => (
                <TableRow key={m.id}>
                  <TableCell>{m.username}</TableCell>
                  <TableCell>{m.fullName}</TableCell>
                  <TableCell>{m.email}</TableCell>
                  <TableCell>
                    {m.roleCode ? getRoleLabel(m.roleCode) : m.roleName}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={m.status} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Dialog open={openCreate} onOpenChange={setOpenCreate}>
        <DialogContent className="max-w-2xl lg:max-w-4xl xl:max-w-6xl w-full p-4 sm:p-6 lg:p-8 max-h-[90vh] overflow-y-auto">
          <DialogHeader className="border-b pb-4 mb-4">
            <DialogTitle className="text-2xl font-bold">
              Thêm tài khoản mới
            </DialogTitle>
          </DialogHeader>
          <CreateOrganizationMemberForm
            onSubmit={handleCreateMember}
            loading={submitting}
            organizationType={data.profile.type}
          />
        </DialogContent>
      </Dialog>

      {/* Dialog thêm tài khoản đã có (chỉ hiển thị với non-SYSTEM) */}
      {!isSystem && (
        <AddExistingUserDialog
          open={openAddExisting}
          onOpenChange={setOpenAddExisting}
          organizationId={data.profile.organizationId}
          onSuccess={fetchOrganizationDetail}
          availableRoles={availableRoles}
        />
      )}
    </div>
  );
}