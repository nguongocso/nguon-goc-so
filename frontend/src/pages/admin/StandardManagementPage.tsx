import React from 'react';
import { StandardList } from '@/components/admin/StandardList';
import { HelpButton } from '@/components/help/HelpButton';

const StandardManagementPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Quản lý tiêu chuẩn chất lượng</h1>
          <p className="text-sm text-muted-foreground">
            Quản lý danh mục tiêu chuẩn chất lượng dùng chung cho toàn nền tảng.
          </p>
        </div>
        <HelpButton screenKey="admin-standards" />
      </div>
      <StandardList />
    </div>
  );
};

export default StandardManagementPage;