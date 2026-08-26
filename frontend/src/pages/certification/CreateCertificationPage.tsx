import React from 'react';
import { CreateCertificationForm } from '@/components/certification/CreateCertificationForm';
import { HelpButton } from '@/components/help/HelpButton';

const CreateCertificationPage: React.FC = () => {
  return (
    <div className="space-y-6">
      
      <div className="mb-6 flex justify-end">
        <HelpButton screenKey="certification-create" />
      </div>
      <CreateCertificationForm />
    </div>
  );
};

export default CreateCertificationPage;