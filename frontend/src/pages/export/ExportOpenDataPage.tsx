import { useNavigate } from 'react-router-dom';
import { FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ExportOpenDataForm } from '@/components/export/ExportOpenDataForm';
import { HelpButton } from '@/components/help/HelpButton';
import { usePermission } from '@/hooks/usePermission';

const ExportOpenDataPage = () => {
  const navigate = useNavigate();
  const isManager = usePermission(['VT-02']);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          {isManager && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate('/export/profile-templates')}
              className="gap-2 border-primary/30 text-primary hover:bg-primary/10"
            >
              <FileText className="size-4" />
              <span>Quản lý mẫu hồ sơ đối tác</span>
            </Button>
          )}
        </div>
        <div className="flex items-center gap-2">
          <HelpButton screenKey="export-open-data" />
        </div>
      </div>
      <ExportOpenDataForm />
    </div>
  );
};

export default ExportOpenDataPage;