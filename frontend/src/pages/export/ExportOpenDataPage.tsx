import { ExportOpenDataForm } from '@/components/export/ExportOpenDataForm';
import { HelpButton } from '@/components/help/HelpButton';

const ExportOpenDataPage = () => {
  return (
    <div className="container mx-auto py-8">
      
      <div className="mb-6 flex justify-end">
        <HelpButton screenKey="export-open-data" />
      </div>
      <ExportOpenDataForm />
    </div>
  );
};

export default ExportOpenDataPage;