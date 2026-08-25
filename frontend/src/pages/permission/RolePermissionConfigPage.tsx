import React from 'react';
import { RolePermissionConfig } from '@/components/permission/RolePermissionConfig';
import { HelpButton } from '@/components/help/HelpButton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const RolePermissionConfigPage: React.FC = () => {
  return (
    <div className="space-y-6">
      
      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div>
            <CardTitle>Cấu hình phân quyền Người ghi sự kiện</CardTitle>
            <CardDescription>
              Bật/tắt quyền theo nhóm chức năng và các sự kiện chuỗi cho vai trò Người ghi sự kiện trong hợp tác xã.
              Cấu hình áp dụng cho tất cả người ghi sự kiện của tổ chức bạn.
            </CardDescription>
          </div>
          <HelpButton screenKey="permission-config" />
        </CardHeader>
        <CardContent>
          <RolePermissionConfig />
        </CardContent>
      </Card>
    </div>
  );
};

export default RolePermissionConfigPage;