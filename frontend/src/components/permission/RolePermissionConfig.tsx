import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Loader2, Save, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import {
  getOrganizationRoles,
  getRolePermissions,
  updateRolePermissions,
} from '@/api/permissionApi';
import { PermissionGroup } from './PermissionGroup';
import type { RoleInfo, PermissionGroup as PermissionGroupType } from '@/types/permission';
import { getRoleLabel } from '@/config/roleAccess';

export const RolePermissionConfig: React.FC = () => {
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const organizationId = user?.organizationId;

  const [roles, setRoles] = useState<RoleInfo[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [permissions, setPermissions] = useState<PermissionGroupType[]>([]);
  const [loadingRoles, setLoadingRoles] = useState(true);
  const [loadingPermissions, setLoadingPermissions] = useState(false);
  const [saving, setSaving] = useState(false);

  // Lấy danh sách vai trò của tổ chức
  useEffect(() => {
    const fetchRoles = async () => {
      if (!organizationId) return;
      try {
        setLoadingRoles(true);
        const data = await getOrganizationRoles(organizationId);
        // Loại bỏ VT-01 (Admin hệ thống) khỏi danh sách
        const filtered = data.filter((role) => role.roleCode !== 'VT-01');
        setRoles(filtered);

        // Kiểm tra xem có roleId hoặc roleCode trên URL không
        const paramRoleId = searchParams.get('roleId');
        const paramRoleCode = searchParams.get('roleCode');

        if (paramRoleId) {
          const matched = filtered.find((r) => r.roleId === Number(paramRoleId));
          if (matched) {
            setSelectedRoleId(matched.roleId);
            return;
          }
        }
        if (paramRoleCode) {
          const matched = filtered.find((r) => r.roleCode === paramRoleCode);
          if (matched) {
            setSelectedRoleId(matched.roleId);
            return;
          }
        }

        if (filtered.length > 0) {
          // Mặc định chọn VT-03 (Người ghi sự kiện) nếu có
          const defaultRole = filtered.find((r) => r.roleCode === 'VT-03') || filtered[0];
          setSelectedRoleId(defaultRole.roleId);
        }
      } catch (error: any) {
        toast.error(error.response?.data?.message || 'Không thể tải danh sách vai trò');
      } finally {
        setLoadingRoles(false);
      }
    };
    fetchRoles();
  }, [organizationId, searchParams]);

  // Lấy cấu hình quyền khi chọn vai trò
  const fetchPermissions = useCallback(async (roleId: number) => {
    if (!organizationId) return;
    try {
      setLoadingPermissions(true);
      const data = await getRolePermissions(organizationId, roleId);
      setPermissions(data.groups);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Không thể tải cấu hình quyền';
      toast.error(message);
    } finally {
      setLoadingPermissions(false);
    }
  }, [organizationId]);

  useEffect(() => {
    if (selectedRoleId) {
      fetchPermissions(selectedRoleId);
    }
  }, [selectedRoleId, fetchPermissions]);

  const handleToggle = (permissionId: number, enabled: boolean) => {
    setPermissions((prev) =>
      prev.map((group) => ({
        ...group,
        permissions: group.permissions.map((p) =>
          p.permissionId === permissionId ? { ...p, isEnabled: enabled, isDefault: false } : p
        ),
      }))
    );
  };

  const handleSave = async () => {
    if (!organizationId || !selectedRoleId) return;

    const allPermissions = permissions.flatMap((g) => g.permissions);
    const payload = {
      permissions: allPermissions.map((p) => ({
        permissionId: p.permissionId,
        isEnabled: p.isEnabled,
      })),
    };

    setSaving(true);
    try {
      const updated = await updateRolePermissions(organizationId, selectedRoleId, payload);
      setPermissions(updated.groups);
      toast.success('Cập nhật cấu hình quyền thành công!');
    } catch (error: any) {
      const status = error.response?.status;
      const message = error.response?.data?.message;
      if (status === 403) {
        toast.error('Bạn không có quyền cấu hình phân quyền.');
      } else if (status === 404) {
        toast.error('Không tìm thấy vai trò hoặc tổ chức.');
      } else if (status === 400 && message?.includes('VT-01')) {
        toast.error('Không thể cấu hình quyền cho quản trị viên hệ thống.');
      } else {
        toast.error(message || 'Cập nhật thất bại');
      }
    } finally {
      setSaving(false);
    }
  };

  const getDisplayName = (roleCode: string) => getRoleLabel(roleCode);

  // Xác định vai trò đang được chọn để hiển thị tên trên trigger
  const selectedRole = roles.find((r) => r.roleId === selectedRoleId);

  if (loadingRoles && roles.length === 0) {
    return (
      <div className="flex justify-center p-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!organizationId) {
    return <div className="p-8 text-center text-muted-foreground">Không tìm thấy tổ chức của bạn.</div>;
  }

  return (
    <div className="space-y-6">
      {/* Chọn vai trò */}
      <div className="flex items-center justify-between gap-4 flex-wrap bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <label className="text-sm font-semibold text-slate-800">Cấu hình phân quyền cho vai trò</label>
            <p className="text-xs text-muted-foreground">Tùy biến quyền hạn và các sự kiện chuỗi được phép thao tác</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Select
            value={selectedRoleId?.toString() || ''}
            onValueChange={(val) => {
              if (val) setSelectedRoleId(parseInt(val));
            }}
          >
            <SelectTrigger className="w-[260px] border-emerald-200 focus:ring-emerald-100 font-medium">
              <SelectValue placeholder="Chọn vai trò">
                {selectedRole ? getDisplayName(selectedRole.roleCode) : ''}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {roles.map((role) => (
                <SelectItem key={role.roleId} value={role.roleId.toString()} className="font-medium">
                  {getDisplayName(role.roleCode)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Danh sách nhóm quyền */}
      {loadingPermissions ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-emerald-600" />
        </div>
      ) : permissions.length === 0 ? (
        <div className="text-center py-12 text-muted-foreground bg-white rounded-xl border">
          Không có quyền nào để cấu hình.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {permissions.map((group) => (
            <PermissionGroup
              key={group.resource}
              resource={group.resource}
              resourceLabel={group.resourceLabel}
              permissions={group.permissions}
              onToggle={handleToggle}
              disabled={saving}
            />
          ))}
        </div>
      )}

      {/* Nút lưu */}
      <div className="flex justify-end">
        <Button variant="create" onClick={handleSave} disabled={saving || loadingPermissions}>
          {saving ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Đang lưu...
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-2" />
              Lưu cấu hình
            </>
          )}
        </Button>
      </div>
    </div>
  );
};