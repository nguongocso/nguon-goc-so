import React from 'react';
import { CreateInvitationForm } from '@/components/invitation/CreateInvitationForm';

const CreateInvitationPage: React.FC = () => {
  return (
    <div className="container mx-auto py-8">
      
      <CreateInvitationForm />
    </div>
  );
};

export default CreateInvitationPage;