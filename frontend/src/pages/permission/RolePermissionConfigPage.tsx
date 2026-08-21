import React from 'react';
import { RolePermissionConfig } from '@/components/permission/RolePermissionConfig';
import { HelpButton } from '@/components/help/HelpButton';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

const RolePermissionConfigPage: React.FC = () => {
  return (
    <div className="container mx-auto py-6 max-w-6xl">
      
      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div>
            <CardTitle>Cấu hình phân quyền chi tiết</CardTitle>
            <CardDescription>
              Bật/tắt quyền theo nhóm chức năng cho từng vai trò trong tổ chức.
              Cấu hình chỉ áp dụng cho tổ chức của bạn.
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