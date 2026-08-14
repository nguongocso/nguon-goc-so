import React from 'react';
import { StandardList } from '@/components/admin/StandardList';

const StandardManagementPage: React.FC = () => {
  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Quản lý tiêu chuẩn chất lượng</h1>
        <p className="text-sm text-muted-foreground">
          Quản lý danh mục tiêu chuẩn chất lượng dùng chung cho toàn nền tảng.
        </p>
      </div>
      <StandardList />
    </div>
  );
};

export default StandardManagementPage;