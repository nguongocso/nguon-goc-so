import React, { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Loader2, Save, ShieldCheck } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import {
  getOrganizationRoles,
  getRolePermissions,
  updateRolePermissions,
} from '@/api/permissionApi';
import { PermissionGroup } from './PermissionGroup';
import type { RoleInfo, PermissionGroup as PermissionGroupType } from '@/types/permission';

export const RolePermissionConfig: React.FC = () => {
  const { user } = useAuth();
  const organizationId = user?.organizationId;

  const [roleInfo, setRoleInfo] = useState<RoleInfo | null>(null);
  const [permissions, setPermissions] = useState<PermissionGroupType[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Tự động tải vai trò Người ghi sự kiện (VT-03) và danh sách quyền
  useEffect(() => {
    const fetchRoleAndPermissions = async () => {
      if (!organizationId) return;
      try {
        setLoading(true);
        const rolesData = await getOrganizationRoles(organizationId);
        // Tìm vai trò VT-03 (Người ghi sự kiện)
        const eventRecorderRole = rolesData.find((r) => r.roleCode === 'VT-03') || {
          roleId: 3,
          roleCode: 'VT-03',
          roleName: 'Người ghi sự kiện',
        };
        setRoleInfo(eventRecorderRole);

        const permData = await getRolePermissions(organizationId, eventRecorderRole.roleId);
        setPermissions(permData.groups);
      } catch (error: any) {
        toast.error(error.response?.data?.message || 'Không thể tải cấu hình quyền của Người ghi sự kiện');
      } finally {
        setLoading(false);
      }
    };
    fetchRoleAndPermissions();
  }, [organizationId]);

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
    if (!organizationId || !roleInfo) return;

    const allPermissions = permissions.flatMap((g) => g.permissions);
    const payload = {
      permissions: allPermissions.map((p) => ({
        permissionId: p.permissionId,
        isEnabled: p.isEnabled,
      })),
    };

    setSaving(true);
    try {
      const updated = await updateRolePermissions(organizationId, roleInfo.roleId, payload);
      setPermissions(updated.groups);
      toast.success('Cập nhật cấu hình quyền cho Người ghi sự kiện thành công!');
    } catch (error: any) {
      const status = error.response?.status;
      const message = error.response?.data?.message;
      if (status === 403) {
        toast.error('Bạn không có quyền cấu hình phân quyền.');
      } else if (status === 404) {
        toast.error('Không tìm thấy vai trò hoặc tổ chức.');
      } else {
        toast.error(message || 'Cập nhật thất bại');
      }
    } finally {
      setSaving(false);
    }
  };

  if (!organizationId) {
    return <div className="p-8 text-center text-muted-foreground">Không tìm thấy tổ chức của bạn.</div>;
  }

  return (
    <div className="space-y-6">
      {/* Banner thông tin vai trò */}
      <div className="flex items-center justify-between gap-4 flex-wrap bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-slate-800">Phân quyền Người ghi sự kiện</h3>
            <p className="text-xs text-muted-foreground">
              Tùy biến quyền hạn và các sự kiện chuỗi áp dụng cho vai trò Người ghi sự kiện trong hợp tác xã
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold">
            <span className="size-2 rounded-full bg-emerald-500" />
            Vai trò: Người ghi sự kiện (VT-03)
          </span>
        </div>
      </div>

      {/* Danh sách nhóm quyền */}
      {loading ? (
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
        <Button variant="create" onClick={handleSave} disabled={saving || loading}>
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