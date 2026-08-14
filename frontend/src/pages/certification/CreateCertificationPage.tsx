import React from 'react';
import { CreateCertificationForm } from '@/components/certification/CreateCertificationForm';

const CreateCertificationPage: React.FC = () => {
  return (
    <div className="container mx-auto py-8">
      <CreateCertificationForm />
    </div>
  );
};

export default CreateCertificationPage;