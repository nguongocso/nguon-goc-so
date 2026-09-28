import React from 'react';
import { useNavigate } from 'react-router-dom';
import { FileSpreadsheet, FileText } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { ExportOpenDataForm } from '@/components/export/ExportOpenDataForm';
import { HelpButton } from '@/components/help/HelpButton';
import { usePermission } from '@/hooks/usePermission';
import { ListPageHeader } from '@/components/common/ListPageHeader';

/** Trang xuất dữ liệu mở (Open Data Export). Hiển thị biểu mẫu xuất dữ liệu và lối vào quản lý mẫu hồ sơ đối tác. */
export const ExportOpenDataPage: React.FC = () => {
  const navigate = useNavigate();
  const isManager = usePermission(['VT-02']);

  return (
    <div className="space-y-6">
      <ListPageHeader
        icon={FileSpreadsheet}
        title="Xuất dữ liệu mở"
        description="Trích xuất và tải xuống dữ liệu mở phục vụ phân tích, đối soát và tích hợp dữ liệu chuỗi cung ứng nông sản."
        actions={
          <div className="flex items-center gap-2">
            {isManager && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => navigate('/export/profile-templates')}
                className="gap-2 border-emerald-600/30 text-emerald-700 hover:bg-emerald-50 dark:text-emerald-400 dark:hover:bg-emerald-950/40"
              >
                <FileText className="size-4" />
                <span>Quản lý mẫu hồ sơ đối tác</span>
              </Button>
            )}
            <HelpButton screenKey="export-open-data" />
          </div>
        }
      />
      <ExportOpenDataForm />
    </div>
  );
};

export default ExportOpenDataPage;
