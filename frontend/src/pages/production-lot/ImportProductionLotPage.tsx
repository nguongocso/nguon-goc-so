import React from 'react';
import { ImportProductionLotForm } from '@/components/production-lot/ImportProductionLotForm';
import { HelpButton } from '@/components/help/HelpButton';

const ImportProductionLotPage: React.FC = () => {
  return (
    <div className="container mx-auto py-8">
      <div className="mb-6 flex justify-end">
        <HelpButton screenKey="production-lot-import" />
      </div>
      <ImportProductionLotForm />
    </div>
  );
};

export default ImportProductionLotPage;