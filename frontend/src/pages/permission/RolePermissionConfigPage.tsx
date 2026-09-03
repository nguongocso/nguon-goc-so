import React from 'react';
import { RolePermissionConfig } from '@/components/permission/RolePermissionConfig';
import { HelpButton } from '@/components/help/HelpButton';

const RolePermissionConfigPage: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Cấu hình phân quyền Người ghi sự kiện
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Bật/tắt quyền theo nhóm chức năng và các sự kiện chuỗi cho vai trò Người ghi sự kiện trong hợp tác xã. Cấu hình áp dụng cho tất cả người ghi sự kiện của tổ chức bạn.
          </p>
        </div>
        <HelpButton screenKey="permission-config" />
      </div>

      <RolePermissionConfig />
    </div>
  );
};

export default RolePermissionConfigPage;